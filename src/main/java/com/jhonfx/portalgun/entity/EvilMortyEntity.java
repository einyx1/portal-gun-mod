package com.jhonfx.portalgun.entity;

import com.jhonfx.portalgun.curve.CurveHandler;
import com.jhonfx.portalgun.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.thewinnt.cutscenes.CutsceneManager;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Boss da ruptura da Curva: hacking, portais dourados, escudo e implantes de combate. */
public final class EvilMortyEntity extends Monster implements GeoEntity {
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1f,.76f,.05f),1.35f);
    private static final DustParticleOptions WHITE = new DustParticleOptions(new Vector3f(1f,1f,.9f),.8f);
    private static final RawAnimation IDLE=RawAnimation.begin().thenLoop("animation.evil_morty.idle");
    private static final RawAnimation WALK=RawAnimation.begin().thenLoop("animation.evil_morty.walk");
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent=new ServerBossEvent(Component.literal("EVIL MORTY — CENTRAL FINITE CURVE"), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_12);
    private BlockPos battleCenter;
    private int introTicks;
    private int shieldTicks;
    private boolean intro=true;
    private boolean secondPhase;
    private boolean storyAlly;
    private java.util.UUID introEntryPortal,introExitPortal;

    public EvilMortyEntity(EntityType<? extends EvilMortyEntity> type, Level level){super(type,level);xpReward=120;}
    public static AttributeSupplier.Builder createAttributes(){return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH,320).add(Attributes.MOVEMENT_SPEED,.36).add(Attributes.ATTACK_DAMAGE,12)
            .add(Attributes.ARMOR,14).add(Attributes.KNOCKBACK_RESISTANCE,.78).add(Attributes.FOLLOW_RANGE,52);}
    public void configureArena(BlockPos center){battleCenter=center.immutable();setNoAi(true);setInvulnerable(true);
        setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new net.minecraft.world.item.ItemStack(com.jhonfx.portalgun.init.ModItems.EVIL_MORTY_PORTAL_GUN.get()));
        setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND,0.0f);}
    public void setStoryAlly(boolean ally){storyAlly=ally;if(ally){intro=false;setNoAi(false);setInvulnerable(false);
        setCustomName(Component.literal("Evil Morty — Aliado"));setCustomNameVisible(true);
        setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new ItemStack(ModItems.EVIL_MORTY_PORTAL_GUN.get()));
        setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND,0.0f);}}
    public boolean isStoryAlly(){return storyAlly;}

    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(2,new MeleeAttackGoal(this,1.25,false));
        goalSelector.addGoal(5,new LookAtPlayerGoal(this,Player.class,30));goalSelector.addGoal(6,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    @Override public void tick(){
        super.tick();if(level().isClientSide||!isAlive()||!(level() instanceof ServerLevel server))return;
        if(storyAlly){setTarget(null);return;}
        if(intro){tickIntro(server);return;}
        LivingEntity target=getTarget();if(target==null||!target.isAlive()){target=server.getNearestPlayer(this,52);if(target!=null)setTarget(target);else return;}
        if(shieldTicks>0){shieldTicks--;shieldVfx(server);}
        if(tickCount%38==0)armBlaster(server,target);
        if(tickCount%(secondPhase?72:105)==0)projectileVolley(server,target);
        if(tickCount%85==0)portalBlink(server,target);
        if(tickCount%120==0)mechanicalArms(server,target);
        if(tickCount%170==0){shieldTicks=55;addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,60,1,false,false));}
        if(!secondPhase&&getHealth()/getMaxHealth()<=.48f){secondPhase=true;getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.43);addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,120,1,false,false));hackingPulse(server);}
    }
    private void tickIntro(ServerLevel level){
        Player player=level.getNearestPlayer(this,40);if(player==null)return;introTicks++;
        if(introTicks==1){ResourceLocation scene=new ResourceLocation("portalgun","evil_morty_intro");Vec3 c=Vec3.atCenterOf(battleCenter==null?blockPosition():battleCenter);
            if(CutsceneManager.REGISTRY.containsKey(scene))for(ServerPlayer viewer:level.players())if(viewer.distanceToSqr(c)<55*55)com.jhonfx.portalgun.event.CinematicSessionGuard.start(scene,c,viewer,this);
            level.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("message.portalgun.evil_morty_arrival"),false);}
        if(introTicks==20){Vec3 center=Vec3.atCenterOf(battleCenter==null?blockPosition():battleCenter);
            introEntryPortal=CinematicPortal.spawn(level,position().add(0,1,-.7),Direction.SOUTH,PortalColor.YELLOW,.72f);
            introExitPortal=CinematicPortal.spawn(level,center.add(0,1,-1.5),Direction.SOUTH,PortalColor.YELLOW,.72f);
            level.playSound(null,blockPosition(),com.jhonfx.portalgun.init.ModSounds.PORTAL_SPAWN.get(),SoundSource.HOSTILE,1.8f,1.15f);}
        if(introTicks==55)CinematicPortal.activate(level,introEntryPortal,introExitPortal,this);
        if(introTicks>=110){setInvisible(false);CinematicPortal.close(level,introEntryPortal);CinematicPortal.close(level,introExitPortal);intro=false;setNoAi(false);setInvulnerable(false);setTarget(player);}
    }
    private void armBlaster(ServerLevel level,LivingEntity target){Vec3 start=getEyePosition().add(getLookAngle().scale(.25));Vec3 delta=target.getEyePosition().subtract(start);
        for(int i=0;i<30;i++){Vec3 p=start.add(delta.scale(i/29d));level.sendParticles(i%5==0?WHITE:GOLD,p.x,p.y,p.z,1,0,0,0,0);}
        target.hurt(damageSources().magic(),secondPhase?10:7);level.playSound(null,blockPosition(),SoundEvents.GUARDIAN_ATTACK,SoundSource.HOSTILE,1.1f,1.65f);}
    private void portalBlink(ServerLevel level,LivingEntity target){Vec3 old=position();double a=random.nextDouble()*Math.PI*2;Vec3 next=target.position().add(Math.cos(a)*7,.2,Math.sin(a)*7);
        portalRing(level,old.add(0,1,0),1.4,0);teleportTo(next.x,next.y,next.z);portalRing(level,next.add(0,1,0),1.4,Math.PI);level.playSound(null,blockPosition(),SoundEvents.ENDERMAN_TELEPORT,SoundSource.HOSTILE,1.4f,1.7f);}
    private void mechanicalArms(ServerLevel level,LivingEntity target){for(int arm=0;arm<6;arm++){double a=arm*Math.PI/3;Vec3 start=position().add(Math.cos(a)*.6,1.3,Math.sin(a)*.6);Vec3 end=target.position().add(Math.cos(a)*1.2,.8,Math.sin(a)*1.2);Vec3 d=end.subtract(start);
            for(int i=0;i<16;i++){Vec3 p=start.add(d.scale(i/15d));level.sendParticles(i%3==0?net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK:GOLD,p.x,p.y,p.z,1,0,0,0,0);}}
        for(LivingEntity e:level.getEntitiesOfClass(LivingEntity.class,new AABB(target.blockPosition()).inflate(3),e->e instanceof Player))e.hurt(damageSources().mobAttack(this),8);
        level.playSound(null,target.blockPosition(),SoundEvents.TRIDENT_THUNDER,SoundSource.HOSTILE,1.2f,1.8f);}
    private void projectileVolley(ServerLevel level,LivingEntity target){for(int i=0;i<3;i++){double a=(i-1)*.65;Vec3 hit=target.position().add(Math.cos(a)*2,0,Math.sin(a)*2);level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,hit.x,hit.y+.5,hit.z,25,.3,.5,.3,.04);level.explode(this,hit.x,hit.y+.25,hit.z,1.35f,Level.ExplosionInteraction.NONE);}level.playSound(null,target.blockPosition(),SoundEvents.FIREWORK_ROCKET_BLAST,SoundSource.HOSTILE,1.3f,.75f);}
    private void hackingPulse(ServerLevel level){level.sendParticles(net.minecraft.core.particles.ParticleTypes.SONIC_BOOM,getX(),getY()+1,getZ(),1,0,0,0,0);for(Player p:level.players())if(distanceToSqr(p)<48*48){p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,140,1));p.addEffect(new MobEffectInstance(MobEffects.DARKNESS,55,0));p.displayClientMessage(Component.translatable("message.portalgun.evil_morty_hack"),true);}}
    private void shieldVfx(ServerLevel level){if((tickCount&1)!=0)return;for(int i=0;i<14;i++){double a=i*Math.PI*2/14+tickCount*.08;double b=((i*5)%14)*Math.PI/14-Math.PI/2;level.sendParticles(i%4==0?WHITE:GOLD,getX()+Math.cos(a)*Math.cos(b)*1.45,getY()+1+Math.sin(b)*1.45,getZ()+Math.sin(a)*Math.cos(b)*1.45,1,0,0,0,0);}}
    private static void portalRing(ServerLevel level,Vec3 c,double r,double phase){for(int i=0;i<48;i++){double a=i*Math.PI*2/48+phase;level.sendParticles(i%6==0?WHITE:GOLD,c.x+Math.cos(a)*r,c.y+Math.sin(a)*r,c.z,1,0,0,0,0);}}
    @Override public boolean hurt(DamageSource source,float amount){if(shieldTicks>0&&!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))return false;return super.hurt(source,amount);}
    @Override public boolean doHurtTarget(net.minecraft.world.entity.Entity entity){boolean hit=super.doHurtTarget(entity);if(hit&&entity instanceof LivingEntity living&&random.nextFloat()<.38f){living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,50,5));living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,80,2));if(level() instanceof ServerLevel server)server.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,living.getX(),living.getY()+1,living.getZ(),28,.4,.7,.4,.15);}return hit;}
    @Override public boolean canAttack(LivingEntity target){return !storyAlly&&super.canAttack(target);}
    @Override public void die(DamageSource source){super.die(source);if(!storyAlly&&!level().isClientSide&&level() instanceof ServerLevel server){spawnAtLocation(new ItemStack(ModItems.CURVE_STABILIZER.get()));CurveHandler.onEvilMortyDefeated(server,blockPosition());}}
    @Override protected void customServerAiStep(){super.customServerAiStep();bossEvent.setProgress(getHealth()/getMaxHealth());}
    @Override public void startSeenByPlayer(ServerPlayer p){super.startSeenByPlayer(p);if(!storyAlly)bossEvent.addPlayer(p);}
    @Override public void stopSeenByPlayer(ServerPlayer p){super.stopSeenByPlayer(p);bossEvent.removePlayer(p);}
    @Override public boolean canChangeDimensions(){return false;}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("Intro",intro);tag.putInt("IntroTicks",introTicks);tag.putInt("ShieldTicks",shieldTicks);tag.putBoolean("SecondPhase",secondPhase);tag.putBoolean("StoryAlly",storyAlly);if(battleCenter!=null)tag.putLong("BattleCenter",battleCenter.asLong());}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);intro=tag.getBoolean("Intro");introTicks=tag.getInt("IntroTicks");shieldTicks=tag.getInt("ShieldTicks");secondPhase=tag.getBoolean("SecondPhase");storyAlly=tag.getBoolean("StoryAlly");if(tag.contains("BattleCenter"))battleCenter=BlockPos.of(tag.getLong("BattleCenter"));}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<>(this,"movement",3,s->s.setAndContinue(s.isMoving()?WALK:IDLE)));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}

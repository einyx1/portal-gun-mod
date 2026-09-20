package com.jhonfx.portalgun.entity;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.level.Level;

/**
 * Galactic Federation Gromflomite — multi-rank unit.
 *
 * Ranks:
 *   0 SECURITY     — checkpoint/patrol, does NOT attack on sight (low wanted)
 *   1 SOLDIER      — combat unit, standard attacker
 *   2 ELITE        — heavy hunter for high wanted players
 *   3 OFFICER      — buff aura, rallies nearby units
 *   4 SCIENTIST    — lab/brainalyzer operator, fragile
 *   5 PRISON_GUARD — cell guard, slower but tough
 */
public final class FederationAlienEntity extends Monster implements RangedAttackMob {
    private static final EntityDataAccessor<Integer> RANK =
            SynchedEntityData.defineId(FederationAlienEntity.class, EntityDataSerializers.INT);
    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.literal("ALTO COMANDANTE ZARN"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);

    public enum Rank { SECURITY, SOLDIER, ELITE, OFFICER, SCIENTIST, PRISON_GUARD, COMMANDER }

    public FederationAlienEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,          34)
                .add(Attributes.ARMOR,                5)
                .add(Attributes.MOVEMENT_SPEED,      .28)
                .add(Attributes.FOLLOW_RANGE,        36)
                .add(Attributes.ATTACK_DAMAGE,        6)
                .add(Attributes.KNOCKBACK_RESISTANCE,.15);
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(RANK, 1); // SOLDIER by default
    }

    public Rank getRank() {
        int r = entityData.get(RANK);
        Rank[] vals = Rank.values();
        return vals[Math.max(0, Math.min(vals.length-1, r))];
    }

    public void setRank(Rank rank) {
        entityData.set(RANK, rank.ordinal());
        applyRankAttributes(rank);
    }

    private void applyRankAttributes(Rank rank) {
        switch (rank) {
            case SECURITY -> {
                getAttribute(Attributes.MAX_HEALTH).setBaseValue(28);
                getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.22);
                getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4);
            }
            case SOLDIER -> { /* defaults */ }
            case ELITE -> {
                getAttribute(Attributes.MAX_HEALTH).setBaseValue(60);
                getAttribute(Attributes.ARMOR).setBaseValue(10);
                getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.32);
                getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(10);
                getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(.35);
                setGlowingTag(true);
            }
            case OFFICER -> {
                getAttribute(Attributes.MAX_HEALTH).setBaseValue(50);
                getAttribute(Attributes.ARMOR).setBaseValue(8);
                getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(48);
            }
            case SCIENTIST -> {
                getAttribute(Attributes.MAX_HEALTH).setBaseValue(18);
                getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.20);
                getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2);
            }
            case PRISON_GUARD -> {
                getAttribute(Attributes.MAX_HEALTH).setBaseValue(45);
                getAttribute(Attributes.ARMOR).setBaseValue(8);
                getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.22);
                getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(.40);
            }
            case COMMANDER -> {
                getAttribute(Attributes.MAX_HEALTH).setBaseValue(140);
                getAttribute(Attributes.ARMOR).setBaseValue(14);
                getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.30);
                getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(14);
                getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(56);
                getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(.65);
                setGlowingTag(true);
            }
        }
        setHealth(getMaxHealth());
        xpReward = switch (rank) {
            case SECURITY, SCIENTIST -> 6;
            case SOLDIER -> 12;
            case ELITE -> 24;
            case OFFICER -> 18;
            case PRISON_GUARD -> 15;
            case COMMANDER -> 80;
        };
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));

        // Rank is assigned after construction. Query it at attack time rather
        // than freezing the constructor's default SOLDIER behavior forever.
        goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 100, 20));

        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, .9));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());

        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true,
                candidate -> canAttack(candidate)));
    }

    @Override public void tick() {
        super.tick();
        if(!level().isClientSide&&getRank()==Rank.COMMANDER){
            bossEvent.setProgress(Math.max(0,getHealth()/getMaxHealth()));
            if(getHealth()<=getMaxHealth()*.55f&&!getPersistentData().getBoolean("CommanderReinforcements")){
                getPersistentData().putBoolean("CommanderReinforcements",true);
                addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE,160,1));
                for(int i=-1;i<=1;i+=2){FederationAlienEntity guard=com.jhonfx.portalgun.init.ModEntityTypes.FEDERATION_ALIEN.get().create(level());if(guard!=null){guard.setRank(Rank.ELITE);guard.moveTo(getX()+i*3,getY(),getZ()+2,0,0);guard.setTarget(getTarget());guard.getPersistentData().putInt("FederationSabotageStage",getPersistentData().getInt("FederationSabotageStage"));level().addFreshEntity(guard);}}
                level().playSound(null,blockPosition(),SoundEvents.RAID_HORN.value(),SoundSource.HOSTILE,2f,.72f);
                if(level() instanceof net.minecraft.server.level.ServerLevel server)server.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,getX(),getY()+1,getZ(),90,1.2,1.5,1.2,.12);
            }
            if(getHealth()<=getMaxHealth()*.25f&&tickCount%90==0&&level() instanceof net.minecraft.server.level.ServerLevel server){
                server.explode(this,getX(),getY(),getZ(),2.4f,Level.ExplosionInteraction.NONE);
                addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED,100,1));
            }
        }
        // Officer buff: every 40 ticks, boost nearby allies
        if ((getRank() == Rank.OFFICER||getRank()==Rank.COMMANDER) && tickCount % 40 == 0 && !level().isClientSide) {
            level().getEntitiesOfClass(FederationAlienEntity.class,
                    getBoundingBox().inflate(12),
                    e -> e != this && e.isAlive() && e.getRank() != Rank.OFFICER)
                    .forEach(e -> e.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 60, 0, false, false)));
        }

        // Security: warn player before attacking (wanted 0-1)
        if (getRank() == Rank.SECURITY && !level().isClientSide
                && getTarget() instanceof ServerPlayer sp && tickCount % 80 == 0) {
            int wanted = sp.getPersistentData().getInt(com.jhonfx.portalgun.federation.FederationThreatHandler.KEY_WANTED);
            if (wanted <= 1) {
                sp.displayClientMessage(Component.literal(
                        "§c[FED SECURITY]§r Halt. Present your authorization code."), true);
            }
        }
    }

    @Override public void startSeenByPlayer(ServerPlayer player){super.startSeenByPlayer(player);if(getRank()==Rank.COMMANDER)bossEvent.addPlayer(player);}
    @Override public void stopSeenByPlayer(ServerPlayer player){super.stopSeenByPlayer(player);bossEvent.removePlayer(player);}

    @Override public void performRangedAttack(LivingEntity target, float power) {
        if (level().isClientSide || !canAttack(target) || !hasLineOfSight(target)) return;
        ShulkerBullet bolt = new ShulkerBullet(level(), this, target, Direction.Axis.Y);
        bolt.setPos(getX(), getEyeY() - 0.15, getZ());
        level().addFreshEntity(bolt);
        if(getRank()==Rank.COMMANDER){
            ShulkerBullet second=new ShulkerBullet(level(),this,target,Direction.Axis.X);
            second.setPos(getX(),getEyeY()+.2,getZ());level().addFreshEntity(second);
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING,80,0));
        }
        level().playSound(null, blockPosition(),
                getRank() == Rank.ELITE ? SoundEvents.BEACON_POWER_SELECT : SoundEvents.BEACON_DEACTIVATE,
                SoundSource.HOSTILE,
                getRank() == Rank.ELITE ? 0.9f : 0.65f,
                getRank() == Rank.ELITE ? 1.4f  : 1.7f);
    }

    @Override public boolean canAttack(LivingEntity target){
        if(target instanceof ServerPlayer player){int wanted=player.getPersistentData().getInt(com.jhonfx.portalgun.federation.FederationThreatHandler.KEY_WANTED);
            if(getRank()==Rank.SECURITY&&wanted<=1)return false;
            if(getRank()==Rank.SCIENTIST&&wanted<=2)return false;
        }
        return super.canAttack(target);
    }

    @Override protected void dropCustomDeathLoot(net.minecraft.world.damagesource.DamageSource source,int looting,boolean recentlyHit){
        super.dropCustomDeathLoot(source,looting,recentlyHit);
        if(getRank()==Rank.COMMANDER){spawnAtLocation(new net.minecraft.world.item.ItemStack(com.jhonfx.portalgun.init.ModItems.BLEMFLARCKS.get(),32));spawnAtLocation(new net.minecraft.world.item.ItemStack(com.jhonfx.portalgun.init.ModItems.WEAPON_ENERGY_CELL.get(),2));spawnAtLocation(new net.minecraft.world.item.ItemStack(com.jhonfx.portalgun.init.ModItems.QUANTUM_ISOTOPE.get(),1));}
    }

    @Override protected net.minecraft.world.InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
        if (!level().isClientSide && getPersistentData().getBoolean("PortalGunStoryCommander")
                && player instanceof ServerPlayer serverPlayer) {
            com.jhonfx.portalgun.federation.FederationSignalArc.interactCommander(serverPlayer);
            return net.minecraft.world.InteractionResult.CONSUME;
        }
        return super.mobInteract(player, hand);
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Rank", entityData.get(RANK));
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        float savedHealth=getHealth();
        if (tag.contains("Rank")) {
            int r = tag.getInt("Rank");
            Rank[] vals = Rank.values();
            setRank(vals[Math.max(0, Math.min(vals.length-1, r))]);
            setHealth(Math.min(savedHealth,getMaxHealth()));
        }
    }
}

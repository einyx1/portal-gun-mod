package com.jhonfx.portalgun.item;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.List;

/** Hitscan Rick technology with distinct damage and status behavior per weapon. */
public final class RickWeaponItem extends Item implements software.bernie.geckolib.animatable.GeoItem {
    private static final String HEAT="PortalGunWeaponHeat";
    public enum Weapon { LASER, PLASMA_9_GAUGE, FREEZE_RAY, MINDBLOWER, NEUTRINO_BOMB }
    private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(1f,.05f,.02f),1.15f);
    private static final DustParticleOptions PLASMA = new DustParticleOptions(new Vector3f(.45f,.05f,1f),1.35f);
    private static final DustParticleOptions ICE = new DustParticleOptions(new Vector3f(.15f,.8f,1f),1.1f);
    private final Weapon weapon;
    private final software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache cache=software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    public RickWeaponItem(Properties properties, Weapon weapon) { super(properties); this.weapon=weapon;software.bernie.geckolib.animatable.SingletonGeoAnimatable.registerSyncedAnimatable(this); }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack=player.getItemInHand(hand);
        if(level.isClientSide) return InteractionResultHolder.success(stack);
        ServerLevel server=(ServerLevel)level;
        int heat=stack.getOrCreateTag().getInt(HEAT);
        if(heat>=100){triggerAnim(player,software.bernie.geckolib.animatable.GeoItem.getOrAssignId(stack,server),"weapon","overheat");player.displayClientMessage(Component.translatable("message.portalgun.weapon_overheated"),true);server.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,SoundSource.PLAYERS,.6f,1.8f);return InteractionResultHolder.fail(stack);}
        if(weapon==Weapon.NEUTRINO_BOMB) {
            Vec3 hit=player.pick(56,0,false).getLocation();
            server.sendParticles(PLASMA,hit.x,hit.y,hit.z,180,3,3,3,.16);
            server.explode(player,hit.x,hit.y,hit.z,9f,Level.ExplosionInteraction.TNT);
            for(LivingEntity victim:server.getEntitiesOfClass(LivingEntity.class,new AABB(hit,hit).inflate(38),v->v!=player&&v.isAlive())) {
                float damage=(victim instanceof com.jhonfx.portalgun.entity.RickPrimeEntity
                        || victim instanceof com.jhonfx.portalgun.entity.EvilMortyEntity
                        || victim instanceof com.jhonfx.portalgun.entity.DianeRobotEntity) ? 40 : 120;
                victim.hurt(player.damageSources().explosion(player,player),damage);
            }
            server.playSound(null,player.blockPosition(),com.jhonfx.portalgun.init.ModSounds.WEAPON_NEUTRINO.get(),SoundSource.PLAYERS,2.5f,.72f);
            if(!player.getAbilities().instabuild)stack.shrink(1);
            return InteractionResultHolder.consume(stack);
        }

        LivingEntity primary=target(player,weapon==Weapon.PLASMA_9_GAUGE?42:48);
        if(primary==null)return InteractionResultHolder.fail(stack);
        switch(weapon) {
            case LASER -> {
                List<LivingEntity> pierced=targetsOnRay(player,48);
                for(int i=0;i<Math.min(3,pierced.size());i++)pierced.get(i).hurt(player.damageSources().playerAttack(player),16-i*3);
                beam(server,player.getEyePosition(),primary.getEyePosition(),RED);
                cooldown(player,6);
            }
            case PLASMA_9_GAUGE -> {
                beam(server,player.getEyePosition(),primary.getEyePosition(),PLASMA);
                primary.hurt(player.damageSources().playerAttack(player),34);
                server.explode(player,primary.getX(),primary.getY()+.8,primary.getZ(),2.4f,Level.ExplosionInteraction.NONE);
                cooldown(player,22);
            }
            case FREEZE_RAY -> {
                beam(server,player.getEyePosition(),primary.getEyePosition(),ICE);
                primary.setTicksFrozen(300);
                primary.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,240,10));
                primary.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,240,4));
                primary.getPersistentData().putLong("PortalGunFrozenUntil",level.getGameTime()+240);
                cooldown(player,30);
            }
            case MINDBLOWER -> {
                beam(server,player.getEyePosition(),primary.getEyePosition(),PLASMA);
                primary.removeAllEffects();
                primary.addEffect(new MobEffectInstance(MobEffects.CONFUSION,360,1));
                primary.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,180,0));
                primary.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,360,4));
                if(primary instanceof Mob mob){mob.setTarget(null);mob.getNavigation().stop();}
                primary.getPersistentData().putLong("PortalGunMindWipedUntil",level.getGameTime()+360);
                cooldown(player,50);
            }
            default -> {}
        }
        int added=switch(weapon){case LASER->9;case PLASMA_9_GAUGE->28;case FREEZE_RAY->18;case MINDBLOWER->22;default->0;};
        stack.getOrCreateTag().putInt(HEAT,Math.min(100,heat+added));
        triggerAnim(player,software.bernie.geckolib.animatable.GeoItem.getOrAssignId(stack,server),"weapon","fire");
        stack.hurtAndBreak(1,player,p->p.broadcastBreakEvent(hand));
        net.minecraft.sounds.SoundEvent sound=switch(weapon){case LASER->com.jhonfx.portalgun.init.ModSounds.WEAPON_LASER.get();case PLASMA_9_GAUGE->com.jhonfx.portalgun.init.ModSounds.WEAPON_PLASMA.get();case FREEZE_RAY->com.jhonfx.portalgun.init.ModSounds.WEAPON_FREEZE.get();case MINDBLOWER->com.jhonfx.portalgun.init.ModSounds.WEAPON_MINDBLOWER.get();default->com.jhonfx.portalgun.init.ModSounds.WEAPON_LASER.get();};
        server.playSound(null,player.blockPosition(),sound,SoundSource.PLAYERS,1.1f,1f);
        return InteractionResultHolder.consume(stack);
    }

    private void cooldown(Player player,int ticks){player.getCooldowns().addCooldown(this,ticks);}
    private static LivingEntity target(Player p,double range){return targetsOnRay(p,range).stream().findFirst().orElse(null);}
    private static List<LivingEntity> targetsOnRay(Player p,double range){Vec3 eye=p.getEyePosition(),look=p.getLookAngle();AABB box=p.getBoundingBox().expandTowards(look.scale(range)).inflate(2.5);
        return p.level().getEntitiesOfClass(LivingEntity.class,box,e->e!=p&&e.isAlive()).stream().filter(e->{Vec3 c=e.getBoundingBox().getCenter();double along=c.subtract(eye).dot(look);return along>=0&&along<=range&&c.distanceToSqr(eye.add(look.scale(along)))<4.2;}).sorted(Comparator.comparingDouble(p::distanceToSqr)).toList();}
    private static void beam(ServerLevel l,Vec3 a,Vec3 b,DustParticleOptions color){Vec3 d=b.subtract(a);int steps=Math.min(36,Math.max(12,(int)(d.length()*1.4)));for(int i=0;i<=steps;i++){Vec3 q=a.add(d.scale(i/(double)steps));l.sendParticles(i%7==0?net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK:color,q.x,q.y,q.z,1,0,0,0,0);}}

    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> tooltip,net.minecraft.world.item.TooltipFlag flag){
        tooltip.add(Component.translatable("tooltip.portalgun.weapon."+weapon.name().toLowerCase()));
        if(weapon!=Weapon.NEUTRINO_BOMB)tooltip.add(Component.translatable("tooltip.portalgun.weapon_energy",stack.getMaxDamage()-stack.getDamageValue(),stack.getMaxDamage(),stack.getOrCreateTag().getInt(HEAT)));
    }
    @Override public void inventoryTick(ItemStack stack,Level level,net.minecraft.world.entity.Entity entity,int slot,boolean selected){if(!level.isClientSide&&level.getGameTime()%5==0&&stack.hasTag()){int heat=stack.getTag().getInt(HEAT);if(heat>0){int cooled=Math.max(0,heat-2);stack.getTag().putInt(HEAT,cooled);if(cooled==0&&entity instanceof ServerPlayer player&&level instanceof ServerLevel server)triggerAnim(player,software.bernie.geckolib.animatable.GeoItem.getOrAssignId(stack,server),"weapon","reload");}}}
    @Override public boolean isBarVisible(ItemStack stack){return weapon!=Weapon.NEUTRINO_BOMB&&(super.isBarVisible(stack)||stack.getOrCreateTag().getInt(HEAT)>0);}
    @Override public int getBarWidth(ItemStack stack){int heat=stack.getOrCreateTag().getInt(HEAT);return heat>0?Math.round(13f*(100-heat)/100f):super.getBarWidth(stack);}
    @Override public int getBarColor(ItemStack stack){return stack.getOrCreateTag().getInt(HEAT)>0?0xFF5A19:super.getBarColor(stack);}
    public String assetId(){return switch(weapon){case LASER->"laser_gun";case PLASMA_9_GAUGE->"plasma_pistol";case FREEZE_RAY->"freeze_ray";case MINDBLOWER->"mindblower_gun";case NEUTRINO_BOMB->"neutrino_bomb";};}
    @Override public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers){controllers.add(new software.bernie.geckolib.core.animation.AnimationController<>(this,"weapon",0,state->state.setAndContinue(software.bernie.geckolib.core.animation.RawAnimation.begin().thenLoop("animation.rick_weapon.idle"))).triggerableAnim("fire",software.bernie.geckolib.core.animation.RawAnimation.begin().thenPlay("animation.rick_weapon.fire")).triggerableAnim("overheat",software.bernie.geckolib.core.animation.RawAnimation.begin().thenPlay("animation.rick_weapon.overheat")).triggerableAnim("reload",software.bernie.geckolib.core.animation.RawAnimation.begin().thenPlay("animation.rick_weapon.reload")));}
    @Override public software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){if(weapon==Weapon.NEUTRINO_BOMB)return;consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){private com.jhonfx.portalgun.client.RickWeaponRenderer renderer;@Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new com.jhonfx.portalgun.client.RickWeaponRenderer();return renderer;}});}
}

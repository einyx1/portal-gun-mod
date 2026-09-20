package com.jhonfx.portalgun.event;

import com.jhonfx.portalgun.init.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class CombatTechHandler {
    @SubscribeEvent public void bossDrops(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
        if (event.getEntity() instanceof com.jhonfx.portalgun.entity.RickPrimeEntity
                || event.getEntity() instanceof com.jhonfx.portalgun.entity.EvilMortyEntity)
            event.getDrops().removeIf(drop -> drop.getItem().getItem() instanceof com.jhonfx.portalgun.item.PortalGunItem);
    }
    @SubscribeEvent public void clonePlayer(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        var original = event.getOriginal().getPersistentData();
        var replacement = event.getEntity().getPersistentData();
        java.util.Set<String> transientKeys=java.util.Set.of("PortalGunJetpackFlight","PortalGunPreviousFlight",
                "PortalGunPurgeAirborne","PortalGunPurgeFall","PortalGunFrozenUntil","PortalGunMindWipedUntil",
                "PortalGunFederationScanUntil","PortalGunPurgeWeaponReady","PortalGunEyepatchDodgeReady");
        for (String key : original.getAllKeys()) {
            if ((key.startsWith("PortalGun") && !transientKeys.contains(key)) || key.equals("RickPrimeDefeated"))
                replacement.put(key, original.get(key).copy());
        }
    }
    @SubscribeEvent public void livingTick(LivingEvent.LivingTickEvent e){if(e.getEntity().level().isClientSide)return;long now=e.getEntity().level().getGameTime();long until=e.getEntity().getPersistentData().getLong("PortalGunFederationScanUntil");if(until>0&&now>=until){e.getEntity().setGlowingTag(false);e.getEntity().getPersistentData().remove("PortalGunFederationScanUntil");}long wiped=e.getEntity().getPersistentData().getLong("PortalGunMindWipedUntil");if(wiped>now&&e.getEntity() instanceof net.minecraft.world.entity.Mob mob){mob.setTarget(null);mob.getNavigation().stop();}else if(wiped>0&&wiped<=now)e.getEntity().getPersistentData().remove("PortalGunMindWipedUntil");}
    @SubscribeEvent public void tick(TickEvent.PlayerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END||e.player.level().isClientSide)return;
        boolean equipped=e.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(ModItems.COMBAT_JETPACK.get());
        var tag=e.player.getPersistentData(); var abilities=e.player.getAbilities();
        if(equipped&&!tag.getBoolean("PortalGunJetpackFlight")) {
            tag.putBoolean("PortalGunPreviousFlight", abilities.mayfly);
            tag.putBoolean("PortalGunJetpackFlight",true);
            abilities.mayfly=true; e.player.onUpdateAbilities();
        } else if(!equipped&&tag.getBoolean("PortalGunJetpackFlight")) {
            abilities.mayfly=tag.getBoolean("PortalGunPreviousFlight")||e.player.isCreative()||e.player.isSpectator();
            if(!abilities.mayfly)abilities.flying=false;
            tag.remove("PortalGunJetpackFlight");tag.remove("PortalGunPreviousFlight");e.player.onUpdateAbilities();
        }
        if(e.player.tickCount%80==0&&tag.getBoolean("PortalGunCybernetics")) {
            e.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,220,0,false,false));
            e.player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,220,0,false,false));
            e.player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,220,0,false,false));
            e.player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,220,0,false,false));
        }
        if(e.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(ModItems.EVIL_MORTY_EYEPATCH.get())) {
            e.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,240,0,false,false));
            if(e.player.tickCount%10==0) analyzeCombat(e.player);
        }
        if(hasFullPurgeSuit(e.player)&&e.player.tickCount%40==0) {
            e.player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,80,1,false,false));
            e.player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,80,0,false,false));
            e.player.addEffect(new MobEffectInstance(MobEffects.JUMP,80,1,false,false));
        }
        if(hasFullPurgeSuit(e.player)) purgeMovementVfx((ServerPlayer)e.player);
    }
    @SubscribeEvent public void attack(LivingAttackEvent e){
        if(!(e.getEntity() instanceof ServerPlayer player)||!player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(ModItems.EVIL_MORTY_EYEPATCH.get()))return;
        long now=player.level().getGameTime(),ready=player.getPersistentData().getLong("PortalGunEyepatchDodgeReady");
        if(now<ready)return;
        boolean projectile=e.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile;
        float chance=projectile?.62f:.30f;
        if(player.getRandom().nextFloat()>=chance)return;
        e.setCanceled(true);player.getPersistentData().putLong("PortalGunEyepatchDodgeReady",now+28);
        net.minecraft.world.phys.Vec3 side=player.getLookAngle().cross(new net.minecraft.world.phys.Vec3(0,1,0)).normalize().scale(player.getRandom().nextBoolean()?1.35:-1.35);
        if(player.level().noCollision(player,player.getBoundingBox().move(side)))player.teleportTo(player.getX()+side.x,player.getY(),player.getZ()+side.z);
        player.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,player.getX(),player.getY()+1,player.getZ(),24,.35,.7,.35,.06);
        player.serverLevel().playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,net.minecraft.sounds.SoundSource.PLAYERS,.65f,1.8f);
        player.displayClientMessage(Component.translatable("message.portalgun.eyepatch_predicted"),true);
    }
    @SubscribeEvent public void hurt(LivingHurtEvent e){
        if(e.getSource().getEntity() instanceof ServerPlayer attacker){
            if(attacker.getPersistentData().getBoolean("PortalGunCybernetics")&&attacker.getMainHandItem().isEmpty()){e.setAmount(e.getAmount()*1.75f);attacker.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,e.getEntity().getX(),e.getEntity().getY()+1,e.getEntity().getZ(),18,.35,.5,.35,.1);}
            if(hasFullPurgeSuit(attacker)){e.setAmount(e.getAmount()*2.1f);e.getEntity().setSecondsOnFire(4);}
        }
        long frozen=e.getEntity().getPersistentData().getLong("PortalGunFrozenUntil");
        if(frozen>e.getEntity().level().getGameTime()){e.setAmount(e.getAmount()*3f);e.getEntity().getPersistentData().remove("PortalGunFrozenUntil");if(e.getEntity().level() instanceof net.minecraft.server.level.ServerLevel server)server.sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE,e.getEntity().getX(),e.getEntity().getY()+1,e.getEntity().getZ(),45,.6,.8,.6,.12);}
        if(!(e.getEntity() instanceof ServerPlayer player))return;
        if(hasFullPurgeSuit(player))e.setAmount(e.getAmount()*.42f);
        if(!player.getPersistentData().getBoolean("PortalGunShieldActive"))return;
        e.setAmount(e.getAmount()*.25f);net.minecraft.server.level.ServerLevel level=player.serverLevel();net.minecraft.core.particles.DustParticleOptions cyan=new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(.05f,.85f,1f),1.15f);for(int i=0;i<72;i++){double a=i*2.39996,b=Math.asin(1-2*(i+.5)/72);level.sendParticles(i%8==0?net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK:cyan,player.getX()+Math.cos(a)*Math.cos(b)*1.5,player.getY()+1+Math.sin(b)*1.5,player.getZ()+Math.sin(a)*Math.cos(b)*1.5,1,0,0,0,0);}level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.SHIELD_BLOCK,net.minecraft.sounds.SoundSource.PLAYERS,1.1f,e.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile?1.7f:1.25f);
    }
    @SubscribeEvent public void death(LivingDeathEvent e){if(!(e.getEntity() instanceof ServerPlayer player)||!player.getPersistentData().getBoolean("PortalGunPhoenixActive"))return;e.setCanceled(true);net.minecraft.server.level.ServerLevel target=player.getServer().getLevel(player.getRespawnDimension());net.minecraft.core.BlockPos spawn=player.getRespawnPosition();if(target==null)target=player.getServer().overworld();if(spawn==null)spawn=target.getSharedSpawnPos();player.teleportTo(target,spawn.getX()+.5,spawn.getY()+1,spawn.getZ()+.5,player.getYRot(),player.getXRot());player.setHealth(player.getMaxHealth());player.removeAllEffects();player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,240,2));player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,240,2));player.displayClientMessage(Component.translatable("message.portalgun.phoenix_activated"),false);}
    private static void analyzeCombat(net.minecraft.world.entity.player.Player player){
        net.minecraft.world.entity.LivingEntity target=player.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,player.getBoundingBox().inflate(24),e->e!=player&&e.isAlive()&&(e instanceof net.minecraft.world.entity.monster.Monster||e instanceof com.jhonfx.portalgun.entity.RickPrimeEntity||e instanceof com.jhonfx.portalgun.entity.EvilMortyEntity||e instanceof com.jhonfx.portalgun.entity.FederationAlienEntity)).stream().min(java.util.Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
        if(target==null)return;target.setGlowingTag(true);target.getPersistentData().putLong("PortalGunFederationScanUntil",player.level().getGameTime()+30);
        if(player.tickCount%20==0){boolean attacking=target instanceof net.minecraft.world.entity.Mob mob&&mob.getTarget()==player;player.displayClientMessage(Component.translatable(attacking?"message.portalgun.eyepatch_warning":"message.portalgun.eyepatch_scan",target.getDisplayName(),String.format("%.1f",target.getHealth()),String.format("%.1f",player.distanceTo(target))),true);}
    }
    private static boolean hasFullPurgeSuit(net.minecraft.world.entity.LivingEntity e){return e.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(ModItems.PURGE_HELMET.get())&&e.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(ModItems.PURGE_CHESTPLATE.get())&&e.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).is(ModItems.PURGE_LEGGINGS.get())&&e.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(ModItems.PURGE_BOOTS.get());}
    public static void handlePurgeInput(ServerPlayer player,boolean cycle){
        if(!hasFullPurgeSuit(player)){player.displayClientMessage(Component.translatable("message.portalgun.purge_need_suit"),true);return;}
        ItemStack chest=player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        String[] modes={"MISSILE","FLAMETHROWER","SHOCK","BLADE","SAW"};int current=Math.floorMod(chest.getOrCreateTag().getInt("PortalGunPurgeMode"),modes.length);
        if(cycle){current=(current+1)%modes.length;chest.getOrCreateTag().putInt("PortalGunPurgeMode",current);player.getPersistentData().putInt("PortalGunPurgeMode",current);player.displayClientMessage(Component.translatable("message.portalgun.purge_mode",Component.translatable("mode.portalgun.purge."+modes[current].toLowerCase())),true);return;}
        long now=player.level().getGameTime();if(now<player.getPersistentData().getLong("PortalGunPurgeWeaponReady"))return;
        int cooldown=switch(current){case 0->30;case 1->8;case 2->24;case 3->12;default->5;};player.getPersistentData().putLong("PortalGunPurgeWeaponReady",now+cooldown);
        double range=current==0?36:current==1?11:current==2?20:5;
        net.minecraft.world.entity.LivingEntity target=aimedTarget(player,range,current<=2?3.8:6.5);if(target==null)return;
        if(chest.getItem() instanceof com.jhonfx.portalgun.item.PurgeSuitItem suit)
            suit.triggerAnim(player,software.bernie.geckolib.animatable.GeoItem.getOrAssignId(chest,player.serverLevel()),"suit",modes[current].toLowerCase());
        net.minecraft.world.phys.Vec3 a=player.getEyePosition(),b=target.getBoundingBox().getCenter(),d=b.subtract(a);net.minecraft.server.level.ServerLevel server=player.serverLevel();
        switch(current){
            case 0->{for(int i=0;i<=22;i++){var p=a.add(d.scale(i/22d));server.sendParticles(i%3==0?net.minecraft.core.particles.ParticleTypes.FLAME:net.minecraft.core.particles.ParticleTypes.SMOKE,p.x,p.y,p.z,1,0,0,0,0);}server.explode(player,target.getX(),target.getY()+.7,target.getZ(),2.2f,net.minecraft.world.level.Level.ExplosionInteraction.NONE);target.hurt(player.damageSources().playerAttack(player),14);server.playSound(null,target.blockPosition(),com.jhonfx.portalgun.init.ModSounds.PURGE_MISSILE.get(),net.minecraft.sounds.SoundSource.PLAYERS,1.3f,.9f);}
            case 1->{target.setSecondsOnFire(8);target.hurt(player.damageSources().onFire(),6);server.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,target.getX(),target.getY()+1,target.getZ(),55,.8,.8,.8,.1);server.playSound(null,target.blockPosition(),com.jhonfx.portalgun.init.ModSounds.PURGE_FLAME.get(),net.minecraft.sounds.SoundSource.PLAYERS,1.1f,1f);}
            case 2->{target.hurt(player.damageSources().lightningBolt(),11);target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,80,8));server.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,target.getX(),target.getY()+1,target.getZ(),60,.7,.9,.7,.14);server.playSound(null,target.blockPosition(),com.jhonfx.portalgun.init.ModSounds.PURGE_SHOCK.get(),net.minecraft.sounds.SoundSource.PLAYERS,1.2f,1.2f);}
            case 3->{target.hurt(player.damageSources().playerAttack(player),18);target.knockback(1.1,player.getX()-target.getX(),player.getZ()-target.getZ());server.playSound(null,target.blockPosition(),com.jhonfx.portalgun.init.ModSounds.PURGE_BLADE.get(),net.minecraft.sounds.SoundSource.PLAYERS,1.1f,1.1f);}
            default->{target.hurt(player.damageSources().playerAttack(player),8);server.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,target.getX(),target.getY()+1,target.getZ(),25,.35,.5,.35,.15);server.playSound(null,target.blockPosition(),com.jhonfx.portalgun.init.ModSounds.PURGE_SAW.get(),net.minecraft.sounds.SoundSource.PLAYERS,1.0f,.8f);}
        }
    }
    private static void purgeMovementVfx(ServerPlayer player){
        var tag=player.getPersistentData();boolean airborne=!player.onGround();
        if(airborne){tag.putBoolean("PortalGunPurgeAirborne",true);tag.putFloat("PortalGunPurgeFall",Math.max(tag.getFloat("PortalGunPurgeFall"),player.fallDistance));if(player.tickCount%4==0){var l=player.serverLevel();l.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,player.getX(),player.getY()+.15,player.getZ(),4,.28,.05,.28,.015);l.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,player.getX(),player.getY()+.12,player.getZ(),2,.22,.04,.22,.01);}}
        else if(tag.getBoolean("PortalGunPurgeAirborne")){float fall=tag.getFloat("PortalGunPurgeFall");tag.remove("PortalGunPurgeAirborne");tag.remove("PortalGunPurgeFall");if(fall>2.5f){var l=player.serverLevel();l.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,player.getX(),player.getY()+.08,player.getZ(),24,.75,.05,.75,.08);l.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,player.getX(),player.getY()+.12,player.getZ(),14,.6,.08,.6,.12);l.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.IRON_GOLEM_STEP,net.minecraft.sounds.SoundSource.PLAYERS,.8f,.7f);}}
    }
    private static net.minecraft.world.entity.LivingEntity aimedTarget(net.minecraft.world.entity.player.Player player,double range,double tolerance){var eye=player.getEyePosition();var look=player.getLookAngle();return player.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,player.getBoundingBox().expandTowards(look.scale(range)).inflate(3),e->e!=player&&e.isAlive()).stream().filter(e->{var c=e.getBoundingBox().getCenter();double along=c.subtract(eye).dot(look);return along>=0&&along<=range&&c.distanceToSqr(eye.add(look.scale(along)))<=tolerance;}).min(java.util.Comparator.comparingDouble(player::distanceToSqr)).orElse(null);}
}

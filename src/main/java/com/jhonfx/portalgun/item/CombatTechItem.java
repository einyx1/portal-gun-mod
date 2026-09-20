package com.jhonfx.portalgun.item;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Comparator;

public final class CombatTechItem extends Item {
    public enum Tech { IMPLANT, BLASTER, SHIELD, JETPACK, NET, PHOENIX, REGENERATOR }
    private static final DustParticleOptions CYAN=new DustParticleOptions(new Vector3f(.05f,.85f,1f),1.0f);
    private final Tech tech;
    public CombatTechItem(Properties properties,Tech tech){super(properties);this.tech=tech;}

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand){
        ItemStack stack=player.getItemInHand(hand);if(level.isClientSide)return InteractionResultHolder.success(stack);
        ServerLevel server=(ServerLevel)level;
        switch(tech){
            case IMPLANT -> {boolean active=!player.getPersistentData().getBoolean("PortalGunCybernetics");player.getPersistentData().putBoolean("PortalGunCybernetics",active);message(player,active?"message.portalgun.implant_installed":"message.portalgun.implant_disabled");}
            case BLASTER -> {LivingEntity target=target(player,26);if(target==null)return InteractionResultHolder.fail(stack);beam(server,player.getEyePosition(),target.getEyePosition());if(player.isShiftKeyDown()){Vec3 hit=target.position().add(0,.8,0);server.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,hit.x,hit.y,hit.z,45,.8,.7,.8,.08);server.explode(player,hit.x,hit.y,hit.z,2.1f,Level.ExplosionInteraction.NONE);}else target.hurt(player.damageSources().playerAttack(player),11);}
            case SHIELD -> {boolean active=!player.getPersistentData().getBoolean("PortalGunShieldActive");player.getPersistentData().putBoolean("PortalGunShieldActive",active);if(active){player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,200,3));sphere(server,player.position().add(0,1,0));}message(player,active?"message.portalgun.shield_enabled":"message.portalgun.shield_disabled");}
            case JETPACK -> {ItemStack chest=player.getItemBySlot(EquipmentSlot.CHEST);if(!chest.isEmpty())return InteractionResultHolder.fail(stack);ItemStack equipped=stack.copy();equipped.setCount(1);player.setItemSlot(EquipmentSlot.CHEST,equipped);stack.shrink(1);message(player,"message.portalgun.jetpack_enabled");}
            case NET -> {LivingEntity target=target(player,18);if(target==null)return InteractionResultHolder.fail(stack);target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,240,6));target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,240,2));target.setGlowingTag(true);beam(server,player.getEyePosition(),target.getEyePosition());}
            case PHOENIX -> {boolean active=!player.getPersistentData().getBoolean("PortalGunPhoenixActive");player.getPersistentData().putBoolean("PortalGunPhoenixActive",active);message(player,active?"message.portalgun.phoenix_ready":"message.portalgun.phoenix_disabled");}
            case REGENERATOR -> {player.heal(10);player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,200,2));consume(player,stack);server.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,player.getX(),player.getY()+1,player.getZ(),20,.6,.8,.6,.05);}
        }
        server.playSound(null,player.blockPosition(),SoundEvents.BEACON_POWER_SELECT,SoundSource.PLAYERS,1,1.4f);return InteractionResultHolder.consume(stack);
    }
    private LivingEntity target(Player player,double range){Vec3 eye=player.getEyePosition(),look=player.getLookAngle(),end=eye.add(look.scale(range));AABB box=player.getBoundingBox().expandTowards(look.scale(range)).inflate(2);
        return player.level().getEntitiesOfClass(LivingEntity.class,box,e->e!=player&&e.isAlive()).stream().filter(e->{Vec3 p=e.getBoundingBox().getCenter();double along=p.subtract(eye).dot(look);if(along<0||along>range)return false;return p.distanceToSqr(eye.add(look.scale(along)))<3.2;}).min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);}
    private static void beam(ServerLevel level,Vec3 a,Vec3 b){Vec3 d=b.subtract(a);for(int i=0;i<30;i++){Vec3 p=a.add(d.scale(i/29d));level.sendParticles(i%5==0?net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK:CYAN,p.x,p.y,p.z,1,0,0,0,0);}}
    private static void sphere(ServerLevel level,Vec3 c){for(int i=0;i<60;i++){double a=i*2.39996,b=Math.asin(1-2*(i+.5)/60);level.sendParticles(CYAN,c.x+Math.cos(a)*Math.cos(b)*1.5,c.y+Math.sin(b)*1.5,c.z+Math.sin(a)*Math.cos(b)*1.5,1,0,0,0,0);}}
    private static void consume(Player p,ItemStack stack){if(!p.getAbilities().instabuild)stack.shrink(1);}
    private static void message(Player p,String key){p.displayClientMessage(Component.translatable(key),true);}
    public Tech tech(){return tech;}
    @Override public EquipmentSlot getEquipmentSlot(ItemStack stack){return tech==Tech.JETPACK?EquipmentSlot.CHEST:super.getEquipmentSlot(stack);}
}

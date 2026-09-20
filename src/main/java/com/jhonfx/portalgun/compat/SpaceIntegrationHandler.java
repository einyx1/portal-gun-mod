package com.jhonfx.portalgun.compat;

import com.jhonfx.portalgun.init.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Dependency-free environmental bridge for optional Ad Astra worlds. */
public final class SpaceIntegrationHandler {
    @SubscribeEvent public void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer player)||player.tickCount%100!=0)return;
        if(!isSpace(player))return;
        boolean protectedPlayer=protectedPlayer(player);
        if(!protectedPlayer&&player.level().getGameTime()>=player.getPersistentData().getLong("PortalGunSpaceWarningAt")){
            player.getPersistentData().putLong("PortalGunSpaceWarningAt",player.level().getGameTime()+600);
            player.displayClientMessage(Component.literal("§cAMBIENTE ESPACIAL HOSTIL — §fequipe um traje Ad Astra, Traje Purge completo ou ative o campo de força."),true);
        }
        if(protectedPlayer){
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WATER_BREATHING,140,0,false,false));
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE,140,0,false,false));
        }
    }

    @SubscribeEvent public void hurt(LivingHurtEvent event){
        if(!(event.getEntity() instanceof ServerPlayer player)||!isSpace(player)||!protectedPlayer(player))return;
        String id=event.getSource().getMsgId().toLowerCase(java.util.Locale.ROOT);
        if(id.contains("oxygen")||id.contains("space")||id.contains("acid_rain")||id.contains("cryo")||id.contains("temperature"))event.setCanceled(true);
    }

    private static boolean isSpace(ServerPlayer player){return AdAstraCompat.isLoaded()&&player.level().dimension().location().getNamespace().equals("ad_astra");}
    private static boolean protectedPlayer(ServerPlayer player){
        if(AdAstraCompat.hasSpaceSuit(player)||player.getPersistentData().getBoolean("PortalGunShieldActive"))return true;
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.PURGE_HELMET.get())
                &&player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.PURGE_CHESTPLATE.get())
                &&player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.PURGE_LEGGINGS.get())
                &&player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.PURGE_BOOTS.get());
    }
}

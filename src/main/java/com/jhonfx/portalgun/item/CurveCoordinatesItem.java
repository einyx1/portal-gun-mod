package com.jhonfx.portalgun.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class CurveCoordinatesItem extends Item {
    public CurveCoordinatesItem(Properties properties){super(properties);}
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand){
        ItemStack coordinates=player.getItemInHand(hand);ItemStack gun=player.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
        if(!(gun.getItem() instanceof PortalGunItem)){player.displayClientMessage(Component.translatable("message.portalgun.curve_hold_gun"),true);return InteractionResultHolder.fail(coordinates);}
        if(!level.isClientSide){gun.getOrCreateTag().putBoolean("OutsideCurveAccess",true);player.getPersistentData().putBoolean("PortalGunOutsideCurveAccess",true);if(!player.getAbilities().instabuild)coordinates.shrink(1);player.displayClientMessage(Component.translatable("message.portalgun.curve_gun_modified"),false);}
        return InteractionResultHolder.sidedSuccess(coordinates,level.isClientSide);
    }
}

package com.jhonfx.portalgun.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Consumed from the main hand to recharge a Rick weapon held offhand. */
public final class WeaponEnergyCellItem extends Item {
    public WeaponEnergyCellItem(Properties properties){super(properties);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack cell=player.getItemInHand(hand),weapon=hand==InteractionHand.MAIN_HAND?player.getOffhandItem():player.getMainHandItem();
        if(!(weapon.getItem() instanceof RickWeaponItem)||!weapon.isDamaged()){player.displayClientMessage(Component.translatable("message.portalgun.energy_cell_hold_weapon"),true);return InteractionResultHolder.fail(cell);}
        if(!level.isClientSide){weapon.setDamageValue(0);weapon.getOrCreateTag().putInt("PortalGunWeaponHeat",0);if(!player.getAbilities().instabuild)cell.shrink(1);level.playSound(null,player.blockPosition(),SoundEvents.RESPAWN_ANCHOR_CHARGE,SoundSource.PLAYERS,1f,1.35f);player.displayClientMessage(Component.translatable("message.portalgun.weapon_recharged"),true);}
        return InteractionResultHolder.consume(cell);
    }
}

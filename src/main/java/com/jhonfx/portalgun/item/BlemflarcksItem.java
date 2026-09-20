package com.jhonfx.portalgun.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * BLEMFLARCK — New Galactic Federation currency.
 *
 * Stackable to 999. Used in Federation shops, bribes, services.
 * Drop from Federation soldiers; also earned by completing legal tasks.
 */
public final class BlemflarcksItem extends Item {
    public BlemflarcksItem(Properties properties) { super(properties); }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§aMoeda da Nova Federação Galáctica."));
        tooltip.add(Component.literal("§7Use em lojas, subornos e serviços federais."));
    }
}

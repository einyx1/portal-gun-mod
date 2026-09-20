package com.jhonfx.portalgun.federation;

import com.jhonfx.portalgun.init.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Federation economy helper — mirrors CitadelEconomy but for Blemflarcks.
 */
public final class FederationEconomy {

    public static int count(Player player) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(ModItems.BLEMFLARCKS.get())) total += s.getCount();
        }
        return total;
    }

    public static boolean take(Player player, int amount) {
        if (player.getAbilities().instabuild) return true;
        if (count(player) < amount) return false;
        int left = amount;
        for (int i = 0; i < player.getInventory().getContainerSize() && left > 0; i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.is(ModItems.BLEMFLARCKS.get())) continue;
            int removed = Math.min(left, s.getCount());
            s.shrink(removed);
            left -= removed;
        }
        return true;
    }

    public static void give(Player player, int amount) {
        ItemStack stack = new ItemStack(ModItems.BLEMFLARCKS.get(), amount);
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private FederationEconomy() {}
}

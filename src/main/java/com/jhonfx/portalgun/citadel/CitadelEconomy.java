package com.jhonfx.portalgun.citadel;

import com.jhonfx.portalgun.init.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class CitadelEconomy {
    public static int count(Player player) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.CITADEL_CREDIT.get())) total += stack.getCount();
        }
        return total;
    }

    public static boolean take(Player player, int amount) {
        if (player.getAbilities().instabuild) return true;
        if (count(player) < amount) return false;
        int left = amount;
        for (int i = 0; i < player.getInventory().getContainerSize() && left > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(ModItems.CITADEL_CREDIT.get())) continue;
            int removed = Math.min(left, stack.getCount());
            stack.shrink(removed);
            left -= removed;
        }
        return true;
    }

    public static void give(Player player, int amount) {
        ItemStack credits = new ItemStack(ModItems.CITADEL_CREDIT.get(), amount);
        if (!player.getInventory().add(credits)) player.drop(credits, false);
    }

    private CitadelEconomy() {}
}

package com.jhonfx.portalgun.item;

import com.jhonfx.portalgun.init.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Applies the P-0 upgrade even when the player obtained the Neural Scanner after visiting the archive. */
public final class MultiversalScannerUpgradeItem extends Item {
    public MultiversalScannerUpgradeItem(Properties properties) { super(properties); }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack chip=player.getItemInHand(hand);
        if(level.isClientSide)return InteractionResultHolder.success(chip);
        for(ItemStack candidate:player.getInventory().items) {
            if(candidate.is(ModItems.NEURAL_SCANNER.get())) {
                if(candidate.getOrCreateTag().getBoolean("MultiversalUpgrade")) {
                    player.displayClientMessage(Component.literal("§7O Scanner Neural já possui o upgrade multiversal."),true);
                    return InteractionResultHolder.fail(chip);
                }
                candidate.getOrCreateTag().putBoolean("MultiversalUpgrade",true);
                if(!player.getAbilities().instabuild)chip.shrink(1);
                player.displayClientMessage(Component.literal("§bSCANNER MULTIVERSAL ATIVO §7— assinatura P-0 disponível."),false);
                player.playSound(net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,1f,1.6f);
                return InteractionResultHolder.consume(chip);
            }
        }
        player.displayClientMessage(Component.literal("§cColoque um Scanner Neural no inventário antes de usar o chip."),true);
        return InteractionResultHolder.fail(chip);
    }
}

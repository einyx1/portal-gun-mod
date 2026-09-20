package com.jhonfx.portalgun.item;

import com.jhonfx.portalgun.entity.CitadelNpcEntity;
import com.jhonfx.portalgun.entity.FederationAlienEntity;
import com.jhonfx.portalgun.entity.RickPrimeEntity;
import com.jhonfx.portalgun.init.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Neural Scanner — no cooldown.
 *
 * Right-click on any living entity to scan it.
 * Output changes depending on entity type:
 *   - Rick variants (CitadelNpc) → shows dimension signature + threat
 *   - C-524 → grants fragment (original behaviour preserved)
 *   - Rick Prime / clones → shows Prime signature + EXTREME threat
 *   - Federation troops → shows rank profile
 *   - Players → shows federation wanted + portal tech flag
 *   - Generic mobs → basic profile
 */
public final class NeuralScannerItem extends Item {

    public NeuralScannerItem(Properties properties) { super(properties); }

    /** Clique com o Scanner sem alvo — ativa "Localizar P-0" se o scanner estiver upgradado. */
    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(
            net.minecraft.world.level.Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp
                && stack.getOrCreateTag().getBoolean("MultiversalUpgrade")) {
            com.jhonfx.portalgun.federation.FederationSignalArc.triggerDecoyScan(sp);
            return net.minecraft.world.InteractionResultHolder.success(stack);
        }
        return net.minecraft.world.InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                   LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) return InteractionResult.SUCCESS;

        // Save last scanned entity type for other systems that read this
        stack.getOrCreateTag().putString("LastEntity", target.getType().toString());
        stack.getTag().putString("LastName", target.getDisplayName().getString());

        StringBuilder sb = new StringBuilder();
        sb.append("§b[ NEURAL SCANNER — SCAN RESULT ]§r\n");

        if (target instanceof CitadelNpcEntity npc) {
            handleCitadelNpc(npc, player, stack, sb);

        } else if (target instanceof RickPrimeEntity prime) {
            sb.append("§fENTITY: §4RICK SANCHEZ (PRIME VARIANT)§r\n");
            sb.append("§fCLASSIFICATION: §4").append(prime.isClone() ? "CLONE UNIT" : "ORIGINAL — PRIME").append("\n");
            sb.append("§fDIMENSION SIGNATURE: §cUNKNOWN — MULTIVERSAL\n");
            sb.append("§fTHREAT LEVEL: §4EXTREME\n");
            sb.append("§fNEURAL PATTERN: §cREJECTED — HOSTILE ENCRYPTION\n");
            sb.append("§fRECOMMENDATION: §cFLEE OR ELIMINATE\n");

        } else if (target instanceof FederationAlienEntity alien) {
            String rank = alien.getRank().name().replace("_", " ");
            sb.append("§fSPECIES: §aGROMFLOMITE\n");
            sb.append("§fRANK: §a").append(rank).append("\n");
            sb.append("§fAFFILIATION: §aNEW GALACTIC FEDERATION\n");
            sb.append("§fTHREAT: §e").append(
                    alien.getRank() == FederationAlienEntity.Rank.ELITE ? "HIGH"
                    : alien.getRank() == FederationAlienEntity.Rank.OFFICER ? "MEDIUM-HIGH"
                    : "MEDIUM").append("\n");
            sb.append("§fNEURAL PATTERN: §aSTANDARD SOLDIER ENGRAM\n");

        } else if (target instanceof ServerPlayer scanned) {
            int wanted = scanned.getPersistentData().getInt("FedWanted");
            boolean hasPG = scanned.getInventory().items.stream()
                    .anyMatch(s -> s.getItem() instanceof PortalGunItem);
            sb.append("§fSPECIES: §eHUMAN\n");
            sb.append("§fNAME: §e").append(scanned.getName().getString()).append("\n");
            sb.append("§fFED WANTED: §c").append(wanted).append("/5\n");
            sb.append("§fPORTAL TECH: §c").append(hasPG ? "DETECTED" : "NONE").append("\n");
            sb.append("§fNEURAL PATTERN: §eSCANNED — NO IMPLANT\n");

        } else if (target instanceof Monster monster) {
            float hpPct = monster.getHealth() / monster.getMaxHealth();
            String name = target.getDisplayName().getString().toUpperCase();
            sb.append("§fENTITY: §e").append(name).append("\n");
            sb.append("§fTHREAT CLASS: §e").append(hpPct > 0.6 ? "HIGH" : hpPct > 0.3 ? "MEDIUM" : "LOW").append("\n");
            sb.append("§fFED DATABASE: §7NO MATCH\n");
            sb.append("§fNEURAL PATTERN: §7PRIMITIVE — NO DATA\n");

        } else {
            sb.append("§fENTITY: §7").append(target.getDisplayName().getString().toUpperCase()).append("\n");
            sb.append("§fTHREAT: §7UNKNOWN\n");
            sb.append("§fNEURAL PATTERN: §7NOT APPLICABLE\n");
        }

        player.sendSystemMessage(Component.literal(sb.toString()));
        player.level().playSound(null, player.blockPosition(),
                net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.7f);

        return InteractionResult.CONSUME;
    }

    private void handleCitadelNpc(CitadelNpcEntity npc, Player player, ItemStack stack, StringBuilder sb) {
        CitadelNpcEntity.Variant variant = npc.getVariant();

        // C-524: grant fragment (original mechanic preserved)
        if (variant == CitadelNpcEntity.Variant.C524) {
            if (!player.getPersistentData().getBoolean("PortalGunC524Fragment")) {
                player.getPersistentData().putBoolean("PortalGunC524Fragment", true);
                ItemStack fragment = new ItemStack(ModItems.C524_NEURAL_FRAGMENT.get());
                if (!player.getInventory().add(fragment)) player.drop(fragment, false);
                player.displayClientMessage(Component.translatable("message.portalgun.c524_fragment"), false);
            } else {
                player.displayClientMessage(Component.translatable("message.portalgun.c524_scanned"), true);
            }
            return;
        }

        sb.append("§fSPECIES: §bHUMAN (RICK SANCHEZ VARIANT)\n");
        sb.append("§fVARIANT CLASS: §b").append(variant.name()).append("\n");
        sb.append("§fAFFILIATION: §b").append(switch (variant) {
            case GUARD   -> "CITADEL SECURITY";
            case COUNCIL -> "COUNCIL OF RICKS";
            case PRIME   -> "UNKNOWN — PRIME SIGNATURE DETECTED";
            case MORTY   -> "CITADEL — MORTY WORKER";
            default      -> "CITADEL OF RICKS";
        }).append("\n");
        sb.append("§fTHREAT LEVEL: §b").append(
                variant == CitadelNpcEntity.Variant.PRIME   ? "§4EXTREME" :
                variant == CitadelNpcEntity.Variant.COUNCIL ? "§cHIGH" :
                variant == CitadelNpcEntity.Variant.GUARD   ? "§eMEDIUM" : "§7LOW").append("\n");
        if (variant == CitadelNpcEntity.Variant.PRIME) {
            sb.append("§fNEURAL PATTERN: §4SANCHEZ — UNIDENTIFIED VARIANT\n");
            sb.append("§cWARNING: PRIME SIGNATURE DETECTED\n");
        }
    }
}

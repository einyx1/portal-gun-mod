package com.jhonfx.portalgun.item;

import com.jhonfx.portalgun.entity.CitadelNpcEntity;
import com.jhonfx.portalgun.entity.FederationAlienEntity;
import com.jhonfx.portalgun.entity.RickPrimeEntity;
import com.jhonfx.portalgun.federation.FederationThreatHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * FEDERATION IDENTIFICATION SCANNER (Series 9000)
 *
 * Shift+Use on a living entity → detailed profile.
 * Use with no target → area scan of monsters.
 *
 * No cooldown — the original 300-tick cooldown has been removed.
 */
public final class FederationThreatScannerItem extends Item {

    public FederationThreatScannerItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true);

        ServerLevel server = (ServerLevel) level;

        // Find the entity the player is looking at (within 10 blocks)
        LivingEntity target = findLookedAt(server, player);

        if (target != null) {
            printEntityProfile(player, target);
        } else {
            // Area scan
            List<LivingEntity> threats = server.getEntitiesOfClass(LivingEntity.class,
                    new AABB(player.blockPosition()).inflate(48), e -> e.isAlive() && e != player);

            for (LivingEntity e : threats) {
                e.setGlowingTag(true);
                e.getPersistentData().putLong("PortalGunFederationScanUntil", server.getGameTime() + 240);
            }

            // Spiral scan particles
            for (int i = 0; i < 72; i++) {
                double a = i * Math.PI / 18.0, r = 1.0 + (i % 18) * 0.09;
                server.sendParticles(i % 3 == 0 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.END_ROD,
                        player.getX() + Math.cos(a)*r, player.getY() + 1.1, player.getZ() + Math.sin(a)*r,
                        1, 0, 0.03, 0, 0);
            }
            server.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                    SoundSource.PLAYERS, 0.9f, 1.45f);

            printAreaScan(player, threats.size(), server);
        }

        return InteractionResultHolder.success(stack);
    }

    // ── Entity profile printer ────────────────────────────────────────────────
    private static void printEntityProfile(Player player, LivingEntity target) {
        StringBuilder sb = new StringBuilder();
        sb.append("§b[ FEDERATION IDENTIFICATION SCANNER ]§r\n");

        if (target instanceof ServerPlayer scanned) {
            int wanted = scanned.getPersistentData().getInt(FederationThreatHandler.KEY_WANTED);
            boolean hasPG = scanned.getInventory().items.stream()
                    .anyMatch(s -> s.getItem() instanceof PortalGunItem);
            sb.append("§fSPECIES: §eHUMAN\n");
            sb.append("§fFED CITIZENSHIP: §cNONE\n");
            sb.append("§fTHREAT LEVEL: §c").append(wanted).append("\n");
            sb.append("§fWARRANT: §c").append(wanted > 0 ? "ACTIVE" : "NONE").append("\n");
            sb.append("§fRICK TECH SIGNATURE: §c").append(hasPG ? "DETECTED" : "NONE").append("\n");

        } else if (target instanceof RickPrimeEntity prime) {
            sb.append("§fIDENTITY: §4RICK SANCHEZ§r\n");
            sb.append("§fFED DATABASE: §cMATCH — EXTREME THREAT\n");
            sb.append("§fCLASSIFICATION: §4");
            sb.append(prime.isClone() ? "SANCHEZ CLONE UNIT" : "SANCHEZ — UNIDENTIFIED VARIANT").append("\n");
            sb.append("§fWARRANT: §cPERMANENT\n");
            sb.append("§fRECOMMENDATION: §4DO NOT ENGAGE\n");

        } else if (target instanceof FederationAlienEntity) {
            sb.append("§fSPECIES: §aGROMFLOMITE\n");
            sb.append("§fAFFILIATION: §aGALACTIC FEDERATION\n");
            sb.append("§fRANK: §aSECURITY OFFICER\n");
            sb.append("§fCLEARANCE: §a2\n");
            sb.append("§fSTATUS: §aACTIVE PATROL\n");

        } else if (target instanceof CitadelNpcEntity npc) {
            String variant = npc.getVariant().name();
            sb.append("§fSPECIES: §bHUMAN\n");
            sb.append("§fAFFILIATION: §bCITADEL OF RICKS\n");
            sb.append("§fVARIANT: §b").append(variant).append("\n");
            sb.append("§fTHREAT: §b").append(
                    npc.getVariant() == CitadelNpcEntity.Variant.PRIME ? "EXTREME" : "MODERATE").append("\n");

        } else {
            // Generic mob
            String name = target.getType().getDescriptionId().replace("entity.", "").replace(".", " ").toUpperCase();
            float hpPct = target.getHealth() / target.getMaxHealth();
            sb.append("§fSPECIES: §e").append(name).append("\n");
            sb.append("§fTHREAT CLASS: §e").append(hpPct > 0.7 ? "HIGH" : hpPct > 0.3 ? "MEDIUM" : "LOW").append("\n");
            sb.append("§fFED DATABASE: §7NO MATCH\n");
        }

        player.sendSystemMessage(Component.literal(sb.toString()));
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 0.7f, 1.6f);
    }

    private static void printAreaScan(Player player, int count, ServerLevel server) {
        int wanted = player instanceof ServerPlayer sp
                ? sp.getPersistentData().getInt(FederationThreatHandler.KEY_WANTED)
                : 0;
        player.sendSystemMessage(Component.literal(
                "§b[ AREA SCAN ]§r\n" +
                "§fENTITIES TAGGED: §e" + count + "\n" +
                "§fPLAYER THREAT LEVEL: §c" + wanted + "\n" +
                "§fFED STATUS: §c" + (wanted > 0 ? "WANTED" : "CLEAR")));
    }

    // ── Raycast to find looked-at entity ─────────────────────────────────────
    private static LivingEntity findLookedAt(ServerLevel level, Player player) {
        var hit = level.clip(new net.minecraft.world.level.ClipContext(
                player.getEyePosition(),
                player.getEyePosition().add(player.getLookAngle().scale(10.0)),
                net.minecraft.world.level.ClipContext.Block.OUTLINE,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                player));
        double bestDist = 10.0 * 10.0;
        LivingEntity best = null;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().expandTowards(player.getLookAngle().scale(10)).inflate(1.5),
                e2 -> e2.isAlive() && e2 != player)) {
            double dist = e.distanceToSqr(player);
            if (dist < bestDist) { bestDist = dist; best = e; }
        }
        return best;
    }
}

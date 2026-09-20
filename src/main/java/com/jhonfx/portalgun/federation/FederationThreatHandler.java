package com.jhonfx.portalgun.federation;

import com.jhonfx.portalgun.entity.FederationAlienEntity;
import com.jhonfx.portalgun.init.ModEntityTypes;
import com.jhonfx.portalgun.init.ModItems;
import com.jhonfx.portalgun.item.PortalGunItem;
import com.jhonfx.portalgun.item.PortalGunVariant;
import com.jhonfx.portalgun.PortalGunMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * NEW GALACTIC FEDERATION — Wanted Level System
 *
 * Level 0 — CIVIL          (no response)
 * Level 1 — CONTRABAND     (inspect order, may not attack)
 * Level 2 — SUSPECT        (identity flagged, light patrol)
 * Level 3 — FUGITIVE       (active hunt, scanners auto-flag)
 * Level 4 — TERRORIST      (elite units, portal inhibition attempt)
 * Level 5 — PRIORITY TARGET (capture alive, heavy units)
 */
public final class FederationThreatHandler {
    // NBT keys stored per-player
    public static final String KEY_WANTED  = "FedWanted";
    public static final String KEY_LAST    = "FedLastCrime";
    public static final String KEY_CAPTURED = "FedCaptured";   // prison event active
    public static final String KEY_INHIBIT  = "FedInhibit";   // portal inhibited ticks remaining

    private static final ResourceKey<net.minecraft.world.level.Level> FEDERATION =
            ResourceKey.create(Registries.DIMENSION,
                    new ResourceLocation(PortalGunMod.MOD_ID, "federation"));

    // ── Raise wanted when a Federation alien is hurt ──────────────────────────
    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof FederationAlienEntity)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        long now = player.level().getGameTime();
        long last = player.getPersistentData().getLong(KEY_LAST);
        if (now - last > 20) {
            raise(player, 1, "message.portalgun.fed_wanted_assault");
            player.getPersistentData().putLong(KEY_LAST, now);
        }
    }

    // ── Raise wanted more when a Federation alien is killed ───────────────────
    @SubscribeEvent
    public void onKill(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof FederationAlienEntity)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        raise(player, 2, "message.portalgun.fed_wanted_killed");
    }

    // ── Detect Portal Gun use near Federation zones ───────────────────────────
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;

        if (player.level().dimension().equals(FEDERATION)
                && FederationSavedData.get(player.serverLevel()).isDestroyed()) {
            player.getPersistentData().putInt(KEY_WANTED, 0);
            player.getPersistentData().remove(KEY_INHIBIT);
            return;
        }

        int wanted = getWanted(player);
        long now = player.level().getGameTime();

        // Escaping federal space cools the search down instead of leaving the story stuck at level 5.
        if (!player.level().dimension().equals(FEDERATION) && wanted > 0 && player.tickCount % 600 == 0) {
            lower(player, 1);
            wanted = getWanted(player);
        }

        // Portal tech detection in federation dim
        if (player.level().dimension().equals(FEDERATION) && player.tickCount % 60 == 0) {
            if (isCarryingPortalGun(player) && wanted < 1) {
                raise(player, 1, "message.portalgun.fed_tech_detected");
            }
        }

        // Spawn patrols based on wanted level — rate-limited
        if (wanted <= 0 || player.tickCount % spawnInterval(wanted) != 0) return;
        if (!player.level().dimension().equals(FEDERATION) && wanted < 3) return;

        spawnPatrol(player, wanted);

        // Wanted 5: attempt capture (only in Federation dim, low health more likely)
        if (wanted >= 5 && player.level().dimension().equals(FEDERATION)) {
            boolean lowHealth = player.getHealth() <= player.getMaxHealth() * 0.35f;
            if (lowHealth || player.getRandom().nextFloat() < 0.15f) {
                com.jhonfx.portalgun.federation.FederationPrisonEvent.captureTry(player);
            }
        }

        // Portal inhibition at wanted 4+
        if (wanted >= 4 && player.level().dimension().equals(FEDERATION)) {
            int inhibit = player.getPersistentData().getInt(KEY_INHIBIT);
            if (inhibit <= 0) {
                player.getPersistentData().putInt(KEY_INHIBIT, 200);
                player.displayClientMessage(
                        Component.translatable("message.portalgun.fed_inhibitor_active"), true);
            }
        }

        // Tick down inhibitor
        int inhibit = player.getPersistentData().getInt(KEY_INHIBIT);
        if (inhibit > 0) {
            player.getPersistentData().putInt(KEY_INHIBIT, inhibit - 1);
        }
    }

    // ── Patrol spawn ──────────────────────────────────────────────────────────
    private static void spawnPatrol(ServerPlayer player, int wanted) {
        int count = switch (wanted) {
            case 1 -> 1;
            case 2 -> 1;
            case 3 -> 2;
            case 4 -> 2;
            default -> 3;
        };

        // Cap total aliens near player to avoid mob spam
        long near = player.serverLevel().getEntitiesOfClass(FederationAlienEntity.class,
                new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(48),
                FederationAlienEntity::isAlive).size();
        count = (int) Math.max(0, Math.min(count, 4 - near));

        for (int i = 0; i < count; i++) {
            FederationAlienEntity alien = ModEntityTypes.FEDERATION_ALIEN.get().create(player.serverLevel());
            if (alien == null) continue;

            // Scale stats with wanted level
            if (wanted >= 4 && alien.getAttribute(Attributes.MAX_HEALTH) != null) {
                alien.getAttribute(Attributes.MAX_HEALTH).setBaseValue(60 + (wanted - 4) * 20);
                alien.setHealth(alien.getMaxHealth());
            }

            double angle = i * Math.PI * 2.0 / count + player.getRandom().nextDouble();
            double radius = 12 + player.getRandom().nextDouble() * 8;
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * radius);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * radius);
            int y = player.serverLevel().getHeight(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y < player.serverLevel().getMinBuildHeight() + 2) y = player.blockPosition().getY();

            alien.moveTo(x + .5, y, z + .5, (float) Math.toDegrees(angle + Math.PI), 0);

            // Levels 1-2: do NOT immediately target player — they inspect
            if (wanted >= 3) alien.setTarget(player);

            alien.setPersistenceRequired();
            player.serverLevel().addFreshEntity(alien);
        }

        // ── Drones join at wanted 4+ ────────────────────────────────────────────
        if (wanted >= 4) {
            int drones = wanted >= 5 ? 2 : 1;
            int nearbyDrones = player.serverLevel().getEntitiesOfClass(
                    com.jhonfx.portalgun.entity.FederationDroneEntity.class,
                    new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(64), drone -> drone.isAlive()).size();
            drones = Math.min(drones, Math.max(0, 3 - nearbyDrones));
            for (int i = 0; i < drones; i++) {
                var drone = com.jhonfx.portalgun.init.ModEntityTypes.FEDERATION_DRONE.get().create(player.serverLevel());
                if (drone == null) continue;
                double angle = player.getRandom().nextDouble() * Math.PI * 2;
                double radius = 14 + player.getRandom().nextDouble() * 6;
                drone.moveTo(player.getX() + Math.cos(angle) * radius, player.getY() + 6,
                        player.getZ() + Math.sin(angle) * radius, 0, 0);
                drone.setTarget(player);
                drone.setPersistenceRequired();
                player.serverLevel().addFreshEntity(drone);
            }
        }

        // ── Heavy Trooper joins at wanted 5 (capture operations) ────────────────
        if (wanted >= 5) {
            long heavyNear = player.serverLevel().getEntitiesOfClass(
                    com.jhonfx.portalgun.entity.FederationHeavyTrooperEntity.class,
                    new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(48),
                    e -> e.isAlive()).size();
            if (heavyNear < 1) {
                var heavy = com.jhonfx.portalgun.init.ModEntityTypes.FEDERATION_HEAVY.get().create(player.serverLevel());
                if (heavy != null) {
                    double angle = player.getRandom().nextDouble() * Math.PI * 2;
                    int x = (int) Math.floor(player.getX() + Math.cos(angle) * 16);
                    int z = (int) Math.floor(player.getZ() + Math.sin(angle) * 16);
                    int y = player.serverLevel().getHeight(
                            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    heavy.moveTo(x + .5, y, z + .5, 0, 0);
                    heavy.setTarget(player);
                    heavy.setPersistenceRequired();
                    player.serverLevel().addFreshEntity(heavy);
                }
            }
        }

        String msgKey = wanted <= 2
                ? "message.portalgun.fed_response_inspect"
                : wanted <= 4
                ? "message.portalgun.fed_response_pursuit"
                : "message.portalgun.fed_response_capture";
        player.displayClientMessage(Component.translatable(msgKey, wanted), true);
    }

    // ── Raise wanted ──────────────────────────────────────────────────────────
    public static void raise(ServerPlayer player, int amount, String reasonKey) {
        int current = getWanted(player);
        int newLevel = Math.min(5, current + amount);
        if (newLevel == current) return;
        player.getPersistentData().putInt(KEY_WANTED, newLevel);
        player.displayClientMessage(
                Component.translatable("message.portalgun.fed_wanted_level", newLevel), false);
        if (!reasonKey.isEmpty()) {
            player.displayClientMessage(Component.translatable(reasonKey), true);
        }
    }

    public static void lower(ServerPlayer player, int amount) {
        int newLevel = Math.max(0, getWanted(player) - amount);
        player.getPersistentData().putInt(KEY_WANTED, newLevel);
        if (newLevel == 0) player.displayClientMessage(
                Component.translatable("message.portalgun.fed_cleared"), true);
    }

    public static int getWanted(ServerPlayer player) {
        return player.getPersistentData().getInt(KEY_WANTED);
    }

    public static boolean isPortalInhibited(ServerPlayer player) {
        return player.getPersistentData().getInt(KEY_INHIBIT) > 0;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static boolean isCarryingPortalGun(ServerPlayer player) {
        for (ItemStack s : player.getInventory().items) {
            if (s.getItem() instanceof PortalGunItem) return true;
        }
        return false;
    }

    private static int spawnInterval(int wanted) {
        return switch (wanted) {
            case 1 -> 2400;
            case 2 -> 1400;
            case 3 -> 800;
            case 4 -> 500;
            default -> 320;
        };
    }
}

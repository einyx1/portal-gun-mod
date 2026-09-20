package com.jhonfx.portalgun.omega;

import com.jhonfx.portalgun.block.OmegaDeviceBlock;
import com.jhonfx.portalgun.init.ModBlocks;
import com.jhonfx.portalgun.init.ModSounds;
import com.jhonfx.portalgun.network.ModNetwork;
import com.jhonfx.portalgun.network.OmegaEffectPacket;
import com.jhonfx.portalgun.entity.RickPrimeEntity;
import com.jhonfx.portalgun.entity.EvilMortyEntity;
import com.jhonfx.portalgun.entity.DianeRobotEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OmegaDeviceHandler {
    private static final int CHARGE_TICKS = 120;
    private static final Map<DeviceKey, Session> ACTIVE = new HashMap<>();
    private static final Map<DeviceKey, Cooldown> COOLDOWNS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Vec3> ARMED_FINAL_SHOTS = new HashMap<>();
    private static final Map<ResourceKey<Level>, SelfDestruct> SELF_DESTRUCTS = new HashMap<>();
    private static final DustParticleOptions OMEGA_GREEN = new DustParticleOptions(new Vector3f(0.05f, 1.0f, 0.22f), 1.25f);
    private static final DustParticleOptions OMEGA_CYAN = new DustParticleOptions(new Vector3f(0.05f, 0.72f, 1.0f), 1.35f);
    private static final DustParticleOptions OMEGA_WHITE = new DustParticleOptions(new Vector3f(0.95f, 1.0f, 1.0f), 1.0f);
    private static final DustParticleOptions OMEGA_BLACK = new DustParticleOptions(new Vector3f(0.01f, 0.015f, 0.01f), 1.7f);

    public static void buildPlayerStructure(Level rawLevel, BlockPos console, BlockState state, LivingEntity placer) {
        if (!(rawLevel instanceof ServerLevel level)) return;
        Direction facing = state.getValue(OmegaDeviceBlock.FACING);
        BlockPos center = console.relative(facing, OmegaStructureBuilder.CONSOLE_DISTANCE);
        OmegaStructureBuilder.build(level, center, facing);
        if (placer instanceof Player player)
            player.displayClientMessage(Component.translatable("message.portalgun.omega_built"), true);
    }

    public static void activate(Level rawLevel, BlockPos console, BlockState state, Player player) {
        if (!(rawLevel instanceof ServerLevel level)) return;
        boolean finalShot = ARMED_FINAL_SHOTS.containsKey(level.dimension())
                && ARMED_FINAL_SHOTS.get(level.dimension()).distanceTo(Vec3.atCenterOf(console)) < 42;
        if (level.dimension().location().equals(com.jhonfx.portalgun.federation.UnmortrickenFacility.ID)) {
            var campaign = com.jhonfx.portalgun.federation.UnmortrickenFacility.get(level);
            if (!campaign.isDefeated() || campaign.destructTicks() >= 0) {
                player.displayClientMessage(Component.literal(campaign.isDefeated() ? "O disparo Ômega já foi consumido." : "OMEGA LOCKDOWN — ELIMINE RICK PRIME"), true);
                return;
            }
            finalShot = true;
        }
        if (isPortalSuppressed(level, Vec3.atCenterOf(console))) {
            player.displayClientMessage(Component.literal("OMEGA LOCKDOWN — ELIMINE RICK PRIME"), true);
            return;
        }
        DeviceKey key = new DeviceKey(level.dimension(), console.asLong());
        if (ACTIVE.containsKey(key) || COOLDOWNS.containsKey(key)) {
            player.displayClientMessage(Component.translatable("message.portalgun.omega_active"), true);
            return;
        }

        Direction facing = state.getValue(OmegaDeviceBlock.FACING);
        BlockPos centerBlock = console.relative(facing, OmegaStructureBuilder.CONSOLE_DISTANCE).above(2);
        Vec3 center = Vec3.atBottomCenterOf(centerBlock);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(4.5, 4.0, 4.5),
                entity -> entity.isAlive() && !(entity instanceof Player));
        LivingEntity target = targets.stream().min((a, b) ->
                Double.compare(a.distanceToSqr(center), b.distanceToSqr(center))).orElse(null);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.portalgun.omega_no_target"), true);
            return;
        }

        markHeld(target);

        ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        int variants = typeId == null ? 1 : countExisting(level, typeId);
        ACTIVE.put(key, new Session(target.getUUID(), console.immutable(), center, 0, typeId, variants, finalShot));
        level.setBlock(console, state.setValue(OmegaDeviceBlock.ACTIVE, true), 3);
        setMachineActive(level, center, true);
        level.playSound(null, center.x, center.y, center.z, ModSounds.OMEGA_POWERUP.get(),
                SoundSource.BLOCKS, 3.2f, 1.0f);
        player.displayClientMessage(Component.translatable("message.portalgun.omega_started",
                target.getDisplayName()), true);
        sendScreenEffect(level, center, 140, 0.45f, false);
    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        if (level.dimension().location().equals(com.jhonfx.portalgun.federation.UnmortrickenFacility.ID)
                && !SELF_DESTRUCTS.containsKey(level.dimension())) {
            var campaign = com.jhonfx.portalgun.federation.UnmortrickenFacility.get(level);
            if (campaign.destructTicks() >= 0 && campaign.destructTicks() < 657)
                SELF_DESTRUCTS.put(level.dimension(), new SelfDestruct(campaign.destructCenter(), campaign.destructTicks()));
        }
        List<DeviceKey> finished = new ArrayList<>();
        for (Map.Entry<DeviceKey, Session> entry : ACTIVE.entrySet()) {
            if (!entry.getKey().dimension.equals(level.dimension())) continue;
            Session session = entry.getValue();
            Entity raw = level.getEntity(session.target);
            if (!(raw instanceof LivingEntity target) || !target.isAlive()) {
                if(raw instanceof LivingEntity living)restoreHeld(living);
                omegaFailure(level, entry.getKey(), session);
                finished.add(entry.getKey());
                continue;
            }

            target.teleportTo(session.center.x, session.center.y, session.center.z);
            target.setDeltaMovement(Vec3.ZERO);
            target.setInvulnerable(true);
            if (target instanceof Mob mob) mob.setNoAi(true);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 10, 0, false, false));

            emitEnergy(level, session.center, session.ticks);
            if (session.ticks % 5 == 0) showStatus(level, session);
            if (session.ticks > 0 && session.ticks % 20 == 0)
                level.playSound(null, session.center.x, session.center.y, session.center.z,
                        ModSounds.OMEGA_ARC.get(), SoundSource.BLOCKS, 1.8f,
                        0.7f + session.ticks / (float) CHARGE_TICKS * 0.6f);

            if (session.ticks >= CHARGE_TICKS) {
                ResourceLocation typeId = session.typeId;
                if (typeId != null) OmegaBanSavedData.get(level).erase(typeId);
                eraseExisting(level, typeId, session.center);
                finalPulse(level, session.center);
                level.playSound(null, session.center.x, session.center.y, session.center.z,
                        ModSounds.OMEGA_ERASE.get(), SoundSource.BLOCKS, 4.0f, 1.0f);
                level.getServer().getPlayerList().broadcastSystemMessage(Component.translatable(
                        "message.portalgun.omega_erased", typeId == null ? "unknown" : typeId.toString()), false);
                sendScreenEffect(level, session.center, 20, 1.0f, true);
                COOLDOWNS.put(entry.getKey(), new Cooldown(session.console, session.center, 0));
                if (session.finalShot) {
                    ARMED_FINAL_SHOTS.remove(level.dimension());
                    beginSelfDestruct(level, session.center);
                }
                finished.add(entry.getKey());
            } else {
                entry.setValue(new Session(session.target, session.console, session.center,
                        session.ticks + 1, session.typeId, session.variants, session.finalShot));
            }
        }
        finished.forEach(ACTIVE::remove);
        tickCooldowns(level);
        tickSelfDestruct(level);
    }

    public static boolean isPortalSuppressed(ServerLevel level, Vec3 position) {
        boolean prime=!level.getEntitiesOfClass(RickPrimeEntity.class,
                new AABB(position, position).inflate(OmegaStructureBuilder.RADIUS * 2 + 10),
                entity -> entity.isAlive() && !entity.isClone()).isEmpty();
        boolean evil=!level.getEntitiesOfClass(EvilMortyEntity.class,
                new AABB(position,position).inflate(64),entity->entity.isAlive()).isEmpty();
        return prime||evil;
    }

    public static void armFinalShot(ServerLevel level, Vec3 center) {
        ARMED_FINAL_SHOTS.put(level.dimension(), center);
        level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(
                "RICK PRIME ELIMINADO - O DISPOSITIVO OMEGA ESTA ARMADO PARA UM DISPARO"), false);
    }

    private static void beginSelfDestruct(ServerLevel level, Vec3 center) {
        SELF_DESTRUCTS.put(level.dimension(), new SelfDestruct(center, 0));
        if (level.dimension().location().equals(com.jhonfx.portalgun.federation.UnmortrickenFacility.ID))
            com.jhonfx.portalgun.federation.UnmortrickenFacility.get(level).saveDestruction(center, 0);
        level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(
                "DISPARO OMEGA CONCLUIDO - AUTODESTRUICAO EM 30s"), false);
        sendScreenEffect(level, center, 30, 0.55f, true);
    }

    private static void tickSelfDestruct(ServerLevel level) {
        SelfDestruct destruct = SELF_DESTRUCTS.get(level.dimension());
        if (destruct == null) return;
        destruct.ticks++;
        int remaining = Math.max(0, 30 - destruct.ticks / 20);
        if (destruct.ticks % 20 == 0) {
            Component warning = Component.literal("OMEGA SELF-DESTRUCT: " + remaining + "s");
            for (ServerPlayer player : level.players())
                if (player.distanceToSqr(destruct.center) < 80 * 80) player.displayClientMessage(warning, true);
            level.playSound(null, destruct.center.x, destruct.center.y + 4, destruct.center.z,
                    destruct.ticks > 500 ? ModSounds.OMEGA_ARC.get() : SoundEvents.NOTE_BLOCK_HAT.value(),
                    SoundSource.BLOCKS, 2.0f, destruct.ticks > 500 ? 1.65f : 0.7f + destruct.ticks / 900f);
        }
        double radius = 3.0 + destruct.ticks / 75.0;
        for (int i = 0; i < 12; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            level.sendParticles(i % 4 == 0 ? OMEGA_WHITE : OMEGA_GREEN,
                    destruct.center.x + Math.cos(angle) * radius,
                    destruct.center.y + 1 + level.random.nextDouble() * 11,
                    destruct.center.z + Math.sin(angle) * radius, 1, 0.08, 0.08, 0.08, 0.02);
        }
        if (destruct.ticks > 480 && destruct.ticks % 12 == 0)
            level.explode(null, destruct.center.x + level.random.nextGaussian() * 9,
                    destruct.center.y + 2 + level.random.nextDouble() * 8,
                    destruct.center.z + level.random.nextGaussian() * 9,
                    2.4f, Level.ExplosionInteraction.NONE);
        if (destruct.ticks == 600) collapseFacility(level, destruct.center);
        if (destruct.ticks >= 600 && destruct.ticks <= 656)
            collapseSlice(level, destruct.center, destruct.ticks - 628);
        if (level.dimension().location().equals(com.jhonfx.portalgun.federation.UnmortrickenFacility.ID))
            com.jhonfx.portalgun.federation.UnmortrickenFacility.get(level).saveDestruction(destruct.center,
                    destruct.ticks >= 656 ? 657 : destruct.ticks);
        if (destruct.ticks >= 656) {
            SELF_DESTRUCTS.remove(level.dimension());
        }
    }

    private static void collapseFacility(ServerLevel level, Vec3 center) {
        BlockPos origin = BlockPos.containing(center);
        level.playSound(null, origin, ModSounds.OMEGA_ERASE.get(), SoundSource.BLOCKS, 5.0f, 0.52f);
        sendScreenEffect(level, center, 45, 1.35f, true);
        for (int shell = 0; shell < 5; shell++)
            level.explode(null, center.x, center.y + shell * 2, center.z,
                    5.0f + shell, Level.ExplosionInteraction.NONE);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y + 4, center.z,
                12, 8, 6, 8, 0.2);
    }

    private static void collapseSlice(ServerLevel level, Vec3 center, int x) {
        BlockPos origin = BlockPos.containing(center);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int radius = 28;
        for (int y = -7; y <= 25; y++) for (int z = -radius; z <= radius; z++) {
            double normalized = x * x + z * z + y * y * 1.8;
            if (normalized > radius * radius) continue;
            cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
            if (!level.getBlockState(cursor).is(net.minecraft.world.level.block.Blocks.BEDROCK))
                level.setBlock(cursor, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private static void emitEnergy(ServerLevel level, Vec3 center, int tick) {
        double progress = tick / (double) CHARGE_TICKS;
        Vec3 focus = center.add(0, 1.0, 0);

        // Dense rotating green sphere with white electrical details.
        double radius = 1.65 + Math.sin(tick * 0.12) * 0.12;
        for (int i = 0; i < 42; i++) {
            double y = 1.0 - (i / 41.0) * 2.0;
            double ringRadius = Math.sqrt(Math.max(0, 1.0 - y * y));
            double angle = i * 2.399963 + tick * (i % 2 == 0 ? 0.12 : -0.085);
            Vec3 point = focus.add(Math.cos(angle) * ringRadius * radius, y * radius,
                    Math.sin(angle) * ringRadius * radius);
            level.sendParticles(i % 7 == 0 ? OMEGA_WHITE : OMEGA_CYAN,
                    point.x, point.y, point.z, 1, 0.015, 0.015, 0.015, 0);
        }

        // Black pre-erasure silhouette with green/white edge sparks.
        if (tick > 82) {
            level.sendParticles(OMEGA_BLACK, focus.x, focus.y, focus.z, 18,
                    0.32, 0.9, 0.32, 0.005);
            level.sendParticles(OMEGA_WHITE, focus.x, focus.y, focus.z, 6,
                    0.38, 1.0, 0.38, 0.01);
        }

        // Three counter-rotating ceiling rings.
        for (int ring = 0; ring < 3; ring++) {
            double ringRadius = 2.6 + ring * 0.65;
            double tilt = (ring - 1) * 0.34;
            for (int i = 0; i < 20; i++) {
                double a = i * Math.PI * 2 / 20.0 + tick * (ring % 2 == 0 ? 0.08 : -0.065);
                double x = Math.cos(a) * ringRadius;
                double z = Math.sin(a) * ringRadius;
                double y = 7.0 + Math.sin(a) * tilt;
                level.sendParticles((i + ring) % 5 == 0 ? OMEGA_WHITE : OMEGA_CYAN,
                        center.x + x, center.y + y, center.z + z, 1, 0, 0, 0, 0);
            }
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Vec3 start = center.add(direction.getStepX() * 7, 4.5, direction.getStepZ() * 7);
            for (int i = 0; i <= 12; i++) {
                double t = i / 12.0;
                Vec3 point = start.lerp(focus, t);
                level.sendParticles(i % 4 == 0 ? OMEGA_WHITE : OMEGA_CYAN,
                        point.x, point.y, point.z, 1, 0.015, 0.015, 0.015, 0);
            }
        }

        // Multiverse holograms disappear one by one as erasure advances.
        int holograms = Math.max(0, 8 - (int) (progress * 8));
        for (int copy = 0; copy < holograms; copy++) {
            double angle = copy * Math.PI * 2 / 8.0 + tick * 0.025;
            Vec3 ghost = focus.add(Math.cos(angle) * 3.1, 0, Math.sin(angle) * 3.1);
            for (int y = -4; y <= 5; y++)
                level.sendParticles(OMEGA_CYAN, ghost.x, ghost.y + y * 0.16, ghost.z,
                        1, 0.12, 0.02, 0.12, 0);
        }

        // Last-second reality fracture: displaced copies of the room converge.
        if (tick > CHARGE_TICKS - 20) {
            for (int reality = -2; reality <= 2; reality++) {
                double displaced = reality * (CHARGE_TICKS - tick) * 0.025;
                for (int i = 0; i < 16; i++) {
                    double a = i * Math.PI * 2 / 16.0;
                    level.sendParticles(reality % 2 == 0 ? OMEGA_WHITE : OMEGA_CYAN,
                            center.x + Math.cos(a) * 8 + displaced, center.y + 3.5,
                            center.z + Math.sin(a) * 8 - displaced, 1, 0, 0, 0, 0);
                }
            }
        }
    }

    private static void showStatus(ServerLevel level, Session session) {
        int percent = Math.min(100, session.ticks * 100 / CHARGE_TICKS);
        Component status = Component.literal("VARIANTS FOUND: " + session.variants
                + "   |   ERASURE: " + percent + "%   |   OMEGA STATUS: ACTIVE");
        for (ServerPlayer player : level.players())
            if (player.distanceToSqr(session.center) <= 40 * 40) player.displayClientMessage(status, true);
    }

    private static int countExisting(ServerLevel origin, ResourceLocation typeId) {
        int count = 0;
        for (ServerLevel level : origin.getServer().getAllLevels())
            for (Entity entity : level.getAllEntities())
                if (typeId.equals(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()))) count++;
        return Math.max(1, count);
    }

    private static void eraseExisting(ServerLevel origin, ResourceLocation typeId, Vec3 center) {
        if (typeId == null) return;
        for (ServerLevel level : origin.getServer().getAllLevels()) {
            List<Entity> erased = new ArrayList<>();
            for (Entity entity : level.getAllEntities())
                if (!(entity instanceof Player)
                        && typeId.equals(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()))) erased.add(entity);
            for (Entity entity : erased) {
                if (level == origin && entity.distanceToSqr(center) < 48 * 48)
                    level.sendParticles(OMEGA_GREEN, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5,
                            entity.getZ(), 20, entity.getBbWidth() * 0.35, entity.getBbHeight() * 0.4,
                            entity.getBbWidth() * 0.35, 0.02);
                entity.discard();
            }
        }
    }

    private static void finalPulse(ServerLevel level, Vec3 center) {
        Vec3 focus = center.add(0, 1, 0);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, focus.x, focus.y, focus.z, 2, 0, 0, 0, 0);
        for (int radius = 1; radius <= 12; radius++) {
            for (int i = 0; i < 32; i++) {
                double a = i * Math.PI * 2 / 32.0;
                level.sendParticles(radius % 3 == 0 ? OMEGA_WHITE : OMEGA_GREEN,
                        focus.x + Math.cos(a) * radius, focus.y, focus.z + Math.sin(a) * radius,
                        1, 0, 0.02, 0, 0);
            }
        }
        // 0.3-second afterimage.
        level.sendParticles(OMEGA_WHITE, focus.x, focus.y, focus.z, 45, 0.35, 0.9, 0.35, 0.003);
    }

    private static void sendScreenEffect(ServerLevel level, Vec3 center, int ticks, float intensity, boolean pulse) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(center) <= 40 * 40)
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new OmegaEffectPacket(ticks, intensity, pulse));
        }
    }

    private static void omegaFailure(ServerLevel level, DeviceKey key, Session session) {
        Entity raw=level.getEntity(session.target);
        if(raw instanceof LivingEntity target)restoreHeld(target);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, session.center.x, session.center.y + 2,
                session.center.z, 120, 6, 4, 6, 0.18);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, session.center.x, session.center.y + 1,
                session.center.z, 45, 2, 2, 2, 0.05);
        level.playSound(null, session.center.x, session.center.y, session.center.z,
                ModSounds.OMEGA_FAILURE.get(), SoundSource.BLOCKS, 3.0f, 1.0f);
        level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("OMEGA FAILURE — TARGET LOST"), false);
        COOLDOWNS.put(key, new Cooldown(session.console, session.center, 0));
    }

    private static void tickCooldowns(ServerLevel level) {
        List<DeviceKey> done = new ArrayList<>();
        for (Map.Entry<DeviceKey, Cooldown> entry : COOLDOWNS.entrySet()) {
            if (!entry.getKey().dimension.equals(level.dimension())) continue;
            Cooldown cooldown = entry.getValue();
            level.sendParticles(cooldown.ticks % 3 == 0 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.SMOKE,
                    cooldown.center.x, cooldown.center.y + 1.0, cooldown.center.z,
                    4, 1.4, 0.8, 1.4, 0.02);
            if (cooldown.ticks == 1) level.playSound(null, cooldown.center.x, cooldown.center.y,
                    cooldown.center.z, ModSounds.OMEGA_COOLDOWN.get(), SoundSource.BLOCKS, 2.2f, 1.0f);
            if (cooldown.ticks >= 60) {
                setMachineActive(level, cooldown.center, false);
                finishConsole(level, cooldown.console);
                done.add(entry.getKey());
            } else entry.setValue(new Cooldown(cooldown.console, cooldown.center, cooldown.ticks + 1));
        }
        done.forEach(COOLDOWNS::remove);
    }

    private static void finishConsole(ServerLevel level, BlockPos console) {
        BlockState state = level.getBlockState(console);
        if (state.is(ModBlocks.OMEGA_DEVICE.get()))
            level.setBlock(console, state.setValue(OmegaDeviceBlock.ACTIVE, false), 3);
    }

    private static void setMachineActive(ServerLevel level, Vec3 center, boolean active) {
        BlockPos origin = BlockPos.containing(center);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        // Official Omega components occupy only the reactor's six lower layers.
        for (int x = -28; x <= 28; x++) for (int y = -5; y <= 0; y++) for (int z = -28; z <= 28; z++) {
            cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
            BlockState state = level.getBlockState(cursor);
            if (state.hasProperty(OmegaDeviceBlock.ACTIVE) && (state.is(ModBlocks.OMEGA_DEVICE.get())
                    || state.is(ModBlocks.OMEGA_CORE.get()) || state.is(ModBlocks.OMEGA_EMITTER.get())
                    || state.is(ModBlocks.OMEGA_RING.get())))
                level.setBlock(cursor, state.setValue(OmegaDeviceBlock.ACTIVE, active), 2);
        }
    }

    private static void markHeld(LivingEntity target){
        var tag=target.getPersistentData();
        if(tag.getBoolean("PortalGunOmegaHeld"))return;
        tag.putBoolean("PortalGunOmegaHeld",true);
        tag.putBoolean("PortalGunOmegaWasInvulnerable",target.isInvulnerable());
        if(target instanceof Mob mob)tag.putBoolean("PortalGunOmegaWasNoAi",mob.isNoAi());
    }

    private static void restoreHeld(LivingEntity target){
        var tag=target.getPersistentData();
        if(!tag.getBoolean("PortalGunOmegaHeld"))return;
        target.setInvulnerable(tag.getBoolean("PortalGunOmegaWasInvulnerable"));
        if(target instanceof Mob mob)mob.setNoAi(tag.getBoolean("PortalGunOmegaWasNoAi"));
        tag.remove("PortalGunOmegaHeld");
        tag.remove("PortalGunOmegaWasInvulnerable");
        tag.remove("PortalGunOmegaWasNoAi");
    }

    private static boolean isActiveTarget(UUID id){
        for(Session session:ACTIVE.values())if(session.target.equals(id))return true;
        return false;
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getEntity() instanceof Player) return;
        if(event.getEntity() instanceof LivingEntity living
                &&living.getPersistentData().getBoolean("PortalGunOmegaHeld")&&!isActiveTarget(living.getUUID()))
            restoreHeld(living);
        if (event.getEntity() instanceof net.minecraft.world.entity.monster.Monster
                && !(event.getEntity() instanceof RickPrimeEntity)
                && !(event.getEntity() instanceof DianeRobotEntity)
                && isPortalSuppressed(level, event.getEntity().position())) {
            event.setCanceled(true);
            return;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());
        if (id != null && OmegaBanSavedData.get(level).contains(id)) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event){
        for(Map.Entry<DeviceKey,Session> entry:new ArrayList<>(ACTIVE.entrySet())){
            ServerLevel level=event.getServer().getLevel(entry.getKey().dimension);
            if(level==null)continue;
            Entity raw=level.getEntity(entry.getValue().target);
            if(raw instanceof LivingEntity target)restoreHeld(target);
            finishConsole(level,entry.getValue().console);
        }
        ACTIVE.clear();
        COOLDOWNS.clear();
        ARMED_FINAL_SHOTS.clear();
        SELF_DESTRUCTS.clear();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        OmegaCommands.register(event.getDispatcher());
    }

    private record DeviceKey(ResourceKey<Level> dimension, long pos) {}
    private record Session(UUID target, BlockPos console, Vec3 center, int ticks,
                           ResourceLocation typeId, int variants, boolean finalShot) {}
    private record Cooldown(BlockPos console, Vec3 center, int ticks) {}
    private static final class SelfDestruct {
        final Vec3 center;
        int ticks;
        SelfDestruct(Vec3 center, int ticks) { this.center = center; this.ticks = ticks; }
    }
}

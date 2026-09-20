package com.jhonfx.portalgun.federation;

import com.jhonfx.portalgun.entity.RickPrimeEntity;
import com.jhonfx.portalgun.entity.CitadelNpcEntity;
import com.jhonfx.portalgun.entity.FederationDroneEntity;
import com.jhonfx.portalgun.init.ModBlocks;
import com.jhonfx.portalgun.init.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Versioned, resumable construction confined to its own empty dimension. */
public final class UnmortrickenFacility extends SavedData {
    private static final int CURRENT_LAYOUT_VERSION = 4;
    public static final ResourceLocation ID = new ResourceLocation("portalgun", "unmortricken");
    public static final BlockPos ARRIVAL = new BlockPos(0, 97, 148);
    public static final BlockPos ARENA = new BlockPos(0, 97, 0);
    private static final int MIN_X = -56, MIN_Z = -56;
    private static final int WIDTH = 113, DEPTH = 217;
    private int column;
    private int layoutVersion = CURRENT_LAYOUT_VERSION;
    private boolean rebuildPurge;
    private boolean requested, bossSpawned, alliesSpawned, defeated;
    private int destructTicks = -1;
    private int epilogueTicks = -1;
    private net.minecraft.world.phys.Vec3 destructCenter = net.minecraft.world.phys.Vec3.atBottomCenterOf(ARENA.above(2));

    public static UnmortrickenFacility get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(UnmortrickenFacility::load,
                UnmortrickenFacility::new, "portalgun_unmortricken");
    }

    public static UnmortrickenFacility load(CompoundTag tag) {
        UnmortrickenFacility state = new UnmortrickenFacility();
        state.layoutVersion = tag.contains("Version") ? tag.getInt("Version") : 1;
        state.column = Math.max(0, Math.min(WIDTH * DEPTH, tag.getInt("Column")));
        state.requested = tag.getBoolean("Requested");
        state.bossSpawned = tag.getBoolean("BossSpawned");
        state.alliesSpawned = tag.getBoolean("AlliesSpawned");
        state.defeated = tag.getBoolean("RickPrimeDefeated");
        state.rebuildPurge = tag.getBoolean("RebuildPurge");
        // Dedicated boss dimension: rebuilding it is safe and required to
        // migrate old preview worlds to the new Prime plating and layout.
        if (state.layoutVersion < CURRENT_LAYOUT_VERSION) {
            state.column = 0;
            state.layoutVersion = CURRENT_LAYOUT_VERSION;
            state.rebuildPurge = true;
            state.bossSpawned = false;
            state.alliesSpawned = false;
        }
        state.destructTicks = tag.contains("DestructTicks") ? tag.getInt("DestructTicks") : -1;
        state.epilogueTicks = tag.contains("EpilogueTicks") ? tag.getInt("EpilogueTicks") : -1;
        if (tag.contains("DestructX")) state.destructCenter = new net.minecraft.world.phys.Vec3(
                tag.getDouble("DestructX"), tag.getDouble("DestructY"), tag.getDouble("DestructZ"));
        return state;
    }

    @Override public CompoundTag save(CompoundTag tag) {
        tag.putInt("Version", CURRENT_LAYOUT_VERSION);
        tag.putInt("Column", column);
        tag.putBoolean("RebuildPurge", rebuildPurge);
        tag.putBoolean("Requested", requested);
        tag.putBoolean("BossSpawned", bossSpawned);
        tag.putBoolean("AlliesSpawned", alliesSpawned);
        tag.putBoolean("RickPrimeDefeated", defeated);
        tag.putInt("DestructTicks", destructTicks);
        tag.putInt("EpilogueTicks", epilogueTicks);
        tag.putDouble("DestructX", destructCenter.x); tag.putDouble("DestructY", destructCenter.y); tag.putDouble("DestructZ", destructCenter.z);
        return tag;
    }
    public boolean isDefeated() { return defeated; }
    public int destructTicks() { return destructTicks; }
    public net.minecraft.world.phys.Vec3 destructCenter() { return destructCenter; }
    public void saveDestruction(net.minecraft.world.phys.Vec3 center, int ticks) {
        destructCenter = center; destructTicks = ticks; setDirty();
    }

    public static boolean ensureBuilt(ServerLevel level) {
        if (!level.dimension().location().equals(ID)) return false;
        UnmortrickenFacility state = get(level);
        if (!state.requested) {
            state.requested = true;
            // Safe landing pad exists before either command or portal arrivals.
            for (int x = -5; x <= 5; x++) for (int z = 143; z <= 153; z++)
                level.setBlock(new BlockPos(x, 96, z), Blocks.SEA_LANTERN.defaultBlockState(), 2);
            state.setDirty();
        }
        return state.column == WIDTH * DEPTH;
    }

    /** Returns a guaranteed floor-level arrival, never the facility roof. */
    public static BlockPos prepareArrival(ServerLevel level) {
        BlockPos feet = ARRIVAL;
        level.getChunkAt(feet);
        level.setBlock(feet.below(), Blocks.SEA_LANTERN.defaultBlockState(), 2);
        level.setBlock(feet, Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(feet.above(), Blocks.AIR.defaultBlockState(), 2);
        return feet;
    }

    public static void markDefeated(ServerLevel level) {
        if (!level.dimension().location().equals(ID)) return;
        UnmortrickenFacility state = get(level);
        if (state.defeated) return;
        state.defeated = true;
        state.epilogueTicks = 0;
        state.setDirty();
        // The encounter is over: remove every temporary enemy so a clone or
        // Diane cannot keep the arena locked during the epilogue.
        for (var entity : level.getEntities((net.minecraft.world.entity.Entity)null,
                new net.minecraft.world.phys.AABB(ARENA).inflate(64), entity ->
                        entity instanceof RickPrimeEntity && ((RickPrimeEntity)entity).isClone()
                                || entity instanceof com.jhonfx.portalgun.entity.DianeRobotEntity
                                || entity.getPersistentData().contains("UnmortrickenEncounter"))) entity.discard();
        for (var player : level.players()) {
            player.getPersistentData().putBoolean("RickPrimeDefeated", true);
            CompoundTag arc = player.getPersistentData().getCompound("PortalGunP0Arc");
            arc.putInt("Stage", Math.max(14, arc.getInt("Stage")));
            player.getPersistentData().put("PortalGunP0Arc", arc);
            player.getPersistentData().putInt("PortalGunUnmortrickenSector", 6);
            grantCompletionReward(player);
            player.displayClientMessage(Component.literal("§aRICK PRIME ELIMINADO§r — iniciando epílogo de UNMORTRICKEN."), false);
        }
    }

    @SubscribeEvent public void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
                || !level.dimension().location().equals(ID)) return;
        UnmortrickenFacility state = get(level);
        if (!state.requested) return;
        if (state.column < WIDTH * DEPTH) {
            if (state.rebuildPurge) {
                for (var entity : level.getEntities((net.minecraft.world.entity.Entity)null,
                        new net.minecraft.world.phys.AABB(-72, 64, -72, 72, 150, 176),
                        e -> !(e instanceof net.minecraft.world.entity.player.Player))) entity.discard();
                state.rebuildPurge = false;
            }
            // Incremental rebuild keeps the server responsive while replacing
            // the entire old preview facility.
            int end = Math.min(WIDTH * DEPTH, state.column + 48);
            while (state.column < end) {
                buildColumn(level, state.column % WIDTH + MIN_X, state.column / WIDTH + MIN_Z);
                state.column++;
            }
            state.setDirty();
            return;
        }
        state.tickEpilogue(level);
        if (level.getGameTime() % 10 == 0) state.tickSectors(level);
        state.rescueFallenPlayers(level);
        if (state.defeated && level.getGameTime() % 100 == 0) {
            for (var player : level.players()) grantCompletionReward(player);
            boolean exitExists = !level.getEntitiesOfClass(com.jhonfx.portalgun.entity.PortalEntity.class,
                    new net.minecraft.world.phys.AABB(ARENA).inflate(40), portal ->
                            portal.isAlive() && portal.getPersistentData().getBoolean("UnmortrickenExit")).isEmpty();
            if (!exitExists) openEvacuationPortal(level);
        }
        if (state.bossSpawned && !state.defeated) {
            assistAllies(level);
            if (level.getGameTime() % 100 == 0) {
                boolean primeExists = !level.getEntitiesOfClass(RickPrimeEntity.class,
                        new net.minecraft.world.phys.AABB(ARENA).inflate(64), p -> p.isAlive() && !p.isClone()).isEmpty();
                if (!primeExists) {
                    // Recovers worlds where a command/mod removed the boss without completing the encounter.
                    state.bossSpawned = false;
                    state.setDirty();
                } else {
                    state.spawnAllies(level);
                }
            }
        }
        if (state.bossSpawned || state.defeated || level.getGameTime() % 20 != 0) return;
        boolean entered = level.players().stream().anyMatch(p -> !p.isSpectator()
                && p.blockPosition().distSqr(ARENA) < 28 * 28
                && (p.isCreative() || p.getPersistentData().getBoolean("PortalGunUnmortrickenUnlocked")));
        if (!entered) return;
        RickPrimeEntity prime = ModEntityTypes.RICK_PRIME.get().create(level);
        if (prime == null) return;
        prime.moveTo(0.5, 115, -10.5, 0, 0);
        prime.configureArena(ARENA);
        prime.setPersistenceRequired();
        if (level.addFreshEntity(prime)) {
            state.bossSpawned = true;
            state.spawnAllies(level);
            state.setDirty();
        }
    }

    /** Six persistent campaign sectors with two real combat locks before the boss. */
    private void tickSectors(ServerLevel level) {
        for (var player : level.players()) {
            if (player.isSpectator() || Math.abs(player.getX()) > 56 || player.getZ() < -52 || player.getZ() > 164)
                continue;
            CompoundTag data = player.getPersistentData();
            int gate = data.getInt("PortalGunUnmortrickenGate");
            if (gate > 0) {
                boolean enemies = !level.getEntities((net.minecraft.world.entity.Entity) null,
                        new net.minecraft.world.phys.AABB(-46, 94, 48, 46, 126, 124), entity ->
                                entity.isAlive()
                                        && player.getUUID().toString().equals(entity.getPersistentData().getString("UnmortrickenOwner"))
                                        && entity.getPersistentData().getInt("UnmortrickenEncounter") == gate).isEmpty();
                if (enemies) {
                    int limit = gate == 2 ? 92 : 62;
                    if (player.getZ() < limit) player.teleportTo(level, player.getX(), player.getY(),
                            limit + 1.5, player.getYRot(), player.getXRot());
                    continue;
                }
                data.remove("PortalGunUnmortrickenGate");
                player.displayClientMessage(Component.literal(gate == 2
                        ? "§aSETOR LIBERADO§r — segurança Prime neutralizada."
                        : "§aSETOR LIBERADO§r — todas as iscas do Rick Prime foram eliminadas."), false);
            }

            int sector = sectorAt(player.getZ());
            int previous = data.getInt("PortalGunUnmortrickenSector");
            if (defeated) sector = 6;
            if (sector <= previous) continue;
            data.putInt("PortalGunUnmortrickenSector", sector);
            announceSector(player, sector);
            if (sector == 2) {
                spawnSecurityEncounter(level, player);
                data.putInt("PortalGunUnmortrickenGate", 2);
            } else if (sector == 3) {
                spawnCloneEncounter(level, player);
                data.putInt("PortalGunUnmortrickenGate", 3);
            } else if (sector == 4) {
                level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT,
                        net.minecraft.sounds.SoundSource.AMBIENT, 2.5f, .55f);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                        0.5, 100, 44.5, 100, 12, 3, 12, .05);
            }
        }
    }

    private static int sectorAt(double z) {
        if (z > 128) return 1; // orbital arrival and decontamination
        if (z > 96) return 2;  // security gauntlet
        if (z > 64) return 3;  // clone/decoy laboratory
        if (z > 38) return 4;  // Omega observation sector
        return 5;              // Prime arena
    }

    private static void announceSector(net.minecraft.server.level.ServerPlayer player, int sector) {
        String[] names = {"", "I — ANCORAGEM PRIME", "II — SEGURANÇA AUTÔNOMA",
                "III — CÂMARAS DE VARIANTES", "IV — OBSERVATÓRIO ÔMEGA",
                "V — NEXO UNMORTRICKEN", "VI — EPÍLOGO"};
        String[] tasks = {"", "Avance pelo corredor de descontaminação.",
                "Destrua os drones para liberar a porta seguinte.",
                "Elimine as iscas do Rick Prime.",
                "Atravesse o núcleo energizado e alcance a plataforma.",
                "Sobreviva a Rick Prime, seus clones e ao Diane-Bot.",
                "A singularidade Prime foi obtida. O Dispositivo Ômega agora responde a você."};
        player.displayClientMessage(Component.literal("§3[ UNMORTRICKEN / SETOR " + names[sector] + " ]§r\n§f" + tasks[sector]), false);
    }

    private static void spawnSecurityEncounter(ServerLevel level, net.minecraft.server.level.ServerPlayer player) {
        int[][] positions = {{-12, 104}, {12, 108}, {0, 114}, {-7, 99}};
        for (int[] pos : positions) {
            FederationDroneEntity drone = ModEntityTypes.FEDERATION_DRONE.get().create(level);
            if (drone == null) continue;
            drone.moveTo(pos[0] + .5, 101, pos[1] + .5, 0, 0);
            drone.setPersistenceRequired();
            drone.setTarget(player);
            drone.getPersistentData().putString("UnmortrickenOwner", player.getUUID().toString());
            drone.getPersistentData().putInt("UnmortrickenEncounter", 2);
            level.addFreshEntity(drone);
        }
    }

    private static void spawnCloneEncounter(ServerLevel level, net.minecraft.server.level.ServerPlayer player) {
        int[][] positions = {{-18, 82}, {18, 82}, {-8, 70}, {8, 70}};
        for (int[] pos : positions) {
            RickPrimeEntity clone = ModEntityTypes.RICK_PRIME.get().create(level);
            if (clone == null) continue;
            clone.moveTo(pos[0] + .5, 97, pos[1] + .5, 180, 0);
            clone.setClone(true);
            clone.setPersistenceRequired();
            clone.setTarget(player);
            clone.getPersistentData().putString("UnmortrickenOwner", player.getUUID().toString());
            clone.getPersistentData().putInt("UnmortrickenEncounter", 3);
            level.addFreshEntity(clone);
        }
    }

    private void tickEpilogue(ServerLevel level) {
        if (epilogueTicks < 0) return;
        epilogueTicks++;
        if (epilogueTicks == 20) broadcast(level, "§7[ C-137 ]§r Acabou. Pegue o núcleo antes que este lugar perceba que ele morreu.");
        if (epilogueTicks == 60) broadcast(level, "§e[ EVIL MORTY ]§r A máquina está destravada. Um disparo — depois ela apaga os próprios rastros.");
        if (epilogueTicks == 100) {
            level.playSound(null, ARENA, net.minecraft.sounds.SoundEvents.END_PORTAL_SPAWN,
                    net.minecraft.sounds.SoundSource.AMBIENT, 3f, .65f);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    ARENA.getX() + .5, ARENA.getY() + 3, ARENA.getZ() + .5, 180, 8, 4, 8, .04);
            openEvacuationPortal(level);
        }
        if (epilogueTicks == 140)
            broadcast(level, "§aUNMORTRICKEN CONCLUÍDA§r — o Dispositivo Ômega está liberado. Atravesse o portal verde para evacuar.");
        if (epilogueTicks >= 160) epilogueTicks = -2;
        setDirty();
    }

    /** A persistent real portal pair finishes the episode and works for every survivor. */
    private static void openEvacuationPortal(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        if (overworld == null) return;
        BlockPos raw = overworld.getSharedSpawnPos();
        BlockPos exitFeet = overworld.getHeightmapPos(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, raw).above();
        overworld.getChunkAt(exitFeet);
        if (!overworld.getBlockState(exitFeet.below()).isSolidRender(overworld, exitFeet.below()))
            overworld.setBlock(exitFeet.below(), Blocks.SMOOTH_STONE.defaultBlockState(), 2);
        overworld.setBlock(exitFeet, Blocks.AIR.defaultBlockState(), 2);
        overworld.setBlock(exitFeet.above(), Blocks.AIR.defaultBlockState(), 2);

        com.jhonfx.portalgun.entity.PortalEntity local = ModEntityTypes.PORTAL.get().create(level);
        com.jhonfx.portalgun.entity.PortalEntity remote = ModEntityTypes.PORTAL.get().create(overworld);
        if (local == null || remote == null) return;
        local.setPos(ARENA.getX() + .5, ARENA.getY() + 1.8, ARENA.getZ() + 25.5);
        local.setPortalFacing(net.minecraft.core.Direction.SOUTH);
        remote.setPos(exitFeet.getX() + .5, exitFeet.getY() + 1.6, exitFeet.getZ() + .5);
        remote.setPortalFacing(net.minecraft.core.Direction.NORTH);
        for (var portal : new com.jhonfx.portalgun.entity.PortalEntity[]{local, remote}) {
            portal.setColor(com.jhonfx.portalgun.entity.PortalColor.GREEN);
            portal.setPortalScale(1.0f);
            portal.setSafeMode(true);
            portal.setLifetimeTicks(20 * 60 * 10);
            portal.getPersistentData().putBoolean("UnmortrickenExit", true);
        }
        level.addFreshEntity(local);
        overworld.addFreshEntity(remote);
        local.setLinkedPortal(remote);
        remote.setLinkedPortal(local);
        level.playSound(null, local.blockPosition(), com.jhonfx.portalgun.init.ModSounds.PORTAL_SPAWN.get(),
                net.minecraft.sounds.SoundSource.AMBIENT, 2.4f, .8f);
    }

    private static void grantCompletionReward(net.minecraft.server.level.ServerPlayer player) {
        if (player.getPersistentData().getBoolean("PortalGunUnmortrickenReward")) return;
        net.minecraft.world.item.ItemStack reward = new net.minecraft.world.item.ItemStack(
                com.jhonfx.portalgun.init.ModItems.PRIME_SINGULARITY_CORE.get());
        if (!player.getInventory().add(reward)) player.drop(reward, false);
        player.getPersistentData().putBoolean("PortalGunUnmortrickenReward", true);
        player.displayClientMessage(Component.literal("§dNúcleo de Singularidade Prime adquirido."), false);
    }

    /** Keeps a missed jump from destroying a several-minute boss run. */
    private void rescueFallenPlayers(ServerLevel level) {
        for (var player : level.players()) {
            if (player.isSpectator() || player.getY() >= 72) continue;
            int sector = Math.max(1, player.getPersistentData().getInt("PortalGunUnmortrickenSector"));
            BlockPos checkpoint = switch (sector) {
                case 1 -> ARRIVAL;
                case 2 -> new BlockPos(0, 97, 122);
                case 3 -> new BlockPos(0, 97, 94);
                case 4 -> new BlockPos(0, 97, 62);
                default -> ARENA.offset(0, 1, 30);
            };
            level.getChunkAt(checkpoint);
            level.setBlock(checkpoint.below(), ModBlocks.PRIME_LAIR_PANEL.get().defaultBlockState(), 2);
            level.setBlock(checkpoint, Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(checkpoint.above(), Blocks.AIR.defaultBlockState(), 2);
            player.fallDistance = 0;
            player.teleportTo(level, checkpoint.getX() + .5, checkpoint.getY(), checkpoint.getZ() + .5,
                    player.getYRot(), player.getXRot());
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 80, 4, false, false));
            player.displayClientMessage(Component.literal("§7Sistema Prime restaurou você no último setor."), true);
        }
    }

    private static void broadcast(ServerLevel level, String message) {
        for (var player : level.players()) player.displayClientMessage(Component.literal(message), false);
    }

    private void spawnAllies(ServerLevel level) {
        if (!hasAlly(level, "C137"))
            spawnAlly(level, ARENA.offset(-12, 1, 8), CitadelNpcEntity.Variant.RICK, "Rick C-137", "C137");
        // Migrate the old preview stand-in, which used Rick's geometry.
        for (CitadelNpcEntity npc : level.getEntitiesOfClass(CitadelNpcEntity.class,
                new net.minecraft.world.phys.AABB(ARENA).inflate(64),
                n -> "EvilMorty".equals(n.getPersistentData().getString("PortalGunPrimeAlly")))) npc.discard();
        if (!hasAlly(level, "EvilMorty")) spawnEvilMortyAlly(level, ARENA.offset(12, 1, 8));
        alliesSpawned = true;
        setDirty();
    }

    private static boolean hasAlly(ServerLevel level, String role) {
        return !level.getEntities((net.minecraft.world.entity.Entity)null,
                new net.minecraft.world.phys.AABB(ARENA).inflate(64),
                entity -> entity.isAlive() && role.equals(entity.getPersistentData().getString("PortalGunPrimeAlly"))).isEmpty();
    }

    private static void spawnEvilMortyAlly(ServerLevel level, BlockPos pos) {
        com.jhonfx.portalgun.entity.EvilMortyEntity ally = ModEntityTypes.EVIL_MORTY_ALLY.get().create(level);
        if (ally == null) return;
        ally.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 180, 0);
        ally.setStoryAlly(true);
        ally.setPersistenceRequired();
        ally.getPersistentData().putString("PortalGunPrimeAlly", "EvilMorty");
        level.addFreshEntity(ally);
    }

    private static void spawnAlly(ServerLevel level, BlockPos pos, CitadelNpcEntity.Variant variant,
                                  String name, String role) {
        CitadelNpcEntity ally = ModEntityTypes.CITADEL_CITIZEN.get().create(level);
        if (ally == null) return;
        ally.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 180, 0);
        ally.setVariant(variant);
        ally.setCustomName(Component.literal(name));
        ally.setCustomNameVisible(true);
        ally.setPersistenceRequired();
        ally.getPersistentData().putString("PortalGunPrimeAlly", role);
        level.addFreshEntity(ally);
    }

    private static void assistAllies(ServerLevel level) {
        if (level.getGameTime() % 35 != 0) return;
        RickPrimeEntity prime = level.getEntitiesOfClass(RickPrimeEntity.class,
                new net.minecraft.world.phys.AABB(ARENA).inflate(45), p -> p.isAlive() && !p.isClone())
                .stream().findFirst().orElse(null);
        if (prime == null || prime.getHealth() <= 1.5f) return; // the player owns the final hit
        for (CitadelNpcEntity ally : level.getEntitiesOfClass(CitadelNpcEntity.class,
                new net.minecraft.world.phys.AABB(ARENA).inflate(45),
                n -> n.getPersistentData().contains("PortalGunPrimeAlly"))) {
            ally.getLookControl().setLookAt(prime, 30, 30);
            net.minecraft.world.phys.Vec3 from = ally.getEyePosition();
            net.minecraft.world.phys.Vec3 delta = prime.getEyePosition().subtract(from);
            for (int i = 0; i <= 12; i++) {
                net.minecraft.world.phys.Vec3 point = from.add(delta.scale(i / 12d));
                level.sendParticles(ally.getPersistentData().getString("PortalGunPrimeAlly").equals("EvilMorty")
                                ? net.minecraft.core.particles.ParticleTypes.WAX_ON
                                : net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                        point.x, point.y, point.z, 1, 0, 0, 0, 0);
            }
            prime.hurt(level.damageSources().indirectMagic(ally, ally), 1.5f);
        }
        for (com.jhonfx.portalgun.entity.EvilMortyEntity ally : level.getEntitiesOfClass(
                com.jhonfx.portalgun.entity.EvilMortyEntity.class,
                new net.minecraft.world.phys.AABB(ARENA).inflate(45),
                e -> e.isStoryAlly() && "EvilMorty".equals(e.getPersistentData().getString("PortalGunPrimeAlly")))) {
            ally.getLookControl().setLookAt(prime, 30, 30);
            net.minecraft.world.phys.Vec3 from = ally.getEyePosition();
            net.minecraft.world.phys.Vec3 delta = prime.getEyePosition().subtract(from);
            for (int i = 0; i <= 12; i++) {
                net.minecraft.world.phys.Vec3 point = from.add(delta.scale(i / 12d));
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.WAX_ON,
                        point.x, point.y, point.z, 1, 0, 0, 0, 0);
            }
            prime.hurt(level.damageSources().indirectMagic(ally, ally), 1.5f);
        }
    }

    private static void buildColumn(ServerLevel level, int x, int z) {
        // This is a dedicated encounter dimension, so version migrations may
        // safely erase the previous procedural base column-by-column.
        for (int y=72;y<=140;y++) {
            BlockPos old=new BlockPos(x,y,z);
            if(!level.getBlockState(old).isAir()) level.setBlock(old,Blocks.AIR.defaultBlockState(),2);
        }
        if(!insideV3(x,z))return;

        BlockState panel=ModBlocks.PRIME_LAIR_PANEL.get().defaultBlockState();
        BlockState trim=ModBlocks.OMEGA_LAB_PANEL.get().defaultBlockState();
        BlockState glass=ModBlocks.OMEGA_ENERGY_GLASS.get().defaultBlockState();
        BlockState alarm=ModBlocks.OMEGA_ALARM_PANEL.get().defaultBlockState();
        int r2=x*x+z*z;
        boolean arena=r2<=52*52;
        boolean arrival=x*x+(z-148)*(z-148)<=20*20;
        boolean edge=!insideV3(x+1,z)||!insideV3(x-1,z)||!insideV3(x,z+1)||!insideV3(x,z-1);
        int top=arena?124+(int)Math.round(10*Math.sqrt(Math.max(0,1-r2/(52d*52d)))):arrival?112:108;
        boolean floorLight=Math.floorMod(x*7+z*11,41)==0;

        level.setBlock(new BlockPos(x,95,z),trim,2);
        level.setBlock(new BlockPos(x,96,z),floorLight?Blocks.SEA_LANTERN.defaultBlockState():panel,2);
        for(int y=97;y<=top;y++){
            BlockState state=null;
            if(edge)state=(y%7==0)?glass:panel;
            if(y==top)state=(Math.floorMod(x+z,5)==0)?glass:panel;
            if(state!=null)level.setBlock(new BlockPos(x,y,z),state,2);
        }

        // Reinforced ribs and luminous sector bulkheads, leaving a 7-wide door.
        if((z==128||z==96||z==64||z==38)&&Math.abs(x)<=31&&Math.abs(x)>3)
            for(int y=97;y<=105;y++)level.setBlock(new BlockPos(x,y,z),y==101?glass:panel,2);
        if((z>=99&&z<=120)&&Math.abs(x)==19&&Math.floorMod(z,8)<4)
            for(int y=97;y<=102;y++)level.setBlock(new BlockPos(x,y,z),y==100?alarm:trim,2);

        // Clone pods: large glass cylinders on both sides of the traversal route.
        if(z>=69&&z<=91&&((Math.abs(x)>=17&&Math.abs(x)<=22))){
            int local=Math.floorMod(z-69,11);
            if(local<=5&&(Math.abs(x)==17||Math.abs(x)==22||local==0||local==5))
                for(int y=97;y<=104;y++)level.setBlock(new BlockPos(x,y,z),y==97||y==104?trim:glass,2);
        }

        // Omega observatory: suspended catwalk around the active conduit.
        if(z>=42&&z<=60&&Math.abs(x)<=25){
            int dx=x,dz=z-51,ring=dx*dx+dz*dz;
            if(ring>=11*11&&ring<=16*16)level.setBlock(new BlockPos(x,102,z),ring>15*15?glass:trim,2);
            if((Math.abs(x)<=2||Math.abs(z-51)<=2)&&ring<=16*16)
                level.setBlock(new BlockPos(x,102,z),panel,2);
        }

        if(arena){
            // Two walkable concentric combat decks with cardinal bridges.
            if((r2>=20*20&&r2<=29*29)||(Math.min(Math.abs(x),Math.abs(z))<=3&&r2<=29*29))
                level.setBlock(new BlockPos(x,106,z),r2>28*28?glass:panel,2);
            if((r2>=37*37&&r2<=46*46)||(Math.min(Math.abs(x),Math.abs(z))<=3&&r2<=46*46))
                level.setBlock(new BlockPos(x,116,z),r2>45*45?glass:trim,2);

            // Four broad side platforms used by clones and the Diane phase.
            int[][] platforms={{24,24},{-24,24},{24,-24},{-24,-24}};
            for(int[] p:platforms){int dx=x-p[0],dz=z-p[1];if(dx*dx+dz*dz<=8*8)
                level.setBlock(new BlockPos(x,101,z),(dx*dx+dz*dz>7*7)?glass:panel,2);}

            // Real ramps: one block up per three blocks travelled.
            if(Math.abs(x)>=31&&Math.abs(x)<=36&&z>=-24&&z<=6){
                int rise=Math.min(10,Math.max(0,(6-z)/3));
                level.setBlock(new BlockPos(x,97+rise,z),Math.floorMod(z,6)==0?glass:trim,2);
            }
            if(Math.abs(z)>=39&&Math.abs(z)<=44&&x>=-30&&x<=0){
                int rise=Math.min(19,Math.max(0,(x+30)*19/30));
                level.setBlock(new BlockPos(x,97+rise,z),Math.floorMod(x,6)==0?glass:panel,2);
            }

            // Hanging mechanical ring above the boss platform.
            int ring=(x*x+z*z);
            if(ring>=8*8&&ring<=10*10)level.setBlock(new BlockPos(x,126,z),
                    Math.floorMod(x-z,4)==0?ModBlocks.OMEGA_RING.get().defaultBlockState():trim,2);
        }

        // Central containment dais and reactor visible through glass.
        if(r2<=7*7){
            level.setBlock(new BlockPos(x,97,z),r2>6*6?Blocks.CRYING_OBSIDIAN.defaultBlockState():Blocks.SMOOTH_QUARTZ.defaultBlockState(),2);
            if(r2<=3*3)level.setBlock(new BlockPos(x,97,z),Blocks.LIME_STAINED_GLASS.defaultBlockState(),2);
        }
        if(x==0&&z==0)level.setBlock(new BlockPos(0,94,0),ModBlocks.OMEGA_CORE.get().defaultBlockState(),2);
        if((Math.abs(x)==18&&z==0)||(x==0&&Math.abs(z)==18)){
            level.setBlock(new BlockPos(x,97,z),ModBlocks.OMEGA_EMITTER.get().defaultBlockState(),2);
            level.setBlock(new BlockPos(x,98,z),glass,2);
        }
        if(x==0&&z==31)level.setBlock(new BlockPos(x,97,z),ModBlocks.OMEGA_DEVICE.get().defaultBlockState()
                .setValue(com.jhonfx.portalgun.block.OmegaDeviceBlock.FACING,net.minecraft.core.Direction.NORTH),2);

        // Exterior hanging fins make the station readable against the void.
        if(arena&&r2>=45*45&&r2<=50*50&&Math.floorMod(x+z,9)<=1)
            for(int y=78;y<=94;y++)level.setBlock(new BlockPos(x,y,z),y%5==0?glass:panel,2);
    }

    private static boolean insideV3(int x,int z){
        int r2=x*x+z*z;
        if(r2<=52*52)return true;
        if(x*x+(z-148)*(z-148)<=20*20)return true;
        if(z>=124&&z<=148&&Math.abs(x)<=12)return true;
        if(z>=96&&z<=124&&Math.abs(x)<=20)return true;
        if(z>=64&&z<=96&&Math.abs(x)<=32)return true;
        if(z>=38&&z<=64&&Math.abs(x)<=26)return true;
        return z>=28&&z<=42&&Math.abs(x)<=10;
    }
}

package com.jhonfx.portalgun.command;

import com.jhonfx.portalgun.citadel.CitadelBuilder;
import com.jhonfx.portalgun.federation.FederationBuilder;
import com.jhonfx.portalgun.item.PortalGunItem;
import com.jhonfx.portalgun.item.PortalGunVariant;
import com.jhonfx.portalgun.item.PortalMode;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class PortalCommands {

    private static final String[] DIMENSIONS_BASE = {
        "minecraft:overworld", "minecraft:the_nether", "minecraft:the_end",
        "portalgun:citadel", "portalgun:federation", "portalgun:unmortricken"
    };

    private static String[] allDimensions() {
        java.util.List<String> list = new java.util.ArrayList<>(java.util.List.of(DIMENSIONS_BASE));
        list.addAll(com.jhonfx.portalgun.compat.AdAstraCompat.spaceDimensions());
        return list.toArray(new String[0]);
    }

    private static final String[] STRUCTURES = {
        "minecraft:village_plains", "minecraft:village_desert",
        "minecraft:village_taiga", "minecraft:village_snowy",
        "minecraft:pillager_outpost", "minecraft:stronghold",
        "minecraft:mineshaft", "minecraft:ocean_monument",
        "minecraft:nether_fortress", "minecraft:end_city",
        "minecraft:ruined_portal", "minecraft:mansion"
    };

    private static final String[] BIOMES = {
        "minecraft:plains", "minecraft:desert", "minecraft:forest",
        "minecraft:taiga", "minecraft:swamp", "minecraft:jungle",
        "minecraft:badlands", "minecraft:ocean", "minecraft:mountains",
        "minecraft:mushroom_fields", "minecraft:nether_wastes",
        "minecraft:soul_sand_valley", "minecraft:the_end"
    };

    @SubscribeEvent
    public void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("portalgun")

            // ── destination ──────────────────────────────────────────────────
            .then(Commands.literal("destination")
                .then(Commands.literal("dimension")
                    .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggestResource(ctx.getSource().getServer().levelKeys().stream().map(ResourceKey::location), b))
                        .executes(ctx -> selectDimension(
                            ctx.getSource().getPlayerOrException(),
                            StringArgumentType.getString(ctx, "id")))))
                .then(Commands.literal("coordinates")
                    .then(Commands.argument("position", Vec3Argument.vec3())
                        .executes(ctx -> selectCoordinates(
                            ctx.getSource().getPlayerOrException(),
                            Vec3Argument.getVec3(ctx, "position")))))
                .then(Commands.literal("coords_in_dim")
                    .then(Commands.argument("dim", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggestResource(ctx.getSource().getServer().levelKeys().stream().map(ResourceKey::location), b))
                        .then(Commands.argument("position", Vec3Argument.vec3())
                            .executes(ctx -> selectCoordsInDim(
                                ctx.getSource().getPlayerOrException(),
                                StringArgumentType.getString(ctx, "dim"),
                                Vec3Argument.getVec3(ctx, "position")))))))

            // ── find ──────────────────────────────────────────────────────────
            .then(Commands.literal("find")
                .then(Commands.literal("curve")
                    .executes(ctx -> show(ctx.getSource().getPlayerOrException(),
                        "Curva Finita Central", CitadelBuilder.CENTER.offset(0, 2, -76))))
                .then(Commands.literal("citadel")
                    .executes(ctx -> show(ctx.getSource().getPlayerOrException(),
                        "Cidadela dos Ricks", CitadelBuilder.CENTER)))
                .then(Commands.literal("federation")
                    .executes(ctx -> show(ctx.getSource().getPlayerOrException(),
                        "Federação Galáctica", FederationBuilder.CENTER)))
                .then(Commands.literal("structure")
                    .then(Commands.argument("structure", StringArgumentType.string())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggestResource(ctx.getSource().registryAccess().registryOrThrow(Registries.STRUCTURE).keySet(), b))
                        .executes(ctx -> findStructure(
                            ctx.getSource().getPlayerOrException(),
                            StringArgumentType.getString(ctx, "structure")))))
                .then(Commands.literal("biome")
                    .then(Commands.argument("biome", StringArgumentType.string())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggestResource(ctx.getSource().registryAccess().registryOrThrow(Registries.BIOME).keySet(), b))
                        .executes(ctx -> findBiome(
                            ctx.getSource().getPlayerOrException(),
                            StringArgumentType.getString(ctx, "biome"))))))

            // ── charge ────────────────────────────────────────────────────────
            .then(Commands.literal("charge")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("amount", IntegerArgumentType.integer(0, 2000))
                    .executes(ctx -> setCharge(
                        ctx.getSource().getPlayerOrException(),
                        IntegerArgumentType.getInteger(ctx, "amount")))))

            // ── wanted (federation) ───────────────────────────────────────────
            .then(Commands.literal("wanted")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("clear")
                    .executes(ctx -> clearWanted(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("set")
                    .then(Commands.argument("level", IntegerArgumentType.integer(0, 5))
                        .executes(ctx -> setWanted(
                            ctx.getSource().getPlayerOrException(),
                            IntegerArgumentType.getInteger(ctx, "level"))))))

            // ── story diagnostics/recovery ──────────────────────────────────
            .then(Commands.literal("story")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("status")
                    .executes(ctx -> storyStatus(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("set")
                    .then(Commands.argument("stage", IntegerArgumentType.integer(0, 14))
                        .executes(ctx -> storySet(ctx.getSource().getPlayerOrException(),
                                IntegerArgumentType.getInteger(ctx, "stage")))))
                .then(Commands.literal("reset")
                    .executes(ctx -> storyReset(ctx.getSource().getPlayerOrException()))))

            // ── explicit structure recovery/render commands ────────────────
            .then(Commands.literal("render")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("trap")
                    .executes(ctx -> com.jhonfx.portalgun.federation.FederationSignalArc
                            .renderTrapFor(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("armadilha")
                    .executes(ctx -> com.jhonfx.portalgun.federation.FederationSignalArc
                            .renderTrapFor(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("omega")
                    .executes(ctx -> com.jhonfx.portalgun.federation.FederationSignalArc
                            .renderOmegaFor(ctx.getSource().getPlayerOrException()))))

            // ── federation render (força a dimensão a existir/rodar mesmo à distância) ──
            .then(Commands.literal("federation")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("render")
                    .executes(ctx -> renderFederation(ctx.getSource().getServer(), true)))
                .then(Commands.literal("unrender")
                    .executes(ctx -> renderFederation(ctx.getSource().getServer(), false))))
            .then(Commands.literal("ship")
                .then(Commands.literal("home")
                    .executes(ctx -> shipHome(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("return")
                    .executes(ctx -> shipReturn(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("defense")
                    .then(Commands.literal("on").executes(ctx -> shipDefense(ctx.getSource().getPlayerOrException(),true)))
                    .then(Commands.literal("off").executes(ctx -> shipDefense(ctx.getSource().getPlayerOrException(),false)))
                    .then(Commands.literal("toggle").executes(ctx -> shipDefense(ctx.getSource().getPlayerOrException(),null)))))
            .then(Commands.literal("diagnostics")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> diagnostics(ctx.getSource().getPlayerOrException())))
        );
    }

    private static com.jhonfx.portalgun.entity.RickShipEntity ownedShip(ServerPlayer player) {
        if(player.getVehicle() instanceof com.jhonfx.portalgun.entity.RickShipEntity mounted && mounted.isOwnedBy(player)) return mounted;
        return player.serverLevel().getEntitiesOfClass(com.jhonfx.portalgun.entity.RickShipEntity.class,
                player.getBoundingBox().inflate(128),ship->ship.isOwnedBy(player)).stream()
                .min(java.util.Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
    }
    private static int shipHome(ServerPlayer player){var ship=ownedShip(player);if(ship==null){player.displayClientMessage(Component.translatable("message.portalgun.ship_not_found"),true);return 0;}ship.setHome(ship.blockPosition());player.displayClientMessage(Component.translatable("message.portalgun.ship_home_set",ship.blockPosition().toShortString()),false);return 1;}
    private static int shipReturn(ServerPlayer player){var ship=ownedShip(player);if(ship==null){player.displayClientMessage(Component.translatable("message.portalgun.ship_not_found"),true);return 0;}boolean ok=ship.startReturn();player.displayClientMessage(Component.translatable(ok?"message.portalgun.ship_return":"message.portalgun.ship_home_invalid"),true);return ok?1:0;}
    private static int shipDefense(ServerPlayer player,Boolean requested){var ship=ownedShip(player);if(ship==null){player.displayClientMessage(Component.translatable("message.portalgun.ship_not_found"),true);return 0;}boolean enabled=requested==null?!ship.isDefenseEnabled():requested;ship.setDefenseEnabled(enabled);player.displayClientMessage(Component.translatable("message.portalgun.ship_defense",enabled?"ON":"OFF"),true);return 1;}

    // ── Implementations ───────────────────────────────────────────────────────

    private static int selectDimension(ServerPlayer player, String dim) {
        ResourceLocation id = ResourceLocation.tryParse(dim);
        if (id == null || player.server.getLevel(ResourceKey.create(Registries.DIMENSION, id)) == null) {
            player.displayClientMessage(Component.literal("Dimensão desconhecida: " + dim), true);
            return 0;
        }
        ItemStack gun = heldGun(player);
        if (gun.isEmpty()) return 0;
        Vec3 pos = switch (dim) {
            case "portalgun:citadel"    -> Vec3.atBottomCenterOf(CitadelBuilder.ARRIVAL);
            case "portalgun:federation" -> Vec3.atBottomCenterOf(FederationBuilder.ARRIVAL);
            case "portalgun:unmortricken" -> Vec3.atBottomCenterOf(com.jhonfx.portalgun.federation.UnmortrickenFacility.ARRIVAL);
            default -> player.position();
        };
        putDestination(gun, pos, dim);
        player.displayClientMessage(Component.literal("§aDestino dimensional: §f" + dim), false);
        return 1;
    }

    private static int selectCoordinates(ServerPlayer player, Vec3 pos) {
        ItemStack gun = heldGun(player);
        if (gun.isEmpty()) return 0;
        putDestination(gun, pos, player.level().dimension().location().toString());
        player.displayClientMessage(Component.literal(String.format(
                "§aDestino: §f%d, %d, %d", (int)pos.x, (int)pos.y, (int)pos.z)), false);
        return 1;
    }

    private static int selectCoordsInDim(ServerPlayer player, String dim, Vec3 pos) {
        ItemStack gun = heldGun(player);
        if (gun.isEmpty()) return 0;
        putDestination(gun, pos, dim);
        player.displayClientMessage(Component.literal(String.format(
                "§aDestino: §f%d, %d, %d §7em §f%s", (int)pos.x, (int)pos.y, (int)pos.z, dim)), false);
        return 1;
    }

    private static int findStructure(ServerPlayer player, String structureId) {
        ItemStack gun = heldGun(player);
        if (gun.isEmpty()) return 0;
        // Only Tier 2+ can structure-search
        if (gun.getItem() instanceof PortalGunItem pgi && !pgi.variant().supportsStructureSearch()) {
            player.displayClientMessage(Component.literal("§cProtótipo não suporta busca de estrutura."), true);
            return 0;
        }
        ResourceLocation rl = ResourceLocation.tryParse(structureId);
        if (rl == null) { player.displayClientMessage(Component.literal("§cID inválido."), true); return 0; }

        ServerLevel level = player.serverLevel();
        var structureRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var structure = structureRegistry.get(rl);
        if (structure == null) {
            player.displayClientMessage(Component.literal("§cEstrutura desconhecida: §f" + structureId), true);
            return 0;
        }
        Holder<net.minecraft.world.level.levelgen.structure.Structure> holder =
                structureRegistry.getHolderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE, rl));
        var set = net.minecraft.core.HolderSet.direct(holder);

        var result = level.getChunkSource().getGenerator().findNearestMapStructure(
                level, set, player.blockPosition(), 100, false);

        if (result == null) {
            player.displayClientMessage(Component.literal("§cNenhuma §f" + structureId + " §cencontrada nas proximidades."), false);
        } else {
            BlockPos pos = result.getFirst();
            player.displayClientMessage(Component.literal(String.format(
                    "§aEstrutura §f%s§a encontrada em §f%d, %d, %d",
                    structureId, pos.getX(), pos.getY(), pos.getZ())), false);
            putDestination(gun, Vec3.atBottomCenterOf(pos), level.dimension().location().toString());
        }
        return 1;
    }

    private static int findBiome(ServerPlayer player, String biomeId) {
        ResourceLocation rl = ResourceLocation.tryParse(biomeId);
        if (rl == null) { player.displayClientMessage(Component.literal("§cID de bioma inválido."), true); return 0; }
        ServerLevel level = player.serverLevel();
        var result = level.findClosestBiome3d(
                b -> b.is(ResourceKey.create(Registries.BIOME, rl)),
                player.blockPosition(), 6400, 8, 8);
        if (result == null) {
            player.displayClientMessage(Component.literal("§cBioma §f" + biomeId + " §cnão encontrado próximo."), false);
        } else {
            BlockPos pos = result.getFirst();
            player.displayClientMessage(Component.literal(String.format(
                    "§aBioma §f%s§a encontrado em §f%d, %d, %d",
                    biomeId, pos.getX(), pos.getY(), pos.getZ())), false);
            // Set as portal destination if holding gun
            ItemStack gun = heldGun(player);
            if (!gun.isEmpty()) putDestination(gun,
                    Vec3.atBottomCenterOf(pos), level.dimension().location().toString());
        }
        return 1;
    }

    private static int setCharge(ServerPlayer player, int amount) {
        ItemStack gun = heldGun(player);
        if (gun.isEmpty()) return 0;
        PortalGunItem.initialize(gun);
        gun.getOrCreateTag().putInt(PortalGunItem.TAG_CHARGE, amount);
        player.displayClientMessage(Component.literal("§aCarga definida para §f" + amount), true);
        return 1;
    }

    private static int clearWanted(ServerPlayer player) {
        com.jhonfx.portalgun.federation.FederationThreatHandler.lower(player, 5);
        player.displayClientMessage(Component.literal("§aNível de procurado zerado."), true);
        return 1;
    }

    private static int setWanted(ServerPlayer player, int level) {
        player.getPersistentData().putInt(
                com.jhonfx.portalgun.federation.FederationThreatHandler.KEY_WANTED, level);
        player.displayClientMessage(Component.literal("§cNível de procurado: §f" + level), true);
        return 1;
    }

    private static int storyStatus(ServerPlayer player) {
        var curve = com.jhonfx.portalgun.curve.CurveSavedData.get(player.serverLevel());
        CompoundTag root = player.getPersistentData();
        player.displayClientMessage(Component.literal("§b[PortalGun Story]§r estágio P-0: §f"
                + com.jhonfx.portalgun.federation.FederationSignalArc.getStage(player)
                + "§7/14 §8| §rCurva: §f" + (curve.broken() ? "rompida" : curve.battleStarted() ? "batalha" : curve.breaking() ? "rompendo" : "intacta")
                + " §8| §rNeural: §f" + curve.neuralData() + "% §8| §rDrive: §f" + curve.fluid() + "%"), false);
        player.displayClientMessage(Component.literal("§7UNMORTRICKEN: "
                + (root.getBoolean("PortalGunUnmortrickenUnlocked") ? "§aliberado" : "§cbloqueado")
                + " §8| §7Rick Prime: " + (root.getBoolean("RickPrimeDefeated") ? "§aderrotado" : "§cvivo")
                + " §8| §7Wanted: §f" + root.getInt(com.jhonfx.portalgun.federation.FederationThreatHandler.KEY_WANTED)), false);
        return 1;
    }

    private static int storySet(ServerPlayer player, int stage) {
        com.jhonfx.portalgun.federation.FederationSignalArc.setStageForTesting(player, stage);
        player.displayClientMessage(Component.literal("§e[PortalGun Story] §fEstágio P-0 ajustado para §b" + stage
                + "§f. Use §b/portalgun story status§f para conferir."), false);
        return 1;
    }

    private static int storyReset(ServerPlayer player) {
        com.jhonfx.portalgun.federation.FederationSignalArc.resetForTesting(player);
        player.displayClientMessage(Component.literal("§e[PortalGun Story] §fProgresso pessoal P-0/Prime resetado. "
                + "O estado global da Curva e construções foi preservado para não reconstruir/explodir o mundo por acidente."), false);
        return 1;
    }

    /**
     * Força a dimensão da Federação a existir e continuar rodando (chunks
     * carregados/ticking) mesmo que quem executou o comando esteja no
     * Overworld ou em qualquer outra dimensão. Útil para admins que querem
     * testar/preparar a Federação sem precisar viajar até lá.
     *
     * "render" força o carregamento de um raio de chunks ao redor do centro
     * da Federação (garante a construção via FederationBuilder e mantém os
     * chunks ativos). "unrender" desfaz isso, liberando os chunks para não
     * deixar o servidor processando a dimensão pra sempre sem necessidade.
     */
    private static int renderFederation(net.minecraft.server.MinecraftServer server, boolean forceLoad) {
        ServerLevel fedLevel = server.getLevel(
                ResourceKey.create(Registries.DIMENSION, new ResourceLocation("portalgun", "federation")));
        if (fedLevel == null) {
            server.getPlayerList().broadcastSystemMessage(Component.literal(
                    "§cDimensão portalgun:federation não encontrada. Verifique se o mod registrou a dimensão corretamente."), false);
            return 0;
        }

        if (forceLoad) {
            FederationBuilder.ensureBuilt(fedLevel);
        }

        int chunkRadius = 6; // ~6 chunks (96 blocos) ao redor do centro — cobre toda a estrutura
        int centerChunkX = FederationBuilder.CENTER.getX() >> 4;
        int centerChunkZ = FederationBuilder.CENTER.getZ() >> 4;
        int count = 0;
        for (int cx = -chunkRadius; cx <= chunkRadius; cx++) {
            for (int cz = -chunkRadius; cz <= chunkRadius; cz++) {
                fedLevel.setChunkForced(centerChunkX + cx, centerChunkZ + cz, forceLoad);
                count++;
            }
        }

        server.getPlayerList().broadcastSystemMessage(Component.literal(
                forceLoad
                        ? "§a[FEDERAÇÃO] §fDimensão renderizando ativamente (" + count + " chunks forçados). Continuará rodando mesmo longe dela."
                        : "§e[FEDERAÇÃO] §fRenderização forçada desativada (" + count + " chunks liberados)."
        ), false);
        return 1;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void putDestination(ItemStack gun, Vec3 pos, String dim) {
        PortalGunItem.initialize(gun);
        CompoundTag dest = new CompoundTag();
        dest.putDouble("X", pos.x);
        dest.putDouble("Y", pos.y);
        dest.putDouble("Z", pos.z);
        dest.putString("Dimension", dim);
        gun.getOrCreateTag().put(PortalGunItem.TAG_DESTINATION, dest);
        gun.getTag().putString(PortalGunItem.TAG_MODE, PortalMode.CUSTOM.name());
    }

    private static ItemStack heldGun(ServerPlayer player) {
        ItemStack main = player.getMainHandItem(), off = player.getOffhandItem();
        if (main.getItem() instanceof PortalGunItem) return main;
        if (off.getItem()  instanceof PortalGunItem) return off;
        player.displayClientMessage(
                Component.literal("§cSegure uma Portal Gun para selecionar o destino."), false);
        return ItemStack.EMPTY;
    }

    private static int diagnostics(ServerPlayer player){
        int passed=0,total=0;var server=player.server;
        for(String raw:DIMENSIONS_BASE){total++;ResourceLocation id=ResourceLocation.tryParse(raw);
            boolean ok=id!=null&&server.getLevel(ResourceKey.create(Registries.DIMENSION,id))!=null;if(ok)passed++;
            player.displayClientMessage(Component.literal((ok?"§a✔ ":"§c✘ ")+raw),false);}
        ServerLevel citadel=server.getLevel(ResourceKey.create(Registries.DIMENSION,new ResourceLocation("portalgun","citadel")));
        ServerLevel federation=server.getLevel(ResourceKey.create(Registries.DIMENSION,new ResourceLocation("portalgun","federation")));
        ServerLevel prime=server.getLevel(ResourceKey.create(Registries.DIMENSION,com.jhonfx.portalgun.federation.UnmortrickenFacility.ID));
        if(citadel!=null){CitadelBuilder.ensureBuilt(citadel);total++;if(safeNear(citadel,CitadelBuilder.ARRIVAL))passed++;}
        if(federation!=null){FederationBuilder.ensureBuilt(federation);total++;if(safeNear(federation,FederationBuilder.ARRIVAL))passed++;
            player.displayClientMessage(Component.literal("§7Construção da Federação: §f"+FederationBuilder.buildPercent(federation)+"%"),false);}
        if(prime!=null){com.jhonfx.portalgun.federation.UnmortrickenFacility.ensureBuilt(prime);
            com.jhonfx.portalgun.federation.UnmortrickenFacility.prepareArrival(prime);total++;if(safeNear(prime,com.jhonfx.portalgun.federation.UnmortrickenFacility.ARRIVAL))passed++;}
        ItemStack gun=heldGun(player);if(!gun.isEmpty()){
            total++;PortalGunItem.initialize(gun);CompoundTag d=gun.getTag().getCompound(PortalGunItem.TAG_DESTINATION);
            ResourceLocation id=ResourceLocation.tryParse(d.getString("Dimension"));ServerLevel level=id==null?null:server.getLevel(ResourceKey.create(Registries.DIMENSION,id));
            BlockPos pos=BlockPos.containing(d.getDouble("X"),d.getDouble("Y"),d.getDouble("Z"));
            boolean safe=level!=null&&level.getBlockState(pos).isAir()&&level.getBlockState(pos.above()).isAir()
                    &&level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),net.minecraft.core.Direction.UP);
            if(safe)passed++;player.displayClientMessage(Component.literal((safe?"§a✔ ":"§c✘ ")+"Destino ativo: "+d.getString("Dimension")+" "+pos.toShortString()),false);
        }
        player.displayClientMessage(Component.literal("§bDIAGNÓSTICO PORTALGUN: §f"+passed+"/"+total+" verificações aprovadas."),false);return passed;
    }

    private static boolean safeNear(ServerLevel level,BlockPos center){
        for(BlockPos pos:BlockPos.betweenClosed(center.offset(-3,-3,-3),center.offset(3,3,3)))
            if(level.getBlockState(pos).isAir()&&level.getBlockState(pos.above()).isAir()
                    &&level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),net.minecraft.core.Direction.UP))return true;
        return false;
    }

    private static int show(ServerPlayer player, String name, net.minecraft.core.BlockPos pos) {
        player.displayClientMessage(Component.literal("§b" + name + "§r: §f" + pos.toShortString()), false);
        return 1;
    }
}

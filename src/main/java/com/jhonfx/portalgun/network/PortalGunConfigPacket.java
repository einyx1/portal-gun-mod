package com.jhonfx.portalgun.network;

import com.jhonfx.portalgun.entity.PortalColor;
import com.jhonfx.portalgun.init.ModItems;
import com.jhonfx.portalgun.init.ModSounds;
import com.jhonfx.portalgun.item.PortalFluidTubeItem;
import com.jhonfx.portalgun.item.PortalGunItem;
import com.jhonfx.portalgun.item.PortalMode;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Optional;
import java.util.function.Supplier;

public record PortalGunConfigPacket(InteractionHand hand, Action action, String mode,
                                    double x, double y, double z, String dimension,
                                    float scale, int duration, int airRange, String structure) {
    public enum Action { UPDATE, CLOSE_PORTALS, PLUG_TUBE, UNPLUG_TUBE, LOCATE_STRUCTURE, LOCATE_BIOME, TERMINAL }

    public static void encode(PortalGunConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.hand); buffer.writeEnum(packet.action); buffer.writeUtf(packet.mode);
        buffer.writeDouble(packet.x); buffer.writeDouble(packet.y); buffer.writeDouble(packet.z);
        buffer.writeUtf(packet.dimension); buffer.writeFloat(packet.scale); buffer.writeVarInt(packet.duration);
        buffer.writeVarInt(packet.airRange);
        buffer.writeUtf(packet.structure);
    }

    public static PortalGunConfigPacket decode(FriendlyByteBuf buffer) {
        return new PortalGunConfigPacket(buffer.readEnum(InteractionHand.class), buffer.readEnum(Action.class),
                buffer.readUtf(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readUtf(),
                buffer.readFloat(), buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf());
    }

    public static void handle(PortalGunConfigPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (!Double.isFinite(packet.x) || !Double.isFinite(packet.y) || !Double.isFinite(packet.z)
                    || !Float.isFinite(packet.scale)) return;
            if (packet.action == Action.UPDATE && !packet.dimension.isBlank()) {
                ResourceLocation id = ResourceLocation.tryParse(normalizeDimension(packet.dimension));
                if (id == null || player.server.getLevel(ResourceKey.create(Registries.DIMENSION, id)) == null) {
                    player.displayClientMessage(Component.literal("Dimensão desconhecida. Use uma das sugestões do servidor."), true);
                    return;
                }
            }
            ItemStack gunStack = player.getItemInHand(packet.hand);
            if (!(gunStack.getItem() instanceof PortalGunItem gunItem)) return;
            PortalGunItem.initialize(gunStack);
            if (gunItem.variant() == com.jhonfx.portalgun.item.PortalGunVariant.BLUE_PROTOTYPE) {
                if (packet.action == Action.LOCATE_STRUCTURE || packet.action == Action.LOCATE_BIOME) {
                    player.displayClientMessage(Component.literal("O Protótipo Azul ainda não possui localizador dimensional."), true);
                    return;
                }
                if (packet.action == Action.UPDATE && !packet.dimension.isBlank()
                        && !normalizeDimension(packet.dimension).equals(player.level().dimension().location().toString())) {
                    player.displayClientMessage(Component.literal("O Protótipo Azul só opera na dimensão atual."), true);
                    return;
                }
            } else if (gunItem.variant() == com.jhonfx.portalgun.item.PortalGunVariant.PROTOTYPE) {
                String requested = normalizeDimension(packet.dimension);
                if (packet.action == Action.UPDATE && !packet.dimension.isBlank()
                        && !requested.equals("portalgun:citadel") && !requested.equals("minecraft:overworld")) {
                    player.displayClientMessage(Component.literal("O Protótipo Verde só alcança o Overworld e a Cidadela."), true);
                    return;
                }
                if ((packet.action == Action.LOCATE_STRUCTURE || packet.action == Action.LOCATE_BIOME)
                        && !player.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
                    player.displayClientMessage(Component.literal("O localizador do Protótipo Verde funciona somente no Overworld."), true);
                    return;
                }
            }
            switch (packet.action) {
                case CLOSE_PORTALS -> PortalGunItem.closeAllPortals(player, gunStack);
                case PLUG_TUBE -> plugFirstTube(player, gunStack);
                case UNPLUG_TUBE -> unplugTube(player, gunStack);
                case LOCATE_STRUCTURE -> locateStructure(player, gunStack, packet.structure);
                case LOCATE_BIOME -> locateBiome(player, gunStack, packet.structure);
                case TERMINAL -> runTerminal(player, gunStack, packet.structure);
                case UPDATE -> {
                    update(gunStack, packet);
                    if (gunItem.variant() == com.jhonfx.portalgun.item.PortalGunVariant.PROTOTYPE
                            && normalizeDimension(packet.dimension).equals("portalgun:citadel"))
                        setDestination(gunStack, 0.5, 97, 10.5, "portalgun:citadel");
                }
            }
            if (packet.action == Action.UPDATE) player.playSound(ModSounds.SELECTION.get(), 0.55f, 1.0f);
            else if (packet.action != Action.PLUG_TUBE && packet.action != Action.UNPLUG_TUBE)
                player.playSound(ModSounds.CLICK.get(), 0.55f, 1.0f);
        });
        context.setPacketHandled(true);
    }

    private static void update(ItemStack stack, PortalGunConfigPacket packet) {
        try { stack.getTag().putString(PortalGunItem.TAG_MODE, PortalMode.valueOf(packet.mode).name()); }
        catch (IllegalArgumentException ignored) { stack.getTag().putString(PortalGunItem.TAG_MODE, PortalMode.FIFO.name()); }
        stack.getTag().putFloat(PortalGunItem.TAG_SCALE, Math.max(0.5f, Math.min(4.0f, packet.scale)));
        stack.getTag().putInt(PortalGunItem.TAG_DURATION, Math.max(20 * 3, Math.min(20 * 60, packet.duration)));
        stack.getTag().putInt(PortalGunItem.TAG_AIR_RANGE, Math.max(8, Math.min(256, packet.airRange)));
        if (!packet.dimension.isBlank()) setDestination(stack, packet.x, packet.y, packet.z,
                normalizeDimension(packet.dimension));
    }

    private static void plugFirstTube(ServerPlayer player, ItemStack gun) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack candidate = player.getInventory().getItem(slot);
            if (!(candidate.getItem() instanceof PortalFluidTubeItem tube)) continue;
            PortalGunItem gunItem = (PortalGunItem) gun.getItem();
            if (tube.color() != gunItem.variant().color()) continue;
            if (PortalGunItem.getLoadedColor(gun) != null) unplugTube(player, gun);
            gun.getTag().putString(PortalGunItem.TAG_TUBE_COLOR, tube.color().name());
            gun.getTag().putInt(PortalGunItem.TAG_CHARGE, PortalFluidTubeItem.getCharge(candidate));
            if (!player.getAbilities().instabuild) candidate.shrink(1);
            player.playSound(ModSounds.PLUG.get(), 0.8f, 1.0f);
            return;
        }
        player.displayClientMessage(Component.translatable("message.portalgun.need_fluid"), true);
    }

    private static void unplugTube(ServerPlayer player, ItemStack gun) {
        PortalColor color = PortalGunItem.getLoadedColor(gun);
        if (color == null) return;
        ItemStack tube = new ItemStack(switch (color) {
            case BLUE -> ModItems.BLUE_PORTAL_FLUID.get();
            case GREEN -> ModItems.GREEN_PORTAL_FLUID.get();
            case YELLOW -> ModItems.YELLOW_PORTAL_FLUID.get();
        });
        PortalFluidTubeItem.setCharge(tube, PortalGunItem.getCharge(gun));
        if (!player.getInventory().add(tube)) player.drop(tube, false);
        gun.getTag().putString(PortalGunItem.TAG_TUBE_COLOR, "");
        gun.getTag().putInt(PortalGunItem.TAG_CHARGE, 0);
        player.playSound(ModSounds.UNPLUG.get(), 0.8f, 1.0f);
    }

    private static void locateStructure(ServerPlayer player, ItemStack gun, String rawId) {
        ResourceLocation id = ResourceLocation.tryParse(normalizeDimension(rawId));
        if (id == null) return;
        ServerLevel level = player.serverLevel();
        ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, id);
        Optional<Holder.Reference<Structure>> holder = level.registryAccess()
                .registryOrThrow(Registries.STRUCTURE).getHolder(key);
        if (holder.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.portalgun.structure_invalid", id), true);
            return;
        }
        Pair<BlockPos, Holder<Structure>> result = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, HolderSet.direct(holder.get()), player.blockPosition(), 128, false);
        if (result == null) {
            player.displayClientMessage(Component.translatable("message.portalgun.structure_not_found", id), true);
            return;
        }
        BlockPos pos = findStandable(level,result.getFirst());
        setDestination(gun, pos.getX(), pos.getY(), pos.getZ(), level.dimension().location().toString());
        gun.getTag().putString(PortalGunItem.TAG_MODE, PortalMode.CUSTOM.name());
        gun.getTag().putString(PortalGunItem.TAG_STRUCTURE, id.toString());
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new StructureLocatedPacket(pos.getX(), pos.getY(), pos.getZ(),
                        level.dimension().location().toString(), id.toString()));
        player.displayClientMessage(Component.translatable("message.portalgun.structure_found",
                id, pos.getX(), pos.getY(), pos.getZ()), false);
    }

    private static void locateBiome(ServerPlayer player,ItemStack gun,String rawId){
        ResourceLocation id=ResourceLocation.tryParse(normalizeDimension(rawId));if(id==null)return;
        ResourceKey<Biome> key=ResourceKey.create(Registries.BIOME,id);
        Optional<Holder.Reference<Biome>> holder=player.serverLevel().registryAccess().registryOrThrow(Registries.BIOME).getHolder(key);
        if(holder.isEmpty()){player.displayClientMessage(Component.literal("Bioma inválido: "+id),true);return;}
        Pair<BlockPos,Holder<Biome>> found=player.serverLevel().findClosestBiome3d(h->h.is(key),player.blockPosition(),6400,32,64);
        if(found==null){player.displayClientMessage(Component.literal("Bioma não encontrado: "+id),true);return;}
        BlockPos pos=findStandable(player.serverLevel(),found.getFirst());setDestination(gun,pos.getX(),pos.getY(),pos.getZ(),player.serverLevel().dimension().location().toString());
        gun.getTag().putString(PortalGunItem.TAG_MODE,PortalMode.CUSTOM.name());
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new StructureLocatedPacket(pos.getX(),pos.getY(),pos.getZ(),player.serverLevel().dimension().location().toString(),id.toString()));
        player.displayClientMessage(Component.literal("Bioma encontrado em local seguro: "+pos.toShortString()),false);
    }

    private static BlockPos findStandable(ServerLevel level,BlockPos requested){
        for(int radius=0;radius<=24;radius++)for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            if(radius>0 && Math.abs(x)!=radius && Math.abs(z)!=radius)continue;
            int worldX=requested.getX()+x,worldZ=requested.getZ()+z;
            int y=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,worldX,worldZ);
            BlockPos p=new BlockPos(worldX,y,worldZ);
            if(level.getWorldBorder().isWithinBounds(p)&&level.getBlockState(p).isAir()&&level.getBlockState(p.above()).isAir()
                    &&level.getBlockState(p.below()).isFaceSturdy(level,p.below(),net.minecraft.core.Direction.UP))return p;
        }return requested;
    }

    private static void setDestination(ItemStack stack, double x, double y, double z, String dimension) {
        CompoundTag destination = new CompoundTag();
        destination.putDouble("X", x); destination.putDouble("Y", y); destination.putDouble("Z", z);
        destination.putString("Dimension", dimension);
        stack.getTag().put(PortalGunItem.TAG_DESTINATION, destination);
        ListTag history = stack.getTag().getList(PortalGunItem.TAG_HISTORY, Tag.TAG_COMPOUND);
        history.add(0, destination.copy());
        while (history.size() > 30) history.remove(history.size() - 1);
        stack.getTag().put(PortalGunItem.TAG_HISTORY, history);
    }

    private static void runTerminal(ServerPlayer player, ItemStack gun, String rawCommand) {
        String command = rawCommand == null ? "" : rawCommand.trim();
        if(command.startsWith("/"))command=command.substring(1);
        String[] args = command.split("\\s+", 2);
        String op = args.length == 0 ? "help" : args[0].toLowerCase();
        String value = args.length > 1 ? args[1].trim() : "";
        switch (op) {
            case "coords" -> player.displayClientMessage(Component.literal(String.format(
                    "X %.1f  Y %.1f  Z %.1f  [%s]", player.getX(), player.getY(), player.getZ(),
                    player.level().dimension().location())), false);
            case "save" -> saveLocation(player, gun, value);
            case "showlocs" -> showLocations(player, gun, value);
            case "delloc" -> deleteLocation(player, gun, value);
            case "use" -> useLocation(player, gun, value);
            case "history" -> useHistory(player, gun, value);
            case "pressure" -> toggle(gun, PortalGunItem.TAG_HIGH_PRESSURE, value, player, "Alta pressão");
            case "safety" -> toggle(gun, PortalGunItem.TAG_SAFE_MODE, value, player, "Colocação segura");
            case "autoclose" -> toggle(gun, PortalGunItem.TAG_AUTO_CLOSE, value, player, "Auto-fechar");
            case "singleuse" -> toggle(gun,PortalGunItem.TAG_CLOSE_AFTER_USE,value,player,"Fechar após primeira travessia");
            case "reset" -> {
                gun.removeTagKey(PortalGunItem.TAG_DESTINATION);
                gun.removeTagKey(PortalGunItem.TAG_SAVED_LOCATIONS);
                gun.removeTagKey(PortalGunItem.TAG_HISTORY);
                gun.getTag().putString(PortalGunItem.TAG_MODE, PortalMode.FIFO.name());
                gun.getTag().putFloat(PortalGunItem.TAG_SCALE, 1.0f);
                gun.getTag().putInt(PortalGunItem.TAG_DURATION, 20 * 10);
                gun.getTag().putBoolean(PortalGunItem.TAG_HIGH_PRESSURE, false);
                gun.getTag().putBoolean(PortalGunItem.TAG_SAFE_MODE, true);
                gun.getTag().putBoolean(PortalGunItem.TAG_AUTO_CLOSE, true);
                gun.getTag().putBoolean(PortalGunItem.TAG_CLOSE_AFTER_USE, false);
                gun.getTag().putInt(PortalGunItem.TAG_AIR_RANGE, 48);
                PortalGunItem.initialize(gun);
                player.displayClientMessage(Component.translatable("message.portalgun.reset"), false);
            }
            case "gunconfig" -> player.displayClientMessage(Component.literal(String.format(
                    "Modo=%s | Escala=%.1fx | %ds | Ar=%dm | Pressão=%s | Segurança=%s | Auto-fechar=%s | Uso único=%s",
                    PortalGunItem.getMode(gun).displayName(), PortalGunItem.getPortalScale(gun),
                    PortalGunItem.getPortalDuration(gun) / 20, PortalGunItem.getAirRange(gun),
                    gun.getTag().getBoolean(PortalGunItem.TAG_HIGH_PRESSURE),
                    gun.getTag().getBoolean(PortalGunItem.TAG_SAFE_MODE),
                    gun.getTag().getBoolean(PortalGunItem.TAG_AUTO_CLOSE),
                    gun.getTag().getBoolean(PortalGunItem.TAG_CLOSE_AFTER_USE))), false);
            default -> player.displayClientMessage(Component.translatable("message.portalgun.help"), false);
        }
    }

    private static void saveLocation(ServerPlayer player, ItemStack gun, String name) {
        if (name.isBlank()) { player.displayClientMessage(Component.literal("Use: save <nome>"), false); return; }
        ListTag list = gun.getOrCreateTag().getList(PortalGunItem.TAG_SAVED_LOCATIONS, Tag.TAG_COMPOUND);
        for (int i = list.size() - 1; i >= 0; i--) if (list.getCompound(i).getString("Name").equalsIgnoreCase(name)) list.remove(i);
        CompoundTag entry = new CompoundTag(); entry.putString("Name", name);
        entry.putDouble("X", player.getX()); entry.putDouble("Y", player.getY()); entry.putDouble("Z", player.getZ());
        entry.putString("Dimension", player.level().dimension().location().toString());
        list.add(entry);
        gun.getTag().put(PortalGunItem.TAG_SAVED_LOCATIONS, list);
        player.displayClientMessage(Component.translatable("message.portalgun.location_saved", name), false);
    }

    private static void showLocations(ServerPlayer player, ItemStack gun, String search) {
        ListTag list = gun.getOrCreateTag().getList(PortalGunItem.TAG_SAVED_LOCATIONS, Tag.TAG_COMPOUND);
        int shown = 0;
        for (Tag tag : list) { CompoundTag e = (CompoundTag) tag;
            if (search.isBlank() || e.getString("Name").toLowerCase().contains(search.toLowerCase())) {
                player.displayClientMessage(Component.literal("• " + e.getString("Name") + " — "
                        + (int)e.getDouble("X") + ", " + (int)e.getDouble("Y") + ", " + (int)e.getDouble("Z")), false); shown++;
            }
        }
        if (shown == 0) player.displayClientMessage(Component.translatable("message.portalgun.no_locations"), false);
    }

    private static void deleteLocation(ServerPlayer player, ItemStack gun, String name) {
        ListTag list = gun.getOrCreateTag().getList(PortalGunItem.TAG_SAVED_LOCATIONS, Tag.TAG_COMPOUND);
        boolean removed = false;
        for (int i = list.size() - 1; i >= 0; i--) if (list.getCompound(i).getString("Name").equalsIgnoreCase(name)) { list.remove(i); removed = true; }
        gun.getTag().put(PortalGunItem.TAG_SAVED_LOCATIONS, list);
        player.displayClientMessage(Component.literal(removed ? "Local apagado: " + name : "Local não encontrado: " + name), false);
    }

    private static void useLocation(ServerPlayer player, ItemStack gun, String name) {
        ListTag list = gun.getOrCreateTag().getList(PortalGunItem.TAG_SAVED_LOCATIONS, Tag.TAG_COMPOUND);
        for (Tag tag : list) { CompoundTag e = (CompoundTag) tag; if (e.getString("Name").equalsIgnoreCase(name)) {
            setDestination(gun, e.getDouble("X"), e.getDouble("Y"), e.getDouble("Z"), e.getString("Dimension"));
            gun.getTag().putString(PortalGunItem.TAG_MODE, PortalMode.CUSTOM.name());
            player.displayClientMessage(Component.translatable("message.portalgun.location_selected", name), false); return;
        }}
        player.displayClientMessage(Component.literal("Local não encontrado: " + name), false);
    }

    private static void useHistory(ServerPlayer player, ItemStack gun, String indexText) {
        int index; try { index = Math.max(1, Integer.parseInt(indexText)); } catch (NumberFormatException ex) { index = 1; }
        ListTag history = gun.getOrCreateTag().getList(PortalGunItem.TAG_HISTORY, Tag.TAG_COMPOUND);
        if (index > history.size()) { player.displayClientMessage(Component.literal("Histórico vazio nessa posição."), false); return; }
        CompoundTag e = history.getCompound(index - 1);
        setDestination(gun, e.getDouble("X"), e.getDouble("Y"), e.getDouble("Z"), e.getString("Dimension"));
        gun.getTag().putString(PortalGunItem.TAG_MODE, PortalMode.CUSTOM.name());
    }

    private static void toggle(ItemStack gun, String key, String value, ServerPlayer player, String label) {
        boolean enabled = value.equalsIgnoreCase("on") || value.equalsIgnoreCase("true") || value.equals("1");
        gun.getOrCreateTag().putBoolean(key, enabled);
        player.displayClientMessage(Component.literal(label + ": " + (enabled ? "ON" : "OFF")), false);
    }

    private static String normalizeDimension(String value) {
        String trimmed = value == null ? "" : value.trim().toLowerCase();
        return trimmed.contains(":") ? trimmed : "minecraft:" + trimmed;
    }
}

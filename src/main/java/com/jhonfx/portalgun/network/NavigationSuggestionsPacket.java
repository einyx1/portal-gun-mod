package com.jhonfx.portalgun.network;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

/** Bounded, read-only registry completion. Search never loads a chunk. */
public record NavigationSuggestionsPacket(int kind, String query) {
    public static void encode(NavigationSuggestionsPacket p, FriendlyByteBuf b) { b.writeVarInt(p.kind); b.writeUtf(p.query, 128); }
    public static NavigationSuggestionsPacket decode(FriendlyByteBuf b) { return new NavigationSuggestionsPacket(b.readVarInt(), b.readUtf(128)); }
    public static void handle(NavigationSuggestionsPacket p, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null || p.kind < 0 || p.kind > 2) return;
            if (!(player.getMainHandItem().getItem() instanceof com.jhonfx.portalgun.item.PortalGunItem)
                    && !(player.getOffhandItem().getItem() instanceof com.jhonfx.portalgun.item.PortalGunItem)) return;
            long now = player.server.overworld().getGameTime();
            var tag = player.getPersistentData();
            long last = tag.getLong("PortalGunSuggestionsTick");
            if (last > 0 && now >= last && now - last < 4) return;
            tag.putLong("PortalGunSuggestionsTick", now);
            Stream<ResourceLocation> values = switch (p.kind) {
                case 0 -> player.server.levelKeys().stream().map(ResourceKey::location);
                case 1 -> player.serverLevel().registryAccess().registryOrThrow(Registries.STRUCTURE).keySet().stream();
                default -> player.serverLevel().registryAccess().registryOrThrow(Registries.BIOME).keySet().stream();
            };
            String search = p.query.toLowerCase(Locale.ROOT).trim();
            List<String> matches = values.map(ResourceLocation::toString).filter(id -> id.contains(search)).sorted().limit(12).toList();
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Reply(p.kind, p.query, matches));
        });
        context.setPacketHandled(true);
    }

    public record Reply(int kind, String query, List<String> matches) {
        public static void encode(Reply p, FriendlyByteBuf b) {
            b.writeVarInt(p.kind); b.writeUtf(p.query, 128); b.writeVarInt(p.matches.size());
            p.matches.forEach(s -> b.writeUtf(s, 256));
        }
        public static Reply decode(FriendlyByteBuf b) {
            int kind = b.readVarInt(); String query = b.readUtf(128); int size = b.readVarInt();
            if (size < 0 || size > 12) throw new IllegalArgumentException("Invalid suggestion count");
            var values = new java.util.ArrayList<String>();
            for (int i = 0; i < size; i++) values.add(b.readUtf(256));
            return new Reply(kind, query, List.copyOf(values));
        }
        public static void handle(Reply p, Supplier<NetworkEvent.Context> supplier) {
            supplier.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> com.jhonfx.portalgun.client.PortalGunMenuScreen.receiveSuggestions(p.kind, p.query, p.matches)));
            supplier.get().setPacketHandled(true);
        }
    }
}

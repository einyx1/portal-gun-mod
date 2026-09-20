package com.jhonfx.portalgun.network;

import com.jhonfx.portalgun.client.ClientPortalEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PortalTravelEffectPacket(int rgb, int orientation) {
    public static void encode(PortalTravelEffectPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.rgb); buffer.writeVarInt(packet.orientation);
    }
    public static PortalTravelEffectPacket decode(FriendlyByteBuf buffer) {
        return new PortalTravelEffectPacket(buffer.readInt(), buffer.readVarInt());
    }
    public static void handle(PortalTravelEffectPacket packet, Supplier<NetworkEvent.Context> supplier) {
        supplier.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPortalEffects.start(packet.rgb, packet.orientation)));
        supplier.get().setPacketHandled(true);
    }
}

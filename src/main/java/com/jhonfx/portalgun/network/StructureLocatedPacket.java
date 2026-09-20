package com.jhonfx.portalgun.network;

import com.jhonfx.portalgun.client.ClientScreens;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Sends the located structure back to the open gun screen immediately. */
public record StructureLocatedPacket(double x, double y, double z, String dimension, String structure) {
    public static void encode(StructureLocatedPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeUtf(packet.dimension);
        buffer.writeUtf(packet.structure);
    }

    public static StructureLocatedPacket decode(FriendlyByteBuf buffer) {
        return new StructureLocatedPacket(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readUtf(), buffer.readUtf());
    }

    public static void handle(StructureLocatedPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientScreens.structureLocated(packet)));
        context.setPacketHandled(true);
    }
}

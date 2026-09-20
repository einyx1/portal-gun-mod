package com.jhonfx.portalgun.network;

import com.jhonfx.portalgun.client.ClientOmegaEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record OmegaEffectPacket(int durationTicks, float intensity, boolean finalPulse) {
    public static void encode(OmegaEffectPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.durationTicks);
        buffer.writeFloat(packet.intensity);
        buffer.writeBoolean(packet.finalPulse);
    }
    public static OmegaEffectPacket decode(FriendlyByteBuf buffer) {
        return new OmegaEffectPacket(buffer.readVarInt(), buffer.readFloat(), buffer.readBoolean());
    }
    public static void handle(OmegaEffectPacket packet, Supplier<NetworkEvent.Context> supplier) {
        supplier.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientOmegaEffects.start(packet.durationTicks, packet.intensity, packet.finalPulse)));
        supplier.get().setPacketHandled(true);
    }
}

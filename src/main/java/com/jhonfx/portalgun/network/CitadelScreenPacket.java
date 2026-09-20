package com.jhonfx.portalgun.network;

import com.jhonfx.portalgun.client.ClientScreens;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record CitadelScreenPacket(String screen, int credits, int mission, int progress, int target, int reward) {
    public static void encode(CitadelScreenPacket p, FriendlyByteBuf b) {
        b.writeUtf(p.screen); b.writeInt(p.credits); b.writeInt(p.mission); b.writeInt(p.progress); b.writeInt(p.target); b.writeInt(p.reward);
    }
    public static CitadelScreenPacket decode(FriendlyByteBuf b) {
        return new CitadelScreenPacket(b.readUtf(),b.readInt(),b.readInt(),b.readInt(),b.readInt(),b.readInt());
    }
    public static void handle(CitadelScreenPacket p, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx=supplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,() -> () -> ClientScreens.openCitadelService(p)));
        ctx.setPacketHandled(true);
    }
}

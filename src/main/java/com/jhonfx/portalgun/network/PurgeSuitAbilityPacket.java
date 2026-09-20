package com.jhonfx.portalgun.network;

import com.jhonfx.portalgun.event.CombatTechHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Client key input for the equipped Purge Suit. */
public record PurgeSuitAbilityPacket(Action action) {
    public enum Action { CYCLE, FIRE }
    public static void encode(PurgeSuitAbilityPacket packet, FriendlyByteBuf buffer){buffer.writeEnum(packet.action);}
    public static PurgeSuitAbilityPacket decode(FriendlyByteBuf buffer){return new PurgeSuitAbilityPacket(buffer.readEnum(Action.class));}
    public static void handle(PurgeSuitAbilityPacket packet, Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context context=supplier.get();ServerPlayer player=context.getSender();
        if(player!=null)context.enqueueWork(()->CombatTechHandler.handlePurgeInput(player,packet.action==Action.CYCLE));
        context.setPacketHandled(true);
    }
}

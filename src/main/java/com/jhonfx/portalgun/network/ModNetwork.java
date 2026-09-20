package com.jhonfx.portalgun.network;

import com.jhonfx.portalgun.PortalGunMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "3";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(PortalGunMod.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, PortalGunConfigPacket.class,
                PortalGunConfigPacket::encode, PortalGunConfigPacket::decode, PortalGunConfigPacket::handle);
        CHANNEL.registerMessage(1, PortalTravelEffectPacket.class,
                PortalTravelEffectPacket::encode, PortalTravelEffectPacket::decode, PortalTravelEffectPacket::handle);
        CHANNEL.registerMessage(2, StructureLocatedPacket.class,
                StructureLocatedPacket::encode, StructureLocatedPacket::decode, StructureLocatedPacket::handle);
        CHANNEL.registerMessage(3, OmegaEffectPacket.class,
                OmegaEffectPacket::encode, OmegaEffectPacket::decode, OmegaEffectPacket::handle);
        CHANNEL.registerMessage(4, CitadelScreenPacket.class,
                CitadelScreenPacket::encode, CitadelScreenPacket::decode, CitadelScreenPacket::handle);
        CHANNEL.registerMessage(5, CitadelActionPacket.class,
                CitadelActionPacket::encode, CitadelActionPacket::decode, CitadelActionPacket::handle);
        CHANNEL.registerMessage(6, MeeseeksCommandPacket.class,
                MeeseeksCommandPacket::encode, MeeseeksCommandPacket::decode, MeeseeksCommandPacket::handle);
        CHANNEL.registerMessage(7, NavigationSuggestionsPacket.class,
                NavigationSuggestionsPacket::encode, NavigationSuggestionsPacket::decode, NavigationSuggestionsPacket::handle,
                java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(8, NavigationSuggestionsPacket.Reply.class,
                NavigationSuggestionsPacket.Reply::encode, NavigationSuggestionsPacket.Reply::decode, NavigationSuggestionsPacket.Reply::handle,
                java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(9, PurgeSuitAbilityPacket.class,
                PurgeSuitAbilityPacket::encode,PurgeSuitAbilityPacket::decode,PurgeSuitAbilityPacket::handle,
                java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER));
    }
}

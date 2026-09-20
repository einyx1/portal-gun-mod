package com.jhonfx.portalgun.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.jhonfx.portalgun.network.StructureLocatedPacket;
import com.jhonfx.portalgun.network.CitadelScreenPacket;

@OnlyIn(Dist.CLIENT)
public final class ClientScreens {
    private ClientScreens() {
    }

    public static void openPortalGunMenu(InteractionHand hand) {
        Minecraft.getInstance().setScreen(new PortalGunMenuScreen(hand));
    }

    public static void openMeeseeksMenu(InteractionHand hand) {
        Minecraft.getInstance().setScreen(new MeeseeksMenuScreen(hand));
    }

    public static void structureLocated(StructureLocatedPacket result) {
        if (Minecraft.getInstance().screen instanceof PortalGunMenuScreen screen) {
            screen.acceptLocatedStructure(result.x(), result.y(), result.z(), result.dimension(), result.structure());
        }
    }

    public static void openCitadelService(CitadelScreenPacket data) {
        if(Minecraft.getInstance().screen instanceof CitadelServiceScreen screen) screen.update(data);
        else Minecraft.getInstance().setScreen(new CitadelServiceScreen(data));
    }
}

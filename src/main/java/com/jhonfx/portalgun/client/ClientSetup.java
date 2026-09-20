package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.init.ModEntityTypes;
import com.jhonfx.portalgun.init.ModBlockEntityTypes;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import com.jhonfx.portalgun.init.ModBlocks;

public class ClientSetup {

    public static void setup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CITADEL_GLASS.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.FEDERATION_GLASS.get(), RenderType.translucent());
        });
    }

    public static void init(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.PORTAL.get(), PortalEntityRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.DIANE_ROBOT.get(), DianeRobotRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.RICK_PRIME.get(), RickPrimeRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.CITADEL_CITIZEN.get(), CitadelNpcRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.EVIL_MORTY.get(), EvilMortyRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.EVIL_MORTY_ALLY.get(), EvilMortyRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.FEDERATION_ALIEN.get(), FederationAlienRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.FEDERATION_DRONE.get(), FederationDroneRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.FEDERATION_HEAVY.get(), FederationHeavyRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.MEESEEKS.get(), MeeseeksRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.CURVE_NODE.get(), CurveNodeRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.RICK_SHIP.get(), RickShipRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.PORTAL_SOLUTION.get(),
                context -> new PortalSolutionRenderer());
        event.registerBlockEntityRenderer(ModBlockEntityTypes.OMEGA_MACHINE.get(),
                context -> new OmegaMachineRenderer());
    }
}

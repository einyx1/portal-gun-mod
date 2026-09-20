package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.PortalGunMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = PortalGunMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ClientSetup.init(event);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        ClientSetup.setup(event);
    }

    @SubscribeEvent public static void registerKeys(RegisterKeyMappingsEvent event){
        event.register(CombatKeybinds.CYCLE);event.register(CombatKeybinds.FIRE);
    }

    private ClientModEvents() {}
}

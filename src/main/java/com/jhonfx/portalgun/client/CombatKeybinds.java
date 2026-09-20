package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.PortalGunMod;
import com.jhonfx.portalgun.network.ModNetwork;
import com.jhonfx.portalgun.network.PurgeSuitAbilityPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid=PortalGunMod.MOD_ID,value=Dist.CLIENT)
public final class CombatKeybinds {
    public static final String CATEGORY="key.categories.portalgun.combat";
    public static final KeyMapping CYCLE=new KeyMapping("key.portalgun.purge_cycle",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_G,CATEGORY);
    public static final KeyMapping FIRE=new KeyMapping("key.portalgun.purge_fire",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_V,CATEGORY);

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        while(CYCLE.consumeClick())ModNetwork.CHANNEL.sendToServer(new PurgeSuitAbilityPacket(PurgeSuitAbilityPacket.Action.CYCLE));
        while(FIRE.consumeClick())ModNetwork.CHANNEL.sendToServer(new PurgeSuitAbilityPacket(PurgeSuitAbilityPacket.Action.FIRE));
    }
    private CombatKeybinds(){}
}

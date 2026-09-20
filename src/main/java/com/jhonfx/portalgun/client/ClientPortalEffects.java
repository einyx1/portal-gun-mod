package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.PortalGunMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PortalGunMod.MOD_ID, value = Dist.CLIENT)
public final class ClientPortalEffects {
    private static final long EFFECT_MS = 180;
    private static int color;
    private static long startedAt;
    private static int orientation;

    public static void start(int rgb, int portalOrientation) {
        color = rgb; orientation = portalOrientation; startedAt = System.currentTimeMillis();
    }

    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {
        long elapsed = System.currentTimeMillis() - startedAt;
        if (elapsed < 0 || elapsed > EFFECT_MS) return;
        float phase = elapsed / (float) EFFECT_MS;
        int alpha = (int) (105 * (1.0f - phase));
        // Slightly stronger from floor/ceiling for a visibly different traversal animation.
        if (orientation == 1) alpha = Math.min(190, alpha + 25);
        event.getGuiGraphics().fill(0, 0, event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight(), (alpha << 24) | (color & 0xFFFFFF));
    }

    private ClientPortalEffects() {}
}

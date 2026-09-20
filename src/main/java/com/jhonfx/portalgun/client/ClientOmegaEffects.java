package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.PortalGunMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PortalGunMod.MOD_ID, value = Dist.CLIENT)
public final class ClientOmegaEffects {
    private static long endsAt;
    private static long startedAt;
    private static float intensity;
    private static boolean finalPulse;

    public static void start(int durationTicks, float strength, boolean pulse) {
        startedAt = System.currentTimeMillis();
        endsAt = startedAt + durationTicks * 50L;
        intensity = strength;
        finalPulse = pulse;
    }

    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {
        long now = System.currentTimeMillis();
        if (now >= endsAt) return;
        float remaining = Math.min(1f, (endsAt - now) / Math.max(1f, endsAt - startedAt));
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        int alpha = (int) ((finalPulse ? 80 : 28) * intensity * remaining);
        int shift = Math.max(1, (int) (4 * intensity * remaining));
        int energyColor = finalPulse ? 0x18FF65 : 0x19CFFF;
        event.getGuiGraphics().fill(shift, 0, width, height, (alpha << 24) | energyColor);
        event.getGuiGraphics().fill(0, shift, width - shift, height, ((alpha / 2) << 24) | 0xF4FFFF);
        if (finalPulse) event.getGuiGraphics().fill(0, 0, width, height,
                ((int) (105 * remaining) << 24) | 0xDFFFFF);
    }

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles event) {
        long now = System.currentTimeMillis();
        if (now >= endsAt) return;
        double time = now / 35.0;
        float amount = intensity * (finalPulse ? 2.8f : 0.75f);
        event.setYaw(event.getYaw() + (float) Math.sin(time) * amount);
        event.setPitch(event.getPitch() + (float) Math.cos(time * 1.31) * amount * 0.65f);
        event.setRoll(event.getRoll() + (float) Math.sin(time * 0.73) * amount * 0.45f);
    }

    private ClientOmegaEffects() {}
}

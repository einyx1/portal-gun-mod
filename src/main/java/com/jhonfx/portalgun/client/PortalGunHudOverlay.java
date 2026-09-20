package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.PortalGunMod;
import com.jhonfx.portalgun.item.PortalGunItem;
import com.jhonfx.portalgun.item.PortalGunVariant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Original Bedrock charge bar. Saved locations are destinations, not world markers. */
@Mod.EventBusSubscriber(modid = PortalGunMod.MOD_ID, value = Dist.CLIENT)
public final class PortalGunHudOverlay {
    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.screen instanceof PortalGunMenuScreen) return;
        ItemStack stack = minecraft.player.getMainHandItem();
        PortalGunItem gun;
        if (stack.getItem() instanceof PortalGunItem mainHandGun) {
            gun = mainHandGun;
        } else {
            stack = minecraft.player.getOffhandItem();
            if (!(stack.getItem() instanceof PortalGunItem offhandGun)) return;
            gun = offhandGun;
        }
        renderCharge(event.getGuiGraphics(), event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight(), stack, gun.variant());
    }

    private static void renderCharge(GuiGraphics graphics, int width, int height,
                                     ItemStack stack, PortalGunVariant variant) {
        String color = switch (variant.color()) { case BLUE -> "blue"; case GREEN -> "green"; case YELLOW -> "yellow"; };
        ResourceLocation background = texture("hud/" + color + "_charge_bar_bg.png");
        ResourceLocation full = texture("hud/" + color + "_charge_bar_full.png");
        int x = width / 2 - 126, y = height - 66;
        graphics.blit(background, x, y, 7, 62, 0, 0, 7, 62, 7, 62);
        int filled = Mth.clamp(Math.round(51.0f * PortalGunItem.getCharge(stack) / 1000.0f), 0, 51);
        if (filled > 0) {
            graphics.enableScissor(x + 1, y + 51 - filled, x + 6, y + 51);
            graphics.blit(full, x + 1, y, 5, 51, 0, 0, 5, 51, 5, 51);
            graphics.disableScissor();
        }
    }

    private static ResourceLocation texture(String path) {
        return new ResourceLocation(PortalGunMod.MOD_ID, "textures/gui/" + path);
    }

    private PortalGunHudOverlay() {}
}

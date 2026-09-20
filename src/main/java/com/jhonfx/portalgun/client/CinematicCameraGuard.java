package com.jhonfx.portalgun.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.thewinnt.cutscenes.client.ClientCutsceneManager;
import net.thewinnt.cutscenes.event.EndingReason;

/** Keeps the last ordinary camera state; only supervises this mod's scenes. */
@Mod.EventBusSubscriber(modid = "portalgun", value = Dist.CLIENT)
public final class CinematicCameraGuard {
    private static ClientLevel savedLevel;
    private static Entity savedCamera;
    private static CameraType perspective;
    private static int fov, ticks;
    private static float yaw, pitch;
    private static boolean hideGui, active;

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getInstance();
        var scene = ClientCutsceneManager.runningCutscene;
        var id = scene == null ? null : ClientCutsceneManager.CLIENT_REGISTRY.inverse().get(scene.cutscene);
        boolean ours = id != null && id.getNamespace().equals("portalgun");
        if (ours && !active) {
            active = true;
            ticks = 0;
        }
        if (active) {
            boolean broken = mc.player == null || mc.level != savedLevel || !mc.player.isAlive()
                    || ++ticks > 1200 || (ours && ClientCutsceneManager.camera == null);
            if (broken && ours) ClientCutsceneManager.stopCutsceneImmediate(EndingReason.INTERRUPT);
            if (broken || scene == null) {
                if (perspective != null) {
                    mc.options.setCameraType(perspective);
                    mc.options.fov().set(fov);
                    mc.options.hideGui = hideGui;
                }
                if (mc.player != null) {
                    mc.setCameraEntity(savedCamera != null && savedCamera.isAlive()
                            && savedCamera.level() == mc.level ? savedCamera : mc.player);
                    if (savedLevel == mc.level) {
                        mc.player.setYRot(yaw);
                        mc.player.setXRot(pitch);
                    }
                }
                active = false;
                savedCamera = null;
                savedLevel = null;
            } else if (!ours) {
                // Another mod took ownership; do not overwrite its camera.
                active = false;
            }
        }
        if (!active && scene == null && mc.player != null && mc.level != null) {
            savedLevel = mc.level;
            savedCamera = mc.getCameraEntity();
            perspective = mc.options.getCameraType();
            fov = mc.options.fov().get();
            hideGui = mc.options.hideGui;
            yaw = mc.player.getYRot();
            pitch = mc.player.getXRot();
        }
    }
}

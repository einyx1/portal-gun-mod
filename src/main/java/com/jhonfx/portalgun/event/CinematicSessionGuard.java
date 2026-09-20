package com.jhonfx.portalgun.event;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.thewinnt.cutscenes.CutsceneManager;

/** Actor lifetime/dimension watchdog shared by both boss introductions. */
public final class CinematicSessionGuard {
    private record Session(Entity actor, long deadline) {}
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    public static void start(ResourceLocation scene, Vec3 origin, ServerPlayer viewer, Entity actor) {
        if (!CutsceneManager.REGISTRY.containsKey(scene)) return;
        // Never stack two camera owners on the same client. Apart from visual
        // snapping, Cutscene API 1.6.5 can leave the previous path active.
        if (SESSIONS.remove(viewer.getUUID()) != null) CutsceneManager.stopCutscene(viewer);
        CutsceneManager.startCutscene(scene, origin, Vec3.ZERO, Vec3.ZERO, viewer);
        SESSIONS.put(viewer.getUUID(), new Session(actor, viewer.server.overworld().getGameTime() + 220));
    }

    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var iterator = SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var session = entry.getValue();
            var server = session.actor.getServer();
            ServerPlayer viewer = server == null ? null : server.getPlayerList().getPlayer(entry.getKey());
            if (viewer == null || !viewer.isAlive() || !session.actor.isAlive()
                    || viewer.level() != session.actor.level() || server.overworld().getGameTime() >= session.deadline) {
                if (viewer != null) CutsceneManager.stopCutscene(viewer);
                iterator.remove();
            }
        }
    }

    @SubscribeEvent public void stopped(ServerStoppedEvent event) { SESSIONS.clear(); }
}

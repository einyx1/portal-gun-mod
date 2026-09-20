package com.jhonfx.portalgun.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * CURVE NODE — a rounded technological sphere that makes up the visible
 * lattice of the Finite Curve. Replaces the old square armor-stand nodes.
 *
 * Uses a GeckoLib geometry (geo/curve_node_sphere.geo.json) built from
 * three interlocking rotated rings around a glowing core, which reads as
 * a genuinely rounded orb instead of a floating block head.
 *
 * Each node continuously:
 *   - slowly rotates in place (handled client-side by the animation)
 *   - bobs gently on a sine wave
 *   - draws a thin particle tether to its "next" node (by UUID) to visually
 *     connect the lattice, replacing the previous static/disconnected look.
 */
public final class CurveNodeEntity extends Entity implements GeoEntity {
    private static final EntityDataAccessor<Integer> RING =
            SynchedEntityData.defineId(CurveNodeEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("animation.curve_node.spin");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final DustParticleOptions TETHER =
            new DustParticleOptions(new Vector3f(1.0f, 0.85f, 0.15f), 0.9f);

    private double baseY;
    private double bobPhase;
    private UUID nextNodeId;   // the node this one is "connected" to for tether rendering

    public CurveNodeEntity(EntityType<?> type, Level level) { super(type, level); noPhysics = true; }

    @Override protected void defineSynchedData() {
        entityData.define(RING, 0);
    }

    public void setRing(int ring) { entityData.set(RING, ring); }
    public int  getRing()         { return entityData.get(RING); }

    public void setBase(double x, double y, double z) {
        this.baseY = y;
        setPos(x, y, z);
        this.bobPhase = (x + z) * 0.7;
    }

    public void linkTo(CurveNodeEntity other) { this.nextNodeId = other.getUUID(); }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) return;

        // When the Curve is cut, the physical lattice lifts clear of the arena
        // before Evil Morty arrives. Keeping the nodes as entities preserves the
        // intended visual while preventing them from obscuring combat.
        if (level() instanceof ServerLevel server) {
            com.jhonfx.portalgun.curve.CurveSavedData curve =
                    com.jhonfx.portalgun.curve.CurveSavedData.get(server);
            if ((curve.breaking() || curve.battleStarted()) && !curve.broken()) {
                if (getY() < baseY + 34.0) setPos(getX(), getY() + 0.58, getZ());
                return;
            }
        }

        // Gentle bob
        // Visual motion belongs to the renderer; no position updates every server tick.

        // Tether particles to next node
        if (nextNodeId != null && (tickCount + getId()) % 40 == 0 && level() instanceof ServerLevel server) {
            Entity other = server.getEntity(nextNodeId);
            if (other != null && other.isAlive()) {
                Vec3 from = position().add(0, 0.05, 0);
                Vec3 to   = other.position().add(0, 0.05, 0);
                Vec3 delta = to.subtract(from);
                int steps = Math.min(5, Math.max(2, (int) delta.length()));
                for (int i = 0; i <= steps; i++) {
                    Vec3 p = from.add(delta.scale(i / (double) steps));
                    server.sendParticles(TETHER, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                }
            }
        }
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        baseY = tag.getDouble("BaseY");
        bobPhase = tag.getDouble("BobPhase");
        entityData.set(RING, tag.getInt("Ring"));
        if (tag.hasUUID("NextNode")) nextNodeId = tag.getUUID("NextNode");
    }

    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("BaseY", baseY);
        tag.putDouble("BobPhase", bobPhase);
        tag.putInt("Ring", getRing());
        if (nextNodeId != null) tag.putUUID("NextNode", nextNodeId);
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    public boolean isCurveNode() { return true; }

    // ── GeckoLib ──────────────────────────────────────────────────────────────
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "spin", 0,
                state -> state.setAndContinue(SPIN)));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}

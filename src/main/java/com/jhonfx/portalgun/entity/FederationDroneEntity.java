package com.jhonfx.portalgun.entity;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * FEDERATION DRONE — flying surveillance/combat unit.
 *
 * Deployed at wanted level 4+. Hovers, scans, and fires energy bolts.
 * Much lower HP than ground troops but harder to hit due to flight.
 */
public final class FederationDroneEntity extends Monster implements GeoEntity, RangedAttackMob {
    private static final RawAnimation HOVER = RawAnimation.begin().thenLoop("animation.federation_drone.hover");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public FederationDroneEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.xpReward = 8;
        this.noPhysics = false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16)
                .add(Attributes.MOVEMENT_SPEED, .32)
                .add(Attributes.FLYING_SPEED, .55)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.ARMOR, 2);
    }

    @Override protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new RangedAttackGoal(this, 1.0, 40, 16));
        goalSelector.addGoal(2, new HoverGoal(this));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override public void performRangedAttack(LivingEntity target, float power) {
        if (level().isClientSide) return;
        ShulkerBullet bolt = new ShulkerBullet(level(), this, target, Direction.Axis.Y);
        bolt.setPos(getX(), getEyeY(), getZ());
        level().addFreshEntity(bolt);
        level().playSound(null, blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 0.7f, 1.9f);
    }

    @Override public void aiStep() {
        super.aiStep();
        if (!level().isClientSide) {
            // Idle hover bob
            setDeltaMovement(getDeltaMovement().add(0, Math.sin(tickCount * 0.1) * 0.002, 0));
        }
    }

    @Override public boolean canBeLeashed(Player player) { return false; }
    @Override protected boolean canRide(net.minecraft.world.entity.Entity vehicle) { return false; }
    @Override public boolean isFlapping() { return true; }

    /** Simple hover-in-place goal — keeps drone airborne at a target height above ground/player. */
    static final class HoverGoal extends Goal {
        private final FederationDroneEntity drone;
        HoverGoal(FederationDroneEntity drone) { this.drone = drone; setFlags(java.util.EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() { return true; }
        @Override public void tick() {
            LivingEntity target = drone.getTarget();
            if (target == null) return;
            double desiredY = target.getY() + 4.5;
            double dx = target.getX() - drone.getX();
            double dz = target.getZ() - drone.getZ();
            double dist = Math.sqrt(dx*dx + dz*dz);
            if (dist > 14) drone.getMoveControl().setWantedPosition(target.getX(), desiredY, target.getZ(), 1.0);
            else if (dist < 8) drone.getMoveControl().setWantedPosition(
                    drone.getX() - dx * 0.3, desiredY, drone.getZ() - dz * 0.3, 1.0);
            else drone.getMoveControl().setWantedPosition(drone.getX(), desiredY, drone.getZ(), 0.5);
        }
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 0, state -> state.setAndContinue(HOVER)));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}

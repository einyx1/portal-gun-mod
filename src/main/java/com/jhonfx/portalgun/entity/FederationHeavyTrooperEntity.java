package com.jhonfx.portalgun.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * FEDERATION HEAVY TROOPER — armored tank unit.
 *
 * Deployed only at wanted level 5 as part of capture operations.
 * Slow but extremely tanky; ground-slam AoE knocks back and damages
 * everything in a radius when it lands a melee hit.
 */
public final class FederationHeavyTrooperEntity extends Monster implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.federation_heavy.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.federation_heavy.walk");
    private static final RawAnimation SLAM = RawAnimation.begin().thenPlay("animation.federation_heavy.slam");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int slamCooldown;

    public FederationHeavyTrooperEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120)
                .add(Attributes.MOVEMENT_SPEED, .18)
                .add(Attributes.ATTACK_DAMAGE, 14)
                .add(Attributes.ARMOR, 16)
                .add(Attributes.KNOCKBACK_RESISTANCE, .95)
                .add(Attributes.FOLLOW_RANGE, 30);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, .7));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override public void tick() {
        super.tick();
        if (slamCooldown > 0) slamCooldown--;
        if (!level().isClientSide && getTarget() != null && distanceToSqr(getTarget()) < 5 && slamCooldown <= 0) {
            groundSlam();
            slamCooldown = 100;
        }
    }

    private void groundSlam() {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel server)) return;
        server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.1, getZ(), 1, 0, 0, 0, 0);
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI * 2 / 24.0;
            server.sendParticles(ParticleTypes.CRIT, getX() + Math.cos(a)*2, getY()+0.2, getZ() + Math.sin(a)*2,
                    1, 0.05, 0.05, 0.05, 0.01);
        }
        level().playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.4f, 0.6f);
        for (var target : level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                new AABB(blockPosition()).inflate(3.5), e -> e != this && e.isAlive())) {
            target.hurt(damageSources().mobAttack(this), 6.0f);
            double dx = target.getX() - getX(), dz = target.getZ() - getZ();
            double dist = Math.max(0.1, Math.sqrt(dx*dx+dz*dz));
            target.push(dx/dist * 0.8, 0.45, dz/dist * 0.8);
        }
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 4, state -> {
            state.setAnimation(state.isMoving() ? WALK : IDLE);
            return software.bernie.geckolib.core.object.PlayState.CONTINUE;
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}

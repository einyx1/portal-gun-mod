package com.jhonfx.portalgun.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class DianeRobotEntity extends Monster implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.diane_robot.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.diane_robot.walk");
    private static final DustParticleOptions CYAN = new DustParticleOptions(new Vector3f(0.0f, 0.9f, 1.0f), 1.35f);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.literal("DIANE DEFENSE UNIT"),
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);

    public DianeRobotEntity(EntityType<? extends DianeRobotEntity> type, Level level) {
        super(type, level);
        xpReward = 55;
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 240).add(Attributes.ARMOR, 18)
                .add(Attributes.ATTACK_DAMAGE, 18).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9).add(Attributes.FOLLOW_RANGE, 48);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.7));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (tickCount % 4 == 0) {
            double angle=tickCount*0.14;
            ((net.minecraft.server.level.ServerLevel)level()).sendParticles(CYAN,
                    getX()+Math.cos(angle)*2.2,getY()+3.0,getZ()+Math.sin(angle)*2.2,
                    4,0.05,0.12,0.05,0.01);
        }
        if (tickCount % 35 != 0) return;
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || distanceToSqr(target) > 30 * 30) return;
        Vec3 start = getEyePosition(); Vec3 end = target.getEyePosition();
        for (int i=0;i<=28;i++) { Vec3 p=start.lerp(end,i/28.0); ((net.minecraft.server.level.ServerLevel)level()).sendParticles(CYAN,p.x,p.y,p.z,1,0.01,0.01,0.01,0); }
        target.hurt(damageSources().lightningBolt(), 9.0f);
        level().playSound(null, blockPosition(), SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE, 1.6f, 0.65f);
    }

    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setProgress(getHealth()/getMaxHealth());
    }
    @Override public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player); bossEvent.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); bossEvent.removePlayer(player); }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this,"movement",3,s -> s.setAndContinue(s.isMoving()?WALK:IDLE)));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}

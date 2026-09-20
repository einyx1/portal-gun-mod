package com.jhonfx.portalgun.entity;

import com.jhonfx.portalgun.init.ModEntityTypes;
import com.jhonfx.portalgun.init.ModItems;
import com.jhonfx.portalgun.init.ModSounds;
import com.jhonfx.portalgun.omega.OmegaDeviceHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.thewinnt.cutscenes.CutsceneManager;

/**
 * RICK PRIME — Boss Battle
 *
 * Phases:
 *   INTRO          awaitingIntro=true → cinematic portal entry → lands on center platform
 *   PHASE 1  100%→76%  melee + dimensional shots + portal blink
 *   PHASE 2   76%→52%  +2 clones, absorption
 *   DIANE     52%       retreats to high balcony, Diane Defense Unit spawns
 *   PHASE 3   52%→34%  returns, +3 clones, faster blink
 *   FINAL     34%→0%   +4 clones, full speed, rift barrage, rapid fire
 */
public class RickPrimeEntity extends Monster implements GeoEntity {
    // Synced state
    private static final EntityDataAccessor<Boolean> CLONE =
            SynchedEntityData.defineId(RickPrimeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RETREATING =
            SynchedEntityData.defineId(RickPrimeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(RickPrimeEntity.class, EntityDataSerializers.INT);

    // Particles
    private static final DustParticleOptions PRIME_GREEN =
            new DustParticleOptions(new Vector3f(0.15f, 1.0f, 0.28f), 1.2f);
    private static final DustParticleOptions PRIME_DARK =
            new DustParticleOptions(new Vector3f(0.05f, 0.55f, 0.12f), 1.4f);

    // Animations
    private static final RawAnimation IDLE  = RawAnimation.begin().thenLoop("animation.rick_prime.idle");
    private static final RawAnimation WALK  = RawAnimation.begin().thenLoop("animation.rick_prime.walk");
    private static final RawAnimation SHOOT = RawAnimation.begin().thenPlay("animation.rick_prime.shoot");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.literal("RICK PRIME"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.NOTCHED_10);

    // State flags
    private boolean secondPhase, finalPhase, dianePhase;
    private boolean awaitingIntro;
    private int introTicks;
    private BlockPos battleCenter;
    private java.util.UUID introEntryPortal, introExitPortal;

    // Attack cooldown tracking
    private int shotCooldown, blinkCooldown, barrageCooldown;
    private int shotWindup;
    private Vec3 shotAim;
    private int shockwaveCooldown = 120, shockwaveTicks, combatPortalTicks, deathSequenceTicks;
    private java.util.UUID combatEntryPortal, combatExitPortal;
    private DamageSource deathSource;
    private final java.util.Set<java.util.UUID> shockwaveHits = new java.util.HashSet<>();

    public RickPrimeEntity(EntityType<? extends RickPrimeEntity> type, Level level) {
        super(type, level);
        xpReward = 80;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 360.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 14.0)
                .add(Attributes.ARMOR, 14.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.80)
                .add(Attributes.FOLLOW_RANGE, 52.0);
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(CLONE, false);
        entityData.define(RETREATING, false);
        entityData.define(PHASE, 1);
    }

    public boolean isClone() { return entityData.get(CLONE); }
    public int getPhase()    { return entityData.get(PHASE); }

    public void setClone(boolean clone) {
        entityData.set(CLONE, clone);
        if (clone) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(42.0);
            setHealth(42.0f);
            xpReward = 0;
        }
    }

    private void setPhase(int phase) { entityData.set(PHASE, phase); }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.85));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 28));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public void configureArena(BlockPos center) {
        setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                new net.minecraft.world.item.ItemStack(ModItems.PRIME_PORTAL_GUN.get()));
        setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.0f);
        battleCenter = center.immutable();
        awaitingIntro = true;
        setNoAi(true);
        setInvulnerable(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Override public void tick() {
        super.tick();
        if (level().isClientSide || !isAlive() || !(level() instanceof ServerLevel server)) return;
        if (deathSequenceTicks > 0) { tickDeathSequence(server); return; }

        if (combatPortalTicks > 0 && --combatPortalTicks == 0) {
            CinematicPortal.close(server, combatEntryPortal);
            CinematicPortal.close(server, combatExitPortal);
            combatEntryPortal = combatExitPortal = null;
        }

        if (!isClone() && awaitingIntro) { tickIntro(server); return; }
        if (!isClone() && entityData.get(RETREATING)) { tickDianeIntermission(server); return; }

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            target = server.getNearestPlayer(this, 52.0);
            if (target != null) setTarget(target);
            else return;
        }

        // Decrease cooldowns
        if (shotCooldown > 0) shotCooldown--;
        if (blinkCooldown > 0) blinkCooldown--;
        if (barrageCooldown > 0) barrageCooldown--;
        if (shockwaveCooldown > 0) shockwaveCooldown--;

        int shotRate    = isClone() ? 45 : (finalPhase ? 18 : 28);
        int blinkRate   = finalPhase ? 40 : 70;
        int barrageRate = finalPhase ? 60 : 110;

        if (shotWindup > 0) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, shotAim.x, shotAim.y, shotAim.z, 2, .12, .12, .12, 0);
            if (--shotWindup == 0) fireDimensionalShot(server, target);
        } else if (shotCooldown <= 0 && hasLineOfSight(target)) {
            shotAim = target.getEyePosition(); shotWindup = finalPhase ? 10 : 16; shotCooldown = shotRate;
            server.playSound(null, blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, .8f, 1.7f);
        }
        if (!isClone() && blinkCooldown <= 0)   { portalBlink(server, target);       blinkCooldown = blinkRate; }
        if (!isClone() && barrageCooldown <= 0) { dimensionalRiftBarrage(server, target); barrageCooldown = barrageRate; }
        if (!isClone() && getPhase() >= 2 && shockwaveCooldown <= 0 && shockwaveTicks == 0) {
            shockwaveTicks = 1; shockwaveHits.clear(); shockwaveCooldown = finalPhase ? 110 : 170;
            server.playSound(null, blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 2f, .55f);
        }
        if (shockwaveTicks > 0) tickShockwave(server);

        if (!isClone() && generatorsActive(server) && tickCount % 10 == 0) renderGeneratorShield(server);

        // Ambient green glow near boss
        if (tickCount % 4 == 0) ambientParticles(server);

        // ── Phase transitions ──────────────────────────────────────────────
        float hp = getHealth() / getMaxHealth();

        if (!isClone() && !secondPhase && hp <= 0.76f) enterPhase2(server);
        if (!isClone() && !dianePhase  && hp <= 0.52f) enterDianePhase(server, target);
        if (!isClone() && !finalPhase  && hp <= 0.34f) enterFinalPhase(server);
    }

    // ── Phase transitions ─────────────────────────────────────────────────────
    private void enterPhase2(ServerLevel level) {
        secondPhase = true;
        setPhase(2);
        summonClones(level, 2);
        addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 2, false, false));
        broadcastBoss(level, "RICK PRIME: ISSO É IRRITANTE.");
        level.playSound(null, blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 2.0f, 0.75f);
    }

    private void enterDianePhase(ServerLevel level, LivingEntity target) {
        dianePhase = true;
        setPhase(3);
        summonDianeRobot(level, target);
    }

    private void enterFinalPhase(ServerLevel level) {
        finalPhase = true;
        setPhase(4);
        summonClones(level, 4);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.42);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(18.0);
        addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1, false, false));
        bossEvent.setColor(BossEvent.BossBarColor.PURPLE);
        broadcastBoss(level, "RICK PRIME: CHEGA DE BRINCAR.");
        // One batched energy bloom replaces sixty separate FLASH packets.
        Vec3 c = Vec3.atCenterOf(battleCenter != null ? battleCenter : blockPosition());
        level.sendParticles(PRIME_GREEN,c.x,c.y+1,c.z,72,15,.7,15,.035);
        level.sendParticles(ParticleTypes.FLASH,c.x,c.y+1,c.z,3,5,.2,5,0);
        level.playSound(null, blockPosition(), SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 3.0f, 0.55f);
    }

    // ── Intro cinematic ───────────────────────────────────────────────────────
    private void tickIntro(ServerLevel level) {
        Player player = level.getNearestPlayer(this, 30.0);
        if (player == null) return;
        introTicks++;

        Vec3 center = battleCenter != null
                ? Vec3.atCenterOf(battleCenter)
                : position();

        if (introTicks == 1) {
            // Start cutscene for nearby players
            ResourceLocation scene = new ResourceLocation("portalgun", "rick_prime_intro");
            if (CutsceneManager.REGISTRY.containsKey(scene)) {
                for (ServerPlayer viewer : level.players()) {
                    if (viewer.distanceToSqr(center) < 40 * 40)
                        com.jhonfx.portalgun.event.CinematicSessionGuard.start(scene, center, viewer, this);
                }
            }
            broadcastBoss(level, "RICK PRIME: VOCÊ ENTROU NA BASE ERRADA.");
        }

        // Tick 24: spawn entry portal above Rick (outside arena) + exit portal at center
        if (introTicks == 24) {
            Vec3 rp = position();
            // Entry portal: above Rick's current position (he's on the upper balcony)
            introEntryPortal = CinematicPortal.spawn(level,
                    rp.add(0, 0.5, 0), Direction.UP, PortalColor.GREEN, 0.80f);
            // Exit portal: at center platform, facing up so Rick lands on it
            introExitPortal = CinematicPortal.spawn(level,
                    center.add(0, 0.5, 0), Direction.UP, PortalColor.GREEN, 0.80f);
            level.playSound(null, blockPosition(), ModSounds.PORTAL_SPAWN.get(),
                    SoundSource.HOSTILE, 2.0f, 0.75f);
        }

        // Linking now lets PortalTickHandler transport the actor through the real pair.
        if (introTicks == 48) {
            CinematicPortal.activate(level, introEntryPortal, introExitPortal, this);
            level.playSound(null, blockPosition(), SoundEvents.BEACON_ACTIVATE,
                    SoundSource.HOSTILE, 1.8f, 0.62f);
        }

        // Tick 72: Rick appears exactly at center platform
        if (introTicks == 72) {
            setInvisible(false);
            CinematicPortal.close(level, introEntryPortal);
            CinematicPortal.close(level, introExitPortal);
            // Landing particles
            level.sendParticles(PRIME_GREEN, center.x, center.y + 0.5, center.z, 80, 1.2, 0.4, 1.2, 0.08);
            level.playSound(null, blockPosition(), ModSounds.PORTAL_SPAWN.get(),
                    SoundSource.HOSTILE, 1.6f, 1.1f);
        }

        // Tick 90: battle begins
        if (introTicks >= 120) {
            setInvisible(false);
            awaitingIntro = false;
            setNoAi(false);
            setInvulnerable(false);
            setTarget(player);
            broadcastBoss(level, "RICK PRIME: HORA DE MORRER.");
        }
    }

    // ── Diane intermission ────────────────────────────────────────────────────
    private void summonDianeRobot(ServerLevel level, LivingEntity target) {
        DianeRobotEntity robot = ModEntityTypes.DIANE_ROBOT.get().create(level);
        if (robot == null) return;
        Vec3 center = Vec3.atCenterOf(battleCenter != null ? battleCenter : blockPosition());
        robot.moveTo(center.x, center.y + 1, center.z, getYRot(), 0);
        robot.setTarget(target);
        robot.setPersistenceRequired();
        level.addFreshEntity(robot);

        entityData.set(RETREATING, true);
        setNoAi(true);
        setInvulnerable(true);
        setTarget(null);
        // Rick retreats to high balcony (center + y17, back wall)
        teleportTo(center.x, center.y + 18, center.z - 10);
        level.sendParticles(PRIME_GREEN, center.x, center.y + 2, center.z, 60, 1, 1, 1, 0.1);
        level.playSound(null, battleCenter != null ? battleCenter : blockPosition(),
                SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2.5f, 0.60f);
        broadcastBoss(level, "DIANE DEFENSE UNIT: ONLINE");
    }

    private void tickDianeIntermission(ServerLevel level) {
        BlockPos center = battleCenter != null ? battleCenter : blockPosition();
        boolean robotAlive = !level.getEntitiesOfClass(DianeRobotEntity.class,
                new net.minecraft.world.phys.AABB(center).inflate(58), e -> e.isAlive()).isEmpty();
        if (robotAlive) return;

        entityData.set(RETREATING, false);
        setNoAi(false);
        setInvulnerable(false);
        Vec3 c = Vec3.atCenterOf(center).add(0, 1, 0);
        teleportTo(c.x, c.y, c.z);
        Player player = level.getNearestPlayer(this, 52);
        if (player != null) setTarget(player);
        summonClones(level, 3);
        level.sendParticles(PRIME_GREEN, c.x, c.y + 1, c.z, 100, 1.5, 1.5, 1.5, 0.12);
        broadcastBoss(level, "RICK PRIME: MEU TURNO.");
    }

    // ── Attacks ───────────────────────────────────────────────────────────────
    /** Hitscan dimensional beam: particles + instant damage */
    private void fireDimensionalShot(ServerLevel level, LivingEntity target) {
        Vec3 start = getEyePosition();
        Vec3 end   = shotAim == null ? target.getEyePosition() : shotAim;
        Vec3 delta = end.subtract(start);
        int steps = Math.max(1, (int) Math.min(40, delta.length() * 3));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = start.add(delta.scale(i / (double) steps));
            level.sendParticles(i % 5 == 0 ? ParticleTypes.ELECTRIC_SPARK : PRIME_GREEN,
                    point.x, point.y, point.z, 1, 0.01, 0.01, 0.01, 0.0);
        }
        float dmg = isClone() ? 4.0f : (finalPhase ? 11.0f : 7.0f);
        if (hasLineOfSight(target) && target.getEyePosition().distanceToSqr(end) < 2.25)
            target.hurt(damageSources().magic(), dmg);
        level.playSound(null, blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.HOSTILE, isClone() ? 0.6f : 1.1f, finalPhase ? 1.6f : 1.15f);
    }

    /** Portal-blink teleport around target */
    private void portalBlink(ServerLevel level, LivingEntity target) {
        Vec3 old = position();
        double angle  = random.nextDouble() * Math.PI * 2;
        double radius = finalPhase ? (3.5 + random.nextDouble() * 4.0) : (5.0 + random.nextDouble() * 6.0);
        Vec3 next = target.position().add(Math.cos(angle) * radius, 0.1, Math.sin(angle) * radius);

        CinematicPortal.close(level, combatEntryPortal);
        CinematicPortal.close(level, combatExitPortal);
        combatEntryPortal = CinematicPortal.spawn(level, old.add(0, .05, 0), Direction.UP, PortalColor.GREEN, .65f);
        combatExitPortal = CinematicPortal.spawn(level, next.add(0, .05, 0), Direction.UP, PortalColor.GREEN, .65f);
        if (CinematicPortal.activate(level, combatEntryPortal, combatExitPortal, this)) combatPortalTicks = 14;
        level.playSound(null, blockPosition(), ModSounds.PORTAL_SPAWN.get(), SoundSource.HOSTILE, 1.4f, 1.35f);
    }

    private void tickShockwave(ServerLevel level) {
        Vec3 center = Vec3.atCenterOf(battleCenter != null ? battleCenter : blockPosition());
        double radius = shockwaveTicks * .72;
        int points = 48;
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points;
            level.sendParticles(i % 5 == 0 ? ParticleTypes.ELECTRIC_SPARK : PRIME_GREEN,
                    center.x + Math.cos(angle) * radius, center.y + .15,
                    center.z + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
        }
        for (ServerPlayer player : level.players()) {
            double horizontal = Math.sqrt(player.distanceToSqr(center.x, player.getY(), center.z));
            if (Math.abs(horizontal - radius) <= 1.1 && player.onGround()
                    && shockwaveHits.add(player.getUUID())) {
                player.hurt(damageSources().mobAttack(this), finalPhase ? 10 : 7);
                Vec3 away = player.position().subtract(center).multiply(1, 0, 1).normalize();
                player.push(away.x * 1.15, .35, away.z * 1.15);
            }
        }
        if (++shockwaveTicks > 31) { shockwaveTicks = 0; shockwaveHits.clear(); }
    }

    private boolean generatorsActive(ServerLevel level) {
        BlockPos center = battleCenter != null ? battleCenter : blockPosition();
        for (BlockPos pos : new BlockPos[]{center.offset(18, 0, 0), center.offset(-18, 0, 0),
                center.offset(0, 0, 18), center.offset(0, 0, -18)})
            if (level.getBlockState(pos).is(com.jhonfx.portalgun.init.ModBlocks.OMEGA_ALARM_PANEL.get())) return true;
        return false;
    }

    private void renderGeneratorShield(ServerLevel level) {
        Vec3 target = getEyePosition();
        BlockPos center = battleCenter != null ? battleCenter : blockPosition();
        for (BlockPos generator : new BlockPos[]{center.offset(18, 0, 0), center.offset(-18, 0, 0),
                center.offset(0, 0, 18), center.offset(0, 0, -18)}) {
            if (!level.getBlockState(generator).is(com.jhonfx.portalgun.init.ModBlocks.OMEGA_ALARM_PANEL.get())) continue;
            Vec3 from = Vec3.atCenterOf(generator.above());
            Vec3 delta = target.subtract(from);
            for (int i = 0; i <= 8; i++) {
                Vec3 point = from.add(delta.scale(i / 8d));
                level.sendParticles(PRIME_GREEN, point.x, point.y, point.z, 1, 0, 0, 0, 0);
            }
        }
    }

    /** Expanding rift barrage: concentric rings + strikes */
    private void dimensionalRiftBarrage(ServerLevel level, LivingEntity target) {
        for (int r = 2; r <= 7; r += 2) {
            for (int i = 0; i < 28; i++) {
                double a = i * Math.PI * 2 / 28.0 + tickCount * 0.05;
                level.sendParticles(i % 4 == 0 ? ParticleTypes.ELECTRIC_SPARK : PRIME_GREEN,
                        target.getX() + Math.cos(a)*r, target.getY() + 0.2, target.getZ() + Math.sin(a)*r,
                        1, 0.03, 0.05, 0.03, 0.01);
            }
        }
        int strikes = finalPhase ? 7 : 4;
        for (int i = 0; i < strikes; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double dist = random.nextDouble() * (finalPhase ? 5.5 : 4.0);
            Vec3 strike = target.position().add(Math.cos(a)*dist, 0, Math.sin(a)*dist);
            level.sendParticles(ParticleTypes.FLASH, strike.x, strike.y + 0.9, strike.z, 1, 0, 0, 0, 0);
            level.explode(this, strike.x, strike.y, strike.z, 1.4f, Level.ExplosionInteraction.NONE);
        }
        level.playSound(null, target.blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                SoundSource.HOSTILE, 1.5f, 1.45f);
    }

    /** Ambient swirling particles around Rick */
    private void ambientParticles(ServerLevel level) {
        double a = tickCount * 0.15;
        for (int i = 0; i < 3; i++) {
            double angle = a + i * Math.PI * 2 / 3.0;
            level.sendParticles(PRIME_DARK,
                    getX() + Math.cos(angle)*0.8, getY() + 1.2, getZ() + Math.sin(angle)*0.8,
                    1, 0.05, 0.08, 0.05, 0.01);
        }
    }

    /** Summon clone copies */
    private void summonClones(ServerLevel level, int count) {
        for (int i = 0; i < count; i++) {
            RickPrimeEntity clone = ModEntityTypes.RICK_PRIME.get().create(level);
            if (clone == null) continue;
            clone.setClone(true);
            double angle = i * Math.PI * 2 / count;
            clone.moveTo(
                    getX() + Math.cos(angle) * 3.5,
                    getY(),
                    getZ() + Math.sin(angle) * 3.5,
                    getYRot(), 0);
            clone.setTarget(getTarget());
            clone.setGlowingTag(true);
            level.addFreshEntity(clone);
        }
        level.playSound(null, blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE,
                SoundSource.HOSTILE, 1.8f, 0.70f);
    }

    // ── Death ─────────────────────────────────────────────────────────────────
    @Override public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && !isClone() && deathSequenceTicks == 0
                && level() instanceof ServerLevel server && generatorsActive(server) && !isInvulnerable()) {
            renderGeneratorShield(server);
            if (source.getEntity() instanceof ServerPlayer player && tickCount % 10 == 0)
                player.displayClientMessage(Component.literal("ESCUDO PRIME ATIVO — DESTRUA OS 4 GERADORES DA ARENA"), true);
            return false;
        }
        return deathSequenceTicks > 0 ? false : super.hurt(source, amount);
    }

    @Override public void die(DamageSource source) {
        if (isClone()) { super.die(source); return; }
        if (deathSequenceTicks != 0 || level().isClientSide) return;
        deathSource = source;
        deathSequenceTicks = 1;
        setHealth(1);
        setNoAi(true);
        setInvulnerable(true);
        bossEvent.setProgress(0);
        broadcastBoss(level(), "RICK PRIME: ...VOCÊ REALMENTE CONSEGUIU.");
    }

    private void tickDeathSequence(ServerLevel server) {
        deathSequenceTicks++;
        setDeltaMovement(Vec3.ZERO);
        if (deathSequenceTicks % 3 == 0)
            server.sendParticles(deathSequenceTicks < 38 ? PRIME_DARK : ParticleTypes.END_ROD,
                    getX(), getY() + 1, getZ(), 8, .45, .9, .45, .04);
        if (deathSequenceTicks == 30)
            server.playSound(null, blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 3f, .35f);
        if (deathSequenceTicks < 60) return;
        deathSequenceTicks = -1;
        setInvulnerable(false);
        super.die(deathSource == null ? damageSources().magic() : deathSource);
        finishVictory(server);
    }

    private void finishVictory(ServerLevel server) {
            com.jhonfx.portalgun.federation.UnmortrickenFacility.markDefeated(server);
            // Final explosion
            Vec3 c = Vec3.atCenterOf(battleCenter != null ? battleCenter : blockPosition());
            for (int i = 0; i < 5; i++) {
                double a = i * Math.PI * 2 / 5.0;
                server.explode(this, c.x + Math.cos(a)*4, c.y, c.z + Math.sin(a)*4,
                        2.5f, Level.ExplosionInteraction.NONE);
            }
            server.sendParticles(PRIME_GREEN, c.x, c.y + 2, c.z, 200, 3, 2, 3, 0.12);
            // UnmortrickenFacility grants one persistent reward to every
            // participating player. Do not leave a single shared drop that
            // can burn, despawn, or be stolen in multiplayer.
            OmegaDeviceHandler.armFinalShot(server, c);
            broadcastBoss(server, "RICK PRIME foi eliminado.");
    }

    // ── Boss bar ──────────────────────────────────────────────────────────────
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        if (!isClone()) {
            bossEvent.setProgress(getHealth() / getMaxHealth());
        }
    }

    @Override public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (!isClone()) bossEvent.addPlayer(player);
    }

    @Override public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    private void broadcastBoss(Level level, String msg) {
        level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(msg), false);
    }
    private void broadcastBoss(ServerLevel level, String msg) {
        broadcastBoss((Level) level, msg);
    }

    // ── Serialization ─────────────────────────────────────────────────────────
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Clone",        isClone());
        tag.putBoolean("SecondPhase",  secondPhase);
        tag.putBoolean("FinalPhase",   finalPhase);
        tag.putBoolean("DianePhase",   dianePhase);
        tag.putBoolean("Retreating", entityData.get(RETREATING));
        tag.putBoolean("AwaitingIntro",awaitingIntro);
        tag.putInt("IntroTicks",       introTicks);
        tag.putInt("Phase",            getPhase());
        tag.putInt("ShotCD",           shotCooldown);
        tag.putInt("BlinkCD",          blinkCooldown);
        tag.putInt("BarrageCD",        barrageCooldown);
        tag.putInt("ShockwaveCD", shockwaveCooldown);
        tag.putInt("DeathSequence", deathSequenceTicks);
        if (battleCenter != null) tag.putLong("BattleCenter", battleCenter.asLong());
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setClone(tag.getBoolean("Clone"));
        secondPhase   = tag.getBoolean("SecondPhase");
        finalPhase    = tag.getBoolean("FinalPhase");
        dianePhase    = tag.getBoolean("DianePhase");
        entityData.set(RETREATING, tag.getBoolean("Retreating"));
        awaitingIntro = tag.getBoolean("AwaitingIntro");
        introTicks    = tag.getInt("IntroTicks");
        if (tag.contains("Phase"))     entityData.set(PHASE, tag.getInt("Phase"));
        if (tag.contains("ShotCD"))    shotCooldown    = tag.getInt("ShotCD");
        if (tag.contains("BlinkCD"))   blinkCooldown   = tag.getInt("BlinkCD");
        if (tag.contains("BarrageCD")) barrageCooldown = tag.getInt("BarrageCD");
        if (tag.contains("ShockwaveCD")) shockwaveCooldown = tag.getInt("ShockwaveCD");
        deathSequenceTicks = tag.getInt("DeathSequence");
        if (tag.contains("BattleCenter")) battleCenter = BlockPos.of(tag.getLong("BattleCenter"));
    }

    @Override public boolean canChangeDimensions() { return false; }

    // ── GeckoLib ──────────────────────────────────────────────────────────────
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 3,
                state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}

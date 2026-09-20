package com.jhonfx.portalgun.entity;

import com.jhonfx.portalgun.init.ModItems;
import com.jhonfx.portalgun.item.NeuralScannerItem;
import com.jhonfx.portalgun.prologue.PrologueHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.PathfinderMob;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Citadel NPC — Ricks, Mortys, Guards, Council, and special variants.
 *
 * Variant ordinals are append-only to preserve existing saves:
 *   0 RICK   1 MORTY   2 C524   3 PRIME   4 COUNCIL   5 GUARD
 *   6 MORTY_NERVOUS   7 MORTY_REBEL   8 MORTY_WORKER
 *
 * Guards now patrol between assigned posts and defend against hostile mobs
 * and unauthorized players (high Federation/Citadel wanted).
 */
public final class CitadelNpcEntity extends PathfinderMob implements GeoEntity {
    public enum Variant {
        RICK, MORTY, C524, PRIME, COUNCIL, GUARD,
        MORTY_NERVOUS,  // skittish, flees combat, gives lore tips
        MORTY_REBEL,    // hostile-adjacent dialogue, cynical about the Citadel
        MORTY_WORKER,   // stationed at Workshop/Archive, gives crafting hints
        // ── Ricks cativos da arena de Rick Prime (arco "P-0") ──────────────
        RICK_COMMANDO, RICK_CYBORG, RICK_ASSASSIN, RICK_SOLDIER,
        RICK_AUGMENTED, RICK_SCARRED, RICK_ELDER
    }

    /** Ricks capturados por Rick Prime — todos foram caçá-lo e falharam. */
    public static boolean isCaptiveRick(Variant v) {
        return v == Variant.RICK_COMMANDO || v == Variant.RICK_CYBORG || v == Variant.RICK_ASSASSIN
            || v == Variant.RICK_SOLDIER  || v == Variant.RICK_AUGMENTED || v == Variant.RICK_SCARRED
            || v == Variant.RICK_ELDER;
    }

    public static boolean isMortyVariant(Variant v){return v==Variant.MORTY||v==Variant.MORTY_NERVOUS||v==Variant.MORTY_REBEL||v==Variant.MORTY_WORKER;}

    private static final EntityDataAccessor<Integer> VARIANT =
            SynchedEntityData.defineId(CitadelNpcEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.rick_prime.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.rick_prime.walk");
    private static final RawAnimation MORTY_IDLE = RawAnimation.begin().thenLoop("animation.evil_morty.idle");
    private static final RawAnimation MORTY_WALK = RawAnimation.begin().thenLoop("animation.evil_morty.walk");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Guard patrol state
    private net.minecraft.core.BlockPos[] patrolPosts;
    private int patrolIndex;
    private int patrolWaitTicks;

    public CitadelNpcEntity(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 20)
                .add(Attributes.ARMOR, 2)
                .add(Attributes.ATTACK_DAMAGE, 3);
    }

    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(VARIANT, 0); }

    public Variant getVariant() {
        int i = entityData.get(VARIANT);
        Variant[] v = Variant.values();
        return v[Math.max(0, Math.min(v.length - 1, i))];
    }

    public void setVariant(Variant variant) {
        entityData.set(VARIANT, variant.ordinal());
        applyVariantSetup(variant);
    }

    private void applyVariantSetup(Variant variant) {
        switch (variant) {
            case GUARD -> {
                getAttribute(Attributes.MAX_HEALTH).setBaseValue(36);
                getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(7);
                getAttribute(Attributes.ARMOR).setBaseValue(6);
                setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.0f);
            }
            case MORTY_NERVOUS -> getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.32); // flees faster
            case MORTY_REBEL   -> getAttribute(Attributes.MAX_HEALTH).setBaseValue(28);
            case PRIME         -> getAttribute(Attributes.MAX_HEALTH).setBaseValue(60);

            // ── Ricks cativos: cada um luta diferente na arena ──────────────
            case RICK_COMMANDO -> { // tático, dano alto, vida média
                getAttribute(Attributes.MAX_HEALTH).setBaseValue(45);
                getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(9);
                getAttribute(Attributes.ARMOR).setBaseValue(4);
                setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
            }
            case RICK_CYBORG -> { // tanque lento, muita vida e armadura
                getAttribute(Attributes.MAX_HEALTH).setBaseValue(70);
                getAttribute(Attributes.ARMOR).setBaseValue(12);
                getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.18);
                getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(8);
            }
            case RICK_ASSASSIN -> { // rápido, fraco, esconde-se
                getAttribute(Attributes.MAX_HEALTH).setBaseValue(24);
                getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.38);
                getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(11);
                setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            }
            case RICK_SOLDIER -> { // equilibrado, armado
                getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);
                getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(7);
                getAttribute(Attributes.ARMOR).setBaseValue(5);
                setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
            }
            case RICK_AUGMENTED -> { // dano alto, sem armadura
                getAttribute(Attributes.MAX_HEALTH).setBaseValue(38);
                getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(13);
                getAttribute(Attributes.ARMOR).setBaseValue(1);
            }
            case RICK_SCARRED -> { // veterano, tenta fugir da arena
                getAttribute(Attributes.MAX_HEALTH).setBaseValue(30);
                getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.30);
                getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(6);
            }
            case RICK_ELDER -> { // frágil, tenta negociar/se esconder
                getAttribute(Attributes.MAX_HEALTH).setBaseValue(18);
                getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.22);
                getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(3);
            }
            default -> {}
        }
        setHealth(getMaxHealth());
    }

    /** Assign a patrol route (used by guards). Call after spawn. */
    public void setPatrolRoute(net.minecraft.core.BlockPos[] posts) {
        this.patrolPosts = posts;
        this.patrolIndex = 0;
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        // Variants are assigned after construction, while registerGoals runs in
        // the superclass constructor. These goals inspect live arena state.
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.18, true));
        targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(this, CitadelNpcEntity.class, true,
                candidate -> candidate instanceof CitadelNpcEntity other && isArenaOpponent(other)));
        // A variante só é atribuída depois que o construtor registra os goals.
        // Portanto estes predicados precisam consultar a variante em tempo real.
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Monster.class, true,
                candidate -> getVariant() == Variant.GUARD));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true,
                candidate -> isCaptiveRick(getVariant())
                        && getPersistentData().getBoolean("PortalGunArenaRick")));
        Variant v = getVariant();

        if (v == Variant.GUARD) {
            goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15, true));
            goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 10));
            targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Monster.class, true));
            // Guard patrol handled manually in customServerAiStep via patrolPosts
        } else if (v == Variant.MORTY_NERVOUS) {
            goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Monster.class, 10.0f, 1.3, 1.4));
            goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 7));

        } else if (isCaptiveRick(v)) {
            // Arena de Ricks cativos — cada personalidade reage diferente ao caos
            if (v == Variant.RICK_ELDER || v == Variant.RICK_SCARRED) {
                // Tentam fugir/se esconder em vez de lutar
                goalSelector.addGoal(1, new AvoidEntityGoal<>(this, CitadelNpcEntity.class, 8.0f, 1.2, 1.3));
                goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 6.0f, 1.15, 1.25));
            } else {
                // Os demais brigam entre si E com o jogador (caos de "battle royale")
                goalSelector.addGoal(1, new MeleeAttackGoal(this, v == Variant.RICK_ASSASSIN ? 1.35 : 1.1, true));
                targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, CitadelNpcEntity.class, true,
                        e -> e instanceof CitadelNpcEntity npc && isCaptiveRick(npc.getVariant()) && npc != this));
                targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
            }
            goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8));

        } else {
            goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
            goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 7));
        }
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    private boolean isArenaOpponent(CitadelNpcEntity other) {
        if (other == this || !isCaptiveRick(getVariant()) || !isCaptiveRick(other.getVariant())) return false;
        if (!getPersistentData().getBoolean("PortalGunArenaRick")
                || !other.getPersistentData().getBoolean("PortalGunArenaRick")) return false;
        boolean finalRound = getPersistentData().getBoolean("PortalGunArenaFinalRound");
        return finalRound
                ? other.getPersistentData().getBoolean("PortalGunArenaFinalRound")
                : getPersistentData().getInt("PortalGunArenaQuadrant")
                    == other.getPersistentData().getInt("PortalGunArenaQuadrant");
    }

    // ── Guard patrol logic ────────────────────────────────────────────────────
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        if (getVariant() != Variant.GUARD || patrolPosts == null || patrolPosts.length == 0) return;
        if (getTarget() != null) return; // busy fighting

        net.minecraft.core.BlockPos post = patrolPosts[patrolIndex];
        if (blockPosition().distSqr(post) < 4) {
            if (patrolWaitTicks++ > 60) {
                patrolWaitTicks = 0;
                patrolIndex = (patrolIndex + 1) % patrolPosts.length;
            }
        } else {
            getNavigation().moveTo(post.getX() + .5, post.getY(), post.getZ() + .5, 1.0);
        }
    }

    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.SUCCESS;

        if (getPersistentData().getBoolean("PortalGunC137Cameo") && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            if (com.jhonfx.portalgun.federation.FederationSignalArc.interactC137Cameo(this, sp)) return InteractionResult.CONSUME;
        }

        if (getVariant() == Variant.PRIME) { PrologueHandler.interactPrime(this, player); return InteractionResult.CONSUME; }

        if (getVariant() == Variant.C524) {
            if (player.getItemInHand(hand).getItem() instanceof NeuralScannerItem) return super.mobInteract(player, hand);
            player.displayClientMessage(Component.translatable(
                    player.getPersistentData().getBoolean("PortalGunC524Fragment")
                            ? "dialogue.portalgun.c524_after" : "dialogue.portalgun.c524"), false);
            return InteractionResult.CONSUME;
        }

        if (getPersistentData().getString("PortalGunPrimeAlly").equals("C137")) {
            player.displayClientMessage(Component.translatable("dialogue.portalgun.unmortricken_c137_"
                    + Math.floorMod(getId(), 3)), false);
            return InteractionResult.CONSUME;
        }

        if (isCaptiveRick(getVariant())) {
            player.displayClientMessage(Component.translatable("dialogue.portalgun.captive_"
                    + getVariant().name().toLowerCase(java.util.Locale.ROOT)), false);
            return InteractionResult.CONSUME;
        }

        CompoundTag persistent = player.getPersistentData();
        if (!persistent.getBoolean("PortalGunCitadelWelcome")) {
            persistent.putBoolean("PortalGunCitadelWelcome", true);
            ItemStack credits = new ItemStack(ModItems.CITADEL_CREDIT.get(), 5);
            if (!player.getInventory().add(credits)) player.drop(credits, false);
            player.displayClientMessage(Component.translatable("message.portalgun.citadel_welcome"), false);
            return InteractionResult.CONSUME;
        }

        String dialogueKey = switch (getVariant()) {
            case RICK          -> "dialogue.portalgun.citadel_rick_" + Math.floorMod(getId(), 5);
            case MORTY         -> "dialogue.portalgun.citadel_morty_" + Math.floorMod(getId(), 4);
            case COUNCIL       -> "dialogue.portalgun.citadel_council_" + Math.floorMod(getId(), 3);
            case MORTY_NERVOUS -> "dialogue.portalgun.citadel_morty_nervous_" + Math.floorMod(getId(), 3);
            case MORTY_REBEL   -> "dialogue.portalgun.citadel_morty_rebel_" + Math.floorMod(getId(), 3);
            case MORTY_WORKER  -> "dialogue.portalgun.citadel_morty_worker_" + Math.floorMod(getId(), 3);
            default            -> "dialogue.portalgun.citadel_guard_" + Math.floorMod(getId(), 3);
        };
        player.displayClientMessage(Component.translatable(dialogueKey), false);
        return InteractionResult.CONSUME;
    }

    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {
        return getVariant() == Variant.PRIME
                || getPersistentData().contains("PortalGunPrimeAlly")
                || super.isInvulnerableTo(source);
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", entityData.get(VARIANT));
        tag.putInt("PatrolIndex", patrolIndex);
        if (patrolPosts != null) {
            long[] encoded = new long[patrolPosts.length];
            for (int i = 0; i < patrolPosts.length; i++) encoded[i] = patrolPosts[i].asLong();
            tag.putLongArray("PatrolPosts", encoded);
        }
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(VARIANT, tag.getInt("Variant"));
        patrolIndex = tag.getInt("PatrolIndex");
        if (tag.contains("PatrolPosts")) {
            long[] encoded = tag.getLongArray("PatrolPosts");
            patrolPosts = new net.minecraft.core.BlockPos[encoded.length];
            for (int i = 0; i < encoded.length; i++) patrolPosts[i] = net.minecraft.core.BlockPos.of(encoded[i]);
        }
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.core.animation.AnimationController<>(this, "movement", 5, state -> {
            boolean morty=isMortyVariant(getVariant());
            state.setAnimation(state.isMoving() ? (morty?MORTY_WALK:WALK) : (morty?MORTY_IDLE:IDLE));
            return software.bernie.geckolib.core.object.PlayState.CONTINUE;
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}

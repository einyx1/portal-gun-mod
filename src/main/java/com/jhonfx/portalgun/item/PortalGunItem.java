package com.jhonfx.portalgun.item;

import com.jhonfx.portalgun.client.ClientScreens;
import com.jhonfx.portalgun.client.PortalGunRenderer;
import com.jhonfx.portalgun.entity.PortalColor;
import com.jhonfx.portalgun.entity.PortalEntity;
import com.jhonfx.portalgun.init.ModEntityTypes;
import com.jhonfx.portalgun.init.ModSounds;
import com.jhonfx.portalgun.omega.OmegaDeviceHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.joml.Vector3f;

public class PortalGunItem extends Item implements GeoItem {
    public static final String TAG_MODE = "PortalMode";
    public static final String TAG_CHARGE = "Charge";
    public static final String TAG_SCALE = "PortalScale";
    public static final String TAG_PORTALS = "Portals";
    public static final String TAG_DESTINATION = "Destination";
    public static final String TAG_TUBE_COLOR = "TubeColor";
    public static final String TAG_DURATION = "PortalDuration";
    public static final String TAG_STRUCTURE = "TargetStructure";
    public static final String TAG_HIGH_PRESSURE = "HighPressure";
    public static final String TAG_SAFE_MODE = "SafePlacement";
    public static final String TAG_AUTO_CLOSE = "AutoClose";
    public static final String TAG_CLOSE_AFTER_USE = "CloseAfterUse";
    public static final String TAG_AIR_RANGE = "AirPortalRange";
    public static final String TAG_SAVED_LOCATIONS = "SavedLocations";
    public static final String TAG_HISTORY = "DestinationHistory";
    private static final double MAX_RANGE = 128.0;
    private static final int MAX_PORTALS = 10;
    private static final RawAnimation SHOOT_ANIMATION =
            RawAnimation.begin().thenPlay("animation.portal_gun.first_person_shoot");

    private final PortalGunVariant variant;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public PortalGunItem(Properties properties, PortalGunVariant variant) {
        super(properties);
        this.variant = variant;
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    public PortalGunVariant variant() {
        return variant;
    }

    public static void initialize(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(TAG_MODE)) tag.putString(TAG_MODE, PortalMode.FIFO.name());
        if (!tag.contains(TAG_CHARGE)) tag.putInt(TAG_CHARGE, PortalFluidTubeItem.MAX_CHARGE);
        else if (tag.getInt(TAG_CHARGE) <= 100 && !tag.contains(TAG_TUBE_COLOR))
            tag.putInt(TAG_CHARGE, tag.getInt(TAG_CHARGE) * 10); // migrate 0.2 saves
        if (!tag.contains(TAG_TUBE_COLOR)) tag.putString(TAG_TUBE_COLOR,
                ((PortalGunItem) stack.getItem()).variant().color().name());
        if (!tag.contains(TAG_SCALE)) tag.putFloat(TAG_SCALE, 1.0f);
        if (!tag.contains(TAG_DURATION)) tag.putInt(TAG_DURATION, 20 * 10);
        else if (tag.getInt(TAG_DURATION) > 20 * 60) tag.putInt(TAG_DURATION, 20 * 10); // migrate minute-based saves
        else tag.putInt(TAG_DURATION, Math.max(20 * 3, tag.getInt(TAG_DURATION)));
        if (!tag.contains(TAG_HIGH_PRESSURE)) tag.putBoolean(TAG_HIGH_PRESSURE, false);
        if (!tag.contains(TAG_SAFE_MODE)) tag.putBoolean(TAG_SAFE_MODE, true);
        if (!tag.contains(TAG_AUTO_CLOSE)) tag.putBoolean(TAG_AUTO_CLOSE, true);
        if (!tag.contains(TAG_CLOSE_AFTER_USE)) tag.putBoolean(TAG_CLOSE_AFTER_USE, false);
        if (!tag.contains(TAG_AIR_RANGE)) tag.putInt(TAG_AIR_RANGE, 48);
        else tag.putInt(TAG_AIR_RANGE, Math.max(8, Math.min(256, tag.getInt(TAG_AIR_RANGE))));
        if (!tag.contains(TAG_SAVED_LOCATIONS, Tag.TAG_LIST)) tag.put(TAG_SAVED_LOCATIONS, new ListTag());
        if (!tag.contains(TAG_HISTORY, Tag.TAG_LIST)) tag.put(TAG_HISTORY, new ListTag());
        if (!tag.contains(TAG_PORTALS, Tag.TAG_LIST)) tag.put(TAG_PORTALS, new ListTag());
        if (((PortalGunItem) stack.getItem()).variant() == PortalGunVariant.PROTOTYPE
                && !tag.contains(TAG_DESTINATION, Tag.TAG_COMPOUND)) {
            CompoundTag destination = new CompoundTag();
            destination.putDouble("X", 0.5); destination.putDouble("Y", 97); destination.putDouble("Z", 10.5);
            destination.putString("Dimension", "portalgun:citadel");
            tag.put(TAG_DESTINATION, destination);
            tag.putString(TAG_MODE, PortalMode.CUSTOM.name());
        }
    }

    public static PortalMode getMode(ItemStack stack) {
        initialize(stack);
        try {
            return PortalMode.valueOf(stack.getTag().getString(TAG_MODE));
        } catch (IllegalArgumentException ignored) {
            return PortalMode.FIFO;
        }
    }

    public static int getCharge(ItemStack stack) {
        initialize(stack);
        return stack.getTag().getInt(TAG_CHARGE);
    }

    public static float getPortalScale(ItemStack stack) {
        initialize(stack);
        return stack.getTag().getFloat(TAG_SCALE);
    }

    public static int getPortalDuration(ItemStack stack) {
        initialize(stack);
        return stack.getTag().getInt(TAG_DURATION);
    }

    public static int getAirRange(ItemStack stack) {
        initialize(stack);
        return stack.getTag().getInt(TAG_AIR_RANGE);
    }

    @Nullable
    public static PortalColor getLoadedColor(ItemStack stack) {
        initialize(stack);
        String value = stack.getTag().getString(TAG_TUBE_COLOR);
        if (value.isBlank()) return null;
        try { return PortalColor.valueOf(value); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        initialize(stack);

        if (player.isShiftKeyDown()) {
            player.swing(hand, true);
            if (level.isClientSide) ClientScreens.openPortalGunMenu(hand);
            else level.playSound(null, player.blockPosition(), ModSounds.OPEN_MENU.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.75f, 1.0f);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        if (OmegaDeviceHandler.isPortalSuppressed((ServerLevel) level, player.position())) {
            player.displayClientMessage(Component.literal("BLOQUEIO DIMENSIONAL - PORTAL GUNS DESATIVADAS DURANTE A BATALHA"), true);
            level.playSound(null, player.blockPosition(), ModSounds.PORTAL_GUN_ERROR.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.55f);
            return InteractionResultHolder.fail(stack);
        }

        // Portal inhibitor: Federation wanted 4+ blocks portal fire in the Federation dimension
        if (!level.isClientSide && level.dimension().location().equals(new ResourceLocation("portalgun", "federation"))) {
            if (player instanceof net.minecraft.server.level.ServerPlayer sp &&
                    com.jhonfx.portalgun.federation.FederationThreatHandler.isPortalInhibited(sp)) {
                player.displayClientMessage(Component.translatable("message.portalgun.fed_inhibitor_blocked"), true);
                level.playSound(null, player.blockPosition(), ModSounds.PORTAL_GUN_ERROR.get(),
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.35f);
                return InteractionResultHolder.fail(stack);
            }
        }

        // Protótipos não DISPARAM dentro da cidadela (Shift+Clique abre menu normalmente)
        if (variant.isPrototype() && level.dimension().location().equals(new ResourceLocation("portalgun", "citadel"))) {
            player.displayClientMessage(Component.literal("PROTÓTIPO ESPACIAL: não dispara dentro da Cidadela. Use Shift+Clique para configurar coordenadas de saída."), true);
            level.playSound(null, player.blockPosition(), ModSounds.PORTAL_GUN_ERROR.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1, .55f);
            return InteractionResultHolder.fail(stack);
        }

        PortalColor loadedColor = getLoadedColor(stack);
        if (loadedColor == null) {
            player.displayClientMessage(Component.translatable("message.portalgun.no_tube"), true);
            return InteractionResultHolder.fail(stack);
        }
        boolean highPressure = stack.getTag().getBoolean(TAG_HIGH_PRESSURE);
        int cost = Math.max(2, Math.round(getPortalScale(stack) * (highPressure ? 7 : 3) * variant.fluidMultiplier()));
        if (getCharge(stack) < cost) {
            level.playSound(null, player.blockPosition(), ModSounds.PORTAL_GUN_ERROR.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.9f, 0.8f);
            player.displayClientMessage(Component.translatable("message.portalgun.empty"), true);
            return InteractionResultHolder.fail(stack);
        }

        HitResult hit = raycast(level, player, highPressure ? MAX_RANGE * 2 : MAX_RANGE);
        BlockHitResult blockHit;
        if (hit instanceof BlockHitResult found && hit.getType() == HitResult.Type.BLOCK) {
            blockHit = found;
        } else {
            Vec3 air = player.getEyePosition().add(player.getViewVector(1.0f).scale(getAirRange(stack)));
            Vec3 reverseView = player.getViewVector(1.0f).scale(-1);
            Direction face = Direction.getNearest(reverseView.x, reverseView.y, reverseView.z);
            blockHit = new BlockHitResult(air, face, BlockPos.containing(air), false);
        }

        if (firePortal((ServerLevel) level, player, stack, blockHit, loadedColor)) {
            player.swing(hand, true);
            triggerAnim(player, GeoItem.getOrAssignId(stack, (ServerLevel) level),
                    "portal_gun", "shoot");
            stack.getTag().putInt(TAG_CHARGE, Math.max(0, getCharge(stack) - cost));
        }
        return InteractionResultHolder.success(stack);
    }

    private boolean firePortal(ServerLevel level, Player player, ItemStack stack,
                               BlockHitResult hit, PortalColor color) {
        Direction facing = hit.getDirection();
        BlockPos emptyPos = hit.getBlockPos().relative(facing);
        if (!canPlacePortal(level, emptyPos, facing, player, stack)) {
            player.displayClientMessage(Component.translatable("message.portalgun.invalid_placement"), true);
            level.playSound(null, player.blockPosition(), ModSounds.PORTAL_GUN_ERROR.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.0f);
            return false;
        }

        Vec3 spawnPos = Vec3.atCenterOf(emptyPos).add(
                -facing.getStepX() * 0.49,
                -facing.getStepY() * 0.49,
                -facing.getStepZ() * 0.49);
        PortalEntity localPortal = spawnPortal(level, spawnPos, facing, color, player.getUUID(),
                getPortalScale(stack), stack.getTag().getBoolean(TAG_AUTO_CLOSE)
                        ? getPortalDuration(stack) : Integer.MAX_VALUE, stack.getTag().getBoolean(TAG_SAFE_MODE),stack.getTag().getBoolean(TAG_CLOSE_AFTER_USE));

        List<PortalRef> refs = readPortalRefs(stack);
        refs.removeIf(ref -> resolvePortal(level.getServer(), ref) == null);
        PortalMode mode = getMode(stack);

        if (mode == PortalMode.ONE_SHOT) {
            removeAll(level.getServer(), refs);
            refs.clear();
            PortalEntity destination = spawnRandomDestinationPortal(level, stack, color, player.getUUID(), player.blockPosition());
            if (destination == null) {
                localPortal.discardWithViews();
                player.displayClientMessage(Component.translatable("message.portalgun.random_destination_failed"), true);
                return false;
            }
            link(localPortal, destination);
            refs.add(PortalRef.of(destination));
            refs.add(PortalRef.of(localPortal));
            player.displayClientMessage(Component.translatable("message.portalgun.random_destination_ready"), true);
        } else if (mode == PortalMode.CUSTOM && stack.getTag().contains(TAG_DESTINATION, Tag.TAG_COMPOUND)) {
            removeAll(level.getServer(), refs);
            refs.clear();
            PortalEntity destination = spawnDestinationPortal(level.getServer(), level, stack, color, player.getUUID());
            if (destination == null) {
                localPortal.discardWithViews();
                player.displayClientMessage(Component.translatable("message.portalgun.invalid_destination"), true);
                return false;
            }
            link(localPortal, destination);
            refs.add(PortalRef.of(destination));
            refs.add(PortalRef.of(localPortal));
        } else {
            refs.add(PortalRef.of(localPortal));
            applyMode(level.getServer(), refs, mode);
        }

        writePortalRefs(stack, refs);
        level.playSound(null, player.blockPosition(), ModSounds.FIRE_PORTAL_GUN.get(),
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
        level.playSound(null, emptyPos, ModSounds.PORTAL_SPAWN.get(),
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        spawnShotTrail(level, player.getEyePosition().add(player.getViewVector(1).scale(0.7)), spawnPos, color);
        return true;
    }

    @Nullable
    private PortalEntity spawnRandomDestinationPortal(ServerLevel level, ItemStack stack, PortalColor color,
                                                       UUID owner, BlockPos origin) {
        WorldBorder border = level.getWorldBorder();
        // Several bounded attempts avoid force-loading an excessive number of chunks while
        // still making every shot meaningfully unpredictable.
        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            int distance = 384 + level.random.nextInt(3713);
            int x = origin.getX() + (int) Math.round(Math.cos(angle) * distance);
            int z = origin.getZ() + (int) Math.round(Math.sin(angle) * distance);
            BlockPos column = new BlockPos(x, level.getSeaLevel(), z);
            if (!border.isWithinBounds(column)) continue;
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos safe = findSafeDestination(level, new BlockPos(x, Math.max(level.getMinBuildHeight() + 1, surfaceY), z));
            if (safe == null || safe.distSqr(origin) < 128 * 128) continue;
            return spawnPortal(level, Vec3.atBottomCenterOf(safe), Direction.UP, color, owner,
                    getPortalScale(stack), stack.getTag().getBoolean(TAG_AUTO_CLOSE)
                            ? getPortalDuration(stack) : Integer.MAX_VALUE, true,stack.getTag().getBoolean(TAG_CLOSE_AFTER_USE));
        }
        return null;
    }

    @Nullable
    private PortalEntity spawnDestinationPortal(MinecraftServer server, ServerLevel sourceLevel, ItemStack stack,
                                                PortalColor color, UUID owner) {
        CompoundTag destination = stack.getTag().getCompound(TAG_DESTINATION);
        ResourceLocation dimensionId = ResourceLocation.tryParse(destination.getString("Dimension"));
        if (dimensionId == null) return null;
        ServerLevel targetLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
        if (targetLevel == null) return null;
        boolean primeDestination = dimensionId.equals(com.jhonfx.portalgun.federation.UnmortrickenFacility.ID);
        if (primeDestination) {
            ServerPlayer traveler = server.getPlayerList().getPlayer(owner);
            if (traveler == null || (!traveler.isCreative()
                    && !traveler.getPersistentData().getBoolean("PortalGunUnmortrickenUnlocked"))) return null;
            if (!com.jhonfx.portalgun.federation.UnmortrickenFacility.ensureBuilt(targetLevel)) {
                traveler.displayClientMessage(Component.literal("Estabilizando a instalação Prime. Aguarde alguns segundos e tente novamente."), true);
                return null;
            }
        }
        if(dimensionId.equals(new ResourceLocation("portalgun","federation")))com.jhonfx.portalgun.federation.FederationBuilder.ensureBuilt(targetLevel);
        if(dimensionId.equals(new ResourceLocation("portalgun","citadel")))com.jhonfx.portalgun.citadel.CitadelBuilder.ensureBuilt(targetLevel);
        boolean sameDimension = targetLevel.dimension().equals(sourceLevel.dimension());
        // Azul é puramente espacial/local. O verde é o primeiro protótipo capaz de alcançar a Cidadela.
        if (!sameDimension && variant == PortalGunVariant.BLUE_PROTOTYPE) return null;
        if (!sameDimension && variant == PortalGunVariant.PROTOTYPE) {
            // The green prototype is the story bridge into both early space hubs.
            // Blocking Federation here made the P-0 trap destination impossible
            // immediately after the player escaped that dimension.
            boolean earlyHub = dimensionId.equals(new ResourceLocation("portalgun", "citadel"))
                    || dimensionId.equals(new ResourceLocation("portalgun", "federation"));
            if (!earlyHub) return null;
        }
        // Federation is a planet/sector accessible from any universe — no OutsideCurveAccess needed.
        // OutsideCurveAccess is only required for truly external dimensions (not federation).
        BlockPos requested = BlockPos.containing(destination.getDouble("X"), destination.getDouble("Y"),
                destination.getDouble("Z"));
        if (primeDestination) requested = com.jhonfx.portalgun.federation.UnmortrickenFacility.ARRIVAL;
        if(dimensionId.equals(new ResourceLocation("portalgun","federation"))&&requested.getY()<2)
            requested=com.jhonfx.portalgun.federation.FederationBuilder.CENTER.above();
        if(dimensionId.equals(new ResourceLocation("portalgun","citadel"))&&requested.getY()<2)
            requested=com.jhonfx.portalgun.citadel.CitadelBuilder.ARRIVAL;
        if(dimensionId.equals(new ResourceLocation("portalgun","citadel"))
                && com.jhonfx.portalgun.curve.CurveSavedData.get(targetLevel).broken())
            requested=com.jhonfx.portalgun.citadel.CitadelBuilder.REFUGE;
        BlockPos safe = findSafeDestination(targetLevel, requested);
        if (safe == null) return null;
        return spawnPortal(targetLevel, Vec3.atBottomCenterOf(safe), Direction.UP, color, owner,
                getPortalScale(stack), stack.getTag().getBoolean(TAG_AUTO_CLOSE)
                        ? getPortalDuration(stack) : Integer.MAX_VALUE, stack.getTag().getBoolean(TAG_SAFE_MODE),stack.getTag().getBoolean(TAG_CLOSE_AFTER_USE));
    }

    @Nullable
    private BlockPos findSafeDestination(ServerLevel level, BlockPos requested) {
        for (int radius = 0; radius <= 16; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (radius > 0 && Math.abs(x) != radius && Math.abs(z) != radius) continue;
                    int worldX=requested.getX()+x,worldZ=requested.getZ()+z;
                    if (!level.getWorldBorder().isWithinBounds(new BlockPos(worldX, requested.getY(), worldZ))) continue;
                    int surface=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,worldX,worldZ);
                    int[] bases={requested.getY(),surface};
                    for(int base:bases)for(int delta=0;delta<=32;delta++)for(int sign:delta==0?new int[]{1}:new int[]{1,-1}){
                        int worldY=base+delta*sign;if(worldY<=level.getMinBuildHeight()+1||worldY>=level.getMaxBuildHeight()-2)continue;
                        BlockPos cursor=new BlockPos(worldX,worldY,worldZ);
                        var feet=level.getBlockState(cursor);var head=level.getBlockState(cursor.above());var floor=level.getBlockState(cursor.below());
                        if(feet.isAir()&&head.isAir()&&feet.getFluidState().isEmpty()&&head.getFluidState().isEmpty()
                                &&floor.getFluidState().isEmpty()&&floor.isFaceSturdy(level,cursor.below(),Direction.UP)
                                &&!floor.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)
                                &&!floor.is(net.minecraft.world.level.block.Blocks.CACTUS))return cursor;
                    }
                }
            }
        }
        return null;
    }

    private PortalEntity spawnPortal(ServerLevel level, Vec3 position, Direction facing,
                                     PortalColor color, UUID owner, float scale, int lifetime, boolean safeMode,boolean closeAfterUse) {
        PortalEntity portal = new PortalEntity(ModEntityTypes.PORTAL.get(), level);
        portal.setPos(position);
        portal.setPortalFacing(facing);
        portal.setColor(color);
        portal.setOwnerPlayerId(owner);
        portal.setPortalScale(scale);
        portal.setLifetimeTicks(lifetime);
        portal.setSafeMode(safeMode);
        portal.setCloseAfterUse(closeAfterUse);
        level.addFreshEntity(portal);
        return portal;
    }

    private void spawnShotTrail(ServerLevel level, Vec3 start, Vec3 end, PortalColor color) {
        Vec3 delta = end.subtract(start);
        int steps = Math.max(8, Math.min(180, (int) Math.ceil(delta.length() * 2.4)));
        int rgb = color.rgb();
        DustParticleOptions particle = new DustParticleOptions(new Vector3f(
                ((rgb >> 16) & 255) / 255.0f, ((rgb >> 8) & 255) / 255.0f, (rgb & 255) / 255.0f), 0.72f);
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3 point = start.add(delta.scale(t)).add(0, Math.sin(t * Math.PI) * 0.045, 0);
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0.012, 0.012, 0.012, 0.0);
        }
    }

    private void applyMode(MinecraftServer server, List<PortalRef> refs, PortalMode mode) {
        switch (mode) {
            case FIFO -> {
                while (refs.size() > 2) remove(server, refs.remove(0));
                linkLastPair(server, refs);
            }
            case LIFO -> {
                while (refs.size() > 2) remove(server, refs.remove(1));
                linkLastPair(server, refs);
            }
            case MULTI_PAIR -> {
                if (refs.size() > MAX_PORTALS) {
                    removeAll(server, refs);
                    refs.clear();
                } else if (refs.size() % 2 == 0) {
                    linkRefs(server, refs.get(refs.size() - 2), refs.get(refs.size() - 1));
                }
            }
            case ROOT -> {
                if (refs.size() > MAX_PORTALS) {
                    removeAll(server, refs);
                    refs.clear();
                } else if (refs.size() > 1) {
                    linkRefs(server, refs.get(0), refs.get(refs.size() - 1));
                }
            }
            case ONE_SHOT -> {
                while (refs.size() > 2) remove(server, refs.remove(0));
                linkLastPair(server, refs);
            }
            case CUSTOM -> {
                while (refs.size() > 2) remove(server, refs.remove(0));
                linkLastPair(server, refs);
            }
            case STRUCTURE -> {
                while (refs.size() > 2) remove(server, refs.remove(0));
                linkLastPair(server, refs);
            }
        }
    }

    private void linkLastPair(MinecraftServer server, List<PortalRef> refs) {
        if (refs.size() == 2) linkRefs(server, refs.get(0), refs.get(1));
    }

    private void linkRefs(MinecraftServer server, PortalRef a, PortalRef b) {
        PortalEntity first = resolvePortal(server, a);
        PortalEntity second = resolvePortal(server, b);
        if (first != null && second != null) link(first, second);
    }

    private void link(PortalEntity first, PortalEntity second) {
        first.setLinkedPortal(second);
        second.setLinkedPortal(first);
    }

    public static void closeAllPortals(ServerPlayer player, ItemStack stack) {
        initialize(stack);
        List<PortalRef> refs = readPortalRefs(stack);
        removeAll(player.server, refs);
        // Old preview guns did not persist anchor positions, so their entities
        // could become detached from the gun NBT. Clean every loaded orphan
        // owned by this player as part of the explicit "close all" action.
        List<PortalEntity> orphans=new ArrayList<>();
        for(ServerLevel level:player.server.getAllLevels())for(Entity entity:level.getAllEntities())
            if(entity instanceof PortalEntity portal&&player.getUUID().equals(portal.getOwnerPlayerId()))orphans.add(portal);
        orphans.forEach(PortalEntity::discardWithViews);
        writePortalRefs(stack, List.of());
    }

    private static void removeAll(MinecraftServer server, List<PortalRef> refs) {
        for (PortalRef ref : new ArrayList<>(refs)) remove(server, ref);
    }

    private static void remove(MinecraftServer server, PortalRef ref) {
        PortalEntity portal = resolvePortal(server, ref);
        if (portal != null) portal.discardWithViews();
    }

    @Nullable
    private static PortalEntity resolvePortal(MinecraftServer server, PortalRef ref) {
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, ref.dimension));
        if (level == null) return null;
        if (ref.position != null) level.getChunkAt(ref.position);
        Entity entity = level.getEntity(ref.id);
        return entity instanceof PortalEntity portal ? portal : null;
    }

    private static List<PortalRef> readPortalRefs(ItemStack stack) {
        initialize(stack);
        List<PortalRef> refs = new ArrayList<>();
        ListTag list = stack.getTag().getList(TAG_PORTALS, Tag.TAG_COMPOUND);
        for (Tag value : list) {
            CompoundTag entry = (CompoundTag) value;
            ResourceLocation dimension = ResourceLocation.tryParse(entry.getString("Dimension"));
            BlockPos position=entry.contains("Pos")?BlockPos.of(entry.getLong("Pos")):null;
            if (dimension != null && entry.hasUUID("Id")) refs.add(new PortalRef(entry.getUUID("Id"), dimension,position));
        }
        return refs;
    }

    private static void writePortalRefs(ItemStack stack, List<PortalRef> refs) {
        ListTag list = new ListTag();
        for (PortalRef ref : refs) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", ref.id);
            entry.putString("Dimension", ref.dimension.toString());
            if(ref.position!=null)entry.putLong("Pos",ref.position.asLong());
            list.add(entry);
        }
        stack.getOrCreateTag().put(TAG_PORTALS, list);
    }

    private boolean canPlacePortal(Level level, BlockPos emptyPos, Direction facing, Player player, ItemStack stack) {
        return level.getBlockState(emptyPos).canBeReplaced();
    }

    private HitResult raycast(Level level, Player player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0f).scale(range));
        return level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        initialize(stack);
        // Tier label
        tooltip.add(Component.literal(variant.tierLabel()));
        tooltip.add(Component.translatable("tooltip.portalgun.mode", getMode(stack).displayName()));
        tooltip.add(Component.translatable("tooltip.portalgun.charge",
                Math.round(100.0f * getCharge(stack) / PortalFluidTubeItem.MAX_CHARGE)));
        tooltip.add(Component.translatable("tooltip.portalgun.air_range", getAirRange(stack)));
        tooltip.add(Component.translatable("tooltip.portalgun.menu"));
        tooltip.add(Component.translatable("tooltip.portalgun.technology." + variant.technologyKey()));
        // Capability flags
        if (variant.crossesFiniteCurve())      tooltip.add(Component.literal("§e✦ Cruza a Curva Finita"));
        if (variant.supportsVerticalPortals())  tooltip.add(Component.literal("§e✦ Portais verticais (chão/teto)"));
        if (variant.hasBiometrics())           tooltip.add(Component.literal("§c✦ Bloqueio biométrico ativo"));
        if (variant.bypassesRickRestrictions())tooltip.add(Component.literal("§e✦ Ignora restrições da Cidadela"));
        if (variant.isPrototype())             tooltip.add(Component.literal("§7✦ Protótipo — alcance limitado"));
        if (stack.getTag().getBoolean("OutsideCurveAccess"))
            tooltip.add(Component.translatable("tooltip.portalgun.outside_curve"));
        if (stack.getTag().contains(TAG_DESTINATION, Tag.TAG_COMPOUND)) {
            CompoundTag destination = stack.getTag().getCompound(TAG_DESTINATION);
            tooltip.add(Component.translatable("tooltip.portalgun.tracker",
                    (int) destination.getDouble("X"), (int) destination.getDouble("Y"),
                    (int) destination.getDouble("Z"), destination.getString("Dimension")));
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private PortalGunRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new PortalGunRenderer();
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "portal_gun", 0, state -> PlayState.STOP)
                .triggerableAnim("shoot", SHOOT_ANIMATION));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getLoadedColor(stack) != null;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * getCharge(stack) / PortalFluidTubeItem.MAX_CHARGE);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        PortalColor color = getLoadedColor(stack);
        return color == null ? 0x555555 : color.rgb();
    }

    private record PortalRef(UUID id, ResourceLocation dimension, @Nullable BlockPos position) {
        static PortalRef of(PortalEntity portal) {
            return new PortalRef(portal.getUUID(), portal.level().dimension().location(),portal.blockPosition());
        }
    }
}

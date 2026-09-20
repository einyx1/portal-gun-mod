package com.jhonfx.portalgun.event;

import com.jhonfx.portalgun.entity.PortalEntity;
import com.jhonfx.portalgun.init.ModSounds;
import com.jhonfx.portalgun.network.ModNetwork;
import com.jhonfx.portalgun.network.PortalTravelEffectPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nullable;

/** Two-sided, swept portal collision and immediate entity/fluid transfer. */
public class PortalTickHandler {
    /** Entity -> exit portal. The lock clears as soon as the entity leaves the exit area. */
    private final Map<UUID, UUID> exitLocks = new HashMap<>();
    private final Map<ServerLevel,List<PortalEntity>> portalCache = new WeakHashMap<>();

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        List<PortalEntity> portals=portalCache.computeIfAbsent(level,l->new CopyOnWriteArrayList<>());
        if(now%200==0||portals.isEmpty()){
            portals.clear();
            for(Entity entity:level.getAllEntities())if(entity instanceof PortalEntity portal&&portal.isAlive())portals.add(portal);
        } else portals.removeIf(portal->!portal.isAlive());
        for (PortalEntity portal : portals) {
            clearExitedLocks(level, portal);
            PortalEntity linked = portal.isAlive() ? portal.getLinkedPortal() : null;
            if (linked == null || !linked.isAlive()) continue;
            double radius = Math.max(portal.getDetectionRadius(), 1.15) + 0.75;
            AABB search = new AABB(portal.position(), portal.position()).inflate(
                    portal.getPortalWidth() * 0.5 + radius,
                    portal.getPortalHeight() * 0.5 + radius,
                    portal.getPortalWidth() * 0.5 + radius);
            List<Entity> nearby = level.getEntities(portal, search, entity ->
                    !(entity instanceof PortalEntity) && entity.isAlive()
                            && !entity.isPassenger()
                            && (!portal.getPersistentData().hasUUID(com.jhonfx.portalgun.entity.CinematicPortal.TRAVELER)
                                || portal.getPersistentData().getUUID(com.jhonfx.portalgun.entity.CinematicPortal.TRAVELER).equals(entity.getUUID()))
                            && !portal.getUUID().equals(exitLocks.get(entity.getUUID()))
                            && isInsidePortalTrigger(portal, entity));
            for (Entity entity : nearby) {
                Entity moved = teleportEntity(portal, linked, entity);
                if (moved != null) {
                    exitLocks.remove(entity.getUUID());
                    exitLocks.put(moved.getUUID(), linked.getUUID());
                    // Never paint the screen unless the server actually moved the player.
                    if (moved instanceof ServerPlayer player) ModNetwork.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new PortalTravelEffectPacket(portal.getColor().rgb(), portal.isHorizontal() ? 1 : 0));
                    level.playSound(null, portal.blockPosition(), ModSounds.PORTAL_ENTER.get(),
                            net.minecraft.sounds.SoundSource.PLAYERS, 0.75f, 1.08f);
                    if(portal.isCloseAfterUse()||linked.isCloseAfterUse()){
                        portal.discardWithViews();linked.discardWithViews();break;
                    }
                }
            }
            if ((now + portal.getId()) % 20 == 0) transferFluid(portal, linked);
        }
    }

    @SubscribeEvent public void onPortalJoin(EntityJoinLevelEvent event){
        if(event.getLevel() instanceof ServerLevel level&&event.getEntity() instanceof PortalEntity portal){
            List<PortalEntity> portals=portalCache.computeIfAbsent(level,l->new CopyOnWriteArrayList<>());
            if(!portals.contains(portal))portals.add(portal);
        }
    }

    @SubscribeEvent public void onPortalLeave(EntityLeaveLevelEvent event){
        if(event.getLevel() instanceof ServerLevel level&&event.getEntity() instanceof PortalEntity portal){
            List<PortalEntity> portals=portalCache.get(level);
            if(portals!=null)portals.remove(portal);
        }
    }

    private void clearExitedLocks(ServerLevel level, PortalEntity portal) {
        exitLocks.entrySet().removeIf(entry -> {
            if (!portal.getUUID().equals(entry.getValue())) return false;
            Entity entity = level.getEntity(entry.getKey());
            return entity == null || !isInsideExitLock(portal, entity);
        });
    }

    /** Bedrock behavior: entering the animated portal area is enough; no wall-plane crossing is required. */
    private boolean isInsidePortalTrigger(PortalEntity portal, Entity entity) {
        AABB current = entity.getBoundingBox();
        AABB previous = current.move(entity.xo-entity.getX(),entity.yo-entity.getY(),entity.zo-entity.getZ());
        AABB box = current.minmax(previous);
        Vec3 center = box.getCenter();
        Vec3 p = portal.position();
        Direction.Axis axis = portal.getPortalFacing().getAxis();
        double plane=component(p,axis),min=axis==Direction.Axis.X?box.minX:axis==Direction.Axis.Y?box.minY:box.minZ;
        double max=axis==Direction.Axis.X?box.maxX:axis==Direction.Axis.Y?box.maxY:box.maxZ;
        if(!overlaps(min,max,plane-portal.getDetectionRadius(),plane+portal.getDetectionRadius()))return false;
        double halfW = portal.getPortalWidth() * 0.5 + Math.max(box.getXsize(), box.getZsize()) * 0.35;
        double halfH = portal.getPortalHeight() * 0.5 + box.getYsize() * 0.35;
        return switch (axis) {
            case X -> overlaps(box.minZ, box.maxZ, p.z - halfW, p.z + halfW)
                    && overlaps(box.minY, box.maxY, p.y - halfH, p.y + halfH);
            case Y -> overlaps(box.minX, box.maxX, p.x - halfW, p.x + halfW)
                    && overlaps(box.minZ, box.maxZ, p.z - halfH, p.z + halfH);
            case Z -> overlaps(box.minX, box.maxX, p.x - halfW, p.x + halfW)
                    && overlaps(box.minY, box.maxY, p.y - halfH, p.y + halfH);
        };
    }

    private boolean isInsideExitLock(PortalEntity portal, Entity entity) {
        AABB box = entity.getBoundingBox();
        Vec3 delta = box.getCenter().subtract(portal.position());
        Direction.Axis axis = portal.getPortalFacing().getAxis();
        double normal = Math.abs(component(delta, axis));
        double limit = portal.getDetectionRadius() + 1.0 + Math.max(box.getXsize(), box.getYsize()) * 0.5;
        return normal <= limit && entity.distanceToSqr(portal) <= (limit + portal.getPortalWidth()) * (limit + portal.getPortalWidth());
    }

    private static double component(Vec3 value, Direction.Axis axis) {
        return axis == Direction.Axis.X ? value.x : axis == Direction.Axis.Y ? value.y : value.z;
    }

    private static boolean overlaps(double a0, double a1, double b0, double b1) {
        return a1 >= b0 && a0 <= b1;
    }

    private Entity teleportEntity(PortalEntity from, PortalEntity to, Entity entity) {
        if (to.level().dimension().location().equals(com.jhonfx.portalgun.federation.UnmortrickenFacility.ID)
                && entity instanceof ServerPlayer traveler && !traveler.isCreative()
                && !traveler.getPersistentData().getBoolean("PortalGunUnmortrickenUnlocked")) return null;
        Vec3 normal = Vec3.atLowerCornerOf(to.getPortalFacing().getNormal());
        double clearance = switch (to.getPortalFacing()) {
            case UP -> 0.12; // entity position is at its feet
            case DOWN -> entity.getBbHeight() + 0.22; // keep its head below a ceiling portal
            default -> Math.max(1.15, entity.getBbWidth() * 0.5 + 0.72);
        };
        Vec3 dest = to.isSafeMode() ? findSafeExit(to, entity, normal, clearance)
                : to.position().add(normal.scale(clearance));
        // UNMORTRICKEN is a closed orbital facility with several stacked
        // decks. Heightmap-based fallbacks can select its roof. Cross-world
        // arrivals always use the dedicated illuminated landing deck.
        if (entity.level() != to.level()
                && to.level().dimension().location().equals(com.jhonfx.portalgun.federation.UnmortrickenFacility.ID)) {
            BlockPos landing = com.jhonfx.portalgun.federation.UnmortrickenFacility.prepareArrival((ServerLevel)to.level());
            dest = Vec3.atBottomCenterOf(landing);
        }
        if (dest == null) return null;
        Vec3 oldVelocity = entity.getDeltaMovement();
        double speed = Math.max(0.32, oldVelocity.length());
        Vec3 velocity = transformVelocity(oldVelocity, from.getPortalFacing(), to.getPortalFacing());
        if (velocity.dot(normal) < 0.18) velocity = velocity.add(normal.scale(speed + 0.18));
        float yaw = entity.getYRot() + yawFor(to.getPortalFacing()) - yawFor(from.getPortalFacing()) + 180.0f;
        Entity moved = entity;
        List<Entity> passengers=new ArrayList<>(entity.getPassengers());
        if (entity.level() != to.level()) {
            ServerLevel target = (ServerLevel) to.level();
            if (entity instanceof ServerPlayer player) {
                player.teleportTo(target, dest.x, dest.y, dest.z, yaw, entity.getXRot());
            } else {
                passengers.forEach(Entity::stopRiding);
                moved = entity.changeDimension(target);
                if (moved == null) return null;
                moved.teleportTo(dest.x, dest.y, dest.z);
                for(Entity passenger:passengers){
                    Entity movedPassenger=passenger;
                    if(passenger instanceof ServerPlayer playerPassenger)
                        playerPassenger.teleportTo(target,dest.x,dest.y+moved.getPassengersRidingOffset(),dest.z,yaw,playerPassenger.getXRot());
                    else movedPassenger=passenger.changeDimension(target);
                    if(movedPassenger!=null){
                        movedPassenger.teleportTo(dest.x,dest.y+moved.getPassengersRidingOffset(),dest.z);
                        movedPassenger.startRiding(moved,true);
                    }
                }
            }
        } else {
            if (entity instanceof ServerPlayer player) player.connection.teleport(dest.x, dest.y, dest.z, yaw, entity.getXRot());
            else entity.teleportTo(dest.x, dest.y, dest.z);
        }
        moved.setYRot(yaw);
        moved.setDeltaMovement(velocity);
        moved.fallDistance = 0;
        return moved;
    }

    /**
     * Safety mode: keep the portal where it was created and find the nearest
     * genuinely safe standing position on the exit side. This avoids blocking
     * travel merely because the exact portal anchor is obstructed.
     */
    @Nullable
    private Vec3 findSafeExit(PortalEntity portal, Entity entity, Vec3 normal, double clearance) {
        ServerLevel level = (ServerLevel) portal.level();
        Vec3 requested = portal.position().add(normal.scale(clearance));

        if (isSafeExitPosition(level, entity, requested)) return requested;

        BlockPos origin = BlockPos.containing(requested);
        Vec3 best = null;
        double bestScore = Double.MAX_VALUE;
        final int horizontalRadius = 12;
        final int verticalRadius = 16;

        for (int x = -horizontalRadius; x <= horizontalRadius; x++) {
            for (int z = -horizontalRadius; z <= horizontalRadius; z++) {
                for (int y = -verticalRadius; y <= verticalRadius; y++) {
                    Vec3 candidate = new Vec3(origin.getX() + x + 0.5,
                            origin.getY() + y, origin.getZ() + z + 0.5);

                    // Never choose a point behind the exit portal's plane.
                    if (candidate.subtract(portal.position()).dot(normal) < 0.1) continue;
                    if (!isSafeExitPosition(level, entity, candidate)) continue;

                    double horizontalDistance = x * x + z * z;
                    double score = horizontalDistance + Math.abs(y) * 1.75;
                    if (score < bestScore) {
                        bestScore = score;
                        best = candidate;
                    }
                }
            }
        }

        if (best != null) return best;

        // A ceiling portal or a portal mounted above the floor may have no standing
        // block immediately beside it. Travel must still happen: choose the first
        // collision-free point on the outgoing side and let normal gravity take over.
        for (double distance = clearance; distance <= 5.0; distance += 0.25) {
            Vec3 candidate = portal.position().add(normal.scale(distance));
            if (isClearExitPosition(level, entity, candidate)) return candidate;
        }
        return null;
    }

    private boolean isClearExitPosition(ServerLevel level, Entity entity, Vec3 candidate) {
        AABB movedBox = entity.getBoundingBox().move(candidate.subtract(entity.position()));
        return movedBox.minY >= level.getMinBuildHeight()
                && movedBox.maxY < level.getMaxBuildHeight()
                && level.getWorldBorder().isWithinBounds(movedBox)
                && level.noCollision(entity, movedBox)
                && !level.containsAnyLiquid(movedBox)
                && !containsHazard(level, movedBox.inflate(0.08));
    }

    private boolean isSafeExitPosition(ServerLevel level, Entity entity, Vec3 candidate) {
        AABB currentBox = entity.getBoundingBox();
        AABB movedBox = currentBox.move(candidate.subtract(entity.position()));

        if (movedBox.minY < level.getMinBuildHeight()
                || movedBox.maxY >= level.getMaxBuildHeight()) return false;
        if (!level.getWorldBorder().isWithinBounds(movedBox)) return false;
        if (!level.noCollision(entity, movedBox) || level.containsAnyLiquid(movedBox)) return false;
        if (containsHazard(level, movedBox.inflate(0.12))) return false;

        return hasSafeFloor(level, movedBox);
    }

    private boolean hasSafeFloor(ServerLevel level, AABB box) {
        double insetX = Math.min(0.08, box.getXsize() * 0.2);
        double insetZ = Math.min(0.08, box.getZsize() * 0.2);
        double[] xs = {box.minX + insetX, box.maxX - insetX};
        double[] zs = {box.minZ + insetZ, box.maxZ - insetZ};
        int floorY = (int) Math.floor(box.minY - 0.05);

        for (double x : xs) {
            for (double z : zs) {
                BlockPos floorPos = BlockPos.containing(x, floorY, z);
                BlockState floor = level.getBlockState(floorPos);
                if (!floor.isFaceSturdy(level, floorPos, Direction.UP) || isHazard(floor)) return false;
            }
        }
        return true;
    }

    private boolean containsHazard(ServerLevel level, AABB box) {
        int minX = (int) Math.floor(box.minX);
        int minY = (int) Math.floor(box.minY);
        int minZ = (int) Math.floor(box.minZ);
        int maxX = (int) Math.floor(box.maxX);
        int maxY = (int) Math.floor(box.maxY);
        int maxZ = (int) Math.floor(box.maxZ);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.set(x, y, z);
                    if (!level.getFluidState(cursor).isEmpty() || isHazard(level.getBlockState(cursor))) return true;
                }
            }
        }
        return false;
    }

    private static boolean isHazard(BlockState state) {
        return state.is(Blocks.FIRE)
                || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.LAVA)
                || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.WITHER_ROSE)
                || state.is(Blocks.POWDER_SNOW);
    }

    private static Vec3 transformVelocity(Vec3 velocity, Direction from, Direction to) {
        Vec3 fromN = Vec3.atLowerCornerOf(from.getNormal());
        Vec3 toN = Vec3.atLowerCornerOf(to.getNormal());
        double along = velocity.dot(fromN);
        Vec3 tangent = velocity.subtract(fromN.scale(along));
        return tangent.add(toN.scale(-along));
    }

    private static float yawFor(Direction direction) {
        return switch (direction) {
            case SOUTH -> 0f;
            case WEST -> 90f;
            case NORTH -> 180f;
            case EAST -> -90f;
            default -> 0f;
        };
    }

    private void transferFluid(PortalEntity from, PortalEntity to) {
        ServerLevel sourceLevel = (ServerLevel) from.level();
        ServerLevel targetLevel = (ServerLevel) to.level();
        BlockPos source = BlockPos.containing(from.position());
        FluidState fluid = sourceLevel.getFluidState(source);
        if (fluid.isEmpty() || !fluid.isSource()) return;
        BlockPos target = BlockPos.containing(to.position()).relative(to.getPortalFacing());
        if (!targetLevel.getBlockState(target).canBeReplaced()) return;
        sourceLevel.setBlock(source, Blocks.AIR.defaultBlockState(), 3);
        targetLevel.setBlock(target, fluid.createLegacyBlock(), 3);
    }
}

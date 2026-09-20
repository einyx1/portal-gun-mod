package com.jhonfx.portalgun.entity;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import com.jhonfx.portalgun.init.ModSounds;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Entidade "portal" — port de entities/ram_pg/portals/*.json + a lógica de
 * pareamento em utils/utils.js (linkPortals / spawnPortal).
 *
 * Cada portal guarda a cor, a direção que ele "encara" (equivalente à
 * orientation/rotation do addon: parede/teto/chão), a escala e o UUID do
 * portal linkado (dualPortal no script original).
 */
public class PortalEntity extends Entity {

    private static final EntityDataAccessor<Integer> COLOR =
            SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FACING =
            SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE =
            SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> LIFETIME =
            SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SAFE_MODE =
            SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.BOOLEAN);

    private static final String TAG_LINKED_UUID = "LinkedPortal";
    private static final String TAG_LINKED_DIMENSION = "LinkedDimension";
    private static final String TAG_OWNER_UUID = "OwnerPlayer";
    private static final String TAG_LINKED_POS = "LinkedPortalPos";

    @Nullable
    private UUID linkedPortalId;
    @Nullable
    private ResourceKey<Level> linkedDimension;
    @Nullable
    private BlockPos linkedPortalPos;
    @Nullable
    private UUID ownerPlayerId;
    private boolean closeAfterUse;

    public PortalEntity(EntityType<? extends PortalEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.blocksBuilding = false;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(COLOR, PortalColor.BLUE.ordinal());
        this.entityData.define(FACING, Direction.NORTH.get3DDataValue());
        this.entityData.define(SCALE, 1.0f);
        this.entityData.define(LIFETIME, 20 * 10);
        this.entityData.define(SAFE_MODE, true);
    }

    public void setColor(PortalColor color) {
        this.entityData.set(COLOR, color.ordinal());
    }

    public PortalColor getColor() {
        int color=this.entityData.get(COLOR);
        return color>=0&&color<PortalColor.values().length?PortalColor.values()[color]:PortalColor.BLUE;
    }

    public void setPortalFacing(Direction direction) {
        this.entityData.set(FACING, direction.get3DDataValue());
    }

    public Direction getPortalFacing() {
        return Direction.from3DDataValue(this.entityData.get(FACING));
    }

    /** true = portal no chão/teto (orientation 2 no script original), false = parede (orientation 0/1). */
    public boolean isHorizontal() {
        Direction d = getPortalFacing();
        return d == Direction.UP || d == Direction.DOWN;
    }

    public void setPortalScale(float scale) {
        this.entityData.set(SCALE, scale);
    }

    public float getPortalScale() {
        return this.entityData.get(SCALE);
    }

    public void setLifetimeTicks(int ticks) {
        this.entityData.set(LIFETIME, Math.max(20 * 30, ticks));
    }

    public int getLifetimeTicks() {
        return this.entityData.get(LIFETIME);
    }
    public void setSafeMode(boolean enabled) { entityData.set(SAFE_MODE, enabled); }
    public boolean isSafeMode() { return entityData.get(SAFE_MODE); }
    public void setCloseAfterUse(boolean enabled){closeAfterUse=enabled;}
    public boolean isCloseAfterUse(){return closeAfterUse;}

    public double getPortalWidth() {
        return 5.0 * getPortalScale();
    }

    public double getPortalHeight() {
        return 4.875 * getPortalScale();
    }

    public void setLinkedPortalId(@Nullable UUID id) {
        this.linkedPortalId = id;
    }

    public void setLinkedPortal(PortalEntity portal) {
        this.linkedPortalId = portal.getUUID();
        this.linkedDimension = portal.level().dimension();
        this.linkedPortalPos = portal.blockPosition().immutable();
    }

    @Nullable
    public UUID getLinkedPortalId() {
        return linkedPortalId;
    }

    public void setOwnerPlayerId(@Nullable UUID id) {
        this.ownerPlayerId = id;
    }

    @Nullable
    public UUID getOwnerPlayerId() {
        return ownerPlayerId;
    }

    public void discardWithViews() {
        discard();
    }

    /** Equivalente a getActivePortalIds() + world.getEntity(dualityPortalId) em teleport.js */
    @Nullable
    public PortalEntity getLinkedPortal() {
        if (linkedPortalId == null || level().isClientSide) return null;
        ServerLevel current = (ServerLevel) level();
        ServerLevel target = linkedDimension == null ? current : current.getServer().getLevel(linkedDimension);
        if (target == null) return null;
        // Cross-dimensional entity lookup only sees loaded chunks. Keep the
        // remote anchor coordinates and synchronously load that single chunk
        // when somebody is close enough to use the local portal.
        if (linkedPortalPos != null) target.getChunkAt(linkedPortalPos);
        Entity e = target.getEntity(linkedPortalId);
        return e instanceof PortalEntity portal ? portal : null;
    }

    /** Raio de detecção — port de getPortalRadius() em teleport.js */
    public double getDetectionRadius() {
        float scale = getPortalScale();
        boolean wall = !isHorizontal();
        if (scale <= 0.5f) return 0.8;
        if (scale <= 1.0f) return wall ? 1.2 : 1.0;
        return wall ? 2.2 : 2.0;
    }

    @Override
    public void tick() {
        super.tick();
        // O deslocamento/teleporte em si é conduzido pelo PortalTickHandler
        // (equivalente ao onTick() global de teleport.js), não aqui —
        // assim como no addon, onde a lógica central roda uma vez por tick
        // do jogo iterando getActivePortalIds().
        this.setDeltaMovement(Vec3.ZERO);
        this.move(MoverType.SELF, Vec3.ZERO);
        if (!level().isClientSide && tickCount >= getLifetimeTicks()) discardWithViews();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (ownerPlayerId != null && !ownerPlayerId.equals(player.getUUID()) && !player.getAbilities().instabuild)
            return InteractionResult.FAIL;
        PortalEntity linked = getLinkedPortal();
        if (linked != null) linked.discardWithViews();
        level().playSound(null, blockPosition(), ModSounds.POWER_OFF.get(),
                net.minecraft.sounds.SoundSource.BLOCKS, 0.9f, 0.9f);
        discardWithViews();
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID(TAG_LINKED_UUID)) {
            this.linkedPortalId = tag.getUUID(TAG_LINKED_UUID);
        }
        if (tag.contains(TAG_LINKED_DIMENSION)) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString(TAG_LINKED_DIMENSION));
            if (id != null) this.linkedDimension = ResourceKey.create(Registries.DIMENSION, id);
        }
        if (tag.contains(TAG_LINKED_POS)) this.linkedPortalPos = BlockPos.of(tag.getLong(TAG_LINKED_POS));
        if (tag.hasUUID(TAG_OWNER_UUID)) {
            this.ownerPlayerId = tag.getUUID(TAG_OWNER_UUID);
        }
        int color=tag.getInt("Color");
        this.setColor(color>=0&&color<PortalColor.values().length?PortalColor.values()[color]:PortalColor.BLUE);
        this.setPortalFacing(Direction.from3DDataValue(tag.getInt("Facing")));
        this.setPortalScale(tag.getFloat("Scale"));
        if (tag.contains("Lifetime")) this.setLifetimeTicks(tag.getInt("Lifetime"));
        if (tag.contains("SafeMode")) this.setSafeMode(tag.getBoolean("SafeMode"));
        closeAfterUse=tag.getBoolean("CloseAfterUse");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (linkedPortalId != null) tag.putUUID(TAG_LINKED_UUID, linkedPortalId);
        if (linkedDimension != null) tag.putString(TAG_LINKED_DIMENSION, linkedDimension.location().toString());
        if (linkedPortalPos != null) tag.putLong(TAG_LINKED_POS, linkedPortalPos.asLong());
        if (ownerPlayerId != null) tag.putUUID(TAG_OWNER_UUID, ownerPlayerId);
        tag.putInt("Color", getColor().ordinal());
        tag.putInt("Facing", getPortalFacing().get3DDataValue());
        tag.putFloat("Scale", getPortalScale());
        tag.putInt("Lifetime", getLifetimeTicks());
        tag.putBoolean("SafeMode", isSafeMode());
        tag.putBoolean("CloseAfterUse",closeAfterUse);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {
        return true;
    }
}

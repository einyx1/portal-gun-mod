package com.jhonfx.portalgun.entity;

import com.jhonfx.portalgun.init.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;
import net.minecraft.core.particles.DustParticleOptions;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.UUID;

/** Pilotable, persistent flying vehicle designed to fit through Portal Cannon portals. */
public final class RickShipEntity extends Entity implements GeoEntity, MenuProvider {
    public static final int MAX_ENERGY = 12_000;
    private static final EntityDataAccessor<Integer> ENERGY = SynchedEntityData.defineId(RickShipEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HEALTH = SynchedEntityData.defineId(RickShipEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DEFENSE = SynchedEntityData.defineId(RickShipEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> AUTOPILOT = SynchedEntityData.defineId(RickShipEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.rick_ship.idle");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.rick_ship.fly");
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final SimpleContainer cargo = new SimpleContainer(9);
    @Nullable private UUID owner;
    @Nullable private BlockPos home;
    private String homeDimension = "";

    public RickShipEntity(EntityType<? extends RickShipEntity> type, Level level) {
        super(type, level);
        noPhysics = false;
        setNoGravity(true);
    }

    @Override protected void defineSynchedData() { entityData.define(ENERGY, MAX_ENERGY); entityData.define(HEALTH, 160); entityData.define(DEFENSE, true); entityData.define(AUTOPILOT, false); }
    public int getEnergy() { return entityData.get(ENERGY); }
    public void setEnergy(int value) { entityData.set(ENERGY, Math.max(0, Math.min(MAX_ENERGY, value))); }
    public int getShipHealth() { return entityData.get(HEALTH); }
    public void setShipHealth(int value) { entityData.set(HEALTH, Math.max(0, Math.min(160, value))); }
    public void setOwner(@Nullable UUID owner) { this.owner = owner; }
    public boolean isOwnedBy(Player player) { return owner == null || owner.equals(player.getUUID()) || player.getAbilities().instabuild; }
    public boolean isDefenseEnabled() { return entityData.get(DEFENSE); }
    public void setDefenseEnabled(boolean enabled) { entityData.set(DEFENSE, enabled); }
    public boolean isAutopilot() { return entityData.get(AUTOPILOT); }
    public void setHome(BlockPos pos) { home = pos.immutable(); homeDimension = level().dimension().location().toString(); }
    public boolean startReturn() { if (home == null || !homeDimension.equals(level().dimension().location().toString())) return false; entityData.set(AUTOPILOT, true); return true; }

    @Override public void tick() {
        super.tick();
        setNoGravity(true);
        Entity rider = getFirstPassenger();
        if (isAutopilot() && home != null && getEnergy() > 0) {
            Vec3 delta = Vec3.atCenterOf(home).subtract(position());
            if (delta.lengthSqr() < 5) { entityData.set(AUTOPILOT, false); setDeltaMovement(Vec3.ZERO); }
            else {
                Vec3 desired = delta.normalize().scale(.48);
                if(!level().noCollision(this,getBoundingBox().move(desired.scale(3)))){
                    Vec3 climb=new Vec3(desired.x*.25,.55,desired.z*.25);
                    desired=level().noCollision(this,getBoundingBox().move(climb.scale(2)))?climb:
                            new Vec3(-desired.z,.18,desired.x).normalize().scale(.42);
                }
                setYRot((float)(Math.toDegrees(Math.atan2(-desired.x, desired.z))));
                setDeltaMovement(getDeltaMovement().scale(.68).add(desired.scale(.32)));
                if (!level().isClientSide && tickCount % 10 == 0) setEnergy(getEnergy()-1);
            }
        } else if (rider instanceof Player player && getEnergy() > 0) {
            setYRot(player.getYRot());
            setXRot(player.getXRot() * 0.22f);
            yRotO = getYRot();
            float forward = player.zza;
            float strafe = player.xxa;
            Vec3 look = player.getLookAngle();
            Vec3 right = new Vec3(-Math.cos(Math.toRadians(getYRot())), 0, -Math.sin(Math.toRadians(getYRot())));
            double throttle = player.isSprinting() ? 0.68 : 0.42;
            Vec3 desired = look.scale(forward * throttle).add(right.scale(strafe * 0.28));
            if (player.isShiftKeyDown()) desired = desired.add(0, -0.32, 0);
            setDeltaMovement(getDeltaMovement().scale(0.72).add(desired.scale(0.28)));
            if (!level().isClientSide && tickCount % 10 == 0 && (Math.abs(forward) > 0.01 || Math.abs(strafe) > 0.01 || player.isShiftKeyDown())) setEnergy(getEnergy() - 1);
        } else setDeltaMovement(getDeltaMovement().scale(0.82).add(0, Math.sin(tickCount * 0.08) * 0.0015, 0));
        move(MoverType.SELF, getDeltaMovement());
        if (horizontalCollision) setDeltaMovement(getDeltaMovement().multiply(-0.18, 0.45, -0.18));
        fallDistance = 0;
        if (!level().isClientSide) {
            if (tickCount % 20 == 0 && rider instanceof ServerPlayer pilot)
                pilot.displayClientMessage(Component.translatable("message.portalgun.rick_ship_energy", getEnergy(), MAX_ENERGY), true);
            if (getDeltaMovement().lengthSqr() > 0.012 && level() instanceof net.minecraft.server.level.ServerLevel server) {
                Vec3 rear = position().subtract(getLookAngle().scale(1.8)).add(0, 0.35, 0);
                server.sendParticles(new DustParticleOptions(new Vector3f(0.05f, 0.95f, 1f), 1.3f), rear.x, rear.y, rear.z, 4, .3, .15, .3, .015);
            }
            if (isDefenseEnabled() && tickCount % 18 == 0 && getEnergy() >= 8) autoDefense((net.minecraft.server.level.ServerLevel) level(), rider);
        }
    }

    private void autoDefense(net.minecraft.server.level.ServerLevel level, @Nullable Entity rider) {
        Player protectedPlayer = rider instanceof Player player ? player : owner == null ? null : level.getPlayerByUUID(owner);
        Vec3 center = protectedPlayer == null ? position() : protectedPlayer.position();
        LivingEntity target = level.getEntitiesOfClass(LivingEntity.class,
                        new net.minecraft.world.phys.AABB(center, center).inflate(protectedPlayer == null ? 18 : 28),
                        entity -> entity instanceof Monster && entity.isAlive() && entity != protectedPlayer)
                .stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (target == null) return;
        Vec3 start = position().add(0, .7, 0), end = target.getEyePosition(), delta = end.subtract(start);
        net.minecraft.world.phys.HitResult obstruction=level.clip(new net.minecraft.world.level.ClipContext(start,end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,this));
        if(obstruction.getType()!=net.minecraft.world.phys.HitResult.Type.MISS)return;
        int steps = Math.max(8, Math.min(34, (int)(delta.length() * 1.5)));
        DustParticleOptions beam = new DustParticleOptions(new Vector3f(.05f, .95f, 1f), 1.15f);
        for (int i = 0; i <= steps; i++) { Vec3 point = start.add(delta.scale(i / (double) steps)); level.sendParticles(beam, point.x, point.y, point.z, 1, 0, 0, 0, 0); }
        target.hurt(damageSources().magic(), 11f);
        setEnergy(getEnergy() - 8);
        level.playSound(null, blockPosition(), SoundEvents.GUARDIAN_ATTACK, SoundSource.PLAYERS, .9f, 1.65f);
    }

    @Override public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.CHEST) && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, this);
            return InteractionResult.CONSUME;
        }
        if (held.is(ModItems.WEAPON_ENERGY_CELL.get()) && getEnergy() < MAX_ENERGY) {
            if (!level().isClientSide) {
                setEnergy(getEnergy() + 2400);
                if (!player.getAbilities().instabuild) held.shrink(1);
                level().playSound(null, blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1, 1.25f);
                player.displayClientMessage(Component.translatable("message.portalgun.rick_ship_recharged", getEnergy(), MAX_ENERGY), true);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (player.isShiftKeyDown() && getPassengers().isEmpty()) {
            if (!level().isClientSide && (owner == null || owner.equals(player.getUUID()) || player.getAbilities().instabuild)) {
                ItemStack ship = new ItemStack(ModItems.RICK_SHIP.get());
                saveToItem(ship);
                if (!player.addItem(ship)) spawnAtLocation(ship);
                discard();
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!level().isClientSide && !player.isPassenger()) player.startRiding(this);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override protected boolean canAddPassenger(Entity passenger) { return getPassengers().isEmpty(); }
    @Override public double getPassengersRidingOffset() { return 1.05; }
    @Override protected void positionRider(Entity passenger, MoveFunction move) {
        if (hasPassenger(passenger)) move.accept(passenger, getX(), getY() + getPassengersRidingOffset(), getZ());
    }
    @Override public boolean isPickable() { return true; }
    @Override public boolean isPushable() { return true; }
    @Override public boolean canCollideWith(Entity other) { return other.canBeCollidedWith() && !isPassengerOfSameVehicle(other); }
    @Override public boolean canBeCollidedWith() { return true; }
    @Override public boolean fireImmune() { return true; }

    @Override public Component getDisplayName() { return Component.translatable("container.portalgun.rick_ship"); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new ChestMenu(MenuType.GENERIC_9x1, id, inventory, cargo, 1); }

    public void saveToItem(ItemStack stack) {
        CompoundTag data = new CompoundTag();
        data.putInt("Energy", getEnergy()); data.putInt("Health", getShipHealth());
        data.putBoolean("Defense", isDefenseEnabled());
        if (home != null) { data.putLong("Home", home.asLong()); data.putString("HomeDimension", homeDimension); }
        data.put("Cargo", saveCargo());
        stack.getOrCreateTag().put("ShipData", data);
    }

    public void loadFromItem(ItemStack stack) {
        if (!stack.hasTag()) return;
        CompoundTag data = stack.getTag().contains("ShipData") ? stack.getTag().getCompound("ShipData") : stack.getTag();
        if (data.contains("Energy")) setEnergy(data.getInt("Energy"));
        if (data.contains("Health")) setShipHealth(data.getInt("Health"));
        if (data.contains("Defense")) setDefenseEnabled(data.getBoolean("Defense"));
        if (data.contains("Home")) { home=BlockPos.of(data.getLong("Home")); homeDimension=data.getString("HomeDimension"); }
        if (data.contains("Cargo", Tag.TAG_LIST)) loadCargo(data.getList("Cargo", Tag.TAG_COMPOUND));
    }

    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (level().isClientSide) return true;
        if (isInvulnerableTo(source) || source.getEntity() == getFirstPassenger()) return false;
        setShipHealth(getShipHealth() - Math.max(1, Math.round(amount)));
        level().playSound(null, blockPosition(), SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.PLAYERS, 1, .9f);
        if (getShipHealth() <= 0) {
            ejectPassengers();
            if (level() instanceof net.minecraft.server.level.ServerLevel server) server.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, getX(), getY()+.5, getZ(), 2, 1, .5, 1, .05);
            net.minecraft.world.Containers.dropContents(level(), blockPosition(), cargo);
            spawnAtLocation(new ItemStack(ModItems.RICK_SHIP.get()));
            discard();
        }
        return true;
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        setEnergy(tag.contains("Energy") ? tag.getInt("Energy") : MAX_ENERGY);
        setShipHealth(tag.contains("Health") ? tag.getInt("Health") : 160);
        if (tag.hasUUID("Owner")) owner = tag.getUUID("Owner");
        setDefenseEnabled(!tag.contains("Defense") || tag.getBoolean("Defense"));
        entityData.set(AUTOPILOT, tag.getBoolean("Autopilot"));
        if (tag.contains("Home")) { home=BlockPos.of(tag.getLong("Home")); homeDimension=tag.getString("HomeDimension"); }
        if (tag.contains("Cargo", Tag.TAG_LIST)) loadCargo(tag.getList("Cargo", Tag.TAG_COMPOUND));
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Energy", getEnergy());
        tag.putInt("Health", getShipHealth());
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putBoolean("Defense", isDefenseEnabled()); tag.putBoolean("Autopilot", isAutopilot());
        if (home != null) { tag.putLong("Home", home.asLong()); tag.putString("HomeDimension", homeDimension); }
        tag.put("Cargo", saveCargo());
    }
    private ListTag saveCargo(){ListTag list=new ListTag();for(int slot=0;slot<cargo.getContainerSize();slot++){ItemStack stack=cargo.getItem(slot);if(stack.isEmpty())continue;CompoundTag entry=new CompoundTag();entry.putByte("Slot",(byte)slot);stack.save(entry);list.add(entry);}return list;}
    private void loadCargo(ListTag list){cargo.clearContent();for(int i=0;i<list.size();i++){CompoundTag entry=list.getCompound(i);int slot=entry.getByte("Slot")&255;if(slot<cargo.getContainerSize())cargo.setItem(slot,ItemStack.of(entry));}}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "engine", 2, state -> state.setAndContinue(getDeltaMovement().lengthSqr() > .01 ? FLY : IDLE)));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animationCache; }
}

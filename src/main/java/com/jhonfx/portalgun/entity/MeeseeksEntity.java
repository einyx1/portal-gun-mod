package com.jhonfx.portalgun.entity;

import com.jhonfx.portalgun.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;

public final class MeeseeksEntity extends PathfinderMob implements GeoEntity {
    private static final RawAnimation IDLE  = RawAnimation.begin().thenLoop("animation.meeseeks.idle");
    private static final RawAnimation WALK  = RawAnimation.begin().thenLoop("animation.meeseeks.walk");
    private static final RawAnimation PANIC = RawAnimation.begin().thenLoop("animation.meeseeks.panic");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private UUID ownerId;
    private UUID targetId;
    private MeeseeksOrder order  = MeeseeksOrder.FOLLOW;
    private int  progress;
    private int  activeTicks;
    private int  stuckTicks;
    private int  patrolAngle;        // for PATROL order
    private BlockPos guideTarget;
    private BlockPos buildOrigin;
    private net.minecraft.resources.ResourceLocation requestedItem;
    private net.minecraft.resources.ResourceLocation requestedBlock;
    private Vec3 lastWorkPosition;
    private int noMovementTicks;

    // ── Saved project NBT key ────────────────────────────────────────────────
    public static final String TAG_SAVED_PROJECT = "MeeseeksSavedProject";

    public MeeseeksEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,     40)
                .add(Attributes.MOVEMENT_SPEED, .31)
                .add(Attributes.ATTACK_DAMAGE,   6)
                .add(Attributes.FOLLOW_RANGE,   48);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    // ── Configuration ─────────────────────────────────────────────────────────
    public void configure(ServerPlayer owner, MeeseeksOrder newOrder, LivingEntity target) {
        getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(ModConfig.MEESEEKS_RANGE.get());
        ownerId   = owner.getUUID();
        order     = newOrder;
        targetId  = target == null ? null : target.getUUID();
        progress  = 0;
        stuckTicks = 0;
        patrolAngle = 0;
        ItemStack selector=owner.getOffhandItem();
        if(!selector.isEmpty()&&!(selector.getItem() instanceof com.jhonfx.portalgun.item.MeeseeksBoxItem)){
            requestedItem=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(selector.getItem());
            if(selector.getItem() instanceof net.minecraft.world.item.BlockItem blockItem)
                requestedBlock=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(blockItem.getBlock());
        }
        if(newOrder==MeeseeksOrder.BUILD||newOrder==MeeseeksOrder.BUILD_WALL
                ||newOrder==MeeseeksOrder.BUILD_BRIDGE||newOrder==MeeseeksOrder.BUILD_TOWER)
            buildOrigin=owner.blockPosition().relative(owner.getDirection(),6);
        if (newOrder == MeeseeksOrder.GUIDE || newOrder == MeeseeksOrder.PORTAL_HELP)
            guideTarget = findStandable(owner.serverLevel(), owner.blockPosition().offset(
                    (int)(owner.getLookAngle().x * 24), 0, (int)(owner.getLookAngle().z * 24)));
        updateLabel();
    }

    // ── Tick ──────────────────────────────────────────────────────────────────
    @Override public void tick() {
        super.tick();
        if (level().isClientSide || ownerId == null) return;
        activeTicks++;

        ServerPlayer owner = ((ServerLevel)level()).getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null || owner.level() != level()) {
            if (activeTicks > 1200) discard();
            return;
        }

        int lifetimeTicks = ModConfig.MEESEEKS_LIFETIME_MINUTES.get() * 60 * 20;
        if (tickCount % 10 == 0) execute(owner);
        if(tickCount%20==0&&order!=MeeseeksOrder.FOLLOW&&order!=MeeseeksOrder.PROTECT&&order!=MeeseeksOrder.PATROL){
            if(lastWorkPosition!=null&&position().distanceToSqr(lastWorkPosition)<.16)noMovementTicks+=20;else noMovementTicks=0;
            lastWorkPosition=position();
            if(noMovementTicks==300){getNavigation().stop();BlockPos alternate=findStandable((ServerLevel)level(),blockPosition().offset(random.nextInt(17)-8,0,random.nextInt(17)-8));getNavigation().moveTo(alternate.getX()+.5,alternate.getY(),alternate.getZ()+.5,1.2);owner.displayClientMessage(Component.literal("§eMR. MEESEEKS: §fVou tentar uma rota alternativa!"),true);}
            if(noMovementTicks>700){stuckTicks+=200;noMovementTicks=0;}
        }
        if (tickCount % 20 == 0) updateLabel();
        if (activeTicks > lifetimeTicks) {
            stuckTicks += 20;
            if (stuckTicks > 200) crisis(owner);
        }
    }

    // ── Order dispatch ────────────────────────────────────────────────────────
    private void execute(ServerPlayer owner) {
        switch (order) {
            case FOLLOW      -> follow(owner, 3.0);
            case PROTECT     -> { protect(owner); follow(owner, 5.0); }
            case PORTAL_HELP -> { protect(owner); follow(owner, 5.0); }
            case KILL        -> killSelected(owner);
            case COLLECT     -> collect(owner, false);
            case COLLECT_ALL -> collect(owner, true);
            case GUIDE       -> guide(owner);
            case MINE        -> mine(owner, false);
            case MINE_ORE    -> mine(owner, true);
            case MINE_LOGS   -> mineLogs(owner);
            case BUILD       -> build(owner, 0);
            case BUILD_WALL  -> build(owner, 1);
            case BUILD_BRIDGE-> build(owner, 2);
            case BUILD_TOWER -> build(owner, 3);
            case TRAINING    -> {
                protect(owner);
                progress = Math.min(order.goal(), progress + 1);
                if (progress >= order.goal()) complete(owner);
            }
            case FARM        -> farm(owner);
            case PATROL      -> patrol(owner);
            case CARRY       -> follow(owner, 2.0); // carry: just stay close, handled on death
        }
    }

    // ── Behaviours ────────────────────────────────────────────────────────────
    private void follow(ServerPlayer owner, double stop) {
        if (distanceToSqr(owner) > stop * stop) getNavigation().moveTo(owner, 1.08);
        else getNavigation().stop();
    }

    private void protect(ServerPlayer owner) {
        level().getEntitiesOfClass(Monster.class, owner.getBoundingBox().inflate(12), Monster::isAlive)
                .stream().min(Comparator.comparingDouble(owner::distanceToSqr))
                .ifPresent(this::setTarget);
    }

    private void killSelected(ServerPlayer owner) {
        var target = targetId == null ? null : ((ServerLevel)level()).getEntity(targetId);
        if (target instanceof LivingEntity living && living.isAlive()) {
            setTarget(living);
            getNavigation().moveTo(living, 1.2);
        } else if (targetId != null) {
            progress = 1; complete(owner);
        } else {
            protect(owner);
        }
    }

    private void collect(ServerPlayer owner, boolean indefinite) {
        int range = ModConfig.MEESEEKS_RANGE.get();
        var items = level().getEntitiesOfClass(ItemEntity.class,
                getBoundingBox().inflate(range), e->e.isAlive()&&(requestedItem==null||requestedItem.equals(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(e.getItem().getItem()))));
        if (items.isEmpty()) { follow(owner, 5); return; }
        ItemEntity item = items.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElseThrow();
        if (distanceToSqr(item) > 2.4) {
            getNavigation().moveTo(item, 1.18);
        } else {
            int count = item.getItem().getCount();
            if (!owner.getInventory().add(item.getItem().copy())) owner.drop(item.getItem().copy(), false);
            item.discard();
            progress += count;
            if (!indefinite && progress >= order.goal()) complete(owner);
        }
    }

    private void guide(ServerPlayer owner) {
        if (guideTarget == null) { stuckTicks += 10; return; }
        getNavigation().moveTo(guideTarget.getX() + .5, guideTarget.getY(), guideTarget.getZ() + .5, 1.1);
        if (blockPosition().distSqr(guideTarget) < 6) {
            progress = 1;
            owner.displayClientMessage(Component.literal(
                    "Mr. Meeseeks encontrou ponto seguro: " + guideTarget.toShortString()), false);
            complete(owner);
        }
    }

    private void mine(ServerPlayer owner, boolean oreOnly) {
        if (!canGrief()) { stuckTicks += 10; return; }
        BlockPos base = blockPosition();
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-5,-4,-5), base.offset(5,4,5))) {
            BlockState s = level().getBlockState(p);
            if (!s.is(BlockTags.MINEABLE_WITH_PICKAXE)) continue;
            if(requestedBlock!=null&&!requestedBlock.equals(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(s.getBlock())))continue;
            float speed = s.getDestroySpeed(level(), p);
            if (speed < 0 || speed > 12) continue;
            if (level().getBlockEntity(p) != null) continue;
            if (oreOnly && !s.getBlock().getDescriptionId().contains("_ore")) continue;
            candidates.add(p.immutable());
        }
        candidates.sort(Comparator.comparingInt(p -> level().getBlockState(p).getBlock()
                .getDescriptionId().contains("_ore") ? 0 : 1));
        int broken = 0;
        for (BlockPos p : candidates) {
            if (progress >= order.goal() || broken >= 4) break;
            if (((ServerLevel)level()).destroyBlock(p, true, this)) { progress++; broken++; }
        }
        if (progress >= order.goal()) complete(owner);
        else follow(owner, 5);
    }

    private void mineLogs(ServerPlayer owner){
        if(!canGrief()){stuckTicks+=10;return;}int broken=0;
        for(BlockPos p:BlockPos.betweenClosed(blockPosition().offset(-7,-3,-7),blockPosition().offset(7,7,7))){
            if(broken>=4||progress>=order.goal())break;BlockState state=level().getBlockState(p);
            if(!state.is(BlockTags.LOGS)||level().getBlockEntity(p)!=null)continue;
            if(((ServerLevel)level()).destroyBlock(p,true,this)){progress++;broken++;}
        }
        if(progress>=order.goal())complete(owner);else follow(owner,6);
    }

    private void build(ServerPlayer owner, int project) {
        if (!canGrief()) { stuckTicks += 10; return; }
        BlockPos origin = buildOrigin==null?owner.blockPosition().relative(owner.getDirection(),6):buildOrigin;
        BlockState selectedMaterial=null;
        if(requestedBlock!=null){
            net.minecraft.world.level.block.Block selected=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(requestedBlock);
            if(selected!=null&&!selected.defaultBlockState().isAir())selectedMaterial=selected.defaultBlockState();
        }
        List<BlockPos> plan = new ArrayList<>();
        if (project==1) {
            // Simple 7-block-wide, 4-block-tall wall
            for (int w = -3; w <= 3; w++) for (int h = 0; h <= 3; h++)
                plan.add(origin.offset(w, h, 0));
        } else if(project==0) {
            // 5×5 shelter with roof
            for (int dx=-2;dx<=2;dx++) for (int dz=-2;dz<=2;dz++) plan.add(origin.offset(dx,-1,dz));
            for (int dy=0;dy<=2;dy++) for (int dx=-2;dx<=2;dx++) for (int dz=-2;dz<=2;dz++)
                if (Math.abs(dx)==2||Math.abs(dz)==2)
                    if (!(dz==-2&&dx==0&&dy<2)) plan.add(origin.offset(dx,dy,dz));
            for (int dx=-2;dx<=2;dx++) for (int dz=-2;dz<=2;dz++) plan.add(origin.offset(dx,3,dz));
        } else if(project==2) {
            Direction forward=owner.getDirection(),side=forward.getClockWise();
            for(int length=0;length<21;length++)for(int width=-1;width<=1;width++)
                plan.add(origin.relative(forward,length).relative(side,width));
        } else {
            for(int y=0;y<12;y++)for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)
                if(y==0||Math.abs(dx)==2||Math.abs(dz)==2)plan.add(origin.offset(dx,y,dz));
        }
        int placed = 0;
        for (BlockPos p : plan) {
            if (progress >= order.goal() || placed >= 6) break;
            if (level().getBlockState(p).canBeReplaced() && level().getBlockEntity(p) == null) {
                BlockState material=selectedMaterial!=null?selectedMaterial:(project==1?Blocks.STONE_BRICKS
                        :project==2?Blocks.SPRUCE_PLANKS
                        :project==3?(p.getY()%3==0?Blocks.SEA_LANTERN:Blocks.DEEPSLATE_BRICKS)
                        :p.getY()==origin.getY()+3?Blocks.OAK_PLANKS:Blocks.COBBLESTONE).defaultBlockState();
                level().setBlock(p,material,3);
                progress++; placed++;
            }
        }
        if (progress >= order.goal()) complete(owner);
        else stuckTicks += 10;
    }

    private void farm(ServerPlayer owner) {
        if (!canGrief()) { stuckTicks += 10; return; }
        BlockPos base = blockPosition();
        int harvested = 0;
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-8,-2,-8), base.offset(8,2,8))) {
            if (harvested >= 4 || progress >= order.goal()) break;
            BlockState state = level().getBlockState(p);
            if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
                level().destroyBlock(p, true, this);
                level().setBlock(p, crop.defaultBlockState(), 3);
                progress++; harvested++;
            }
        }
        if (progress >= order.goal()) complete(owner);
        else follow(owner, 8);
    }

    private void patrol(ServerPlayer owner) {
        patrolAngle = (patrolAngle + 3) % 360;
        double rad = Math.toRadians(patrolAngle);
        double r = 12;
        BlockPos target = new BlockPos(
                (int)(owner.getX() + Math.cos(rad)*r),
                owner.blockPosition().getY(),
                (int)(owner.getZ() + Math.sin(rad)*r));
        int surface = level().getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                target.getX(), target.getZ());
        target = new BlockPos(target.getX(), surface, target.getZ());
        if (blockPosition().distSqr(target) > 9) getNavigation().moveTo(target.getX()+.5, target.getY(), target.getZ()+.5, 1.05);
        protect(owner);
    }

    private void crisis(ServerPlayer owner) {
        owner.displayClientMessage(Component.literal(
                "MR. MEESEEKS: A tarefa parece impossível! §cEXISTENCE IS PAIN!§r Altere a ordem."), false);
        stuckTicks  = 0;
        activeTicks = (int)(ModConfig.MEESEEKS_LIFETIME_MINUTES.get() * 60 * 20 * 0.5);
    }

    private void complete(ServerPlayer owner) {
        if (isRemoved()) return;
        owner.displayClientMessage(Component.literal(
                "§aMR. MEESEEKS: §fEXISTENCE IS PAIN! §aTarefa concluída!"), false);
        ServerLevel server = (ServerLevel) level();
        server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY()+1, getZ(), 70, .55, 1, .55, .04);
        server.sendParticles(ParticleTypes.CLOUD,           getX(), getY()+1, getZ(), 25, .3, .6, .3, .05);
        discard();
    }

    private void updateLabel() {
        int secs = activeTicks / 20;
        int maxSecs = ModConfig.MEESEEKS_LIFETIME_MINUTES.get() * 60;
        String mood = activeTicks > maxSecs * 20 * 0.75 ? "§cCRISE"
                    : activeTicks > maxSecs * 20 * 0.4  ? "§eIMPACIENTE"
                    : "§aEXECUTANDO";
        setCustomName(Component.literal(String.format(
                "TAREFA: %s | %d/%d | %02d:%02d | %s",
                order.label()+(requestedItem==null?"":" ["+requestedItem.getPath()+"]"), progress, order.goal() == 0 ? 99 : order.goal(),
                secs/60, secs%60, mood)));
        setCustomNameVisible(true);
    }

    private boolean canGrief() {
        return ModConfig.MEESEEKS_GRIEFING.get()
                && level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
    }

    private static BlockPos findStandable(ServerLevel level, BlockPos around) {
        for (int radius = 0; radius <= 12; radius++) {
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
                int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        around.getX()+x, around.getZ()+z);
                BlockPos p = new BlockPos(around.getX()+x, y, around.getZ()+z);
                if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()
                        && level.getBlockState(p.below()).isSolidRender(level, p.below())) return p;
            }
        }
        return around;
    }

    // ── Serialization ─────────────────────────────────────────────────────────
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerId    != null) tag.putUUID("Owner",   ownerId);
        if (targetId   != null) tag.putUUID("Target",  targetId);
        if (guideTarget != null) tag.putLong("Guide",  guideTarget.asLong());
        if (buildOrigin != null) tag.putLong("BuildOrigin",buildOrigin.asLong());
        if(requestedItem!=null)tag.putString("RequestedItem",requestedItem.toString());
        if(requestedBlock!=null)tag.putString("RequestedBlock",requestedBlock.toString());
        tag.putString("Order",       order.name());
        tag.putInt("Progress",       progress);
        tag.putInt("ActiveTicks",    activeTicks);
        tag.putInt("PatrolAngle",    patrolAngle);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner"))  ownerId    = tag.getUUID("Owner");
        if (tag.hasUUID("Target")) targetId   = tag.getUUID("Target");
        if (tag.contains("Guide")) guideTarget = BlockPos.of(tag.getLong("Guide"));
        if (tag.contains("BuildOrigin")) buildOrigin = BlockPos.of(tag.getLong("BuildOrigin"));
        if(tag.contains("RequestedItem"))requestedItem=net.minecraft.resources.ResourceLocation.tryParse(tag.getString("RequestedItem"));
        if(tag.contains("RequestedBlock"))requestedBlock=net.minecraft.resources.ResourceLocation.tryParse(tag.getString("RequestedBlock"));
        try { order = MeeseeksOrder.valueOf(tag.getString("Order")); } catch (Exception ignored) {}
        progress    = tag.getInt("Progress");
        activeTicks = tag.getInt("ActiveTicks");
        patrolAngle = tag.getInt("PatrolAngle");
    }

    // ── GeckoLib ──────────────────────────────────────────────────────────────
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 4, state -> {
            int maxTicks = ModConfig.MEESEEKS_LIFETIME_MINUTES.get() * 60 * 20;
            state.setAnimation(activeTicks > maxTicks * 0.75 ? PANIC
                    : state.isMoving() ? WALK : IDLE);
            return software.bernie.geckolib.core.object.PlayState.CONTINUE;
        }));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}

package com.jhonfx.portalgun.block.entity;

import com.jhonfx.portalgun.block.OmegaDeviceBlock;
import com.jhonfx.portalgun.init.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class OmegaMachineBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.omega.idle");
    private static final RawAnimation ACTIVE = RawAnimation.begin().thenLoop("animation.omega.active");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public OmegaMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.OMEGA_MACHINE.get(), pos, state);
    }

    public boolean isActive() {
        return getBlockState().hasProperty(OmegaDeviceBlock.ACTIVE)
                && getBlockState().getValue(OmegaDeviceBlock.ACTIVE);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "omega_machine", 4,
                state -> state.setAndContinue(isActive() ? ACTIVE : IDLE)));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(8.0);
    }
}

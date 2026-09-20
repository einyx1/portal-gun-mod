package com.jhonfx.portalgun.block;

import com.jhonfx.portalgun.block.entity.OmegaMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import javax.annotation.Nullable;

public class OmegaComponentBlock extends BaseEntityBlock {
    private final OmegaComponent component;

    public OmegaComponentBlock(Properties properties, OmegaComponent component) {
        super(properties);
        this.component = component;
        registerDefaultState(stateDefinition.any()
                .setValue(OmegaDeviceBlock.FACING, Direction.NORTH)
                .setValue(OmegaDeviceBlock.ACTIVE, false));
    }

    public OmegaComponent component() { return component; }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(OmegaDeviceBlock.FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OmegaDeviceBlock.FACING, OmegaDeviceBlock.ACTIVE);
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    @Nullable
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OmegaMachineBlockEntity(pos, state);
    }
}

package com.jhonfx.portalgun.block;

import com.jhonfx.portalgun.entity.PortalColor;
import com.jhonfx.portalgun.init.ModItems;
import com.jhonfx.portalgun.item.PortalFluidTubeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.jhonfx.portalgun.block.entity.PortalSolutionBlockEntity;
import javax.annotation.Nullable;

public class PortalSolutionBlock extends BaseEntityBlock {
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 1, 3);
    private final PortalColor color;
    private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 6, 11);

    public PortalSolutionBlock(Properties properties, PortalColor color) {
        super(properties); this.color = color;
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 3));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    public PortalColor color() { return color; }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                               BlockPos pos, CollisionContext context) { return SHAPE; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PortalSolutionBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        boolean empty = held.is(ModItems.EMPTY_TUBE.get());
        boolean matching = held.getItem() instanceof PortalFluidTubeItem tube && tube.color() == color
                && PortalFluidTubeItem.getCharge(held) < PortalFluidTubeItem.MAX_CHARGE;
        if (!empty && !matching) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ItemStack filled = matching ? held.copy() : new ItemStack(switch (color) {
            case GREEN -> ModItems.GREEN_PORTAL_FLUID.get();
            case BLUE -> ModItems.BLUE_PORTAL_FLUID.get();
            case YELLOW -> ModItems.YELLOW_PORTAL_FLUID.get();
        });
        PortalFluidTubeItem.setCharge(filled, PortalFluidTubeItem.MAX_CHARGE);
        if (player.getAbilities().instabuild) {
            player.setItemInHand(hand, filled);
        } else {
            held.shrink(1);
            if (held.isEmpty()) player.setItemInHand(hand, filled);
            else if (!player.getInventory().add(filled)) player.drop(filled, false);
        }
        int remaining = state.getValue(LEVEL) - 1;
        if (remaining > 0) level.setBlock(pos, state.setValue(LEVEL, remaining), 3);
        else {
            level.removeBlock(pos, false);
            if (player instanceof ServerPlayer serverPlayer)
                serverPlayer.getInventory().add(new ItemStack(ModItems.BEAKER.get()));
        }
        return InteractionResult.CONSUME;
    }
}

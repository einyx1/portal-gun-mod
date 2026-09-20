package com.jhonfx.portalgun.block;

import com.jhonfx.portalgun.citadel.CitadelEconomy;
import com.jhonfx.portalgun.citadel.CitadelMissions;
import com.jhonfx.portalgun.network.CitadelScreenPacket;
import com.jhonfx.portalgun.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.PacketDistributor;

public final class CitadelServiceBlock extends Block {
    public enum Service { SHOP, WORKSHOP, MISSION, SCANNER, DRIVE, CURVE, ARCHIVE }
    private final Service service;

    public CitadelServiceBlock(Properties properties, Service service) {
        super(properties); this.service=service;
    }

    public Service service(){return service;}

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if(level.isClientSide) return InteractionResult.SUCCESS;
        if(!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.CONSUME;
        if(service==Service.MISSION) CitadelMissions.open(serverPlayer);
        else if(service==Service.SCANNER||service==Service.DRIVE||service==Service.CURVE||service==Service.ARCHIVE)
            com.jhonfx.portalgun.curve.CurveProgress.open(serverPlayer,service.name());
        else ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                new CitadelScreenPacket(service.name(),CitadelEconomy.count(player),0,0,0,0));
        return InteractionResult.CONSUME;
    }
}

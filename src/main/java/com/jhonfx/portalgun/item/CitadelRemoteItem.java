package com.jhonfx.portalgun.item;

import com.jhonfx.portalgun.PortalGunMod;
import com.jhonfx.portalgun.citadel.CitadelBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.Heightmap;

public final class CitadelRemoteItem extends Item {
    public static final ResourceKey<Level> CITADEL = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation(PortalGunMod.MOD_ID,"citadel"));
    private static final String RETURN = "CitadelReturn";

    public CitadelRemoteItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack=player.getItemInHand(hand);
        if(level.isClientSide) return InteractionResultHolder.success(stack);
        ServerPlayer serverPlayer=(ServerPlayer)player;
        ServerLevel current=(ServerLevel)level;
        if(current.dimension().equals(CITADEL)) leave(serverPlayer,stack);
        else enter(serverPlayer,stack,current);
        return InteractionResultHolder.consume(stack);
    }

    private void enter(ServerPlayer player, ItemStack stack, ServerLevel current) {
        ServerLevel citadel=player.server.getLevel(CITADEL);
        if(citadel==null) {
            player.displayClientMessage(Component.translatable("message.portalgun.citadel_unavailable"),true);
            return;
        }
        CompoundTag back=new CompoundTag();
        back.putString("Dimension",current.dimension().location().toString());
        back.putDouble("X",player.getX()); back.putDouble("Y",player.getY()); back.putDouble("Z",player.getZ());
        stack.getOrCreateTag().put(RETURN,back);
        CitadelBuilder.ensureBuilt(citadel);
        player.teleportTo(citadel,CitadelBuilder.ARRIVAL.getX()+0.5,CitadelBuilder.ARRIVAL.getY(),
                CitadelBuilder.ARRIVAL.getZ()+0.5,180,0);
        player.displayClientMessage(Component.translatable("message.portalgun.citadel_arrival"),false);
    }

    private void leave(ServerPlayer player, ItemStack stack) {
        CompoundTag back=stack.getOrCreateTag().getCompound(RETURN);
        ResourceLocation id=ResourceLocation.tryParse(back.getString("Dimension"));
        ServerLevel target=id==null?null:player.server.getLevel(ResourceKey.create(Registries.DIMENSION,id));
        if(target==null) target=player.server.overworld();
        double x=back.contains("X")?back.getDouble("X"):target.getSharedSpawnPos().getX()+0.5;
        double y=back.contains("Y")?back.getDouble("Y"):target.getSharedSpawnPos().getY()+1;
        double z=back.contains("Z")?back.getDouble("Z"):target.getSharedSpawnPos().getZ()+0.5;
        BlockPos requested=BlockPos.containing(x,y,z);
        if(!isSafe(target,requested)) {
            int surface=target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,requested.getX(),requested.getZ());
            BlockPos fallback=new BlockPos(requested.getX(),surface,requested.getZ());
            requested=isSafe(target,fallback)?fallback:target.getSharedSpawnPos().above();
            x=requested.getX()+0.5; y=requested.getY(); z=requested.getZ()+0.5;
        }
        player.teleportTo(target,x,y,z,player.getYRot(),player.getXRot());
    }

    private boolean isSafe(ServerLevel level, BlockPos feet) {
        return level.getBlockState(feet).isAir()&&level.getBlockState(feet.above()).isAir()
                &&level.getBlockState(feet.below()).isFaceSturdy(level,feet.below(), Direction.UP);
    }
}

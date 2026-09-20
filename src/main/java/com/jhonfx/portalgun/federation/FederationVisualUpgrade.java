package com.jhonfx.portalgun.federation;

import com.jhonfx.portalgun.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Incremental material migration for Federation bases made by older previews. */
public final class FederationVisualUpgrade {
    private static final ResourceLocation ID=new ResourceLocation("portalgun","federation");
    private static final int MIN_X=-95,MAX_X=95,MIN_Z=-110,MAX_Z=90;
    private static final int WIDTH=MAX_X-MIN_X+1,TOTAL=WIDTH*(MAX_Z-MIN_Z+1);

    @SubscribeEvent public void tick(TickEvent.LevelTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.level instanceof ServerLevel level)
                ||!level.dimension().location().equals(ID))return;
        FederationBuilder.tickBuild(level);
        if(FederationBuilder.isBuilding(level))return;
        FederationSavedData data=FederationSavedData.get(level);
        if(data.isDestroyed()||data.collapseTicks()>0)return;
        if(!data.visualUpgrade())return;
        int start=data.visualUpgradeCursor(),end=Math.min(TOTAL,start+30);
        for(int cursor=start;cursor<end;cursor++){
            int x=MIN_X+cursor%WIDTH,z=MIN_Z+cursor/WIDTH;
            migrateColumn(level,x,z);
        }
        data.advanceVisualUpgrade(end-start);
        if(end>=TOTAL){
            FederationBuilder.finishVisualUpgrade(level);
            data.setLayoutVersion(3);
            for(var player:level.players())player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "§a[FEDERAÇÃO] §fModernização visual concluída sem reconstruir seu mundo."),false);
        }
    }

    private static void migrateColumn(ServerLevel level,int x,int z){
        for(int y=95;y<=132;y++){
            BlockPos pos=new BlockPos(x,y,z);BlockState old=level.getBlockState(pos);BlockState replacement=null;
            if(old.is(ModBlocks.CITADEL_ALLOY.get()))replacement=ModBlocks.FEDERATION_HULL.get().defaultBlockState();
            else if(old.is(ModBlocks.CITADEL_CIRCUIT.get()))replacement=ModBlocks.FEDERATION_ENERGY.get().defaultBlockState();
            else if(old.is(Blocks.POLISHED_BLACKSTONE_BRICKS))replacement=ModBlocks.FEDERATION_FLOOR.get().defaultBlockState();
            else if(old.is(Blocks.CYAN_STAINED_GLASS))replacement=ModBlocks.FEDERATION_GLASS.get().defaultBlockState();
            else if(old.is(Blocks.CHISELED_STONE_BRICKS))replacement=ModBlocks.FEDERATION_TERMINAL.get().defaultBlockState();
            if(replacement!=null)level.setBlock(pos,replacement,2);
        }
    }
}

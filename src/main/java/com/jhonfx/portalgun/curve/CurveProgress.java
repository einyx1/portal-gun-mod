package com.jhonfx.portalgun.curve;

import com.jhonfx.portalgun.init.ModItems;
import com.jhonfx.portalgun.item.PortalFluidTubeItem;
import com.jhonfx.portalgun.item.RickBrainItem;
import com.jhonfx.portalgun.network.CitadelScreenPacket;
import com.jhonfx.portalgun.network.ModNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

public final class CurveProgress {
    public static void open(ServerPlayer player,String screen) {
        CurveSavedData d=CurveSavedData.get(player.serverLevel());
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),new CitadelScreenPacket(screen,
                com.jhonfx.portalgun.citadel.CitadelEconomy.count(player),d.broken()?2:(d.breaking()||d.battleStarted())?1:0,d.neuralData(),d.fluid(),0));
    }
    public static void analyzeBrain(ServerPlayer player) {
        int slot=find(player,ModItems.RICK_BRAIN.get());
        if(slot<0){player.displayClientMessage(Component.translatable("message.portalgun.need_rick_brain"),true);return;}
        ItemStack brain=player.getInventory().getItem(slot); int amount=RickBrainItem.data(brain);
        if(!player.getAbilities().instabuild)brain.shrink(1);
        int total=CurveSavedData.get(player.serverLevel()).addNeuralData(amount);
        player.displayClientMessage(Component.translatable("message.portalgun.brain_analyzed",amount,total),false);open(player,"SCANNER");
    }
    public static void feedDrive(ServerPlayer player) {
        int slot=findTube(player); if(slot<0){player.displayClientMessage(Component.translatable("message.portalgun.drive_need_fluid"),true);return;}
        ItemStack tube=player.getInventory().getItem(slot); int amount=Math.max(5,Math.round(PortalFluidTubeItem.getCharge(tube)/1000f*20f));
        if(!player.getAbilities().instabuild)tube.shrink(1);
        int total=CurveSavedData.get(player.serverLevel()).addFluid(amount);
        player.displayClientMessage(Component.translatable("message.portalgun.drive_filled",amount,total),false);open(player,"DRIVE");
    }
    private static int find(ServerPlayer p,net.minecraft.world.item.Item item){for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(item))return i;return-1;}
    private static int findTube(ServerPlayer p){for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).getItem() instanceof PortalFluidTubeItem)return i;return-1;}
    private CurveProgress(){}
}

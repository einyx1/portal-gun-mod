package com.jhonfx.portalgun.network;

import com.jhonfx.portalgun.citadel.CitadelEconomy;
import com.jhonfx.portalgun.citadel.CitadelMissions;
import com.jhonfx.portalgun.entity.PortalColor;
import com.jhonfx.portalgun.init.ModItems;
import com.jhonfx.portalgun.item.PortalFluidTubeItem;
import com.jhonfx.portalgun.item.PortalGunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import java.util.function.Supplier;

public record CitadelActionPacket(String action) {
    public static void encode(CitadelActionPacket p, FriendlyByteBuf b) { b.writeUtf(p.action); }
    public static CitadelActionPacket decode(FriendlyByteBuf b) { return new CitadelActionPacket(b.readUtf()); }
    public static void handle(CitadelActionPacket p, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx=supplier.get(); ServerPlayer player=ctx.getSender();
        if(player!=null) ctx.enqueueWork(() -> run(player,p.action));
        ctx.setPacketHandled(true);
    }

    private static void run(ServerPlayer player,String action) {
        if(!nearServiceTerminal(player,action)){
            player.displayClientMessage(Component.translatable("message.portalgun.service_too_far"),true);
            return;
        }
        switch(action) {
            case "BUY_GREEN" -> buyTube(player,ModItems.GREEN_PORTAL_FLUID.get());
            case "BUY_BLUE" -> buyTube(player,ModItems.BLUE_PORTAL_FLUID.get());
            case "BUY_YELLOW" -> buyTube(player,ModItems.YELLOW_PORTAL_FLUID.get());
            case "BUY_SCANNER" -> buy(player,ModItems.NEURAL_SCANNER.get(),8);
            case "BUY_ISOTOPE" -> buy(player,ModItems.QUANTUM_ISOTOPE.get(),6);
            case "BUY_MEESEEKS" -> buy(player,ModItems.MEESEEKS_BOX.get(),12);
            case "BUY_SHIELD" -> buy(player,ModItems.FORCE_FIELD_EMITTER.get(),18);
            case "BUY_JETPACK" -> buy(player,ModItems.COMBAT_JETPACK.get(),22);
            case "BUY_BLASTER" -> buy(player,ModItems.ARM_BLASTER.get(),16);
            case "BUY_CANNON" -> buy(player,ModItems.PORTAL_CANNON.get(),36);
            case "RECHARGE" -> recharge(player);
            case "REPAIR_EQUIPMENT" -> repairEquipment(player);
            case "WORKSHOP_CELLS" -> {
                if(!CitadelEconomy.take(player,8)){noCredits(player,8);return;}
                give(player,new ItemStack(ModItems.WEAPON_ENERGY_CELL.get(),4));refresh(player,"WORKSHOP");
            }
            case "MISSION_ZOMBIE" -> CitadelMissions.accept(player,CitadelMissions.Mission.ZOMBIE);
            case "MISSION_SKELETON" -> CitadelMissions.accept(player,CitadelMissions.Mission.SKELETON);
            case "MISSION_SPIDER" -> CitadelMissions.accept(player,CitadelMissions.Mission.SPIDER);
            case "MISSION_ENDERMAN" -> CitadelMissions.accept(player,CitadelMissions.Mission.ENDERMAN);
            case "MISSION_REDSTONE" -> CitadelMissions.accept(player,CitadelMissions.Mission.REDSTONE);
            case "MISSION_NETHER" -> CitadelMissions.accept(player,CitadelMissions.Mission.NETHER);
            case "MISSION_DISTRICTS" -> CitadelMissions.accept(player,CitadelMissions.Mission.DISTRICTS);
            case "MISSION_FEDERATION" -> CitadelMissions.accept(player,CitadelMissions.Mission.FEDERATION_RAID);
            case "MISSION_SALVAGE" -> CitadelMissions.accept(player,CitadelMissions.Mission.FEDERATION_SALVAGE);
            case "MISSION_FLIGHT" -> CitadelMissions.accept(player,CitadelMissions.Mission.FLIGHT_TEST);
            case "MISSION_CLAIM" -> CitadelMissions.claim(player);
            case "ANALYZE_C524" -> analyze(player);
            case "ANALYZE_BRAIN" -> com.jhonfx.portalgun.curve.CurveProgress.analyzeBrain(player);
            case "ANALYZE_P0" -> analyzePrimeEvidence(player,ModItems.P0_SIGNATURE_FRAGMENT.get(),
                    "PortalGunP0Analyzed",18,20,"message.portalgun.scanner_p0_result");
            case "ANALYZE_RESIDUE" -> analyzePrimeEvidence(player,ModItems.PRIME_TELEPORT_RESIDUE.get(),
                    "PortalGunResidueAnalyzed",24,30,"message.portalgun.scanner_residue_result");
            case "FEED_DRIVE" -> com.jhonfx.portalgun.curve.CurveProgress.feedDrive(player);
            case "BREAK_CURVE" -> { com.jhonfx.portalgun.curve.CurveHandler.begin(player); com.jhonfx.portalgun.curve.CurveProgress.open(player,"CURVE"); }
        }
    }

    private static boolean nearServiceTerminal(ServerPlayer player,String action){
        com.jhonfx.portalgun.block.CitadelServiceBlock.Service expected=
                action.startsWith("BUY_")?com.jhonfx.portalgun.block.CitadelServiceBlock.Service.SHOP:
                (action.equals("RECHARGE")||action.equals("REPAIR_EQUIPMENT")||action.equals("WORKSHOP_CELLS"))?com.jhonfx.portalgun.block.CitadelServiceBlock.Service.WORKSHOP:
                action.startsWith("MISSION_")?com.jhonfx.portalgun.block.CitadelServiceBlock.Service.MISSION:
                action.equals("FEED_DRIVE")?com.jhonfx.portalgun.block.CitadelServiceBlock.Service.DRIVE:
                action.equals("BREAK_CURVE")?com.jhonfx.portalgun.block.CitadelServiceBlock.Service.CURVE:
                action.startsWith("ANALYZE_")?com.jhonfx.portalgun.block.CitadelServiceBlock.Service.SCANNER:null;
        if(expected==null)return false;
        net.minecraft.core.BlockPos center=player.blockPosition();
        for(net.minecraft.core.BlockPos pos:net.minecraft.core.BlockPos.betweenClosed(center.offset(-8,-5,-8),center.offset(8,5,8)))
            if(player.level().getBlockState(pos).getBlock() instanceof com.jhonfx.portalgun.block.CitadelServiceBlock block
                    &&block.service()==expected)return true;
        return false;
    }

    private static void buyTube(ServerPlayer player,Item item) {
        if(!CitadelEconomy.take(player,3)) { noCredits(player,3); return; }
        ItemStack stack=new ItemStack(item); PortalFluidTubeItem.setCharge(stack,PortalFluidTubeItem.MAX_CHARGE); give(player,stack);
        player.displayClientMessage(Component.translatable("message.portalgun.shop_purchase"),true); refresh(player,"SHOP");
    }
    private static void buy(ServerPlayer player,Item item,int price) {
        if(!CitadelEconomy.take(player,price)) { noCredits(player,price); return; }
        give(player,new ItemStack(item)); player.displayClientMessage(Component.translatable("message.portalgun.shop_purchase"),true); refresh(player,"SHOP");
    }
    private static void recharge(ServerPlayer player) {
        ItemStack gun=player.getMainHandItem();
        if(!(gun.getItem() instanceof PortalGunItem portalGun)) gun=player.getOffhandItem();
        if(!(gun.getItem() instanceof PortalGunItem portalGun)) { player.displayClientMessage(Component.translatable("message.portalgun.workshop_hold_gun"),true); return; }
        if(!CitadelEconomy.take(player,5)) { noCredits(player,5); return; }
        PortalGunItem.initialize(gun); gun.getTag().putInt(PortalGunItem.TAG_CHARGE,PortalFluidTubeItem.MAX_CHARGE);
        gun.getTag().putString(PortalGunItem.TAG_TUBE_COLOR,portalGun.variant().color().name());
        player.displayClientMessage(Component.translatable("message.portalgun.workshop_complete"),true); refresh(player,"WORKSHOP");
    }
    private static void repairEquipment(ServerPlayer player) {
        ItemStack stack=player.getMainHandItem();
        if(!stack.isDamaged())stack=player.getOffhandItem();
        if(!stack.isDamaged()){
            player.displayClientMessage(Component.translatable("message.portalgun.repair_hold_damaged"),true);return;
        }
        if(!CitadelEconomy.take(player,8)){noCredits(player,8);return;}
        stack.setDamageValue(0);
        player.displayClientMessage(Component.translatable("message.portalgun.workshop_complete"),true);
        refresh(player,"WORKSHOP");
    }
    private static void analyze(ServerPlayer player) {
        if(!has(player,ModItems.C524_NEURAL_FRAGMENT.get())) { player.displayClientMessage(Component.translatable("message.portalgun.scanner_need_fragment"),true); return; }
        if(player.getPersistentData().getCompound("PortalGunPrologue").getInt("Stage")<5) { player.displayClientMessage(Component.translatable("message.portalgun.calibrator_story_locked"),true); return; }
        if(player.getPersistentData().getBoolean("PortalGunC524Analyzed")) { player.displayClientMessage(Component.translatable("message.portalgun.scanner_already"),true); return; }
        player.getPersistentData().putBoolean("PortalGunC524Analyzed",true); CitadelEconomy.give(player,10);
        give(player,new ItemStack(ModItems.INTERDIMENSIONAL_CALIBRATOR.get()));
        com.jhonfx.portalgun.curve.CurveSavedData.get(player.serverLevel()).addNeuralData(34);
        player.displayClientMessage(Component.translatable("message.portalgun.scanner_result"),false); com.jhonfx.portalgun.curve.CurveProgress.open(player,"SCANNER");
    }
    private static void analyzePrimeEvidence(ServerPlayer player,Item evidence,String flag,int credits,int data,String message) {
        if(!has(player,evidence)) { player.displayClientMessage(Component.translatable("message.portalgun.scanner_need_evidence"),true); return; }
        if(player.getPersistentData().getBoolean(flag)) { player.displayClientMessage(Component.translatable("message.portalgun.scanner_already"),true); return; }
        player.getPersistentData().putBoolean(flag,true);
        CitadelEconomy.give(player,credits);
        com.jhonfx.portalgun.curve.CurveSavedData.get(player.serverLevel()).addNeuralData(data);
        if(player.getPersistentData().getBoolean("PortalGunP0Analyzed")
                && player.getPersistentData().getBoolean("PortalGunResidueAnalyzed")
                && !player.getPersistentData().getBoolean("PortalGunPrimeForensicsComplete")) {
            player.getPersistentData().putBoolean("PortalGunPrimeForensicsComplete",true);
            give(player,new ItemStack(ModItems.MULTIVERSAL_SCANNER_UPGRADE_CHIP.get()));
        }
        player.displayClientMessage(Component.translatable(message,credits,data),false);
        com.jhonfx.portalgun.curve.CurveProgress.open(player,"SCANNER");
    }
    private static boolean has(ServerPlayer p,Item item) { for(ItemStack s:p.getInventory().items) if(s.is(item)) return true; return false; }
    private static void give(ServerPlayer p,ItemStack s) { if(!p.getInventory().add(s)) p.drop(s,false); }
    private static void noCredits(ServerPlayer p,int n) { p.displayClientMessage(Component.translatable("message.portalgun.not_enough_credits",n),true); }
    private static void refresh(ServerPlayer p,String screen) { ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),new CitadelScreenPacket(screen,CitadelEconomy.count(p),0,0,0,0)); }
}

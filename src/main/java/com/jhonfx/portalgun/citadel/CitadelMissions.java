package com.jhonfx.portalgun.citadel;

import com.jhonfx.portalgun.network.CitadelScreenPacket;
import com.jhonfx.portalgun.network.ModNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

public final class CitadelMissions {
    private static final String ROOT = "PortalGunCitadelMission";

    public enum Mission {
        NONE("", 0, 0), ZOMBIE("mission.portalgun.zombie", 10, 7),
        SKELETON("mission.portalgun.skeleton", 8, 7), SPIDER("mission.portalgun.spider", 8, 6),
        ENDERMAN("mission.portalgun.enderman",4,10), REDSTONE("mission.portalgun.redstone",24,9),
        NETHER("mission.portalgun.nether",1,8), DISTRICTS("mission.portalgun.districts",3,12),
        FEDERATION_RAID("mission.portalgun.federation_raid",6,14),
        FEDERATION_SALVAGE("mission.portalgun.federation_salvage",8,16),
        FLIGHT_TEST("mission.portalgun.flight_test",600,18);
        public final String key; public final int target; public final int reward;
        Mission(String key, int target, int reward) { this.key=key; this.target=target; this.reward=reward; }
    }

    public static CompoundTag data(Player player) { return player.getPersistentData().getCompound(ROOT); }
    private static void save(Player player, CompoundTag tag) { player.getPersistentData().put(ROOT, tag); }
    public static Mission active(Player player) {
        int value=data(player).getInt("Type");
        return value<0||value>=Mission.values().length?Mission.NONE:Mission.values()[value];
    }
    public static int progress(Player player) { return data(player).getInt("Progress"); }

    public static void accept(ServerPlayer player, Mission mission) {
        if (mission==Mission.NONE || active(player)!=Mission.NONE) return;
        CompoundTag tag=new CompoundTag(); tag.putInt("Type",mission.ordinal()); tag.putInt("Progress",0); save(player,tag);
        player.displayClientMessage(Component.translatable("message.portalgun.mission_accepted",Component.translatable(mission.key),mission.target),false);
        open(player);
    }

    public static void claim(ServerPlayer player) {
        refreshProgress(player);
        Mission mission=active(player);
        if(mission==Mission.NONE || progress(player)<mission.target) return;
        if(mission==Mission.REDSTONE)consume(player,net.minecraft.world.item.Items.REDSTONE,mission.target);
        if(mission==Mission.FEDERATION_SALVAGE)consume(player,com.jhonfx.portalgun.init.ModItems.FEDERATION_ENERGY.get(),mission.target);
        CitadelEconomy.give(player,mission.reward);
        save(player,new CompoundTag());
        player.displayClientMessage(Component.translatable("message.portalgun.mission_claimed",mission.reward),false);
        open(player);
    }

    public static void open(ServerPlayer player) {
        refreshProgress(player);
        Mission mission=active(player);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new CitadelScreenPacket("MISSION",CitadelEconomy.count(player),mission.ordinal(),progress(player),mission.target,mission.reward));
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        Entity source=event.getSource().getEntity();
        if(!(source instanceof ServerPlayer player)) return;
        Mission mission=active(player); if(mission==Mission.NONE) return;
        LivingEntity victim=event.getEntity();
        boolean matches=switch(mission) {
            case ZOMBIE -> victim.getType().toString().contains("zombie");
            case SKELETON -> victim.getType().toString().contains("skeleton");
            case SPIDER -> victim.getType().toString().contains("spider");
            case ENDERMAN -> victim.getType().toString().contains("enderman");
            case FEDERATION_RAID -> victim instanceof com.jhonfx.portalgun.entity.FederationAlienEntity
                    || victim instanceof com.jhonfx.portalgun.entity.FederationHeavyTrooperEntity
                    || victim instanceof com.jhonfx.portalgun.entity.FederationDroneEntity;
            default -> false;
        };
        if(!matches) return;
        CompoundTag tag=data(player); int value=Math.min(mission.target,tag.getInt("Progress")+1); tag.putInt("Progress",value); save(player,tag);
        player.displayClientMessage(Component.translatable("message.portalgun.mission_progress",value,mission.target),true);
    }

    @SubscribeEvent public void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer player)||player.tickCount%40!=0)return;
        refreshProgress(player);
    }

    private static void refreshProgress(ServerPlayer player){
        Mission mission=active(player);int value=progress(player);
        if(mission==Mission.REDSTONE){value=0;for(var stack:player.getInventory().items)if(stack.is(net.minecraft.world.item.Items.REDSTONE))value+=stack.getCount();}
        else if(mission==Mission.FEDERATION_SALVAGE){value=0;for(var stack:player.getInventory().items)if(stack.is(com.jhonfx.portalgun.init.ModItems.FEDERATION_ENERGY.get()))value+=stack.getCount();}
        else if(mission==Mission.FLIGHT_TEST&&player.getVehicle() instanceof com.jhonfx.portalgun.entity.RickShipEntity){
            CompoundTag flight=data(player);double x=player.getX(),y=player.getY(),z=player.getZ();
            if(flight.contains("FlightX")){double dx=x-flight.getDouble("FlightX"),dy=y-flight.getDouble("FlightY"),dz=z-flight.getDouble("FlightZ");value+=Math.min(20,(int)Math.round(Math.sqrt(dx*dx+dy*dy+dz*dz)));}
            flight.putDouble("FlightX",x);flight.putDouble("FlightY",y);flight.putDouble("FlightZ",z);save(player,flight);
        }
        else if(mission==Mission.NETHER&&player.level().dimension()==net.minecraft.world.level.Level.NETHER)value=1;
        else if(mission==Mission.DISTRICTS&&player.level().dimension().location().equals(new net.minecraft.resources.ResourceLocation("portalgun","citadel"))){
            int mask=data(player).getInt("DistrictMask");net.minecraft.core.BlockPos p=player.blockPosition();
            net.minecraft.core.BlockPos[] districts={new net.minecraft.core.BlockPos(-82,97,12),new net.minecraft.core.BlockPos(86,97,8),new net.minecraft.core.BlockPos(-18,97,-82)};
            for(int i=0;i<districts.length;i++)if(p.distSqr(districts[i])<20*20)mask|=1<<i;
            CompoundTag visit=data(player);visit.putInt("DistrictMask",mask);save(player,visit);value=Integer.bitCount(mask);
        }
        value=Math.min(mission.target,value);CompoundTag tag=data(player);if(tag.getInt("Progress")!=value){tag.putInt("Progress",value);save(player,tag);}
    }

    private static void consume(ServerPlayer player,net.minecraft.world.item.Item item,int amount){for(var stack:player.getInventory().items){if(!stack.is(item))continue;int n=Math.min(amount,stack.getCount());stack.shrink(n);amount-=n;if(amount<=0)return;}}
}

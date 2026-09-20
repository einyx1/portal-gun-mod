package com.jhonfx.portalgun.citadel;

import com.jhonfx.portalgun.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Resumable canonical saucer layout: central dome, hanging needle and three satellite disks. */
public final class CitadelV4Layout extends SavedData {
    private static final ResourceLocation ID=new ResourceLocation("portalgun","citadel");
    private static final int MIN=-112,SIZE=225,TOTAL=SIZE*SIZE;
    private boolean requested,cleared,complete;
    private int progress;

    public static CitadelV4Layout get(ServerLevel level){return level.getDataStorage().computeIfAbsent(CitadelV4Layout::load,CitadelV4Layout::new,"portalgun_citadel_v4");}
    public static CitadelV4Layout load(CompoundTag tag){CitadelV4Layout s=new CitadelV4Layout();s.requested=tag.getBoolean("Requested");s.cleared=tag.getBoolean("Cleared");s.complete=tag.getBoolean("Complete");s.progress=tag.getInt("Progress");return s;}
    @Override public CompoundTag save(CompoundTag tag){tag.putBoolean("Requested",requested);tag.putBoolean("Cleared",cleared);tag.putBoolean("Complete",complete);tag.putInt("Progress",progress);return tag;}

    public static void request(ServerLevel level){CitadelV4Layout s=get(level);
        if(com.jhonfx.portalgun.curve.CurveSavedData.get(level).broken())return;
        // Layout v8 is the final walkable city pass and removes the remaining bottlenecks.
        if(s.complete&&CitadelSavedData.get(level).getLayoutVersion()>=8)return;
        if(s.complete){s.complete=false;s.cleared=false;s.progress=0;s.requested=true;CitadelSavedData.get(level).prepareV4Rebuild();}
        else if(!s.requested)CitadelSavedData.get(level).prepareV4Rebuild();
        s.requested=true;s.setDirty();landing(level);}

    @SubscribeEvent public void tick(TickEvent.LevelTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.level instanceof ServerLevel level)||!level.dimension().location().equals(ID))return;
        CitadelV4Layout s=get(level);if(!s.requested||s.complete)return;
        if(com.jhonfx.portalgun.curve.CurveSavedData.get(level).broken()){
            s.requested=false;s.setDirty();return;
        }
        // A reconstrução antiga alterava milhares de blocos em um único tick,
        // causando os congelamentos fortes relatados. O trabalho continua
        // persistente, mas agora é distribuído em lotes pequenos.
        int end=Math.min(TOTAL,s.progress+(s.cleared?18:24));
        long deadline=System.nanoTime()+4_000_000L;
        do {int x=MIN+s.progress%SIZE,z=MIN+s.progress/SIZE;if(s.cleared)buildColumn(level,x,z);else clearColumn(level,x,z);s.progress++;}
        while(s.progress<end&&System.nanoTime()<deadline);
        landing(level);s.setDirty();
        if(s.progress<TOTAL)return;
        if(!s.cleared){
            s.cleared=true;s.progress=0;s.setDirty();
            level.getEntitiesOfClass(com.jhonfx.portalgun.entity.CitadelNpcEntity.class,new net.minecraft.world.phys.AABB(CitadelBuilder.CENTER).inflate(130)).forEach(net.minecraft.world.entity.Entity::discard);
            level.getEntitiesOfClass(com.jhonfx.portalgun.entity.CurveNodeEntity.class,new net.minecraft.world.phys.AABB(CitadelBuilder.CENTER).inflate(130)).forEach(net.minecraft.world.entity.Entity::discard);
            level.players().forEach(p->p.displayClientMessage(Component.literal("§6[CIDADELA] §fEstrutura antiga removida. Montando o núcleo orbital v4..."),false));
        }else{
            installLandmarks(level);s.complete=true;s.setDirty();CitadelBuilder.finishV4(level);
            level.players().forEach(p->p.displayClientMessage(Component.literal("§a[CIDADELA] §fReconstrução orbital concluída."),false));
        }
    }

    private static void clearColumn(ServerLevel l,int x,int z){for(int y=72;y<=145;y++){BlockPos p=CitadelBuilder.CENTER.offset(x,y-CitadelBuilder.CENTER.getY(),z);if(!l.getBlockState(p).isAir())l.setBlock(p,Blocks.AIR.defaultBlockState(),2);}}

    private static void buildColumn(ServerLevel l,int x,int z){
        BlockState panel=ModBlocks.CITADEL_PANEL.get().defaultBlockState(),alloy=ModBlocks.CITADEL_ALLOY.get().defaultBlockState(),gold=ModBlocks.CITADEL_GOLD.get().defaultBlockState(),glass=ModBlocks.CITADEL_GLASS.get().defaultBlockState(),circuit=ModBlocks.CITADEL_CIRCUIT.get().defaultBlockState();
        int central=x*x+z*z,west=(x+82)*(x+82)+(z-12)*(z-12),east=(x-86)*(x-86)+(z-8)*(z-8),north=(x+18)*(x+18)+(z+82)*(z+82);
        boolean c=central<=47*47,w=west<=27*27,e=east<=29*29,n=north<=25*25;
        boolean bw=Math.abs(z-(int)Math.round(12*x/-82d))<=4&&x>=-82&&x<=0;
        boolean be=Math.abs(z-(int)Math.round(8*x/86d))<=4&&x>=0&&x<=86;
        boolean bn=Math.abs(x-(int)Math.round(-18*z/-82d))<=4&&z>=-82&&z<=0;
        if(c||w||e||n||bw||be||bn){
            int d=c?central:w?west:e?east:n?north:0,r=c?47:w?27:e?29:n?25:0;
            BlockState floor=d>0&&d>(r-2)*(r-2)?gold:(Math.floorMod(x*3+z*5,29)==0?circuit:panel);
            set(l,new BlockPos(x,96,z),floor);set(l,new BlockPos(x,95,z),alloy);
            if((c||w||e||n)&&d>(r-1)*(r-1))for(int y=97;y<=100;y++)set(l,new BlockPos(x,y,z),y==99?glass:alloy);
            if(bw||be||bn){
                boolean tubeEdge=(bw&&Math.abs(z-(int)Math.round(12*x/-82d))>=4)
                        ||(be&&Math.abs(z-(int)Math.round(8*x/86d))>=4)
                        ||(bn&&Math.abs(x-(int)Math.round(-18*z/-82d))>=4);
                if(tubeEdge)for(int y=97;y<=101;y++)set(l,new BlockPos(x,y,z),y==99?glass:alloy);
                set(l,new BlockPos(x,102,z),glass);
            }
        }
        for(int y=74;y<96;y++){int radius=Math.max(3,(y-72)/2),inner=Math.max(0,radius-2);if(central<=radius*radius&&central>inner*inner)set(l,new BlockPos(x,y,z),y%4==0?circuit:alloy);}
        tower(l,x,z,0,0,9,44,panel,gold,glass,circuit);tower(l,x,z,-82,12,5,22,alloy,gold,glass,circuit);tower(l,x,z,86,8,6,27,panel,gold,glass,circuit);tower(l,x,z,-18,-82,6,30,alloy,gold,glass,circuit);
        if(central<=45*45){double dist=Math.sqrt(central);int roof=101+(int)Math.round(15*Math.sqrt(Math.max(0,1-dist*dist/(45d*45d))));set(l,new BlockPos(x,roof,z),Math.floorMod(x+z,11)==0?circuit:glass);}
        satelliteDome(l,x,z,-82,12,25,12,glass,gold,circuit);
        satelliteDome(l,x,z,86,8,27,13,glass,gold,circuit);
        satelliteDome(l,x,z,-18,-82,23,14,glass,gold,circuit);
    }

    private static void satelliteDome(ServerLevel l,int x,int z,int cx,int cz,int radius,int height,BlockState glass,BlockState trim,BlockState light){
        int dx=x-cx,dz=z-cz,d=dx*dx+dz*dz;if(d>radius*radius)return;
        double distance=Math.sqrt(d);int roof=101+(int)Math.round(height*Math.sqrt(Math.max(0,1-distance*distance/(radius*(double)radius))));
        set(l,new BlockPos(x,roof,z),Math.floorMod(x*3+z*5,17)==0?light:(d>(radius-2)*(radius-2)?trim:glass));
    }

    private static void tower(ServerLevel l,int x,int z,int cx,int cz,int radius,int height,BlockState wall,BlockState trim,BlockState glass,BlockState light){int dx=x-cx,dz=z-cz,d=dx*dx+dz*dz;if(d>radius*radius)return;for(int y=97;y<=96+height;y++){boolean shell=d>(radius-2)*(radius-2)||y==96+height||y%9==0;if(shell)set(l,new BlockPos(x,y,z),y%7==0?light:(y%4==0?glass:wall));}}
    private static void installLandmarks(ServerLevel l){set(l,CitadelBuilder.CENTER.offset(12,1,4),ModBlocks.CITADEL_SHOP.get().defaultBlockState());set(l,CitadelBuilder.CENTER.offset(-12,1,4),ModBlocks.CITADEL_WORKSHOP.get().defaultBlockState());set(l,CitadelBuilder.CENTER.offset(0,1,12),ModBlocks.CITADEL_MISSION_TERMINAL.get().defaultBlockState());set(l,CitadelBuilder.CENTER.offset(-8,1,-12),ModBlocks.NEURAL_SCANNER.get().defaultBlockState());set(l,CitadelBuilder.CENTER.offset(8,1,-12),ModBlocks.COUNCIL_ARCHIVE.get().defaultBlockState());}
    private static void landing(ServerLevel l){for(int x=-4;x<=4;x++)for(int z=6;z<=14;z++)set(l,new BlockPos(x,96,z),x==0||z==10?Blocks.SEA_LANTERN.defaultBlockState():ModBlocks.CITADEL_ALLOY.get().defaultBlockState());}
    private static void set(ServerLevel l,BlockPos p,BlockState s){l.setBlock(p,s,2);}
}

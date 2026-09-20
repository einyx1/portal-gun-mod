package com.jhonfx.portalgun.federation;

import com.jhonfx.portalgun.entity.FederationAlienEntity;
import com.jhonfx.portalgun.init.ModBlocks;
import com.jhonfx.portalgun.init.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * NEW GALACTIC FEDERATION — Sector Gromflom Prime
 *
 * Layout (from CENTER, Y=96):
 *   Public Plaza          CENTER radius 28
 *   Galactic Customs      CENTER.offset(0,0,-45)  — checkpoint to enter
 *   Command Spire         CENTER.offset(0,0,-75)  — central government
 *   Prison Block          CENTER.offset(55,0,0)   — detention facility
 *   Armory / Barracks     CENTER.offset(-55,0,0)
 *   Docking Bay           CENTER.offset(0,0,60)   — arrival/departure
 */
public final class FederationBuilder {
    public static final BlockPos CENTER      = new BlockPos(0,  96, 0);
    public static final BlockPos CUSTOMS_POS = new BlockPos(0,  97, -45);
    public static final BlockPos PRISON_POS  = new BlockPos(55, 97, 0);
    public static final BlockPos ARRIVAL     = new BlockPos(0,  97, 55);   // docking bay arrival

    // NBT key stored in level's SavedData to track build stages
    private static final String KEY_BUILT    = "FedBuilt";
    private static final String KEY_EXPANDED = "FedExpanded";
    private static final int BLOCKS_PER_TICK = 1800;
    private static final java.util.Map<ServerLevel, BuildJob> BUILD_JOBS = new java.util.WeakHashMap<>();
    private static final ThreadLocal<java.util.List<Placement>> CAPTURE = new ThreadLocal<>();

    public static void ensureBuilt(ServerLevel level) {
        FederationSavedData data = FederationSavedData.get(level);
        if(data.isDestroyed())return;
        if(!data.isBuilt()||!data.isExpanded()){
            prepareSafetyPads(level);
            if(!BUILD_JOBS.containsKey(level))beginIncrementalBuild(level,data);
            return;
        }
        if(data.layoutVersion()<3)data.beginVisualUpgrade();
        repairCriticalLayout(level);
        ensureStoryLandmarks(level);
        ensureNpcs(level);
        installArmory(level);
    }

    private static void beginIncrementalBuild(ServerLevel level,FederationSavedData data){
        java.util.List<Placement> placements=new java.util.ArrayList<>();
        CAPTURE.set(placements);
        try{
            if(!data.isBuilt())buildBase(level);
            if(!data.isExpanded())buildExpansion(level);
        }finally{CAPTURE.remove();}
        BUILD_JOBS.put(level,new BuildJob(placements,0,!data.isBuilt(),!data.isExpanded()));
        for(var player:level.players())player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "message.portalgun.federation_building",placements.size()),true);
    }

    static void tickBuild(ServerLevel level){
        if(FederationSavedData.get(level).isDestroyed()||FederationSavedData.get(level).collapseTicks()>0){BUILD_JOBS.remove(level);return;}
        BuildJob job=BUILD_JOBS.get(level);if(job==null)return;
        int end=Math.min(job.placements.size(),job.cursor+BLOCKS_PER_TICK);
        long deadline=System.nanoTime()+4_000_000L;
        for(int i=job.cursor;i<end;i++){
            Placement placement=job.placements.get(i);
            if(!level.getBlockState(placement.pos).equals(placement.state))level.setBlock(placement.pos,placement.state,2);
            if(System.nanoTime()>=deadline){end=i+1;break;}
        }
        if(end<job.placements.size()){
            BUILD_JOBS.put(level,new BuildJob(job.placements,end,job.markBuilt,job.markExpanded));
            return;
        }
        BUILD_JOBS.remove(level);
        FederationSavedData data=FederationSavedData.get(level);
        if(job.markBuilt)data.markBuilt();
        if(job.markExpanded)data.markExpanded();
        data.setLayoutVersion(3);
        repairCriticalLayout(level);ensureStoryLandmarks(level);ensureNpcs(level);
        installArmory(level);
        for(var player:level.players())player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "message.portalgun.federation_build_complete"),false);
    }

    static boolean isBuilding(ServerLevel level){return BUILD_JOBS.containsKey(level);}
    public static int buildPercent(ServerLevel level){
        BuildJob job=BUILD_JOBS.get(level);
        return job==null?100:Math.min(100,Math.round(job.cursor*100f/Math.max(1,job.placements.size())));
    }

    private static void prepareSafetyPads(ServerLevel level){
        BlockState floor=ModBlocks.FEDERATION_FLOOR.get().defaultBlockState();
        for(BlockPos center:new BlockPos[]{CENTER.above(),ARRIVAL})for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++){
            BlockPos pos=center.offset(x,-1,z);
            level.setBlock(pos.below(),Blocks.BEDROCK.defaultBlockState(),2);
            level.setBlock(pos,floor,2);
            for(int y=1;y<=3;y++)level.setBlock(pos.above(y),Blocks.AIR.defaultBlockState(),2);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  BASE LAYER — ground, plaza, spire, docks
    // ──────────────────────────────────────────────────────────────────────────
    private static void buildBase(ServerLevel level) {
        BlockState floor  = ModBlocks.FEDERATION_FLOOR.get().defaultBlockState();
        BlockState metal  = ModBlocks.FEDERATION_HULL.get().defaultBlockState();
        BlockState energy = ModBlocks.FEDERATION_ENERGY.get().defaultBlockState();
        BlockState red    = ModBlocks.FEDERATION_TERMINAL.get().defaultBlockState();
        BlockState glass  = ModBlocks.FEDERATION_GLASS.get().defaultBlockState();

        // Ground plate — main plaza, radius 32
        for (int x = -32; x <= 32; x++) for (int z = -32; z <= 32; z++) {
            int d2 = x*x + z*z;
            if (d2 > 32*32) continue;
            BlockPos p = CENTER.offset(x, 0, z);
            BlockState surf = d2 > 29*29 ? red : ((x + z) % 11 == 0 ? energy : floor);
            set(level, p, surf);
            set(level, p.below(), Blocks.BEDROCK.defaultBlockState());
        }

        // ── Central Command Spire (north of plaza) ──
        buildSpire(level, CENTER.offset(0, 0, -75), 28, floor, metal, energy, glass, red);

        // ── Docking Bay (south) ──
        buildDock(level, CENTER.offset(0, 0, 60), floor, metal, energy, red);

        // ── Prison Block (east) ──
        buildPrison(level, PRISON_POS, floor, metal, energy, red, glass);

        // ── Armory / Barracks (west) ──
        buildArmory(level, CENTER.offset(-55, 0, 0), floor, metal, energy, red);

        // ── Galactic Customs Checkpoint ──
        buildCustoms(level, CUSTOMS_POS, floor, metal, energy, red, glass);

        // Connecting avenues
        avenue(level, CENTER, Direction.NORTH, 45, 6, floor, energy);
        avenue(level, CENTER, Direction.SOUTH, 60, 6, floor, energy);
        avenue(level, CENTER, Direction.EAST,  50, 6, floor, energy);
        avenue(level, CENTER, Direction.WEST,  50, 6, floor, energy);

        // Bedrock base under avenues
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (int step = 1; step <= 65; step++) {
                BlockPos ap = CENTER.relative(d, step);
                for (int w = -3; w <= 3; w++) {
                    set(level, ap.relative(d.getClockWise(), w).below(), Blocks.BEDROCK.defaultBlockState());
                }
            }
        }

        // Portal inhibitor bedrock marker (prison east wing)
        set(level, PRISON_POS.offset(0, -1, 0), Blocks.BEDROCK.defaultBlockState());
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  COMMAND SPIRE
    // ──────────────────────────────────────────────────────────────────────────
    private static void buildSpire(ServerLevel level, BlockPos base,
                                   int height, BlockState floor, BlockState metal,
                                   BlockState energy, BlockState glass, BlockState red) {
        // Disk base
        for (int x = -14; x <= 14; x++) for (int z = -14; z <= 14; z++) {
            if (x*x + z*z > 14*14) continue;
            set(level, base.offset(x, 0, z), (x*x+z*z)%17==0 ? energy : floor);
            set(level, base.offset(x, -1, z), Blocks.BEDROCK.defaultBlockState());
        }
        // Tower walls
        for (int y = 1; y <= height; y++) {
            int r = Math.max(3, 10 - y / 4);
            for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
                boolean wall = Math.abs(x) == r || Math.abs(z) == r;
                if (!wall) continue;
                BlockState s = (y % 6 == 0) ? energy : ((y % 3 == 0) ? glass : metal);
                set(level, base.offset(x, y, z), s);
            }
        }
        // Floors at y=8,16,24
        for (int deck : new int[]{8, 16, 24}) {
            for (int x = -7; x <= 7; x++) for (int z = -7; z <= 7; z++) {
                if (x*x + z*z > 50) continue;
                set(level, base.offset(x, deck, z), (x+z)%5==0 ? energy : metal);
            }
            // ladder
            set(level, base.offset(0, deck+1, 6), Blocks.LADDER.defaultBlockState()
                    .setValue(LadderBlock.FACING, Direction.NORTH));
        }
        // Ladder up full height
        for (int y = 1; y <= height; y++) {
            set(level, base.offset(0, y, 6), Blocks.LADDER.defaultBlockState()
                    .setValue(LadderBlock.FACING, Direction.NORTH));
            if (y % 4 == 0) set(level, base.offset(0, y, 4),
                    Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15));
        }
        // Top beacon
        set(level, base.offset(0, height+1, 0), Blocks.BEACON.defaultBlockState());
        set(level, base.offset(0, height+2, 0), red);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  GALACTIC CUSTOMS / CHECKPOINT
    // ──────────────────────────────────────────────────────────────────────────
    private static void buildCustoms(ServerLevel level, BlockPos center,
                                     BlockState floor, BlockState metal,
                                     BlockState energy, BlockState red, BlockState glass) {
        // Main hall: 30×12, height 6
        int hw = 15, hd = 6, ht = 6;
        for (int x = -hw; x <= hw; x++) for (int z = -hd; z <= hd; z++) for (int y = 0; y <= ht; y++) {
            boolean shell = y==0||y==ht||Math.abs(x)==hw||Math.abs(z)==hd;
            BlockPos p = center.offset(x, y, z);
            if (shell) set(level, p, (y==ht && x%4==0) ? glass : ((y==0 && (x+z)%7==0) ? energy : metal));
            else set(level, p, Blocks.AIR.defaultBlockState());
        }
        // Floor
        for (int x = -hw+1; x <= hw-1; x++) for (int z = -hd+1; z <= hd-1; z++) {
            set(level, center.offset(x, 0, z), (x+z)%9==0 ? energy : floor);
        }
        // 3 checkpoint lanes with barriers
        for (int lane : new int[]{-8, 0, 8}) {
            // barrier posts
            for (int z2 = -2; z2 <= 2; z2++) {
                set(level, center.offset(lane, 1, z2), Blocks.RED_STAINED_GLASS_PANE.defaultBlockState());
                set(level, center.offset(lane, 2, z2), Blocks.RED_STAINED_GLASS_PANE.defaultBlockState());
            }
            // scanner block (reuse energy)
            set(level, center.offset(lane, 1, 0), energy);
        }
        // Signs / lights
        for (int x = -hw+2; x <= hw-2; x += 6) {
            set(level, center.offset(x, ht-1, -hd+1), Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 12));
            set(level, center.offset(x, ht-1, hd-1), Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 12));
        }
        // Entry/exit doors (air gaps)
        for (int dy = 1; dy <= 3; dy++) {
            set(level, center.offset(0, dy, -hd), Blocks.AIR.defaultBlockState());
            set(level, center.offset(0, dy, hd), Blocks.AIR.defaultBlockState());
        }
        // Warning strips at entry
        for (int x = -3; x <= 3; x++) {
            set(level, center.offset(x, 0, -hd+1), red);
            set(level, center.offset(x, 0, hd-1), red);
        }
        // Bedrock base
        for (int x = -hw; x <= hw; x++) for (int z = -hd; z <= hd; z++) {
            set(level, center.offset(x, -1, z), Blocks.BEDROCK.defaultBlockState());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  PRISON BLOCK
    // ──────────────────────────────────────────────────────────────────────────
    private static void buildPrison(ServerLevel level, BlockPos center,
                                    BlockState floor, BlockState metal,
                                    BlockState energy, BlockState red, BlockState glass) {
        int pw = 22, pd = 30, ph = 7;
        // Outer shell
        for (int x = -pw; x <= pw; x++) for (int z = -pd; z <= pd; z++) for (int y = 0; y <= ph; y++) {
            boolean shell = y==0||y==ph||Math.abs(x)==pw||Math.abs(z)==pd;
            BlockPos p = center.offset(x, y, z);
            if (shell) set(level, p, (y%2==0&&Math.abs(x)==pw) ? glass : ((y==0||(x+z)%13==0) ? energy : metal));
            else set(level, p, Blocks.AIR.defaultBlockState());
        }
        // Floor
        for (int x = -pw+1; x <= pw-1; x++) for (int z = -pd+1; z <= pd-1; z++) {
            set(level, center.offset(x, 0, z), (x*z)%7==0 ? energy : floor);
        }
        // Cell rows — two rows of 4 cells each side
        int[] cellZ = {-20, -10, 0, 10};
        for (int side : new int[]{-1, 1}) {
            int cx = side * (pw - 6);
            for (int cz : cellZ) {
                buildCell(level, center.offset(cx, 0, cz), side < 0 ? Direction.EAST : Direction.WEST,
                          metal, Blocks.IRON_BARS.defaultBlockState(), floor);
            }
        }
        // Central corridor lights
        for (int z = -pd+2; z <= pd-2; z += 4) {
            set(level, center.offset(0, ph-1, z), Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 14));
        }
        // Interrogation room (north end)
        for (int x = -5; x <= 5; x++) for (int z = -pd+1; z <= -pd+8; z++) {
            boolean wall = Math.abs(x)==5||z==-pd+8;
            set(level, center.offset(x, 0, z), wall ? metal : floor);
            for (int y = 1; y <= 4; y++) {
                set(level, center.offset(x, y, z), wall ? metal : Blocks.AIR.defaultBlockState());
            }
        }
        set(level, center.offset(0, 1, -pd+2), energy); // brainalyzer position
        // Door opening
        for (int dy = 1; dy <= 3; dy++) {
            set(level, center.offset(0, dy, -pd), Blocks.AIR.defaultBlockState());
            set(level, center.offset(0, dy, pd), Blocks.AIR.defaultBlockState());
        }
        // Warning strips
        for (int x = -pw; x <= pw; x++) {
            set(level, center.offset(x, -1, 0), Blocks.BEDROCK.defaultBlockState());
        }
        // Structural core below the prison. The actual inhibitor is the visible,
        // breakable console above the floor and is refreshed on every capture.
        set(level, center.below(), Blocks.BEDROCK.defaultBlockState());
        set(level, center.below(2), Blocks.BEDROCK.defaultBlockState());
        set(level, center.above(), Blocks.REDSTONE_LAMP.defaultBlockState()
                .setValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT, true));
        // Alert lighting
        for (int z = -pd+1; z <= pd-1; z += 8) {
            set(level, center.offset(-pw+1, 3, z), red);
            set(level, center.offset(pw-1, 3, z), red);
        }
    }

    private static void buildCell(ServerLevel level, BlockPos corner, Direction open,
                                  BlockState wall, BlockState bars, BlockState floor) {
        int w = 4, d = 5;
        for (int x = 0; x < w; x++) for (int z = 0; z < d; z++) for (int y = 0; y <= 3; y++) {
            boolean isWall = x==0||x==w-1||z==0||z==d-1||y==3;
            BlockPos p = corner.offset(x, y, z);
            if (y==0) set(level, p, floor);
            else if (isWall) {
                // front face = bars
                boolean front = (open==Direction.EAST&&x==0)||(open==Direction.WEST&&x==w-1);
                set(level, p, (front&&y<3) ? bars : wall);
            } else set(level, p, Blocks.AIR.defaultBlockState());
        }
        // Bed
        set(level, corner.offset(1, 1, 1), Blocks.RED_BED.defaultBlockState());
        // Light
        set(level, corner.offset(1, 3, 2), Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 8));
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  ARMORY / BARRACKS
    // ──────────────────────────────────────────────────────────────────────────
    private static void buildArmory(ServerLevel level, BlockPos center,
                                    BlockState floor, BlockState metal,
                                    BlockState energy, BlockState red) {
        int aw = 18, ad = 14, ah = 7;
        for (int x = -aw; x <= aw; x++) for (int z = -ad; z <= ad; z++) for (int y = 0; y <= ah; y++) {
            boolean shell = y==0||y==ah||Math.abs(x)==aw||Math.abs(z)==ad;
            BlockPos p = center.offset(x, y, z);
            if (shell) set(level, p, (y==0||(x+z)%11==0) ? energy : metal);
            else set(level, p, Blocks.AIR.defaultBlockState());
        }
        // Floor
        for (int x = -aw+1; x <= aw-1; x++) for (int z = -ad+1; z <= ad-1; z++) {
            set(level, center.offset(x, 0, z), x%8==0 ? energy : floor);
        }
        // Weapon lockers. Loot is installed once after incremental placement completes.
        for (int x = -aw+2; x <= aw-2; x += 6) {
            set(level, center.offset(x, 1, -ad+2), Blocks.BARREL.defaultBlockState());
            set(level, center.offset(x, 1, ad-2), Blocks.BARREL.defaultBlockState());
        }
        // Lights
        for (int x = -aw+3; x <= aw-3; x += 5) {
            set(level, center.offset(x, ah-1, 0), Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 13));
        }
        // Entry
        for (int dy = 1; dy <= 3; dy++) {
            set(level, center.offset(aw, dy, 0), Blocks.AIR.defaultBlockState());
        }
        // Bedrock base
        for (int x = -aw; x <= aw; x++) for (int z = -ad; z <= ad; z++) {
            set(level, center.offset(x, -1, z), Blocks.BEDROCK.defaultBlockState());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  DOCKING BAY
    // ──────────────────────────────────────────────────────────────────────────
    private static void buildDock(ServerLevel level, BlockPos center,
                                  BlockState floor, BlockState metal,
                                  BlockState energy, BlockState red) {
        // Large open pad
        for (int x = -20; x <= 20; x++) for (int z = -12; z <= 12; z++) {
            int d2 = x*x+z*z;
            if (d2 > 22*22) continue;
            set(level, center.offset(x, 0, z), d2>18*18 ? red : ((x+z)%9==0 ? energy : floor));
            set(level, center.offset(x, -1, z), Blocks.BEDROCK.defaultBlockState());
        }
        // 4 landing struts
        for (int sx : new int[]{-12, 12}) for (int sz : new int[]{-7, 7}) {
            for (int y = 1; y <= 8; y++) set(level, center.offset(sx, y, sz), metal);
            set(level, center.offset(sx, 9, sz), energy);
        }
        // Control tower
        for (int y = 1; y <= 12; y++) {
            int r = y < 8 ? 2 : 1;
            for (int x = -r; x <= r; x++) for (int z2 = -r; z2 <= r; z2++) {
                if (Math.abs(x)==r||Math.abs(z2)==r) set(level, center.offset(x+15, y, z2), metal);
            }
        }
        // Arrival lights
        for (int x = -15; x <= 15; x += 5) {
            set(level, center.offset(x, 1, -11), Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 12));
        }
        // Bedrock
        for (int x = -20; x <= 20; x++) for (int z = -12; z <= 12; z++) {
            set(level, center.offset(x, -1, z), Blocks.BEDROCK.defaultBlockState());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  EXPANSION (called after base)
    // ──────────────────────────────────────────────────────────────────────────
    private static void buildExpansion(ServerLevel level) {
        BlockState floor  = ModBlocks.FEDERATION_FLOOR.get().defaultBlockState();
        BlockState metal  = ModBlocks.FEDERATION_HULL.get().defaultBlockState();
        BlockState energy = ModBlocks.FEDERATION_ENERGY.get().defaultBlockState();
        BlockState red    = ModBlocks.FEDERATION_TERMINAL.get().defaultBlockState();

        // Outer ring wall around entire compound
        int R = 90;
        for (int x = -R; x <= R; x++) for (int z = -R; z <= R; z++) {
            int d2 = x*x+z*z;
            if (d2 < (R-3)*(R-3) || d2 > R*R) continue;
            set(level, CENTER.offset(x, 0, z), d2>(R-1)*(R-1) ? red : metal);
            set(level, CENTER.offset(x, 1, z), metal);
            set(level, CENTER.offset(x, 2, z), (x+z)%12==0 ? energy : metal);
        }
        // Four gate openings
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (int w = -4; w <= 4; w++) for (int y = 0; y <= 3; y++) {
                set(level, CENTER.relative(d, R).relative(d.getClockWise(), w).above(y),
                        Blocks.AIR.defaultBlockState());
            }
        }
        buildAdvancedWings(level);
    }

    /** Research and reactor wings added by layout v3. */
    static void finishVisualUpgrade(ServerLevel level){buildAdvancedWings(level);repairCriticalLayout(level);ensureStoryLandmarks(level);}

    private static void buildAdvancedWings(ServerLevel level){
        BlockState floor=ModBlocks.FEDERATION_FLOOR.get().defaultBlockState(),hull=ModBlocks.FEDERATION_HULL.get().defaultBlockState();
        BlockState energy=ModBlocks.FEDERATION_ENERGY.get().defaultBlockState(),glass=ModBlocks.FEDERATION_GLASS.get().defaultBlockState(),terminal=ModBlocks.FEDERATION_TERMINAL.get().defaultBlockState();
        BlockPos research=CENTER.offset(52,0,-52),reactor=CENTER.offset(-52,0,-52);
        enclosedCorridor(level,CENTER.offset(8,0,-8),research.offset(-12,0,12),3,floor,hull,glass,energy);
        enclosedCorridor(level,CENTER.offset(-8,0,-8),reactor.offset(12,0,12),3,floor,hull,glass,energy);
        facilityShell(level,research,14,11,7,floor,hull,glass,energy);
        facilityShell(level,reactor,14,11,8,floor,hull,glass,energy);
        // Xenobiology and dimensional-analysis stations.
        for(int x=-9;x<=9;x+=6){set(level,research.offset(x,1,-6),terminal);set(level,research.offset(x,1,6),terminal);for(int y=1;y<=4;y++)set(level,research.offset(x,y,0),y==1?energy:glass);}
        // Four containment rings around the military reactor.
        for(int y=1;y<=6;y++){set(level,reactor.offset(0,y,0),energy);if(y%2==0)for(Direction d:Direction.Plane.HORIZONTAL)for(int r=1;r<=4;r++)set(level,reactor.relative(d,r).above(y),r==4?terminal:glass);}
        set(level,research.offset(0,1,-9),terminal);set(level,reactor.offset(0,1,-9),terminal);
        // Functional armory access console used by the campaign/lore handler.
        set(level,CENTER.offset(-55,1,-10),terminal);
    }

    private static void facilityShell(ServerLevel level,BlockPos center,int rx,int rz,int height,BlockState floor,BlockState hull,BlockState glass,BlockState energy){
        for(int x=-rx;x<=rx;x++)for(int z=-rz;z<=rz;z++)for(int y=0;y<=height;y++){
            boolean shell=y==0||y==height||Math.abs(x)==rx||Math.abs(z)==rz;BlockPos p=center.offset(x,y,z);
            if(shell)set(level,p,y==0?((x+z)%9==0?energy:floor):(y==height||y%3==0?glass:hull));else set(level,p,Blocks.AIR.defaultBlockState());
        }
        for(int y=1;y<=4;y++)for(int x=-2;x<=2;x++)set(level,center.offset(x,y,rz),Blocks.AIR.defaultBlockState());
    }

    private static void enclosedCorridor(ServerLevel level,BlockPos from,BlockPos to,int half,BlockState floor,BlockState hull,BlockState glass,BlockState energy){
        int dx=to.getX()-from.getX(),dz=to.getZ()-from.getZ(),steps=Math.max(Math.abs(dx),Math.abs(dz));boolean eastWest=Math.abs(dx)>=Math.abs(dz);
        for(int i=0;i<=steps;i++){double t=i/(double)Math.max(1,steps);int cx=(int)Math.round(from.getX()+dx*t),cz=(int)Math.round(from.getZ()+dz*t);
            for(int w=-half;w<=half;w++){int x=cx+(eastWest?0:w),z=cz+(eastWest?w:0);set(level,new BlockPos(x,96,z),i%8==0&&w==0?energy:floor);for(int y=1;y<=4;y++)set(level,new BlockPos(x,96+y,z),Math.abs(w)==half?(y==2?glass:hull):Blocks.AIR.defaultBlockState());set(level,new BlockPos(x,101,z),glass);}
        }
    }

    /**
     * Repairs coordinates from early previews without replacing a player's full dimension.
     * The old armory was accidentally built 96 blocks above the compound because CENTER's Y
     * was added twice. Only the missing canonical wing is generated here.
     */
    private static void repairCriticalLayout(ServerLevel level) {
        BlockPos armory = CENTER.offset(-55, 0, 0);
        if (level.getBlockState(armory).isAir()) {
            buildArmory(level, armory,
                    ModBlocks.FEDERATION_FLOOR.get().defaultBlockState(),
                    ModBlocks.FEDERATION_HULL.get().defaultBlockState(),
                    ModBlocks.FEDERATION_ENERGY.get().defaultBlockState(),
                    ModBlocks.FEDERATION_TERMINAL.get().defaultBlockState());
        }
    }

    private static void ensureStoryLandmarks(ServerLevel level) {
        BlockPos archive = CENTER.offset(0, 1, -72);
        if (!(level.getBlockState(archive).getBlock() instanceof com.jhonfx.portalgun.block.CitadelServiceBlock))
            set(level, archive, ModBlocks.COUNCIL_ARCHIVE.get().defaultBlockState());
        boolean commanderExists = !level.getEntitiesOfClass(FederationAlienEntity.class,
                new net.minecraft.world.phys.AABB(CENTER.offset(0, 1, -68)).inflate(16),
                alien -> alien.getPersistentData().getBoolean("PortalGunStoryCommander")).isEmpty();
        if (!commanderExists && FederationSavedData.get(level).invasionStage()==0) {
            FederationAlienEntity commander = ModEntityTypes.FEDERATION_ALIEN.get().create(level);
            if (commander != null) {
                commander.moveTo(0.5, 97, -68.5, 180, 0);
                commander.setRank(FederationAlienEntity.Rank.OFFICER);
                commander.setCustomName(net.minecraft.network.chat.Component.literal("Comandante Gromflomite Vrax"));
                commander.setCustomNameVisible(true);
                commander.setPersistenceRequired();
                commander.getPersistentData().putBoolean("PortalGunStoryCommander", true);
                level.addFreshEntity(commander);
            }
        }
    }

    private static void installArmory(ServerLevel level) {
        FederationSavedData data=FederationSavedData.get(level);
        if(data.armoryInstalled()||data.isDestroyed()||data.collapseTicks()>0)return;
        BlockPos center=CENTER.offset(-55,0,0);
        for(int x=-16;x<=16;x+=6)for(int z:new int[]{-12,12}){
            BlockPos pos=center.offset(x,1,z);
            // Preserve any existing inventory on migration.
            if(!(level.getBlockEntity(pos) instanceof net.minecraft.world.Container))
                level.setBlock(pos,Blocks.BARREL.defaultBlockState(),2);
            if(level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity container
                    &&container.isEmpty())container.setLootTable(
                            new net.minecraft.resources.ResourceLocation("portalgun","chests/federation_armory"),level.random.nextLong());
        }
        // A five-wide entrance gives combat room and removes the old single-cell bottleneck.
        for(int z=-2;z<=2;z++)for(int y=1;y<=3;y++)
            level.setBlock(center.offset(18,y,z),Blocks.AIR.defaultBlockState(),2);
        data.markArmoryInstalled();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  NPC PATROL
    // ──────────────────────────────────────────────────────────────────────────
    private static void ensureNpcs(ServerLevel level) {
        if(FederationSavedData.get(level).invasionStage()>0)return;
        long count = level.getEntitiesOfClass(FederationAlienEntity.class,
                new net.minecraft.world.phys.AABB(CENTER).inflate(120),
                FederationAlienEntity::isAlive).size();
        if (count >= 12) return;
        int needed = (int)(12 - count);
        BlockPos[] posts = {
            CENTER.offset(0,1,8),CENTER.offset(8,1,0),CENTER.offset(-8,1,0),CENTER.offset(0,1,-8),
            CUSTOMS_POS.offset(0,1,-8),CUSTOMS_POS.offset(-8,1,0),CUSTOMS_POS.offset(8,1,0),
            PRISON_POS.offset(0,1,0),PRISON_POS.offset(10,1,0),
            CENTER.offset(-55,1,0),CENTER.offset(0,1,50),CENTER.offset(0,1,-40)
        };
        for (int i = 0; i < needed && i < posts.length; i++) {
            FederationAlienEntity alien = ModEntityTypes.FEDERATION_ALIEN.get().create(level);
            if (alien == null) continue;
            alien.setRank(i>=7&&i<=8?FederationAlienEntity.Rank.PRISON_GUARD:FederationAlienEntity.Rank.SECURITY);
            BlockPos p = posts[i];
            alien.moveTo(p.getX()+.5, p.getY(), p.getZ()+.5, 0, 0);
            alien.setPersistenceRequired();
            level.addFreshEntity(alien);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ──────────────────────────────────────────────────────────────────────────
    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        java.util.List<Placement> capture=CAPTURE.get();
        if(capture!=null)capture.add(new Placement(pos.immutable(),state));
        else level.setBlock(pos, state, 2);
    }

    private record Placement(BlockPos pos,BlockState state){}
    private record BuildJob(java.util.List<Placement> placements,int cursor,boolean markBuilt,boolean markExpanded){}

    private static void avenue(ServerLevel level, BlockPos from, Direction dir,
                                int length, int halfWidth, BlockState floor, BlockState accent) {
        for (int step = 1; step <= length; step++) {
            for (int w = -halfWidth; w <= halfWidth; w++) {
                BlockPos p = from.relative(dir, step).relative(dir.getClockWise(), w);
                set(level, p, (w==halfWidth||w==-halfWidth) ? accent : floor);
            }
        }
    }

    private FederationBuilder() {}
}

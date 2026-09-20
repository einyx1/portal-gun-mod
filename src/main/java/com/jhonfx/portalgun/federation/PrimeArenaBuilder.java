package com.jhonfx.portalgun.federation;

import com.jhonfx.portalgun.entity.CitadelNpcEntity;
import com.jhonfx.portalgun.entity.CitadelNpcEntity.Variant;
import com.jhonfx.portalgun.init.ModBlocks;
import com.jhonfx.portalgun.init.ModEntityTypes;
import com.jhonfx.portalgun.entity.PortalColor;
import com.jhonfx.portalgun.entity.PortalEntity;
import com.jhonfx.portalgun.omega.OmegaStructureBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sala da armadilha de Rick Prime — "A Armadilha dos Ricks".
 *
 * Uma câmara fechada e isolada, com campo de bloqueio dimensional
 * (PORTAL FIELD JAMMED) enquanto os Ricks cativos estiverem vivos.
 * Localizada bem longe do centro da Federação para não colidir com
 * a estrutura pública.
 */
public final class PrimeArenaBuilder {
    /** Ponto de entrada — onde o jogador chega antes da armadilha ser ativada. */
    public static final BlockPos TRAP_ENTRANCE = new BlockPos(300, 97, 300);
    /** Guaranteed two-block-high air space directly above the fake lab floor. */
    public static final BlockPos TRAP_ARRIVAL = TRAP_ENTRANCE.above();
    /** Survival-accessible Omega calculation hall used between the trap and UNMORTRICKEN. */
    public static final BlockPos OMEGA_RETURN_CENTER = new BlockPos(-300, 98, 300);
    public static final BlockPos OMEGA_RETURN_ARRIVAL = OMEGA_RETURN_CENTER.relative(Direction.SOUTH, 29);
    /** Centro real da arena fechada (um pouco abaixo/distante da entrada). */
    private static final BlockPos ARENA_CENTER = new BlockPos(300, 60, 400);
    public static final int RADIUS = 18;

    private static final String KEY_BUILT = "PrimeArenaBuilt";

    public static BlockPos ensureBuilt(ServerLevel level) {
        // Marca simples: se já existe bedrock no centro, não reconstrói.
        if (!level.getBlockState(ARENA_CENTER.below()).is(Blocks.BEDROCK)) {
            build(level);
        }
        // Ponto de entrada simples (pequena sala falsa "instalação pequena")
        buildFakeLab(level, TRAP_ENTRANCE);
        return ARENA_CENTER;
    }

    public static BlockPos currentArenaCenter(ServerLevel level) {
        return level.getBlockState(ARENA_CENTER.below()).is(Blocks.BEDROCK) ? ARENA_CENTER : null;
    }

    // ── Falsa instalação de entrada (onde o "Prime" falso é encontrado) ──────
    private static void buildFakeLab(ServerLevel level, BlockPos center) {
        BlockState metal = ModBlocks.CITADEL_ALLOY.get().defaultBlockState();
        BlockState floor = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
            for (int y = 0; y <= 4; y++) {
                boolean shell = y == 0 || y == 4 || Math.abs(x) == 6 || Math.abs(z) == 6;
                BlockPos p = center.offset(x, y, z);
                set(level, p, shell ? metal : Blocks.AIR.defaultBlockState());
            }
        }
        for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) set(level, center.offset(x, -1, z), floor);
        set(level, center.above(3), Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 12));
        for (int dy = 1; dy <= 3; dy++) set(level, center.offset(6, dy, 0), Blocks.AIR.defaultBlockState());
    }

    // ── Arena fechada, isolada, sem saída visível ────────────────────────────
    private static void build(ServerLevel level) {
        BlockState wall = ModBlocks.CITADEL_ALLOY.get().defaultBlockState();
        BlockState floor = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState energy = ModBlocks.CITADEL_CIRCUIT.get().defaultBlockState();
        BlockState red = Blocks.RED_STAINED_GLASS.defaultBlockState();

        for (int x = -RADIUS; x <= RADIUS; x++) for (int z = -RADIUS; z <= RADIUS; z++) {
            double d = Math.sqrt(x * x + z * z);
            if (d > RADIUS) continue;
            set(level, ARENA_CENTER.offset(x, -1, z), Blocks.BEDROCK.defaultBlockState());
            set(level, ARENA_CENTER.offset(x, 0, z), d > RADIUS - 2 ? red : ((x + z) % 9 == 0 ? energy : floor));
            for (int y = 1; y <= 8; y++) {
                boolean shell = d > RADIUS - 1.4;
                set(level, ARENA_CENTER.offset(x, y, z), shell ? wall : Blocks.AIR.defaultBlockState());
            }
        }
        // Teto fechado — sem saída (isolamento total)
        for (int x = -RADIUS; x <= RADIUS; x++) for (int z = -RADIUS; z <= RADIUS; z++) {
            if (x * x + z * z > RADIUS * RADIUS) continue;
            set(level, ARENA_CENTER.offset(x, 9, z), (x + z) % 7 == 0 ? energy : wall);
        }
        // Tela gigante central (representa a transmissão de Rick Prime)
        for (int x = -3; x <= 3; x++) for (int y = 3; y <= 6; y++) {
            set(level, ARENA_CENTER.offset(x, y, -RADIUS + 2), Blocks.BLACK_STAINED_GLASS.defaultBlockState());
        }
        set(level, ARENA_CENTER.below(2), Blocks.REINFORCED_DEEPSLATE.defaultBlockState());
    }

    // ── Spawn dos Ricks cativos ──────────────────────────────────────────────
    public static void spawnCaptiveRicks(ServerLevel level, BlockPos arena) {
        prepareBattleRoyale(level, arena);
        Variant[] captives = {
            Variant.RICK_COMMANDO, Variant.RICK_CYBORG, Variant.RICK_ASSASSIN, Variant.RICK_SOLDIER,
            Variant.RICK_AUGMENTED, Variant.RICK_SCARRED, Variant.RICK_ELDER, Variant.RICK_COMMANDO
        };
        String[] names = {
            "Rick Commando", "Rick Ciborgue", "Rick Assassino", "Rick Soldado",
            "Rick Aumentado", "Rick Cicatrizado", "Rick Idoso", "Rick Caçador"
        };
        int[][] spots={{-10,-9},{-6,-9},{6,-9},{10,-9},{-10,9},{-6,9},{6,9},{10,9}};
        for (int i = 0; i < captives.length; i++) {
            CitadelNpcEntity rick = com.jhonfx.portalgun.init.ModEntityTypes.CITADEL_CITIZEN.get().create(level);
            if (rick == null) continue;
            rick.setVariant(captives[i]);
            rick.setCustomName(Component.literal(names[i]));
            rick.setCustomNameVisible(true);
            rick.moveTo(arena.getX()+spots[i][0]+.5,arena.getY()+1,arena.getZ()+spots[i][1]+.5,0,0);
            rick.setPersistenceRequired();
            rick.getPersistentData().putBoolean("PortalGunArenaRick",true);
            rick.getPersistentData().putInt("PortalGunArenaQuadrant",(spots[i][0]>0?1:0)+(spots[i][1]>0?2:0));
            level.addFreshEntity(rick);
        }
    }

    private static void prepareBattleRoyale(ServerLevel level,BlockPos arena){
        BlockState divider=ModBlocks.CITADEL_ALLOY.get().defaultBlockState();
        for(int axis=-15;axis<=15;axis++)for(int y=1;y<=4;y++){
            set(level,arena.offset(0,y,axis),divider);set(level,arena.offset(axis,y,0),divider);
        }
        for(int x:new int[]{-8,8})for(int z:new int[]{-8,8})
            set(level,arena.offset(x,8,z),Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL,15));
    }

    /** Opens a quadrant when its local duel has produced one winner. */
    public static boolean openWinnerGate(ServerLevel level,BlockPos arena,int quadrant){
        int sx=(quadrant&1)==0?-1:1,sz=(quadrant&2)==0?-1:1;
        boolean changed=false;
        for(int n=3;n<=8;n++)for(int y=1;y<=3;y++){
            BlockPos a=arena.offset(0,y,sz*n),b=arena.offset(sx*n,y,0);
            if(!level.getBlockState(a).isAir()){set(level,a,Blocks.AIR.defaultBlockState());changed=true;}
            if(!level.getBlockState(b).isAir()){set(level,b,Blocks.AIR.defaultBlockState());changed=true;}
        }
        return changed;
    }

    // ── Bloqueio dimensional da sala (PORTAL FIELD JAMMED) ───────────────────
    /** Enquanto houver Ricks cativos vivos na arena, o bloqueio permanece ativo. */
    public static boolean isJammed(ServerLevel level, BlockPos playerPos) {
        BlockPos arena = currentArenaCenter(level);
        if (arena == null) return false;
        if (playerPos.distSqr(arena) > (RADIUS + 4) * (RADIUS + 4)) return false;
        return !level.getEntitiesOfClass(CitadelNpcEntity.class,
                new net.minecraft.world.phys.AABB(arena).inflate(RADIUS),
                e -> CitadelNpcEntity.isCaptiveRick(e.getVariant()) && e.isAlive()).isEmpty();
    }

    public static void releaseJam(ServerLevel level, BlockPos arena, net.minecraft.server.level.ServerPlayer player) {
        // Repair the old void-facing doorway before creating a real linked exit.
        for (int dx = -2; dx <= 2; dx++) for (int dy = 1; dy <= 3; dy++) {
            set(level, arena.offset(dx, dy, RADIUS - 1), ModBlocks.CITADEL_ALLOY.get().defaultBlockState());
        }
        ensureOmegaReturn(level);
        PortalEntity entry=ModEntityTypes.PORTAL.get().create(level),exit=ModEntityTypes.PORTAL.get().create(level);
        if(entry==null||exit==null)return;
        entry.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(arena.offset(0,1,RADIUS-5)));
        exit.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(OMEGA_RETURN_ARRIVAL));
        for(PortalEntity portal:new PortalEntity[]{entry,exit}){portal.setPortalFacing(Direction.UP);portal.setColor(PortalColor.GREEN);
            portal.setPortalScale(.9f);portal.setLifetimeTicks(Integer.MAX_VALUE);portal.setSafeMode(true);portal.setOwnerPlayerId(player.getUUID());level.addFreshEntity(portal);}
        entry.setLinkedPortal(exit);exit.setLinkedPortal(entry);
    }

    /** Builds the missing story bridge without spawning a second Rick Prime. */
    public static void ensureOmegaReturn(ServerLevel level) {
        BlockPos console = OMEGA_RETURN_CENTER.relative(Direction.NORTH, OmegaStructureBuilder.CONSOLE_DISTANCE);
        if (!level.getBlockState(console).is(ModBlocks.OMEGA_DEVICE.get())) {
            level.getChunkAt(OMEGA_RETURN_CENTER);
            OmegaStructureBuilder.build(level, OMEGA_RETURN_CENTER, Direction.SOUTH, false);
        }else if(level.getBlockState(console).getValue(com.jhonfx.portalgun.block.OmegaDeviceBlock.FACING)!=Direction.SOUTH){
            level.setBlock(console,level.getBlockState(console).setValue(com.jhonfx.portalgun.block.OmegaDeviceBlock.FACING,Direction.SOUTH),2);
        }
    }

    /**
     * Migration route for preview saves whose old evacuation portal already
     * delivered the player to the fake laboratory.
     */
    public static boolean ensureLegacyOmegaPortal(ServerLevel level, net.minecraft.server.level.ServerPlayer player) {
        var data = player.getPersistentData();
        if (data.getBoolean("PortalGunOmegaReturnRoute")
                && data.hasUUID("PortalGunOmegaReturnEntry")
                && level.getEntity(data.getUUID("PortalGunOmegaReturnEntry")) instanceof PortalEntity alive
                && alive.isAlive()) return false;
        ensureOmegaReturn(level);
        level.getChunkAt(TRAP_ARRIVAL);
        level.getChunkAt(OMEGA_RETURN_ARRIVAL);
        PortalEntity entry=ModEntityTypes.PORTAL.get().create(level),exit=ModEntityTypes.PORTAL.get().create(level);
        if(entry==null||exit==null)return false;
        entry.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(TRAP_ARRIVAL.offset(4,0,0)));
        exit.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(OMEGA_RETURN_ARRIVAL));
        for(PortalEntity portal:new PortalEntity[]{entry,exit}){
            portal.setPortalFacing(Direction.UP);portal.setColor(PortalColor.GREEN);
            portal.setPortalScale(.9f);portal.setLifetimeTicks(Integer.MAX_VALUE);
            portal.setSafeMode(true);portal.setOwnerPlayerId(player.getUUID());level.addFreshEntity(portal);
        }
        entry.setLinkedPortal(exit);exit.setLinkedPortal(entry);
        data.putBoolean("PortalGunOmegaReturnRoute",true);
        data.putUUID("PortalGunOmegaReturnEntry",entry.getUUID());
        data.putUUID("PortalGunOmegaReturnExit",exit.getUUID());
        return true;
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 2);
    }

    private PrimeArenaBuilder() {}
}

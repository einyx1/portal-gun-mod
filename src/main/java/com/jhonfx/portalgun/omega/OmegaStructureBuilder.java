package com.jhonfx.portalgun.omega;

import com.jhonfx.portalgun.block.OmegaDeviceBlock;
import com.jhonfx.portalgun.init.ModBlocks;
import com.jhonfx.portalgun.init.ModEntityTypes;
import com.jhonfx.portalgun.entity.RickPrimeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Rick Prime's lab arena.
 *
 * Layout (relative to center, Y=0 = floor level):
 *   Ground floor:  open circular arena, radius 36
 *   Deck 1:        y=8,  annular ring r14-19   (4 cardinal bridges)
 *   Deck 2:        y=16, annular ring r 8-14   (diagonal bridges)
 *   Deck 3:        y=23, annular ring r24-31   (wide balcony)
 *   Rick's balcony: y=17, north wall — where Rick starts and retreats to during Diane phase
 *   Central dais:  raised pedestal with Omega reactor below
 *   Entry portal frame: north wall at deck 3 height — cinematic arrival point
 */
public final class OmegaStructureBuilder {
    public static final int RADIUS           = 36;
    public static final int HEIGHT           = 30;
    public static final int CONSOLE_DISTANCE = 31;

    /** Y offset (above center) of the high balcony Rick uses for his intro/retreat */
    public static final int BALCONY_Y = 17;

    public static void build(ServerLevel level, BlockPos center, Direction consoleFacing) {
        build(level, center, consoleFacing, true);
    }

    /**
     * Builds the machine hall. Story transit rooms use {@code spawnPrime=false}
     * so reaching the calculation console cannot accidentally start the final
     * boss before UNMORTRICKEN has actually been unlocked.
     */
    public static void build(ServerLevel level, BlockPos center, Direction consoleFacing, boolean spawnPrime) {
        BlockState floor  = ModBlocks.OMEGA_LAB_PANEL.get().defaultBlockState();
        BlockState trim   = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState wall   = ModBlocks.OMEGA_LAB_PANEL.get().defaultBlockState();
        BlockState energy = ModBlocks.OMEGA_ENERGY_GLASS.get().defaultBlockState();

        // ── Ground plate + cylindrical walls ─────────────────────────────────
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist > RADIUS) continue;

                boolean floorLight = x % 6 == 0 && z % 6 == 0 && dist < RADIUS - 3;
                set(level, center.offset(x, -1, z),
                        dist > RADIUS - 2 ? trim
                        : floorLight ? Blocks.SEA_LANTERN.defaultBlockState() : floor);

                for (int y = 0; y <= HEIGHT; y++) {
                    BlockPos p = center.offset(x, y, z);
                    if (y == HEIGHT) {
                        set(level, p, dist <= 17.5 ? energy : wall);
                    } else if (dist < RADIUS - 1.2) {
                        set(level, p, Blocks.AIR.defaultBlockState());
                    } else {
                        set(level, p, wall);
                    }
                }

                // Wall accents
                if (dist > RADIUS - 1.4 && dist <= RADIUS) {
                    if ((x + z & 3) == 0) set(level, center.offset(x, 5, z), ModBlocks.OMEGA_ALARM_PANEL.get().defaultBlockState());
                    if ((x - z & 7) == 0) for (int y = 10; y <= 17; y++)
                        set(level, center.offset(x, y, z), energy);
                }
            }
        }

        // ── Spawn-proof lighting grid ────────────────────────────────────────
        for (int x = -32; x <= 32; x += 6) for (int z = -32; z <= 32; z += 6) {
            if (x*x + z*z > 32*32) continue;
            for (int y : new int[]{3, 11, 19, 27})
                set(level, center.offset(x, y, z),
                        Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15));
        }

        // ── Central containment dais ─────────────────────────────────────────
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            if (x*x + z*z <= 10) set(level, center.offset(x, 0, z),
                    (Math.abs(x)==3 || Math.abs(z)==3)
                    ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                    : Blocks.QUARTZ_BLOCK.defaultBlockState());
        }
        set(level, center.below(3), Blocks.REINFORCED_DEEPSLATE.defaultBlockState());
        set(level, center.below(2), ModBlocks.OMEGA_CORE.get().defaultBlockState());
        set(level, center.below(),  Blocks.LIME_STAINED_GLASS.defaultBlockState());
        set(level, center,          Blocks.LIME_STAINED_GLASS.defaultBlockState());
        set(level, center.above(),  Blocks.PINK_STAINED_GLASS.defaultBlockState());

        // ── Three annular decks + staircases ─────────────────────────────────
        buildUpperDeck(level, center,  8, 14, 19, false);  // deck 1 — cardinal
        buildUpperDeck(level, center, 16,  8, 14, true);   // deck 2 — diagonal
        buildUpperDeck(level, center, 23, 24, 31, false);  // deck 3 — wide outer ring

        // ── Rick's arrival balcony (north wall, y=17) ─────────────────────────
        // A 7×5 platform inset into the north wall where Rick spawns and retreats to.
        buildBalcony(level, center, consoleFacing);

        // ── Omega console ─────────────────────────────────────────────────────
        // FACING always points from the console into the reactor. Therefore the
        // physical console sits on the opposite side of the center vector.
        Direction inward = consoleFacing;
        BlockPos console = center.relative(inward.getOpposite(), CONSOLE_DISTANCE);
        set(level, console.below(), Blocks.IRON_BLOCK.defaultBlockState());
        set(level, console, ModBlocks.OMEGA_DEVICE.get().defaultBlockState()
                .setValue(OmegaDeviceBlock.FACING, inward));

        // ── Entry door ────────────────────────────────────────────────────────
        Direction outside = consoleFacing.getOpposite();
        Direction side    = outside.getClockWise();
        BlockPos doorway  = center.relative(outside, RADIUS);
        for (int lateral = -1; lateral <= 1; lateral++) for (int y = 0; y <= 3; y++)
            set(level, doorway.relative(side, lateral).above(y), Blocks.AIR.defaultBlockState());
        for (int lateral = -1; lateral <= 0; lateral++) {
            BlockPos door = doorway.relative(side, lateral);
            BlockState lower = Blocks.IRON_DOOR.defaultBlockState()
                    .setValue(DoorBlock.FACING, consoleFacing)
                    .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
            set(level, door,         lower);
            set(level, door.above(), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        }
        set(level, doorway.relative(consoleFacing,  1), Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE.defaultBlockState());
        set(level, doorway.relative(outside,        1), Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE.defaultBlockState());

        // ── Spawn Rick Prime if not already present ───────────────────────────
        if (spawnPrime && level.getEntitiesOfClass(RickPrimeEntity.class,
                new net.minecraft.world.phys.AABB(center).inflate(RADIUS),
                e -> !e.isClone()).isEmpty()) {
            RickPrimeEntity prime = ModEntityTypes.RICK_PRIME.get().create(level);
            if (prime != null) {
                // Spawn on balcony (north wall, high up)
                BlockPos balcony = center.relative(Direction.NORTH, RADIUS - 4).above(BALCONY_Y);
                prime.moveTo(balcony.getX() + 0.5, balcony.getY(), balcony.getZ() + 0.5, 180, 0);
                prime.configureArena(center);
                prime.setPersistenceRequired();
                level.addFreshEntity(prime);
            }
        }
    }

    // ── Rick's balcony ────────────────────────────────────────────────────────
    private static void buildBalcony(ServerLevel level, BlockPos center, Direction consoleFacing) {
        // Put the balcony on the wall OPPOSITE to the console (same as the entry door side)
        Direction balconyWall = consoleFacing; // same wall Rick faces the arena from
        BlockPos balconyBase  = center.relative(balconyWall, RADIUS - 4).above(BALCONY_Y);

        BlockState deck   = ModBlocks.OMEGA_LAB_PANEL.get().defaultBlockState();
        BlockState railing = Blocks.IRON_BARS.defaultBlockState();

        Direction side = balconyWall.getClockWise();
        for (int w = -3; w <= 3; w++) for (int d = 0; d <= 4; d++) {
            set(level, balconyBase.relative(side, w).relative(balconyWall.getOpposite(), d), deck);
        }
        // Front railing
        for (int w = -3; w <= 3; w++) {
            set(level, balconyBase.relative(side, w).relative(balconyWall.getOpposite(), 4).above(), railing);
        }
        // Side railings
        for (int d = 0; d <= 4; d++) {
            set(level, balconyBase.relative(side, -3).relative(balconyWall.getOpposite(), d).above(), railing);
            set(level, balconyBase.relative(side,  3).relative(balconyWall.getOpposite(), d).above(), railing);
        }
        // Accent light
        set(level, balconyBase.above(2), Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 14));
        // Portal-arrival marker (glowing block Rick steps through)
        set(level, balconyBase.above(), Blocks.LIME_STAINED_GLASS.defaultBlockState());
    }

    // ── Annular deck + staircases ─────────────────────────────────────────────
    private static void buildUpperDeck(ServerLevel level, BlockPos center,
                                       int y, int inner, int outer, boolean diagonal) {
        BlockState deck = ModBlocks.OMEGA_LAB_PANEL.get().defaultBlockState();
        BlockState edge = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();

        for (int x = -outer; x <= outer; x++) for (int z = -outer; z <= outer; z++) {
            double d = Math.sqrt(x*x + z*z);
            boolean ring   = d >= inner && d <= outer;
            boolean bridge = diagonal
                    ? (Math.abs(x-z) <= 2 || Math.abs(x+z) <= 2) && d <= outer
                    : (Math.abs(x) <= 2 || Math.abs(z) <= 2) && d <= outer;
            if (ring || bridge) {
                set(level, center.offset(x, y, z),
                        (x+z & 7) == 0 ? Blocks.SEA_LANTERN.defaultBlockState()
                        : d < inner+1 || d > outer-1 ? edge : deck);
            }
        }
        // Iron-bar railings on outer edge
        for (int x = -outer; x <= outer; x++) for (int z = -outer; z <= outer; z++) {
            double d = Math.sqrt(x*x + z*z);
            if (d >= outer-0.7 && d <= outer+0.3 && (x+z & 1) == 0)
                set(level, center.offset(x, y+1, z), Blocks.IRON_BARS.defaultBlockState());
        }

        // Staircases
        Direction[] axes = diagonal
                ? new Direction[]{Direction.EAST, Direction.WEST}
                : new Direction[]{Direction.NORTH, Direction.SOUTH};
        for (Direction dir : axes) {
            Direction sl = dir.getClockWise();
            BlockState stair = Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState()
                    .setValue(StairBlock.FACING, dir.getOpposite());
            for (int step = 0; step < 8; step++) {
                BlockPos mid = center.relative(dir, outer-step).above(y-8+step);
                for (int w = -2; w <= 2; w++) set(level, mid.relative(sl, w), stair);
                set(level, mid.relative(sl, -3), Blocks.IRON_BARS.defaultBlockState());
                set(level, mid.relative(sl,  3), Blocks.IRON_BARS.defaultBlockState());
            }
            BlockPos landing = center.relative(dir, outer-8).above(y);
            for (int fw = 0; fw <= 2; fw++) for (int w = -2; w <= 2; w++)
                set(level, landing.relative(dir.getOpposite(), fw).relative(sl, w), deck);
        }
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 3);
    }

    private OmegaStructureBuilder() {}
}

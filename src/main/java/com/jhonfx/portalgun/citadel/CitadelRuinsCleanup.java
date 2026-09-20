package com.jhonfx.portalgun.citadel;

import com.jhonfx.portalgun.curve.CurveSavedData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Migrates already-destroyed preview saves whose old Citadel geometry survived the VFX. */
public final class CitadelRuinsCleanup {
    private static final ResourceLocation CITADEL = new ResourceLocation("portalgun", "citadel");

    @SubscribeEvent public void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
                || !level.dimension().location().equals(CITADEL) || !CurveSavedData.get(level).broken()) return;
        if(CurveSavedData.get(level).collapseTicks()>0)return;
        CitadelSavedData state = CitadelSavedData.get(level);
        if (state.isOldCitadelPurged()) return;
        int start = state.getRuinCleanupColumn();
        int end = Math.min(52441, start + 12),index=start;
        long deadline=System.nanoTime()+4_000_000L;
        while(index<end) {
            int x = -114 + index%229,z=-114+index/229;
            // The refuge already contains survivors and service booths. Its
            // northern edge overlaps the old city's cleanup footprint.
            if(!(Math.abs(x)<=27&&z>=92&&z<=133))for (int y = 74; y <= 145; y++) {
                var pos = CitadelBuilder.CENTER.offset(x, y - CitadelBuilder.CENTER.getY(), z);
                if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
            index++;
            if(System.nanoTime()>=deadline)break;
        }
        state.advanceRuinCleanup(index - start);
        if (index >= 52441) {
            state.markOldCitadelPurged();
            CitadelBuilder.ensureRefuge(level);
        }
    }
}

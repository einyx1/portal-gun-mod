package com.jhonfx.portalgun;

import com.jhonfx.portalgun.citadel.CitadelSavedData;
import com.jhonfx.portalgun.federation.FederationSavedData;
import net.minecraft.nbt.CompoundTag;

/** Save/reload regressions that run without a client, worlds, or extra test dependencies. */
public final class CampaignPersistenceTest {
    public static void main(String[] args) {
        FederationSavedData federation=new FederationSavedData();
        federation.markBuilt();federation.markExpanded();federation.markArmoryInstalled();
        federation.beginInvasion();federation.advanceObjective();
        FederationSavedData resumed=FederationSavedData.load(federation.save(new CompoundTag()));
        check(resumed.invasionStage()==1&&resumed.objectiveProgress()==1,"Invasion objective lost on reload");
        check(resumed.armoryInstalled(),"Loot installation lost: could duplicate weapons");
        resumed.beginCollapse();
        for(int i=0;i<51;i++)resumed.tickCollapse();
        resumed=FederationSavedData.load(resumed.save(new CompoundTag()));
        check(resumed.collapseTicks()==149,"Countdown restarted after reload");
        resumed.finishInvasion();resumed.advanceRuins(600);
        resumed=FederationSavedData.load(resumed.save(new CompoundTag()));
        check(resumed.isDestroyed()&&resumed.ruinCursor()==600,"Destruction failed to persist");
        CompoundTag legacy=new CompoundTag();legacy.putBoolean("Destroyed",true);
        check(FederationSavedData.load(legacy).ruinCursor()==27000,"Old destroyed world would be destroyed twice");
        CompoundTag malformed=new CompoundTag();malformed.putInt("InvasionStage",99);malformed.putInt("ObjectiveProgress",-1);
        FederationSavedData bounded=FederationSavedData.load(malformed);
        check(bounded.invasionStage()==7&&bounded.objectiveProgress()==0,"Invalid save could index outside objective array");
        CompoundTag oldCity=new CompoundTag();oldCity.putBoolean("OldCitadelPurged",true);oldCity.putInt("RuinCleanupColumn",201);
        CitadelSavedData city=CitadelSavedData.load(oldCity);
        check(!city.isOldCitadelPurged()&&city.getRuinCleanupColumn()==0,"Incomplete legacy cleanup was not migrated");
        city.advanceRuinCleanup(24);
        city=CitadelSavedData.load(city.save(new CompoundTag()));
        check(city.getRuinCleanupColumn()==24,"City cleanup progress lost");
        city.markOldCitadelPurged();
        city=CitadelSavedData.load(city.save(new CompoundTag()));
        check(city.isOldCitadelPurged()&&city.getRuinCleanupColumn()==52441,"Completed cleanup would restart");
        System.out.println("Campaign persistence: 9 regression checks passed.");
    }
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}

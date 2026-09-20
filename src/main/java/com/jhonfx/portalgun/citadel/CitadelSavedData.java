package com.jhonfx.portalgun.citadel;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

public final class CitadelSavedData extends SavedData {
    private static final String NAME = "portalgun_citadel";
    private boolean built;
    private boolean curveWingBuilt;
    private int layoutVersion;
    private boolean refugeBuilt;
    private int refugeVersion;
    private int ruinCleanupColumn;
    private boolean oldCitadelPurged;
    private int navigationVersion;

    public static CitadelSavedData load(CompoundTag tag) {
        CitadelSavedData data = new CitadelSavedData();
        data.built = tag.getBoolean("Built");
        data.curveWingBuilt = tag.getBoolean("CurveWingBuilt");
        data.layoutVersion = tag.getInt("LayoutVersion");
        data.refugeBuilt = tag.getBoolean("RefugeBuilt");
        data.refugeVersion = tag.getInt("RefugeVersion");
        data.ruinCleanupColumn = tag.getInt("CleanupVersion")==2?Math.max(0,Math.min(52441,tag.getInt("RuinCleanupColumn"))):0;
        data.oldCitadelPurged = tag.getInt("CleanupVersion")==2&&tag.getBoolean("OldCitadelPurged");
        data.navigationVersion = tag.getInt("NavigationVersion");
        return data;
    }

    public static CitadelSavedData get(net.minecraft.server.level.ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(CitadelSavedData::load, CitadelSavedData::new, NAME);
    }

    public boolean isBuilt() { return built; }
    public boolean isRefugeBuilt() { return refugeBuilt; }
    public void markRefugeBuilt() { refugeBuilt = true; setDirty(); }
    public int getRefugeVersion() { return refugeVersion; }
    public void setRefugeVersion(int version) { refugeBuilt = true; refugeVersion = version; setDirty(); }
    public int getRuinCleanupColumn() { return ruinCleanupColumn; }
    public boolean isOldCitadelPurged() { return oldCitadelPurged; }
    public void advanceRuinCleanup(int amount) { ruinCleanupColumn = Math.min(52441, ruinCleanupColumn + amount); setDirty(); }
    public void markOldCitadelPurged() { ruinCleanupColumn = 52441; oldCitadelPurged = true; setDirty(); }
    public boolean isCurveWingBuilt(){return curveWingBuilt;}
    public void markCurveWingBuilt(){curveWingBuilt=true;setDirty();}
    public int getLayoutVersion(){return layoutVersion;}
    public void setLayoutVersion(int version){layoutVersion=version;setDirty();}
    public void prepareV4Rebuild(){curveWingBuilt=false;setDirty();}
    public int getNavigationVersion(){return navigationVersion;}
    public void setNavigationVersion(int version){navigationVersion=version;setDirty();}

    public void markBuilt() {
        built = true;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("Built", built);
        tag.putBoolean("CurveWingBuilt",curveWingBuilt);
        tag.putInt("LayoutVersion",layoutVersion);
        tag.putBoolean("RefugeBuilt",refugeBuilt);
        tag.putInt("RefugeVersion",refugeVersion);
        tag.putInt("RuinCleanupColumn",ruinCleanupColumn);
        tag.putInt("CleanupVersion",2);
        tag.putBoolean("OldCitadelPurged",oldCitadelPurged);
        tag.putInt("NavigationVersion",navigationVersion);
        return tag;
    }
}

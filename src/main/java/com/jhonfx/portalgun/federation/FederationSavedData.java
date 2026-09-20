package com.jhonfx.portalgun.federation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class FederationSavedData extends SavedData {
    private static final String NAME = "portalgun_federation";
    private boolean built;
    private boolean expanded;
    private int invasionStage;
    private boolean destroyed;
    private int defenseStageSpawned;
    private int layoutVersion;
    private boolean visualUpgrade;
    private int visualUpgradeCursor;
    private int collapseTicks;
    private int objectiveProgress;
    private boolean armoryInstalled;
    private int ruinCursor;

    public static FederationSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                FederationSavedData::load,
                FederationSavedData::new,
                NAME);
    }

    public static FederationSavedData load(CompoundTag tag) {
        FederationSavedData d = new FederationSavedData();
        d.built    = tag.getBoolean("Built");
        d.expanded = tag.getBoolean("Expanded");
        d.invasionStage = Math.max(0,Math.min(7,tag.getInt("InvasionStage")));
        d.destroyed = tag.getBoolean("Destroyed");
        d.defenseStageSpawned = tag.getInt("DefenseStageSpawned");
        d.layoutVersion = tag.getInt("LayoutVersion");
        d.visualUpgrade = tag.getBoolean("VisualUpgrade");
        d.visualUpgradeCursor = tag.getInt("VisualUpgradeCursor");
        d.collapseTicks = tag.getInt("CollapseTicks");
        d.objectiveProgress = Math.max(0,Math.min(4,tag.getInt("ObjectiveProgress")));
        d.armoryInstalled=tag.getBoolean("ArmoryInstalled");
        d.ruinCursor=tag.contains("RuinCursor")?Math.max(0,tag.getInt("RuinCursor")):(d.destroyed?27000:0);
        return d;
    }

    public boolean isBuilt()    { return built; }
    public boolean isExpanded() { return expanded; }
    public void markBuilt()    { built    = true; setDirty(); }
    public void markExpanded() { expanded = true; setDirty(); }
    public int invasionStage() { return invasionStage; }
    public boolean isDestroyed() { return destroyed; }
    public int defenseStageSpawned() { return defenseStageSpawned; }
    public void markDefenseSpawned(int stage) { defenseStageSpawned = Math.max(defenseStageSpawned, stage); setDirty(); }
    public void beginInvasion() { if (invasionStage == 0) { invasionStage = 1; setDirty(); } }
    public void advanceInvasion() { invasionStage = Math.min(7, invasionStage + 1); objectiveProgress = 0; setDirty(); }
    public void finishInvasion() { invasionStage = 7; destroyed = true; setDirty(); }
    public int layoutVersion(){return layoutVersion;}
    public void setLayoutVersion(int version){layoutVersion=version;visualUpgrade=false;visualUpgradeCursor=0;setDirty();}
    public boolean visualUpgrade(){return visualUpgrade;}
    public int visualUpgradeCursor(){return visualUpgradeCursor;}
    public void beginVisualUpgrade(){if(layoutVersion<3&&!visualUpgrade){visualUpgrade=true;visualUpgradeCursor=0;setDirty();}}
    public void advanceVisualUpgrade(int amount){visualUpgradeCursor+=amount;setDirty();}
    public int collapseTicks(){return collapseTicks;}
    public void beginCollapse(){invasionStage=7;collapseTicks=200;setDirty();}
    public int tickCollapse(){if(collapseTicks>0){collapseTicks--;setDirty();}return collapseTicks;}
    public int objectiveProgress(){return objectiveProgress;}
    public boolean armoryInstalled(){return armoryInstalled;}
    public void markArmoryInstalled(){armoryInstalled=true;setDirty();}
    public int ruinCursor(){return ruinCursor;}
    public void advanceRuins(int count){ruinCursor+=count;setDirty();}
    public void advanceObjective(){objectiveProgress++;setDirty();}

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("Built",    built);
        tag.putBoolean("Expanded", expanded);
        tag.putInt("InvasionStage", invasionStage);
        tag.putBoolean("Destroyed", destroyed);
        tag.putInt("DefenseStageSpawned", defenseStageSpawned);
        tag.putInt("LayoutVersion",layoutVersion);
        tag.putBoolean("VisualUpgrade",visualUpgrade);
        tag.putInt("VisualUpgradeCursor",visualUpgradeCursor);
        tag.putInt("CollapseTicks",collapseTicks);
        tag.putInt("ObjectiveProgress",objectiveProgress);
        tag.putBoolean("ArmoryInstalled",armoryInstalled);
        tag.putInt("RuinCursor",ruinCursor);
        return tag;
    }
}

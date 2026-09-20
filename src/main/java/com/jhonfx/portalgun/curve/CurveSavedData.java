package com.jhonfx.portalgun.curve;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class CurveSavedData extends SavedData {
    private static final String NAME="portalgun_curve_progress";
    private int neuralData,fluid;
    private boolean breaking,battleStarted,broken;
    private long finishTime;
    private int collapseTicks;
    public static CurveSavedData get(ServerLevel level) { return level.getServer().overworld().getDataStorage().computeIfAbsent(CurveSavedData::load,CurveSavedData::new,NAME); }
    public static CurveSavedData load(CompoundTag tag) { CurveSavedData d=new CurveSavedData(); d.neuralData=tag.getInt("NeuralData");d.fluid=tag.getInt("Fluid");d.breaking=tag.getBoolean("Breaking");d.battleStarted=tag.getBoolean("BattleStarted");d.broken=tag.getBoolean("Broken");d.finishTime=tag.getLong("FinishTime");d.collapseTicks=tag.getInt("CollapseTicks");return d; }
    public int neuralData(){return neuralData;} public int fluid(){return fluid;} public boolean breaking(){return breaking;} public boolean battleStarted(){return battleStarted;} public boolean broken(){return broken;} public long finishTime(){return finishTime;} public int collapseTicks(){return collapseTicks;}
    public int addNeuralData(int amount){neuralData=Math.min(100,neuralData+amount);setDirty();return neuralData;}
    public int addFluid(int amount){fluid=Math.min(100,fluid+amount);setDirty();return fluid;}
    public void begin(long finish){breaking=true;battleStarted=false;finishTime=finish;setDirty();}
    public void startBattle(){breaking=false;battleStarted=true;setDirty();}
    public void complete(){breaking=false;battleStarted=false;broken=true;collapseTicks=240;setDirty();}
    public int advanceCollapse(){if(collapseTicks>0){collapseTicks--;setDirty();}return collapseTicks;}
    @Override public CompoundTag save(CompoundTag tag){tag.putInt("NeuralData",neuralData);tag.putInt("Fluid",fluid);tag.putBoolean("Breaking",breaking);tag.putBoolean("BattleStarted",battleStarted);tag.putBoolean("Broken",broken);tag.putLong("FinishTime",finishTime);tag.putInt("CollapseTicks",collapseTicks);return tag;}
}

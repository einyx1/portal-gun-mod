package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.citadel.CitadelMissions;
import com.jhonfx.portalgun.network.CitadelActionPacket;
import com.jhonfx.portalgun.network.CitadelScreenPacket;
import com.jhonfx.portalgun.network.ModNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class CitadelServiceScreen extends Screen {
    private CitadelScreenPacket data;
    private int panelWidth() { return Math.min(380,width-16); }
    private int panelHeight() { return Math.min(252,height-8); }
    private int columnWidth() { return (panelWidth()-32)/2; }
    public CitadelServiceScreen(CitadelScreenPacket data) { super(Component.translatable("screen.portalgun.citadel_terminal")); this.data=data; }
    public void update(CitadelScreenPacket next) { this.data=next; rebuildWidgets(); }
    @Override protected void init() {
        int x=(width-panelWidth())/2+12,y=(height-panelHeight())/2+54;
        int second=x+columnWidth()+8;
        switch(data.screen()) {
            case "SHOP" -> {
                addSmall(x,y,"screen.portalgun.buy_green", "BUY_GREEN"); addSmall(second,y,"screen.portalgun.buy_blue","BUY_BLUE");
                addSmall(x,y+24,"screen.portalgun.buy_yellow","BUY_YELLOW"); addSmall(second,y+24,"screen.portalgun.buy_scanner","BUY_SCANNER");
                addSmall(x,y+48,"screen.portalgun.buy_isotope","BUY_ISOTOPE"); addSmall(second,y+48,"screen.portalgun.buy_meeseeks","BUY_MEESEEKS");
                addSmall(x,y+72,"screen.portalgun.buy_shield","BUY_SHIELD"); addSmall(second,y+72,"screen.portalgun.buy_jetpack","BUY_JETPACK");
                addSmall(x,y+96,"screen.portalgun.buy_blaster","BUY_BLASTER");
                addSmall(second,y+96,"screen.portalgun.buy_cannon","BUY_CANNON");
            }
            case "WORKSHOP" -> {
                add(x,y,"screen.portalgun.recharge","RECHARGE");
                add(x,y+26,"screen.portalgun.repair_equipment","REPAIR_EQUIPMENT");
                add(x,y+52,"screen.portalgun.buy_cells","WORKSHOP_CELLS");
            }
            case "SCANNER" -> {
                add(x,y+12,"screen.portalgun.analyze_c524","ANALYZE_C524");
                add(x,y+38,"screen.portalgun.analyze_brain","ANALYZE_BRAIN");
                add(x,y+64,"screen.portalgun.analyze_p0","ANALYZE_P0");
                add(x,y+90,"screen.portalgun.analyze_residue","ANALYZE_RESIDUE");
            }
            case "DRIVE" -> add(x,y+42,"screen.portalgun.feed_drive","FEED_DRIVE");
            case "CURVE" -> add(x,y+42,"screen.portalgun.break_curve","BREAK_CURVE");
            case "ARCHIVE" -> { }
            case "MISSION" -> missionButtons(x,y);
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),b -> onClose()).bounds(width/2-45,(height+panelHeight())/2-28,90,20).build());
    }
    private void missionButtons(int x,int y) {
        if(data.mission()==0) {
            addSmall(x,y,"mission.portalgun.zombie","MISSION_ZOMBIE"); addSmall(x+108,y,"mission.portalgun.skeleton","MISSION_SKELETON");
            addSmall(x,y+24,"mission.portalgun.spider","MISSION_SPIDER"); addSmall(x+108,y+24,"mission.portalgun.enderman","MISSION_ENDERMAN");
                addSmall(x,y+48,"mission.portalgun.redstone","MISSION_REDSTONE"); addSmall(x+108,y+48,"mission.portalgun.nether","MISSION_NETHER");
                addSmall(x,y+72,"mission.portalgun.districts","MISSION_DISTRICTS"); addSmall(x+108,y+72,"mission.portalgun.federation_raid","MISSION_FEDERATION");
                addSmall(x,y+96,"mission.portalgun.federation_salvage","MISSION_SALVAGE"); addSmall(x+108,y+96,"mission.portalgun.flight_test","MISSION_FLIGHT");
        }
        else if(data.progress()>=data.target()) add(x,y+40,"screen.portalgun.claim","MISSION_CLAIM");
    }
    private void add(int x,int y,String key,String action) { addRenderableWidget(Button.builder(Component.translatable(key),b -> ModNetwork.CHANNEL.sendToServer(new CitadelActionPacket(action))).bounds(x,y,panelWidth()-24,20).build()); }
    private void addSmall(int x,int y,String key,String action) {
        int left=(width-panelWidth())/2+12;
        if(x>left)x=left+columnWidth()+8;
        addRenderableWidget(Button.builder(Component.translatable(key),b -> ModNetwork.CHANNEL.sendToServer(new CitadelActionPacket(action))).bounds(x,y,columnWidth(),20).build());
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial) {
        renderBackground(graphics); int x=(width-panelWidth())/2,y=(height-panelHeight())/2;
        graphics.fill(x,y,x+panelWidth(),y+panelHeight(),0xF0142022); graphics.fill(x,y,x+panelWidth(),y+3,0xFF38F2AF);
        graphics.drawCenteredString(font,Component.translatable("screen.portalgun.service."+data.screen().toLowerCase(java.util.Locale.ROOT)),width/2,y+12,0x7DFFD1);
        graphics.drawString(font,Component.translatable("screen.portalgun.credits",data.credits()),x+12,y+30,0xF5D76E,false);
        if(data.screen().equals("MISSION")&&data.mission()>0) {
            CitadelMissions.Mission m=CitadelMissions.Mission.values()[Math.min(data.mission(),CitadelMissions.Mission.values().length-1)];
            graphics.drawString(font,Component.translatable(m.key),x+12,y+46,0xFFFFFF,false);
            graphics.drawString(font,Component.translatable("screen.portalgun.mission_status",data.progress(),data.target(),data.reward()),x+12,y+60,0xA8F8D6,false);
        }
        if(data.screen().equals("SCANNER")||data.screen().equals("DRIVE")||data.screen().equals("CURVE")||data.screen().equals("ARCHIVE")) {
            graphics.drawString(font,Component.translatable("screen.portalgun.curve_data",data.progress()),x+12,y+174,0x55FFBB,false);
            graphics.drawString(font,Component.translatable("screen.portalgun.drive_fluid",data.target()),x+12,y+187,0x44DDEE,false);
            if(data.mission()==1)graphics.drawString(font,Component.translatable("screen.portalgun.curve_breaking"),x+12,y+200,0xFF55FF,false);
            if(data.mission()==2)graphics.drawString(font,Component.translatable("screen.portalgun.curve_broken"),x+12,y+200,0xAAFF55,false);
        }
        super.render(graphics,mouseX,mouseY,partial);
    }
    @Override public boolean isPauseScreen() { return false; }
}

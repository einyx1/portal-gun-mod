package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.item.PortalGunItem;
import com.jhonfx.portalgun.item.PortalGunVariant;
import com.jhonfx.portalgun.item.PortalMode;
import com.jhonfx.portalgun.network.ModNetwork;
import com.jhonfx.portalgun.network.PortalGunConfigPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Compact, paged recreation of the Bedrock Portal Gun interface. */
public class PortalGunMenuScreen extends Screen {
    private enum Page { MAIN, COORDINATES, LOCATOR, SETTINGS, WAYPOINTS }

    private final InteractionHand hand;
    private final ItemStack gunStack;
    private final PortalGunVariant variant;
    private PortalMode mode;
    private float portalScale;
    private int duration;
    private int airRange;
    private Page page = Page.MAIN;
    private int waypointPage;
    private EditBox xField, yField, zField, dimensionField, structureField, terminalField, waypointNameField;
    private Button modeButton, scaleButton, durationButton;
    private boolean locateBiome;
    private int commandSuggestionIndex;
    private List<String> registrySuggestions = List.of();
    private String suggestionQuery = "";
    private int suggestionKind = -1, suggestionTick, suggestionIndex;

    private EditBox navigationField() {
        if (dimensionField != null && dimensionField.isFocused() && !variant.isPrototype()) return dimensionField;
        if (structureField != null && structureField.isFocused()) return structureField;
        return null;
    }

    @Override public void tick() {
        super.tick();
        EditBox field = navigationField();
        if (field == null) { registrySuggestions = List.of(); suggestionKind = -1; return; }
        if (++suggestionTick % 8 != 0) return;
        int kind = field == dimensionField ? 0 : locateBiome && page == Page.LOCATOR ? 2 : 1;
        String query = field.getValue();
        if (query.length() > 128 || (kind == suggestionKind && query.equals(suggestionQuery))) return;
        suggestionKind = kind; suggestionQuery = query; suggestionIndex = 0; registrySuggestions = List.of();
        ModNetwork.CHANNEL.sendToServer(new com.jhonfx.portalgun.network.NavigationSuggestionsPacket(kind, query));
    }

    public static void receiveSuggestions(int kind, String query, List<String> matches) {
        if (Minecraft.getInstance().screen instanceof PortalGunMenuScreen screen
                && screen.suggestionKind == kind && screen.suggestionQuery.equals(query))
            screen.registrySuggestions = matches;
    }
    private static final String[] COMMANDS={"/coords","/save ","/showlocs","/delloc ","/use ","/history 1","/pressure on","/pressure off","/safety on","/safety off","/autoclose on","/autoclose off","/singleuse on","/singleuse off","/gunconfig","/reset","/help"};
    private int panelX, panelY, panelW, panelH;

    public PortalGunMenuScreen(InteractionHand hand) {
        super(Component.translatable("screen.portalgun.title"));
        this.hand = hand;
        this.gunStack = Minecraft.getInstance().player.getItemInHand(hand);
        this.variant = ((PortalGunItem) gunStack.getItem()).variant();
        this.mode = PortalGunItem.getMode(gunStack);
        this.portalScale = PortalGunItem.getPortalScale(gunStack);
        this.duration = PortalGunItem.getPortalDuration(gunStack);
        this.airRange = PortalGunItem.getAirRange(gunStack);
    }

    @Override
    protected void init() {
        panelW = Math.min(360, width - 12);
        panelH = Math.min(226, height - 12);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        xField = yField = zField = dimensionField = structureField = terminalField = waypointNameField = null;
        switch (page) {
            case MAIN -> initMain();
            case COORDINATES -> initCoordinates();
            case LOCATOR -> initLocator();
            case SETTINGS -> initSettings();
            case WAYPOINTS -> initWaypoints();
        }
    }

    private void initMain() {
        int x = width / 2 - 106, y = height / 2 - 57;
        addRenderableWidget(Button.builder(Component.translatable("screen.portalgun.waypoints"),
                b -> showPage(Page.WAYPOINTS)).bounds(x + 6, y + 8, 82, 42).build()).active=variant.supportsSavedDestinations();
        addRenderableWidget(Button.builder(Component.translatable("screen.portalgun.set_coordinates"),
                b -> showPage(Page.COORDINATES)).bounds(x + 96, y + 8, 110, 24).build()).active=variant.supportsSavedDestinations();
        addRenderableWidget(Button.builder(Component.literal("Localizar bioma/estrutura"),
                b -> showPage(Page.LOCATOR)).bounds(x + 96, y + 36, 110, 24).build()).active=variant.supportsSavedDestinations();
        modeButton = addRenderableWidget(Button.builder(modeLabel(), b -> {
            mode = mode.next(); b.setMessage(modeLabel()); send(PortalGunConfigPacket.Action.UPDATE);
        }).bounds(x + 6, y + 56, 200, 24).build());
        addRenderableWidget(Button.builder(Component.literal("⚙"), b -> showPage(Page.SETTINGS))
                .bounds(x + 6, y + 84, 34, 24).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(x + 44, y + 84, 162, 24).build());
        addRenderableWidget(Button.builder(Component.literal("▣"), b -> send(PortalGunConfigPacket.Action.UNPLUG_TUBE))
                .bounds(x + 218, y - 14, 28, 24).build());
        addRenderableWidget(Button.builder(Component.literal("×"), b -> onClose())
                .bounds(x + 218, y + 14, 28, 24).build());
    }

    private void initLocator() {
        int x=panelX+12,y=panelY+34,inner=panelW-24;addBackButton();
        addRenderableWidget(Button.builder(Component.literal(locateBiome?"BIOMAS":"ESTRUTURAS"),b->{locateBiome=!locateBiome;refreshPage();}).bounds(x,y,inner,22).build());
        structureField=field(x,y+34,inner,"minecraft:village_plains",locateBiome?"minecraft:plains":"minecraft:village_plains");
        addRenderableWidget(Button.builder(Component.literal("Buscar e definir destino seguro"),b->send(locateBiome?PortalGunConfigPacket.Action.LOCATE_BIOME:PortalGunConfigPacket.Action.LOCATE_STRUCTURE)).bounds(x,y+64,inner,24).build());
        String[] suggestions=locateBiome?new String[]{"minecraft:plains","minecraft:desert","minecraft:badlands","minecraft:deep_dark"}
                :new String[]{"minecraft:village_plains","minecraft:stronghold","minecraft:ancient_city","minecraft:woodland_mansion"};
        for(int i=0;i<suggestions.length;i++){String value=suggestions[i];addRenderableWidget(Button.builder(Component.literal(value.replace("minecraft:","")),b->structureField.setValue(value)).bounds(x+(i%2)*(inner/2+3),y+100+(i/2)*28,inner/2-3,22).build());}
    }

    private void initCoordinates() {
        int x = panelX + 12, y = panelY + 30, inner = panelW - 24;
        addBackButton();
        int third = (inner - 12) / 3;
        if (variant == com.jhonfx.portalgun.item.PortalGunVariant.BLUE_PROTOTYPE) {
            // Azul: coordenadas locais apenas. A progressão para a Cidadela começa no protótipo verde.
            xField = field(x, y, third, "X", destinationValue("X", minecraft.player.getX()));
            yField = field(x + third + 6, y, third, "Y", destinationValue("Y", minecraft.player.getY()));
            zField = field(x + (third + 6) * 2, y, inner - (third + 6) * 2, "Z", destinationValue("Z", minecraft.player.getZ()));
            String localDimension=minecraft.level.dimension().location().toString();
            dimensionField = field(x, y + 28, inner, localDimension, localDimension);
            dimensionField.setEditable(false);
            addRenderableWidget(Button.builder(Component.translatable("screen.portalgun.save_destination"), b -> {
                mode = com.jhonfx.portalgun.item.PortalMode.CUSTOM; send(PortalGunConfigPacket.Action.UPDATE);
            }).bounds(x, y + 56, inner, 22).build());
        } else {
            xField = field(x, y, third, "X", destinationValue("X", minecraft.player.getX()));
            yField = field(x + third + 6, y, third, "Y", destinationValue("Y", minecraft.player.getY()));
            zField = field(x + (third + 6) * 2, y, inner - (third + 6) * 2, "Z", destinationValue("Z", minecraft.player.getZ()));
            dimensionField = field(x, y + 28, inner, "minecraft:overworld", destinationDimension());
            String[] dimensions=variant == com.jhonfx.portalgun.item.PortalGunVariant.PROTOTYPE
                    ? new String[]{"minecraft:overworld","portalgun:citadel"}
                    : new String[]{"minecraft:overworld","minecraft:the_nether","minecraft:the_end","portalgun:citadel","portalgun:federation"};
            int buttonWidth=inner/dimensions.length;
            for(int i=0;i<dimensions.length;i++){String dimension=dimensions[i];addRenderableWidget(Button.builder(Component.literal(dimension.replace("minecraft:","").replace("portalgun:","")),b->{dimensionField.setValue(dimension);if(dimension.equals("portalgun:federation")){xField.setValue("0");yField.setValue("97");zField.setValue("0");}if(dimension.equals("portalgun:citadel")){xField.setValue("0");yField.setValue("97");zField.setValue("10");}}).bounds(x+i*buttonWidth,y+56,buttonWidth-3,18).build());}
            addRenderableWidget(Button.builder(Component.translatable("screen.portalgun.save_destination"), b -> {
                mode = PortalMode.CUSTOM; send(PortalGunConfigPacket.Action.UPDATE);
            }).bounds(x, y + 80, inner, 22).build());
        }
        if (variant.supportsStructureSearch()) {
            structureField = field(x, y + 116, inner - 86, "minecraft:village_plains",
                    gunStack.getOrCreateTag().getString(PortalGunItem.TAG_STRUCTURE).isBlank()
                            ? "minecraft:village_plains" : gunStack.getTag().getString(PortalGunItem.TAG_STRUCTURE));
            addRenderableWidget(Button.builder(Component.translatable("screen.portalgun.locate"), b ->
                    send(PortalGunConfigPacket.Action.LOCATE_STRUCTURE)).bounds(x + inner - 80, y + 116, 80, 22).build());
            addRenderableWidget(Button.builder(Component.translatable("screen.portalgun.waypoints"),
                    b -> showPage(Page.WAYPOINTS)).bounds(x, y + 148, inner / 2 - 3, 22).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                    .bounds(x + inner / 2 + 3, y + 148, inner / 2 - 3, 22).build());
        } else {
            // Azul: apenas coordenadas locais.
            addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                    .bounds(x, y + 120, inner, 22).build());
        }
    }

    private void initSettings() {
        int x = panelX + 12, y = panelY + 30, inner = panelW - 24;
        addBackButton();
        modeButton = addRenderableWidget(Button.builder(modeLabel(), b -> {
            mode = mode.next(); b.setMessage(modeLabel()); send(PortalGunConfigPacket.Action.UPDATE);
        }).bounds(x, y, inner, 22).build());
        scaleButton = addRenderableWidget(Button.builder(scaleLabel(), b -> {
            portalScale += 0.5f; if (portalScale > 4.0f) portalScale = 0.5f;
            b.setMessage(scaleLabel()); send(PortalGunConfigPacket.Action.UPDATE);
        }).bounds(x, y + 28, inner / 2 - 3, 22).build());
        durationButton = addRenderableWidget(Button.builder(durationLabel(), b -> {
            int seconds = duration / 20;
            seconds = seconds < 10 ? 10 : seconds < 20 ? 20 : seconds < 30 ? 30 : seconds < 45 ? 45 : seconds < 60 ? 60 : 3;
            duration = seconds * 20; b.setMessage(durationLabel()); send(PortalGunConfigPacket.Action.UPDATE);
        }).bounds(x + inner / 2 + 3, y + 28, inner / 2 - 3, 22).build());
        addRenderableWidget(Button.builder(airRangeLabel(), b -> {
            airRange = airRange < 16 ? 16 : airRange < 32 ? 32 : airRange < 48 ? 48 : airRange < 64 ? 64
                    : airRange < 96 ? 96 : airRange < 128 ? 128 : airRange < 192 ? 192 : airRange < 256 ? 256 : 8;
            b.setMessage(airRangeLabel()); send(PortalGunConfigPacket.Action.UPDATE);
        }).bounds(x, y + 56, inner, 22).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.portalgun.plug_tube"), b ->
                send(PortalGunConfigPacket.Action.PLUG_TUBE)).bounds(x, y + 84, inner / 2 - 3, 22).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.portalgun.unplug_tube"), b ->
                send(PortalGunConfigPacket.Action.UNPLUG_TUBE)).bounds(x + inner / 2 + 3, y + 84, inner / 2 - 3, 22).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.portalgun.close_all"), b ->
                send(PortalGunConfigPacket.Action.CLOSE_PORTALS)).bounds(x, y + 112, inner, 22).build());
        terminalField = field(x, y + 150, inner - 48, "save casa | showlocs | help", "");
        terminalField.setResponder(value->commandSuggestionIndex=0);
        addRenderableWidget(Button.builder(Component.literal(">"), b -> send(PortalGunConfigPacket.Action.TERMINAL))
                .bounds(x + inner - 42, y + 150, 42, 22).build());
        String[] suggestions={"showlocs","safety on","autoclose on","singleuse on"};
        for(int i=0;i<suggestions.length;i++){String command=suggestions[i];addRenderableWidget(Button.builder(Component.literal(command),b->terminalField.setValue(command)).bounds(x+i*(inner/4),y+178,inner/4-3,18).build());}
    }

    private void initWaypoints() {
        int x = panelX + 10, y = panelY + 28, inner = panelW - 20;
        addBackButton();
        waypointNameField = field(x, y, inner - 104, "Nome do destino", "");
        addRenderableWidget(Button.builder(Component.translatable("screen.portalgun.save_current"), b -> {
            if (!waypointNameField.getValue().isBlank()) sendCommand("save " + waypointNameField.getValue().trim());
        }).bounds(x + inner - 98, y, 98, 22).build());
        List<CompoundTag> entries = savedLocations();
        int start = waypointPage * 5;
        for (int row = 0; row < 5 && start + row < entries.size(); row++) {
            CompoundTag entry = entries.get(start + row);
            String name = entry.getString("Name");
            int rowY = y + 30 + row * 27;
            String label = trim(name, 18) + "  " + (int) entry.getDouble("X") + ", " + (int) entry.getDouble("Z");
            addRenderableWidget(Button.builder(Component.literal(label), b -> selectDestination(name))
                    .bounds(x, rowY, inner - 42, 22).build());
            addRenderableWidget(Button.builder(Component.literal("×"), b -> sendCommand("delloc " + name))
                    .bounds(x + inner - 36, rowY, 36, 22).build());
        }
        int pages = Math.max(1, (entries.size() + 4) / 5);
        addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            waypointPage = Math.max(0, waypointPage - 1); refreshPage();
        }).bounds(x, panelY + panelH - 26, 36, 20).build());
        addRenderableWidget(Button.builder(Component.literal((waypointPage + 1) + "/" + pages), b -> {})
                .bounds(x + 42, panelY + panelH - 26, inner - 84, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            waypointPage = Math.min(pages - 1, waypointPage + 1); refreshPage();
        }).bounds(x + inner - 36, panelY + panelH - 26, 36, 20).build());
    }

    private void addBackButton() {
        addRenderableWidget(Button.builder(Component.literal("<"), b -> showPage(Page.MAIN))
                .bounds(panelX + 6, panelY + 5, 28, 20).build());
    }

    private void showPage(Page next) { page = next; refreshPage(); }
    private void refreshPage() { clearWidgets(); init(); }

    private EditBox field(int x, int y, int w, String hint, String value) {
        EditBox field = new EditBox(font, x, y, w, 22, Component.literal(hint));
        field.setHint(Component.literal(hint)); field.setValue(value); addRenderableWidget(field); return field;
    }

    public void acceptLocatedStructure(double x, double y, double z, String dimension, String structure) {
        if (xField != null) xField.setValue(Integer.toString((int) x));
        if (yField != null) yField.setValue(Integer.toString((int) y));
        if (zField != null) zField.setValue(Integer.toString((int) z));
        if (dimensionField != null) dimensionField.setValue(dimension);
        if (structureField != null) structureField.setValue(structure);
        mode = PortalMode.CUSTOM;
        if (modeButton != null) modeButton.setMessage(modeLabel());
    }

    private Component modeLabel() { return Component.translatable("screen.portalgun.mode", mode.displayName()); }
    private Component scaleLabel() { return Component.translatable("screen.portalgun.scale", String.format("%.1fx", portalScale)); }
    private Component durationLabel() { return Component.translatable("screen.portalgun.duration", duration / 20); }
    private Component airRangeLabel() { return Component.translatable("screen.portalgun.air_range", airRange); }

    private void send(PortalGunConfigPacket.Action action) {
        String payload = action == PortalGunConfigPacket.Action.TERMINAL
                ? (terminalField == null ? "" : terminalField.getValue())
                : (structureField == null ? "" : structureField.getValue());
        sendPayload(action, payload);
    }

    private void sendCommand(String command) { sendPayload(PortalGunConfigPacket.Action.TERMINAL, command); }

    private void selectDestination(String name) {
        mode = PortalMode.CUSTOM;
        sendCommand("use " + name);
        showPage(Page.MAIN);
    }

    private void sendPayload(PortalGunConfigPacket.Action action, String payload) {
        ModNetwork.CHANNEL.sendToServer(new PortalGunConfigPacket(hand, action, mode.name(), parse(xField),
                parse(yField), parse(zField), dimensionField == null ? "" : dimensionField.getValue(),
                portalScale, duration, airRange, payload == null ? "" : payload));
    }

    private double parse(EditBox field) {
        if (field == null) return 0;
        try { return Double.parseDouble(field.getValue().trim()); } catch (NumberFormatException ignored) { return 0; }
    }

    private String destinationValue(String key, double fallback) {
        CompoundTag destination = gunStack.getOrCreateTag().getCompound(PortalGunItem.TAG_DESTINATION);
        return destination.contains(key) ? Integer.toString((int) destination.getDouble(key)) : Integer.toString((int) fallback);
    }

    private String destinationDimension() {
        CompoundTag destination = gunStack.getOrCreateTag().getCompound(PortalGunItem.TAG_DESTINATION);
        return destination.contains("Dimension") ? destination.getString("Dimension") : minecraft.level.dimension().location().toString();
    }

    private List<CompoundTag> savedLocations() {
        List<CompoundTag> result = new ArrayList<>();
        ListTag list = gunStack.getOrCreateTag().getList(PortalGunItem.TAG_SAVED_LOCATIONS, Tag.TAG_COMPOUND);
        for (Tag tag : list) result.add((CompoundTag) tag);
        return result;
    }

    private String trim(String text, int max) { return text.length() <= max ? text : text.substring(0, max - 1) + "…"; }
    private List<String> commandMatches(){
        if(terminalField==null)return List.of();String typed=terminalField.getValue().toLowerCase();if(typed.isBlank())return List.of(COMMANDS);
        String normalized=typed.startsWith("/")?typed:"/"+typed;List<String> matches=new ArrayList<>();for(String command:COMMANDS)if(command.startsWith(normalized))matches.add(command);return matches;
    }
    @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers){
        EditBox field = navigationField();
        if (field != null && !registrySuggestions.isEmpty()) {
            if (keyCode == 264 || keyCode == 265) {
                suggestionIndex = Math.floorMod(suggestionIndex + (keyCode == 264 ? 1 : -1), registrySuggestions.size());
                return true;
            }
            if (keyCode == 258) {
                field.setValue(registrySuggestions.get(suggestionIndex));
                field.moveCursorToEnd(); registrySuggestions = List.of(); return true;
            }
        }
        if(keyCode==258&&terminalField!=null&&terminalField.isFocused()){
            List<String> matches=commandMatches();if(!matches.isEmpty()){terminalField.setValue(matches.get(commandSuggestionIndex++%matches.size()));terminalField.moveCursorToEnd();return true;}
        }return super.keyPressed(keyCode,scanCode,modifiers);
    }
    @Override public void onClose() { send(PortalGunConfigPacket.Action.UPDATE); super.onClose(); }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        if (page == Page.MAIN) renderOriginalHud(g); else renderCompactPanel(g);
        super.render(g, mouseX, mouseY, partialTick);
        EditBox field = navigationField();
        if (field != null && !registrySuggestions.isEmpty()) {
            int x = field.getX(), y = field.getY() + field.getHeight() + 2;
            g.pose().pushPose(); g.pose().translate(0, 0, 300);
            g.fill(x, y, x + field.getWidth(), y + 36, 0xF5101818);
            for (int i = 0; i < Math.min(3, registrySuggestions.size()); i++) {
                int index = (suggestionIndex + i) % registrySuggestions.size();
                g.drawString(font, font.plainSubstrByWidth(registrySuggestions.get(index), field.getWidth() - 8),
                        x + 3, y + 3 + i * 11, i == 0 ? 0xFF73FFA0 : 0xFFBBCDC0, false);
            }
            g.pose().popPose();
        }
    }

    private void renderOriginalHud(GuiGraphics g) {
        int imageH = Math.min(340, height - 8), imageW = imageH / 2;
        int imageX = (width - imageW) / 2, imageY = (height - imageH) / 2;
        g.blit(menuBackground(), imageX, imageY, imageW, imageH, 0, 0, sourceWidth(), sourceHeight(), sourceWidth(), sourceHeight());
        int x = width / 2 - 110, y = height / 2 - 52, accent = accent();
        if (variant == PortalGunVariant.EVIL_MORTY)
            g.blit(new ResourceLocation("portalgun", "textures/gui/addon_menu/holo_panel.png"), x - 12, y - 12, 244, 140, 0, 0, 128, 128, 128, 128);
        g.fill(x, y, x + 220, y + 104, 0xD80A0D10);
        g.fill(x, y, x + 220, y + 2, 0xFF000000 | accent);
        int percent = Math.round(100.0f * PortalGunItem.getCharge(gunStack) / 1000.0f);
        g.drawString(font, Component.translatable("screen.portalgun.charge", percent), x + 98, y + 91, 0xFFFFFFFF);
    }

    private void renderCompactPanel(GuiGraphics g) {
        int accent = accent();
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF20A0D10);
        g.fill(panelX, panelY, panelX + panelW, panelY + 3, 0xFF000000 | accent);
        g.drawCenteredString(font, pageTitle(), width / 2, panelY + 9, 0xFF000000 | accent);
        if (page == Page.COORDINATES) {
            g.drawString(font, Component.translatable("screen.portalgun.coordinates"), panelX + 12, panelY + 21, 0xFFFFFFFF);
            g.drawString(font, Component.translatable("screen.portalgun.structure"), panelX + 12, panelY + 112, 0xFFFFFFFF);
        } else if (page == Page.LOCATOR) {
            g.drawString(font, "Busca no servidor e escolhe um piso seguro", panelX + 12, panelY + 22, 0xFFB9D5C2, false);
        } else if (page == Page.SETTINGS) {
            g.drawString(font, Component.translatable("screen.portalgun.terminal"), panelX + 12, panelY + 170, 0xFFFFFFFF);
            List<String> matches=commandMatches();int shown=Math.min(3,matches.size());for(int i=0;i<shown;i++)g.drawString(font,matches.get(i),panelX+14,panelY+204+i*10,0xFF78E89B,false);
        }
    }

    private Component pageTitle() {
        return switch (page) {
            case MAIN -> variantTitle();
            case COORDINATES -> Component.translatable("screen.portalgun.set_coordinates");
            case LOCATOR -> Component.literal("LOCALIZADOR DIMENSIONAL");
            case SETTINGS -> Component.translatable("screen.portalgun.settings");
            case WAYPOINTS -> Component.translatable("screen.portalgun.waypoints");
        };
    }

    private int accent() {
        return switch (variant) {
            case STANDARD -> 0x25FF68; case PROTOTYPE -> 0x18F5CF; case BLUE_PROTOTYPE -> 0x258CFF;
            case EVIL_MORTY -> 0xFFD21A; case PRIME -> 0xFF3540;
        };
    }

    private ResourceLocation menuBackground() {
        boolean discharged = PortalGunItem.getLoadedColor(gunStack) == null;
        String name = switch (variant) {
            case STANDARD -> discharged ? "standard_pg_discharged" : "standard_pg";
            case PROTOTYPE -> discharged ? "prototype_pg_discharged" : "prototype_pg";
            case BLUE_PROTOTYPE -> discharged ? "prototype_pg_discharged" : "prototype_pg_blue";
            case EVIL_MORTY -> discharged ? "evil_morty_pg_discharged" : "evil_morty_pg";
            case PRIME -> discharged ? "prime_pg_discharged" : "prime_pg";
        };
        return new ResourceLocation("portalgun", "textures/gui/addon_menu/" + name + ".png");
    }

    private int sourceWidth() { return variant == PortalGunVariant.PROTOTYPE || variant == PortalGunVariant.BLUE_PROTOTYPE ? 200 : 170; }
    private int sourceHeight() { return variant == PortalGunVariant.PROTOTYPE || variant == PortalGunVariant.BLUE_PROTOTYPE ? 400 : 340; }
    private Component variantTitle() { return Component.translatable("screen.portalgun.variant." + variant.name().toLowerCase()); }
    @Override public boolean isPauseScreen() { return false; }
}

package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.entity.MeeseeksOrder;
import com.jhonfx.portalgun.network.MeeseeksCommandPacket;
import com.jhonfx.portalgun.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

/**
 * Caixa de Meeseeks — menu expandido com todas as 15 ordens em grade scrollável.
 * Layout: 3 colunas × 5 linhas visíveis, com barra de scroll se necessário.
 */
public final class MeeseeksMenuScreen extends Screen {
    private final InteractionHand hand;
    private int x, y;
    private static final int W = 420, H = 260;
    private static final int BTN_W = 130, BTN_H = 22, PAD = 4;
    private static final int COLS = 3;

    public MeeseeksMenuScreen(InteractionHand hand) {
        super(Component.literal("CAIXA DE MEESEEKS"));
        this.hand = hand;
    }

    @Override
    protected void init() {
        x = (width  - W) / 2;
        y = (height - H) / 2;

        MeeseeksOrder[] orders = MeeseeksOrder.values();
        for (int i = 0; i < orders.length; i++) {
            MeeseeksOrder order = orders[i];
            int col = i % COLS;
            int row = i / COLS;
            int bx  = x + 10 + col * (BTN_W + PAD);
            int by  = y + 48 + row * (BTN_H + PAD);

            // Highlight griefing orders differently (they respect config)
            String label = order.label();
            if (order.requiresGriefing()) label = "⛏ " + label;

            addRenderableWidget(Button.builder(Component.literal(label), b -> {
                ModNetwork.CHANNEL.sendToServer(new MeeseeksCommandPacket(hand, order));
                onClose();
            }).bounds(bx, by, BTN_W, BTN_H).build());
        }

        // Cancel
        addRenderableWidget(Button.builder(Component.literal("Cancelar"), b -> onClose())
                .bounds(x + W - 94, y + H - 28, 84, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        // Panel
        g.fill(x, y, x + W, y + H, 0xF0081720);
        // Top accent
        g.fill(x, y, x + W, y + 3, 0xFF32E6F2);
        // Bottom accent
        g.fill(x, y + H - 3, x + W, y + H, 0xFF32E6F2);
        // Left accent
        g.fill(x, y, x + 3, y + H, 0xFF32E6F2);
        // Right accent
        g.fill(x + W - 3, y, x + W, y + H, 0xFF32E6F2);

        // Title
        g.drawCenteredString(font, "[ MR. MEESEEKS — ORDEM ]", width / 2, y + 10, 0xFF65F7FF);
        g.drawCenteredString(font,
                "§7Múltiplos Meeseeks permitidos. ⛏ = requer permissão de griefing.",
                width / 2, y + 24, 0xFFA9DDE3);

        // Column headers
        String[] headers = { "MISSÃO", "COMBATE / UTILIDADE", "CONSTRUÇÃO / COLETA" };
        for (int c = 0; c < COLS; c++) {
            g.drawString(font, headers[c], x + 10 + c * (BTN_W + PAD), y + 38, 0xFF32E6F2, false);
        }

        super.render(g, mx, my, pt);
    }

    @Override public boolean isPauseScreen() { return false; }
}

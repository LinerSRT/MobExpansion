package net.mcskill.mobexpansion.client.screen.widget;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;


public final class GuiMarquee {
    private static final int GAP = 16;
    private static final int PAUSE_MS = 1000;
    private static final int MS_PER_PIXEL = 40;

    private GuiMarquee() {
    }

    public static void draw(GuiGraphics guiGraphics, Font font, Component text, int x, int y, int maxWidth, int color, boolean dropShadow) {
        if (maxWidth <= 0)
            return;
        final int textWidth = font.width(text);
        if (textWidth <= maxWidth) {
            guiGraphics.drawString(font, text, x, y, color, dropShadow);
            return;
        }
        final int loop = textWidth + GAP;
        final long cycle = PAUSE_MS + (long) loop * MS_PER_PIXEL;
        final long phase = Util.getMillis() % cycle;
        final int offset = phase < PAUSE_MS ? 0 : (int) Math.min(loop, (phase - PAUSE_MS) / MS_PER_PIXEL);
        guiGraphics.enableScissor(x, y - 1, x + maxWidth, y + 9);
        guiGraphics.drawString(font, text, x - offset, y, color, dropShadow);
        guiGraphics.drawString(font, text, x - offset + loop, y, color, dropShadow);
        guiGraphics.disableScissor();
    }
}

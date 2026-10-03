package net.mcskill.mobexpansion.client.screen.oreui;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.client.screen.widget.NineSlicePainter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class OreUIAtlas {
    public static final ResourceLocation TEXTURE = Core.loc("textures/gui/atlas.png");
    public static final int SIZE = 256;

    public static final int TOGGLE_WIDTH = 32;
    public static final int TOGGLE_HEIGHT = 16;

    public static final int CHECKBOX_SIZE = 16;

    public static final int BUTTON_SRC = 15;
    public static final int BUTTON_STRIDE = 15;
    public static final int BUTTON_SLICE = 5;

    public static final int SLIDER_THUMB_U = 32;
    public static final int SLIDER_THUMB_V = 0;
    public static final int SLIDER_THUMB_DISABLED_V = 16;
    public static final int SLIDER_THUMB_PRESSED_V = 32;
    public static final int SLIDER_THUMB_WIDTH = 16;
    public static final int SLIDER_THUMB_HEIGHT = 16;

    public static final int SLIDER_TRACK_U = 48;
    public static final int SLIDER_TRACK_FILLED_V = 0;
    public static final int SLIDER_TRACK_EMPTY_V = 6;
    public static final int SLIDER_TRACK_DISABLED_V = 12;
    public static final int SLIDER_TRACK_SRC_WIDTH = 16;
    public static final int SLIDER_TRACK_HEIGHT = 6;
    public static final int SLIDER_TRACK_SLICE = 2;

    public static final int EDIT_U = 0;
    public static final int EDIT_DISABLED_U = 16;
    public static final int EDIT_V = 96;
    public static final int EDIT_SRC = 16;
    public static final int EDIT_SLICE = 4;
    public static final int EDIT_HEIGHT = 16;

    private OreUIAtlas() {
    }

    public static void blit(GuiGraphics guiGraphics, int x, int y, int u, int v, int width, int height) {
        guiGraphics.blit(TEXTURE, x, y, width, height, (float) u, (float) v, width, height, SIZE, SIZE);
    }

    public static void blitNineSlice(GuiGraphics guiGraphics, int x, int y, int width, int height, int u, int v, int sourceWidth, int sourceHeight, int slice) {
        NineSlicePainter.blit(guiGraphics, TEXTURE, x, y, width, height, u, v, sourceWidth, sourceHeight, SIZE, SIZE, slice);
    }

    public static void blitSliderTrack(GuiGraphics guiGraphics, int x, int y, int width, int v) {
        if (width <= 0)
            return;
        NineSlicePainter.blit(
                guiGraphics,
                TEXTURE,
                x,
                y,
                width,
                SLIDER_TRACK_HEIGHT,
                SLIDER_TRACK_U,
                v,
                SLIDER_TRACK_SRC_WIDTH,
                SLIDER_TRACK_HEIGHT,
                SIZE,
                SIZE,
                SLIDER_TRACK_SLICE,
                0,
                SLIDER_TRACK_SLICE,
                0
        );
    }

    public static void blitSliderThumb(GuiGraphics guiGraphics, int x, int y, boolean enabled) {
        blitSliderThumb(guiGraphics, x, y, enabled, false);
    }

    public static void blitSliderThumb(GuiGraphics guiGraphics, int x, int y, boolean enabled, boolean pressed) {
        final int v;
        if (!enabled)
            v = SLIDER_THUMB_DISABLED_V;
        else if (pressed)
            v = SLIDER_THUMB_PRESSED_V;
        else
            v = SLIDER_THUMB_V;
        blit(guiGraphics, x, y, SLIDER_THUMB_U, v, SLIDER_THUMB_WIDTH, SLIDER_THUMB_HEIGHT);
    }

    public static void blitEditBox(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean enabled) {
        blitNineSlice(
                guiGraphics,
                x,
                y,
                width,
                height,
                enabled ? EDIT_U : EDIT_DISABLED_U,
                EDIT_V,
                EDIT_SRC,
                EDIT_SRC,
                EDIT_SLICE
        );
    }

    public static int toggleV(boolean on, boolean hovered, boolean enabled) {
        if (!enabled)
            return on ? 64 : 80;
        if (hovered)
            return on ? 32 : 48;
        return on ? 0 : 16;
    }

    public static int checkboxV(boolean checked, boolean enabled) {
        if (!enabled)
            return 32;
        return checked ? 0 : 16;
    }

    public static int buttonU(ButtonStyle style, boolean hovered, boolean pressed, boolean enabled) {
        final int state;
        if (!enabled)
            state = 3;
        else if (pressed)
            state = 2;
        else if (hovered)
            state = 1;
        else
            state = 0;
        return style.u + state * BUTTON_STRIDE;
    }

    public enum ButtonStyle {
        PRIMARY(0, 226, 15, 0xFFFFFFFF),
        SECONDARY(0, 241, 15, 0xFF1E1E1F),
        TERTIARY(0, 196, 15, 0xFFFFFFFF),
        DESTRUCTIVE(0, 211, 15, 0xFFFFFFFF);

        public final int u;
        public final int v;
        public final int sourceHeight;
        public final int textColor;

        ButtonStyle(int u, int v, int sourceHeight, int textColor) {
            this.u = u;
            this.v = v;
            this.sourceHeight = sourceHeight;
            this.textColor = textColor;
        }
    }
}

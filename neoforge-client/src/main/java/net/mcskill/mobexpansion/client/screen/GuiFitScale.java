package net.mcskill.mobexpansion.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Fits oversized GUIs into the current window by scaling around the screen center.
 * Mouse coordinates must be unscaled with {@link #unscaleX(double)} / {@link #unscaleY(double)}.
 */
public final class GuiFitScale {
    private static float scale = 1.0F;
    private static int screenWidth = 1;
    private static int screenHeight = 1;
    private static boolean poseActive;

    private GuiFitScale() {
    }

    public static float compute(int screenW, int screenH, int guiW, int guiH) {
        if (guiW <= 0 || guiH <= 0 || screenW <= 0 || screenH <= 0)
            return 1.0F;
        final int availW = Math.max(1, screenW - 8);
        final int availH = Math.max(1, screenH - 8);
        return Math.min(1.0F, Math.min(availW / (float) guiW, availH / (float) guiH));
    }

    public static void update(int screenW, int screenH, int guiW, int guiH) {
        screenWidth = Math.max(1, screenW);
        screenHeight = Math.max(1, screenH);
        scale = compute(screenWidth, screenHeight, guiW, guiH);
    }

    public static void push(GuiGraphics guiGraphics, int screenW, int screenH, int guiW, int guiH) {
        update(screenW, screenH, guiW, guiH);
        poseActive = true;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(screenWidth / 2.0F, screenHeight / 2.0F, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.pose().translate(-screenWidth / 2.0F, -screenHeight / 2.0F, 0.0F);
    }

    public static void pop(GuiGraphics guiGraphics) {
        if (poseActive) {
            guiGraphics.pose().popPose();
            poseActive = false;
        }
    }

    public static boolean isPoseActive() {
        return poseActive && scale != 1.0F;
    }

    public static int screenX(int x) {
        if (!isPoseActive())
            return x;
        return Math.round(screenWidth * 0.5F + (x - screenWidth * 0.5F) * scale);
    }

    public static int screenY(int y) {
        if (!isPoseActive())
            return y;
        return Math.round(screenHeight * 0.5F + (y - screenHeight * 0.5F) * scale);
    }

    public static double unscaleX(double mouseX) {
        if (scale == 1.0F)
            return mouseX;
        return (mouseX - screenWidth * 0.5D) / scale + screenWidth * 0.5D;
    }

    public static double unscaleY(double mouseY) {
        if (scale == 1.0F)
            return mouseY;
        return (mouseY - screenHeight * 0.5D) / scale + screenHeight * 0.5D;
    }

    public static double unscaleDelta(double delta) {
        return scale == 1.0F ? delta : delta / scale;
    }

    public static int unscaleMouseX(double mouseX) {
        return Mth.floor(unscaleX(mouseX));
    }

    public static int unscaleMouseY(double mouseY) {
        return Mth.floor(unscaleY(mouseY));
    }
}

package net.mcskill.mobexpansion.client.screen.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws Bedrock-style nine-slice textures (corners fixed, edges/center stretched).
 */
public final class NineSlicePainter {
    private NineSlicePainter() {
    }

    public static void blit(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int width, int height, int textureSize, int slice) {
        blit(guiGraphics, texture, x, y, width, height, textureSize, textureSize, slice, slice, slice, slice);
    }

    public static void blit(
            GuiGraphics guiGraphics,
            ResourceLocation texture,
            int x,
            int y,
            int width,
            int height,
            int u,
            int v,
            int sourceWidth,
            int sourceHeight,
            int textureWidth,
            int textureHeight,
            int slice
    ) {
        blit(guiGraphics, texture, x, y, width, height, u, v, sourceWidth, sourceHeight, textureWidth, textureHeight, slice, slice, slice, slice);
    }

    public static void blit(
            GuiGraphics guiGraphics,
            ResourceLocation texture,
            int x,
            int y,
            int width,
            int height,
            int u,
            int v,
            int sourceWidth,
            int sourceHeight,
            int textureWidth,
            int textureHeight,
            int left,
            int top,
            int right,
            int bottom
    ) {
        left = Mth.clamp(left, 0, sourceWidth);
        top = Mth.clamp(top, 0, sourceHeight);
        right = Mth.clamp(right, 0, sourceWidth - left);
        bottom = Mth.clamp(bottom, 0, sourceHeight - top);

        final int centerSourceWidth = Math.max(1, sourceWidth - left - right);
        final int centerSourceHeight = Math.max(1, sourceHeight - top - bottom);
        final int destCenterWidth = Math.max(0, width - left - right);
        final int destCenterHeight = Math.max(0, height - top - bottom);

        blitRegion(guiGraphics, texture, x, y, left, top, u, v, left, top, textureWidth, textureHeight);
        blitRegion(guiGraphics, texture, x + width - right, y, right, top, u + sourceWidth - right, v, right, top, textureWidth, textureHeight);
        blitRegion(guiGraphics, texture, x, y + height - bottom, left, bottom, u, v + sourceHeight - bottom, left, bottom, textureWidth, textureHeight);
        blitRegion(guiGraphics, texture, x + width - right, y + height - bottom, right, bottom, u + sourceWidth - right, v + sourceHeight - bottom, right, bottom, textureWidth, textureHeight);

        if (destCenterWidth > 0) {
            blitRegion(guiGraphics, texture, x + left, y, destCenterWidth, top, u + left, v, centerSourceWidth, top, textureWidth, textureHeight);
            blitRegion(guiGraphics, texture, x + left, y + height - bottom, destCenterWidth, bottom, u + left, v + sourceHeight - bottom, centerSourceWidth, bottom, textureWidth, textureHeight);
        }
        if (destCenterHeight > 0) {
            blitRegion(guiGraphics, texture, x, y + top, left, destCenterHeight, u, v + top, left, centerSourceHeight, textureWidth, textureHeight);
            blitRegion(guiGraphics, texture, x + width - right, y + top, right, destCenterHeight, u + sourceWidth - right, v + top, right, centerSourceHeight, textureWidth, textureHeight);
        }
        if (destCenterWidth > 0 && destCenterHeight > 0)
            blitRegion(guiGraphics, texture, x + left, y + top, destCenterWidth, destCenterHeight, u + left, v + top, centerSourceWidth, centerSourceHeight, textureWidth, textureHeight);
    }

    public static void blit(
            GuiGraphics guiGraphics,
            ResourceLocation texture,
            int x,
            int y,
            int width,
            int height,
            int textureWidth,
            int textureHeight,
            int left,
            int top,
            int right,
            int bottom
    ) {
        left = Mth.clamp(left, 0, textureWidth);
        top = Mth.clamp(top, 0, textureHeight);
        right = Mth.clamp(right, 0, textureWidth - left);
        bottom = Mth.clamp(bottom, 0, textureHeight - top);

        final int centerSourceWidth = Math.max(1, textureWidth - left - right);
        final int centerSourceHeight = Math.max(1, textureHeight - top - bottom);
        final int destCenterWidth = Math.max(0, width - left - right);
        final int destCenterHeight = Math.max(0, height - top - bottom);

        // Corners
        blitRegion(guiGraphics, texture, x, y, left, top, 0, 0, left, top, textureWidth, textureHeight);
        blitRegion(guiGraphics, texture, x + width - right, y, right, top, textureWidth - right, 0, right, top, textureWidth, textureHeight);
        blitRegion(guiGraphics, texture, x, y + height - bottom, left, bottom, 0, textureHeight - bottom, left, bottom, textureWidth, textureHeight);
        blitRegion(guiGraphics, texture, x + width - right, y + height - bottom, right, bottom, textureWidth - right, textureHeight - bottom, right, bottom, textureWidth, textureHeight);

        // Edges
        if (destCenterWidth > 0) {
            blitRegion(guiGraphics, texture, x + left, y, destCenterWidth, top, left, 0, centerSourceWidth, top, textureWidth, textureHeight);
            blitRegion(guiGraphics, texture, x + left, y + height - bottom, destCenterWidth, bottom, left, textureHeight - bottom, centerSourceWidth, bottom, textureWidth, textureHeight);
        }
        if (destCenterHeight > 0) {
            blitRegion(guiGraphics, texture, x, y + top, left, destCenterHeight, 0, top, left, centerSourceHeight, textureWidth, textureHeight);
            blitRegion(guiGraphics, texture, x + width - right, y + top, right, destCenterHeight, textureWidth - right, top, right, centerSourceHeight, textureWidth, textureHeight);
        }

        // Center
        if (destCenterWidth > 0 && destCenterHeight > 0)
            blitRegion(guiGraphics, texture, x + left, y + top, destCenterWidth, destCenterHeight, left, top, centerSourceWidth, centerSourceHeight, textureWidth, textureHeight);
    }

    private static void blitRegion(
            GuiGraphics guiGraphics,
            ResourceLocation texture,
            int x,
            int y,
            int width,
            int height,
            int u,
            int v,
            int regionWidth,
            int regionHeight,
            int textureWidth,
            int textureHeight
    ) {
        if (width <= 0 || height <= 0 || regionWidth <= 0 || regionHeight <= 0)
            return;
        guiGraphics.blit(texture, x, y, width, height, (float) u, (float) v, regionWidth, regionHeight, textureWidth, textureHeight);
    }
}

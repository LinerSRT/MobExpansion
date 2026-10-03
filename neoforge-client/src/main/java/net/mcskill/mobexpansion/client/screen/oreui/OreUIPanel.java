package net.mcskill.mobexpansion.client.screen.oreui;

import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

public class OreUIPanel {
    public static void renderDark(@NotNull GuiGraphics guiGraphics, int x, int y, int width, int height) {
        OreUIAtlas.blitNineSlice(guiGraphics, x, y, width, height, 224, 16, 16, 16, 4);
    }

    public static void renderLight(@NotNull GuiGraphics guiGraphics, int x, int y, int width, int height) {
        OreUIAtlas.blitNineSlice(guiGraphics, x, y, width, height, 224, 0, 16, 16, 4);
    }
}

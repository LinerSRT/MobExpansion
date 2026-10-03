package net.mcskill.mobexpansion.client.screen.oreui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class OreUITabButton extends OreUITextButton {
    private static final int INDICATOR_HEIGHT = 1;
    private static final int INDICATOR_INSET = 1;
    private static final int INDICATOR_COLOR = 0xFFFFFFFF;

    private final OreUITabs tabs;
    private final int index;

    OreUITabButton(OreUITabs tabs, int index, Component message) {
        super(0, 0, 0, tabs.tabHeight(), message, OreUIAtlas.ButtonStyle.TERTIARY, null);
        this.tabs = tabs;
        this.index = index;
    }

    public int index() {
        return this.index;
    }

    public OreUITabs tabs() {
        return this.tabs;
    }

    public boolean isSelected() {
        return this.tabs.selectedIndex() == this.index;
    }

    @Override
    protected boolean isVisuallyPressed() {
        return isSelected() || super.isVisuallyPressed();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (isSelected())
            return mouseButton == 0 && isActive() && isVisible() && isPointerOver(mouseX, mouseY);
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void onPress() {
        this.tabs.selectFromClick(this);
    }

    @Override
    protected void renderOreUI(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderOreUI(guiGraphics, mouseX, mouseY, partialTick);
        if (!isSelected())
            return;
        final int stripWidth = Math.max(8, getWidth() / 3);
        final int stripX = getX() + (getWidth() - stripWidth) / 2;
        final int stripY = getY() + getHeight() - INDICATOR_HEIGHT - INDICATOR_INSET;
        guiGraphics.fill(stripX, stripY, stripX + stripWidth, stripY + INDICATOR_HEIGHT, INDICATOR_COLOR);
    }
}

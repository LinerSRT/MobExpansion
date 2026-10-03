package net.mcskill.mobexpansion.client.screen.oreui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class OreUITextButton extends OreUIControl<OreUITextButton> {
    private OreUIAtlas.ButtonStyle style;
    private Consumer<OreUITextButton> onPressed;

    public OreUITextButton(int x, int y, Component message) {
        this(x, y, 0, 16, message, OreUIAtlas.ButtonStyle.SECONDARY, null);
    }

    public OreUITextButton(int x, int y, Component message, Consumer<OreUITextButton> onPressed) {
        this(x, y, 0, 16, message, OreUIAtlas.ButtonStyle.SECONDARY, onPressed);
    }

    public OreUITextButton(int x, int y, int width, int height, Component message, Consumer<OreUITextButton> onPressed) {
        this(x, y, width, height, message, OreUIAtlas.ButtonStyle.SECONDARY, onPressed);
    }

    public OreUITextButton(int x, int y, int width, int height, Component message, OreUIAtlas.ButtonStyle style, Consumer<OreUITextButton> onPressed) {
        super(x, y, width, height, message);
        this.style = style;
        this.onPressed = onPressed;
        textAlign(TextAlign.CENTER)
                .textPadding(4)
                .textOffset(0, -1)
                .textColor(style.textColor)
                .disabledTextColor(0xFF6A6A6A)
                .pressShiftsText(true)
                .packIfNeeded();
    }

    public OreUITextButton setOnPressed(Consumer<OreUITextButton> onPressed) {
        this.onPressed = onPressed;
        return this;
    }

    public OreUITextButton style(OreUIAtlas.ButtonStyle style) {
        this.style = style;
        return textColor(style.textColor);
    }

    public OreUIAtlas.ButtonStyle style() {
        return this.style;
    }

    @Override
    public void onPress() {
        if (this.onPressed != null)
            this.onPressed.accept(this);
    }

    @Override
    protected void renderOreUI(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        OreUIAtlas.blitNineSlice(
                guiGraphics,
                getX(),
                getY(),
                getWidth(),
                getHeight(),
                OreUIAtlas.buttonU(style, isPointerOver(mouseX, mouseY), isVisuallyPressed(), isEnabled()),
                style.v,
                OreUIAtlas.BUTTON_SRC,
                style.sourceHeight,
                OreUIAtlas.BUTTON_SLICE
        );
        drawLabel(guiGraphics, mouseX, mouseY);
    }
}

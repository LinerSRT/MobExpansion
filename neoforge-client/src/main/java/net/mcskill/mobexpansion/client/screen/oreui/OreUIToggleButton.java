package net.mcskill.mobexpansion.client.screen.oreui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.function.Consumer;

public class OreUIToggleButton extends OreUIControl<OreUIToggleButton> {
    private boolean toggled;
    private Consumer<OreUIToggleButton> onToggled;

    public OreUIToggleButton(int x, int y, Component message) {
        this(x, y, 0, message, false, null);
    }

    public OreUIToggleButton(int x, int y, Component message, Consumer<OreUIToggleButton> onToggled) {
        this(x, y, 0, message, false, onToggled);
    }

    public OreUIToggleButton(int x, int y, int width, Component message, boolean toggled, Consumer<OreUIToggleButton> onToggled) {
        super(x, y, width, OreUIAtlas.TOGGLE_HEIGHT, message);
        this.toggled = toggled;
        this.onToggled = onToggled;
        iconInset(OreUIAtlas.TOGGLE_WIDTH)
                .textPadding(2)
                .textAlign(TextAlign.LEFT)
                .textColor(0xFFFEFEFE)
                .disabledTextColor(0xFF848484)
                .packIfNeeded();
    }

    public OreUIToggleButton setOnToggled(Consumer<OreUIToggleButton> onToggled) {
        this.onToggled = onToggled;
        return this;
    }

    public OreUIToggleButton setToggled(boolean toggled) {
        this.toggled = toggled;
        return this;
    }

    public boolean isToggled() {
        return this.toggled;
    }

    @Override
    public void onPress() {
        this.toggled = !this.toggled;
        if (this.onToggled != null)
            this.onToggled.accept(this);
    }

    @Override
    protected MutableComponent createNarrationMessage() {
        return wrapDefaultNarrationMessage(getMessage())
                .append(" ")
                .append(Component.translatable(this.toggled ? "options.on" : "options.off"));
    }

    @Override
    protected void renderOreUI(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        OreUIAtlas.blit(
                guiGraphics,
                getX(),
                getY(),
                0,
                OreUIAtlas.toggleV(this.toggled, isPointerOver(mouseX, mouseY) || isVisuallyPressed(), isEnabled()),
                OreUIAtlas.TOGGLE_WIDTH,
                OreUIAtlas.TOGGLE_HEIGHT
        );
        drawLabel(guiGraphics, mouseX, mouseY);
    }
}

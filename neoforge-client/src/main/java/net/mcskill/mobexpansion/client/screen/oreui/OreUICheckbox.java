package net.mcskill.mobexpansion.client.screen.oreui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.function.Consumer;

public class OreUICheckbox extends OreUIControl<OreUICheckbox> {
    private boolean checked;
    private Consumer<OreUICheckbox> onChanged;

    public OreUICheckbox(int x, int y, Component message) {
        this(x, y, 0, message, false, null);
    }

    public OreUICheckbox(int x, int y, Component message, boolean checked, Consumer<OreUICheckbox> onChanged) {
        this(x, y, 0, message, checked, onChanged);
    }

    public OreUICheckbox(int x, int y, int width, Component message, boolean checked, Consumer<OreUICheckbox> onChanged) {
        super(x, y, width, OreUIAtlas.CHECKBOX_SIZE, message);
        this.checked = checked;
        this.onChanged = onChanged;
        iconInset(OreUIAtlas.CHECKBOX_SIZE)
                .textPadding(3)
                .textAlign(TextAlign.LEFT)
                .textColor(0xFFFEFEFE)
                .disabledTextColor(0xFF848484)
                .packIfNeeded();
    }

    public OreUICheckbox setOnChanged(Consumer<OreUICheckbox> onChanged) {
        this.onChanged = onChanged;
        return this;
    }

    public OreUICheckbox setChecked(boolean checked) {
        this.checked = checked;
        return this;
    }

    public boolean isChecked() {
        return this.checked;
    }

    @Override
    public void onPress() {
        this.checked = !this.checked;
        if (this.onChanged != null)
            this.onChanged.accept(this);
    }

    @Override
    protected MutableComponent createNarrationMessage() {
        return wrapDefaultNarrationMessage(getMessage())
                .append(" ")
                .append(Component.translatable(this.checked ? "gui.yes" : "gui.no"));
    }

    @Override
    protected void renderOreUI(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        OreUIAtlas.blit(
                guiGraphics,
                getX(),
                getY(),
                64,
                OreUIAtlas.checkboxV(this.checked, isEnabled()),
                OreUIAtlas.CHECKBOX_SIZE,
                OreUIAtlas.CHECKBOX_SIZE
        );
        drawLabel(guiGraphics, mouseX, mouseY);
    }
}

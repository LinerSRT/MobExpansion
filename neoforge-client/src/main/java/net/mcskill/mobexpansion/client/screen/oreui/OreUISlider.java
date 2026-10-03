package net.mcskill.mobexpansion.client.screen.oreui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class OreUISlider extends OreUIControl<OreUISlider> {
    private double min;
    private double max;
    private double step;
    private double value;
    private boolean integer;
    private boolean dragging;
    private Consumer<OreUISlider> onChanged;
    private Consumer<OreUISlider> onRelease;

    public OreUISlider(int x, int y, int width) {
        this(x, y, width, 0.0, 1.0, 0.0);
    }

    public OreUISlider(int x, int y, int width, double min, double max, double value) {
        super(x, y, Math.max(OreUIAtlas.SLIDER_THUMB_WIDTH, width), OreUIAtlas.SLIDER_THUMB_HEIGHT, Component.empty());
        applyRange(min, max);
        this.value = snap(value);
    }

    public OreUISlider range(double min, double max) {
        applyRange(min, max);
        setValue(this.value, false);
        return this;
    }

    public OreUISlider min(double min) {
        return range(min, this.max);
    }

    public OreUISlider max(double max) {
        return range(this.min, max);
    }

    public OreUISlider step(double step) {
        this.step = Math.max(0.0, step);
        setValue(this.value, false);
        return this;
    }

    public OreUISlider integer() {
        return integer(true);
    }

    public OreUISlider integer(boolean integer) {
        this.integer = integer;
        if (integer && this.step <= 0.0)
            this.step = 1.0;
        setValue(this.value, false);
        return this;
    }

    public OreUISlider value(double value) {
        return setValue(value, false);
    }

    public OreUISlider setValue(double value, boolean notify) {
        final double snapped = snap(value);
        final boolean changed = Double.compare(snapped, this.value) != 0;
        this.value = snapped;
        if (notify && changed && this.onChanged != null)
            this.onChanged.accept(this);
        return this;
    }

    public OreUISlider progress(double progress) {
        return setValue(this.min + Mth.clamp(progress, 0.0, 1.0) * span(), false);
    }

    public OreUISlider onChanged(Consumer<OreUISlider> onChanged) {
        this.onChanged = onChanged;
        return this;
    }

    public OreUISlider onRelease(Consumer<OreUISlider> onRelease) {
        this.onRelease = onRelease;
        return this;
    }

    public double min() {
        return this.min;
    }

    public double max() {
        return this.max;
    }

    public double step() {
        return this.step;
    }

    public double value() {
        return this.value;
    }

    public int valueInt() {
        return (int) Math.round(this.value);
    }

    public double progress() {
        final double span = span();
        return span == 0.0 ? 0.0 : Mth.clamp((this.value - this.min) / span, 0.0, 1.0);
    }

    public boolean isInteger() {
        return this.integer;
    }

    public boolean isDragging() {
        return this.dragging;
    }

    @Override
    public void onPress() {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (mouseButton != 0 || !isActive() || !isVisible() || !isPointerOver(mouseX, mouseY))
            return false;
        this.dragging = true;
        setValueFromMouse(mouseX, true);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0)
            return false;
        return stopDragging();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!this.dragging || button != 0)
            return false;
        setValueFromMouse(mouseX, true);
        return true;
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        if (this.dragging)
            setValueFromMouse(mouseX, true);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isActive() || !isVisible() || !isPointerOver(mouseX, mouseY) || scrollY == 0.0)
            return false;
        nudge(scrollY > 0.0 ? 1 : -1);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isActive() || !isVisible())
            return false;
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_DOWN) {
            nudge(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_UP) {
            nudge(1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            setValue(this.min, true);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            setValue(this.max, true);
            return true;
        }
        return false;
    }

    @Override
    protected MutableComponent createNarrationMessage() {
        return Component.translatable("gui.narrate.slider", Component.literal(formatValue()));
    }

    @Override
    protected void renderOreUI(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.dragging) {
            if (!isPhysicalLeftMouseDown())
                stopDragging();
            else
                setValueFromMouse(mouseX, true);
        }
        final int trackY = getY() + (getHeight() - OreUIAtlas.SLIDER_TRACK_HEIGHT) / 2;
        final int trackX = getX() + 3;
        final int trackWidth = getWidth() - 6;
        if (!isEnabled()) {
            OreUIAtlas.blitSliderTrack(guiGraphics, trackX, trackY, trackWidth, OreUIAtlas.SLIDER_TRACK_DISABLED_V);
        } else {
            final int split = Mth.clamp(thumbX() + OreUIAtlas.SLIDER_THUMB_WIDTH / 2 - trackX, 0, trackWidth);
            OreUIAtlas.blitSliderTrack(guiGraphics, trackX, trackY, split, OreUIAtlas.SLIDER_TRACK_FILLED_V);
            OreUIAtlas.blitSliderTrack(guiGraphics, trackX + split, trackY, trackWidth - split, OreUIAtlas.SLIDER_TRACK_EMPTY_V);
        }
        final int thumbY = getY() + (getHeight() - OreUIAtlas.SLIDER_THUMB_HEIGHT) / 2;
        OreUIAtlas.blitSliderThumb(guiGraphics, thumbX(), thumbY, isEnabled(), this.dragging);
        drawLabel(guiGraphics, mouseX, mouseY);
    }

    private void applyRange(double min, double max) {
        if (max < min) {
            this.min = max;
            this.max = min;
            return;
        }
        this.min = min;
        this.max = max;
    }

    private double span() {
        return this.max - this.min;
    }

    private double snap(double raw) {
        double clamped = this.max < this.min ? raw : Mth.clamp(raw, this.min, this.max);
        final double increment = increment();
        if (increment > 0.0 && span() > 0.0) {
            final double steps = Math.round((clamped - this.min) / increment);
            clamped = this.min + steps * increment;
            clamped = Mth.clamp(clamped, this.min, this.max);
        }
        if (this.integer)
            clamped = Math.round(clamped);
        return clamped;
    }

    private double increment() {
        if (this.step > 0.0)
            return this.step;
        if (this.integer)
            return 1.0;
        return 0.0;
    }

    private double nudgeAmount() {
        if (increment() > 0.0)
            return increment();
        final double span = span();
        return span == 0.0 ? 0.0 : span / 100.0;
    }

    private void nudge(int direction) {
        setValue(this.value + direction * nudgeAmount(), true);
    }

    private void setValueFromMouse(double mouseX, boolean notify) {
        final int travel = Math.max(1, getWidth() - OreUIAtlas.SLIDER_THUMB_WIDTH);
        final double progress = (mouseX - getX() - OreUIAtlas.SLIDER_THUMB_WIDTH / 2.0) / travel;
        setValue(this.min + Mth.clamp(progress, 0.0, 1.0) * span(), notify);
    }

    private int thumbX() {
        final int travel = Math.max(0, getWidth() - OreUIAtlas.SLIDER_THUMB_WIDTH);
        return getX() + (int) Math.round(progress() * travel);
    }

    private boolean stopDragging() {
        if (!this.dragging)
            return false;
        this.dragging = false;
        playDownSound(Minecraft.getInstance().getSoundManager());
        if (this.onRelease != null)
            this.onRelease.accept(this);
        return true;
    }

    private String formatValue() {
        if (this.integer || this.step >= 1.0 && this.step == Math.floor(this.step))
            return Integer.toString(valueInt());
        return trimDouble(this.value);
    }

    private static String trimDouble(double value) {
        final String text = Double.toString(value);
        if (!text.contains("."))
            return text;
        int end = text.length();
        while (end > 0 && text.charAt(end - 1) == '0')
            end--;
        if (end > 0 && text.charAt(end - 1) == '.')
            end--;
        return text.substring(0, end);
    }
}

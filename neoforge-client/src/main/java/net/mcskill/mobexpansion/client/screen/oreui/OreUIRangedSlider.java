package net.mcskill.mobexpansion.client.screen.oreui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class OreUIRangedSlider extends OreUIControl<OreUIRangedSlider> {
    private enum Thumb {
        LOW,
        HIGH
    }

    private double min;
    private double max;
    private double step;
    private double low;
    private double high;
    private double minDistance;
    private boolean integer;
    private boolean dragging;
    private Thumb activeThumb = Thumb.LOW;
    private Consumer<OreUIRangedSlider> onChanged;
    private Consumer<OreUIRangedSlider> onRelease;

    public OreUIRangedSlider(int x, int y, int width) {
        this(x, y, width, 0.0, 1.0, 0.0, 1.0);
    }

    public OreUIRangedSlider(int x, int y, int width, double min, double max, double low, double high) {
        super(x, y, Math.max(OreUIAtlas.SLIDER_THUMB_WIDTH * 2, width), OreUIAtlas.SLIDER_THUMB_HEIGHT, Component.empty());
        applyRange(min, max);
        this.low = snap(low);
        this.high = snap(high);
        normalizeThumbs();
    }

    public OreUIRangedSlider range(double min, double max) {
        applyRange(min, max);
        setValues(this.low, this.high, false);
        return this;
    }

    public OreUIRangedSlider min(double min) {
        return range(min, this.max);
    }

    public OreUIRangedSlider max(double max) {
        return range(this.min, max);
    }

    public OreUIRangedSlider step(double step) {
        this.step = Math.max(0.0, step);
        setValues(this.low, this.high, false);
        return this;
    }

    public OreUIRangedSlider minDistance(double minDistance) {
        this.minDistance = Math.max(0.0, minDistance);
        setValues(this.low, this.high, false);
        return this;
    }

    public OreUIRangedSlider integer() {
        return integer(true);
    }

    public OreUIRangedSlider integer(boolean integer) {
        this.integer = integer;
        if (integer && this.step <= 0.0)
            this.step = 1.0;
        setValues(this.low, this.high, false);
        return this;
    }

    public OreUIRangedSlider values(double low, double high) {
        return setValues(low, high, false);
    }

    public OreUIRangedSlider low(double low) {
        return setValues(low, this.high, false);
    }

    public OreUIRangedSlider high(double high) {
        return setValues(this.low, high, false);
    }

    public OreUIRangedSlider setValues(double low, double high, boolean notify) {
        double nextLow = snap(low);
        double nextHigh = snap(high);
        if (nextHigh < nextLow) {
            final double swap = nextLow;
            nextLow = nextHigh;
            nextHigh = swap;
        }
        final double gap = gap();
        if (nextHigh - nextLow < gap)
            nextHigh = snap(nextLow + gap);
        if (nextHigh > this.max) {
            nextHigh = this.max;
            nextLow = snap(nextHigh - gap);
        }
        nextLow = Mth.clamp(nextLow, this.min, this.max);
        nextHigh = Mth.clamp(nextHigh, this.min, this.max);
        if (nextHigh < nextLow)
            nextHigh = nextLow;
        final boolean changed = Double.compare(nextLow, this.low) != 0 || Double.compare(nextHigh, this.high) != 0;
        this.low = nextLow;
        this.high = nextHigh;
        if (notify && changed && this.onChanged != null)
            this.onChanged.accept(this);
        return this;
    }

    public OreUIRangedSlider onChanged(Consumer<OreUIRangedSlider> onChanged) {
        this.onChanged = onChanged;
        return this;
    }

    public OreUIRangedSlider onRelease(Consumer<OreUIRangedSlider> onRelease) {
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

    public double low() {
        return this.low;
    }

    public double high() {
        return this.high;
    }

    public int lowInt() {
        return (int) Math.round(this.low);
    }

    public int highInt() {
        return (int) Math.round(this.high);
    }

    public double lowProgress() {
        return progressOf(this.low);
    }

    public double highProgress() {
        return progressOf(this.high);
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
        this.activeThumb = nearestThumb(mouseX);
        this.dragging = true;
        setThumbFromMouse(mouseX, true);
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
        setThumbFromMouse(mouseX, true);
        return true;
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        if (this.dragging)
            setThumbFromMouse(mouseX, true);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isActive() || !isVisible() || !isPointerOver(mouseX, mouseY) || scrollY == 0.0)
            return false;
        this.activeThumb = nearestThumb(mouseX);
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
            if (this.activeThumb == Thumb.LOW)
                setValues(this.min, this.high, true);
            else
                setValues(this.low, this.low + gap(), true);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            if (this.activeThumb == Thumb.HIGH)
                setValues(this.low, this.max, true);
            else
                setValues(this.high - gap(), this.high, true);
            return true;
        }
        return false;
    }

    @Override
    protected MutableComponent createNarrationMessage() {
        return Component.translatable("gui.narrate.slider", Component.literal(formatValue(this.low) + " - " + formatValue(this.high)));
    }

    @Override
    protected void renderOreUI(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.dragging) {
            if (!isPhysicalLeftMouseDown())
                stopDragging();
            else
                setThumbFromMouse(mouseX, true);
        }
        final int trackY = getY() + (getHeight() - OreUIAtlas.SLIDER_TRACK_HEIGHT) / 2;
        final int trackX = getX() + 3;
        final int trackWidth = getWidth() - 6;
        final int thumbY = getY() + (getHeight() - OreUIAtlas.SLIDER_THUMB_HEIGHT) / 2;
        final int lowX = lowThumbX();
        final int highX = highThumbX();
        if (!isEnabled()) {
            OreUIAtlas.blitSliderTrack(guiGraphics, trackX, trackY, trackWidth, OreUIAtlas.SLIDER_TRACK_DISABLED_V);
        } else {
            final int filledStart = Mth.clamp(lowX + OreUIAtlas.SLIDER_THUMB_WIDTH / 2 - trackX, 0, trackWidth);
            final int filledEnd = Mth.clamp(highX + OreUIAtlas.SLIDER_THUMB_WIDTH / 2 - trackX, filledStart, trackWidth);
            OreUIAtlas.blitSliderTrack(guiGraphics, trackX, trackY, filledStart, OreUIAtlas.SLIDER_TRACK_EMPTY_V);
            OreUIAtlas.blitSliderTrack(guiGraphics, trackX + filledStart, trackY, filledEnd - filledStart, OreUIAtlas.SLIDER_TRACK_FILLED_V);
            OreUIAtlas.blitSliderTrack(guiGraphics, trackX + filledEnd, trackY, trackWidth - filledEnd, OreUIAtlas.SLIDER_TRACK_EMPTY_V);
        }
        if (this.activeThumb == Thumb.LOW) {
            OreUIAtlas.blitSliderThumb(guiGraphics, highX, thumbY, isEnabled(), false);
            OreUIAtlas.blitSliderThumb(guiGraphics, lowX, thumbY, isEnabled(), this.dragging);
        } else {
            OreUIAtlas.blitSliderThumb(guiGraphics, lowX, thumbY, isEnabled(), false);
            OreUIAtlas.blitSliderThumb(guiGraphics, highX, thumbY, isEnabled(), this.dragging);
        }
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

    private void normalizeThumbs() {
        setValues(this.low, this.high, false);
    }

    private double span() {
        return this.max - this.min;
    }

    private double gap() {
        if (this.minDistance > 0.0)
            return this.minDistance;
        return 0.0;
    }

    private double snap(double raw) {
        double clamped = Mth.clamp(raw, this.min, this.max);
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
        final double delta = direction * nudgeAmount();
        if (this.activeThumb == Thumb.LOW)
            setThumbValue(Thumb.LOW, this.low + delta, true);
        else
            setThumbValue(Thumb.HIGH, this.high + delta, true);
    }

    private void setThumbFromMouse(double mouseX, boolean notify) {
        final int travel = Math.max(1, thumbTravel());
        final double origin = this.activeThumb == Thumb.LOW
                ? getX() + OreUIAtlas.SLIDER_THUMB_WIDTH / 2.0
                : getX() + OreUIAtlas.SLIDER_THUMB_WIDTH * 1.5;
        final double progress = (mouseX - origin) / travel;
        setThumbValue(this.activeThumb, this.min + Mth.clamp(progress, 0.0, 1.0) * span(), notify);
    }

    private void setThumbValue(Thumb thumb, double raw, boolean notify) {
        final double snapped = snap(raw);
        if (thumb == Thumb.LOW)
            setValues(Math.min(snapped, this.high - gap()), this.high, notify);
        else
            setValues(this.low, Math.max(snapped, this.low + gap()), notify);
    }

    private Thumb nearestThumb(double mouseX) {
        final double lowCenter = lowThumbX() + OreUIAtlas.SLIDER_THUMB_WIDTH / 2.0;
        final double highCenter = highThumbX() + OreUIAtlas.SLIDER_THUMB_WIDTH / 2.0;
        if (Math.abs(mouseX - lowCenter) <= Math.abs(mouseX - highCenter))
            return Thumb.LOW;
        return Thumb.HIGH;
    }

    private double progressOf(double value) {
        final double span = span();
        return span == 0.0 ? 0.0 : Mth.clamp((value - this.min) / span, 0.0, 1.0);
    }

    private int thumbTravel() {
        return Math.max(0, getWidth() - OreUIAtlas.SLIDER_THUMB_WIDTH * 2);
    }

    private int lowThumbX() {
        return getX() + (int) Math.round(lowProgress() * thumbTravel());
    }

    private int highThumbX() {
        return getX() + OreUIAtlas.SLIDER_THUMB_WIDTH + (int) Math.round(highProgress() * thumbTravel());
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

    private String formatValue(double value) {
        if (this.integer || this.step >= 1.0 && this.step == Math.floor(this.step))
            return Integer.toString((int) Math.round(value));
        return trimDouble(value);
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

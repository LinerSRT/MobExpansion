package net.mcskill.mobexpansion.client.screen.oreui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.mcskill.mobexpansion.client.screen.widget.GuiMarquee;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public abstract class OreUIControl<T extends OreUIControl<T>> extends Button {
    public enum TextAlign {
        LEFT,
        CENTER,
        RIGHT
    }

    private boolean leftMousePressed;
    private boolean pointerOver;
    private boolean autoWidth;
    private int minWidth;
    private int iconInset;
    private int trailingInset;
    private int textPadLeft = 2;
    private int textPadRight = 2;
    private int textOffsetX;
    private int textOffsetY;
    private int textColor = 0xFFFEFEFE;
    private int disabledTextColor = 0xFF848484;
    private int hoverTextColor;
    private int pressedTextColor;
    private TextAlign textAlign = TextAlign.LEFT;
    private boolean marquee = true;
    private boolean textShadow;
    private boolean pressShiftsText;
    private float clickPitch = 1.3F;

    protected OreUIControl(int x, int y, int width, int height, Component message) {
        super(x, y, Math.max(1, width), height, message, button -> {
        }, DEFAULT_NARRATION);
        this.autoWidth = width <= 0;
    }

    @SuppressWarnings("unchecked")
    protected final T self() {
        return (T) this;
    }

    protected T packIfNeeded() {
        if (this.autoWidth)
            pack();
        return self();
    }

    public T pack() {
        setWidth(Math.max(this.minWidth, computeAutoWidth()));
        this.autoWidth = true;
        return self();
    }

    public T autoWidth(boolean autoWidth) {
        this.autoWidth = autoWidth;
        if (autoWidth)
            pack();
        return self();
    }

    public boolean isAutoWidth() {
        return this.autoWidth;
    }

    public T minWidth(int minWidth) {
        this.minWidth = Math.max(0, minWidth);
        if (this.autoWidth)
            pack();
        return self();
    }

    public T width(int width) {
        this.autoWidth = false;
        setWidth(width);
        return self();
    }

    public T height(int height) {
        setHeight(height);
        return self();
    }

    public T size(int width, int height) {
        this.autoWidth = false;
        setSize(width, height);
        return self();
    }

    public T pos(int x, int y) {
        setPosition(x, y);
        return self();
    }

    public T x(int x) {
        setX(x);
        return self();
    }

    public T y(int y) {
        setY(y);
        return self();
    }

    public T label(Component message) {
        setMessage(message);
        return self();
    }

    @Override
    public void setMessage(@NotNull Component message) {
        super.setMessage(message);
        if (this.autoWidth)
            pack();
    }

    public T enabled(boolean enabled) {
        this.active = enabled;
        return self();
    }

    public T visible(boolean visible) {
        this.visible = visible;
        return self();
    }

    public T setEnabled(boolean enabled) {
        return enabled(enabled);
    }

    public T setVisible(boolean visible) {
        return visible(visible);
    }

    public boolean isEnabled() {
        return this.active;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public T alpha(float alpha) {
        setAlpha(alpha);
        return self();
    }

    public T tooltip(@Nullable Component tooltip) {
        setTooltip(tooltip == null ? null : Tooltip.create(tooltip));
        return self();
    }

    public T tooltip(@Nullable Tooltip tooltip) {
        setTooltip(tooltip);
        return self();
    }

    public T iconInset(int iconInset) {
        this.iconInset = Math.max(0, iconInset);
        if (this.autoWidth)
            pack();
        return self();
    }

    public T trailingInset(int trailingInset) {
        this.trailingInset = Math.max(0, trailingInset);
        if (this.autoWidth)
            pack();
        return self();
    }

    public T textPadding(int padding) {
        return textPadding(padding, padding);
    }

    public T textPadding(int left, int right) {
        this.textPadLeft = Math.max(0, left);
        this.textPadRight = Math.max(0, right);
        if (this.autoWidth)
            pack();
        return self();
    }

    public T textOffset(int x, int y) {
        this.textOffsetX = x;
        this.textOffsetY = y;
        return self();
    }

    public T textAlign(TextAlign textAlign) {
        this.textAlign = textAlign;
        return self();
    }

    public T textColor(int color) {
        this.textColor = color;
        return self();
    }

    public T disabledTextColor(int color) {
        this.disabledTextColor = color;
        return self();
    }

    public T hoverTextColor(int color) {
        this.hoverTextColor = color;
        return self();
    }

    public T pressedTextColor(int color) {
        this.pressedTextColor = color;
        return self();
    }

    public T marquee(boolean marquee) {
        this.marquee = marquee;
        return self();
    }

    public T textShadow(boolean textShadow) {
        this.textShadow = textShadow;
        return self();
    }

    public T pressShiftsText(boolean pressShiftsText) {
        this.pressShiftsText = pressShiftsText;
        return self();
    }

    public T clickPitch(float pitch) {
        this.clickPitch = pitch;
        return self();
    }

    public boolean isPressed() {
        return this.leftMousePressed;
    }

    protected boolean isVisuallyPressed() {
        return this.leftMousePressed && this.pointerOver;
    }


    protected boolean isPointerOver(double mouseX, double mouseY) {
        return mouseX >= getX()
                && mouseY >= getY()
                && mouseX < getX() + getWidth()
                && mouseY < getY() + getHeight();
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return isVisible() && isEnabled() && isPointerOver(mouseX, mouseY);
    }

    protected int computeAutoWidth() {
        return this.iconInset + this.textPadLeft + font().width(getMessage()) + this.textPadRight + this.trailingInset;
    }

    protected int pressShift() {
        return this.pressShiftsText && isVisuallyPressed() ? 2 : 0;
    }

    protected Font font() {
        return Minecraft.getInstance().font;
    }

    protected int currentTextColor(double mouseX, double mouseY) {
        if (!isEnabled())
            return this.disabledTextColor;
        if (isVisuallyPressed() && this.pressedTextColor != 0)
            return this.pressedTextColor;
        if (isPointerOver(mouseX, mouseY) && this.hoverTextColor != 0)
            return this.hoverTextColor;
        return this.textColor;
    }

    protected void drawLabel(GuiGraphics guiGraphics, double mouseX, double mouseY) {
        final Component text = getMessage();
        if (text.getString().isEmpty())
            return;
        final Font font = font();
        final int left = getX() + this.iconInset + this.textPadLeft + this.textOffsetX;
        final int maxWidth = Math.max(0, getX() + getWidth() - this.trailingInset - this.textPadRight - left);
        if (maxWidth == 0)
            return;
        final int y = getY() + (getHeight() - 8) / 2 + this.textOffsetY + pressShift();
        final int textWidth = font.width(text);
        final int color = currentTextColor(mouseX, mouseY);
        if (textWidth <= maxWidth) {
            int x = left;
            if (this.textAlign == TextAlign.CENTER)
                x = left + (maxWidth - textWidth) / 2;
            else if (this.textAlign == TextAlign.RIGHT)
                x = left + maxWidth - textWidth;
            guiGraphics.drawString(font, text, x, y, color, this.textShadow);
            return;
        }
        if (this.marquee) {
            GuiMarquee.draw(guiGraphics, font, text, left, y, maxWidth, color, this.textShadow);
            return;
        }
        guiGraphics.drawString(font, font.plainSubstrByWidth(text.getString(), maxWidth), left, y, color, this.textShadow);
    }

    @Override
    public void playDownSound(@NotNull SoundManager handler) {
        handler.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, this.clickPitch));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (mouseButton != 0 || !isActive() || !isVisible() || !isPointerOver(mouseX, mouseY))
            return false;
        this.leftMousePressed = true;
        playDownSound(Minecraft.getInstance().getSoundManager());
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0)
            return false;
        final boolean armed = this.leftMousePressed;
        this.leftMousePressed = false;
        if (armed && isActive() && isVisible() && isPointerOver(mouseX, mouseY))
            onPress();
        return armed;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.leftMousePressed && !isPhysicalLeftMouseDown())
            this.leftMousePressed = false;
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        this.pointerOver = isEnabled() && isVisible() && isPointerOver(mouseX, mouseY);
        renderOreUI(guiGraphics, mouseX, mouseY, partialTick);
    }

    protected static boolean isPhysicalLeftMouseDown() {
        return GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    protected abstract void renderOreUI(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);
}

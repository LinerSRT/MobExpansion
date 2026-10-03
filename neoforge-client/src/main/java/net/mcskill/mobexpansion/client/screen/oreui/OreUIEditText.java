package net.mcskill.mobexpansion.client.screen.oreui;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class OreUIEditText extends EditBox {
    private static final int TEXT_PAD = 4;
    private static final int TEXT_COLOR = 0xFFFEFEFE;
    private static final int DISABLED_TEXT_COLOR = 0xFF848484;

    public OreUIEditText(int x, int y, int width) {
        this(x, y, width, OreUIAtlas.EDIT_HEIGHT, Component.empty());
    }

    public OreUIEditText(int x, int y, int width, Component message) {
        this(x, y, width, OreUIAtlas.EDIT_HEIGHT, message);
    }

    public OreUIEditText(int x, int y, int width, int height, Component message) {
        this(Minecraft.getInstance().font, x, y, width, height, null, message);
    }

    public OreUIEditText(Font font, int x, int y, int width, int height, Component message) {
        this(font, x, y, width, height, null, message);
    }

    public OreUIEditText(Font font, int x, int y, int width, int height, @Nullable EditBox previous, Component message) {
        super(font, x, y, Math.max(OreUIAtlas.EDIT_SRC, width), Math.max(OreUIAtlas.EDIT_HEIGHT, height), previous, message);
        setTextColor(TEXT_COLOR);
        setTextColorUneditable(DISABLED_TEXT_COLOR);
        setTextShadow(false);
    }

    public OreUIEditText value(String value) {
        setValue(value);
        return this;
    }

    public OreUIEditText hint(@Nullable Component hint) {
        setHint(hint);
        return this;
    }

    public OreUIEditText hint(@Nullable String hint) {
        return hint(hint == null ? null : Component.literal(hint));
    }

    public OreUIEditText maxLength(int length) {
        setMaxLength(length);
        return this;
    }

    public OreUIEditText filter(Predicate<String> filter) {
        setFilter(filter);
        return this;
    }

    public OreUIEditText suggestion(@Nullable String suggestion) {
        setSuggestion(suggestion);
        return this;
    }

    public OreUIEditText onChanged(@Nullable Consumer<String> responder) {
        setResponder(responder);
        return this;
    }

    public OreUIEditText editable(boolean editable) {
        setEditable(editable);
        return this;
    }

    public OreUIEditText enabled(boolean enabled) {
        this.active = enabled;
        setEditable(enabled);
        return this;
    }

    public OreUIEditText visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public OreUIEditText pos(int x, int y) {
        setPosition(x, y);
        return this;
    }

    public OreUIEditText size(int width, int height) {
        setSize(width, height);
        return this;
    }

    public OreUIEditText width(int width) {
        setWidth(width);
        return this;
    }

    public OreUIEditText height(int height) {
        setHeight(height);
        return this;
    }

    public OreUIEditText textColor(int color) {
        setTextColor(color);
        return this;
    }

    public OreUIEditText disabledTextColor(int color) {
        setTextColorUneditable(color);
        return this;
    }

    public OreUIEditText textShadow(boolean textShadow) {
        setTextShadow(textShadow);
        return this;
    }

    public OreUIEditText tooltip(@Nullable Component tooltip) {
        setTooltip(tooltip == null ? null : Tooltip.create(tooltip));
        return this;
    }

    public OreUIEditText tooltip(@Nullable Tooltip tooltip) {
        setTooltip(tooltip);
        return this;
    }

    public boolean isEnabled() {
        return this.active;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return isVisible()
                && this.active
                && mouseX >= getX()
                && mouseY >= getY()
                && mouseX < getX() + getWidth()
                && mouseY < getY() + getHeight();
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!isVisible())
            return;
        OreUIAtlas.blitEditBox(guiGraphics, getX(), getY(), getWidth(), getHeight(), isActive() && this.isEditable);
        renderText(guiGraphics);
    }

    private void renderText(GuiGraphics guiGraphics) {
        final int color = this.isEditable ? this.textColor : this.textColorUneditable;
        final int cursorOffset = this.cursorPos - this.displayPos;
        final String visible = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), getInnerWidth());
        final boolean cursorInRange = cursorOffset >= 0 && cursorOffset <= visible.length();
        final boolean showCursor = isFocused() && (Util.getMillis() - this.focusedTime) / 300L % 2L == 0L && cursorInRange;
        final int textX = getX() + 3;
        final int textY = getY() + (this.height - 8) / 2 + 1;
        int cursorX = textX;
        final int selectionOffset = Mth.clamp(this.highlightPos - this.displayPos, 0, visible.length());
        if (!visible.isEmpty()) {
            final String beforeCursor = cursorInRange ? visible.substring(0, cursorOffset) : visible;
            cursorX = guiGraphics.drawString(this.font, this.formatter.apply(beforeCursor, this.displayPos), textX, textY, color, this.textShadow);
        }
        final boolean cursorAtEnd = this.cursorPos < this.value.length() || this.value.length() >= this.maxLength;
        int caretX = cursorX;
        if (!cursorInRange)
            caretX = cursorOffset > 0 ? textX + this.width : textX;
        else if (cursorAtEnd) {
            caretX = cursorX - 1;
            cursorX--;
        }
        if (!visible.isEmpty() && cursorInRange && cursorOffset < visible.length())
            guiGraphics.drawString(this.font, this.formatter.apply(visible.substring(cursorOffset), this.cursorPos), cursorX, textY, color, this.textShadow);
        if (this.hint != null && visible.isEmpty() && !isFocused())
            guiGraphics.drawString(this.font, this.hint, cursorX, textY, color, this.textShadow);
        if (!cursorAtEnd && this.suggestion != null)
            guiGraphics.drawString(this.font, this.suggestion, caretX - 1, textY, 0xFF808080, this.textShadow);
        if (showCursor) {
            if (cursorAtEnd)
                guiGraphics.fill(RenderType.guiOverlay(), caretX, textY - 1, caretX + 1, textY + 1 + 9, 0xFFD0D0D0);
            else
                guiGraphics.drawString(this.font, "_", caretX, textY, color, this.textShadow);
        }
        if (selectionOffset != cursorOffset) {
            final int selectionX = textX + this.font.width(visible.substring(0, selectionOffset));
            renderHighlight(guiGraphics, caretX, textY - 1, selectionX - 1, textY + 1 + 9);
        }
    }

    private void renderHighlight(GuiGraphics guiGraphics, int startX, int startY, int endX, int endY) {
        if (startX < endX) {
            final int swap = startX;
            startX = endX;
            endX = swap;
        }
        if (startY < endY) {
            final int swap = startY;
            startY = endY;
            endY = swap;
        }
        endX = Math.min(endX, getX() + getWidth() - TEXT_PAD);
        startX = Math.min(startX, getX() + getWidth() - TEXT_PAD);
        guiGraphics.fill(RenderType.guiTextHighlight(), startX, startY, endX, endY, 0xFF212181);
    }
}

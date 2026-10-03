package net.mcskill.mobexpansion.client.screen.oreui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class OreUIBlockTextButton extends OreUITextButton {
    public enum IconSide {
        LEFT,
        RIGHT
    }

    private static final int NATIVE_ICON_SIZE = 16;

    private ItemStack icon = ItemStack.EMPTY;
    private IconSide iconSide = IconSide.LEFT;
    private int iconSize = NATIVE_ICON_SIZE;
    private int iconPadding = 2;

    public OreUIBlockTextButton(int x, int y, Component message) {
        super(x, y, message);
    }

    public OreUIBlockTextButton(int x, int y, Component message, Consumer<OreUITextButton> onPressed) {
        super(x, y, message, onPressed);
    }

    public OreUIBlockTextButton(int x, int y, int width, int height, Component message, Consumer<OreUITextButton> onPressed) {
        super(x, y, width, height, message, onPressed);
    }

    public OreUIBlockTextButton(int x, int y, int width, int height, Component message, OreUIAtlas.ButtonStyle style, Consumer<OreUITextButton> onPressed) {
        super(x, y, width, height, message, style, onPressed);
    }

    public OreUIBlockTextButton icon(@Nullable ItemStack stack) {
        this.icon = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        return applyIconInsets();
    }

    public OreUIBlockTextButton icon(@Nullable ItemLike item) {
        return icon(item == null ? ItemStack.EMPTY : new ItemStack(item));
    }

    public OreUIBlockTextButton icon(@Nullable BlockState state) {
        return icon(state == null ? null : state.getBlock());
    }

    public OreUIBlockTextButton clearIcon() {
        return icon(ItemStack.EMPTY);
    }

    public OreUIBlockTextButton iconSide(IconSide iconSide) {
        this.iconSide = iconSide == null ? IconSide.LEFT : iconSide;
        return applyIconInsets();
    }

    public OreUIBlockTextButton iconLeft() {
        return iconSide(IconSide.LEFT);
    }

    public OreUIBlockTextButton iconRight() {
        return iconSide(IconSide.RIGHT);
    }

    public OreUIBlockTextButton iconSize(int iconSize) {
        this.iconSize = Mth.clamp(iconSize, 1, NATIVE_ICON_SIZE);
        return applyIconInsets();
    }

    public OreUIBlockTextButton iconPadding(int iconPadding) {
        this.iconPadding = Math.max(0, iconPadding);
        return applyIconInsets();
    }

    public ItemStack icon() {
        return this.icon;
    }

    public IconSide iconSide() {
        return this.iconSide;
    }

    public int iconSize() {
        return this.iconSize;
    }

    @Override
    protected void renderOreUI(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderOreUI(guiGraphics, mouseX, mouseY, partialTick);
        if (this.icon.isEmpty())
            return;
        final int size = resolvedIconSize();
        final int x = this.iconSide == IconSide.LEFT
                ? getX() + this.iconPadding
                : getX() + getWidth() - this.iconPadding - size;
        final int y = getY() + (getHeight() - size) / 2 + pressShift();
        if (!isEnabled())
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha * 0.5F);
        final PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0.0F);
        final float scale = size / (float) NATIVE_ICON_SIZE;
        pose.scale(scale, scale, 1.0F);
        guiGraphics.renderItem(this.icon, 0, 0);
        pose.popPose();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
    }

    private int resolvedIconSize() {
        return Math.min(this.iconSize, Math.max(1, getHeight() - 2));
    }

    private OreUIBlockTextButton applyIconInsets() {
        final int reserve = this.icon.isEmpty() ? 0 : resolvedIconSize() + this.iconPadding;
        if (this.iconSide == IconSide.LEFT) {
            iconInset(reserve);
            trailingInset(0);
        } else {
            iconInset(0);
            trailingInset(reserve);
        }
        return this;
    }
}

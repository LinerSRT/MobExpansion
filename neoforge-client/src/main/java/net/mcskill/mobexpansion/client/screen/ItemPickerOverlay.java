package net.mcskill.mobexpansion.client.screen;

import net.mcskill.mobexpansion.client.screen.oreui.OreUIAtlas;
import net.mcskill.mobexpansion.client.screen.oreui.OreUIEditText;
import net.mcskill.mobexpansion.client.screen.oreui.OreUIPanel;
import net.mcskill.mobexpansion.client.screen.oreui.OreUITextButton;
import net.mcskill.mobexpansion.client.screen.widget.GuiTextures;
import net.mcskill.mobexpansion.client.screen.widget.NineSlicePainter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Item picker modal matching {@link MobDropConfigScreen} panel / button / scroll style.
 */
public final class ItemPickerOverlay {
    private static final int COLOR_TEXT = 0xFFFEFEFE;
    private static final int COLOR_MUTED = 0xFFAAAAAA;
    private static final int COLOR_ROW_SELECTED = 0x554CAF50;
    private static final int ROW_HEIGHT = 22;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int PANEL_WIDTH = 440;
    private static final int PANEL_HEIGHT = 380;
    private static final int CONTENT_PAD = 10;
    private static final int TITLE_Y = 12;
    private static final int SEARCH_Y = 34;
    private static final int INNER_TOP = 58;
    private static final int LIST_HEADER_OFFSET = 10;
    private static final int LIST_BODY_OFFSET = 26;
    private static final int FOOTER_HEIGHT = 40;
    private static final int BUTTON_HEIGHT = 22;
    private static final int BUTTON_WIDTH = 130;

    private static List<ItemStack> cachedItems;
    private static List<ItemStack> cachedBlockItems;

    private final Screen parentScreen;
    private final Font font;
    private final Consumer<ResourceLocation> onSelect;
    private final Runnable onCancel;
    private final boolean blocksOnly;

    private OreUIEditText searchBox;
    private OreUITextButton selectButton;
    private OreUITextButton cancelButton;
    private final List<ItemStack> filteredItems = new ArrayList<>();
    private int scrollOffset;
    private int selectedIndex;
    private boolean draggingScrollbar;
    private int panelLeft;
    private int panelTop;

    public ItemPickerOverlay(Screen parentScreen, Font font, Consumer<ResourceLocation> onSelect, Runnable onCancel) {
        this(parentScreen, font, onSelect, onCancel, false);
    }

    public ItemPickerOverlay(Screen parentScreen, Font font, Consumer<ResourceLocation> onSelect, Runnable onCancel, boolean blocksOnly) {
        this.parentScreen = parentScreen;
        this.font = font;
        this.onSelect = onSelect;
        this.onCancel = onCancel;
        this.blocksOnly = blocksOnly;
    }

    public static int panelWidth() {
        return PANEL_WIDTH;
    }

    public static int panelHeight() {
        return PANEL_HEIGHT;
    }

    public void init(int screenWidth, int screenHeight) {
        this.panelLeft = (screenWidth - PANEL_WIDTH) / 2;
        this.panelTop = (screenHeight - PANEL_HEIGHT) / 2;
        this.scrollOffset = 0;
        this.selectedIndex = 0;
        this.draggingScrollbar = false;

        this.searchBox = new OreUIEditText(this.font, this.panelLeft + CONTENT_PAD + 2, this.panelTop + SEARCH_Y, PANEL_WIDTH - CONTENT_PAD * 2 - 4, OreUIAtlas.EDIT_HEIGHT, Component.translatable("gui.mobexpansion.item_picker.search"))
                .maxLength(128)
                .hint(Component.translatable("gui.mobexpansion.item_picker.search"))
                .textColor(COLOR_TEXT)
                .onChanged(text -> applyFilter());

        final int buttonY = this.panelTop + PANEL_HEIGHT - FOOTER_HEIGHT + (FOOTER_HEIGHT - BUTTON_HEIGHT) / 2;
        this.cancelButton = new OreUITextButton(this.panelLeft + CONTENT_PAD, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("gui.mobexpansion.item_picker.cancel"), OreUIAtlas.ButtonStyle.DESTRUCTIVE, button -> this.onCancel.run());
        this.selectButton = new OreUITextButton(this.panelLeft + PANEL_WIDTH - CONTENT_PAD - BUTTON_WIDTH, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("gui.mobexpansion.item_picker.select"), OreUIAtlas.ButtonStyle.PRIMARY, button -> confirmSelection());

        applyFilter();
    }

    public boolean isSearchFocused() {
        return this.searchBox != null && this.searchBox.isFocused();
    }

    private int innerLeft() {
        return this.panelLeft + CONTENT_PAD;
    }

    private int innerTop() {
        return this.panelTop + INNER_TOP;
    }

    private int innerWidth() {
        return PANEL_WIDTH - CONTENT_PAD * 2;
    }

    private int innerHeight() {
        return PANEL_HEIGHT - INNER_TOP - FOOTER_HEIGHT;
    }

    private int listLeft() {
        return innerLeft() + 8;
    }

    private int listTop() {
        return innerTop() + LIST_BODY_OFFSET;
    }

    private int listWidth() {
        return needsScrollbar() ? innerWidth() - 16 - SCROLLBAR_WIDTH - 4 : innerWidth() - 16;
    }

    private int listHeight() {
        return Math.max(ROW_HEIGHT, innerHeight() - LIST_BODY_OFFSET - 8);
    }

    private int visibleRows() {
        return Math.max(1, listHeight() / ROW_HEIGHT);
    }

    public void addWidgets(Consumer<AbstractWidget> widgetAdder) {
        widgetAdder.accept(this.searchBox);
        widgetAdder.accept(this.cancelButton);
        widgetAdder.accept(this.selectButton);
        this.parentScreen.setFocused(this.searchBox);
        this.searchBox.setFocused(true);
    }

    public void preselect(ResourceLocation itemId) {
        if (itemId == null)
            return;
        for (int index = 0; index < this.filteredItems.size(); index++) {
            if (BuiltInRegistries.ITEM.getKey(this.filteredItems.get(index).getItem()).equals(itemId)) {
                this.selectedIndex = index;
                ensureSelectionVisible();
                return;
            }
        }
    }

    private void applyFilter() {
        final String query = this.searchBox == null ? "" : this.searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        this.filteredItems.clear();
        for (ItemStack itemStack : allItems(this.blocksOnly)) {
            if (matches(itemStack, query))
                this.filteredItems.add(itemStack);
        }
        this.selectedIndex = Mth.clamp(this.selectedIndex, 0, Math.max(0, this.filteredItems.size() - 1));
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, maxScroll());
        updateSelectButton();
    }

    private static boolean matches(ItemStack itemStack, String query) {
        if (query.isEmpty())
            return true;
        final ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
        if (itemId.toString().toLowerCase(Locale.ROOT).contains(query))
            return true;
        return itemStack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query);
    }

    private void updateSelectButton() {
        if (this.selectButton != null)
            this.selectButton.active = !this.filteredItems.isEmpty();
    }

    private void confirmSelection() {
        if (this.filteredItems.isEmpty() || this.selectedIndex < 0 || this.selectedIndex >= this.filteredItems.size())
            return;
        final ItemStack itemStack = this.filteredItems.get(this.selectedIndex);
        if (this.blocksOnly && itemStack.getItem() instanceof BlockItem blockItem) {
            this.onSelect.accept(BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()));
            return;
        }
        this.onSelect.accept(BuiltInRegistries.ITEM.getKey(itemStack.getItem()));
    }

    private int maxScroll() {
        return Math.max(0, this.filteredItems.size() - visibleRows());
    }

    private boolean needsScrollbar() {
        return maxScroll() > 0;
    }

    private int scrollbarLeft() {
        return listLeft() + listWidth() + 2;
    }

    private void ensureSelectionVisible() {
        final int rows = visibleRows();
        if (this.selectedIndex < this.scrollOffset)
            this.scrollOffset = this.selectedIndex;
        if (this.selectedIndex >= this.scrollOffset + rows)
            this.scrollOffset = this.selectedIndex - rows + 1;
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, maxScroll());
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.fill(0, 0, this.parentScreen.width, this.parentScreen.height, 0xBB000000);

        OreUIPanel.renderDark(guiGraphics, this.panelLeft, this.panelTop, PANEL_WIDTH, PANEL_HEIGHT);
        OreUIPanel.renderLight(guiGraphics, innerLeft(), innerTop(), innerWidth(), innerHeight());

        guiGraphics.drawString(this.font, Component.translatable(this.blocksOnly ? "gui.mobexpansion.item_picker.title_blocks" : "gui.mobexpansion.item_picker.title"), this.panelLeft + CONTENT_PAD, this.panelTop + TITLE_Y, COLOR_TEXT, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.mobexpansion.item_picker.col.name"), listLeft() + 22, innerTop() + LIST_HEADER_OFFSET, COLOR_MUTED, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.mobexpansion.item_picker.col.id"), listLeft() + 180, innerTop() + LIST_HEADER_OFFSET, COLOR_MUTED, false);

        final int listLeft = listLeft();
        final int listTop = listTop();
        final int listWidth = listWidth();
        final int listHeight = listHeight();
        guiGraphics.enableScissor(listLeft, listTop, listLeft + listWidth, listTop + listHeight);
        final int endIndex = Math.min(this.filteredItems.size(), this.scrollOffset + visibleRows());
        for (int index = this.scrollOffset; index < endIndex; index++)
            renderRow(guiGraphics, index, listTop + (index - this.scrollOffset) * ROW_HEIGHT);
        guiGraphics.disableScissor();

        if (this.filteredItems.isEmpty())
            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mobexpansion.item_picker.empty"), this.panelLeft + PANEL_WIDTH / 2, listTop + listHeight / 2 - 4, COLOR_MUTED);

        renderScrollbar(guiGraphics);
    }

    private void renderRow(GuiGraphics guiGraphics, int index, int rowY) {
        final ItemStack itemStack = this.filteredItems.get(index);
        final int rowLeft = listLeft();
        final int rowWidth = listWidth();
        if (index == this.selectedIndex)
            guiGraphics.fill(rowLeft, rowY, rowLeft + rowWidth, rowY + ROW_HEIGHT - 1, COLOR_ROW_SELECTED);
        guiGraphics.renderItem(itemStack, rowLeft + 2, rowY + (ROW_HEIGHT - 16) / 2);
        final String itemName = this.font.plainSubstrByWidth(itemStack.getHoverName().getString(), 150);
        final String itemId = this.font.plainSubstrByWidth(BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString(), Math.max(40, rowWidth - 190));
        guiGraphics.drawString(this.font, itemName, rowLeft + 22, rowY + 7, COLOR_TEXT, false);
        guiGraphics.drawString(this.font, itemId, rowLeft + 180, rowY + 7, COLOR_MUTED, false);
    }

    private void renderScrollbar(GuiGraphics guiGraphics) {
        if (!needsScrollbar())
            return;
        final int barLeft = scrollbarLeft();
        final int barTop = listTop();
        final int barHeight = listHeight();
        NineSlicePainter.blit(guiGraphics, GuiTextures.SCROLL_GUTTER, barLeft, barTop, SCROLLBAR_WIDTH, barHeight, 3, 1);
        final int thumbHeight = Mth.clamp(barHeight * visibleRows() / Math.max(1, this.filteredItems.size()), 18, barHeight);
        final int travel = Math.max(1, barHeight - thumbHeight);
        final int thumbTop = barTop + (int) ((long) this.scrollOffset * travel / Math.max(1, maxScroll()));
        NineSlicePainter.blit(guiGraphics, GuiTextures.SCROLL_HANDLE, barLeft, thumbTop, SCROLLBAR_WIDTH, thumbHeight, 3, 5, 1, 2, 1, 2);
    }

    /**
     * @return true if the click was handled; false if clicked outside the panel (caller may cancel).
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0)
            return isInsidePanel(mouseX, mouseY);
        if (!isInsidePanel(mouseX, mouseY))
            return false;
        if (isOverScrollbar(mouseX, mouseY)) {
            this.draggingScrollbar = true;
            setScrollFromMouseY(mouseY);
            return true;
        }
        // Footer OreUI buttons arm/press via the parent screen's widget pass.
        if (isOverFooterButton(mouseX, mouseY))
            return true;
        if (this.searchBox != null && this.searchBox.isMouseOver(mouseX, mouseY))
            return true;
        final int rowIndex = rowIndexAt(mouseX, mouseY);
        if (rowIndex >= 0) {
            if (rowIndex == this.selectedIndex)
                confirmSelection();
            else {
                this.selectedIndex = rowIndex;
                updateSelectButton();
            }
        }
        return true;
    }

    private boolean isOverFooterButton(double mouseX, double mouseY) {
        return this.cancelButton != null && this.cancelButton.isMouseOver(mouseX, mouseY)
                || this.selectButton != null && this.selectButton.isMouseOver(mouseX, mouseY);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button == 0 && this.draggingScrollbar) {
            setScrollFromMouseY(mouseY);
            return true;
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.draggingScrollbar = false;
        // Do not consume — parent must deliver release to focused OreUI buttons.
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!isInsidePanel(mouseX, mouseY))
            return true;
        this.scrollOffset = Mth.clamp(this.scrollOffset - (int) Math.signum(scrollY), 0, maxScroll());
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isSearchFocused()) {
            if (keyCode == 256) {
                this.onCancel.run();
                return true;
            }
            return this.searchBox.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == 256) {
            this.onCancel.run();
            return true;
        }
        if (keyCode == 257 || keyCode == 335) {
            confirmSelection();
            return true;
        }
        if (keyCode == 264) {
            this.selectedIndex = Mth.clamp(this.selectedIndex + 1, 0, Math.max(0, this.filteredItems.size() - 1));
            ensureSelectionVisible();
            return true;
        }
        if (keyCode == 265) {
            this.selectedIndex = Mth.clamp(this.selectedIndex - 1, 0, Math.max(0, this.filteredItems.size() - 1));
            ensureSelectionVisible();
            return true;
        }
        return false;
    }

    private boolean isInsidePanel(double mouseX, double mouseY) {
        return mouseX >= this.panelLeft && mouseX <= this.panelLeft + PANEL_WIDTH
                && mouseY >= this.panelTop && mouseY <= this.panelTop + PANEL_HEIGHT;
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        if (!needsScrollbar())
            return false;
        final int barLeft = scrollbarLeft();
        return mouseX >= barLeft && mouseX <= barLeft + SCROLLBAR_WIDTH
                && mouseY >= listTop() && mouseY <= listTop() + listHeight();
    }

    private void setScrollFromMouseY(double mouseY) {
        final int barHeight = listHeight();
        final int thumbHeight = Mth.clamp(barHeight * visibleRows() / Math.max(1, this.filteredItems.size()), 18, barHeight);
        final int travel = Math.max(1, barHeight - thumbHeight);
        final double relative = Mth.clamp(mouseY - listTop() - thumbHeight * 0.5D, 0.0D, travel);
        this.scrollOffset = Mth.clamp((int) Math.round(relative * maxScroll() / travel), 0, maxScroll());
    }

    private int rowIndexAt(double mouseX, double mouseY) {
        if (mouseX < listLeft() || mouseX > listLeft() + listWidth() || mouseY < listTop() || mouseY > listTop() + listHeight())
            return -1;
        final int index = this.scrollOffset + (int) ((mouseY - listTop()) / ROW_HEIGHT);
        return index >= 0 && index < this.filteredItems.size() ? index : -1;
    }

    private static List<ItemStack> allItems(boolean blocksOnly) {
        if (blocksOnly) {
            if (cachedBlockItems != null)
                return cachedBlockItems;
            final List<ItemStack> blockStacks = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (item instanceof BlockItem)
                    blockStacks.add(new ItemStack(item));
            }
            cachedBlockItems = List.copyOf(blockStacks);
            return cachedBlockItems;
        }
        if (cachedItems != null)
            return cachedItems;
        final List<ItemStack> itemStacks = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR)
                continue;
            itemStacks.add(new ItemStack(item));
        }
        cachedItems = List.copyOf(itemStacks);
        return cachedItems;
    }
}

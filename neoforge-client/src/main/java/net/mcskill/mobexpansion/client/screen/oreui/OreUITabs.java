package net.mcskill.mobexpansion.client.screen.oreui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class OreUITabs extends AbstractContainerWidget {
    public enum Orientation {
        HORIZONTAL,
        VERTICAL
    }

    private static final int DEFAULT_TAB_HEIGHT = 16;
    private static final int DEFAULT_GAP = 1;

    private final List<OreUITabButton> tabs = new ArrayList<>();
    private Orientation orientation = Orientation.HORIZONTAL;
    private int gap = DEFAULT_GAP;
    private int tabWidth;
    private int tabHeight = DEFAULT_TAB_HEIGHT;
    private int selectedIndex;
    private boolean layingOut;
    private OreUIAtlas.ButtonStyle selectedStyle = OreUIAtlas.ButtonStyle.TERTIARY;
    private OreUIAtlas.ButtonStyle unselectedStyle = OreUIAtlas.ButtonStyle.TERTIARY;
    @Nullable
    private Consumer<OreUITabButton> onChanged;

    public OreUITabs(int x, int y, String... labels) {
        this(x, y, Arrays.stream(labels).map(Component::literal).toList());
    }

    public OreUITabs(int x, int y, Component... labels) {
        this(x, y, Arrays.asList(labels));
    }

    public OreUITabs(int x, int y, Collection<? extends Component> labels) {
        super(x, y, 1, DEFAULT_TAB_HEIGHT, Component.empty());
        for (Component label : labels)
            this.tabs.add(new OreUITabButton(this, this.tabs.size(), label));
        refreshStyles();
        layoutTabs();
    }

    public OreUITabs orientation(Orientation orientation) {
        this.orientation = orientation;
        layoutTabs();
        return this;
    }

    public OreUITabs horizontal() {
        return orientation(Orientation.HORIZONTAL);
    }

    public OreUITabs vertical() {
        return orientation(Orientation.VERTICAL);
    }

    public OreUITabs gap(int gap) {
        this.gap = Math.max(0, gap);
        layoutTabs();
        return this;
    }

    public OreUITabs tabSize(int width, int height) {
        this.tabWidth = Math.max(0, width);
        this.tabHeight = Math.max(1, height);
        layoutTabs();
        return this;
    }

    public OreUITabs tabWidth(int width) {
        this.tabWidth = Math.max(0, width);
        layoutTabs();
        return this;
    }

    public OreUITabs tabHeight(int height) {
        this.tabHeight = Math.max(1, height);
        layoutTabs();
        return this;
    }

    public int tabHeight() {
        return this.tabHeight;
    }

    public OreUITabs selectedStyle(OreUIAtlas.ButtonStyle style) {
        this.selectedStyle = style;
        refreshStyles();
        return this;
    }

    public OreUITabs unselectedStyle(OreUIAtlas.ButtonStyle style) {
        this.unselectedStyle = style;
        refreshStyles();
        return this;
    }

    public OreUITabs selected(int index) {
        select(index, false);
        return this;
    }

    public OreUITabs onChanged(@Nullable Consumer<OreUITabButton> onChanged) {
        this.onChanged = onChanged;
        return this;
    }

    public OreUITabs enabled(boolean enabled) {
        this.active = enabled;
        for (OreUITabButton tab : this.tabs)
            tab.enabled(enabled);
        return this;
    }

    public OreUITabs visible(boolean visible) {
        this.visible = visible;
        for (OreUITabButton tab : this.tabs)
            tab.visible(visible);
        return this;
    }

    public Orientation orientation() {
        return this.orientation;
    }

    public int selectedIndex() {
        return this.selectedIndex;
    }

    public OreUITabButton selectedTab() {
        return tab(this.selectedIndex);
    }

    public OreUITabButton tab(int index) {
        return this.tabs.get(index);
    }

    public List<OreUITabButton> tabs() {
        return Collections.unmodifiableList(this.tabs);
    }

    public void select(int index) {
        select(index, false);
    }

    void selectFromClick(OreUITabButton button) {
        if (button.index() == this.selectedIndex)
            return;
        select(button.index(), true);
    }

    public void select(int index, boolean notify) {
        if (this.tabs.isEmpty())
            return;
        this.selectedIndex = Math.max(0, Math.min(index, this.tabs.size() - 1));
        refreshStyles();
        if (notify && this.onChanged != null)
            this.onChanged.accept(this.tabs.get(this.selectedIndex));
    }

    private void refreshStyles() {
        for (int i = 0; i < this.tabs.size(); i++)
            this.tabs.get(i).style(i == this.selectedIndex ? this.selectedStyle : this.unselectedStyle);
    }

    private void layoutTabs() {
        if (this.layingOut)
            return;
        this.layingOut = true;
        try {
            final int cellWidth = cellWidth();
            int cursorX = getX();
            int cursorY = getY();
            for (OreUITabButton tab : this.tabs) {
                tab.height(this.tabHeight);
                if (this.tabWidth > 0)
                    tab.width(this.tabWidth);
                else
                    tab.width(cellWidth);
                tab.pos(cursorX, cursorY);
                if (this.orientation == Orientation.HORIZONTAL)
                    cursorX += tab.getWidth() + this.gap;
                else
                    cursorY += tab.getHeight() + this.gap;
            }
            if (this.tabs.isEmpty()) {
                super.setWidth(1);
                super.setHeight(this.tabHeight);
                return;
            }
            if (this.orientation == Orientation.HORIZONTAL) {
                super.setWidth(Math.max(1, cursorX - getX() - this.gap));
                super.setHeight(this.tabHeight);
            } else {
                super.setWidth(Math.max(1, cellWidth));
                super.setHeight(Math.max(1, cursorY - getY() - this.gap));
            }
        } finally {
            this.layingOut = false;
        }
    }

    private int cellWidth() {
        if (this.tabWidth > 0)
            return this.tabWidth;
        int widest = 1;
        for (OreUITabButton tab : this.tabs) {
            tab.height(this.tabHeight);
            tab.pack();
            widest = Math.max(widest, tab.getWidth());
        }
        return widest;
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        layoutTabs();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        layoutTabs();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible)
            return false;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.visible
                && this.active
                && mouseX >= getX()
                && mouseY >= getY()
                && mouseX < getX() + getWidth()
                && mouseY < getY() + getHeight();
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        setDragging(false);
        boolean handled = false;
        for (OreUITabButton tab : this.tabs)
            handled |= tab.mouseReleased(mouseX, mouseY, button);
        return handled;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        for (OreUITabButton tab : this.tabs) {
            tab.setAlpha(this.alpha);
            tab.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
        if (!this.tabs.isEmpty())
            this.tabs.get(this.selectedIndex).updateNarration(narrationElementOutput);
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        return this.tabs;
    }
}

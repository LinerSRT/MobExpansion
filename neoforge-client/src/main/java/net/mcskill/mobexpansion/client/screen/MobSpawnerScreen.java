package net.mcskill.mobexpansion.client.screen;

import com.mojang.blaze3d.Blaze3D;
import net.mcskill.mobexpansion.blockentity.MobSpawnerBlockEntity;
import net.mcskill.mobexpansion.client.screen.oreui.OreUIAtlas;
import net.mcskill.mobexpansion.client.screen.oreui.OreUIBlockTextButton;
import net.mcskill.mobexpansion.client.screen.oreui.OreUIEditText;
import net.mcskill.mobexpansion.client.screen.oreui.OreUIPanel;
import net.mcskill.mobexpansion.client.screen.oreui.OreUIRangedSlider;
import net.mcskill.mobexpansion.client.screen.oreui.OreUISlider;
import net.mcskill.mobexpansion.client.screen.oreui.OreUITabs;
import net.mcskill.mobexpansion.client.screen.oreui.OreUITextButton;
import net.mcskill.mobexpansion.client.screen.oreui.OreUIToggleButton;
import net.mcskill.mobexpansion.client.screen.widget.GuiTextures;
import net.mcskill.mobexpansion.client.screen.widget.NineSlicePainter;
import net.mcskill.mobexpansion.menu.MobSpawnerMenu;
import net.mcskill.mobexpansion.network.UpdateMobSpawnerPacket;
import net.mcskill.mobexpansion.spawner.SpawnMode;
import net.mcskill.mobexpansion.spawner.SpawnerEntityEntry;
import net.mcskill.mobexpansion.spawner.SpawnerMobCatalog;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MobSpawnerScreen extends AbstractContainerScreen<MobSpawnerMenu> {
    private static final int COLOR_ACCENT = 0xFF8df1ff;
    private static final int COLOR_TEXT = 0xFFFEFEFE;
    private static final int COLOR_MUTED = 0xFFAAAAAA;
    private static final int COLOR_SELECTED = 0x554CAF50;
    private static final int ROW_HEIGHT = 22;
    private static final int DRAG_ROW_HEIGHT = 26;
    private static final int ICON_SIZE = 18;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_GAP = 4;

    private static final int LEFT_X = 10;
    private static final int LEFT_W = 160;
    private static final int CENTER_X = 178;
    private static final int CENTER_W = 232;
    private static final int RIGHT_X = 418;
    private static final int RIGHT_W = 220;
    private static final int CONTENT_TOP = 30;
    private static final int CONTENT_H = 328;
    private static final int FOOTER_Y = 368;
    private static final int MASK_PREVIEW_TOP = 128;
    private static final int MASK_PREVIEW_H = 108;
    private static final int MASK_PROP_TOP = 244;
    private static final int MASK_PROP_BOTTOM = CONTENT_TOP + CONTENT_H - 8;
    private static final int MASK_PROP_BUTTON_H = 18;
    private static final int MASK_PROP_GAP = 1;
    private static final int MASK_PROP_PAD = 12;
    private static final float MASK_PREVIEW_SPIN_DEG_PER_SEC = 18.0F;

    private static final int SET_VISIBLE_ROWS = 4;
    private static final int PICKER_VISIBLE_ROWS = 6;

    private enum Tab {
        MAIN,
        CONDITIONS,
        VISUAL
    }

    private enum BlockPickTarget {
        UNDER,
        AROUND,
        MASK
    }

    private final List<SpawnerEntityEntry> workingEntries = new ArrayList<>();
    private final List<ResourceLocation> filteredCatalog = new ArrayList<>();
    private final Map<ResourceLocation, LivingEntity> previewByEntityId = new HashMap<>();
    private final List<OreUITextButton> mainStepperButtons = new ArrayList<>();
    private final List<AbstractWidget> maskPropertyWidgets = new ArrayList<>();

    private OreUIEditText searchBox;
    private OreUIEditText intervalBox;
    private OreUIEditText countMinBox;
    private OreUIEditText countMaxBox;
    private OreUIEditText maxSimultaneousBox;
    private OreUIEditText activationRadiusBox;
    private OreUIEditText spawnRadiusBox;
    private OreUIEditText spawnYMinBox;
    private OreUIEditText spawnYMaxBox;
    private OreUIEditText activationDelayBox;
    private OreUIRangedSlider activationDistSlider;

    private OreUITextButton enabledButton;
    private OreUITabs centerTabs;
    private OreUITextButton removeEntityButton;
    private OreUITextButton moveUpButton;
    private OreUITextButton moveDownButton;
    private OreUITextButton spawnModeButton;
    private OreUIToggleButton checkLosButton;
    private OreUIToggleButton checkDaylightButton;
    private OreUIToggleButton checkSpectatorButton;
    private OreUIToggleButton checkHeightButton;
    private OreUIBlockTextButton blockUnderButton;
    private OreUIBlockTextButton blockAroundButton;
    private OreUITextButton clearUnderButton;
    private OreUITextButton clearAroundButton;
    private OreUIBlockTextButton maskButton;
    private OreUITextButton clearMaskButton;

    private Tab activeTab = Tab.MAIN;
    private SpawnMode workingSpawnMode = SpawnMode.STANDARD;
    private boolean requireLos;
    private boolean requireDaylight;
    private boolean ignoreSpectators = true;
    private boolean heightLimit;
    @Nullable
    private ResourceLocation blockUnder;
    @Nullable
    private ResourceLocation blockAround;
    @Nullable
    private BlockState maskState;

    private int selectedSetIndex;
    private int setScrollOffset;
    private int pickerScrollOffset;
    private int dragFromIndex = -1;
    private boolean isDraggingRow;
    private double dragPointerY;
    private boolean draggingSetScrollbar;
    private boolean draggingPickerScrollbar;
    private boolean suppressFieldSync;
    private int workingActivationDelay;
    private float workingActivationDistMin;
    private float workingActivationDistMax;
    private int maskPropScrollOffset;
    @Nullable
    private ItemPickerOverlay blockPickerOverlay;
    @Nullable
    private BlockPickTarget blockPickTarget;

    public MobSpawnerScreen(MobSpawnerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 650;
        this.imageHeight = 400;
        this.titleLabelY = 10;
        this.inventoryLabelY = 10000;
        for (SpawnerEntityEntry entry : menu.getEntries())
            this.workingEntries.add(entry.copy());
        this.workingSpawnMode = menu.getSpawnMode();
        this.requireLos = menu.isRequireLos();
        this.requireDaylight = menu.isRequireDaylight();
        this.ignoreSpectators = menu.isIgnoreSpectators();
        this.heightLimit = menu.isHeightLimit();
        this.blockUnder = menu.getBlockUnder();
        this.blockAround = menu.getBlockAround();
        this.maskState = menu.getMaskBlockState();
        this.workingActivationDelay = menu.getActivationDelay();
        this.workingActivationDistMin = menu.getActivationDistMin();
        this.workingActivationDistMax = menu.getActivationDistMax();
        this.selectedSetIndex = 0;
        rebuildFilteredCatalog("");
    }

    @Override
    protected void init() {
        super.init();
        buildWidgets();
        loadCenterFromSelection();
        loadRightFromWorking();
        updateTabButtons();
        updateCenterVisibility();
    }

    @Override
    public void resize(@NotNull Minecraft minecraft, int width, int height) {
        applySelectedFields();
        super.resize(minecraft, width, height);
    }

    private int fitGuiWidth() {
        return this.blockPickerOverlay != null ? ItemPickerOverlay.panelWidth() : this.imageWidth;
    }

    private int fitGuiHeight() {
        return this.blockPickerOverlay != null ? ItemPickerOverlay.panelHeight() : this.imageHeight;
    }

    private void syncFitScale() {
        GuiFitScale.update(this.width, this.height, fitGuiWidth(), fitGuiHeight());
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
    }

    private void buildWidgets() {
        final ResourceLocation pickerPreselect = this.blockPickerOverlay == null ? null
                : switch (this.blockPickTarget) {
                    case UNDER -> this.blockUnder;
                    case AROUND -> this.blockAround;
                    case MASK -> maskBlockId();
                    case null -> null;
                };
        clearWidgets();
        this.mainStepperButtons.clear();
        this.maskPropertyWidgets.clear();

        if (this.blockPickerOverlay != null) {
            this.centerTabs = null;
            this.blockPickerOverlay.init(this.width, this.height);
            this.blockPickerOverlay.addWidgets(this::addRenderableWidget);
            this.blockPickerOverlay.preselect(pickerPreselect);
            return;
        }

        final int left = this.leftPos;
        final int top = this.topPos;
        final int fieldX = left + CENTER_X + 118;
        final int fieldW = 56;
        final int stepperX = fieldX + fieldW + 4;
        final int rightFieldX = left + RIGHT_X + 8;
        final int rightFieldW = RIGHT_W - 16;

        this.removeEntityButton = addOreButton(left + LEFT_X + 8, top + 138, 100, 18, Component.translatable("gui.mobexpansion.mob_spawner.remove"), OreUIAtlas.ButtonStyle.DESTRUCTIVE, this::removeSelectedEntity);
        this.moveUpButton = addOreButton(left + LEFT_X + 110, top + 138, 20, 18, Component.literal("▲"), () -> moveSelectedEntity(-1));
        this.moveDownButton = addOreButton(left + LEFT_X + 131, top + 138, 20, 18, Component.literal("▼"), () -> moveSelectedEntity(1));
        explain(this.removeEntityButton, "remove");
        explain(this.moveUpButton, "gui.mobexpansion.icon.arrow_up", "gui.mobexpansion.mob_spawner.move_up.tooltip");
        explain(this.moveDownButton, "gui.mobexpansion.icon.arrow_down", "gui.mobexpansion.mob_spawner.move_down.tooltip");

        this.searchBox = createEditBox(left + LEFT_X + 8, top + 182, LEFT_W - 16, 16, Component.translatable("gui.mobexpansion.mob_spawner.search"));
        this.searchBox.hint(Component.translatable("gui.mobexpansion.mob_spawner.search")).onChanged(this::onSearchChanged);
        addRenderableWidget(this.searchBox);
        explain(this.searchBox, "search");

        final int tabGap = 1;
        final int tabWidth = (CENTER_W - 16 - tabGap * 2) / 3;
        this.centerTabs = addRenderableWidget(new OreUITabs(
                left + CENTER_X + 8,
                top + 36,
                Component.translatable("gui.mobexpansion.mob_spawner.tab.main"),
                Component.translatable("gui.mobexpansion.mob_spawner.tab.conditions"),
                Component.translatable("gui.mobexpansion.mob_spawner.tab.visual")
        ).gap(tabGap).tabSize(tabWidth, 18).selected(this.activeTab.ordinal())
                .onChanged(tab -> setActiveTab(Tab.values()[tab.index()])));
        explain(this.centerTabs.tab(0), "tab.main");
        explain(this.centerTabs.tab(1), "tab.conditions");
        explain(this.centerTabs.tab(2), "tab.visual");

        this.intervalBox = createEditBox(fieldX, top + 70, fieldW, 16, Component.empty());
        this.countMinBox = createEditBox(fieldX, top + 96, fieldW, 16, Component.empty());
        this.countMaxBox = createEditBox(fieldX, top + 122, fieldW, 16, Component.empty());
        this.maxSimultaneousBox = createEditBox(fieldX, top + 148, fieldW, 16, Component.empty());
        this.activationRadiusBox = createEditBox(fieldX, top + 174, fieldW, 16, Component.empty());
        this.spawnRadiusBox = createEditBox(fieldX, top + 200, fieldW, 16, Component.empty());
        this.spawnYMinBox = createEditBox(fieldX, top + 226, fieldW, 16, Component.empty());
        this.spawnYMaxBox = createEditBox(fieldX, top + 252, fieldW, 16, Component.empty());
        addRenderableWidget(this.intervalBox);
        addRenderableWidget(this.countMinBox);
        addRenderableWidget(this.countMaxBox);
        addRenderableWidget(this.maxSimultaneousBox);
        addRenderableWidget(this.activationRadiusBox);
        addRenderableWidget(this.spawnRadiusBox);
        addRenderableWidget(this.spawnYMinBox);
        addRenderableWidget(this.spawnYMaxBox);
        explain(this.intervalBox, "interval");
        explain(this.countMinBox, "count_min");
        explain(this.countMaxBox, "count_max");
        explain(this.maxSimultaneousBox, "max_simultaneous");
        explain(this.activationRadiusBox, "activation_radius");
        explain(this.spawnRadiusBox, "spawn_radius");
        explain(this.spawnYMinBox, "spawn_y_min");
        explain(this.spawnYMaxBox, "spawn_y_max");

        addStepperPair(stepperX, top + 68, () -> nudgeSelectedInt(this.intervalBox, -20, SpawnerEntityEntry.MIN_INTERVAL, SpawnerEntityEntry.MAX_INTERVAL), () -> nudgeSelectedInt(this.intervalBox, 20, SpawnerEntityEntry.MIN_INTERVAL, SpawnerEntityEntry.MAX_INTERVAL), "interval");
        addStepperPair(stepperX, top + 94, () -> nudgeSelectedInt(this.countMinBox, -1, SpawnerEntityEntry.MIN_COUNT, SpawnerEntityEntry.MAX_COUNT), () -> nudgeSelectedInt(this.countMinBox, 1, SpawnerEntityEntry.MIN_COUNT, SpawnerEntityEntry.MAX_COUNT), "count_min");
        addStepperPair(stepperX, top + 120, () -> nudgeSelectedInt(this.countMaxBox, -1, SpawnerEntityEntry.MIN_COUNT, SpawnerEntityEntry.MAX_COUNT), () -> nudgeSelectedInt(this.countMaxBox, 1, SpawnerEntityEntry.MIN_COUNT, SpawnerEntityEntry.MAX_COUNT), "count_max");
        addStepperPair(stepperX, top + 146, () -> nudgeSelectedInt(this.maxSimultaneousBox, -1, SpawnerEntityEntry.MIN_SIMULTANEOUS, SpawnerEntityEntry.MAX_SIMULTANEOUS), () -> nudgeSelectedInt(this.maxSimultaneousBox, 1, SpawnerEntityEntry.MIN_SIMULTANEOUS, SpawnerEntityEntry.MAX_SIMULTANEOUS), "max_simultaneous");
        addStepperPair(stepperX, top + 172, () -> nudgeSelectedFloat(this.activationRadiusBox, -1.0F, SpawnerEntityEntry.MIN_RADIUS, SpawnerEntityEntry.MAX_RADIUS), () -> nudgeSelectedFloat(this.activationRadiusBox, 1.0F, SpawnerEntityEntry.MIN_RADIUS, SpawnerEntityEntry.MAX_RADIUS), "activation_radius");
        addStepperPair(stepperX, top + 198, () -> nudgeSelectedFloat(this.spawnRadiusBox, -0.5F, SpawnerEntityEntry.MIN_RADIUS, SpawnerEntityEntry.MAX_RADIUS), () -> nudgeSelectedFloat(this.spawnRadiusBox, 0.5F, SpawnerEntityEntry.MIN_RADIUS, SpawnerEntityEntry.MAX_RADIUS), "spawn_radius");
        addStepperPair(stepperX, top + 224, () -> nudgeSelectedInt(this.spawnYMinBox, -1, SpawnerEntityEntry.MIN_SPAWN_Y, SpawnerEntityEntry.MAX_SPAWN_Y), () -> nudgeSelectedInt(this.spawnYMinBox, 1, SpawnerEntityEntry.MIN_SPAWN_Y, SpawnerEntityEntry.MAX_SPAWN_Y), "spawn_y_min");
        addStepperPair(stepperX, top + 250, () -> nudgeSelectedInt(this.spawnYMaxBox, -1, SpawnerEntityEntry.MIN_SPAWN_Y, SpawnerEntityEntry.MAX_SPAWN_Y), () -> nudgeSelectedInt(this.spawnYMaxBox, 1, SpawnerEntityEntry.MIN_SPAWN_Y, SpawnerEntityEntry.MAX_SPAWN_Y), "spawn_y_max");

        this.enabledButton = addOreButton(fieldX, top + 276, 110, 18, Component.empty(), this::toggleSelectedEnabled);
        explain(this.enabledButton, "enabled");

        this.spawnModeButton = addOreButton(rightFieldX, top + 70, rightFieldW, 18, spawnModeLabel(this.workingSpawnMode), this::cycleSpawnMode);
        this.activationDelayBox = createEditBox(rightFieldX, top + 108, rightFieldW - 44, 16, Component.empty());
        addRenderableWidget(this.activationDelayBox);
        explain(this.activationDelayBox, "activation_delay");
        explain(addOreButton(rightFieldX + rightFieldW - 40, top + 106, 18, 18, Component.literal("-"), () -> nudgeGlobalInt(this.activationDelayBox, -20, MobSpawnerBlockEntity.MIN_ACTIVATION_DELAY, MobSpawnerBlockEntity.MAX_ACTIVATION_DELAY)), "activation_delay");
        explain(addOreButton(rightFieldX + rightFieldW - 20, top + 106, 18, 18, Component.literal("+"), () -> nudgeGlobalInt(this.activationDelayBox, 20, MobSpawnerBlockEntity.MIN_ACTIVATION_DELAY, MobSpawnerBlockEntity.MAX_ACTIVATION_DELAY)), "activation_delay");


        this.checkLosButton = addRenderableWidget(new OreUIToggleButton(rightFieldX, top + 132, rightFieldW, checkboxLabel("check_los"), this.requireLos, toggle -> this.requireLos = toggle.isToggled()));
        this.checkDaylightButton = addRenderableWidget(new OreUIToggleButton(rightFieldX, top + 150, rightFieldW, checkboxLabel("check_daylight"), this.requireDaylight, toggle -> this.requireDaylight = toggle.isToggled()));
        this.checkSpectatorButton = addRenderableWidget(new OreUIToggleButton(rightFieldX, top + 168, rightFieldW, checkboxLabel("check_spectator"), this.ignoreSpectators, toggle -> this.ignoreSpectators = toggle.isToggled()));
        this.checkHeightButton = addRenderableWidget(new OreUIToggleButton(rightFieldX, top + 186, rightFieldW, checkboxLabel("check_height"), this.heightLimit, toggle -> this.heightLimit = toggle.isToggled()));
        explain(this.checkLosButton, "check_los");
        explain(this.checkDaylightButton, "check_daylight");
        explain(this.checkSpectatorButton, "check_spectator");
        explain(this.checkHeightButton, "check_height");

        this.activationDistSlider = addRenderableWidget(new OreUIRangedSlider(rightFieldX, top + 241, rightFieldW)
                .range(MobSpawnerBlockEntity.MIN_ACTIVATION_DIST, MobSpawnerBlockEntity.MAX_ACTIVATION_DIST)
                .step(1)
                .values(this.workingActivationDistMin, this.workingActivationDistMax)
                .onChanged(slider -> {
                    this.workingActivationDistMin = (float) slider.low();
                    this.workingActivationDistMax = (float) slider.high();
                }));
        this.activationDistSlider.setTooltip(Tooltip.create(Component.translatable("gui.mobexpansion.mob_spawner.activation_distance")
                .append("\n")
                .append(Component.translatable("gui.mobexpansion.mob_spawner.activation_dist_min.tooltip").withStyle(ChatFormatting.GRAY))
                .append("\n")
                .append(Component.translatable("gui.mobexpansion.mob_spawner.activation_dist_max.tooltip").withStyle(ChatFormatting.GRAY))));

        this.blockUnderButton = addBlockButton(rightFieldX, top + 288, rightFieldW - 22, 18, this.blockUnder, "block_under", () -> openBlockPicker(BlockPickTarget.UNDER));
        this.clearUnderButton = addOreButton(rightFieldX + rightFieldW - 20, top + 288, 20, 18, Component.literal("X"), OreUIAtlas.ButtonStyle.DESTRUCTIVE, () -> {
            this.blockUnder = null;
            bindBlockButton(this.blockUnderButton, null, "block_under");
        });
        this.blockAroundButton = addBlockButton(rightFieldX, top + 314, rightFieldW - 22, 18, this.blockAround, "block_around", () -> openBlockPicker(BlockPickTarget.AROUND));
        this.clearAroundButton = addOreButton(rightFieldX + rightFieldW - 20, top + 314, 20, 18, Component.literal("X"), OreUIAtlas.ButtonStyle.DESTRUCTIVE, () -> {
            this.blockAround = null;
            bindBlockButton(this.blockAroundButton, null, "block_around");
        });
        explain(this.blockUnderButton, "block_under");
        explain(this.blockAroundButton, "block_around");
        explain(this.clearUnderButton, "clear_block");
        explain(this.clearAroundButton, "clear_block");

        final int maskButtonX = left + CENTER_X + 12;
        final int maskClearX = left + CENTER_X + CENTER_W - 32;
        this.maskButton = addBlockButton(maskButtonX, top + 86, maskClearX - maskButtonX - 4, 18, maskBlockId(), "mask", () -> openBlockPicker(BlockPickTarget.MASK));
        this.clearMaskButton = addOreButton(maskClearX, top + 86, 20, 18, Component.literal("X"), OreUIAtlas.ButtonStyle.DESTRUCTIVE, () -> {
            this.maskState = null;
            this.maskPropScrollOffset = 0;
            bindBlockButton(this.maskButton, null, "mask");
            rebuildMaskPropertyButtons();
        });
        explain(this.maskButton, "mask");
        explain(this.clearMaskButton, "clear_mask");
        updateSpawnModeTooltip();

        explain(addRenderableWidget(new OreUITextButton(left + LEFT_X, top + FOOTER_Y, LEFT_W, 20, Component.translatable("gui.mobexpansion.mob_spawner.reset"), OreUIAtlas.ButtonStyle.SECONDARY, button -> resetToDefaults())), "reset");
        explain(addRenderableWidget(new OreUITextButton(left + 200, top + FOOTER_Y, 120, 22, Component.translatable("gui.cancel"), OreUIAtlas.ButtonStyle.DESTRUCTIVE, button -> onClose())), "gui.cancel", "gui.mobexpansion.mob_spawner.cancel.tooltip");
        explain(addRenderableWidget(new OreUITextButton(left + 340, top + FOOTER_Y, 120, 22, Component.translatable("gui.mobexpansion.mob_spawner.done"), OreUIAtlas.ButtonStyle.PRIMARY, button -> saveAndClose())), "done");
        rebuildMaskPropertyButtons();
    }

    private OreUITextButton addOreButton(int x, int y, int width, int height, Component message, OreUIAtlas.ButtonStyle style,  Runnable onPress) {
        return addRenderableWidget(new OreUITextButton(x, y, width, height, message, style, button -> onPress.run()));
    }
    private OreUITextButton addOreButton(int x, int y, int width, int height, Component message, Runnable onPress) {
        return addOreButton(x, y, width, height, message, OreUIAtlas.ButtonStyle.SECONDARY, onPress);
    }

    private OreUIBlockTextButton addBlockButton(int x, int y, int width, int height, @Nullable ResourceLocation blockId, String key, Runnable onPress) {
        return addRenderableWidget(new OreUIBlockTextButton(x, y, width, height, blockLabel(blockId, key), OreUIAtlas.ButtonStyle.SECONDARY, button -> onPress.run())
                .iconLeft()
                .iconSize(10)
                .iconPadding(4)
                .icon(stackFromBlockId(blockId)));
    }

    private void bindBlockButton(@Nullable OreUIBlockTextButton button, @Nullable ResourceLocation blockId, String key) {
        if (button == null)
            return;
        button.label(blockLabel(blockId, key));
        button.icon(stackFromBlockId(blockId));
    }

    private static ItemStack stackFromBlockId(@Nullable ResourceLocation blockId) {
        if (blockId == null || !BuiltInRegistries.BLOCK.containsKey(blockId))
            return ItemStack.EMPTY;
        return new ItemStack(BuiltInRegistries.BLOCK.get(blockId));
    }

    private void addStepperPair(int stepperX, int stepperY, Runnable onMinus, Runnable onPlus, String fieldKey) {
        final OreUITextButton minusButton = addOreButton(stepperX, stepperY, 18, 18, Component.literal("-"), onMinus);
        final OreUITextButton plusButton = addOreButton(stepperX + 22, stepperY, 18, 18, Component.literal("+"), onPlus);
        explain(minusButton, fieldKey);
        explain(plusButton, fieldKey);
        this.mainStepperButtons.add(minusButton);
        this.mainStepperButtons.add(plusButton);
    }

    private void explain(AbstractWidget widget, String key) {
        explain(widget, "gui.mobexpansion.mob_spawner." + key, "gui.mobexpansion.mob_spawner." + key + ".tooltip");
    }

    private void explain(AbstractWidget widget, String titleKey, String bodyKey) {
        widget.setTooltip(Tooltip.create(Component.translatable(titleKey)
                .append("\n")
                .append(Component.translatable(bodyKey).withStyle(ChatFormatting.GRAY))));
    }

    private void explain(AbstractWidget widget, Component title, String bodyKey) {
        widget.setTooltip(Tooltip.create(title.copy()
                .append("\n")
                .append(Component.translatable(bodyKey).withStyle(ChatFormatting.GRAY))));
    }

    private void updateSpawnModeTooltip() {
        if (this.spawnModeButton == null)
            return;
        final String suffix = this.workingSpawnMode.name().toLowerCase(Locale.ROOT);
        explain(this.spawnModeButton, "gui.mobexpansion.mob_spawner.spawn_mode." + suffix, "gui.mobexpansion.mob_spawner.spawn_mode." + suffix + ".tooltip");
    }

    private OreUIEditText createEditBox(int boxX, int boxY, int boxWidth, int boxHeight, Component message) {
        return new OreUIEditText(this.font, boxX, boxY, boxWidth, boxHeight, message).maxLength(64);
    }

    private void openBlockPicker(BlockPickTarget target) {
        applySelectedFields();
        this.blockPickTarget = target;
        this.blockPickerOverlay = new ItemPickerOverlay(
                this,
                this.font,
                selectedId -> {
                    if (this.blockPickTarget == BlockPickTarget.UNDER)
                        this.blockUnder = selectedId;
                    else if (this.blockPickTarget == BlockPickTarget.AROUND)
                        this.blockAround = selectedId;
                    else
                        this.maskState = MobSpawnerBlockEntity.maskStateFromBlockId(selectedId);
                    this.maskPropScrollOffset = 0;
                    closeBlockPicker();
                },
                this::closeBlockPicker,
                true
        );
        rebuildWidgets();
    }

    private void closeBlockPicker() {
        this.blockPickerOverlay = null;
        this.blockPickTarget = null;
        rebuildWidgets();
    }

    private void setActiveTab(Tab tab) {
        applySelectedFields();
        this.activeTab = tab;
        updateTabButtons();
        updateCenterVisibility();
    }

    private void updateTabButtons() {
        if (this.centerTabs != null)
            this.centerTabs.selected(this.activeTab.ordinal());
    }

    private void updateCenterVisibility() {
        final boolean mainVisible = this.activeTab == Tab.MAIN && selectedEntry() != null;
        final boolean visualVisible = this.activeTab == Tab.VISUAL && this.blockPickerOverlay == null;
        setShown(this.intervalBox, mainVisible);
        setShown(this.countMinBox, mainVisible);
        setShown(this.countMaxBox, mainVisible);
        setShown(this.maxSimultaneousBox, mainVisible);
        setShown(this.activationRadiusBox, mainVisible);
        setShown(this.spawnRadiusBox, mainVisible);
        setShown(this.spawnYMinBox, mainVisible);
        setShown(this.spawnYMaxBox, mainVisible);
        setShown(this.enabledButton, mainVisible);
        for (OreUITextButton stepperButton : this.mainStepperButtons)
            setShown(stepperButton, mainVisible);
        if (this.maskButton != null)
            setShown(this.maskButton, visualVisible);
        if (this.clearMaskButton != null)
            setShown(this.clearMaskButton, visualVisible);
        if (visualVisible)
            rebuildMaskPropertyButtons();
        else
            hideMaskPropertyButtons();
    }

    private static void setShown(AbstractWidget widget, boolean shown) {
        widget.visible = shown;
        widget.active = shown;
    }

    @Nullable
    private ResourceLocation maskBlockId() {
        return this.maskState == null ? null : BuiltInRegistries.BLOCK.getKey(this.maskState.getBlock());
    }

    private void hideMaskPropertyButtons() {
        for (AbstractWidget widget : this.maskPropertyWidgets)
            setShown(widget, false);
    }

    private void rebuildMaskPropertyButtons() {
        for (AbstractWidget widget : this.maskPropertyWidgets)
            removeWidget(widget);
        this.maskPropertyWidgets.clear();
        if (this.maskState == null || this.activeTab != Tab.VISUAL || this.blockPickerOverlay != null)
            return;
        final List<Property<?>> properties = List.copyOf(this.maskState.getProperties());
        this.maskPropScrollOffset = Mth.clamp(this.maskPropScrollOffset, 0, maxMaskPropScroll(properties.size()));
        final int listLeft = maskPropListLeft();
        final int listTop = this.topPos + MASK_PROP_TOP + MASK_PROP_PAD;
        final int rowWidth = maskPropListWidth();
        final int visible = maskPropVisibleRows();
        final int endIndex = Math.min(properties.size(), this.maskPropScrollOffset + visible);
        for (int index = this.maskPropScrollOffset; index < endIndex; index++) {
            final Property<?> property = properties.get(index);
            final int row = index - this.maskPropScrollOffset;
            final int rowY = listTop + row * (MASK_PROP_BUTTON_H + MASK_PROP_GAP);
            if (property instanceof BooleanProperty booleanProperty) {
                final boolean value = this.maskState.getValue(booleanProperty);
                final OreUIToggleButton toggle = addRenderableWidget(new OreUIToggleButton(
                        listLeft,
                        rowY,
                        rowWidth,
                        Component.literal(property.getName()),
                        value,
                        button -> setMaskBooleanProperty(booleanProperty, button.isToggled())
                ));
                explain(toggle, Component.literal(property.getName()), "gui.mobexpansion.mob_spawner.mask.cycle_hint");
                this.maskPropertyWidgets.add(toggle);
            } else {
                final OreUITextButton cycleButton = addOreButton(
                        listLeft,
                        rowY,
                        rowWidth,
                        MASK_PROP_BUTTON_H,
                        maskPropertyLabel(property),
                        () -> cycleMaskProperty(property, Screen.hasShiftDown())
                );
                explain(cycleButton, maskPropertyLabel(property), "gui.mobexpansion.mob_spawner.mask.cycle_hint");
                this.maskPropertyWidgets.add(cycleButton);
            }
        }
    }

    private void setMaskBooleanProperty(BooleanProperty property, boolean value) {
        if (this.maskState == null || !this.maskState.hasProperty(property))
            return;
        this.maskState = this.maskState.setValue(property, value);
        rebuildMaskPropertyButtons();
    }

    private int maskPropListLeft() {
        return this.leftPos + CENTER_X + MASK_PROP_PAD;
    }

    private int maskPropListWidth() {
        final int panelWidth = CENTER_W - MASK_PROP_PAD * 2;
        return needsMaskPropScrollbar() ? panelWidth - SCROLLBAR_WIDTH - 4 : panelWidth;
    }

    private int maskPropVisibleRows() {
        final int listHeight = MASK_PROP_BOTTOM - MASK_PROP_TOP - MASK_PROP_PAD * 2;
        return Math.max(1, (listHeight + MASK_PROP_GAP) / (MASK_PROP_BUTTON_H + MASK_PROP_GAP));
    }

    private boolean needsMaskPropScrollbar() {
        return this.maskState != null && this.maskState.getProperties().size() > maskPropVisibleRows();
    }

    private int maxMaskPropScroll(int propertyCount) {
        return Math.max(0, propertyCount - maskPropVisibleRows());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Component maskPropertyLabel(Property<?> property) {
        return formatPropertyLabel((Property) property);
    }

    private <T extends Comparable<T>> Component formatPropertyLabel(Property<T> property) {
        return Component.literal(property.getName() + ": " + property.getName(this.maskState.getValue(property)));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void cycleMaskProperty(Property property, boolean backwards) {
        if (this.maskState == null || !this.maskState.hasProperty(property))
            return;
        this.maskState = this.maskState.setValue(property, nextPropertyValue(property, this.maskState.getValue(property), backwards));
        rebuildMaskPropertyButtons();
    }

    private static <T extends Comparable<T>> T nextPropertyValue(Property<T> property, T current, boolean backwards) {
        return backwards
                ? Util.findPreviousInIterable(property.getPossibleValues(), current)
                : Util.findNextInIterable(property.getPossibleValues(), current);
    }

    private boolean isOverMaskPropertyList(double mouseX, double mouseY) {
        final int listLeft = this.leftPos + CENTER_X + MASK_PROP_PAD;
        final int listTop = this.topPos + MASK_PROP_TOP;
        return mouseX >= listLeft && mouseX <= listLeft + CENTER_W - MASK_PROP_PAD * 2
                && mouseY >= listTop && mouseY <= this.topPos + MASK_PROP_BOTTOM;
    }

    private int maskPreviewLeft() {
        return this.leftPos + CENTER_X + 8;
    }

    private int maskPreviewTop() {
        return this.topPos + MASK_PREVIEW_TOP;
    }

    private int maskPreviewWidth() {
        return CENTER_W - 8 * 2;
    }

    @Nullable
    private SpawnerEntityEntry selectedEntry() {
        if (this.workingEntries.isEmpty())
            return null;
        this.selectedSetIndex = Mth.clamp(this.selectedSetIndex, 0, this.workingEntries.size() - 1);
        return this.workingEntries.get(this.selectedSetIndex);
    }

    private void loadCenterFromSelection() {
        this.suppressFieldSync = true;
        final SpawnerEntityEntry entry = selectedEntry();
        if (entry == null) {
            this.suppressFieldSync = false;
            updateCenterVisibility();
            return;
        }
        this.intervalBox.setValue(String.valueOf(entry.getInterval()));
        this.countMinBox.setValue(String.valueOf(entry.getCountMin()));
        this.countMaxBox.setValue(String.valueOf(entry.getCountMax()));
        this.maxSimultaneousBox.setValue(String.valueOf(entry.getMaxSimultaneous()));
        this.activationRadiusBox.setValue(formatFloat(entry.getActivationRadius()));
        this.spawnRadiusBox.setValue(formatFloat(entry.getSpawnRadius()));
        this.spawnYMinBox.setValue(String.valueOf(entry.getSpawnYMin()));
        this.spawnYMaxBox.setValue(String.valueOf(entry.getSpawnYMax()));
        this.enabledButton.setMessage(enabledLabel(entry.isEnabled()));
        this.suppressFieldSync = false;
        updateCenterVisibility();
    }

    private void loadRightFromWorking() {
        if (this.spawnModeButton == null)
            return;
        this.suppressFieldSync = true;
        this.spawnModeButton.setMessage(spawnModeLabel(this.workingSpawnMode));
        updateSpawnModeTooltip();
        this.activationDelayBox.setValue(String.valueOf(this.workingActivationDelay));
        if (this.activationDistSlider != null)
            this.activationDistSlider.values(this.workingActivationDistMin, this.workingActivationDistMax);
        this.checkLosButton.setToggled(this.requireLos);
        this.checkDaylightButton.setToggled(this.requireDaylight);
        this.checkSpectatorButton.setToggled(this.ignoreSpectators);
        this.checkHeightButton.setToggled(this.heightLimit);
        bindBlockButton(this.blockUnderButton, this.blockUnder, "block_under");
        bindBlockButton(this.blockAroundButton, this.blockAround, "block_around");
        bindBlockButton(this.maskButton, maskBlockId(), "mask");
        this.suppressFieldSync = false;
    }

    private void applySelectedFields() {
        if (this.suppressFieldSync || this.blockPickerOverlay != null)
            return;
        final SpawnerEntityEntry entry = selectedEntry();
        if (entry != null && this.intervalBox != null) {
            entry.setInterval(parseInt(this.intervalBox.getValue(), entry.getInterval()));
            entry.setCountMin(parseInt(this.countMinBox.getValue(), entry.getCountMin()));
            entry.setCountMax(parseInt(this.countMaxBox.getValue(), entry.getCountMax()));
            entry.setMaxSimultaneous(parseInt(this.maxSimultaneousBox.getValue(), entry.getMaxSimultaneous()));
            entry.setActivationRadius(parseFloat(this.activationRadiusBox.getValue(), entry.getActivationRadius()));
            entry.setSpawnRadius(parseFloat(this.spawnRadiusBox.getValue(), entry.getSpawnRadius()));
            entry.setSpawnYMin(parseInt(this.spawnYMinBox.getValue(), entry.getSpawnYMin()));
            entry.setSpawnYMax(parseInt(this.spawnYMaxBox.getValue(), entry.getSpawnYMax()));
        }
        if (this.activationDelayBox != null)
            this.workingActivationDelay = parseInt(this.activationDelayBox.getValue(), this.workingActivationDelay);
        if (this.activationDistSlider != null) {
            this.workingActivationDistMin = (float) this.activationDistSlider.low();
            this.workingActivationDistMax = (float) this.activationDistSlider.high();
        }
    }

    private void onSearchChanged(String query) {
        if (this.suppressFieldSync)
            return;
        rebuildFilteredCatalog(query);
        this.pickerScrollOffset = 0;
    }

    private void rebuildFilteredCatalog(String query) {
        this.filteredCatalog.clear();
        final String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        for (ResourceLocation entityId : SpawnerMobCatalog.getSpawnableIds()) {
            if (normalized.isEmpty()
                    || entityId.toString().toLowerCase(Locale.ROOT).contains(normalized)
                    || SpawnerMobCatalog.displayName(entityId).toLowerCase(Locale.ROOT).contains(normalized))
                this.filteredCatalog.add(entityId);
        }
    }

    private boolean containsEntityId(ResourceLocation entityId) {
        for (SpawnerEntityEntry entry : this.workingEntries) {
            if (entry.getEntityId().equals(entityId))
                return true;
        }
        return false;
    }

    private void addEntityToSet(ResourceLocation entityId) {
        if (!SpawnerMobCatalog.isSpawnable(entityId) || containsEntityId(entityId))
            return;
        applySelectedFields();
        final int defaultY = this.menu.getBlockPos().getY() + 1;
        this.workingEntries.add(SpawnerEntityEntry.createDefault(entityId, defaultY));
        this.selectedSetIndex = this.workingEntries.size() - 1;
        ensureSetSelectionVisible();
        loadCenterFromSelection();
    }

    private void removeSelectedEntity() {
        if (this.workingEntries.isEmpty())
            return;
        applySelectedFields();
        this.selectedSetIndex = Mth.clamp(this.selectedSetIndex, 0, this.workingEntries.size() - 1);
        this.workingEntries.remove(this.selectedSetIndex);
        if (this.selectedSetIndex >= this.workingEntries.size())
            this.selectedSetIndex = Math.max(0, this.workingEntries.size() - 1);
        ensureSetSelectionVisible();
        loadCenterFromSelection();
    }

    private void moveSelectedEntity(int direction) {
        if (this.workingEntries.size() < 2)
            return;
        applySelectedFields();
        final int fromIndex = Mth.clamp(this.selectedSetIndex, 0, this.workingEntries.size() - 1);
        final int toIndex = Mth.clamp(fromIndex + direction, 0, this.workingEntries.size() - 1);
        if (fromIndex == toIndex)
            return;
        final SpawnerEntityEntry moved = this.workingEntries.remove(fromIndex);
        this.workingEntries.add(toIndex, moved);
        this.selectedSetIndex = toIndex;
        ensureSetSelectionVisible();
        loadCenterFromSelection();
    }

    private void toggleSelectedEnabled() {
        final SpawnerEntityEntry entry = selectedEntry();
        if (entry == null)
            return;
        applySelectedFields();
        entry.setEnabled(!entry.isEnabled());
        this.enabledButton.setMessage(enabledLabel(entry.isEnabled()));
    }

    private void cycleSpawnMode() {
        this.workingSpawnMode = this.workingSpawnMode.next();
        this.spawnModeButton.setMessage(spawnModeLabel(this.workingSpawnMode));
        updateSpawnModeTooltip();
    }

    private static Component enabledLabel(boolean enabled) {
        return Component.translatable(enabled ? "gui.mobexpansion.mob_spawner.enabled_on" : "gui.mobexpansion.mob_spawner.enabled_off");
    }

    private static Component spawnModeLabel(SpawnMode spawnMode) {
        return Component.translatable("gui.mobexpansion.mob_spawner.spawn_mode." + spawnMode.name().toLowerCase(Locale.ROOT));
    }

    private static Component checkboxLabel(String key) {
        return Component.translatable("gui.mobexpansion.mob_spawner." + key);
    }

    private Component blockLabel(@Nullable ResourceLocation blockId, String fallbackKey) {
        if (blockId == null)
            return Component.translatable("gui.mobexpansion.mob_spawner." + fallbackKey + ".none");
        return BuiltInRegistries.BLOCK.get(blockId).getName();
    }

    private void resetToDefaults() {
        this.workingEntries.clear();
        this.selectedSetIndex = 0;
        this.setScrollOffset = 0;
        this.workingSpawnMode = SpawnMode.STANDARD;
        this.requireLos = false;
        this.requireDaylight = false;
        this.ignoreSpectators = true;
        this.heightLimit = false;
        this.blockUnder = null;
        this.blockAround = null;
        this.maskState = null;
        this.workingActivationDelay = 0;
        this.workingActivationDistMin = 0.0F;
        this.workingActivationDistMax = 16.0F;
        loadCenterFromSelection();
        loadRightFromWorking();
        rebuildMaskPropertyButtons();
    }

    private void saveAndClose() {
        applySelectedFields();
        PacketDistributor.sendToServer(new UpdateMobSpawnerPacket(
                this.menu.getBlockPos(),
                this.workingEntries,
                this.workingSpawnMode,
                this.workingActivationDelay,
                this.requireLos,
                this.requireDaylight,
                this.ignoreSpectators,
                this.heightLimit,
                this.workingActivationDistMin,
                this.workingActivationDistMax,
                this.blockUnder,
                this.blockAround,
                this.maskState
        ));
        onClose();
    }

    private void nudgeSelectedInt(EditBox editBox, int delta, int minValue, int maxValue) {
        if (selectedEntry() == null)
            return;
        final int currentValue = parseInt(editBox.getValue(), minValue);
        editBox.setValue(String.valueOf(Mth.clamp(currentValue + delta, minValue, maxValue)));
        applySelectedFields();
    }

    private void nudgeSelectedFloat(EditBox editBox, float delta, float minValue, float maxValue) {
        if (selectedEntry() == null)
            return;
        final float currentValue = parseFloat(editBox.getValue(), minValue);
        editBox.setValue(formatFloat(Mth.clamp(currentValue + delta, minValue, maxValue)));
        applySelectedFields();
    }

    private void nudgeGlobalInt(EditBox editBox, int delta, int minValue, int maxValue) {
        final int currentValue = parseInt(editBox.getValue(), minValue);
        final int nextValue = Mth.clamp(currentValue + delta, minValue, maxValue);
        editBox.setValue(String.valueOf(nextValue));
        this.workingActivationDelay = nextValue;
    }

    private static String formatFloat(float value) {
        if (Math.abs(value - Math.round(value)) < 0.001F)
            return String.valueOf(Math.round(value));
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static float parseFloat(String text, float fallback) {
        try {
            return Float.parseFloat(text.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private void ensureSetSelectionVisible() {
        if (this.workingEntries.isEmpty()) {
            this.setScrollOffset = 0;
            return;
        }
        this.selectedSetIndex = Mth.clamp(this.selectedSetIndex, 0, this.workingEntries.size() - 1);
        if (this.selectedSetIndex < this.setScrollOffset)
            this.setScrollOffset = this.selectedSetIndex;
        if (this.selectedSetIndex >= this.setScrollOffset + SET_VISIBLE_ROWS)
            this.setScrollOffset = this.selectedSetIndex - SET_VISIBLE_ROWS + 1;
        this.setScrollOffset = Mth.clamp(this.setScrollOffset, 0, maxSetScroll());
    }

    private int listInnerLeft() {
        return this.leftPos + LEFT_X + 8;
    }

    private int listInnerRight() {
        return this.leftPos + LEFT_X + LEFT_W - 8;
    }

    private int listContentWidth() {
        return listInnerRight() - listInnerLeft() - SCROLLBAR_WIDTH - SCROLLBAR_GAP;
    }

    private int setListTop() {
        return this.topPos + 50;
    }

    private int setListHeight() {
        return SET_VISIBLE_ROWS * ROW_HEIGHT;
    }

    private int maxSetScroll() {
        return Math.max(0, this.workingEntries.size() - SET_VISIBLE_ROWS);
    }

    private boolean needsSetScrollbar() {
        return this.workingEntries.size() > SET_VISIBLE_ROWS;
    }

    private int pickerListTop() {
        return this.topPos + 202;
    }

    private int pickerListHeight() {
        return PICKER_VISIBLE_ROWS * ROW_HEIGHT;
    }

    private int maxPickerScroll() {
        return Math.max(0, this.filteredCatalog.size() - PICKER_VISIBLE_ROWS);
    }

    private boolean needsPickerScrollbar() {
        return this.filteredCatalog.size() > PICKER_VISIBLE_ROWS;
    }

    private int scrollbarLeft() {
        return listInnerRight() - SCROLLBAR_WIDTH;
    }

    private int setScrollbarThumbHeight() {
        final int trackHeight = setListHeight();
        return Mth.clamp(trackHeight * SET_VISIBLE_ROWS / Math.max(1, this.workingEntries.size()), 14, trackHeight);
    }

    private int pickerScrollbarThumbHeight() {
        final int trackHeight = pickerListHeight();
        return Mth.clamp(trackHeight * PICKER_VISIBLE_ROWS / Math.max(1, this.filteredCatalog.size()), 14, trackHeight);
    }

    private int setScrollbarThumbTop() {
        final int travel = setListHeight() - setScrollbarThumbHeight();
        if (maxSetScroll() <= 0)
            return setListTop();
        return setListTop() + (int) ((long) this.setScrollOffset * travel / maxSetScroll());
    }

    private int pickerScrollbarThumbTop() {
        final int travel = pickerListHeight() - pickerScrollbarThumbHeight();
        if (maxPickerScroll() <= 0)
            return pickerListTop();
        return pickerListTop() + (int) ((long) this.pickerScrollOffset * travel / maxPickerScroll());
    }

    private int setRowIndexAt(double mouseX, double mouseY) {
        if (mouseX < listInnerLeft() || mouseX > listInnerLeft() + listContentWidth() || mouseY < setListTop() || mouseY > setListTop() + setListHeight())
            return -1;
        final int rowIndex = this.setScrollOffset + (int) ((mouseY - setListTop()) / ROW_HEIGHT);
        return rowIndex >= 0 && rowIndex < this.workingEntries.size() ? rowIndex : -1;
    }

    private int pickerRowIndexAt(double mouseX, double mouseY) {
        if (mouseX < listInnerLeft() || mouseX > listInnerLeft() + listContentWidth() || mouseY < pickerListTop() || mouseY > pickerListTop() + pickerListHeight())
            return -1;
        final int rowIndex = this.pickerScrollOffset + (int) ((mouseY - pickerListTop()) / ROW_HEIGHT);
        return rowIndex >= 0 && rowIndex < this.filteredCatalog.size() ? rowIndex : -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        syncFitScale();
        mouseX = GuiFitScale.unscaleX(mouseX);
        mouseY = GuiFitScale.unscaleY(mouseY);
        if (this.blockPickerOverlay != null) {
            if (!this.blockPickerOverlay.mouseClicked(mouseX, mouseY, button))
                closeBlockPicker();
            else
                super.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        if (button == 0) {
            if (needsSetScrollbar() && mouseX >= scrollbarLeft() && mouseX <= scrollbarLeft() + SCROLLBAR_WIDTH
                    && mouseY >= setListTop() && mouseY <= setListTop() + setListHeight()) {
                this.draggingSetScrollbar = true;
                updateSetScrollFromThumb(mouseY);
                return true;
            }
            if (needsPickerScrollbar() && mouseX >= scrollbarLeft() && mouseX <= scrollbarLeft() + SCROLLBAR_WIDTH
                    && mouseY >= pickerListTop() && mouseY <= pickerListTop() + pickerListHeight()) {
                this.draggingPickerScrollbar = true;
                updatePickerScrollFromThumb(mouseY);
                return true;
            }
            final int setIndex = setRowIndexAt(mouseX, mouseY);
            if (setIndex >= 0) {
                applySelectedFields();
                this.selectedSetIndex = setIndex;
                this.dragFromIndex = setIndex;
                this.isDraggingRow = false;
                this.dragPointerY = mouseY;
                loadCenterFromSelection();
                return true;
            }
            final int pickerIndex = pickerRowIndexAt(mouseX, mouseY);
            if (pickerIndex >= 0) {
                addEntityToSet(this.filteredCatalog.get(pickerIndex));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        syncFitScale();
        mouseX = GuiFitScale.unscaleX(mouseX);
        mouseY = GuiFitScale.unscaleY(mouseY);
        dragX = GuiFitScale.unscaleDelta(dragX);
        dragY = GuiFitScale.unscaleDelta(dragY);
        if (this.blockPickerOverlay != null)
            return this.blockPickerOverlay.mouseDragged(mouseX, mouseY, button);
        if (this.draggingSetScrollbar) {
            updateSetScrollFromThumb(mouseY);
            return true;
        }
        if (this.draggingPickerScrollbar) {
            updatePickerScrollFromThumb(mouseY);
            return true;
        }
        if (this.dragFromIndex >= 0 && button == 0) {
            this.isDraggingRow = true;
            this.dragPointerY = mouseY;
            final int hoverIndex = setRowIndexAt(mouseX, mouseY);
            if (hoverIndex >= 0 && hoverIndex != this.dragFromIndex) {
                final SpawnerEntityEntry moved = this.workingEntries.remove(this.dragFromIndex);
                this.workingEntries.add(hoverIndex, moved);
                this.dragFromIndex = hoverIndex;
                this.selectedSetIndex = hoverIndex;
                ensureSetSelectionVisible();
            }
            return true;
        }
        if (this.activationDistSlider != null && this.activationDistSlider.isDragging())
            return this.activationDistSlider.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        if (getFocused() instanceof OreUIRangedSlider ranged && ranged.isDragging())
            return ranged.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        if (getFocused() instanceof OreUISlider slider && slider.isDragging())
            return slider.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        syncFitScale();
        mouseX = GuiFitScale.unscaleX(mouseX);
        mouseY = GuiFitScale.unscaleY(mouseY);
        if (this.blockPickerOverlay != null)
            this.blockPickerOverlay.mouseReleased(mouseX, mouseY, button);
        if (this.activationDistSlider != null && this.activationDistSlider.isDragging())
            this.activationDistSlider.mouseReleased(mouseX, mouseY, button);
        this.draggingSetScrollbar = false;
        this.draggingPickerScrollbar = false;
        this.isDraggingRow = false;
        this.dragFromIndex = -1;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        syncFitScale();
        mouseX = GuiFitScale.unscaleX(mouseX);
        mouseY = GuiFitScale.unscaleY(mouseY);
        if (this.blockPickerOverlay != null)
            return this.blockPickerOverlay.mouseScrolled(mouseX, mouseY, scrollY);
        if (this.activeTab == Tab.VISUAL && this.maskState != null && isOverMaskPropertyList(mouseX, mouseY)) {
            final int propertyCount = this.maskState.getProperties().size();
            this.maskPropScrollOffset = Mth.clamp(this.maskPropScrollOffset - (int) Math.signum(scrollY), 0, maxMaskPropScroll(propertyCount));
            rebuildMaskPropertyButtons();
            return true;
        }
        if (mouseX >= listInnerLeft() && mouseX <= scrollbarLeft() + SCROLLBAR_WIDTH
                && mouseY >= setListTop() && mouseY <= setListTop() + setListHeight()) {
            this.setScrollOffset = Mth.clamp(this.setScrollOffset - (int) Math.signum(scrollY), 0, maxSetScroll());
            return true;
        }
        if (mouseX >= listInnerLeft() && mouseX <= scrollbarLeft() + SCROLLBAR_WIDTH
                && mouseY >= pickerListTop() && mouseY <= pickerListTop() + pickerListHeight()) {
            this.pickerScrollOffset = Mth.clamp(this.pickerScrollOffset - (int) Math.signum(scrollY), 0, maxPickerScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.blockPickerOverlay != null && this.blockPickerOverlay.keyPressed(keyCode, scanCode, modifiers))
            return true;
        if (isTextFieldFocused()) {
            if (getFocused() != null && getFocused().keyPressed(keyCode, scanCode, modifiers))
                return true;
            if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode))
                return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean isTextFieldFocused() {
        return getFocused() instanceof EditBox editBox && editBox.isFocused();
    }

    private void updateSetScrollFromThumb(double mouseY) {
        final int travel = setListHeight() - setScrollbarThumbHeight();
        if (travel <= 0 || maxSetScroll() <= 0) {
            this.setScrollOffset = 0;
            return;
        }
        final double relative = (mouseY - setListTop() - setScrollbarThumbHeight() / 2.0D) / travel;
        this.setScrollOffset = Mth.clamp((int) Math.round(relative * maxSetScroll()), 0, maxSetScroll());
    }

    private void updatePickerScrollFromThumb(double mouseY) {
        final int travel = pickerListHeight() - pickerScrollbarThumbHeight();
        if (travel <= 0 || maxPickerScroll() <= 0) {
            this.pickerScrollOffset = 0;
            return;
        }
        final double relative = (mouseY - pickerListTop() - pickerScrollbarThumbHeight() / 2.0D) / travel;
        this.pickerScrollOffset = Mth.clamp((int) Math.round(relative * maxPickerScroll()), 0, maxPickerScroll());
    }

    @Override
    protected void containerTick() {
        for (LivingEntity previewEntity : this.previewByEntityId.values()) {
            previewEntity.tickCount = (int) (Blaze3D.getTime() * 30);
            previewEntity.setDeltaMovement(0.0D, 0.0D, 0.0D);
        }
    }

    @Nullable
    private LivingEntity getPreviewEntity(ResourceLocation entityId) {
        if (this.minecraft == null || this.minecraft.level == null || entityId == null)
            return null;
        final LivingEntity cached = this.previewByEntityId.get(entityId);
        if (cached != null)
            return cached;
        final Mob createdMob = SpawnerMobCatalog.tryCreateMob(entityId, this.minecraft.level);
        if (createdMob == null)
            return null;
        createdMob.setNoAi(true);
        createdMob.setSilent(true);
        createdMob.setYBodyRot(180.0F);
        createdMob.setYHeadRot(180.0F);
        createdMob.setYRot(180.0F);
        createdMob.yBodyRotO = 180.0F;
        createdMob.yHeadRotO = 180.0F;
        createdMob.yRotO = 180.0F;
        this.previewByEntityId.put(entityId, createdMob);
        return createdMob;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        final int left = this.leftPos;
        final int top = this.topPos;
        OreUIPanel.renderDark(guiGraphics, left, top, imageWidth, imageHeight);
        OreUIPanel.renderLight(guiGraphics,left + LEFT_X, top + CONTENT_TOP, LEFT_W, CONTENT_H);
        OreUIPanel.renderLight(guiGraphics,left + CENTER_X, top + CONTENT_TOP, CENTER_W, CONTENT_H);
        OreUIPanel.renderLight(guiGraphics,left + RIGHT_X, top + CONTENT_TOP, RIGHT_W, CONTENT_H);
        if (this.blockPickerOverlay != null)
            this.blockPickerOverlay.render(guiGraphics, mouseX, mouseY);
        else if (this.activeTab == Tab.VISUAL)
            renderMaskPanels(guiGraphics);
    }

    private void renderMaskPanels(GuiGraphics guiGraphics) {
        final int previewLeft = maskPreviewLeft();
        final int previewTop = maskPreviewTop();
        final int previewWidth = maskPreviewWidth();
        OreUIPanel.renderDark(guiGraphics, previewLeft, previewTop, previewWidth, MASK_PREVIEW_H);
        final int propLeft = this.leftPos + CENTER_X + 8;
        final int propTop = this.topPos + MASK_PROP_TOP;
        final int propWidth = CENTER_W - 8 * 2;
        final int propHeight = MASK_PROP_BOTTOM - MASK_PROP_TOP;
        OreUIPanel.renderDark(guiGraphics, propLeft, propTop, propWidth, propHeight);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.blockPickerOverlay != null)
            return;
        guiGraphics.drawString(this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2, this.titleLabelY, COLOR_TEXT, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.mobexpansion.mob_spawner.entity_set"), LEFT_X + 8, CONTENT_TOP + 8, COLOR_TEXT, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.mobexpansion.mob_spawner.mob_picker"), LEFT_X + 8, 168, COLOR_TEXT, false);

        if (this.activeTab == Tab.MAIN) {
            if (selectedEntry() == null)
                guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mobexpansion.mob_spawner.select_entity"), CENTER_X + CENTER_W / 2, CONTENT_TOP + CONTENT_H / 2, COLOR_MUTED);
            else
                renderMainLabels(guiGraphics);
        } else if (this.activeTab == Tab.VISUAL) {
            renderVisualLabels(guiGraphics);
        } else {
            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mobexpansion.mob_spawner.coming_soon"), CENTER_X + CENTER_W / 2, CONTENT_TOP + CONTENT_H / 2, COLOR_MUTED);
        }
        renderRightLabels(guiGraphics);
    }

    private void renderMainLabels(GuiGraphics guiGraphics) {
        final int labelX = CENTER_X + 12;
        guiGraphics.drawString(this.font, Component.translatable("gui.mobexpansion.mob_spawner.interval"), labelX, 74, COLOR_MUTED, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.mobexpansion.mob_spawner.count_min"), labelX, 100, COLOR_MUTED, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.mobexpansion.mob_spawner.count_max"), labelX, 126, COLOR_MUTED, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.mobexpansion.mob_spawner.max_simultaneous"), labelX, 152, COLOR_MUTED, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.mobexpansion.mob_spawner.activation_radius"), labelX, 178, COLOR_MUTED, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.mobexpansion.mob_spawner.spawn_radius"), labelX, 204, COLOR_MUTED, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.mobexpansion.mob_spawner.spawn_y_min"), labelX, 230, COLOR_MUTED, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.mobexpansion.mob_spawner.spawn_y_max"), labelX, 256, COLOR_MUTED, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.mobexpansion.mob_spawner.enabled"), labelX, 280, COLOR_MUTED, false);
    }

    private void renderVisualLabels(GuiGraphics guiGraphics) {
        final int labelX = CENTER_X + 12;
        guiGraphics.drawString(this.font, Component.translatable("gui.mobexpansion.mob_spawner.mask"), labelX, 64, COLOR_TEXT, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.mobexpansion.mob_spawner.mask.hint"), labelX, 112, COLOR_MUTED, false);
        if (this.maskState == null)
            return;
        if (this.maskState.getProperties().isEmpty()) {
            final int propLeft =  CENTER_X + MASK_PROP_PAD;
            final int propTop =  MASK_PROP_TOP;
            final int propWidth = CENTER_W - MASK_PROP_PAD * 2;
            final int propHeight = MASK_PROP_BOTTOM - MASK_PROP_TOP;
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.translatable("gui.mobexpansion.mob_spawner.mask.no_properties"),
                    propLeft + propWidth / 2,
                    propTop + propHeight / 2,
                    COLOR_MUTED
            );
        }
    }

    private void renderMaskPreview(GuiGraphics guiGraphics) {
        if (this.activeTab != Tab.VISUAL || this.blockPickerOverlay != null)
            return;
        final int previewLeft = maskPreviewLeft();
        final int previewTop = maskPreviewTop();
        final int previewWidth = maskPreviewWidth();
        if (this.maskState != null) {
            final int previewSize = Math.min(previewWidth - 16, MASK_PREVIEW_H - 16);
            final int blockX = previewLeft + (previewWidth - previewSize) / 2;
            final int blockY = previewTop + (MASK_PREVIEW_H - previewSize) / 2;
            final float yaw = (float) (Blaze3D.getTime() * MASK_PREVIEW_SPIN_DEG_PER_SEC);
            GuiBlockPreview.render(guiGraphics, this.maskState, blockX, blockY, previewSize, yaw);
        } else {
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.translatable("gui.mobexpansion.mob_spawner.mask.none"),
                    previewLeft + previewWidth / 2,
                    previewTop + MASK_PREVIEW_H / 2 - 4,
                    COLOR_MUTED
            );
        }
        if (this.maskState != null && needsMaskPropScrollbar()) {
            final int propLeft = this.leftPos + CENTER_X + MASK_PROP_PAD;
            final int propTop = this.topPos + MASK_PROP_TOP;
            final int propWidth = CENTER_W - MASK_PROP_PAD * 2;
            final int propHeight = MASK_PROP_BOTTOM - MASK_PROP_TOP;
            renderMaskPropScrollbar(guiGraphics, propLeft, propTop, propWidth, propHeight);
        }
    }

    private void renderMaskPropScrollbar(GuiGraphics guiGraphics, int panelLeft, int panelTop, int panelWidth, int panelHeight) {
        final int propertyCount = this.maskState.getProperties().size();
        final int visible = maskPropVisibleRows();
        final int barLeft = panelLeft + panelWidth - SCROLLBAR_WIDTH - 2;
        final int barTop = panelTop + MASK_PROP_PAD;
        final int barHeight = panelHeight - MASK_PROP_PAD * 2;
        final int thumbHeight = Mth.clamp(barHeight * visible / Math.max(1, propertyCount), 18, barHeight);
        final int travel = Math.max(1, barHeight - thumbHeight);
        final int thumbTop = barTop + (int) ((long) this.maskPropScrollOffset * travel / Math.max(1, maxMaskPropScroll(propertyCount)));
        renderScrollbar(guiGraphics, barLeft, barTop, barHeight, thumbTop, thumbHeight);
    }

    private void renderRightLabels(GuiGraphics guiGraphics) {
        final int labelX = RIGHT_X + 8;
        guiGraphics.drawString(this.font, Component.translatable("gui.mobexpansion.mob_spawner.properties"), labelX, CONTENT_TOP + 8, COLOR_TEXT, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.mobexpansion.mob_spawner.spawn_mode"), labelX, 56, COLOR_MUTED, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.mobexpansion.mob_spawner.activation_delay"), labelX, 96, COLOR_MUTED, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.mobexpansion.mob_spawner.activation_distance"), labelX, 226, COLOR_MUTED, false);
        final String distRange = formatFloat(this.workingActivationDistMin) + " - " + formatFloat(this.workingActivationDistMax);
        guiGraphics.drawString(this.font, distRange, RIGHT_X + RIGHT_W - 8 - this.font.width(distRange), 226, COLOR_TEXT, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.mobexpansion.mob_spawner.block_requirements"), labelX, 272, COLOR_MUTED, false);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        final int rawMouseX = mouseX;
        final int rawMouseY = mouseY;
        syncFitScale();
        this.renderTransparentBackground(guiGraphics);
        final int fitMouseX = GuiFitScale.unscaleMouseX(rawMouseX);
        final int fitMouseY = GuiFitScale.unscaleMouseY(rawMouseY);
        try {
            GuiFitScale.push(guiGraphics, this.width, this.height, fitGuiWidth(), fitGuiHeight());
            super.render(guiGraphics, fitMouseX, fitMouseY, partialTick);
            if (this.blockPickerOverlay == null) {
                renderSetList(guiGraphics);
                renderPickerList(guiGraphics);
                renderMaskPreview(guiGraphics);
            }
        } finally {
            GuiFitScale.pop(guiGraphics);
        }
        if (this.blockPickerOverlay != null)
            return;
        renderListHoverTooltip(guiGraphics, fitMouseX, fitMouseY, rawMouseX, rawMouseY);
        renderTooltip(guiGraphics, rawMouseX, rawMouseY);
    }

    private void renderListHoverTooltip(GuiGraphics guiGraphics, int fitMouseX, int fitMouseY, int rawMouseX, int rawMouseY) {
        if (this.isDraggingRow)
            return;
        final int setIndex = setRowIndexAt(fitMouseX, fitMouseY);
        if (setIndex >= 0) {
            final ResourceLocation entityId = this.workingEntries.get(setIndex).getEntityId();
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal(SpawnerMobCatalog.displayName(entityId)),
                    Component.translatable("gui.mobexpansion.mob_spawner.entity_set.row.tooltip").withStyle(ChatFormatting.GRAY)
            ), rawMouseX, rawMouseY);
            return;
        }
        final int pickerIndex = pickerRowIndexAt(fitMouseX, fitMouseY);
        if (pickerIndex < 0)
            return;
        final ResourceLocation entityId = this.filteredCatalog.get(pickerIndex);
        final String bodyKey = containsEntityId(entityId)
                ? "gui.mobexpansion.mob_spawner.picker.row.in_set.tooltip"
                : "gui.mobexpansion.mob_spawner.picker.row.tooltip";
        guiGraphics.renderComponentTooltip(this.font, List.of(
                Component.literal(SpawnerMobCatalog.displayName(entityId)),
                Component.translatable(bodyKey).withStyle(ChatFormatting.GRAY)
        ), rawMouseX, rawMouseY);
    }

    private void renderSetList(GuiGraphics guiGraphics) {
        final int endIndex = Math.min(this.workingEntries.size(), this.setScrollOffset + SET_VISIBLE_ROWS);
        if (this.isDraggingRow && this.dragFromIndex >= 0 && this.dragFromIndex < this.workingEntries.size()) {
            guiGraphics.fill(listInnerLeft(), setListTop(), listInnerLeft() + listContentWidth(), setListTop() + setListHeight(), 0xFF181818);
            for (int entityIndex = this.setScrollOffset; entityIndex < endIndex; entityIndex++) {
                if (entityIndex == this.dragFromIndex)
                    continue;
                final int rowTop = setListTop() + (entityIndex - this.setScrollOffset) * ROW_HEIGHT;
                renderEntityRow(guiGraphics, this.workingEntries.get(entityIndex).getEntityId(), listInnerLeft(), rowTop, listContentWidth(), true, false);
            }
            final int floatingY = Mth.clamp((int) this.dragPointerY - DRAG_ROW_HEIGHT / 2, setListTop() - 4, setListTop() + setListHeight() - DRAG_ROW_HEIGHT + 4);
            renderEntityRow(guiGraphics, this.workingEntries.get(this.dragFromIndex).getEntityId(), listInnerLeft() - 3, floatingY, listContentWidth() + 6, false, true);
        } else {
            for (int entityIndex = this.setScrollOffset; entityIndex < endIndex; entityIndex++) {
                final int rowTop = setListTop() + (entityIndex - this.setScrollOffset) * ROW_HEIGHT;
                renderEntityRow(guiGraphics, this.workingEntries.get(entityIndex).getEntityId(), listInnerLeft(), rowTop, listContentWidth(), !this.workingEntries.get(entityIndex).isEnabled(), entityIndex == this.selectedSetIndex);
            }
        }
        if (this.workingEntries.isEmpty())
            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mobexpansion.mob_spawner.entity_set_empty"), listInnerLeft() + listContentWidth() / 2, setListTop() + setListHeight() / 2 - 4, COLOR_MUTED);
        if (needsSetScrollbar())
            renderScrollbar(guiGraphics, scrollbarLeft(), setListTop(), setListHeight(), setScrollbarThumbTop(), setScrollbarThumbHeight());
    }

    private void renderPickerList(GuiGraphics guiGraphics) {
        final int endIndex = Math.min(this.filteredCatalog.size(), this.pickerScrollOffset + PICKER_VISIBLE_ROWS);
        for (int catalogIndex = this.pickerScrollOffset; catalogIndex < endIndex; catalogIndex++) {
            final int rowTop = pickerListTop() + (catalogIndex - this.pickerScrollOffset) * ROW_HEIGHT;
            final ResourceLocation entityId = this.filteredCatalog.get(catalogIndex);
            renderEntityRow(guiGraphics, entityId, listInnerLeft(), rowTop, listContentWidth(), containsEntityId(entityId), false);
        }
        if (this.filteredCatalog.isEmpty())
            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mobexpansion.mob_spawner.picker_empty"), listInnerLeft() + listContentWidth() / 2, pickerListTop() + pickerListHeight() / 2 - 4, COLOR_MUTED);
        if (needsPickerScrollbar())
            renderScrollbar(guiGraphics, scrollbarLeft(), pickerListTop(), pickerListHeight(), pickerScrollbarThumbTop(), pickerScrollbarThumbHeight());
    }

    private void renderEntityRow(GuiGraphics guiGraphics, ResourceLocation entityId, int rowLeft, int rowTop, int rowWidth, boolean dimmed, boolean selectedOrEnlarged) {
        final boolean enlarged = selectedOrEnlarged && this.isDraggingRow;
        final int currentRowHeight = enlarged ? DRAG_ROW_HEIGHT : ROW_HEIGHT;
        if (enlarged) {
            guiGraphics.fill(rowLeft - 1, rowTop - 1, rowLeft + rowWidth + 1, rowTop + currentRowHeight, COLOR_ACCENT);
            guiGraphics.fill(rowLeft, rowTop, rowLeft + rowWidth, rowTop + currentRowHeight - 1, 0xFF2F3A2F);
        } else if (selectedOrEnlarged) {
            guiGraphics.fill(rowLeft, rowTop, rowLeft + rowWidth, rowTop + currentRowHeight - 1, COLOR_SELECTED);
        } else if (dimmed) {
            guiGraphics.fill(rowLeft, rowTop, rowLeft + rowWidth, rowTop + currentRowHeight - 1, 0x33121820);
        }

        final int iconX = rowLeft + 2;
        final int iconY = rowTop + (currentRowHeight - ICON_SIZE) / 2;
        renderMobIcon(guiGraphics, entityId, iconX, iconY);

        final int nameX = rowLeft + ICON_SIZE + 6;
        final int nameMaxWidth = Math.max(8, rowWidth - (ICON_SIZE + 8));
        final int textColor = dimmed ? 0xFF6A7080 : COLOR_TEXT;
        final String displayName = this.font.plainSubstrByWidth(SpawnerMobCatalog.displayName(entityId), nameMaxWidth);
        guiGraphics.drawString(this.font, displayName, nameX, rowTop + (currentRowHeight - 8) / 2, textColor, false);
    }

    private void renderMobIcon(GuiGraphics guiGraphics, ResourceLocation entityId, int iconX, int iconY) {
        final LivingEntity previewEntity = getPreviewEntity(entityId);
        if (previewEntity == null)
            return;
        final float entityWidth = Math.max(0.4F, previewEntity.getBbWidth());
        final float entityHeight = Math.max(0.4F, previewEntity.getBbHeight());
        final float scale = Mth.clamp(ICON_SIZE / Math.max(entityWidth, entityHeight) * 0.72F, 6.0F, 14.0F);
        final float centerX = iconX + ICON_SIZE / 2.0F;
        final float centerY = iconY + ICON_SIZE * 0.78F;
        final Vector3f translation = new Vector3f(0.0F, previewEntity.getBbHeight() * 0.5F, 0.0F);
        final Quaternionf pose = new Quaternionf()
                .rotateZ((float) Math.PI)
                .rotateY((float) Math.toRadians(-35.0D))
                .rotateX((float) Math.toRadians(25.0D));
        guiGraphics.enableScissor(iconX, iconY, iconX + ICON_SIZE, iconY + ICON_SIZE);
        InventoryScreen.renderEntityInInventory(guiGraphics, centerX, centerY, scale, translation, pose, null, previewEntity);
        guiGraphics.disableScissor();
    }

    private void renderScrollbar(GuiGraphics guiGraphics, int barLeft, int barTop, int barHeight, int thumbTop, int thumbHeight) {
        NineSlicePainter.blit(guiGraphics, GuiTextures.SCROLL_GUTTER, barLeft, barTop, SCROLLBAR_WIDTH, barHeight, 3, 1);
        NineSlicePainter.blit(guiGraphics, GuiTextures.SCROLL_HANDLE, barLeft, thumbTop, SCROLLBAR_WIDTH, thumbHeight, 3, 5, 1, 2, 1, 2);
    }
}

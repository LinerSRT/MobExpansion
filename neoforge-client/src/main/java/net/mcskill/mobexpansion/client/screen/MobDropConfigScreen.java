package net.mcskill.mobexpansion.client.screen;

import com.mojang.blaze3d.Blaze3D;
import net.mcskill.mobexpansion.attributes.EntityAttributeConfig;
import net.mcskill.mobexpansion.attributes.EntityAttributeDefaults;
import net.mcskill.mobexpansion.attributes.EntityExtraParametersCatalog;
import net.mcskill.mobexpansion.attributes.ExtraParameterDefinition;
import net.mcskill.mobexpansion.client.screen.oreui.OreUIAtlas;
import net.mcskill.mobexpansion.client.screen.oreui.OreUICheckbox;
import net.mcskill.mobexpansion.client.screen.oreui.OreUIEditText;
import net.mcskill.mobexpansion.client.screen.oreui.OreUIPanel;
import net.mcskill.mobexpansion.client.screen.oreui.OreUISlider;
import net.mcskill.mobexpansion.client.screen.oreui.OreUITabs;
import net.mcskill.mobexpansion.client.screen.oreui.OreUITextButton;
import net.mcskill.mobexpansion.client.screen.oreui.OreUIToggleButton;
import net.mcskill.mobexpansion.client.screen.widget.GuiTextures;
import net.mcskill.mobexpansion.client.screen.widget.NineSlicePainter;
import net.mcskill.mobexpansion.drops.*;
import net.mcskill.mobexpansion.menu.MobDropConfigMenu;
import net.mcskill.mobexpansion.network.UpdateMobDropsPacket;
import net.mcskill.mobexpansion.spawner.SpawnerMobCatalog;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
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
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MobDropConfigScreen extends AbstractContainerScreen<MobDropConfigMenu> {
    private static final int COLOR_ACCENT = 0xFF8df1ff;
    private static final int COLOR_TEXT = 0xFFFEFEFE;
    private static final int COLOR_MUTED = 0xFFAAAAAA;
    private static final int ROW_HEIGHT = 22;
    private static final int DRAG_ROW_HEIGHT = 26;
    private static final int VISIBLE_ROWS = 8;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int CONDITION_ROW_HEIGHT = 22;
    private static final int CONDITION_VISIBLE_ROWS = 8;

    private static final int LEFT_X = 10;
    private static final int LEFT_W = 124;
    private static final int CENTER_X = 142;
    private static final int CENTER_W = 250;
    private static final int RIGHT_X = 400;
    private static final int RIGHT_W = 168;
    private static final int CONTENT_TOP = 30;
    private static final int CONTENT_H = 268;
    private static final int LIST_HEADER_Y = 58;
    private static final int LIST_BODY_Y = 74;
    private static final int RIGHT_FIELD_X = 412;
    private static final int RIGHT_FIELD_W = 144;
    private static final int CONDITION_TYPE_Y = 120;
    private static final int CONDITION_PARAM_Y = 148;
    private static final int ATTR_FIELD_START_Y = 52;
    private static final int ATTR_FIELD_STRIDE = 34;
    private static final int ATTR_LABEL_OFFSET = 0;
    private static final int ATTR_BOX_OFFSET = 12;
    private static final int EXTRA_VALUE_WIDTH = 68;

    private enum Tab {
        LOOT,
        EXPERIENCE,
        CONDITIONS,
        CHARACTERISTICS
    }

    private OreUIEditText itemBox;
    private OreUIEditText chanceBox;
    private OreUIEditText minBox;
    private OreUIEditText maxBox;
    private OreUIEditText conditionValueBox;
    private OreUISlider chanceSlider;
    private OreUITextButton chanceMinusButton;
    private OreUITextButton chancePlusButton;
    private OreUITextButton minMinusButton;
    private OreUITextButton minPlusButton;
    private OreUITextButton maxMinusButton;
    private OreUITextButton maxPlusButton;
    private OreUITextButton addEntryButton;
    private OreUITextButton removeEntryButton;
    private OreUITextButton pickItemButton;
    private OreUITextButton cycleConditionTypeButton;
    private OreUITextButton flagAButton;
    private OreUITextButton flagBButton;
    private OreUICheckbox structureExactButton;
    private OreUITabs conditionSourceTabs;
    private OreUITabs sideTabs;
    private OreUITextButton dropDestinationButton;
    private OreUIToggleButton replaceLootButton;
    private OreUIEditText searchBox;
    private OreUIEditText healthBox;
    private OreUIEditText armorBox;
    private OreUIEditText armorToughnessBox;
    private OreUIEditText attackDamageBox;
    private OreUIEditText movementSpeedBox;
    private OreUIEditText knockbackResistanceBox;
    private OreUIEditText followRangeBox;
    private final List<OreUIEditText> extraBoxes = new ArrayList<>();

    private ResourceLocation selectedEntityId;
    private int selectedDropIndex;
    private int selectedXpIndex;
    private int selectedConditionIndex;
    private int listScrollOffset;
    private int dragFromIndex = -1;
    private boolean isDraggingRow;
    private double dragPointerY;
    private boolean draggingScrollbar;
    private Tab activeTab = Tab.LOOT;
    /**
     * LOOT or EXPERIENCE — which entry Conditions tab edits.
     */
    private Tab conditionSource = Tab.LOOT;
    private List<MobDropEntry> workingDrops;
    private List<MobExperienceEntry> workingExperience;
    private boolean workingReplaceVanilla;
    private DropDestination workingDropDestination = DropDestination.KILLER_INVENTORY;
    private EntityAttributeConfig workingAttributes;
    private LivingEntity previewEntity;
    private boolean suppressFieldSync;
    private ItemPickerOverlay itemPickerOverlay;
    private final List<ResourceLocation> filteredCatalog = new ArrayList<>();

    public MobDropConfigScreen(MobDropConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 580;
        this.imageHeight = 340;
        this.titleLabelY = 10;
        this.inventoryLabelY = 10000;
        this.selectedEntityId = menu.getSelectedEntityId();
        if (this.selectedEntityId == null && !SpawnerMobCatalog.getSpawnableIds().isEmpty())
            this.selectedEntityId = SpawnerMobCatalog.getSpawnableIds().getFirst();
        final EntityLootConfig initialConfig = menu.getConfigsByEntity().getOrDefault(this.selectedEntityId, EntityLootConfig.EMPTY);
        this.workingDrops = new ArrayList<>(initialConfig.drops());
        this.workingExperience = new ArrayList<>(initialConfig.experience());
        this.workingReplaceVanilla = initialConfig.replaceVanilla();
        this.workingAttributes = menu.getAttributesByEntity().getOrDefault(this.selectedEntityId, EntityAttributeDefaults.get(this.selectedEntityId));
        this.workingDropDestination = this.workingAttributes.dropDestination();
        this.selectedDropIndex = 0;
        this.selectedXpIndex = 0;
        this.selectedConditionIndex = 0;
    }

    @Override
    protected void init() {
        super.init();
        buildWidgets();
        refreshPreviewEntity();
        loadFieldsFromSelection();
    }

    @Override
    public void resize(@NotNull Minecraft minecraft, int width, int height) {
        applyCurrentFields();
        super.resize(minecraft, width, height);
    }

    private int fitGuiWidth() {
        return itemPickerOverlay != null ? ItemPickerOverlay.panelWidth() : imageWidth;
    }

    private int fitGuiHeight() {
        return itemPickerOverlay != null ? ItemPickerOverlay.panelHeight() : imageHeight;
    }

    private void syncFitScale() {
        GuiFitScale.update(width, height, fitGuiWidth(), fitGuiHeight());
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBg(guiGraphics, partialTick, mouseX, mouseY);
    }

    private void buildWidgets() {
        final ResourceLocation pickerPreselectId = itemBox == null ? null : ResourceLocation.tryParse(itemBox.getValue().trim());
        final String previousSearch = searchBox == null ? "" : searchBox.getValue();
        clearWidgets();
        if (itemPickerOverlay != null) {
            itemPickerOverlay.init(width, height);
            itemPickerOverlay.addWidgets(this::addRenderableWidget);
            itemPickerOverlay.preselect(pickerPreselectId);
            return;
        }

        final int left = leftPos;
        final int top = topPos;
        final int rightFieldX = left + RIGHT_FIELD_X;

        addOreButton(left + LEFT_X, top + 138, 18, 18, Component.literal("◀"), () -> cycleEntity(-1));
        this.searchBox = createBox(left + LEFT_X + 20, top + 138, LEFT_W - 40, "");
        this.searchBox.hint(Component.translatable("gui.mobexpansion.mob_spawner.search"));
        this.searchBox.setTooltip(Tooltip.create(Component.translatable("gui.mobexpansion.mob_drop_config.search.tooltip")));
        addRenderableWidget(this.searchBox);
        addOreButton(left + LEFT_X + LEFT_W - 18, top + 138, 18, 18, Component.literal("▶"), () -> cycleEntity(1));
        final boolean previousSuppress = suppressFieldSync;
        suppressFieldSync = true;
        this.searchBox.setValue(previousSearch);
        this.searchBox.setResponder(this::onSearchChanged);
        suppressFieldSync = previousSuppress;
        rebuildFilteredCatalog(previousSearch);

        sideTabs = addRenderableWidget(new OreUITabs(
                left + LEFT_X, top + 166,
                Component.translatable("gui.mobexpansion.mob_drop_config.tab.loot"),
                Component.translatable("gui.mobexpansion.mob_drop_config.tab.experience"),
                Component.translatable("gui.mobexpansion.mob_drop_config.tab.conditions"),
                Component.translatable("gui.mobexpansion.mob_drop_config.tab.characteristics")
        ).vertical().gap(4).tabSize(LEFT_W, 20)
                .selected(activeTab.ordinal())
                .onChanged(tab -> setActiveTab(Tab.values()[tab.index()])));

        addOreButton(left + LEFT_X, top + 284, LEFT_W, 20, Component.translatable("gui.mobexpansion.mob_drop_config.reset"), this::resetCurrentEntity);
        dropDestinationButton = addOreButton(left + LEFT_X, top + 262, LEFT_W, 18, Component.empty(), this::cycleDropDestination);
        updateDropDestinationButton();

        addEntryButton = addOreButton(left + CENTER_X + CENTER_W - 164, top + 36, 138, 18, Component.translatable("gui.mobexpansion.mob_drop_config.add"), this::onAddPressed);
        removeEntryButton = addOreButton(left + CENTER_X + CENTER_W - 22, top + 36, 18, 18, Component.literal("X"), OreUIAtlas.ButtonStyle.DESTRUCTIVE, this::onRemovePressed);
        removeEntryButton.setTooltip(Tooltip.create(Component.translatable("gui.mobexpansion.mob_drop_config.remove")));
        replaceLootButton = addRenderableWidget(new OreUIToggleButton(
                left + CENTER_X + 8,
                top + LIST_BODY_Y + ROW_HEIGHT * VISIBLE_ROWS + 8,
                CENTER_W - 16,
                Component.translatable("gui.mobexpansion.mob_drop_config.replace_vanilla"),
                workingReplaceVanilla,
                toggle -> workingReplaceVanilla = toggle.isToggled()
        ));
        replaceLootButton.setTooltip(Tooltip.create(Component.translatable("gui.mobexpansion.mob_drop_config.replace_vanilla.tooltip")));

        itemBox = createBox(rightFieldX, top + 78, RIGHT_FIELD_W - 24, "");
        pickItemButton = addOreButton(rightFieldX + RIGHT_FIELD_W - 20, top + 78, 20, 18, Component.literal("..."), this::openItemPicker);
        pickItemButton.setTooltip(Tooltip.create(Component.translatable("gui.mobexpansion.mob_drop_config.pick_item")));

        chanceBox = createBox(rightFieldX, top + 120, 52, "100");
        minBox = createBox(rightFieldX + 26, top + 178, 40, "1");
        maxBox = createBox(rightFieldX + 26, top + 204, 40, "1");
        conditionValueBox = createBox(rightFieldX, top + CONDITION_PARAM_Y, RIGHT_FIELD_W, "");
        conditionValueBox.setResponder(text -> onConditionValueEdited());

        healthBox = createBox(rightFieldX, top + ATTR_FIELD_START_Y + ATTR_BOX_OFFSET, RIGHT_FIELD_W, "20");
        armorBox = createBox(rightFieldX, top + ATTR_FIELD_START_Y + ATTR_FIELD_STRIDE + ATTR_BOX_OFFSET, RIGHT_FIELD_W, "0");
        armorToughnessBox = createBox(rightFieldX, top + ATTR_FIELD_START_Y + ATTR_FIELD_STRIDE * 2 + ATTR_BOX_OFFSET, RIGHT_FIELD_W, "0");
        attackDamageBox = createBox(rightFieldX, top + ATTR_FIELD_START_Y + ATTR_FIELD_STRIDE * 3 + ATTR_BOX_OFFSET, RIGHT_FIELD_W, "0");
        movementSpeedBox = createBox(rightFieldX, top + ATTR_FIELD_START_Y + ATTR_FIELD_STRIDE * 4 + ATTR_BOX_OFFSET, RIGHT_FIELD_W, "0.28");
        knockbackResistanceBox = createBox(rightFieldX, top + ATTR_FIELD_START_Y + ATTR_FIELD_STRIDE * 5 + ATTR_BOX_OFFSET, RIGHT_FIELD_W, "0");
        followRangeBox = createBox(rightFieldX, top + ATTR_FIELD_START_Y + ATTR_FIELD_STRIDE * 6 + ATTR_BOX_OFFSET, RIGHT_FIELD_W, "24");
        setAttributeTooltip(healthBox, "max_health");
        setAttributeTooltip(armorBox, "armor");
        setAttributeTooltip(armorToughnessBox, "armor_toughness");
        setAttributeTooltip(attackDamageBox, "attack_damage");
        setAttributeTooltip(movementSpeedBox, "movement_speed");
        setAttributeTooltip(knockbackResistanceBox, "knockback_resistance");
        setAttributeTooltip(followRangeBox, "follow_range");

        addRenderableWidget(itemBox);
        addRenderableWidget(chanceBox);
        addRenderableWidget(minBox);
        addRenderableWidget(maxBox);
        addRenderableWidget(conditionValueBox);
        addRenderableWidget(healthBox);
        addRenderableWidget(armorBox);
        addRenderableWidget(armorToughnessBox);
        addRenderableWidget(attackDamageBox);
        addRenderableWidget(movementSpeedBox);
        addRenderableWidget(knockbackResistanceBox);
        addRenderableWidget(followRangeBox);
        rebuildExtraBoxes();

        chanceMinusButton = addOreButton(rightFieldX + 76, top + 120, 18, 18, Component.literal("-"), () -> nudgeChance(-1));
        chancePlusButton = addOreButton(rightFieldX + 98, top + 120, 18, 18, Component.literal("+"), () -> nudgeChance(1));
        minMinusButton = addOreButton(rightFieldX, top + 178, 20, 18, Component.literal("-"), () -> nudgeMin(-1));
        minPlusButton = addOreButton(rightFieldX + 70, top + 178, 20, 18, Component.literal("+"), () -> nudgeMin(1));
        maxMinusButton = addOreButton(rightFieldX, top + 204, 20, 18, Component.literal("-"), () -> nudgeMax(-1));
        maxPlusButton = addOreButton(rightFieldX + 70, top + 204, 20, 18, Component.literal("+"), () -> nudgeMax(1));

        chanceSlider = addRenderableWidget(new OreUISlider(rightFieldX, top + 144, RIGHT_FIELD_W, 0, 100, 100)
                .integer().step(1)
                .onChanged(slider -> {
                    if (suppressFieldSync || chanceBox == null)
                        return;
                    chanceBox.setValue(String.valueOf(slider.valueInt()));
                    applyCurrentFields();
                }));

        conditionSourceTabs = addRenderableWidget(new OreUITabs(
                rightFieldX, top + 56,
                Component.translatable("gui.mobexpansion.mob_drop_config.tab.loot"),
                Component.translatable("gui.mobexpansion.mob_drop_config.tab.experience")
        ).horizontal().gap(4).tabSize(70, 18)
                .selected(conditionSource == Tab.EXPERIENCE ? 1 : 0)
                .onChanged(tab -> setConditionSource(tab.index() == 0 ? Tab.LOOT : Tab.EXPERIENCE)));
        cycleConditionTypeButton = addOreButton(rightFieldX, top + CONDITION_TYPE_Y, RIGHT_FIELD_W, 18, Component.empty(), this::cycleConditionType);
        flagAButton = addOreButton(rightFieldX, top + CONDITION_PARAM_Y, 70, 18, Component.empty(), this::cycleFlagA);
        flagBButton = addOreButton(rightFieldX + 74, top + CONDITION_PARAM_Y, 70, 18, Component.empty(), this::cycleFlagB);
        structureExactButton = addRenderableWidget(new OreUICheckbox(
                rightFieldX + RIGHT_FIELD_W - 70, top + CONDITION_PARAM_Y, 70,
                Component.translatable("gui.mobexpansion.condition.exact"),
                false, button -> toggleStructureExact()));

        addRenderableWidget(new OreUITextButton(left + 170, top + 308, 130, 22, Component.translatable("gui.cancel"), OreUIAtlas.ButtonStyle.DESTRUCTIVE, button -> onClose()));
        addRenderableWidget(new OreUITextButton(left + 320, top + 308, 130, 22, Component.translatable("gui.mobexpansion.mob_drop_config.done"), OreUIAtlas.ButtonStyle.PRIMARY, button -> saveAndClose()));

        updateControlsVisibility();
    }

    private OreUITextButton addOreButton(int x, int y, int width, int height, Component message, OreUIAtlas.ButtonStyle style,  Runnable onPress) {
        return addRenderableWidget(new OreUITextButton(x, y, width, height, message, style, button -> onPress.run()));
    }
    private OreUITextButton addOreButton(int x, int y, int width, int height, Component message, Runnable onPress) {
        return addOreButton(x, y, width, height, message, OreUIAtlas.ButtonStyle.SECONDARY, onPress);
    }


    private void onAddPressed() {
        if (activeTab == Tab.CONDITIONS)
            addCondition();
        else
            addCurrentEntry();
    }

    private void onRemovePressed() {
        if (activeTab == Tab.CONDITIONS)
            removeCondition();
        else
            removeCurrentEntry();
    }

    private void setConditionSource(Tab sourceTab) {
        if (sourceTab != Tab.LOOT && sourceTab != Tab.EXPERIENCE)
            return;
        selectedConditionIndex = 0;
        listScrollOffset = 0;
        conditionSource = sourceTab;
        loadConditionFields(currentConditions());
        updateControlsVisibility();
    }

    private void openItemPicker() {
        applyCurrentFields();
        itemPickerOverlay = new ItemPickerOverlay(this, font, this::onItemPicked, this::closeItemPicker);
        buildWidgets();
    }

    private void onItemPicked(ResourceLocation selectedItemId) {
        if (!workingDrops.isEmpty()) {
            final MobDropEntry currentEntry = workingDrops.get(selectedDropIndex);
            workingDrops.set(selectedDropIndex, new MobDropEntry(selectedItemId, currentEntry.minCount(), currentEntry.maxCount(), currentEntry.chance(), currentEntry.conditions()));
        }
        closeItemPicker();
    }

    private void closeItemPicker() {
        itemPickerOverlay = null;
        buildWidgets();
        loadFieldsFromSelection();
        updateControlsVisibility();
    }

    private OreUIEditText createBox(int boxX, int boxY, int width, String value) {
        final OreUIEditText editBox = new OreUIEditText(font, boxX, boxY, width, OreUIAtlas.EDIT_HEIGHT, Component.empty());
        editBox.setValue(value);
        editBox.setMaxLength(128);
        editBox.setResponder(text -> onFieldEdited());
        return editBox;
    }

    private void setAttributeTooltip(OreUIEditText box, String attrKey) {
        box.setTooltip(createParamTooltip(
                "gui.mobexpansion.mob_drop_config.attr." + attrKey,
                "gui.mobexpansion.mob_drop_config.attr." + attrKey + ".tooltip"
        ));
    }

    private static Tooltip createParamTooltip(String titleKey, String tooltipKey) {
        return Tooltip.create(Component.translatable(titleKey)
                .append("\n")
                .append(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY)));
    }

    private void setActiveTab(Tab tab) {
        applyCurrentFields();
        if (activeTab == Tab.LOOT || activeTab == Tab.EXPERIENCE)
            conditionSource = activeTab;
        activeTab = tab;
        listScrollOffset = 0;
        dragFromIndex = -1;
        isDraggingRow = false;
        if (tab == Tab.CONDITIONS)
            selectedConditionIndex = 0;
        if (sideTabs != null)
            sideTabs.selected(activeTab.ordinal());
        loadFieldsFromSelection();
        updateControlsVisibility();
    }

    private boolean isCharacteristicsTab() {
        return activeTab == Tab.CHARACTERISTICS;
    }

    private boolean isEntryListTab() {
        return activeTab == Tab.LOOT || activeTab == Tab.EXPERIENCE;
    }

    private boolean isConditionTab() {
        return activeTab == Tab.CONDITIONS;
    }

    private boolean hasEntrySelection() {
        if (activeTab == Tab.LOOT)
            return !workingDrops.isEmpty();
        if (activeTab == Tab.EXPERIENCE)
            return !workingExperience.isEmpty();
        return false;
    }

    private boolean hasConditionTarget() {
        if (conditionSource == Tab.EXPERIENCE)
            return !workingExperience.isEmpty() && selectedXpIndex < workingExperience.size();
        return !workingDrops.isEmpty() && selectedDropIndex < workingDrops.size();
    }

    private List<LootConditionEntry> currentConditions() {
        if (!hasConditionTarget())
            return List.of();
        if (conditionSource == Tab.EXPERIENCE)
            return workingExperience.get(selectedXpIndex).conditions();
        return workingDrops.get(selectedDropIndex).conditions();
    }

    private void updateControlsVisibility() {
        if (itemPickerOverlay != null || addEntryButton == null)
            return;
        final boolean entryTab = isEntryListTab();
        final boolean conditionTab = isConditionTab();
        final boolean hasSelection = hasEntrySelection();
        final boolean lootTab = activeTab == Tab.LOOT;
        final boolean hasTarget = hasConditionTarget();
        final boolean hasCondition = conditionTab && hasTarget && !currentConditions().isEmpty() && selectedConditionIndex < currentConditions().size();
        final LootConditionEntry selectedCondition = hasCondition ? currentConditions().get(selectedConditionIndex) : null;

        setShown(addEntryButton, entryTab || conditionTab);
        setShown(removeEntryButton, entryTab || (conditionTab && hasCondition));
        if (conditionTab) {
            addEntryButton.setX(leftPos + CENTER_X + CENTER_W - 164);
            addEntryButton.setWidth(138);
            addEntryButton.setMessage(Component.translatable("gui.mobexpansion.mob_drop_config.add_condition"));
            addEntryButton.active = hasTarget;
            removeEntryButton.setTooltip(Tooltip.create(Component.translatable("gui.mobexpansion.mob_drop_config.remove_condition")));
        } else {
            addEntryButton.setX(leftPos + CENTER_X + CENTER_W - 98);
            addEntryButton.setWidth(72);
            addEntryButton.setMessage(Component.translatable("gui.mobexpansion.mob_drop_config.add"));
            removeEntryButton.setTooltip(Tooltip.create(Component.translatable("gui.mobexpansion.mob_drop_config.remove")));
        }

        setShown(itemBox, lootTab && hasSelection);
        setShown(pickItemButton, lootTab && hasSelection);
        setShown(chanceBox, entryTab && hasSelection);
        setShown(minBox, entryTab && hasSelection);
        setShown(maxBox, entryTab && hasSelection);
        setShown(chanceMinusButton, entryTab && hasSelection);
        setShown(chancePlusButton, entryTab && hasSelection);
        setShown(minMinusButton, entryTab && hasSelection);
        setShown(minPlusButton, entryTab && hasSelection);
        setShown(maxMinusButton, entryTab && hasSelection);
        setShown(maxPlusButton, entryTab && hasSelection);
        setShown(chanceSlider, entryTab && hasSelection);

        setShown(conditionSourceTabs, conditionTab);
        if (conditionSourceTabs != null)
            conditionSourceTabs.selected(conditionSource == Tab.EXPERIENCE ? 1 : 0);
        setShown(cycleConditionTypeButton, hasCondition);
        updateConditionParamVisibility(conditionTab ? selectedCondition : null);
        if (hasCondition)
            cycleConditionTypeButton.setMessage(typeLabel(selectedCondition.type()));

        final boolean characteristicsTab = isCharacteristicsTab();
        setShown(healthBox, characteristicsTab);
        setShown(armorBox, characteristicsTab);
        setShown(armorToughnessBox, characteristicsTab);
        setShown(attackDamageBox, characteristicsTab);
        setShown(movementSpeedBox, characteristicsTab);
        setShown(knockbackResistanceBox, characteristicsTab);
        setShown(followRangeBox, characteristicsTab);
        if (characteristicsTab) {
            setShown(addEntryButton, false);
            setShown(removeEntryButton, false);
        }
        if (replaceLootButton != null) {
            replaceLootButton.setToggled(workingReplaceVanilla);
            setShown(replaceLootButton, lootTab);
        }
        updateExtraBoxLayout();
    }

    private List<ExtraParameterDefinition> currentExtraDefs() {
        return EntityExtraParametersCatalog.paramsFor(selectedEntityId);
    }

    private void rebuildExtraBoxes() {
        for (OreUIEditText box : extraBoxes)
            removeWidget(box);
        extraBoxes.clear();
        if (selectedEntityId == null)
            return;
        final EntityAttributeConfig config = workingAttributes == null
                ? EntityAttributeDefaults.get(selectedEntityId)
                : workingAttributes.sanitized(selectedEntityId);
        final boolean previousSuppress = suppressFieldSync;
        suppressFieldSync = true;
        for (ExtraParameterDefinition def : currentExtraDefs()) {
            final OreUIEditText box = createBox(0, 0, EXTRA_VALUE_WIDTH, formatAttribute(config.extra(def.key(), def.defaultValue())));
            box.setTooltip(createParamTooltip(def.langKey(), def.tooltipLangKey()));
            extraBoxes.add(box);
            addRenderableWidget(box);
        }
        suppressFieldSync = previousSuppress;
        updateExtraBoxLayout();
    }

    private void updateExtraBoxLayout() {
        final boolean characteristicsTab = isCharacteristicsTab();
        final int visibleRows = visibleRowCount();
        final int valueX = listLeft() + listWidth() - EXTRA_VALUE_WIDTH;
        for (int index = 0; index < extraBoxes.size(); index++) {
            final OreUIEditText box = extraBoxes.get(index);
            final int visualIndex = index - listScrollOffset;
            final boolean shown = characteristicsTab && visualIndex >= 0 && visualIndex < visibleRows;
            if (!shown && box.isFocused())
                setFocused(null);
            setShown(box, shown);
            if (shown) {
                box.setX(valueX);
                box.setY(listTop() + visualIndex * rowHeight() + 2);
                box.setWidth(EXTRA_VALUE_WIDTH);
            }
        }
    }

    private void updateConditionParamVisibility(LootConditionEntry selectedCondition) {
        if (selectedCondition == null) {
            setShown(conditionValueBox, false);
            setShown(flagAButton, false);
            setShown(flagBButton, false);
            setShown(structureExactButton, false);
            return;
        }
        final LootConditionType conditionType = selectedCondition.type();
        final int paramX = leftPos + RIGHT_FIELD_X;
        final int paramY = topPos + CONDITION_PARAM_Y;
        if (conditionType == LootConditionType.MATCH_WEATHER) {
            setShown(conditionValueBox, false);
            setShown(flagAButton, true);
            setShown(flagBButton, true);
            setShown(structureExactButton, false);
            flagAButton.setX(paramX);
            flagAButton.setY(paramY);
            flagAButton.setWidth(70);
            flagBButton.setX(paramX + 74);
            flagBButton.setY(paramY);
            flagBButton.setWidth(70);
            flagAButton.setMessage(Component.translatable("gui.mobexpansion.condition.raining." + triKey(selectedCondition.flagA())));
            flagBButton.setMessage(Component.translatable("gui.mobexpansion.condition.thundering." + triKey(selectedCondition.flagB())));
            return;
        }
        if (conditionType == LootConditionType.MATCH_STRUCTURE) {
            setShown(conditionValueBox, true);
            setShown(flagAButton, false);
            setShown(flagBButton, false);
            setShown(structureExactButton, true);
            conditionValueBox.setX(paramX);
            conditionValueBox.setY(paramY);
            conditionValueBox.setWidth(RIGHT_FIELD_W - 74);
            structureExactButton.setX(paramX + RIGHT_FIELD_W - 70);
            structureExactButton.setY(paramY);
            structureExactButton.setChecked(selectedCondition.flagA() == LootConditionEntry.TRI_TRUE);
            return;
        }
        final boolean needsValue = conditionType == LootConditionType.MATCH_BIOME
                || conditionType == LootConditionType.MATCH_DIMENSION
                || conditionType == LootConditionType.MATCH_MAIN_HAND
                || conditionType == LootConditionType.MATCH_OFF_HAND
                || conditionType == LootConditionType.MATCH_TIME
                || conditionType == LootConditionType.IS_LIGHT_LEVEL;
        setShown(conditionValueBox, needsValue);
        setShown(flagAButton, false);
        setShown(flagBButton, false);
        setShown(structureExactButton, false);
        if (needsValue) {
            conditionValueBox.setX(paramX);
            conditionValueBox.setY(paramY);
            conditionValueBox.setWidth(RIGHT_FIELD_W);
        }
    }

    private static String triKey(byte flag) {
        if (flag == LootConditionEntry.TRI_TRUE)
            return "yes";
        if (flag == LootConditionEntry.TRI_FALSE)
            return "no";
        return "any";
    }

    private static Component typeLabel(LootConditionType conditionType) {
        return Component.translatable("gui.mobexpansion.condition.type." + conditionType.name().toLowerCase(Locale.ROOT));
    }

    private static void setShown(AbstractWidget widget, boolean shown) {
        if (widget == null)
            return;
        if (widget instanceof OreUITabs tabs) {
            tabs.visible(shown);
            tabs.enabled(shown);
            return;
        }
        widget.visible = shown;
        widget.active = shown;
    }

    private void cycleEntity(int direction) {
        if (filteredCatalog.isEmpty())
            return;
        applyCurrentFields();
        storeWorkingConfig();
        selectedEntityId = SpawnerMobCatalog.cycle(selectedEntityId, direction, filteredCatalog);
        if (selectedEntityId == null)
            selectedEntityId = direction >= 0 ? filteredCatalog.getFirst() : filteredCatalog.getLast();
        loadWorkingFromMenu();
        selectedDropIndex = 0;
        selectedXpIndex = 0;
        selectedConditionIndex = 0;
        listScrollOffset = 0;
        conditionSource = Tab.LOOT;
        refreshPreviewEntity();
        rebuildExtraBoxes();
        loadFieldsFromSelection();
        updateControlsVisibility();
    }

    private void loadWorkingFromMenu() {
        final EntityLootConfig entityConfig = menu.getConfigsByEntity().getOrDefault(selectedEntityId, EntityLootConfig.EMPTY);
        workingDrops = new ArrayList<>(entityConfig.drops());
        workingExperience = new ArrayList<>(entityConfig.experience());
        workingReplaceVanilla = entityConfig.replaceVanilla();
        workingAttributes = menu.getAttributesByEntity().getOrDefault(selectedEntityId, EntityAttributeDefaults.get(selectedEntityId));
        workingDropDestination = workingAttributes.dropDestination();
        updateDropDestinationButton();
    }

    private void refreshPreviewEntity() {
        previewEntity = null;
        if (minecraft == null || minecraft.level == null || selectedEntityId == null)
            return;
        final Mob createdMob = SpawnerMobCatalog.tryCreateMob(selectedEntityId, minecraft.level);
        if (createdMob == null)
            return;
        createdMob.setNoAi(true);
        createdMob.setSilent(true);
        createdMob.setYBodyRot(210.0F);
        createdMob.setYHeadRot(210.0F);
        createdMob.setYRot(210.0F);
        createdMob.yBodyRotO = 210.0F;
        createdMob.yHeadRotO = 210.0F;
        createdMob.yRotO = 210.0F;
        createdMob.tickCount = 0;
        previewEntity = createdMob;
    }

    private void tickPreviewEntity() {
        if (previewEntity == null)
            return;
        previewEntity.tickCount = (int) (Blaze3D.getTime() * 30);
        previewEntity.setDeltaMovement(0.0D, 0.0D, 0.0D);
        previewEntity.xo = previewEntity.getX();
        previewEntity.yo = previewEntity.getY();
        previewEntity.zo = previewEntity.getZ();
        previewEntity.xOld = previewEntity.getX();
        previewEntity.yOld = previewEntity.getY();
        previewEntity.zOld = previewEntity.getZ();
        previewEntity.yBodyRotO = previewEntity.yBodyRot;
        previewEntity.yHeadRotO = previewEntity.yHeadRot;
        previewEntity.yRotO = previewEntity.getYRot();
        previewEntity.xRotO = previewEntity.getXRot();
    }

    private void addCurrentEntry() {
        applyCurrentFields();
        if (activeTab == Tab.LOOT) {
            workingDrops.add(MobDropEntry.createDefault());
            selectedDropIndex = workingDrops.size() - 1;
            conditionSource = Tab.LOOT;
        } else if (activeTab == Tab.EXPERIENCE) {
            workingExperience.add(MobExperienceEntry.createDefault());
            selectedXpIndex = workingExperience.size() - 1;
            conditionSource = Tab.EXPERIENCE;
        }
        selectedConditionIndex = 0;
        ensureSelectionVisible();
        loadFieldsFromSelection();
        updateControlsVisibility();
    }

    private void removeCurrentEntry() {
        if (activeTab == Tab.LOOT) {
            if (workingDrops.isEmpty())
                return;
            workingDrops.remove(selectedDropIndex);
            if (selectedDropIndex >= workingDrops.size())
                selectedDropIndex = Math.max(0, workingDrops.size() - 1);
        } else if (activeTab == Tab.EXPERIENCE) {
            if (workingExperience.isEmpty())
                return;
            workingExperience.remove(selectedXpIndex);
            if (selectedXpIndex >= workingExperience.size())
                selectedXpIndex = Math.max(0, workingExperience.size() - 1);
        }
        selectedConditionIndex = 0;
        ensureSelectionVisible();
        loadFieldsFromSelection();
        updateControlsVisibility();
    }

    private void cycleDropDestination() {
        workingDropDestination = workingDropDestination.next();
        if (workingAttributes != null)
            workingAttributes = workingAttributes.withDropDestination(workingDropDestination);
        updateDropDestinationButton();
    }

    private void updateDropDestinationButton() {
        if (dropDestinationButton == null)
            return;
        dropDestinationButton.setMessage(Component.translatable("gui.mobexpansion.mob_drop_config.drop_destination." + workingDropDestination.langSuffix()));
        dropDestinationButton.setTooltip(Tooltip.create(Component.translatable("gui.mobexpansion.mob_drop_config.drop_destination." + workingDropDestination.langSuffix() + ".tooltip")));
    }

    private void resetCurrentEntity() {
        workingDrops.clear();
        workingExperience.clear();
        workingReplaceVanilla = false;
        workingAttributes = EntityAttributeDefaults.get(selectedEntityId);
        workingDropDestination = workingAttributes.dropDestination();
        selectedDropIndex = 0;
        selectedXpIndex = 0;
        selectedConditionIndex = 0;
        listScrollOffset = 0;
        conditionSource = Tab.LOOT;
        updateDropDestinationButton();
        loadFieldsFromSelection();
        updateControlsVisibility();
    }

    private void loadFieldsFromSelection() {
        suppressFieldSync = true;
        if (isConditionTab()) {
            loadConditionFields(currentConditions());
            suppressFieldSync = false;
            return;
        }
        if (isCharacteristicsTab()) {
            loadAttributeFields();
            suppressFieldSync = false;
            return;
        }
        if (!hasEntrySelection()) {
            if (itemBox != null)
                itemBox.setValue("minecraft:iron_ingot");
            if (chanceBox != null)
                chanceBox.setValue("100");
            if (chanceSlider != null)
                chanceSlider.value(100);
            if (minBox != null)
                minBox.setValue(activeTab == Tab.EXPERIENCE ? "5" : "1");
            if (maxBox != null)
                maxBox.setValue(activeTab == Tab.EXPERIENCE ? "5" : "1");
            suppressFieldSync = false;
            return;
        }
        if (activeTab == Tab.LOOT) {
            final MobDropEntry dropEntry = workingDrops.get(selectedDropIndex);
            itemBox.setValue(dropEntry.itemId().toString());
            final int chancePercent = Math.round(dropEntry.chance() * 100.0F);
            chanceBox.setValue(String.valueOf(chancePercent));
            if (chanceSlider != null)
                chanceSlider.value(chancePercent);
            minBox.setValue(String.valueOf(dropEntry.minCount()));
            maxBox.setValue(String.valueOf(dropEntry.maxCount()));
        } else if (activeTab == Tab.EXPERIENCE) {
            final MobExperienceEntry experienceEntry = workingExperience.get(selectedXpIndex);
            final int chancePercent = Math.round(experienceEntry.chance() * 100.0F);
            chanceBox.setValue(String.valueOf(chancePercent));
            if (chanceSlider != null)
                chanceSlider.value(chancePercent);
            minBox.setValue(String.valueOf(experienceEntry.minAmount()));
            maxBox.setValue(String.valueOf(experienceEntry.maxAmount()));
        }
        suppressFieldSync = false;
    }

    private void loadAttributeFields() {
        if (healthBox == null)
            return;
        final EntityAttributeConfig config = workingAttributes == null
                ? EntityAttributeDefaults.get(selectedEntityId)
                : workingAttributes.sanitized(selectedEntityId);
        healthBox.setValue(formatAttribute(config.maxHealth()));
        armorBox.setValue(formatAttribute(config.armor()));
        armorToughnessBox.setValue(formatAttribute(config.armorToughness()));
        attackDamageBox.setValue(formatAttribute(config.attackDamage()));
        movementSpeedBox.setValue(formatAttribute(config.movementSpeed()));
        knockbackResistanceBox.setValue(formatAttribute(config.knockbackResistance()));
        followRangeBox.setValue(formatAttribute(config.followRange()));
        final List<ExtraParameterDefinition> defs = currentExtraDefs();
        for (int index = 0; index < extraBoxes.size() && index < defs.size(); index++) {
            final ExtraParameterDefinition def = defs.get(index);
            extraBoxes.get(index).setValue(formatAttribute(config.extra(def.key(), def.defaultValue())));
        }
    }

    private void loadConditionFields(List<LootConditionEntry> conditions) {
        if (conditionValueBox == null)
            return;
        if (conditions.isEmpty()) {
            selectedConditionIndex = 0;
            conditionValueBox.setValue("");
            updateConditionParamVisibility(null);
            return;
        }
        selectedConditionIndex = Mth.clamp(selectedConditionIndex, 0, conditions.size() - 1);
        listScrollOffset = Mth.clamp(listScrollOffset, 0, Math.max(0, conditions.size() - CONDITION_VISIBLE_ROWS));
        final LootConditionEntry condition = conditions.get(selectedConditionIndex);
        conditionValueBox.setValue(formatConditionValue(condition));
        if (cycleConditionTypeButton != null)
            cycleConditionTypeButton.setMessage(typeLabel(condition.type()));
        updateConditionParamVisibility(condition);
    }

    private static String formatConditionValue(LootConditionEntry condition) {
        return switch (condition.type()) {
            case MATCH_BIOME, MATCH_DIMENSION, MATCH_STRUCTURE, MATCH_MAIN_HAND, MATCH_OFF_HAND -> condition.stringValue();
            case MATCH_TIME -> condition.intMin() + "," + condition.intMax() + "," + condition.intExtra();
            case IS_LIGHT_LEVEL -> condition.intMin() + "," + condition.intMax();
            default -> "";
        };
    }

    private void onFieldEdited() {
        if (suppressFieldSync)
            return;
        applyCurrentFields();
    }

    private void onConditionValueEdited() {
        if (suppressFieldSync)
            return;
        applyConditionValueFromBox();
    }

    private void applyCurrentFields() {
        if (isCharacteristicsTab()) {
            applyAttributeFields();
            return;
        }
        if (isConditionTab() || !hasEntrySelection() || chanceBox == null)
            return;
        final float chance = Mth.clamp(parseInt(chanceBox.getValue(), 100) / 100.0F, 0.0F, 1.0F);
        if (activeTab == Tab.LOOT) {
            final ResourceLocation itemId = ResourceLocation.tryParse(itemBox.getValue().trim());
            if (itemId == null)
                return;
            final MobDropEntry currentEntry = workingDrops.get(selectedDropIndex);
            final int minCount = parseInt(minBox.getValue(), 1);
            final int maxCount = parseInt(maxBox.getValue(), minCount);
            workingDrops.set(selectedDropIndex, new MobDropEntry(itemId, minCount, maxCount, chance, currentEntry.conditions()));
            return;
        }
        if (activeTab == Tab.EXPERIENCE) {
            final MobExperienceEntry currentEntry = workingExperience.get(selectedXpIndex);
            final int minAmount = parseInt(minBox.getValue(), 5);
            final int maxAmount = parseInt(maxBox.getValue(), minAmount);
            workingExperience.set(selectedXpIndex, new MobExperienceEntry(minAmount, maxAmount, chance, currentEntry.conditions()));
        }
    }

    private void applyAttributeFields() {
        if (healthBox == null)
            return;
        final EntityAttributeConfig fallback = workingAttributes == null
                ? EntityAttributeDefaults.get(selectedEntityId)
                : workingAttributes;
        final Map<String, Double> extras = new LinkedHashMap<>();
        final List<ExtraParameterDefinition> defs = currentExtraDefs();
        for (int index = 0; index < extraBoxes.size() && index < defs.size(); index++) {
            final ExtraParameterDefinition def = defs.get(index);
            extras.put(def.key(), parseDouble(extraBoxes.get(index).getValue(), fallback.extra(def.key(), def.defaultValue())));
        }
        for (ExtraParameterDefinition def : defs)
            extras.putIfAbsent(def.key(), fallback.extra(def.key(), def.defaultValue()));
        workingAttributes = new EntityAttributeConfig(
                parseDouble(healthBox.getValue(), fallback.maxHealth()),
                parseDouble(armorBox.getValue(), fallback.armor()),
                parseDouble(armorToughnessBox.getValue(), fallback.armorToughness()),
                parseDouble(attackDamageBox.getValue(), fallback.attackDamage()),
                parseDouble(movementSpeedBox.getValue(), fallback.movementSpeed()),
                parseDouble(knockbackResistanceBox.getValue(), fallback.knockbackResistance()),
                parseDouble(followRangeBox.getValue(), fallback.followRange()),
                extras,
                workingDropDestination
        ).sanitized(selectedEntityId);
    }

    private void applyConditionValueFromBox() {
        if (!hasConditionTarget() || currentConditions().isEmpty())
            return;
        final LootConditionEntry currentCondition = currentConditions().get(selectedConditionIndex);
        final String rawValue = conditionValueBox.getValue().trim();
        final LootConditionEntry nextCondition = switch (currentCondition.type()) {
            case MATCH_BIOME, MATCH_DIMENSION, MATCH_STRUCTURE, MATCH_MAIN_HAND, MATCH_OFF_HAND -> currentCondition.withStringValue(rawValue);
            case MATCH_TIME -> parseTimeCondition(currentCondition, rawValue);
            case IS_LIGHT_LEVEL -> parseLightCondition(currentCondition, rawValue);
            default -> currentCondition;
        };
        replaceSelectedCondition(nextCondition);
    }

    private static LootConditionEntry parseTimeCondition(LootConditionEntry currentCondition, String rawValue) {
        final String[] parts = rawValue.split(",");
        final int minTime = parts.length > 0 ? parseInt(parts[0], currentCondition.intMin()) : currentCondition.intMin();
        final int maxTime = parts.length > 1 ? parseInt(parts[1], currentCondition.intMax()) : currentCondition.intMax();
        final int period = parts.length > 2 ? parseInt(parts[2], currentCondition.intExtra()) : currentCondition.intExtra();
        return currentCondition.withRange(minTime, maxTime).withExtra(period);
    }

    private static LootConditionEntry parseLightCondition(LootConditionEntry currentCondition, String rawValue) {
        final String[] parts = rawValue.split(",");
        final int minLevel = parts.length > 0 ? parseInt(parts[0], currentCondition.intMin()) : currentCondition.intMin();
        final int maxLevel = parts.length > 1 ? parseInt(parts[1], currentCondition.intMax()) : currentCondition.intMax();
        return currentCondition.withRange(minLevel, maxLevel);
    }

    private void replaceSelectedCondition(LootConditionEntry nextCondition) {
        final List<LootConditionEntry> nextConditions = new ArrayList<>(currentConditions());
        nextConditions.set(selectedConditionIndex, nextCondition);
        setCurrentConditions(nextConditions);
    }

    private void setCurrentConditions(List<LootConditionEntry> nextConditions) {
        if (!hasConditionTarget())
            return;
        if (conditionSource == Tab.EXPERIENCE) {
            workingExperience.set(selectedXpIndex, workingExperience.get(selectedXpIndex).withConditions(nextConditions));
            return;
        }
        workingDrops.set(selectedDropIndex, workingDrops.get(selectedDropIndex).withConditions(nextConditions));
    }

    private void addCondition() {
        if (!hasConditionTarget())
            return;
        final List<LootConditionEntry> nextConditions = new ArrayList<>(currentConditions());
        nextConditions.add(LootConditionEntry.createDefault());
        selectedConditionIndex = nextConditions.size() - 1;
        setCurrentConditions(nextConditions);
        ensureSelectionVisible();
        loadConditionFields(nextConditions);
        updateControlsVisibility();
    }

    private void removeCondition() {
        if (!hasConditionTarget() || currentConditions().isEmpty())
            return;
        final List<LootConditionEntry> nextConditions = new ArrayList<>(currentConditions());
        nextConditions.remove(selectedConditionIndex);
        if (selectedConditionIndex >= nextConditions.size())
            selectedConditionIndex = Math.max(0, nextConditions.size() - 1);
        setCurrentConditions(nextConditions);
        ensureSelectionVisible();
        loadConditionFields(nextConditions);
        updateControlsVisibility();
    }

    private void cycleConditionType() {
        if (!hasConditionTarget() || currentConditions().isEmpty())
            return;
        final LootConditionEntry currentCondition = currentConditions().get(selectedConditionIndex);
        final LootConditionEntry nextCondition = LootConditionEntry.forType(LootConditionEntry.nextType(currentCondition.type()));
        replaceSelectedCondition(nextCondition);
        loadConditionFields(currentConditions());
        updateControlsVisibility();
    }

    private void toggleStructureExact() {
        if (!hasConditionTarget() || currentConditions().isEmpty())
            return;
        final LootConditionEntry currentCondition = currentConditions().get(selectedConditionIndex);
        if (currentCondition.type() != LootConditionType.MATCH_STRUCTURE)
            return;
        final boolean exact = currentCondition.flagA() != LootConditionEntry.TRI_TRUE;
        replaceSelectedCondition(currentCondition.withFlags(
                exact ? LootConditionEntry.TRI_TRUE : LootConditionEntry.TRI_FALSE,
                currentCondition.flagB()
        ));
        loadConditionFields(currentConditions());
        updateControlsVisibility();
    }

    private void cycleFlagA() {
        if (!hasConditionTarget() || currentConditions().isEmpty())
            return;
        final LootConditionEntry currentCondition = currentConditions().get(selectedConditionIndex);
        if (currentCondition.type() != LootConditionType.MATCH_WEATHER)
            return;
        replaceSelectedCondition(currentCondition.withFlags(LootConditionEntry.nextTri(currentCondition.flagA()), currentCondition.flagB()));
        loadConditionFields(currentConditions());
        updateControlsVisibility();
    }

    private void cycleFlagB() {
        if (!hasConditionTarget() || currentConditions().isEmpty())
            return;
        final LootConditionEntry currentCondition = currentConditions().get(selectedConditionIndex);
        if (currentCondition.type() != LootConditionType.MATCH_WEATHER)
            return;
        replaceSelectedCondition(currentCondition.withFlags(currentCondition.flagA(), LootConditionEntry.nextTri(currentCondition.flagB())));
        loadConditionFields(currentConditions());
        updateControlsVisibility();
    }

    private void nudgeChance(int delta) {
        if (!hasEntrySelection())
            return;
        final int chancePercent = Mth.clamp(parseInt(chanceBox.getValue(), 100) + delta, 0, 100);
        chanceBox.setValue(String.valueOf(chancePercent));
        if (chanceSlider != null)
            chanceSlider.value(chancePercent);
        applyCurrentFields();
    }

    private void nudgeMin(int delta) {
        if (!hasEntrySelection())
            return;
        final int minBound = activeTab == Tab.EXPERIENCE ? MobExperienceEntry.MIN_XP : MobDropEntry.MIN_STACK;
        final int maxBound = activeTab == Tab.EXPERIENCE ? MobExperienceEntry.MAX_XP : MobDropEntry.MAX_STACK;
        minBox.setValue(String.valueOf(Mth.clamp(parseInt(minBox.getValue(), minBound) + delta, minBound, maxBound)));
        applyCurrentFields();
    }

    private void nudgeMax(int delta) {
        if (!hasEntrySelection())
            return;
        final int minBound = activeTab == Tab.EXPERIENCE ? MobExperienceEntry.MIN_XP : MobDropEntry.MIN_STACK;
        final int maxBound = activeTab == Tab.EXPERIENCE ? MobExperienceEntry.MAX_XP : MobDropEntry.MAX_STACK;
        maxBox.setValue(String.valueOf(Mth.clamp(parseInt(maxBox.getValue(), minBound) + delta, minBound, maxBound)));
        applyCurrentFields();
    }

    private void storeWorkingConfig() {
        if (selectedEntityId == null)
            return;
        menu.setSelectedEntityId(selectedEntityId);
        menu.setSelectedConfig(new EntityLootConfig(workingDrops, workingExperience, workingReplaceVanilla));
        menu.setSelectedAttributes((workingAttributes == null
                ? EntityAttributeDefaults.get(selectedEntityId)
                : workingAttributes).withDropDestination(workingDropDestination).sanitized(selectedEntityId));
    }

    private void saveAndClose() {
        applyCurrentFields();
        storeWorkingConfig();
        final List<UpdateMobDropsPacket.EntityDropsData> entityDropsList = new ArrayList<>();
        final List<ResourceLocation> entityIds = new ArrayList<>(SpawnerMobCatalog.getSpawnableIds());
        for (ResourceLocation entityId : menu.getConfigsByEntity().keySet()) {
            if (!entityIds.contains(entityId))
                entityIds.add(entityId);
        }
        for (ResourceLocation entityId : menu.getAttributesByEntity().keySet()) {
            if (!entityIds.contains(entityId))
                entityIds.add(entityId);
        }
        for (ResourceLocation entityId : entityIds) {
            final EntityLootConfig entityConfig = menu.getConfigsByEntity().getOrDefault(entityId, EntityLootConfig.EMPTY);
            final EntityAttributeConfig attributeConfig = menu.getAttributesByEntity().getOrDefault(entityId, EntityAttributeDefaults.get(entityId));
            if (!shouldPersistEntity(entityId, entityConfig, attributeConfig))
                continue;
            final List<UpdateMobDropsPacket.DropData> dropDataList = new ArrayList<>();
            for (MobDropEntry dropEntry : entityConfig.drops())
                dropDataList.add(UpdateMobDropsPacket.DropData.from(dropEntry));
            final List<UpdateMobDropsPacket.ExperienceData> experienceDataList = new ArrayList<>();
            for (MobExperienceEntry experienceEntry : entityConfig.experience())
                experienceDataList.add(UpdateMobDropsPacket.ExperienceData.from(experienceEntry));
            entityDropsList.add(new UpdateMobDropsPacket.EntityDropsData(
                    entityId.toString(),
                    dropDataList,
                    experienceDataList,
                    UpdateMobDropsPacket.AttributeData.from(attributeConfig),
                    entityConfig.replaceVanilla()
            ));
        }
        PacketDistributor.sendToServer(new UpdateMobDropsPacket(entityDropsList));
        onClose();
    }

    private static boolean shouldPersistEntity(ResourceLocation entityId, EntityLootConfig loot, EntityAttributeConfig attrs) {
        if (EntityAttributeDefaults.isModDefault(entityId))
            return true;
        if (loot != null && !loot.isEmpty())
            return true;
        if (attrs == null)
            return false;
        return !attrs.sanitized(entityId).equals(EntityAttributeDefaults.get(entityId).sanitized(entityId));
    }

    private void onSearchChanged(String query) {
        if (suppressFieldSync)
            return;
        rebuildFilteredCatalog(query);
        if (selectedEntityId != null && filteredCatalog.contains(selectedEntityId))
            return;
        if (filteredCatalog.isEmpty())
            return;
        applyCurrentFields();
        storeWorkingConfig();
        selectedEntityId = filteredCatalog.getFirst();
        loadWorkingFromMenu();
        selectedDropIndex = 0;
        selectedXpIndex = 0;
        selectedConditionIndex = 0;
        listScrollOffset = 0;
        conditionSource = Tab.LOOT;
        refreshPreviewEntity();
        rebuildExtraBoxes();
        loadFieldsFromSelection();
        updateControlsVisibility();
    }

    private void rebuildFilteredCatalog(String query) {
        filteredCatalog.clear();
        final String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        for (ResourceLocation entityId : SpawnerMobCatalog.getSpawnableIds()) {
            if (normalized.isEmpty()
                    || entityId.toString().toLowerCase(Locale.ROOT).contains(normalized)
                    || SpawnerMobCatalog.displayName(entityId).toLowerCase(Locale.ROOT).contains(normalized))
                filteredCatalog.add(entityId);
        }
    }

    private int currentListSize() {
        if (activeTab == Tab.LOOT)
            return workingDrops.size();
        if (activeTab == Tab.EXPERIENCE)
            return workingExperience.size();
        if (activeTab == Tab.CONDITIONS)
            return currentConditions().size();
        if (activeTab == Tab.CHARACTERISTICS)
            return currentExtraDefs().size();
        return 0;
    }

    private int selectedListIndex() {
        if (activeTab == Tab.EXPERIENCE)
            return selectedXpIndex;
        if (activeTab == Tab.CONDITIONS)
            return selectedConditionIndex;
        if (activeTab == Tab.CHARACTERISTICS)
            return -1;
        return selectedDropIndex;
    }

    private int visibleRowCount() {
        return activeTab == Tab.CONDITIONS ? CONDITION_VISIBLE_ROWS : VISIBLE_ROWS;
    }

    private int rowHeight() {
        return activeTab == Tab.CONDITIONS ? CONDITION_ROW_HEIGHT : ROW_HEIGHT;
    }

    private void ensureSelectionVisible() {
        if (isCharacteristicsTab())
            return;
        final int selectedIndex = selectedListIndex();
        final int visibleRows = visibleRowCount();
        if (selectedIndex < listScrollOffset)
            listScrollOffset = selectedIndex;
        if (selectedIndex >= listScrollOffset + visibleRows)
            listScrollOffset = selectedIndex - visibleRows + 1;
        listScrollOffset = Mth.clamp(listScrollOffset, 0, Math.max(0, currentListSize() - visibleRows));
    }

    private int listLeft() {
        return leftPos + CENTER_X + 8;
    }

    private int listTop() {
        return topPos + LIST_BODY_Y;
    }

    private int maxScroll() {
        return Math.max(0, currentListSize() - visibleRowCount());
    }

    private boolean needsScrollbar() {
        return maxScroll() > 0;
    }

    private int listWidth() {
        return needsScrollbar() ? CENTER_W - 16 - SCROLLBAR_WIDTH - 4 : CENTER_W - 16;
    }

    private int listHeight() {
        return visibleRowCount() * rowHeight();
    }

    private int scrollbarLeft() {
        return listLeft() + listWidth() + 2;
    }

    private int scrollbarThumbHeight() {
        final int trackHeight = listHeight();
        return Mth.clamp(trackHeight * visibleRowCount() / Math.max(1, currentListSize()), 18, trackHeight);
    }

    private int scrollbarThumbTop() {
        final int trackHeight = listHeight();
        final int thumbHeight = scrollbarThumbHeight();
        final int travel = Math.max(1, trackHeight - thumbHeight);
        return listTop() + (int) ((long) listScrollOffset * travel / Math.max(1, maxScroll()));
    }

    private void setScrollFromMouseY(double mouseY) {
        final int trackHeight = listHeight();
        final int thumbHeight = scrollbarThumbHeight();
        final int travel = Math.max(1, trackHeight - thumbHeight);
        final double relative = Mth.clamp(mouseY - listTop() - thumbHeight * 0.5D, 0.0D, travel);
        listScrollOffset = Mth.clamp((int) Math.round(relative * maxScroll() / travel), 0, maxScroll());
        updateExtraBoxLayout();
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        if (!needsScrollbar())
            return false;
        final int barLeft = scrollbarLeft();
        return mouseX >= barLeft && mouseX <= barLeft + SCROLLBAR_WIDTH
                && mouseY >= listTop() && mouseY <= listTop() + listHeight();
    }

    private int rowIndexAt(double mouseX, double mouseY) {
        if (!isEntryListTab() && !isConditionTab())
            return -1;
        if (mouseX < listLeft() || mouseX > listLeft() + listWidth() || mouseY < listTop() || mouseY > listTop() + listHeight())
            return -1;
        final int entryIndex = listScrollOffset + (int) ((mouseY - listTop()) / rowHeight());
        return entryIndex >= 0 && entryIndex < currentListSize() ? entryIndex : -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        syncFitScale();
        mouseX = GuiFitScale.unscaleX(mouseX);
        mouseY = GuiFitScale.unscaleY(mouseY);
        if (itemPickerOverlay != null) {
            if (!itemPickerOverlay.mouseClicked(mouseX, mouseY, button))
                closeItemPicker();
            else
                super.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        if (button == 0 && (isEntryListTab() || isConditionTab() || isCharacteristicsTab()) && isOverScrollbar(mouseX, mouseY)) {
            draggingScrollbar = true;
            setScrollFromMouseY(mouseY);
            return true;
        }
        if (button == 0) {
            final int rowIndex = rowIndexAt(mouseX, mouseY);
            if (rowIndex >= 0) {
                applyCurrentFields();
                if (activeTab == Tab.LOOT) {
                    selectedDropIndex = rowIndex;
                    conditionSource = Tab.LOOT;
                    selectedConditionIndex = 0;
                } else if (activeTab == Tab.EXPERIENCE) {
                    selectedXpIndex = rowIndex;
                    conditionSource = Tab.EXPERIENCE;
                    selectedConditionIndex = 0;
                } else if (activeTab == Tab.CONDITIONS) {
                    selectedConditionIndex = rowIndex;
                }
                dragFromIndex = rowIndex;
                isDraggingRow = false;
                dragPointerY = mouseY;
                loadFieldsFromSelection();
                updateControlsVisibility();
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
        if (itemPickerOverlay != null) {
            itemPickerOverlay.mouseDragged(mouseX, mouseY, button);
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        if (button == 0 && draggingScrollbar) {
            setScrollFromMouseY(mouseY);
            return true;
        }
        if (button == 0 && dragFromIndex >= 0 && (isEntryListTab() || isConditionTab())) {
            isDraggingRow = true;
            dragPointerY = mouseY;
            final int targetIndex = rowIndexAt(mouseX, mouseY);
            if (targetIndex >= 0 && targetIndex != dragFromIndex) {
                moveListEntry(dragFromIndex, targetIndex);
                dragFromIndex = targetIndex;
                ensureSelectionVisible();
            } else if (mouseY < listTop() && listScrollOffset > 0) {
                listScrollOffset--;
            } else if (mouseY > listTop() + listHeight() && listScrollOffset < maxScroll()) {
                listScrollOffset++;
            }
            return true;
        }
        if (chanceSlider != null && chanceSlider.isDragging())
            return chanceSlider.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void moveListEntry(int fromIndex, int toIndex) {
        if (activeTab == Tab.LOOT) {
            final MobDropEntry movedEntry = workingDrops.remove(fromIndex);
            workingDrops.add(toIndex, movedEntry);
            selectedDropIndex = toIndex;
            return;
        }
        if (activeTab == Tab.EXPERIENCE) {
            final MobExperienceEntry movedEntry = workingExperience.remove(fromIndex);
            workingExperience.add(toIndex, movedEntry);
            selectedXpIndex = toIndex;
            return;
        }
        if (activeTab == Tab.CONDITIONS && hasConditionTarget()) {
            final List<LootConditionEntry> nextConditions = new ArrayList<>(currentConditions());
            final LootConditionEntry movedCondition = nextConditions.remove(fromIndex);
            nextConditions.add(toIndex, movedCondition);
            selectedConditionIndex = toIndex;
            setCurrentConditions(nextConditions);
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        syncFitScale();
        mouseX = GuiFitScale.unscaleX(mouseX);
        mouseY = GuiFitScale.unscaleY(mouseY);
        if (itemPickerOverlay != null) {
            itemPickerOverlay.mouseReleased(mouseX, mouseY, button);
            return super.mouseReleased(mouseX, mouseY, button);
        }
        if (chanceSlider != null && chanceSlider.isDragging())
            chanceSlider.mouseReleased(mouseX, mouseY, button);
        dragFromIndex = -1;
        isDraggingRow = false;
        draggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        syncFitScale();
        mouseX = GuiFitScale.unscaleX(mouseX);
        mouseY = GuiFitScale.unscaleY(mouseY);
        if (itemPickerOverlay != null)
            return itemPickerOverlay.mouseScrolled(mouseX, mouseY, scrollY);
        if (mouseX >= leftPos + LEFT_X && mouseX <= leftPos + LEFT_X + LEFT_W
                && mouseY >= topPos + CONTENT_TOP && mouseY <= topPos + 166) {
            cycleEntity(scrollY < 0 ? 1 : -1);
            return true;
        }
        if ((isEntryListTab() || isConditionTab() || isCharacteristicsTab()) && mouseX >= listLeft() && mouseX <= scrollbarLeft() + SCROLLBAR_WIDTH
                && mouseY >= listTop() && mouseY <= listTop() + listHeight()) {
            listScrollOffset = Mth.clamp(listScrollOffset - (int) Math.signum(scrollY), 0, maxScroll());
            updateExtraBoxLayout();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (itemPickerOverlay != null && itemPickerOverlay.keyPressed(keyCode, scanCode, modifiers))
            return true;
        if (isTextFieldFocused()) {
            if (getFocused() != null && getFocused().keyPressed(keyCode, scanCode, modifiers))
                return true;
            if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode))
                return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean isTextFieldFocused() {
        return getFocused() instanceof EditBox editBox && editBox.isFocused();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        final int rawMouseX = mouseX;
        final int rawMouseY = mouseY;
        syncFitScale();
        renderTransparentBackground(guiGraphics);
        final int fitMouseX = GuiFitScale.unscaleMouseX(rawMouseX);
        final int fitMouseY = GuiFitScale.unscaleMouseY(rawMouseY);
        try {
            GuiFitScale.push(guiGraphics, width, height, fitGuiWidth(), fitGuiHeight());
            super.render(guiGraphics, fitMouseX, fitMouseY, partialTick);
        } finally {
            GuiFitScale.pop(guiGraphics);
        }
        renderTooltip(guiGraphics, rawMouseX, rawMouseY);
        if (itemPickerOverlay == null && isCharacteristicsTab())
            renderCharacteristicsHoverTooltip(guiGraphics, fitMouseX, fitMouseY, rawMouseX, rawMouseY);
        tickPreviewEntity();
    }

    private void renderCharacteristicsHoverTooltip(GuiGraphics guiGraphics, int hitX, int hitY, int tooltipX, int tooltipY) {
        final int extraRow = characteristicsRowAt(hitX, hitY);
        if (extraRow >= 0) {
            final int valueLeft = listLeft() + listWidth() - EXTRA_VALUE_WIDTH - 2;
            if (hitX < valueLeft) {
                final ExtraParameterDefinition def = currentExtraDefs().get(extraRow);
                guiGraphics.renderComponentTooltip(font, List.of(
                        Component.translatable(def.langKey()),
                        Component.translatable(def.tooltipLangKey()).withStyle(ChatFormatting.GRAY)
                ), tooltipX, tooltipY);
            }
            return;
        }
        final int attrIndex = baseAttributeIndexAt(hitX, hitY);
        if (attrIndex < 0)
            return;
        final String attrKey = BASE_ATTR_KEYS[attrIndex];
        guiGraphics.renderComponentTooltip(font, List.of(
                Component.translatable("gui.mobexpansion.mob_drop_config.attr." + attrKey),
                Component.translatable("gui.mobexpansion.mob_drop_config.attr." + attrKey + ".tooltip").withStyle(ChatFormatting.GRAY)
        ), tooltipX, tooltipY);
    }

    private static final String[] BASE_ATTR_KEYS = {
            "max_health", "armor", "armor_toughness", "attack_damage",
            "movement_speed", "knockback_resistance", "follow_range"
    };

    private int characteristicsRowAt(double mouseX, double mouseY) {
        if (!isCharacteristicsTab())
            return -1;
        if (mouseX < listLeft() || mouseX > listLeft() + listWidth() || mouseY < listTop() || mouseY > listTop() + listHeight())
            return -1;
        final int entryIndex = listScrollOffset + (int) ((mouseY - listTop()) / rowHeight());
        return entryIndex >= 0 && entryIndex < currentListSize() ? entryIndex : -1;
    }

    private int baseAttributeIndexAt(double mouseX, double mouseY) {
        final int labelX = leftPos + RIGHT_FIELD_X;
        if (mouseX < labelX || mouseX > labelX + RIGHT_FIELD_W)
            return -1;
        for (int index = 0; index < BASE_ATTR_KEYS.length; index++) {
            final int labelTop = topPos + ATTR_FIELD_START_Y + ATTR_FIELD_STRIDE * index + ATTR_LABEL_OFFSET;
            final int boxTop = topPos + ATTR_FIELD_START_Y + ATTR_FIELD_STRIDE * index + ATTR_BOX_OFFSET;
            if (mouseY >= labelTop && mouseY < boxTop)
                return index;
        }
        return -1;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        final int left = leftPos;
        final int top = topPos;
        OreUIPanel.renderDark(guiGraphics, left, top, imageWidth, imageHeight);
        OreUIPanel.renderLight(guiGraphics, left + LEFT_X, top + CONTENT_TOP, LEFT_W, 104);
        OreUIPanel.renderLight(guiGraphics, left + CENTER_X, top + CONTENT_TOP, CENTER_W, CONTENT_H);
        OreUIPanel.renderLight(guiGraphics, left + RIGHT_X, top + CONTENT_TOP, RIGHT_W, CONTENT_H);
        if (itemPickerOverlay == null) {
            renderPreview(guiGraphics, mouseX, mouseY);
            if (activeTab == Tab.LOOT)
                renderLootTab(guiGraphics);
            else if (activeTab == Tab.EXPERIENCE)
                renderExperienceTab(guiGraphics);
            else if (activeTab == Tab.CONDITIONS)
                renderConditionsTab(guiGraphics);
            else
                renderCharacteristicsTab(guiGraphics);
        } else {
            itemPickerOverlay.render(guiGraphics, mouseX, mouseY);
        }
    }

    private void renderPreview(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (previewEntity == null)
            return;
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                guiGraphics,
                leftPos + LEFT_X + 4,
                topPos + CONTENT_TOP + 4,
                leftPos + LEFT_X + LEFT_W - 4,
                topPos + CONTENT_TOP + 100,
                32,
                0.0625F,
                mouseX,
                mouseY,
                previewEntity
        );
    }

    private void renderLootTab(GuiGraphics guiGraphics) {
        final int left = leftPos;
        final int top = topPos;
        final int listLeft = listLeft();
        guiGraphics.drawString(font, Component.translatable("gui.mobexpansion.mob_drop_config.table"), left + CENTER_X + 8, top + 40, COLOR_TEXT);
        guiGraphics.drawString(font, Component.translatable("gui.mobexpansion.mob_drop_config.col.item"), listLeft, top + LIST_HEADER_Y, COLOR_MUTED);
        guiGraphics.drawString(font, Component.translatable("gui.mobexpansion.mob_drop_config.col.chance"), listLeft + 130, top + LIST_HEADER_Y, COLOR_MUTED);
        guiGraphics.drawString(font, Component.translatable("gui.mobexpansion.mob_drop_config.col.min"), listLeft + 176, top + LIST_HEADER_Y, COLOR_MUTED);
        guiGraphics.drawString(font, Component.translatable("gui.mobexpansion.mob_drop_config.col.max"), listLeft + 200, top + LIST_HEADER_Y, COLOR_MUTED);
        renderEntryList(guiGraphics);
        renderListScrollbar(guiGraphics);
        renderRightPanelLabels(guiGraphics, true);
    }

    private void renderExperienceTab(GuiGraphics guiGraphics) {
        final int left = leftPos;
        final int top = topPos;
        final int listLeft = listLeft();
        guiGraphics.drawString(font, Component.translatable("gui.mobexpansion.mob_drop_config.xp_table"), left + CENTER_X + 8, top + 40, COLOR_TEXT);
        guiGraphics.drawString(font, Component.translatable("gui.mobexpansion.mob_drop_config.col.xp"), listLeft, top + LIST_HEADER_Y, COLOR_MUTED);
        guiGraphics.drawString(font, Component.translatable("gui.mobexpansion.mob_drop_config.col.chance"), listLeft + 120, top + LIST_HEADER_Y, COLOR_MUTED);
        guiGraphics.drawString(font, Component.translatable("gui.mobexpansion.mob_drop_config.col.conditions"), listLeft + 170, top + LIST_HEADER_Y, COLOR_MUTED);
        renderEntryList(guiGraphics);
        renderListScrollbar(guiGraphics);
        final int hintY = top + LIST_BODY_Y + listHeight() + 6;
        guiGraphics.drawWordWrap(font, Component.translatable("gui.mobexpansion.mob_drop_config.drag_hint_xp"), left + CENTER_X + 8, hintY + 20, CENTER_W - 16, COLOR_MUTED);
        renderRightPanelLabels(guiGraphics, false);
    }

    private void renderConditionsTab(GuiGraphics guiGraphics) {
        final int left = leftPos;
        final int top = topPos;
        final int listLeft = listLeft();
        final boolean hasConditions = hasConditionTarget() && !currentConditions().isEmpty();
        guiGraphics.drawString(font, Component.translatable("gui.mobexpansion.mob_drop_config.conditions_table"), left + CENTER_X + 8, top + 40, COLOR_TEXT, false);
        if (hasConditions)
            guiGraphics.drawString(font, Component.translatable("gui.mobexpansion.mob_drop_config.col.condition"), listLeft, top + LIST_HEADER_Y, COLOR_MUTED, false);
        renderEntryList(guiGraphics);
        renderListScrollbar(guiGraphics);
        if (!hasConditionTarget()) {
            guiGraphics.drawCenteredString(font, Component.translatable("gui.mobexpansion.condition.no_target"), left + CENTER_X + CENTER_W / 2, top + 120, COLOR_MUTED);
            guiGraphics.drawWordWrap(font, Component.translatable("gui.mobexpansion.mob_drop_config.conditions_hint"), left + CENTER_X + 24, top + 140, CENTER_W - 48, COLOR_MUTED);
        } else if (!hasConditions) {
            guiGraphics.drawCenteredString(font, Component.translatable("gui.mobexpansion.condition.empty"), left + CENTER_X + CENTER_W / 2, top + 130, COLOR_MUTED);
        }
        renderConditionRightPanel(guiGraphics);
    }

    private void renderEntryList(GuiGraphics guiGraphics) {
        final int endIndex = Math.min(currentListSize(), listScrollOffset + visibleRowCount());
        final int selectedIndex = selectedListIndex();
        if (isDraggingRow && dragFromIndex >= 0 && dragFromIndex < currentListSize()) {
            guiGraphics.fill(listLeft(), listTop(), listLeft() + listWidth(), listTop() + listHeight(), 0xFF181818);
            for (int entryIndex = listScrollOffset; entryIndex < endIndex; entryIndex++) {
                if (entryIndex == dragFromIndex)
                    continue;
                renderEntryRow(guiGraphics, entryIndex, listTop() + (entryIndex - listScrollOffset) * rowHeight(), true, false);
            }
            final int floatingY = Mth.clamp((int) dragPointerY - DRAG_ROW_HEIGHT / 2, listTop() - 4, listTop() + listHeight() - DRAG_ROW_HEIGHT + 4);
            renderEntryRow(guiGraphics, dragFromIndex, floatingY, false, true);
            return;
        }
        for (int entryIndex = listScrollOffset; entryIndex < endIndex; entryIndex++)
            renderEntryRow(guiGraphics, entryIndex, listTop() + (entryIndex - listScrollOffset) * rowHeight(), false, entryIndex == selectedIndex && !isDraggingRow);
    }

    private void renderEntryRow(GuiGraphics guiGraphics, int entryIndex, int rowY, boolean dimmed, boolean selectedOrEnlarged) {
        final boolean enlarged = isDraggingRow && entryIndex == dragFromIndex;
        final int rowLeft = enlarged ? listLeft() - 3 : listLeft();
        final int rowWidth = enlarged ? listWidth() + 6 : listWidth();
        final int currentRowHeight = enlarged ? DRAG_ROW_HEIGHT : rowHeight();
        if (enlarged) {
            guiGraphics.fill(rowLeft - 1, rowY - 1, rowLeft + rowWidth + 1, rowY + currentRowHeight, COLOR_ACCENT);
            guiGraphics.fill(rowLeft, rowY, rowLeft + rowWidth, rowY + currentRowHeight - 1, 0xFF2F3A2F);
        } else if (selectedOrEnlarged) {
            guiGraphics.fill(rowLeft, rowY, rowLeft + rowWidth, rowY + currentRowHeight - 1, 0x554CAF50);
        } else if (dimmed) {
            guiGraphics.fill(rowLeft, rowY, rowLeft + rowWidth, rowY + currentRowHeight - 1, 0x33121820);
        }

        final int textY = rowY + (enlarged ? 9 : 7);
        final int textColor = dimmed ? 0xFF6A7080 : COLOR_TEXT;
        if (activeTab == Tab.CHARACTERISTICS) {
            final ExtraParameterDefinition def = currentExtraDefs().get(entryIndex);
            guiGraphics.drawString(font, font.plainSubstrByWidth(Component.translatable(def.langKey()).getString(), rowWidth - EXTRA_VALUE_WIDTH - 12), rowLeft + 4, textY, textColor);
        } else if (activeTab == Tab.CONDITIONS) {
            final LootConditionEntry condition = currentConditions().get(entryIndex);
            guiGraphics.drawString(font, font.plainSubstrByWidth(condition.displayLabel().getString(), rowWidth - 8), rowLeft + 4, textY, textColor);
        } else if (activeTab == Tab.LOOT) {
            final MobDropEntry dropEntry = workingDrops.get(entryIndex);
            final ItemStack itemStack = stackFromId(dropEntry.itemId());
            final int nameWidth = Math.max(40, rowWidth - 120);
            guiGraphics.renderItem(itemStack, rowLeft + 2, rowY + (enlarged ? 5 : 3));
            guiGraphics.drawString(font, font.plainSubstrByWidth(itemStack.getHoverName().getString(), nameWidth), rowLeft + 22, textY, textColor);
            guiGraphics.drawString(font, Math.round(dropEntry.chance() * 100.0F) + "%", rowLeft + 130, textY, dimmed ? 0xFF5A6A50 : chanceColor(dropEntry.chance()));
            guiGraphics.drawString(font, String.valueOf(dropEntry.minCount()), rowLeft + 176, textY, textColor);
            guiGraphics.drawString(font, String.valueOf(dropEntry.maxCount()), rowLeft + 200, textY, textColor);
            if (!dropEntry.conditions().isEmpty())
                guiGraphics.drawString(font, "+" + dropEntry.conditions().size(), rowLeft + rowWidth - 22, textY, dimmed ? 0xFF4A5160 : COLOR_MUTED);
        } else {
            final MobExperienceEntry experienceEntry = workingExperience.get(entryIndex);
            final String amountText = experienceEntry.minAmount() == experienceEntry.maxAmount()
                    ? experienceEntry.minAmount() + " XP"
                    : experienceEntry.minAmount() + "-" + experienceEntry.maxAmount() + " XP";

            guiGraphics.renderItem(new ItemStack(Items.EXPERIENCE_BOTTLE), rowLeft + 2, rowY + (enlarged ? 5 : 3));
            guiGraphics.drawString(font, amountText, rowLeft + 22, textY, textColor);
            guiGraphics.drawString(font, Math.round(experienceEntry.chance() * 100.0F) + "%", rowLeft + 120, textY, dimmed ? 0xFF5A6A50 : chanceColor(experienceEntry.chance()));
            guiGraphics.drawString(font, experienceEntry.conditions().isEmpty() ? "-" : "+" + experienceEntry.conditions().size(), rowLeft + 170, textY, dimmed ? 0xFF4A5160 : COLOR_MUTED);
        }
        if (dimmed)
            guiGraphics.fill(rowLeft, rowY, rowLeft + rowWidth, rowY + currentRowHeight - 1, 0x88000000);
    }

    private void renderListScrollbar(GuiGraphics guiGraphics) {
        if (!needsScrollbar())
            return;
        final int barLeft = scrollbarLeft();
        final int barTop = listTop();
        final int barHeight = listHeight();
        NineSlicePainter.blit(guiGraphics, GuiTextures.SCROLL_GUTTER, barLeft, barTop, SCROLLBAR_WIDTH, barHeight, 3, 1);
        NineSlicePainter.blit(guiGraphics, GuiTextures.SCROLL_HANDLE, barLeft, scrollbarThumbTop(), SCROLLBAR_WIDTH, scrollbarThumbHeight(), 3, 5, 1, 2, 1, 2);
    }

    private void renderRightPanelLabels(GuiGraphics guiGraphics, boolean lootMode) {
        final int labelX = leftPos + RIGHT_FIELD_X;
        final int top = topPos;
        guiGraphics.drawString(font, Component.translatable(lootMode ? "gui.mobexpansion.mob_drop_config.params" : "gui.mobexpansion.mob_drop_config.xp_params"), leftPos + RIGHT_X + 8, top + 38, COLOR_TEXT);
        if (!hasEntrySelection()) {
            guiGraphics.drawCenteredString(font, Component.translatable(lootMode ? "gui.mobexpansion.mob_drop_config.no_drops" : "gui.mobexpansion.mob_drop_config.no_experience"), leftPos + RIGHT_X + RIGHT_W / 2, top + CONTENT_H / 2, COLOR_MUTED);
            return;
        }
        if (lootMode) {
            final ItemStack itemStack = stackFromId(workingDrops.get(selectedDropIndex).itemId());
            guiGraphics.renderItem(itemStack, labelX, top + 54);
            guiGraphics.drawString(font, itemStack.getHoverName(), labelX + 22, top + 58, COLOR_TEXT);
        } else {
            final MobExperienceEntry experienceEntry = workingExperience.get(selectedXpIndex);
            guiGraphics.renderItem(new ItemStack(Items.EXPERIENCE_BOTTLE), labelX, top + 54);
            guiGraphics.drawString(font, experienceEntry.displayLabel(), labelX + 22, top + 58, COLOR_TEXT);
        }
        guiGraphics.drawString(font, Component.translatable("gui.mobexpansion.mob_drop_config.chance_label"), labelX, top + 106, COLOR_MUTED);
        guiGraphics.drawString(font, "%", labelX + 56, top + 124, COLOR_MUTED);
        guiGraphics.drawString(font, Component.translatable(lootMode ? "gui.mobexpansion.mob_drop_config.amount" : "gui.mobexpansion.mob_drop_config.xp_amount"), labelX, top + 160, COLOR_MUTED);
        guiGraphics.drawString(font, Component.translatable("gui.mobexpansion.mob_drop_config.min"), labelX + 96, top + 182, COLOR_MUTED);
        guiGraphics.drawString(font, Component.translatable("gui.mobexpansion.mob_drop_config.max"), labelX + 96, top + 208, COLOR_MUTED);
    }

    private void renderConditionRightPanel(GuiGraphics guiGraphics) {
        final int labelX = leftPos + RIGHT_FIELD_X;
        final int top = topPos;
        final int rightCenterX = leftPos + RIGHT_X + RIGHT_W / 2;
        guiGraphics.drawString(font, Component.translatable("gui.mobexpansion.mob_drop_config.condition_target"), leftPos + RIGHT_X + 8, top + 38, COLOR_TEXT);
        if (!hasConditionTarget()) {
            guiGraphics.drawCenteredString(font, Component.translatable("gui.mobexpansion.condition.no_target"), rightCenterX, top + 150, COLOR_MUTED);
            return;
        }
        final String targetLabel = conditionSource == Tab.EXPERIENCE ? workingExperience.get(selectedXpIndex).displayLabel() : stackFromId(workingDrops.get(selectedDropIndex).itemId()).getHoverName().getString();
        guiGraphics.drawString(font, font.plainSubstrByWidth(targetLabel, RIGHT_FIELD_W), labelX, top + 84, COLOR_TEXT);
        if (currentConditions().isEmpty()) {
            guiGraphics.drawCenteredString(font, Component.translatable("gui.mobexpansion.condition.empty"), rightCenterX, top + 150, COLOR_MUTED);
            return;
        }
        guiGraphics.drawString(font, Component.translatable("gui.mobexpansion.mob_drop_config.condition_type"), labelX, top + 106, COLOR_MUTED);
        final LootConditionEntry selectedCondition = currentConditions().get(selectedConditionIndex);
        if (selectedCondition.type() == LootConditionType.MATCH_TIME || selectedCondition.type() == LootConditionType.IS_LIGHT_LEVEL)
            guiGraphics.drawString(font, Component.translatable("gui.mobexpansion.condition.value_hint"), labelX, top + CONDITION_PARAM_Y - 12, COLOR_MUTED);
    }

    private void renderCharacteristicsTab(GuiGraphics guiGraphics) {
        final int left = leftPos;
        final int top = topPos;
        final int listLeft = listLeft();
        final int labelX = left + RIGHT_FIELD_X;
        guiGraphics.drawString(font, Component.translatable("gui.mobexpansion.mob_drop_config.extras_table"), left + CENTER_X + 8, top + 40, COLOR_TEXT);
        guiGraphics.drawString(font, Component.translatable("gui.mobexpansion.mob_drop_config.col.extra"), listLeft, top + LIST_HEADER_Y, COLOR_MUTED);
        guiGraphics.drawString(font, Component.translatable("gui.mobexpansion.mob_drop_config.col.value"), listLeft + listWidth() - EXTRA_VALUE_WIDTH, top + LIST_HEADER_Y, COLOR_MUTED);
        if (currentExtraDefs().isEmpty())
            guiGraphics.drawCenteredString(font, Component.translatable("gui.mobexpansion.mob_drop_config.no_extras"), left + CENTER_X + CENTER_W / 2, top + 130, COLOR_MUTED);
        else {
            renderEntryList(guiGraphics);
            renderListScrollbar(guiGraphics);
        }
        guiGraphics.drawWordWrap(font, Component.translatable("gui.mobexpansion.mob_drop_config.attributes_hint"), left + CENTER_X + 8, top + LIST_BODY_Y + listHeight() + 8, CENTER_W - 16, COLOR_MUTED);

        guiGraphics.drawString(font, Component.translatable("gui.mobexpansion.mob_drop_config.attributes_title"), left + RIGHT_X + 8, top + 38, COLOR_TEXT);
        guiGraphics.drawString(font, Component.translatable("gui.mobexpansion.mob_drop_config.attr.max_health"), labelX, top + ATTR_FIELD_START_Y + ATTR_LABEL_OFFSET, COLOR_MUTED);
        guiGraphics.drawString(font, Component.translatable("gui.mobexpansion.mob_drop_config.attr.armor"), labelX, top + ATTR_FIELD_START_Y + ATTR_FIELD_STRIDE + ATTR_LABEL_OFFSET, COLOR_MUTED);
        guiGraphics.drawString(font, Component.translatable("gui.mobexpansion.mob_drop_config.attr.armor_toughness"), labelX, top + ATTR_FIELD_START_Y + ATTR_FIELD_STRIDE * 2 + ATTR_LABEL_OFFSET, COLOR_MUTED);
        guiGraphics.drawString(font, Component.translatable("gui.mobexpansion.mob_drop_config.attr.attack_damage"), labelX, top + ATTR_FIELD_START_Y + ATTR_FIELD_STRIDE * 3 + ATTR_LABEL_OFFSET, COLOR_MUTED);
        guiGraphics.drawString(font, Component.translatable("gui.mobexpansion.mob_drop_config.attr.movement_speed"), labelX, top + ATTR_FIELD_START_Y + ATTR_FIELD_STRIDE * 4 + ATTR_LABEL_OFFSET, COLOR_MUTED);
        guiGraphics.drawString(font, Component.translatable("gui.mobexpansion.mob_drop_config.attr.knockback_resistance"), labelX, top + ATTR_FIELD_START_Y + ATTR_FIELD_STRIDE * 5 + ATTR_LABEL_OFFSET, COLOR_MUTED);
        guiGraphics.drawString(font, Component.translatable("gui.mobexpansion.mob_drop_config.attr.follow_range"), labelX, top + ATTR_FIELD_START_Y + ATTR_FIELD_STRIDE * 6 + ATTR_LABEL_OFFSET, COLOR_MUTED);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (itemPickerOverlay != null)
            return;
        guiGraphics.drawString(font, Component.translatable("gui.mobexpansion.mob_drop_config.title", SpawnerMobCatalog.displayName(selectedEntityId)), 12, 10, COLOR_TEXT);
    }

    private static ItemStack stackFromId(ResourceLocation itemId) {
        return new ItemStack(BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.BARRIER));
    }

    private static int chanceColor(float chance) {
        if (chance >= 0.7F)
            return 0xFF6FCF6F;
        if (chance >= 0.2F)
            return 0xFFE0B24A;
        if (chance >= 0.05F)
            return 0xFFE07A3A;
        return 0xFFE14B4B;
    }

    private static int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim().replace("%", ""));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double parseDouble(String text, double fallback) {
        try {
            return Double.parseDouble(text.trim().replace(',', '.'));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String formatAttribute(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001D)
            return String.valueOf((long) Math.rint(value));
        return String.format(Locale.ROOT, "%.4f", value);
    }
}

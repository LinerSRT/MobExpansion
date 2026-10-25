package net.mcskill.mobexpansion.client.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.client.ClientClearance;
import net.mcskill.mobexpansion.client.info.ClientMobInfo;
import net.mcskill.mobexpansion.info.MobInfo;
import net.mcskill.mobexpansion.info.MobSpawnPoint;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Страница моба в EMI (текстура {@code textures/gui/emi/mob_info.png}, 248×166):
 * слева — модель, имя, HP и уровень допуска; справа — расположение (спавнеры), урон, таймер респавна,
 * броня, опыт; снизу справа — до 12 предметов дропа.
 */
public class MobInfoEmiRecipe implements EmiRecipe {
    public static final ResourceLocation TEXTURE = Core.loc("textures/gui/emi/mob_info.png");
    private static final int WIDTH = 248;
    private static final int HEIGHT = 166;

    // Левая верхняя панель (модель + имя)
    private static final int MODEL_LEFT = 11;
    private static final int MODEL_TOP = 11;
    private static final int MODEL_RIGHT = 105;
    private static final int MODEL_BOTTOM = 103;
    private static final int NAME_Y = 106;
    /** Доля панели, которую занимает хитбокс моба. */
    private static final float MODEL_FILL = 0.5F;
    // Левая нижняя панель (HP + допуск)
    private static final int STATS_X = 16;
    private static final int HP_LABEL_Y = 121;
    private static final int HP_BAR_X = 16;
    private static final int HP_BAR_Y = 130;
    private static final int HP_BAR_W = 84;
    private static final int HP_BAR_H = 6;
    private static final int CLEARANCE_LABEL_Y = 137;
    private static final int SEGMENT_X = 16;
    private static final int SEGMENT_Y = 146;
    private static final int SEGMENT_W = 16;
    private static final int SEGMENT_H = 6;
    private static final int SEGMENT_STEP = 17;
    private static final int SEGMENT_FILLED_V = 250;
    // Правая верхняя панель (текст)
    private static final int INFO_X = 123;
    private static final int INFO_Y = 13;
    private static final int INFO_W = 111;
    private static final int LINE_H = 10;
    // Дроп
    private static final int DROPS_LABEL_Y = 110;
    private static final int SLOTS_X = 125;
    private static final int SLOTS_Y = 120;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_COLUMNS = 6;
    private static final int SLOT_COUNT = 12;

    private static final int COLOR_LABEL = 0xFF9DA3A6;
    private static final int COLOR_VALUE = 0xFFFFFFFF;
    private static final int COLOR_ALIVE = 0xFF6CE06C;
    private static final int COLOR_DROPS_LABEL = 0xFF404040;

    private final MobInfo info;
    private final int order;
    private final ResourceLocation id;
    private final EntityType<?> entityType;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;

    @Nullable
    private LivingEntity previewEntity;
    @Nullable
    private Level previewLevel;
    private boolean previewFailed;

    public MobInfoEmiRecipe(MobInfo info, int order) {
        this.info = info;
        this.order = order;
        final ResourceLocation entityId = info.entityId();
        this.id = Core.loc("/mob_info/" + entityId.getNamespace() + "/" + entityId.getPath());
        this.entityType = BuiltInRegistries.ENTITY_TYPE.get(entityId);

        final SpawnEggItem egg = SpawnEggItem.byId(entityType);
        this.inputs = egg == null ? List.of() : List.of(EmiStack.of(egg));

        final Map<Item, EmiStack> unique = new LinkedHashMap<>();
        for (MobInfo.Drop drop : info.drops()) {
            final Item item = BuiltInRegistries.ITEM.get(drop.itemId());
            unique.putIfAbsent(item, EmiStack.of(item));
        }
        this.outputs = List.copyOf(unique.values());
    }

    public int order() {
        return order;
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return MobExEmiPlugin.MOB_INFO;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return inputs;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return outputs;
    }

    @Override
    public int getDisplayWidth() {
        return WIDTH;
    }

    @Override
    public int getDisplayHeight() {
        return HEIGHT;
    }

    @Override
    public boolean supportsRecipeTree() {
        return false;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        ClientMobInfo.requestSpawnPoints(info.entityId());
        final Font font = Minecraft.getInstance().font;

        widgets.addTexture(TEXTURE, 0, 0, WIDTH, HEIGHT, 0, 0);
        widgets.addDrawable(0, 0, WIDTH, HEIGHT, (graphics, mouseX, mouseY, delta) -> {
            renderModel(graphics);
            renderLeftPanel(graphics, font);
            renderInfoPanel(graphics, font);
        });

        // Подсказки слева
        widgets.addTooltipText(List.of(
                Component.translatable("gui.mobexpansion.emi.hp.exact", formatPlain(info.maxHealth())),
                Component.translatable("gui.mobexpansion.emi.armor", formatPlain(info.armor())).withStyle(ChatFormatting.GRAY),
                Component.translatable("gui.mobexpansion.emi.speed", formatSpeed(info.movementSpeed())).withStyle(ChatFormatting.GRAY)
        ), STATS_X, HP_LABEL_Y, HP_BAR_W, HP_BAR_Y + HP_BAR_H - HP_LABEL_Y);
        widgets.addTooltip((mouseX, mouseY) -> toTooltip(clearanceTooltip()),
                STATS_X, CLEARANCE_LABEL_Y, SEGMENT_STEP * 5, SEGMENT_Y + SEGMENT_H - CLEARANCE_LABEL_Y);

        // Подсказки справа: все спавнеры и урон способностей
        widgets.addTooltip((mouseX, mouseY) -> toTooltip(locationTooltip()), INFO_X, INFO_Y, INFO_W, LINE_H * 2);
        if (!info.abilityDamage().isEmpty())
            widgets.addTooltip((mouseX, mouseY) -> toTooltip(damageTooltip()), INFO_X, INFO_Y + LINE_H * 2, INFO_W, LINE_H);

        // Дроп
        final Component dropsLabel = Component.translatable("gui.mobexpansion.emi.drops");
        final int labelX = SLOTS_X + (SLOT_SIZE * SLOT_COLUMNS - font.width(dropsLabel)) / 2;
        widgets.addText(dropsLabel, labelX, DROPS_LABEL_Y, COLOR_DROPS_LABEL, false);
        final List<MobInfo.Drop> drops = info.drops();
        for (int i = 0; i < Math.min(SLOT_COUNT, drops.size()); i++) {
            final MobInfo.Drop drop = drops.get(i);
            // Количество рисуем сами (см. renderDropCounts) — EMI диапазон не умеет.
            final EmiStack stack = EmiStack.of(BuiltInRegistries.ITEM.get(drop.itemId()));
            if (drop.chance() < 1.0F)
                stack.setChance(drop.chance());
            final var slot = widgets.addSlot(stack, SLOTS_X + (i % SLOT_COLUMNS) * SLOT_SIZE, SLOTS_Y + (i / SLOT_COLUMNS) * SLOT_SIZE)
                    .drawBack(false)
                    .recipeContext(this);
            if (drop.minCount() != drop.maxCount())
                slot.appendTooltip(Component.translatable("gui.mobexpansion.emi.drop.count", drop.minCount(), drop.maxCount()).withStyle(ChatFormatting.GRAY));
            if (drop.conditional())
                slot.appendTooltip(Component.translatable("gui.mobexpansion.emi.drop.conditional").withStyle(ChatFormatting.YELLOW));
            slot.appendTooltip(Component.translatable(drop.lootTable() ? "gui.mobexpansion.emi.drop.loot_table" : "gui.mobexpansion.emi.drop.config")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        // После слотов — чтобы текст лёг поверх предметов.
        widgets.addDrawable(0, 0, WIDTH, HEIGHT, (graphics, mouseX, mouseY, delta) -> renderDropCounts(graphics, font));
        if (drops.size() > SLOT_COUNT) {
            final List<Component> more = new ArrayList<>();
            more.add(Component.translatable("gui.mobexpansion.emi.drops.more", drops.size() - SLOT_COUNT));
            for (int i = SLOT_COUNT; i < drops.size(); i++)
                more.add(BuiltInRegistries.ITEM.get(drops.get(i).itemId()).getDescription().copy().withStyle(ChatFormatting.GRAY));
            widgets.addTooltipText(more, labelX, DROPS_LABEL_Y - 1, font.width(dropsLabel), 10);
        }
    }

    /** Количество в правом нижнем углу слота, как у стака: «4» или «2-4» (не влезает в слот — мельче). */
    private void renderDropCounts(GuiGraphics graphics, Font font) {
        final List<MobInfo.Drop> drops = info.drops();
        for (int i = 0; i < Math.min(SLOT_COUNT, drops.size()); i++) {
            final MobInfo.Drop drop = drops.get(i);
            if (drop.maxCount() <= 1)
                continue;
            final String text = drop.minCount() == drop.maxCount() ? String.valueOf(drop.minCount()) : drop.minCount() + "-" + drop.maxCount();
            final int itemX = SLOTS_X + (i % SLOT_COLUMNS) * SLOT_SIZE + 1;
            final int itemY = SLOTS_Y + (i / SLOT_COLUMNS) * SLOT_SIZE + 1;
            final int textWidth = font.width(text);
            final float scale = textWidth > 17 ? 0.75F : 1.0F;
            graphics.pose().pushPose();
            graphics.pose().translate(itemX + 17.0F, itemY + 17.0F, 300.0F);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.drawString(font, text, -textWidth, -8, 0xFFFFFFFF, true);
            graphics.pose().popPose();
        }
    }

    // ---------------------------------------------------------------- левая часть

    private void renderModel(GuiGraphics graphics) {
        final LivingEntity entity = previewEntity();
        if (entity == null)
            return;
        final float width = Math.max(0.3F, entity.getBbWidth());
        final float height = Math.max(0.3F, entity.getBbHeight());
        final float areaW = MODEL_RIGHT - MODEL_LEFT;
        final float areaH = MODEL_BOTTOM - MODEL_TOP;
        // GeckoLib-модели часто заметно больше хитбокса — берём с запасом.
        final float scale = Math.min(32.0F, Math.min(areaH * MODEL_FILL / height, areaW * MODEL_FILL / width));
        final float spin = (System.currentTimeMillis() % 12000L) / 12000.0F * 360.0F;
        final Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI).rotateY((float) Math.toRadians(spin));

        // Модели больше своего хитбокса обрезаем по панели. Ножницы работают в экранных координатах.
        final Matrix4f matrix = graphics.pose().last().pose();
        final int offsetX = Math.round(matrix.m30());
        final int offsetY = Math.round(matrix.m31());
        graphics.enableScissor(offsetX + MODEL_LEFT, offsetY + MODEL_TOP, offsetX + MODEL_RIGHT, offsetY + MODEL_BOTTOM);
        try {
            InventoryScreen.renderEntityInInventory(graphics,
                    (MODEL_LEFT + MODEL_RIGHT) / 2.0F, (MODEL_TOP + MODEL_BOTTOM) / 2.0F, scale,
                    new Vector3f(0.0F, height / 2.0F, 0.0F), pose, null, entity);
        } catch (RuntimeException ignored) {
            previewFailed = true;
        } finally {
            graphics.disableScissor();
        }
    }

    @Nullable
    private LivingEntity previewEntity() {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return null;
        if (previewLevel != level) {
            previewLevel = level;
            previewEntity = null;
            previewFailed = false;
        }
        if (previewEntity != null || previewFailed)
            return previewEntity;
        try {
            final Entity created = entityType.create(level);
            if (created instanceof LivingEntity living) {
                living.setYBodyRot(180.0F);
                living.setYHeadRot(180.0F);
                living.setYRot(180.0F);
                living.yBodyRotO = 180.0F;
                living.yHeadRotO = 180.0F;
                living.yRotO = 180.0F;
                previewEntity = living;
            } else {
                previewFailed = true;
            }
        } catch (RuntimeException e) {
            previewFailed = true;
        }
        return previewEntity;
    }

    private void renderLeftPanel(GuiGraphics graphics, Font font) {
        final String name = font.plainSubstrByWidth(entityType.getDescription().getString().toUpperCase(Locale.ROOT), MODEL_RIGHT - MODEL_LEFT - 4);
        graphics.drawString(font, name, (MODEL_LEFT + MODEL_RIGHT - font.width(name)) / 2, NAME_Y, COLOR_VALUE, false);

        graphics.drawString(font, Component.translatable("gui.mobexpansion.emi.hp"), STATS_X, HP_LABEL_Y, COLOR_VALUE, false);
        final String hp = formatCompact(info.maxHealth());
        graphics.drawString(font, hp, HP_BAR_X + (HP_BAR_W - font.width(hp)) / 2, HP_BAR_Y - 1, COLOR_VALUE, true);

        graphics.drawString(font, Component.translatable("gui.mobexpansion.emi.clearance"), STATS_X, CLEARANCE_LABEL_Y, COLOR_VALUE, false);
        for (int level = 0; level < Math.min(5, info.clearance()); level++)
            graphics.blit(TEXTURE, SEGMENT_X + level * SEGMENT_STEP, SEGMENT_Y, level * SEGMENT_STEP, SEGMENT_FILLED_V, SEGMENT_W, SEGMENT_H);
    }

    private List<Component> clearanceTooltip() {
        final List<Component> lines = new ArrayList<>();
        final int required = info.clearance();
        if (required <= 0) {
            lines.add(Component.translatable("gui.mobexpansion.emi.clearance.none"));
            return lines;
        }
        lines.add(Component.translatable("gui.mobexpansion.emi.clearance.required", roman(required)));
        final int own = ClientClearance.getLevel();
        lines.add(Component.translatable("gui.mobexpansion.emi.clearance.own", roman(own))
                .withStyle(own >= required ? ChatFormatting.GREEN : ChatFormatting.RED));
        return lines;
    }

    // ---------------------------------------------------------------- правая часть

    private void renderInfoPanel(GuiGraphics graphics, Font font) {
        final ClientMobInfo.SpawnPoints spawns = ClientMobInfo.spawnPoints(info.entityId());
        int y = INFO_Y;

        graphics.drawString(font, Component.translatable("gui.mobexpansion.emi.location"), INFO_X, y, COLOR_LABEL, false);
        y += LINE_H;
        drawClipped(graphics, font, locationValue(spawns), INFO_X + 4, y, COLOR_VALUE);
        y += LINE_H;

        drawLine(graphics, font, Component.translatable("gui.mobexpansion.emi.damage"), damageValue(), COLOR_VALUE, y);
        y += LINE_H;

        final int respawnColor = isAlive(spawns) ? COLOR_ALIVE : COLOR_VALUE;
        drawLine(graphics, font, Component.translatable("gui.mobexpansion.emi.respawn"), respawnValue(spawns), respawnColor, y);
        y += LINE_H;

        drawLine(graphics, font, Component.translatable("gui.mobexpansion.emi.armor.short"), Component.literal(formatPlain(info.armor())), COLOR_VALUE, y);
        y += LINE_H;

        if (info.hasExperience()) {
            final String amount = info.xpMin() == info.xpMax() ? String.valueOf(info.xpMin()) : info.xpMin() + "–" + info.xpMax();
            final String chance = info.xpChance() < 1.0F ? " (" + Math.round(info.xpChance() * 100.0F) + "%)" : "";
            drawLine(graphics, font, Component.translatable("gui.mobexpansion.emi.xp"), Component.literal(amount + chance), COLOR_VALUE, y);
        }
    }

    private void drawLine(GuiGraphics graphics, Font font, Component label, Component value, int valueColor, int y) {
        final Component labelText = label.copy().append(" ");
        graphics.drawString(font, labelText, INFO_X, y, COLOR_LABEL, false);
        final int labelWidth = font.width(labelText);
        drawClipped(graphics, font, value, INFO_X + labelWidth, y, valueColor);
    }

    private void drawClipped(GuiGraphics graphics, Font font, Component text, int x, int y, int color) {
        final int maxWidth = INFO_X + INFO_W - x;
        String value = text.getString();
        if (font.width(value) > maxWidth)
            value = font.plainSubstrByWidth(value, maxWidth - font.width("…")) + "…";
        graphics.drawString(font, value, x, y, color, false);
    }

    private Component locationValue(@Nullable ClientMobInfo.SpawnPoints spawns) {
        if (spawns == null)
            return Component.literal("…");
        if (spawns.points().isEmpty())
            return Component.translatable("gui.mobexpansion.emi.location.unknown");
        final MobSpawnPoint first = spawns.points().getFirst();
        final MutableComponent text = describePoint(first);
        if (spawns.points().size() > 1)
            text.append(" (+" + (spawns.points().size() - 1) + ")");
        return text;
    }

    private static MutableComponent describePoint(MobSpawnPoint point) {
        final MutableComponent text = Component.literal(point.pos().getX() + ", " + point.pos().getY() + ", " + point.pos().getZ());
        if (!point.dimension().equals(Level.OVERWORLD.location()))
            text.append(" · ").append(dimensionName(point.dimension()));
        return text;
    }

    private static Component dimensionName(ResourceLocation dimension) {
        return Component.translatableWithFallback("gui.mobexpansion.emi.dimension." + dimension.getNamespace() + "." + dimension.getPath(), dimension.getPath());
    }

    private List<Component> locationTooltip() {
        final ClientMobInfo.SpawnPoints spawns = ClientMobInfo.spawnPoints(info.entityId());
        final List<Component> lines = new ArrayList<>();
        if (spawns == null || spawns.points().isEmpty()) {
            lines.add(Component.translatable("gui.mobexpansion.emi.location.unknown.tooltip"));
            return lines;
        }
        lines.add(Component.translatable("gui.mobexpansion.emi.location.spawners", spawns.points().size()));
        for (MobSpawnPoint point : spawns.points()) {
            final MutableComponent line = describePoint(point).withStyle(ChatFormatting.GRAY);
            if (point.isFull())
                line.append(Component.literal(" — ").append(Component.translatable("gui.mobexpansion.emi.respawn.alive")).withStyle(ChatFormatting.GREEN));
            else
                line.append(Component.literal(" — " + formatDuration(spawns.remainingMillis(point))).withStyle(ChatFormatting.WHITE));
            lines.add(line);
        }
        return lines;
    }

    private Component damageValue() {
        float max = info.attackDamage();
        for (float damage : info.abilityDamage().values())
            max = Math.max(max, damage);
        if (max > info.attackDamage())
            return Component.literal(formatCompact(info.attackDamage()) + "–" + formatCompact(max));
        return Component.literal(formatCompact(info.attackDamage()));
    }

    private List<Component> damageTooltip() {
        final List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.mobexpansion.emi.damage.melee", formatPlain(info.attackDamage())));
        for (Map.Entry<String, Float> ability : info.abilityDamage().entrySet()) {
            lines.add(Component.translatable("gui.mobexpansion.mob_drop_config.extra." + ability.getKey())
                    .append(": " + formatPlain(ability.getValue()))
                    .withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    private static boolean isAlive(@Nullable ClientMobInfo.SpawnPoints spawns) {
        if (spawns == null || spawns.points().isEmpty())
            return false;
        for (MobSpawnPoint point : spawns.points()) {
            if (!point.isFull())
                return false;
        }
        return true;
    }

    private static Component respawnValue(@Nullable ClientMobInfo.SpawnPoints spawns) {
        if (spawns == null)
            return Component.literal("…");
        if (spawns.points().isEmpty())
            return Component.literal("—");
        long nearest = Long.MAX_VALUE;
        for (MobSpawnPoint point : spawns.points()) {
            if (!point.isFull())
                nearest = Math.min(nearest, spawns.remainingMillis(point));
        }
        if (nearest == Long.MAX_VALUE)
            return Component.translatable("gui.mobexpansion.emi.respawn.alive");
        if (nearest <= 0L)
            return Component.translatable("gui.mobexpansion.emi.respawn.soon");
        return Component.literal(formatDuration(nearest));
    }

    // ---------------------------------------------------------------- форматирование

    private static List<ClientTooltipComponent> toTooltip(List<Component> lines) {
        final List<ClientTooltipComponent> tooltip = new ArrayList<>(lines.size());
        for (Component line : lines)
            tooltip.add(ClientTooltipComponent.create(line.getVisualOrderText()));
        return tooltip;
    }

    /** 950 → «950», 12 500 → «12.5 К», 185 000 000 → «185 КК». */
    static String formatCompact(double value) {
        if (value >= 1_000_000.0D)
            return trim(value / 1_000_000.0D) + " " + Component.translatable("gui.mobexpansion.emi.suffix.million").getString();
        if (value >= 10_000.0D)
            return trim(value / 1_000.0D) + " " + Component.translatable("gui.mobexpansion.emi.suffix.thousand").getString();
        return trim(value);
    }

    static String formatPlain(double value) {
        if (Math.abs(value - Math.rint(value)) < 1.0E-4D)
            return String.format(Locale.ROOT, "%,d", Math.round(value)).replace(',', ' ');
        return trim(value);
    }

    private static String trim(double value) {
        final double rounded = Math.round(value * 10.0D) / 10.0D;
        if (Math.abs(rounded - Math.rint(rounded)) < 1.0E-4D)
            return String.valueOf(Math.round(rounded));
        return String.format(Locale.ROOT, "%.1f", rounded);
    }

    private static String formatSpeed(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String formatDuration(long millis) {
        final long totalSeconds = (millis + 999L) / 1000L;
        final long hours = totalSeconds / 3600L;
        final long minutes = totalSeconds / 60L % 60L;
        final long seconds = totalSeconds % 60L;
        if (hours > 0L)
            return Component.translatable("gui.mobexpansion.emi.duration.hours", hours, String.format(Locale.ROOT, "%02d", minutes)).getString();
        return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
    }

    private static String roman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> "0";
        };
    }
}

package net.mcskill.mobexpansion.datagen;

import com.google.gson.JsonObject;
import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.init.MobExBlocks;
import net.mcskill.mobexpansion.init.MobExEntities;
import net.mcskill.mobexpansion.init.MobExItems;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;


public class LanguageProvider implements DataProvider {
    private final String[] locales;
    private final Map<String, String[]> data = new TreeMap<>();
    private final PackOutput output;
    private final String modid;

    public LanguageProvider(PackOutput output, String modid, String... locales) {
        this.output = output;
        this.modid = modid;
        this.locales = locales;
    }

    private void addLocales(){
        add(Core.MODID, "MobExpansion", "MobExpansion");
        add("itemGroup." + Core.MODID + ".main", "MobExpansion", "MobExpansion");
        add(MobExEntities.REGULAR_SPIDER.get(), "Regular Spider", "Обычный паук");
        add(MobExEntities.POISON_SPIDER.get(), "Poison Spider", "Ядовитый паук");
        add(MobExEntities.SPIDER_SPAWN.get(), "Spider spawner", "Паучье гнездо");
        add(MobExEntities.REDSTONE_ENGINEER.get(), "Redstone Engineer", "Редстоун-инженер");
        add(MobExEntities.REDSTONE_AUTOMATON.get(), "Redstone Automaton", "Автоматон");
        add(MobExEntities.REDSTONE_DRONE.get(), "Redstone Drone", "Дрон");
        add(MobExEntities.REDSTONE_TOWER.get(), "Tesla Tower", "Тесла-вышка");
        add(MobExEntities.REDSTONE_PROJECTILE.get(), "Redstone Key", "Редстоун-ключ");
        add(MobExEntities.WRENCH_PROJECTILE.get(), "Wrench", "Гаечный ключ");
        add(MobExEntities.SENTINEL_STATUE.get(), "Sentinel Statue", "Статуя Стража");
        add(MobExEntities.SENTINEL_GOLEM.get(), "Sentinel Golem", "Голем-Страж");
        add(MobExEntities.RAT.get(), "Rat", "Крыса");
        add(MobExEntities.MOSQUITO.get(), "Mosquito", "Комар");
        add(MobExEntities.LEECH.get(), "Leech", "Пиявка");
        add(MobExEntities.RAT_KING.get(), "Kingpin - The Rat King", "Король крыс");
        add(MobExEntities.SEWER_POTION_PROJECTILE.get(), "Sewer Potion", "Канальная отрава");
        add(MobExEntities.SEWER_BOLT_PROJECTILE.get(), "Sewer Bolt", "Болт Короля крыс");
        add(MobExEntities.SEWER_POISON_AREA.get(), "Poison Area", "Ядовитая зона");
        add(MobExItems.REGULAR_SPIDER_SPAWN_EGG.get(), "Regular spider spawn egg", "Яйцо призыва обычного паука");
        add(MobExItems.POISON_SPIDER_SPAWN_EGG.get(), "Poison spider spawn egg", "Яйцо призыва ядовитого паука");
        add(MobExItems.SPIDER_SPAWN_SPAWN_EGG.get(), "Spider spawn egg", "Яйцо призыва паучьего гнезда");
        add(MobExItems.REDSTONE_ENGINEER_SPAWN_EGG.get(), "Redstone Engineer spawn egg", "Яйцо призыва Редстоун-инженера");
        add(MobExItems.REDSTONE_AUTOMATON_SPAWN_EGG.get(), "Redstone Automaton spawn egg", "Яйцо призыва Автоматона");
        add(MobExItems.REDSTONE_DRONE_SPAWN_EGG.get(), "Redstone Drone spawn egg", "Яйцо призыва Дрона");
        add(MobExItems.REDSTONE_TOWER_SPAWN_EGG.get(), "Tesla Tower spawn egg", "Яйцо призыва Тесла-вышки");
        add(MobExItems.SENTINEL_STATUE_SPAWN_EGG.get(), "Sentinel Statue spawn egg", "Яйцо призыва Статуи Стража");
        add(MobExItems.SENTINEL_GOLEM_SPAWN_EGG.get(), "Sentinel Golem spawn egg", "Яйцо призыва Голема-Стража");
        add(MobExItems.RAT_SPAWN_EGG.get(), "Rat spawn egg", "Яйцо призыва крысы");
        add(MobExItems.MOSQUITO_SPAWN_EGG.get(), "Mosquito spawn egg", "Яйцо призыва комара");
        add(MobExItems.LEECH_SPAWN_EGG.get(), "Leech spawn egg", "Яйцо призыва пиявки");
        add(MobExItems.RAT_KING_SPAWN_EGG.get(), "Rat King spawn egg", "Яйцо призыва Короля крыс");
        add(MobExBlocks.MOB_SPAWNER.get(), "Mob Spawner", "Спавнер мобов");
        add(MobExItems.DROP_CONFIGURATOR.get(), "Mob Configurator", "Настройка мобов");
        add("gui.mobexpansion.mob_spawner.entity_set", "Entity set", "Набор сущностей");
        add("gui.mobexpansion.mob_spawner.entity_set_empty", "Empty", "Пусто");
        add("gui.mobexpansion.mob_spawner.mob_picker", "Mob picker", "Выбор моба");
        add("gui.mobexpansion.mob_spawner.picker_empty", "No matches", "Нет совпадений");
        add("gui.mobexpansion.mob_spawner.search", "Search…", "Поиск…");
        add("gui.mobexpansion.mob_spawner.remove", "Remove", "Удалить");
        add("gui.mobexpansion.mob_spawner.interval", "Interval", "Интервал");
        add("gui.mobexpansion.mob_spawner.count_min", "Count min", "Кол-во мин.");
        add("gui.mobexpansion.mob_spawner.count_max", "Count max", "Кол-во макс.");
        add("gui.mobexpansion.mob_spawner.max_simultaneous", "Max near spawner", "Макс. рядом");
        add("gui.mobexpansion.mob_spawner.activation_radius", "Activation radius", "Радиус активации");
        add("gui.mobexpansion.mob_spawner.spawn_radius", "Spawn radius", "Радиус спавна");
        add("gui.mobexpansion.mob_spawner.spawn_y_min", "Spawn Y min", "Y спавна мин.");
        add("gui.mobexpansion.mob_spawner.spawn_y_max", "Spawn Y max", "Y спавна макс.");
        add("gui.mobexpansion.mob_spawner.enabled", "Enabled", "Включён");
        add("gui.mobexpansion.mob_spawner.enabled_on", "Enabled: ON", "Включён: ДА");
        add("gui.mobexpansion.mob_spawner.enabled_off", "Enabled: OFF", "Включён: НЕТ");
        add("gui.mobexpansion.mob_spawner.tab.main", "Main", "Основные");
        add("gui.mobexpansion.mob_spawner.tab.conditions", "Conditions", "Условия");
        add("gui.mobexpansion.mob_spawner.tab.extra", "Extra", "Ещё");
        add("gui.mobexpansion.mob_spawner.tab.visual", "Visual", "Визуал");
        add("gui.mobexpansion.mob_spawner.coming_soon", "Unavailable", "Недоступно");
        add("gui.mobexpansion.mob_spawner.reset", "Reset", "Сброс");
        add("gui.mobexpansion.mob_spawner.done", "Done", "Готово");
        add("gui.mobexpansion.mob_spawner.save", "Save", "Сохранить");
        add("gui.mobexpansion.mob_spawner.properties", "Spawner properties", "Свойства спавнера");
        add("gui.mobexpansion.mob_spawner.spawn_mode", "Spawn mode", "Режим спавна");
        add("gui.mobexpansion.mob_spawner.spawn_mode.standard", "Standard", "Стандартный");
        add("gui.mobexpansion.mob_spawner.spawn_mode.random", "Random", "Случайный");
        add("gui.mobexpansion.mob_spawner.activation_delay", "Activation delay", "Задержка активации");
        add("gui.mobexpansion.mob_spawner.check_los", "Line of sight", "Прямая видимость");
        add("gui.mobexpansion.mob_spawner.check_daylight", "Daylight", "Дневной свет");
        add("gui.mobexpansion.mob_spawner.check_spectator", "Ignore spectators", "Игнор. spectator");
        add("gui.mobexpansion.mob_spawner.check_height", "Height limit", "Лимит высоты");
        add("gui.mobexpansion.mob_spawner.activation_distance", "Activation distance", "Дистанция активации");
        add("gui.mobexpansion.mob_spawner.block_requirements", "Block requirements", "Требования к блокам");
        add("gui.mobexpansion.mob_spawner.block_under.none", "Under: none", "Под: нет");
        add("gui.mobexpansion.mob_spawner.block_around.none", "Around: none", "Вокруг: нет");
        add("gui.mobexpansion.mob_spawner.mask", "Camouflage", "Маскировка");
        add("gui.mobexpansion.mob_spawner.mask.none", "Mask: none", "Маска: нет");
        add("gui.mobexpansion.mob_spawner.mask.hint", "The spawner looks like this block in the world.", "Спавнер выглядит как этот блок в мире.");
        add("gui.mobexpansion.mob_spawner.mask.preview", "Preview", "Превью");
        add("gui.mobexpansion.mob_spawner.mask.no_properties", "No blockstate properties", "Нет свойств blockstate");
        add("gui.mobexpansion.mob_spawner.mask.cycle_hint", "Click to cycle, Shift-click to go back", "Клик — следующее значение, Shift+клик — предыдущее");
        add("gui.mobexpansion.mob_spawner.clear_block", "Clear block", "Сбросить блок");
        add("gui.mobexpansion.mob_spawner.clear_mask", "Clear mask", "Сбросить маску");
        add("gui.mobexpansion.mob_spawner.select_entity", "Select an entity", "Выберите сущность");
        add("gui.mobexpansion.mob_spawner.search.tooltip", "Filter the picker by mob name or id (minecraft:zombie).", "Фильтр списка мобов по имени или id (minecraft:zombie).");
        add("gui.mobexpansion.mob_spawner.remove.tooltip", "Remove the selected mob from this spawner's set.", "Убрать выбранного моба из набора этого спавнера.");
        add("gui.mobexpansion.mob_spawner.move_up.tooltip", "Move the selected mob earlier in Standard spawn order.", "Поднять выбранного моба выше в порядке Standard.");
        add("gui.mobexpansion.mob_spawner.move_down.tooltip", "Move the selected mob later in Standard spawn order.", "Опустить выбранного моба ниже в порядке Standard.");
        add("gui.mobexpansion.mob_spawner.interval.tooltip", "Ticks between spawn waves for this mob. 20 ticks = 1 second. Max 432000 (6 hours). Default 200 (10 seconds).", "Пауза между волнами этого моба в тиках. 20 тиков = 1 секунда. Максимум 432000 (6 часов). Обычно 200 (10 секунд).");
        add("gui.mobexpansion.mob_spawner.count_min.tooltip", "Minimum mobs in one wave. The real count is random between min and max.", "Минимум мобов за одну волну. Реальное число случайно между мин. и макс.");
        add("gui.mobexpansion.mob_spawner.count_max.tooltip", "Maximum mobs in one wave. The real count is random between min and max.", "Максимум мобов за одну волну. Реальное число случайно между мин. и макс.");
        add("gui.mobexpansion.mob_spawner.max_simultaneous.tooltip", "If this many living copies of this mob are already near the spawner, no more will spawn.", "Если рядом со спавнером уже столько живых копий этого моба — новые не появятся.");
        add("gui.mobexpansion.mob_spawner.activation_radius.tooltip", "Extra per-mob range: the player must also be this close for THIS mob to be eligible. Separate from global activation distance.", "Доп. радиус именно этого моба: игрок должен быть ещё и так близко. Это не то же самое, что глобальная дистанция активации.");
        add("gui.mobexpansion.mob_spawner.spawn_radius.tooltip", "How far horizontally from the spawner this mob can appear, in blocks.", "Насколько далеко по горизонтали от спавнера может появиться этот моб (в блоках).");
        add("gui.mobexpansion.mob_spawner.spawn_y_min.tooltip", "Lowest world Y where this mob can appear. Random between min and max. Default is one block above the spawner.", "Нижняя мировая Y, где появляется этот моб. Случайно между мин. и макс. По умолчанию — блок над спавнером.");
        add("gui.mobexpansion.mob_spawner.spawn_y_max.tooltip", "Highest world Y where this mob can appear. Random between min and max.", "Верхняя мировая Y, где появляется этот моб. Случайно между мин. и макс.");
        add("gui.mobexpansion.mob_spawner.enabled.tooltip", "If off, this mob stays in the set but never spawns.", "Если выкл., моб остаётся в наборе, но никогда не спавнится.");
        add("gui.mobexpansion.mob_spawner.tab.main.tooltip", "Per-mob numbers for the selected entry: interval, count, radii, Y.", "Числа выбранного моба: интервал, количество, радиусы, Y.");
        add("gui.mobexpansion.mob_spawner.tab.conditions.tooltip", "Reserved for later. Global wake rules (LOS, daylight, distance, blocks) are always on the right panel.", "Пока пусто. Глобальные правила пробуждения (видимость, день, дистанция, блоки) всегда на правой панели.");
        add("gui.mobexpansion.mob_spawner.tab.visual.tooltip", "Camouflage: which block the spawner looks like in the world, plus its blockstate.", "Маскировка: каким блоком спавнер выглядит в мире и его blockstate.");
        add("gui.mobexpansion.mob_spawner.spawn_mode.standard.tooltip", "Cycles through the set in list order. Use the up/down arrows to change priority.", "Идёт по набору сверху вниз. Стрелки вверх/вниз меняют приоритет.");
        add("gui.mobexpansion.mob_spawner.spawn_mode.random.tooltip", "Each wave picks a random mob that currently meets its conditions.", "Каждая волна берёт случайного моба, который сейчас подходит по условиям.");
        add("gui.mobexpansion.mob_spawner.activation_delay.tooltip", "After a player first enters range, wait this many ticks before the first wave. 0 = spawn immediately. Resets if everyone leaves.", "После первого входа игрока в зону — ждать столько тиков до первой волны. 0 = сразу. Сбрасывается, если все ушли.");
        add("gui.mobexpansion.mob_spawner.check_los.tooltip", "The spawner only wakes if it can see the player's eyes (no solid blocks in between).", "Спавнер просыпается, только если видит глаза игрока (без сплошных блоков на пути).");
        add("gui.mobexpansion.mob_spawner.check_daylight.tooltip", "Only wakes during daytime (day cycle 0–12000).", "Просыпается только днём (цикл дня 0–12000).");
        add("gui.mobexpansion.mob_spawner.check_spectator.tooltip", "Spectator players do not wake the spawner.", "Игроки в режиме Spectator не будят спавнер.");
        add("gui.mobexpansion.mob_spawner.check_height.tooltip", "Player must be within 16 Y of the spawner, and spawned mobs must also appear within 16 Y of the player.", "Игрок должен быть в пределах 16 по Y от спавнера, и мобы тоже появляются не дальше 16 Y от игрока.");
        add("gui.mobexpansion.mob_spawner.activation_dist_min.tooltip", "Players closer than this (blocks) will not wake the spawner. 0 = no inner dead zone.", "Игроки ближе этого (в блоках) не будят спавнер. 0 = нет мёртвой зоны у блока.");
        add("gui.mobexpansion.mob_spawner.activation_dist_max.tooltip", "Players farther than this (blocks) will not wake the spawner. Maximum 64.", "Игроки дальше этого (в блоках) не будят спавнер. Максимум 64.");
        add("gui.mobexpansion.mob_spawner.block_under.tooltip", "If set, the whole spawner stays idle unless this block is directly below it. Empty = any block.", "Если задано, весь спавнер молчит, пока под ним не этот блок. Пусто = любой блок.");
        add("gui.mobexpansion.mob_spawner.block_around.tooltip", "A spawn attempt succeeds only if this block is on one of the four sides of the spawn position. Empty = no check.", "Попытка спавна проходит, только если этот блок с одной из четырёх сторон точки появления. Пусто = без проверки.");
        add("gui.mobexpansion.mob_spawner.mask.tooltip", "The spawner looks like this block to everyone. Only an OP holding a spawner item can see the real one.", "Для всех спавнер выглядит как этот блок. Настоящий видит только OP со спавнером в руке.");
        add("gui.mobexpansion.mob_spawner.clear_block.tooltip", "Clear this block requirement.", "Сбросить требование к блоку.");
        add("gui.mobexpansion.mob_spawner.clear_mask.tooltip", "Remove camouflage. The spawner will look like a normal spawner again.", "Убрать маскировку. Спавнер снова будет выглядеть как обычный спавнер.");
        add("gui.mobexpansion.mob_spawner.reset.tooltip", "Reset the entity set and all settings to defaults. Nothing is saved until you press Done.", "Сбросить набор мобов и все настройки. Сохранится только после «Готово».");
        add("gui.mobexpansion.mob_spawner.done.tooltip", "Save this spawner to the world and close.", "Сохранить этот спавнер в мир и закрыть.");
        add("gui.mobexpansion.mob_spawner.cancel.tooltip", "Close without saving changes.", "Закрыть без сохранения изменений.");
        add("gui.mobexpansion.mob_spawner.entity_set.row.tooltip", "Click to select and edit this mob. Drag to change Standard spawn order.", "Клик — выбрать и править. Перетащи, чтобы сменить порядок в режиме Standard.");
        add("gui.mobexpansion.mob_spawner.picker.row.tooltip", "Click to add this mob to the spawner set.", "Клик — добавить моба в набор спавнера.");
        add("gui.mobexpansion.mob_spawner.picker.row.in_set.tooltip", "Already in the set. Dimmed rows are already added.", "Уже в наборе. Затемнённые строки уже добавлены.");
        add("gui.mobexpansion.item_picker.title_blocks", "Select block", "Выбор блока");
        add("gui.mobexpansion.mob_drop_config.title", "Mob settings: %s", "Настройка моба: %s");
        add("gui.mobexpansion.mob_drop_config.table", "Drop table", "Таблица дропа");
        add("gui.mobexpansion.mob_drop_config.params", "Selected drop", "Параметры выбранного дропа");
        add("gui.mobexpansion.mob_drop_config.col.item", "Item", "Предмет");
        add("gui.mobexpansion.mob_drop_config.col.chance", "Chance", "Шанс");
        add("gui.mobexpansion.mob_drop_config.col.min", "Min", "Мин.");
        add("gui.mobexpansion.mob_drop_config.col.max", "Max", "Макс.");
        add("gui.mobexpansion.mob_drop_config.min", "Min", "Мин.");
        add("gui.mobexpansion.mob_drop_config.max", "Max", "Макс.");
        add("gui.mobexpansion.mob_drop_config.chance_label", "Drop chance", "Шанс выпадения");
        add("gui.mobexpansion.mob_drop_config.amount", "Amount", "Количество");
        add("gui.mobexpansion.mob_drop_config.conditions", "Extra conditions", "Доп. условия");
        add("gui.mobexpansion.mob_drop_config.conditions_table", "Conditions", "Условия");
        add("gui.mobexpansion.mob_drop_config.conditions_hint", "Select a loot/XP entry first, then add conditions here", "Сначала выберите дроп/XP, затем добавляйте условия здесь");
        add("gui.mobexpansion.mob_drop_config.condition_target", "Apply to", "Применить к");
        add("gui.mobexpansion.mob_drop_config.condition_type", "Condition type", "Тип условия");
        add("gui.mobexpansion.mob_drop_config.col.condition", "Condition", "Условие");
        add("gui.mobexpansion.condition.no_target", "No loot/XP entry selected", "Не выбран дроп/XP");
        add("gui.mobexpansion.mob_drop_config.add", "+ Add", "+ Добавить");
        add("gui.mobexpansion.mob_drop_config.remove", "Remove", "Удалить");
        add("gui.mobexpansion.mob_drop_config.add_condition", "+ Add condition", "+ Добавить условие");
        add("gui.mobexpansion.mob_drop_config.add_condition_short", "+ Cond", "+ Усл.");
        add("gui.mobexpansion.mob_drop_config.remove_condition", "Remove condition", "Удалить условие");
        add("gui.mobexpansion.mob_drop_config.reset", "Reset to default", "Сбросить к стандарту");
        add("gui.mobexpansion.mob_drop_config.done", "Done", "Сохранить");
        add("gui.mobexpansion.mob_drop_config.drag_hint", "Drag rows to reorder drops", "Перетаскивайте строки для изменения порядка дропа");
        add("gui.mobexpansion.mob_drop_config.drag_hint_xp", "Drag rows to reorder experience", "Перетаскивайте строки для изменения порядка опыта");
        add("gui.mobexpansion.mob_drop_config.no_drops", "No drops", "Нет дропа");
        add("gui.mobexpansion.mob_drop_config.replace_vanilla", "Replace vanilla loot", "Заменить ванильный лут");
        add("gui.mobexpansion.mob_drop_config.replace_vanilla.tooltip", "ON: clear the vanilla/mod drop table and use only the entries below. OFF: add these drops on top of existing loot.", "Вкл: очистить ванильную таблицу и оставить только записи ниже. Выкл: добавить эти дропы к существующему луту.");
        add("gui.mobexpansion.mob_drop_config.search.tooltip", "Filter every loaded mob by name or id, including vanilla (minecraft:zombie).", "Фильтр всех загруженных мобов по имени или id, включая ванильных (minecraft:zombie).");
        add("gui.mobexpansion.mob_drop_config.no_experience", "No experience", "Нет опыта");
        add("gui.mobexpansion.mob_drop_config.xp_table", "Experience table", "Таблица опыта");
        add("gui.mobexpansion.mob_drop_config.xp_params", "Selected experience", "Параметры опыта");
        add("gui.mobexpansion.mob_drop_config.xp_amount", "XP amount", "Количество XP");
        add("gui.mobexpansion.mob_drop_config.col.xp", "XP", "XP");
        add("gui.mobexpansion.mob_drop_config.col.conditions", "Cond.", "Усл.");
        add("gui.mobexpansion.mob_drop_config.tab.loot", "Loot", "Лут");
        add("gui.mobexpansion.mob_drop_config.tab.experience", "Experience", "Опыт");
        add("gui.mobexpansion.mob_drop_config.tab.conditions", "Conditions", "Условия");
        add("gui.mobexpansion.mob_drop_config.tab.characteristics", "Attributes", "Характеристики");
        add("gui.mobexpansion.mob_drop_config.tab_soon", "Unavailable", "Недоступно");
        add("gui.mobexpansion.mob_drop_config.saved", "Saved. Loot: /reload. Attributes: restart server", "Сохранено. Лут: /reload. Характеристики: рестарт сервера");
        add("gui.mobexpansion.mob_drop_config.pick_item", "Pick item", "Выбрать предмет");
        add("gui.mobexpansion.mob_drop_config.drop_destination.killer", "Drop: inventory", "Дроп: инвентарь");
        add("gui.mobexpansion.mob_drop_config.drop_destination.world", "Drop: world", "Дроп: в мир");
        add("gui.mobexpansion.mob_drop_config.drop_destination.killer.tooltip", "Loot goes into the killer's inventory. Overflow drops on the ground.", "Дроп сразу в инвентарь убийцы. Что не влезло — падает на землю.");
        add("gui.mobexpansion.mob_drop_config.drop_destination.world.tooltip", "Loot drops on the ground as usual.", "Дроп выпадает на землю, как обычно.");
        add("gui.mobexpansion.mob_drop_config.attributes_title", "Mob attributes", "Характеристики моба");
        add("gui.mobexpansion.mob_drop_config.attributes_hint", "Changes apply after server restart", "Изменения применятся после рестарта сервера");
        add("gui.mobexpansion.mob_drop_config.attributes_side_hint", "Each mob has its own section in config/mobexpansion/entity_attributes.json", "У каждого моба своя секция в config/mobexpansion/entity_attributes.json");
        add("gui.mobexpansion.mob_drop_config.attr.max_health", "Max health", "Здоровье");
        add("gui.mobexpansion.mob_drop_config.attr.max_health.tooltip", "Maximum health points of the mob.", "Максимальный запас здоровья моба.");
        add("gui.mobexpansion.mob_drop_config.attr.armor", "Armor", "Броня");
        add("gui.mobexpansion.mob_drop_config.attr.armor.tooltip", "Flat armor that reduces incoming damage.", "Плоская броня, снижающая входящий урон.");
        add("gui.mobexpansion.mob_drop_config.attr.armor_toughness", "Armor toughness", "Прочность брони");
        add("gui.mobexpansion.mob_drop_config.attr.armor_toughness.tooltip", "Reduces how quickly strong hits pierce armor.", "Снижает пробитие брони сильными ударами.");
        add("gui.mobexpansion.mob_drop_config.attr.attack_damage", "Attack damage", "Урон");
        add("gui.mobexpansion.mob_drop_config.attr.attack_damage.tooltip", "Base melee attack damage attribute.", "Базовый атрибут урона в ближнем бою.");
        add("gui.mobexpansion.mob_drop_config.attr.movement_speed", "Movement speed", "Скорость");
        add("gui.mobexpansion.mob_drop_config.attr.movement_speed.tooltip", "Ground movement speed attribute.", "Атрибут скорости передвижения по земле.");
        add("gui.mobexpansion.mob_drop_config.attr.knockback_resistance", "Knockback resistance", "Сопр. отбрасыванию");
        add("gui.mobexpansion.mob_drop_config.attr.knockback_resistance.tooltip", "Chance/strength resistance against knockback (0-1).", "Сопротивление отбрасыванию (обычно 0-1).");
        add("gui.mobexpansion.mob_drop_config.attr.follow_range", "Follow range", "Дальность преследования");
        add("gui.mobexpansion.mob_drop_config.attr.follow_range.tooltip", "How far the mob can detect and chase targets.", "Как далеко моб замечает и преследует цели.");
        add("gui.mobexpansion.mob_drop_config.extras_table", "Extra parameters", "Доп. параметры");
        add("gui.mobexpansion.mob_drop_config.col.extra", "Parameter", "Параметр");
        add("gui.mobexpansion.mob_drop_config.col.value", "Value", "Значение");
        add("gui.mobexpansion.mob_drop_config.no_extras", "No extra parameters", "Нет доп. параметров");
        addExtra("attackReach", "Attack reach", "Дальность атаки",
                "Maximum distance for a successful melee hit.",
                "Максимальная дистанция, на которой засчитывается удар ближнего боя.");
        addExtra("drainHeal", "Heal on bite", "Лечение от укуса",
                "Health restored to the mosquito when its bite connects.",
                "Сколько здоровья восстанавливает комар при успешном укусе.");
        addExtra("hoverCombatRadius", "Hover fight radius", "Радиус боя в полёте",
                "How close the flying mob tries to stay while fighting.",
                "На каком расстоянии летающий моб старается держаться в бою.");
        addExtra("hoverWanderRadius", "Hover wander radius", "Радиус блуждания в полёте",
                "How far the flying mob may drift while idle.",
                "Как далеко летающий моб может улетать в режиме блуждания.");
        addExtra("minHoverHeight", "Min hover height", "Мин. высота полёта",
                "Lowest preferred altitude above the ground.",
                "Минимальная предпочитаемая высота над землёй.");
        addExtra("maxHoverHeight", "Max hover height", "Макс. высота полёта",
                "Highest preferred altitude above the ground.",
                "Максимальная предпочитаемая высота над землёй.");
        addExtra("flyingSpeed", "Flight speed", "Скорость полёта",
                "Movement speed while flying/hovering.",
                "Скорость перемещения во время полёта.");
        addExtra("vampHeal", "Lifesteal heal", "Лечение вампиризмом",
                "Health restored to the leech when it damages a target.",
                "Сколько здоровья восстанавливает пиявка при нанесении урона.");
        addExtra("holdDistance", "Preferred fight distance", "Дистанция ведения боя",
                "Distance the mob tries to maintain from its target during combat.",
                "Дистанция, на которой моб старается держаться от цели в бою.");
        addExtra("homeRadius", "Territory radius", "Радиус территории",
                "How far from the nest/home the mob may patrol.",
                "Как далеко от гнезда/дома моб может патрулировать.");
        addExtra("fleeHealthRatio", "Flee health threshold", "Порог бегства",
                "Flee when health falls below this fraction of max HP (0-1).",
                "Моб убегает, когда здоровье падает ниже этой доли от максимума (0-1).");
        addExtra("maxSilkReserve", "Max silk reserve", "Макс. запас паутины",
                "Maximum silk charges the spider can store for webs/abilities.",
                "Максимальный запас паутины для способностей паука.");
        addExtra("combatLeashBuffer", "Combat zone extension", "Запас зоны боя",
                "Extra distance beyond territory where the spider may still fight before returning home.",
                "Дополнительное расстояние за пределами территории, где паук ещё может сражаться, прежде чем вернуться домой.");
        addExtra("meleePoisonChance", "Melee poison chance", "Шанс яда в ближнем бою",
                "Chance to apply poison on a melee hit (0-1).",
                "Шанс наложить яд при ударе в ближнем бою (0-1).");
        addExtra("meleePoisonDuration", "Melee poison duration", "Длит. яда в ближнем бою",
                "Poison duration from melee hits, in ticks (20 ticks = 1 second).",
                "Длительность яда от удара в ближнем бою, в тиках (20 тиков = 1 секунда).");
        addExtra("meleePoisonAmplifier", "Melee poison level", "Сила яда в ближнем бою",
                "Poison amplifier from melee hits (0 = Poison I).",
                "Уровень яда от удара в ближнем бою (0 = Отравление I).");
        addExtra("maxComfortableLight", "Max comfortable light", "Макс. комфортный свет",
                "Highest light level the spider treats as comfortable.",
                "Максимальный уровень света, при котором пауку ещё комфортно.");
        addExtra("spitSpeed", "Spit projectile speed", "Скорость плевка",
                "Flight speed of the poison spit projectile.",
                "Скорость полёта снаряда-плевка.");
        addExtra("spitAttackRadius", "Spit attack range", "Дальность плевка",
                "Maximum range at which the spider may spit.",
                "Максимальная дистанция, с которой паук может плеваться.");
        addExtra("spitMinInterval", "Min spit interval", "Мин. интервал плевка",
                "Minimum delay between spits, in ticks (20 ticks = 1 second).",
                "Минимальная пауза между плевками, в тиках (20 тиков = 1 секунда).");
        addExtra("spitMaxInterval", "Max spit interval", "Макс. интервал плевка",
                "Maximum delay between spits, in ticks (20 ticks = 1 second).",
                "Максимальная пауза между плевками, в тиках (20 тиков = 1 секунда).");
        addExtra("retreatDistance", "Retreat distance", "Дистанция отступления",
                "If the target is closer than this, the mob tries to back away.",
                "Если цель ближе этого значения, моб старается отойти.");
        addExtra("approachDistance", "Approach distance", "Дистанция сближения",
                "If the target is farther than this, the mob moves closer.",
                "Если цель дальше этого значения, моб подходит ближе.");
        addExtra("bitePoisonDuration", "Bite poison duration", "Длит. яда от укуса",
                "Poison duration from a bite, in ticks (20 ticks = 1 second).",
                "Длительность яда от укуса, в тиках (20 тиков = 1 секунда).");
        addExtra("bitePoisonAmplifier", "Bite poison level", "Сила яда от укуса",
                "Poison amplifier from a bite (0 = Poison I).",
                "Уровень яда от укуса (0 = Отравление I).");
        addExtra("biteSlowDuration", "Bite slow duration", "Длит. замедления от укуса",
                "Slowness duration from a bite, in ticks (20 ticks = 1 second).",
                "Длительность замедления от укуса, в тиках (20 тиков = 1 секунда).");
        addExtra("poisonCloudRadius", "Poison cloud radius", "Радиус облака яда",
                "Radius of the poison cloud left by spit/impact.",
                "Радиус ядовитого облака от плевка или попадания.");
        addExtra("poisonCloudDuration", "Poison cloud duration", "Длит. облака яда",
                "How long the poison cloud lasts, in ticks (20 ticks = 1 second).",
                "Как долго существует ядовитое облако, в тиках (20 тиков = 1 секунда).");
        addExtra("poisonCloudAmplifier", "Poison cloud level", "Сила облака яда",
                "Poison amplifier applied by the cloud (0 = Poison I).",
                "Уровень яда от облака (0 = Отравление I).");
        addExtra("minSpiderStock", "Min spider stock", "Мин. запас пауков",
                "Lower bound used when rolling the nest's starting spider stock.",
                "Нижняя граница случайного начального запаса пауков в гнезде.");
        addExtra("maxSpiderStock", "Max spider stock", "Макс. запас пауков",
                "Upper bound used when rolling the nest's starting spider stock.",
                "Верхняя граница случайного начального запаса пауков в гнезде.");
        addExtra("maxStockCap", "Stock capacity", "Потолок запаса",
                "Hard maximum spider stock the nest can store.",
                "Жёсткий максимум запаса пауков, который гнездо может хранить.");
        addExtra("maxLivingSpiders", "Max living spiders", "Макс. живых пауков",
                "Maximum spiders this nest may keep alive at once.",
                "Сколько пауков этого гнезда может быть живыми одновременно.");
        addExtra("maxOutsideSpiders", "Max spiders outside", "Макс. пауков снаружи",
                "Maximum spiders allowed outside the nest at once.",
                "Сколько пауков одновременно может находиться вне гнезда.");
        addExtra("maxLivingOnHit", "Max living after nest hit", "Макс. живых после удара",
                "Living-spider cap considered when the nest is hit and reacts.",
                "Ограничение по живым паукам при реакции гнезда на удар.");
        addExtra("spidersPerHit", "Spiders spawned on hit", "Пауков за удар по гнезду",
                "How many spiders the nest tries to release when damaged.",
                "Сколько пауков гнездо пытается выпустить при получении урона.");
        addExtra("spawnIntervalTicks", "Spawn interval", "Интервал спавна",
                "Delay between nest spawn attempts, in ticks (20 ticks = 1 second).",
                "Пауза между попытками спавна из гнезда, в тиках (20 тиков = 1 секунда).");
        addExtra("activationRange", "Activation range", "Дальность активации",
                "Player distance at which the nest becomes active.",
                "Дистанция до игрока, на которой гнездо активируется.");
        addExtra("queenMaxHealth", "Queen max health", "Здоровье королевы",
                "Maximum health of the nest queen when she appears.",
                "Максимальное здоровье королевы при появлении.");
        addExtra("queenStockThreshold", "Queen stock threshold", "Порог запаса для королевы",
                "Spider stock required before the queen can spawn.",
                "Какой запас пауков нужен, чтобы могла появиться королева.");
        addExtra("queenSpawnDelayTicks", "Queen spawn delay", "Задержка появления королевы",
                "Delay before the queen can spawn, in ticks (20 ticks = 1 second).",
                "Задержка перед появлением королевы, в тиках (20 тиков = 1 секунда).");
        addExtra("nestHealAmount", "Nest heal amount", "Лечение гнезда",
                "Health restored to the nest by heal events.",
                "Сколько здоровья гнездо восстанавливает при лечении.");
        addExtra("itemsForStockRefill", "Items per stock point", "Предметов на 1 запас",
                "Collected items needed to restore one spider stock point.",
                "Сколько подобранных предметов нужно, чтобы восстановить 1 единицу запаса.");
        addExtra("experienceForStockRefill", "XP per stock point", "Опыта на 1 запас",
                "Collected experience needed to restore one spider stock point.",
                "Сколько подобранного опыта нужно, чтобы восстановить 1 единицу запаса.");
        addExtra("damageForNewSpawner", "Damage for new nest", "Урон для нового гнезда",
                "Damage the nest must take before it can create a new nest.",
                "Сколько урона должно получить гнездо, прежде чем сможет создать новое.");
        addExtra("cobwebAlarmSpawnFraction", "Web alarm spawn share", "Доля спавна по тревоге",
                "Fraction of available stock released when cobweb alarm triggers (0-1).",
                "Какую долю запаса выпускать при тревоге от паутины (0-1).");
        addExtra("spiderHurtSpawnChance", "Spawn chance on spider hurt", "Шанс спавна при ранении",
                "Chance the nest spawns help when a linked spider is hurt (0-1).",
                "Шанс, что гнездо выпустит помощь, когда связанного паука ранят (0-1).");
        addExtra("newSpawnerMinDistance", "New nest min distance", "Мин. дистанция нового гнезда",
                "Minimum distance from this nest to a newly created nest.",
                "Минимальное расстояние от этого гнезда до нового создаваемого гнезда.");
        addExtra("patrolHomeRadius", "Patrol radius", "Радиус патруля",
                "Radius around the nest used for spider patrol assignment.",
                "Радиус вокруг гнезда, в котором паукам назначается патруль.");
        addExtra("handHitDamage", "Hand smash damage", "Урон удара рукой",
                "Damage of the Rat King's hand smash attack.",
                "Урон удара рукой Короля крыс.");
        addExtra("swoopDamage", "Swoop damage", "Урон налёта",
                "Damage of the Rat King's swoop attack.",
                "Урон атаки-налёта Короля крыс.");
        addExtra("potionMeleeDamage", "Potion melee damage", "Урон удара зельем",
                "Melee damage while the Rat King is using potion attacks.",
                "Урон ближнего удара Короля крыс в режиме атак зельем.");
        addExtra("handHitRange", "Hand smash range", "Дальность удара рукой",
                "Reach of the hand smash attack.",
                "Дальность удара рукой.");
        addExtra("swoopRange", "Swoop range", "Дальность налёта",
                "Reach of the swoop attack.",
                "Дальность атаки-налёта.");
        addExtra("meleeReach", "Melee reach", "Дальность ближнего боя",
                "Reach used for general melee checks.",
                "Дальность для обычных проверок ближнего боя.");
        addExtra("rangedPreferredDistance", "Preferred ranged distance", "Дистанция дальней атаки",
                "Distance the mob prefers while using ranged attacks.",
                "Дистанция, на которой моб предпочитает вести дальний бой.");
        addExtra("potionIntervalTicks", "Potion cooldown", "Перезарядка зелья",
                "Cooldown between potion throws, in ticks (20 ticks = 1 second).",
                "Пауза между бросками зелья, в тиках (20 тиков = 1 секунда).");
        addExtra("summonIntervalTicks", "Summon cooldown", "Перезарядка призыва",
                "Cooldown between summon casts, in ticks (20 ticks = 1 second).",
                "Пауза между призывами, в тиках (20 тиков = 1 секунда).");
        addExtra("summonRatCount", "Summoned rats", "Число призываемых крыс",
                "How many rats are summoned per cast.",
                "Сколько крыс появляется за один призыв.");
        addExtra("summonRadius", "Summon radius", "Радиус призыва",
                "Radius around the caster where summons may appear.",
                "Радиус вокруг заклинателя, в котором появляются призванные существа.");
        addExtra("nearShootCooldown", "Close-range shot cooldown", "Перезарядка выстрела вблизи",
                "Bolt shot cooldown at close range, in ticks (20 ticks = 1 second).",
                "Пауза между выстрелами болтом вблизи, в тиках (20 тиков = 1 секунда).");
        addExtra("farShootCooldown", "Long-range shot cooldown", "Перезарядка выстрела вдали",
                "Bolt shot cooldown at long range, in ticks (20 ticks = 1 second).",
                "Пауза между выстрелами болтом на дистанции, в тиках (20 тиков = 1 секунда).");
        addExtra("boltDamage", "Bolt impact damage", "Урон болта",
                "Direct hit damage of the sewer bolt.",
                "Урон прямого попадания канализационного болта.");
        addExtra("boltPassDamage", "Bolt pass-through damage", "Урон при пролёте болта",
                "Damage dealt to enemies the bolt flies through before impact.",
                "Урон врагам, через которых болт пролетает по пути до попадания.");
        addExtra("potionImpactDamage", "Potion burst damage", "Урон взрыва зелья",
                "Damage dealt when the thrown potion bursts.",
                "Урон при взрыве/разбитии брошенного зелья.");
        addExtra("potionImpactRadius", "Potion burst radius", "Радиус взрыва зелья",
                "Radius of the potion burst effect.",
                "Радиус эффекта при взрыве зелья.");
        addExtra("poisonAreaRadius", "Poison zone radius", "Радиус зоны яда",
                "Radius of the lingering poison area.",
                "Радиус оставляемой зоны яда.");
        addExtra("poisonAreaLifetime", "Poison zone lifetime", "Время жизни зоны яда",
                "How long the poison area remains, in ticks (20 ticks = 1 second).",
                "Как долго существует зона яда, в тиках (20 тиков = 1 секунда).");
        addExtra("poisonDuration", "Poison duration", "Длительность яда",
                "Poison effect duration, in ticks (20 ticks = 1 second).",
                "Длительность эффекта яда, в тиках (20 тиков = 1 секунда).");
        addExtra("poisonAmplifier", "Poison level", "Сила яда",
                "Poison amplifier (0 = Poison I).",
                "Уровень яда (0 = Отравление I).");
        addExtra("meleeDamage", "Melee damage", "Урон ближнего боя",
                "Damage of the basic melee attack.",
                "Урон обычной атаки ближнего боя.");
        addExtra("estocadaDamage", "Lunge thrust damage", "Урон выпада",
                "Damage of the estocada/lunge thrust attack.",
                "Урон атаки-выпада (эстокады).");
        addExtra("slamDamage", "Slam damage", "Урон ударной волны",
                "Damage of the ground slam attack.",
                "Урон ударной волны/слэма по земле.");
        addExtra("slamRadius", "Slam radius", "Радиус ударной волны",
                "Area radius affected by the slam.",
                "Радиус области, которую затрагивает слэм.");
        addExtra("meleeRange", "Melee range", "Дистанция ближнего боя",
                "Distance required to use melee attacks.",
                "Дистанция, на которой доступны атаки ближнего боя.");
        addExtra("lungeRange", "Lunge range", "Дистанция выпада",
                "Distance at which the lunge attack may be used.",
                "Дистанция, на которой можно использовать выпад.");
        addExtra("jumpMinRange", "Min jump range", "Мин. дистанция прыжка",
                "Minimum target distance for the jump attack.",
                "Минимальная дистанция до цели для прыжковой атаки.");
        addExtra("jumpMaxRange", "Max jump range", "Макс. дистанция прыжка",
                "Maximum target distance for the jump attack.",
                "Максимальная дистанция до цели для прыжковой атаки.");
        addExtra("attackCooldownTicks", "Attack cooldown", "Перезарядка атаки",
                "Delay between attacks, in ticks (20 ticks = 1 second).",
                "Пауза между атаками, в тиках (20 тиков = 1 секунда).");
        addExtra("explosionDamageMultiplier", "Explosion damage taken", "Множитель урона от взрывов",
                "Multiplier for damage taken from explosions.",
                "Множитель урона, который моб получает от взрывов.");
        addExtra("auraRadius", "Aura radius", "Радиус ауры",
                "Radius of the statue slow aura.",
                "Радиус замедляющей ауры статуи.");
        addExtra("auraIntervalTicks", "Aura pulse interval", "Интервал пульса ауры",
                "Delay between aura pulses, in ticks (20 ticks = 1 second).",
                "Пауза между пульсациями ауры, в тиках (20 тиков = 1 секунда).");
        addExtra("auraSlowDuration", "Aura slow duration", "Длит. замедления ауры",
                "Slowness duration applied by the aura, in ticks (20 ticks = 1 second).",
                "Длительность замедления от ауры, в тиках (20 тиков = 1 секунда).");
        addExtra("auraSlowAmplifier", "Aura slow level", "Сила замедления ауры",
                "Slowness amplifier applied by the aura (0 = Slowness I).",
                "Уровень замедления от ауры (0 = Замедление I).");
        addExtra("wrenchDamage", "Wrench damage", "Урон гаечного ключа",
                "Damage of the engineer's thrown/melee wrench.",
                "Урон гаечного ключа инженера (бросок или удар).");
        addExtra("projectileSpeed", "Projectile speed", "Скорость снаряда",
                "Flight speed of thrown projectiles.",
                "Скорость полёта бросаемых снарядов.");
        addExtra("summonCooldownTicks", "Summon cooldown", "Перезарядка призыва",
                "Cooldown between construct summons, in ticks (20 ticks = 1 second).",
                "Пауза между призывом конструкций, в тиках (20 тиков = 1 секунда).");
        addExtra("throwCooldownTicks", "Throw cooldown", "Перезарядка броска",
                "Cooldown between wrench throws, in ticks (20 ticks = 1 second).",
                "Пауза между бросками ключа, в тиках (20 тиков = 1 секунда).");
        addExtra("meleeCooldownTicks", "Melee cooldown", "Перезарядка ближнего боя",
                "Cooldown between melee attacks, in ticks (20 ticks = 1 second).",
                "Пауза между атаками ближнего боя, в тиках (20 тиков = 1 секунда).");
        addExtra("throwRange", "Throw range", "Дальность броска",
                "Maximum range for throwing the wrench.",
                "Максимальная дальность броска ключа.");
        addExtra("maxAutomatons", "Max automatons", "Макс. автоматонов",
                "Maximum automatons the engineer may keep summoned.",
                "Сколько автоматонов инженер может держать призванными.");
        addExtra("maxDrones", "Max drones", "Макс. дронов",
                "Maximum drones the engineer may keep summoned.",
                "Сколько дронов инженер может держать призванными.");
        addExtra("maxTowers", "Max towers", "Макс. вышек",
                "Maximum towers the engineer may keep summoned.",
                "Сколько вышек инженер может держать призванными.");
        addExtra("constructLeashDistance", "Construct return distance", "Дальность возврата конструкций",
                "If an automaton/drone/tower gets farther than this from the engineer, it returns.",
                "Если автоматон, дрон или вышка отойдут от инженера дальше этого значения, они возвращаются.");
        addExtra("spinDamageMultiplier", "Spin damage multiplier", "Множитель урона вращения",
                "Multiplier applied to spin-attack damage.",
                "Множитель урона атаки вращением.");
        addExtra("spinRadius", "Spin radius", "Радиус вращения",
                "Area radius of the spin attack.",
                "Радиус области атаки вращением.");
        addExtra("projectileDamage", "Projectile damage", "Урон снаряда",
                "Damage dealt by the mob's projectile.",
                "Урон снаряда этого моба.");
        addExtra("maxShootDistance", "Max shoot distance", "Макс. дистанция стрельбы",
                "Maximum distance at which the mob will shoot.",
                "Максимальная дистанция, с которой моб будет стрелять.");
        addExtra("shootCooldownTicks", "Shoot cooldown", "Перезарядка стрельбы",
                "Delay between shots, in ticks (20 ticks = 1 second).",
                "Пауза между выстрелами, в тиках (20 тиков = 1 секунда).");
        addExtra("escortRadius", "Follow radius", "Радиус следования",
                "How closely the drone orbits/follows its owner while idle.",
                "На каком расстоянии дрон следует/кружит вокруг владельца вне боя.");
        addExtra("combatHoverRadius", "Combat hover radius", "Радиус боя в воздухе",
                "Hover distance the drone keeps from the target in combat.",
                "Дистанция, на которой дрон держится от цели во время боя в воздухе.");
        addExtra("laserDamageMultiplier", "Laser damage multiplier", "Множитель урона лазера",
                "Multiplier applied to the tower laser damage.",
                "Множитель урона лазера вышки.");
        addExtra("laserRadius", "Laser radius", "Радиус лазера",
                "Area radius affected by the laser attack.",
                "Радиус области, которую затрагивает лазер.");
        addExtra("stompDamageMultiplier", "Stomp damage multiplier", "Множитель урона топота",
                "Multiplier applied to stomp damage.",
                "Множитель урона атаки топотом.");
        addExtra("stompRadius", "Stomp radius", "Радиус топота",
                "Area radius of the stomp attack.",
                "Радиус области атаки топотом.");
        addExtra("rangedRange", "Ranged attack range", "Дальность дальней атаки",
                "Maximum range of the tower's ranged attack.",
                "Максимальная дальность дальней атаки вышки.");
        addExtra("stompSlowDuration", "Stomp slow duration", "Длит. замедления от топота",
                "Slowness duration from stomp, in ticks (20 ticks = 1 second).",
                "Длительность замедления от топота, в тиках (20 тиков = 1 секунда).");
        addExtra("stompSlowAmplifier", "Stomp slow level", "Сила замедления от топота",
                "Slowness amplifier from stomp (0 = Slowness I).",
                "Уровень замедления от топота (0 = Замедление I).");
        addExtra("stompWeakDuration", "Stomp weakness duration", "Длит. слабости от топота",
                "Weakness duration from stomp, in ticks (20 ticks = 1 second).",
                "Длительность слабости от топота, в тиках (20 тиков = 1 секунда).");
        addExtra("stompWeakAmplifier", "Stomp weakness level", "Сила слабости от топота",
                "Weakness amplifier from stomp (0 = Weakness I).",
                "Уровень слабости от топота (0 = Слабость I).");
        add("gui.mobexpansion.icon.arrow_left", "Previous", "Назад");
        add("gui.mobexpansion.icon.arrow_right", "Next", "Вперёд");
        add("gui.mobexpansion.icon.arrow_up", "Move up", "Выше");
        add("gui.mobexpansion.icon.arrow_down", "Move down", "Ниже");
        add("gui.mobexpansion.condition.empty", "No conditions", "Нет условий");
        add("gui.mobexpansion.condition.value_hint", "min,max[,period]", "мин,макс[,период]");
        add("gui.mobexpansion.condition.exact", "Exact", "Точно");
        add("gui.mobexpansion.condition.raining.any", "Rain: any", "Дождь: любой");
        add("gui.mobexpansion.condition.raining.yes", "Rain: yes", "Дождь: да");
        add("gui.mobexpansion.condition.raining.no", "Rain: no", "Дождь: нет");
        add("gui.mobexpansion.condition.thundering.any", "Thunder: any", "Гром: любой");
        add("gui.mobexpansion.condition.thundering.yes", "Thunder: yes", "Гром: да");
        add("gui.mobexpansion.condition.thundering.no", "Thunder: no", "Гром: нет");
        add("gui.mobexpansion.condition.type.killed_by_player", "Killed by player", "Убит игроком");
        add("gui.mobexpansion.condition.type.match_biome", "Biome", "Биом");
        add("gui.mobexpansion.condition.type.match_dimension", "Dimension", "Измерение");
        add("gui.mobexpansion.condition.type.match_structure", "Structure", "Структура");
        add("gui.mobexpansion.condition.type.match_weather", "Weather", "Погода");
        add("gui.mobexpansion.condition.type.match_time", "Time", "Время");
        add("gui.mobexpansion.condition.type.is_light_level", "Light level", "Уровень света");
        add("gui.mobexpansion.condition.type.survives_explosion", "Survives explosion", "Выживает взрыв");
        add("gui.mobexpansion.condition.type.match_main_hand", "Main hand", "Основная рука");
        add("gui.mobexpansion.condition.type.match_off_hand", "Off hand", "Вторая рука");
        add("gui.mobexpansion.condition.killed_by_player", "Killed by player", "Убит игроком");
        add("gui.mobexpansion.condition.match_biome", "Biome: %s", "Биом: %s");
        add("gui.mobexpansion.condition.match_dimension", "Dim: %s", "Изм.: %s");
        add("gui.mobexpansion.condition.match_structure", "Struct: %s (%s)", "Структура: %s (%s)");
        add("gui.mobexpansion.condition.match_weather", "Weather %s/%s", "Погода %s/%s");
        add("gui.mobexpansion.condition.match_time", "Time %s-%s /%s", "Время %s-%s /%s");
        add("gui.mobexpansion.condition.is_light_level", "Light %s-%s", "Свет %s-%s");
        add("gui.mobexpansion.condition.survives_explosion", "Survives explosion", "Выживает взрыв");
        add("gui.mobexpansion.condition.match_main_hand", "Main: %s", "Осн.: %s");
        add("gui.mobexpansion.condition.match_off_hand", "Off: %s", "Втор.: %s");
        add("gui.mobexpansion.item_picker.title", "Item picker", "Выбор предмета");
        add("gui.mobexpansion.item_picker.search", "Search by name or id...", "Поиск по названию или id...");
        add("gui.mobexpansion.item_picker.select", "Select", "Выбрать");
        add("gui.mobexpansion.item_picker.cancel", "Cancel", "Отмена");
        add("gui.mobexpansion.item_picker.col.name", "Name", "Название");
        add("gui.mobexpansion.item_picker.col.id", "Id", "ID");
        add("gui.mobexpansion.item_picker.empty", "No items found", "Ничего не найдено");
    }

    @Override
    @NotNull
    public CompletableFuture<?> run(@NotNull CachedOutput cachedOutput) {
        addLocales();
        if (data.isEmpty())
            return CompletableFuture.allOf();
        CompletableFuture<?>[] futures = new CompletableFuture[locales.length];
        for (int i = 0; i < locales.length; i++) {
            String locale = locales[i];
            Path target = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                    .resolve(modid)
                    .resolve("lang")
                    .resolve(locale + ".json");
            futures[i] = save(cachedOutput, i, target);
        }

        return CompletableFuture.allOf(futures);
    }

    @NotNull
    public String getName() {
        return "Languages: " + Arrays.toString(locales) + " for mod: " + modid;
    }

    private CompletableFuture<?> save(CachedOutput cache, int localeIndex, Path target) {
        JsonObject json = new JsonObject();
        for (Map.Entry<String, String[]> entry : data.entrySet()) {
            String[] translations = entry.getValue();
            if (localeIndex < translations.length) {
                json.addProperty(entry.getKey(), translations[localeIndex]);
            } else {
                json.addProperty(entry.getKey(), translations.length > 0 ? translations[0] : entry.getKey());
            }
        }
        return DataProvider.saveStable(cache, json, target);
    }

    public void addBlock(Supplier<? extends Block> key, String... translations) {
        add(key.get(), translations);
    }

    public void add(Block key, String... translations) {
        add(key.getDescriptionId(), translations);
    }

    public void addItem(Supplier<? extends Item> key, String... translations) {
        add(key.get(), translations);
    }

    public void add(Item key, String... translations) {
        add(key.getDescriptionId(), translations);
    }

    public void addItemStack(Supplier<ItemStack> key, String... translations) {
        add(key.get(), translations);
    }

    public void add(ItemStack key, String... translations) {
        add(key.getDescriptionId(), translations);
    }

    public void addEffect(Supplier<? extends MobEffect> key, String... translations) {
        add(key.get(), translations);
    }

    public void add(MobEffect key, String... translations) {
        add(key.getDescriptionId(), translations);
    }

    public void addEntityType(Supplier<? extends EntityType<?>> key, String... translations) {
        add(key.get(), translations);
    }

    public void add(EntityType<?> key, String... translations) {
        add(key.getDescriptionId(), translations);
    }

    public void addTag(Supplier<? extends TagKey<?>> key, String... translations) {
        add(key.get(), translations);
    }

    public void add(TagKey<?> tagKey, String... translations) {
        add(Tags.getTagTranslationKey(tagKey), translations);
    }

    public void add(String key, String... translations) {
        if (translations.length != locales.length)
            throw new IllegalArgumentException("Translation lengths do not match to locales. Expected: " + locales.length + ", got: " + translations.length);
        if (data.put(key, translations) != null)
            throw new IllegalStateException("Duplicate translation key " + key);
    }

    private void addExtra(String key, String en, String ru, String enTooltip, String ruTooltip) {
        add("gui.mobexpansion.mob_drop_config.extra." + key, en, ru);
        add("gui.mobexpansion.mob_drop_config.extra." + key + ".tooltip", enTooltip, ruTooltip);
    }

    public void addDimension(ResourceKey<Level> dimension, String... translations) {
        add(dimension.location().toLanguageKey("dimension"), translations);
    }
}
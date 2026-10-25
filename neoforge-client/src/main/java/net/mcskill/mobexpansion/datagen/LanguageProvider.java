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
        add(MobExEntities.SWARMER_ANT.get(), "Swarmer Ant", "Муравей-роевик");
        add(MobExEntities.BEETLE_TROOPER.get(), "Beetle Trooper", "Жук-штурмовик");
        add(MobExEntities.GUNSHIP_MOTH.get(), "Gunship Moth", "§6Авиа-Моль");
        add(MobExEntities.HUNTSMAN_SPIDER.get(), "Huntsman Spider", "Паук-охотник");
        add(MobExEntities.LADYBUG_MORTAR.get(), "Ladybug Mortar", "Божья коровка-миномёт");
        add(MobExEntities.WOODLOUSE_TANK.get(), "Woodlouse Tank", "Мокрица-танк");
        add(MobExEntities.SCULK_CRABBY.get(), "Sculk Crabby", "Скалк-краб");
        add(MobExEntities.SCULKED_CRUSTACEAN.get(), "Sculked Crustacean", "Заскалкованный рак");
        add(MobExEntities.SCULK_SCREAMER.get(), "Sculk Screamer", "Скалк-крикун");
        add(MobExEntities.SCULK_SENTINEL.get(), "Sculk Sentinel", "Скалк-страж");
        add(MobExEntities.ECHO_ENSHROUDER.get(), "Echo Enshrouder", "Эхо-окутыватель");
        add(MobExEntities.SOUL_BEARER.get(), "Soul Bearer", "Носитель душ");
        add(MobExEntities.POSSESSED_CINDER_ARMOR.get(), "Possessed Cinder Armor", "Одержимый пепельный доспех");
        add(MobExEntities.POSSESSED_RUIN_ARMOR.get(), "Possessed Ruin Armor", "Одержимый доспех руин");
        add(MobExEntities.POSSESSED_VIKING_ARMOR.get(), "Possessed Viking Armor", "Одержимый доспех мерзлоты");
        add(MobExEntities.APOCALYPSE_TYPE_A.get(), "Apocalypse Type A", "§5Апокалипсис");
        add(MobExEntities.BLOSSOM_DRAKE.get(), "Blossom Drake", "Цветочный дракон");
        add(MobExEntities.FLORAL_SERPENT.get(), "Floral Serpent", "Цветочный змей");
        add(MobExEntities.GLIMMERING_UNICORN.get(), "Glimmering Unicorn", "Мерцающий единорог");
        add(MobExEntities.SERENITY_SENTINEL.get(), "Serenity Sentinel", "Страж безмятежности");
        add(MobExEntities.QUEEN_EVERBLOOM.get(), "Queen Everbloom", "Королева Эвербллум");
        add(MobExEntities.SCARLET_QUEEN_EVERBLOOM.get(), "Scarlet Queen Everbloom", "Алая королева Эвербллум");
        add(MobExEntities.ABILITY_PROJECTILE.get(), "Projectile", "Снаряд");
        add(MobExEntities.ABILITY_EFFECT.get(), "Ability Effect", "Эффект способности");
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
        add(MobExItems.SWARMER_ANT_SPAWN_EGG.get(), "Swarmer Ant spawn egg", "Яйцо призыва: Муравей-роевик");
        add(MobExItems.BEETLE_TROOPER_SPAWN_EGG.get(), "Beetle Trooper spawn egg", "Яйцо призыва: Жук-штурмовик");
        add(MobExItems.GUNSHIP_MOTH_SPAWN_EGG.get(), "Gunship Moth spawn egg", "Яйцо призыва: §6Авиа-Моль");
        add(MobExItems.HUNTSMAN_SPIDER_SPAWN_EGG.get(), "Huntsman Spider spawn egg", "Яйцо призыва: Паук-охотник");
        add(MobExItems.LADYBUG_MORTAR_SPAWN_EGG.get(), "Ladybug Mortar spawn egg", "Яйцо призыва: Божья коровка-миномёт");
        add(MobExItems.WOODLOUSE_TANK_SPAWN_EGG.get(), "Woodlouse Tank spawn egg", "Яйцо призыва: Мокрица-танк");
        add(MobExItems.SCULK_CRABBY_SPAWN_EGG.get(), "Sculk Crabby spawn egg", "Яйцо призыва: Скалк-краб");
        add(MobExItems.SCULKED_CRUSTACEAN_SPAWN_EGG.get(), "Sculked Crustacean spawn egg", "Яйцо призыва: Заскалкованный рак");
        add(MobExItems.SCULK_SCREAMER_SPAWN_EGG.get(), "Sculk Screamer spawn egg", "Яйцо призыва: Скалк-крикун");
        add(MobExItems.SCULK_SENTINEL_SPAWN_EGG.get(), "Sculk Sentinel spawn egg", "Яйцо призыва: Скалк-страж");
        add(MobExItems.ECHO_ENSHROUDER_SPAWN_EGG.get(), "Echo Enshrouder spawn egg", "Яйцо призыва: Эхо-окутыватель");
        add(MobExItems.SOUL_BEARER_SPAWN_EGG.get(), "Soul Bearer spawn egg", "Яйцо призыва: Носитель душ");
        add(MobExItems.POSSESSED_CINDER_ARMOR_SPAWN_EGG.get(), "Possessed Cinder Armor spawn egg", "Яйцо призыва: Одержимый пепельный доспех");
        add(MobExItems.POSSESSED_RUIN_ARMOR_SPAWN_EGG.get(), "Possessed Ruin Armor spawn egg", "Яйцо призыва: Одержимый доспех руин");
        add(MobExItems.POSSESSED_VIKING_ARMOR_SPAWN_EGG.get(), "Possessed Viking Armor spawn egg", "Яйцо призыва: Одержимый доспех мерзлоты");
        add(MobExItems.APOCALYPSE_TYPE_A_SPAWN_EGG.get(), "Apocalypse Type A spawn egg", "Яйцо призыва: §5Апокалипсис");
        add(MobExItems.BLOSSOM_DRAKE_SPAWN_EGG.get(), "Blossom Drake spawn egg", "Яйцо призыва: Цветочный дракон");
        add(MobExItems.FLORAL_SERPENT_SPAWN_EGG.get(), "Floral Serpent spawn egg", "Яйцо призыва: Цветочный змей");
        add(MobExItems.GLIMMERING_UNICORN_SPAWN_EGG.get(), "Glimmering Unicorn spawn egg", "Яйцо призыва: Мерцающий единорог");
        add(MobExItems.SERENITY_SENTINEL_SPAWN_EGG.get(), "Serenity Sentinel spawn egg", "Яйцо призыва: Страж безмятежности");
        add(MobExItems.QUEEN_EVERBLOOM_SPAWN_EGG.get(), "Queen Everbloom spawn egg", "Яйцо призыва: Королева Эвербллум");
        add(MobExItems.SCARLET_QUEEN_EVERBLOOM_SPAWN_EGG.get(), "Scarlet Queen Everbloom spawn egg", "Яйцо призыва: Алая королева Эвербллум");
        add(MobExBlocks.MOB_SPAWNER.get(), "Mob Spawner", "Спавнер мобов");
        add(MobExItems.DROP_CONFIGURATOR.get(), "Mob Configurator", "Настройка мобов");
        add(MobExItems.CAPRA_SKIN.get(), "Skin: Capra", "Скин: Capra");
        add(MobExItems.LILLITH_SKIN.get(), "Skin: Lillith", "Скин: Lillith");
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
        add("gui.mobexpansion.mob_spawner.activation_delay.tooltip", "After the spawner's chunk loads, wait this many ticks before the first wave. 0 = spawn immediately.", "После загрузки чанка со спавнером — ждать столько тиков до первой волны. 0 = сразу.");
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
        add("gui.mobexpansion.mob_drop_config.attr.clearance_level", "Clearance level", "Уровень допуска");
        add("gui.mobexpansion.mob_drop_config.attr.clearance_level.tooltip", "Minimum player clearance level (0-5) required to damage this mob. 0 - no requirement.", "Минимальный уровень допуска игрока (0-5), чтобы наносить урон этому мобу. 0 — без требования.");
        add("gui.mobexpansion.mob_drop_config.extras_table", "Extra parameters", "Доп. параметры");
        add("gui.mobexpansion.mob_drop_config.col.extra", "Parameter", "Параметр");
        add("gui.mobexpansion.mob_drop_config.col.value", "Value", "Значение");
        add("gui.mobexpansion.mob_drop_config.no_extras", "No extra parameters", "Нет доп. параметров");
        addExtra("attack_damage", "Bite: damage", "Укус: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("attack_speed", "Bite: speed", "Укус: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("axe_combo_damage", "Axe combo: damage", "Серия топором: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("axe_combo_speed", "Axe combo: speed", "Серия топором: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("axe_throw_damage", "Axe throw: damage", "Бросок топора: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("axe_throw_speed", "Axe throw: speed", "Бросок топора: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("basic_attack_damage", "Strike: damage", "Удар: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("basic_attack_speed", "Strike: speed", "Удар: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("basic_attack2_damage", "Double strike: damage", "Двойной удар: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("basic_attack2_speed", "Double strike: speed", "Двойной удар: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("beam_damage", "Beam: damage", "Луч: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("beam_speed", "Beam: speed", "Луч: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("black_hole_damage", "Black hole: damage", "Чёрная дыра: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("black_hole_speed", "Black hole: speed", "Чёрная дыра: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("black_star_field_damage", "Black star field: damage", "Поле чёрных звёзд: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("black_star_field_speed", "Black star field: speed", "Поле чёрных звёзд: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("blackbox_damage", "Black box: damage", "Чёрный ящик: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("blackbox_speed", "Black box: speed", "Чёрный ящик: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("buildings_collapse_damage", "Buildings collapse: damage", "Обрушение зданий: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("buildings_collapse_speed", "Buildings collapse: speed", "Обрушение зданий: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("burrow_damage", "Burrow ambush: damage", "Засада из-под земли: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("burrow_speed", "Burrow ambush: speed", "Засада из-под земли: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("charge_damage", "Charge: damage", "Таран: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("charge_speed", "Charge: speed", "Таран: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("combo_damage", "Combo: damage", "Комбо: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("combo_speed", "Combo: speed", "Комбо: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("dash_damage", "Dash: damage", "Рывок: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("dash_speed", "Dash: speed", "Рывок: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("eat_damage", "Devour: damage", "Поедание: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("eat_speed", "Devour: speed", "Поедание: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("explosion_damage", "Explosion: damage", "Взрыв: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("explosion_speed", "Explosion: speed", "Взрыв: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("fire_damage", "Shot: damage", "Выстрел: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("fire_speed", "Shot: speed", "Выстрел: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("gun_burst_damage", "Gun burst: damage", "Пулемётная очередь: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("gun_burst_speed", "Gun burst: speed", "Пулемётная очередь: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("headbutt_damage", "Headbutt: damage", "Удар головой: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("headbutt_speed", "Headbutt: speed", "Удар головой: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("ice_spikes_damage", "Ice spikes: damage", "Ледяные шипы: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("ice_spikes_speed", "Ice spikes: speed", "Ледяные шипы: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("left_chop_damage", "Left chop: damage", "Рубящий левой: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("left_chop_speed", "Left chop: speed", "Рубящий левой: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("left_fire_damage", "Left cannon: damage", "Левая пушка: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("left_fire_speed", "Left cannon: speed", "Левая пушка: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("left_shot_damage", "Left soul shot: damage", "Сгусток душ (левая): урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("left_shot_speed", "Left soul shot: speed", "Сгусток душ (левая): скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("left_slash_damage", "Left slash: damage", "Удар левой лапой: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("left_slash_speed", "Left slash: speed", "Удар левой лапой: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("overhead_attack_damage", "Overhead strike: damage", "Удар сверху: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("overhead_attack_speed", "Overhead strike: speed", "Удар сверху: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("pinch_damage", "Pinch: damage", "Щипок: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("pinch_speed", "Pinch: speed", "Щипок: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("pummel_damage", "Pummel: damage", "Толчок: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("pummel_speed", "Pummel: speed", "Толчок: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("rampage_damage", "Rampage: damage", "Натиск: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("rampage_speed", "Rampage: speed", "Натиск: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("reckless_leap_damage", "Reckless leap: damage", "Безрассудный прыжок: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("reckless_leap_speed", "Reckless leap: speed", "Безрассудный прыжок: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("reckless_strike_damage", "Reckless strike: damage", "Безрассудный удар: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("reckless_strike_speed", "Reckless strike: speed", "Безрассудный удар: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("right_chop_damage", "Right chop: damage", "Рубящий правой: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("right_chop_speed", "Right chop: speed", "Рубящий правой: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("right_fire_damage", "Right cannon: damage", "Правая пушка: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("right_fire_speed", "Right cannon: speed", "Правая пушка: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("right_shot_damage", "Right soul shot: damage", "Сгусток душ (правая): урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("right_shot_speed", "Right soul shot: speed", "Сгусток душ (правая): скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("right_slash_damage", "Right slash: damage", "Удар правой лапой: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("right_slash_speed", "Right slash: speed", "Удар правой лапой: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("rocket_barrage_damage", "Rocket barrage: damage", "Ракетный залп: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("rocket_barrage_speed", "Rocket barrage: speed", "Ракетный залп: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("roll_damage", "Roll: damage", "Перекат: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("roll_speed", "Roll: speed", "Перекат: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("roll_exit_emp_damage", "EMP blast: damage", "ЭМИ-импульс: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("roll_exit_emp_speed", "EMP blast: speed", "ЭМИ-импульс: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("ruination_damage", "Ruination: damage", "Руина: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("ruination_speed", "Ruination: speed", "Руина: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("scream_damage", "Scream: damage", "Крик: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("scream_speed", "Scream: speed", "Крик: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("shoot_damage", "Soul flames: damage", "Душевное пламя: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("shoot_speed", "Soul flames: speed", "Душевное пламя: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("slices_damage", "Slices: damage", "Рассечения: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("slices_speed", "Slices: speed", "Рассечения: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("smash_damage", "Smash: damage", "Удар с размаху: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("smash_speed", "Smash: speed", "Удар с размаху: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("soul_explosion_damage", "Soul explosion: damage", "Взрыв душ: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("soul_explosion_speed", "Soul explosion: speed", "Взрыв душ: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("stab_damage", "Stab: damage", "Укол: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("stab_speed", "Stab: speed", "Укол: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("stomp_damage", "Stomp: damage", "Топот: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("stomp_speed", "Stomp: speed", "Топот: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("summon_spike_damage", "Sculk spikes: damage", "Скалковые шипы: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("summon_spike_speed", "Sculk spikes: speed", "Скалковые шипы: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("swing_damage", "Swing: damage", "Взмах: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("swing_speed", "Swing: speed", "Взмах: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("throw_damage", "Throw: damage", "Бросок: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("throw_speed", "Throw: speed", "Бросок: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("tongue_attack_damage", "Tongue: damage", "Язык: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("tongue_attack_speed", "Tongue: speed", "Язык: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("underslash_damage", "Underslash: damage", "Удар снизу: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("underslash_speed", "Underslash: speed", "Удар снизу: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("uppercut_damage", "Uppercut: damage", "Апперкот: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("uppercut_speed", "Uppercut: speed", "Апперкот: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("vanish_damage", "Vanish slam: damage", "Исчезновение и удар: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("vanish_speed", "Vanish slam: speed", "Исчезновение и удар: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("void_storm_damage", "Void storm: damage", "Вихрь пустоты: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("void_storm_speed", "Void storm: speed", "Вихрь пустоты: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
        addExtra("white_void_damage", "White void: damage", "Белая пустота: урон",
                "Damage multiplier of this attack: base attack damage × value. For multi-hit attacks it sets the strongest hit, the rest scale proportionally.",
                "Коэффициент урона атаки: базовый урон моба × значение. Для серий ударов задаёт сильнейший удар, остальные меняются пропорционально.");
        addExtra("white_void_speed", "White void: speed", "Белая пустота: скорость",
                "Attack animation speed: 1 = normal, 2 = twice as fast. Hit timings follow the animation.",
                "Скорость анимации атаки: 1 — обычная, 2 — вдвое быстрее. Тайминги ударов следуют за анимацией.");
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
        addExtra("spawnerHomeRadius", "Home radius (spawner)", "Радиус дома (спавнер)",
                "Max distance in blocks a mob spawned by a Mob Expansion spawner can move away from it (1-64). 0 = no limit. The mob will not chase targets beyond it and returns home.",
                "Максимальное расстояние в блоках, на которое моб из спавнера Mob Expansion может отойти от него (1-64). 0 — без ограничения. Моб не преследует цели за пределами радиуса и возвращается домой.");
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

        add("label.mobexpansion.clearance_required", "Clearance level %s required", "Нужен уровень допуска %s");
        add("message.mobexpansion.clearance.denied", "Clearance level %s required (you have %s)", "Нужен уровень допуска %s (у вас %s)");
        add("message.mobexpansion.clearance.changed", "Your clearance level is now %s", "Ваш уровень допуска: %s");
        add("commands.mobexpansion.clearance.get", "%s has clearance level %s (max %s)", "Уровень допуска %s: %s (макс. %s)");
        add("commands.mobexpansion.spawnall", "Spawners activated: %s of %s found within %s blocks", "Спавнеры сработали: %s из %s в радиусе %s блоков");
        // EMI: страница моба
        add("emi.category.mobexpansion.mob_info", "Mobs", "Мобы");
        add("gui.mobexpansion.emi.hp", "HP", "HP");
        add("gui.mobexpansion.emi.hp.exact", "Health: %s", "Здоровье: %s");
        add("gui.mobexpansion.emi.armor", "Armor: %s", "Броня: %s");
        add("gui.mobexpansion.emi.armor.short", "Armor:", "Броня:");
        add("gui.mobexpansion.emi.speed", "Speed: %s", "Скорость: %s");
        add("gui.mobexpansion.emi.clearance", "Clearance level", "Уровень доступа");
        add("gui.mobexpansion.emi.clearance.none", "No clearance required", "Допуск не требуется");
        add("gui.mobexpansion.emi.clearance.required", "Required clearance: %s", "Требуемый допуск: %s");
        add("gui.mobexpansion.emi.clearance.own", "Your clearance: %s", "Ваш допуск: %s");
        add("gui.mobexpansion.emi.location", "Location:", "Расположение:");
        add("gui.mobexpansion.emi.location.unknown", "unknown", "неизвестно");
        add("gui.mobexpansion.emi.location.unknown.tooltip", "No MobExpansion spawners with this mob", "Нет спавнеров MobExpansion с этим мобом");
        add("gui.mobexpansion.emi.location.spawners", "Spawners: %s", "Спавнеры: %s");
        add("gui.mobexpansion.emi.damage", "Damage:", "Урон:");
        add("gui.mobexpansion.emi.damage.melee", "Melee: %s", "Ближний бой: %s");
        add("gui.mobexpansion.emi.respawn", "Respawn in:", "До респавна:");
        add("gui.mobexpansion.emi.respawn.alive", "alive", "жив");
        add("gui.mobexpansion.emi.respawn.soon", "soon", "скоро");
        add("gui.mobexpansion.emi.xp", "XP:", "Опыт:");
        add("gui.mobexpansion.emi.drops", "Drops", "Выпадаемые предметы");
        add("gui.mobexpansion.emi.drops.more", "And %s more:", "И ещё %s:");
        add("gui.mobexpansion.emi.drop.count", "Count: %s–%s", "Количество: %s–%s");
        add("gui.mobexpansion.emi.drop.conditional", "Only under conditions", "Только при условиях");
        add("gui.mobexpansion.emi.drop.loot_table", "Vanilla loot table", "Ванильная таблица лута");
        add("gui.mobexpansion.emi.drop.config", "MobExpansion drop config", "Дроп из настройщика MobExpansion");
        add("gui.mobexpansion.emi.suffix.thousand", "K", "К");
        add("gui.mobexpansion.emi.suffix.million", "M", "КК");
        add("gui.mobexpansion.emi.duration.hours", "%sh %sm", "%s ч %s мин");
        add("gui.mobexpansion.emi.dimension.minecraft.the_nether", "Nether", "Незер");
        add("gui.mobexpansion.emi.dimension.minecraft.the_end", "The End", "Энд");
        add("commands.mobexpansion.clearance.set.single", "Set clearance level of %s to %s", "Уровень допуска %s установлен: %s");
        add("commands.mobexpansion.clearance.set.multiple", "Updated clearance level of %s players", "Уровень допуска изменён у %s игроков");
        add("commands.mobexpansion.skin.set.single", "Set skin of %s to %s", "Скин %s: %s");
        add("commands.mobexpansion.skin.set.multiple", "Set skin %s for %s players", "Скин %s надет на %s игроков");
        add("commands.mobexpansion.skin.reset.single", "Reset skin of %s", "Скин %s снят");
        add("commands.mobexpansion.skin.reset.multiple", "Reset skin of %s players", "Скин снят у %s игроков");
        add("commands.mobexpansion.skin.unknown", "Unknown skin: %s", "Неизвестный скин: %s");
        add("gui.mobexpansion.cosmetics.title", "Character customization", "Кастомизация персонажа");
        add("gui.mobexpansion.cosmetics.prefixes", "Prefixes", "Префиксы");
        add("gui.mobexpansion.cosmetics.no_prefix", "No prefix", "Без префикса");
        add("cosmetics.mobexpansion.prefix.chaos", "Chaos", "Хаос");
        add("cosmetics.mobexpansion.prefix.chaos2", "Chaos II", "Хаос II");
        add("cosmetics.mobexpansion.prefix.chaos3", "Chaos III", "Хаос III");
        add("gui.mobexpansion.cosmetics.skins", "Skins", "Скины");
        add("gui.mobexpansion.cosmetics.trails", "Trails", "Шлейфы");
        add("gui.mobexpansion.cosmetics.no_skin", "No skin", "Без скина");
        add("gui.mobexpansion.cosmetics.no_trail", "No trail", "Без шлейфа");
        add("gui.mobexpansion.cosmetics.equipped", "Equipped", "Надето");
        add("gui.mobexpansion.cosmetics.locked", "Locked", "Закрыто");
        add("config.mobexpansion.hide_skin_attack_fx_first_person", "Hide skin attack effect in first person", "Скрывать эффект удара скина от первого лица");
        add("gui.mobexpansion.cosmetics.locked_tooltip", "Not unlocked yet", "Ещё не открыто");
        add("gui.mobexpansion.cosmetics.click_to_equip", "Click to equip", "Нажми, чтобы надеть");
        add("gui.mobexpansion.cosmetics.preview_hint", "Hover a card to preview", "Наведи на карточку");
        add("gui.mobexpansion.cosmetics.key_hint", "Open: %s or /cosmetics", "Открыть: %s или /cosmetics");
        add("gui.mobexpansion.cosmetics.loading", "Loading...", "Загрузка...");
        add("gui.mobexpansion.cosmetics.unavailable", "Not available on this server", "Недоступно на этом сервере");
        add("message.mobexpansion.cosmetics.locked", "This cosmetic is not unlocked", "Эта косметика вам не открыта");
        add("key.mobexpansion.cosmetics", "Character customization", "Кастомизация персонажа");
        add("key.categories.mobexpansion", "MobExpansion", "MobExpansion");
        add("cosmetics.mobexpansion.skin.capra", "Capra", "Капра");
        add("cosmetics.mobexpansion.skin.lillith", "Lillith", "Лилит");
        add("cosmetics.mobexpansion.trail.nyan_cat", "Nyan Cat", "Нян-кот");
        add("cosmetics.mobexpansion.trail.rainbow", "Rainbow", "Радуга");
        add("cosmetics.mobexpansion.trail.red_sparks", "Red sparks", "Красные искры");
        add("cosmetics.mobexpansion.trail.tiles", "Tiles", "Плитка");
        add("cosmetics.mobexpansion.trail.tron_blue", "Tron (blue)", "Трон (синий)");
        add("cosmetics.mobexpansion.trail.tron_yellow", "Tron (yellow)", "Трон (жёлтый)");
        add("cosmetics.mobexpansion.trail.funny", "Smileys", "Смайлы");
        add("cosmetics.mobexpansion.trail.neon", "Neon", "Неон");
        add("cosmetics.mobexpansion.trail.neon2", "Neon 2", "Неон 2");
        add("cosmetics.mobexpansion.trail.bloks", "Blocks", "Блоки");
        add("cosmetics.mobexpansion.trail.clauds", "Clouds", "Облака");
        add("cosmetics.mobexpansion.trail.coin", "Coins", "Монеты");
        add("cosmetics.mobexpansion.trail.fire", "Fire", "Огонь");
        add("cosmetics.mobexpansion.trail.hearts", "Hearts", "Сердца");
        add("cosmetics.mobexpansion.trail.ice_crystal", "Ice", "Лёд");
        add("cosmetics.mobexpansion.trail.lightning", "Lightning", "Молния");
        add("cosmetics.mobexpansion.trail.skills", "Skills", "Навыки");
        add("cosmetics.mobexpansion.trail.water", "Water", "Вода");
        add("cosmetics.mobexpansion.trail.korolev", "Korolev", "Королёв");
        add("cosmetics.mobexpansion.trail.osiris", "Osiris", "Озирис");
        add("cosmetics.mobexpansion.trail.cycle_trail", "Cycle", "Цикл");
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
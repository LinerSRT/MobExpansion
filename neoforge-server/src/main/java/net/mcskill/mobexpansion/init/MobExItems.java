package net.mcskill.mobexpansion.init;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.item.DropConfiguratorItem;
import net.mcskill.mobexpansion.item.SkinItem;
import net.mcskill.mobexpansion.registry.ItemHolder;
import net.mcskill.mobexpansion.registry.ItemRegistry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;

public class MobExItems {
    public static ItemRegistry ITEM_REGISTRY = new ItemRegistry(Core.MODID);
    public static final ItemHolder<Item> REGULAR_SPIDER_SPAWN_EGG = ITEM_REGISTRY.registerItem("regular_spider_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.REGULAR_SPIDER, 0x1A1A1A, 0x8B0000, properties));
    public static final ItemHolder<Item> POISON_SPIDER_SPAWN_EGG = ITEM_REGISTRY.registerItem("poison_spider_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.POISON_SPIDER, 0x1B2E14, 0x6BFF3A, properties));
    public static final ItemHolder<Item> SPIDER_SPAWN_SPAWN_EGG = ITEM_REGISTRY.registerItem("spider_spawn_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.SPIDER_SPAWN, 0x2B1B0E, 0x5C4033, properties));
    public static final ItemHolder<Item> REDSTONE_ENGINEER_SPAWN_EGG = ITEM_REGISTRY.registerItem("redstone_engineer_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.REDSTONE_ENGINEER, 0x4A1A1A, 0xE03030, properties));
    public static final ItemHolder<Item> REDSTONE_AUTOMATON_SPAWN_EGG = ITEM_REGISTRY.registerItem("redstone_automaton_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.REDSTONE_AUTOMATON, 0x3A2A2A, 0xC02020, properties));
    public static final ItemHolder<Item> REDSTONE_DRONE_SPAWN_EGG = ITEM_REGISTRY.registerItem("redstone_drone_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.REDSTONE_DRONE, 0x2A2A2A, 0xFF4040, properties));
    public static final ItemHolder<Item> REDSTONE_TOWER_SPAWN_EGG = ITEM_REGISTRY.registerItem("redstone_tower_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.REDSTONE_TOWER, 0x3A2020, 0x88CCFF, properties));
    public static final ItemHolder<Item> SENTINEL_STATUE_SPAWN_EGG = ITEM_REGISTRY.registerItem("sentinel_statue_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.SENTINEL_STATUE, 0x5A4A2A, 0xC8A84A, properties));
    public static final ItemHolder<Item> SENTINEL_GOLEM_SPAWN_EGG = ITEM_REGISTRY.registerItem("sentinel_golem_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.SENTINEL_GOLEM, 0x3A3020, 0xE0C050, properties));
    public static final ItemHolder<Item> RAT_SPAWN_EGG = ITEM_REGISTRY.registerItem("rat_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.RAT, 0x5A4A3A, 0xC8B090, properties));
    public static final ItemHolder<Item> MOSQUITO_SPAWN_EGG = ITEM_REGISTRY.registerItem("mosquito_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.MOSQUITO, 0x2A2A1A, 0x8B4513, properties));
    public static final ItemHolder<Item> LEECH_SPAWN_EGG = ITEM_REGISTRY.registerItem("leech_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.LEECH, 0x1A3010, 0x4A8B3A, properties));
    public static final ItemHolder<Item> RAT_KING_SPAWN_EGG = ITEM_REGISTRY.registerItem("rat_king_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.RAT_KING, 0x3A2A1A, 0x6B8B3A, properties));
    public static final ItemHolder<Item> SWARMER_ANT_SPAWN_EGG = ITEM_REGISTRY.registerItem("swarmer_ant_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.SWARMER_ANT, 0x3A3F45, 0xE0A030, properties));
    public static final ItemHolder<Item> BEETLE_TROOPER_SPAWN_EGG = ITEM_REGISTRY.registerItem("beetle_trooper_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.BEETLE_TROOPER, 0x2E3640, 0x6FA0C0, properties));
    public static final ItemHolder<Item> GUNSHIP_MOTH_SPAWN_EGG = ITEM_REGISTRY.registerItem("gunship_moth_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.GUNSHIP_MOTH, 0x4A4038, 0xC08040, properties));
    public static final ItemHolder<Item> HUNTSMAN_SPIDER_SPAWN_EGG = ITEM_REGISTRY.registerItem("huntsman_spider_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.HUNTSMAN_SPIDER, 0x303030, 0xD03030, properties));
    public static final ItemHolder<Item> LADYBUG_MORTAR_SPAWN_EGG = ITEM_REGISTRY.registerItem("ladybug_mortar_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.LADYBUG_MORTAR, 0x8A2020, 0x202020, properties));
    public static final ItemHolder<Item> WOODLOUSE_TANK_SPAWN_EGG = ITEM_REGISTRY.registerItem("woodlouse_tank_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.WOODLOUSE_TANK, 0x505860, 0x90A0A8, properties));
    public static final ItemHolder<Item> SCULK_CRABBY_SPAWN_EGG = ITEM_REGISTRY.registerItem("sculk_crabby_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.SCULK_CRABBY, 0x0A2A33, 0x29DFEB, properties));
    public static final ItemHolder<Item> SCULKED_CRUSTACEAN_SPAWN_EGG = ITEM_REGISTRY.registerItem("sculked_crustacean_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.SCULKED_CRUSTACEAN, 0x0D1F26, 0x1F8A99, properties));
    public static final ItemHolder<Item> SCULK_SCREAMER_SPAWN_EGG = ITEM_REGISTRY.registerItem("sculk_screamer_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.SCULK_SCREAMER, 0x061217, 0x39C6D6, properties));
    public static final ItemHolder<Item> SCULK_SENTINEL_SPAWN_EGG = ITEM_REGISTRY.registerItem("sculk_sentinel_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.SCULK_SENTINEL, 0x08171C, 0x5FF2FF, properties));
    public static final ItemHolder<Item> ECHO_ENSHROUDER_SPAWN_EGG = ITEM_REGISTRY.registerItem("echo_enshrouder_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.ECHO_ENSHROUDER, 0x050B0E, 0x7FE8F0, properties));
    public static final ItemHolder<Item> SOUL_BEARER_SPAWN_EGG = ITEM_REGISTRY.registerItem("soul_bearer_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.SOUL_BEARER, 0x0C1D24, 0x8FF7FF, properties));
    public static final ItemHolder<Item> POSSESSED_CINDER_ARMOR_SPAWN_EGG = ITEM_REGISTRY.registerItem("possessed_cinder_armor_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.POSSESSED_CINDER_ARMOR, 0x2A1A14, 0xFF6A1A, properties));
    public static final ItemHolder<Item> POSSESSED_RUIN_ARMOR_SPAWN_EGG = ITEM_REGISTRY.registerItem("possessed_ruin_armor_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.POSSESSED_RUIN_ARMOR, 0x2A2A26, 0x8A7A5A, properties));
    public static final ItemHolder<Item> POSSESSED_VIKING_ARMOR_SPAWN_EGG = ITEM_REGISTRY.registerItem("possessed_viking_armor_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.POSSESSED_VIKING_ARMOR, 0x2A3038, 0x9AD8FF, properties));
    public static final ItemHolder<Item> APOCALYPSE_TYPE_A_SPAWN_EGG = ITEM_REGISTRY.registerItem("apocalypse_type_a_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.APOCALYPSE_TYPE_A, 0x0A0A0A, 0xF0F0F0, properties));
    public static final ItemHolder<Item> BLOSSOM_DRAKE_SPAWN_EGG = ITEM_REGISTRY.registerItem("blossom_drake_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.BLOSSOM_DRAKE, 0xDF84A5, 0x62AC4C, properties));
    public static final ItemHolder<Item> FLORAL_SERPENT_SPAWN_EGG = ITEM_REGISTRY.registerItem("floral_serpent_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.FLORAL_SERPENT, 0x62AC4C, 0xE7D447, properties));
    public static final ItemHolder<Item> GLIMMERING_UNICORN_SPAWN_EGG = ITEM_REGISTRY.registerItem("glimmering_unicorn_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.GLIMMERING_UNICORN, 0xF4F0FF, 0xCC99C9, properties));
    public static final ItemHolder<Item> SERENITY_SENTINEL_SPAWN_EGG = ITEM_REGISTRY.registerItem("serenity_sentinel_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.SERENITY_SENTINEL, 0x6A73D1, 0xDDEEDD, properties));
    public static final ItemHolder<Item> QUEEN_EVERBLOOM_SPAWN_EGG = ITEM_REGISTRY.registerItem("queen_everbloom_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.QUEEN_EVERBLOOM, 0xFF96FB, 0xE7D447, properties));
    public static final ItemHolder<Item> SCARLET_QUEEN_EVERBLOOM_SPAWN_EGG = ITEM_REGISTRY.registerItem("scarlet_queen_everbloom_spawn_egg", properties -> new DeferredSpawnEggItem(MobExEntities.SCARLET_QUEEN_EVERBLOOM, 0xC0182A, 0xFEB144, properties));
    public static final ItemHolder<BlockItem> MOB_SPAWNER = ITEM_REGISTRY.registerBlockItem(MobExBlocks.MOB_SPAWNER);
    public static final ItemHolder<Item> DROP_CONFIGURATOR = ITEM_REGISTRY.registerItem("drop_configurator", DropConfiguratorItem::new, new Item.Properties().stacksTo(1));
    public static final ItemHolder<SkinItem> CAPRA_SKIN = ITEM_REGISTRY.registerItem("capra_skin", properties -> new SkinItem("capra", properties), new Item.Properties().stacksTo(1));
    public static final ItemHolder<SkinItem> LILLITH_SKIN = ITEM_REGISTRY.registerItem("lillith_skin", properties -> new SkinItem("lillith", properties), new Item.Properties().stacksTo(1));

    public static void register(IEventBus bus) {
        ITEM_REGISTRY.register(bus);
    }
}

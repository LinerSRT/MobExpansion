package net.mcskill.mobexpansion.init;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.item.DropConfiguratorItem;
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
    public static final ItemHolder<BlockItem> MOB_SPAWNER = ITEM_REGISTRY.registerBlockItem(MobExBlocks.MOB_SPAWNER);
    public static final ItemHolder<Item> DROP_CONFIGURATOR = ITEM_REGISTRY.registerItem("drop_configurator", DropConfiguratorItem::new, new Item.Properties().stacksTo(1));

    public static void register(IEventBus bus) {
        ITEM_REGISTRY.register(bus);
    }
}

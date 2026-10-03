package net.mcskill.mobexpansion.datagen;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.init.MobExItems;
import net.mcskill.mobexpansion.registry.BlockHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.Objects;

public class ItemsModelProvider extends ItemModelProvider {
    public ItemsModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Core.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        spawnEggItem(MobExItems.REGULAR_SPIDER_SPAWN_EGG.asItem());
        spawnEggItem(MobExItems.POISON_SPIDER_SPAWN_EGG.asItem());
        spawnEggItem(MobExItems.SPIDER_SPAWN_SPAWN_EGG.asItem());
        spawnEggItem(MobExItems.REDSTONE_ENGINEER_SPAWN_EGG.asItem());
        spawnEggItem(MobExItems.REDSTONE_AUTOMATON_SPAWN_EGG.asItem());
        spawnEggItem(MobExItems.REDSTONE_DRONE_SPAWN_EGG.asItem());
        spawnEggItem(MobExItems.REDSTONE_TOWER_SPAWN_EGG.asItem());
        spawnEggItem(MobExItems.SENTINEL_STATUE_SPAWN_EGG.asItem());
        spawnEggItem(MobExItems.SENTINEL_GOLEM_SPAWN_EGG.asItem());
        spawnEggItem(MobExItems.RAT_SPAWN_EGG.asItem());
        spawnEggItem(MobExItems.MOSQUITO_SPAWN_EGG.asItem());
        spawnEggItem(MobExItems.LEECH_SPAWN_EGG.asItem());
        spawnEggItem(MobExItems.RAT_KING_SPAWN_EGG.asItem());
    }

    public ItemModelBuilder basicBlock(Item item) {
        return basicBlock(Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(item)));
    }

    public ItemModelBuilder basicBlock(ResourceLocation item) {
        return getBuilder(item.toString()).parent(new ModelFile.UncheckedModelFile(ResourceLocation.fromNamespaceAndPath(item.getNamespace(), "block/" + item.getPath())));
    }

    public ItemModelBuilder simpleTexture(BlockHolder<? extends Block> block) {
        return withExistingParent(BuiltInRegistries.BLOCK.getKey(block.get()).getPath(),
                mcLoc("item/generated"))
                .texture("layer0", modLoc("block/" + BuiltInRegistries.BLOCK.getKey(block.get()).getPath()));
    }
}

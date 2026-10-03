package net.mcskill.mobexpansion.datagen;

import net.mcskill.mobexpansion.init.MobExEntities;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;

public class EntityLootProvider extends EntityLootSubProvider {
    protected EntityLootProvider(HolderLookup.Provider registries) {
        super(FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    public void generate() {
        getKnownEntityTypes().filter(this::canHaveLootTable).forEach(entityType -> add(entityType, LootTable.lootTable()));
    }

    @Override
    @NotNull
    protected Stream<EntityType<?>> getKnownEntityTypes() {
        return MobExEntities.REGISTRY.getEntries().stream().map(DeferredHolder::get);
    }
}

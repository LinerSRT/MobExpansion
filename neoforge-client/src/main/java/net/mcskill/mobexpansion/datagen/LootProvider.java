package net.mcskill.mobexpansion.datagen;

import net.mcskill.mobexpansion.init.MobExBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class LootProvider extends BlockLootSubProvider {
    protected LootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        dropSelf(MobExBlocks.MOB_SPAWNER.get());
    }

    @Override
    @NotNull
    protected Iterable<Block> getKnownBlocks() {
        return MobExBlocks.BLOCK_REGISTRY.getEntries().stream()
                .map(registryObject -> (Block) registryObject.get())
                .toList();
    }
}

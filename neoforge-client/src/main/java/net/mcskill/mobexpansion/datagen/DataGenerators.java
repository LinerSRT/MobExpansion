package net.mcskill.mobexpansion.datagen;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.init.MobExBlocks;
import net.mcskill.mobexpansion.init.MobExItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("deprecation")
@EventBusSubscriber(modid = Core.MODID)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        generator.addProvider(event.includeServer(), new DatapackBuiltinEntriesProvider(output, lookupProvider, createRegistrySetBuilder(), Set.of("minecraft", Core.MODID)));
        generator.addProvider(event.includeServer(), new GenericTagProvider<>(output, Registries.BLOCK, b -> b.builtInRegistryHolder().key(), lookupProvider, existingFileHelper, MobExBlocks.BLOCK_REGISTRY.getBlocks()));
        generator.addProvider(event.includeServer(), new GenericTagProvider<>(output, Registries.ITEM, b -> b.builtInRegistryHolder().key(), lookupProvider, existingFileHelper, MobExItems.ITEM_REGISTRY.getItems()));
        generator.addProvider(event.includeClient(), new LanguageProvider(output, Core.MODID, "en_us", "ru_ru"));
        generator.addProvider(event.includeClient(), new ParticleProvider(output, existingFileHelper));
        generator.addProvider(event.includeClient(), new ItemsModelProvider(output, existingFileHelper));
        generator.addProvider(event.includeClient(), new BlockStateProvider(output, existingFileHelper));
        generator.addProvider(event.includeServer(), new SoundsProvider(output, existingFileHelper));
        generator.addProvider(event.includeServer(), new LootTableProvider(
                output,
                Collections.emptySet(),
                List.of(
                        new LootTableProvider.SubProviderEntry(LootProvider::new, LootContextParamSets.BLOCK),
                        new LootTableProvider.SubProviderEntry(EntityLootProvider::new, LootContextParamSets.ENTITY)
                ),
                lookupProvider
        ));
    }

    private static RegistrySetBuilder createRegistrySetBuilder() {
        return new RegistrySetBuilder();
    }
}
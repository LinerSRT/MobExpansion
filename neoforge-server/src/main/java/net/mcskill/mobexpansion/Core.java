package net.mcskill.mobexpansion;

import net.mcskill.mobexpansion.attributes.VanillaAttributeLimits;
import net.mcskill.mobexpansion.drops.LootJsLootApplier;
import net.mcskill.mobexpansion.init.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(Core.MODID)
public class Core {
    public static final String MODID = "mobexpansion";

    public Core(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        MobExSounds.register(modEventBus);
        MobExMemoryTypes.register(modEventBus);
        MobExEntities.register(modEventBus);
        MobExBlocks.register(modEventBus);
        MobExBlockEntities.register(modEventBus);
        MobExMenus.register(modEventBus);
        MobExItems.register(modEventBus);
        MobExCreativeTabs.register(modEventBus);
        MobExParticles.register(modEventBus);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            VanillaAttributeLimits.expand();
            if (ModList.get().isLoaded("lootjs"))
                LootJsLootApplier.register();
        });
    }

    public static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}

package net.mcskill.mobexpansion.init;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.block.MobSpawnerBlock;
import net.mcskill.mobexpansion.registry.BlockHolder;
import net.mcskill.mobexpansion.registry.BlockRegistry;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;

public class MobExBlocks {
    public static final BlockRegistry BLOCK_REGISTRY = new BlockRegistry(Core.MODID);

    public static final BlockHolder<MobSpawnerBlock> MOB_SPAWNER = BLOCK_REGISTRY.registerBlock(
            "mob_spawner",
            MobSpawnerBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .dynamicShape()
    );

    public static void register(IEventBus modEventBus) {
        BLOCK_REGISTRY.register(modEventBus);
    }
}

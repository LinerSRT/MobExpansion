package net.mcskill.mobexpansion.init;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.blockentity.MobSpawnerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MobExBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> REGISTRY = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Core.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MobSpawnerBlockEntity>> MOB_SPAWNER = REGISTRY.register("mob_spawner", () -> BlockEntityType.Builder.of(MobSpawnerBlockEntity::new, MobExBlocks.MOB_SPAWNER.get()).build(null));

    public static void register(IEventBus modEventBus) {
        REGISTRY.register(modEventBus);
    }
}

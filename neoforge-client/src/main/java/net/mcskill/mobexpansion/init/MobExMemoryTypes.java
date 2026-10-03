package net.mcskill.mobexpansion.init;

import net.mcskill.mobexpansion.Core;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MobExMemoryTypes {
    public static final DeferredRegister<MemoryModuleType<?>> MEMORY_REGISTRY = DeferredRegister.create(BuiltInRegistries.MEMORY_MODULE_TYPE, Core.MODID);


    public static void register(IEventBus eventBus) {
        MEMORY_REGISTRY.register(eventBus);
    }
}

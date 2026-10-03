package net.mcskill.mobexpansion.init;

import net.mcskill.mobexpansion.Core;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MobExSounds {
    public static final DeferredRegister<SoundEvent> SOUND_REGISTRY = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Core.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> SENTINEL_GM1 = register("sentinel.gm1");
    public static final DeferredHolder<SoundEvent, SoundEvent> SENTINEL_GM2 = register("sentinel.gm2");
    public static final DeferredHolder<SoundEvent, SoundEvent> SENTINEL_GM3 = register("sentinel.gm3");
    public static final DeferredHolder<SoundEvent, SoundEvent> SENTINEL_GM4 = register("sentinel.gm4");
    public static final DeferredHolder<SoundEvent, SoundEvent> SENTINEL_GM5 = register("sentinel.gm5");
    public static final DeferredHolder<SoundEvent, SoundEvent> SENTINEL_GM6 = register("sentinel.gm6");
    public static final DeferredHolder<SoundEvent, SoundEvent> SENTINEL_GM7 = register("sentinel.gm7");

    private static DeferredHolder<SoundEvent, SoundEvent> register(ResourceLocation name, ResourceLocation location) {
        return SOUND_REGISTRY.register(name.getPath(), () -> SoundEvent.createVariableRangeEvent(location));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(ResourceLocation name) {
        return register(name, name);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return register(Core.loc(name));
    }

    public static void register(IEventBus eventBus) {
        SOUND_REGISTRY.register(eventBus);
    }
}

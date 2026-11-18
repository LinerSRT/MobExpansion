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

    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_BASIC_ATTACK1 = register("apocalypse.basic_attack1");
    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_BASIC_ATTACK2 = register("apocalypse.basic_attack2");
    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_BEAM = register("apocalypse.beam");
    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_BLACK_HOLE = register("apocalypse.black_hole");
    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_BLACK_STAR = register("apocalypse.black_star");
    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_BLACKBOX_BUILD = register("apocalypse.blackbox_build");
    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_BUILDINGS_COLLAPSE = register("apocalypse.buildings_collapse");
    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_COMBO1 = register("apocalypse.combo1");
    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_COMBO2 = register("apocalypse.combo2");
    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_COMBO3 = register("apocalypse.combo3");
    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_COMBO_X = register("apocalypse.combo_x");
    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_GRAB = register("apocalypse.grab");
    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_SWORDS_WHOOSH = register("apocalypse.swords_whoosh");
    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_TELEPORT = register("apocalypse.teleport");
    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_THROW = register("apocalypse.throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_UPPERCUT = register("apocalypse.uppercut");
    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_VANISH = register("apocalypse.vanish");
    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_VOID_STORM = register("apocalypse.void_storm");
    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_WHITE_VOID = register("apocalypse.white_void");

    public static final DeferredHolder<SoundEvent, SoundEvent> AWAKENED_WARRIOR_SLASH1 = register("awakened_warrior.slash1");
    public static final DeferredHolder<SoundEvent, SoundEvent> AWAKENED_WARRIOR_SLASH2 = register("awakened_warrior.slash2");
    public static final DeferredHolder<SoundEvent, SoundEvent> AWAKENED_WARRIOR_PIERCE = register("awakened_warrior.pierce");
    public static final DeferredHolder<SoundEvent, SoundEvent> AWAKENED_WARRIOR_AIRDASH = register("awakened_warrior.airdash");
    public static final DeferredHolder<SoundEvent, SoundEvent> AWAKENED_WARRIOR_CHARGE = register("awakened_warrior.charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> AWAKENED_WARRIOR_STOMP = register("awakened_warrior.stomp");
    public static final DeferredHolder<SoundEvent, SoundEvent> AWAKENED_WARRIOR_STOMP_CHARGE = register("awakened_warrior.stomp_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> AWAKENED_WARRIOR_STOMP_ULT = register("awakened_warrior.stomp_ult");
    public static final DeferredHolder<SoundEvent, SoundEvent> AWAKENED_WARRIOR_SPIN = register("awakened_warrior.spin");
    public static final DeferredHolder<SoundEvent, SoundEvent> AWAKENED_WARRIOR_IMPACT = register("awakened_warrior.impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> AWAKENED_WARRIOR_RUPTURE = register("awakened_warrior.rupture");
    public static final DeferredHolder<SoundEvent, SoundEvent> AWAKENED_WARRIOR_ULT1 = register("awakened_warrior.ult1");
    public static final DeferredHolder<SoundEvent, SoundEvent> AWAKENED_WARRIOR_ULT2 = register("awakened_warrior.ult2");
    public static final DeferredHolder<SoundEvent, SoundEvent> AWAKENED_WARRIOR_ULT3 = register("awakened_warrior.ult3");

    public static final DeferredHolder<SoundEvent, SoundEvent> RATS_SWORD = register("rats.sword");
    public static final DeferredHolder<SoundEvent, SoundEvent> RATS_CLOTH = register("rats.cloth");
    public static final DeferredHolder<SoundEvent, SoundEvent> RATS_ACTIVE = register("rats.active");
    public static final DeferredHolder<SoundEvent, SoundEvent> RATS_WOOSH = register("rats.woosh");

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

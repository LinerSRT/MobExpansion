package net.mcskill.mobexpansion.init;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.menu.MobDropConfigMenu;
import net.mcskill.mobexpansion.menu.MobSpawnerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MobExMenus {
    public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(Registries.MENU, Core.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<MobSpawnerMenu>> MOB_SPAWNER = REGISTRY.register("mob_spawner", () -> IMenuTypeExtension.create(MobSpawnerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<MobDropConfigMenu>> MOB_DROP_CONFIG = REGISTRY.register("mob_drop_config", () -> IMenuTypeExtension.create(MobDropConfigMenu::new));

    public static void register(IEventBus modEventBus) {
        REGISTRY.register(modEventBus);
    }
}

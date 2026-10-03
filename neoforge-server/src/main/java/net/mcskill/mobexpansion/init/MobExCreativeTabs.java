package net.mcskill.mobexpansion.init;

import net.mcskill.mobexpansion.Core;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MobExCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, Core.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + Core.MODID + ".main"))
            .icon(() -> new ItemStack(MobExBlocks.MOB_SPAWNER.get()))
            .displayItems((parameters, output) -> MobExItems.ITEM_REGISTRY.getEntries().forEach(entry -> output.accept(entry.get())))
            .build());

    public static void register(IEventBus bus) {
        CREATIVE_TABS.register(bus);
    }
}

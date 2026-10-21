package net.mcskill.mobexpansion.client;

import net.mcskill.mobexpansion.Core;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Esc → Настройки → Внешний вид: добавляет переключатель эффекта удара скина от первого лица. */
@EventBusSubscriber(modid = Core.MODID, value = Dist.CLIENT)
public final class SkinOptionsHook {
    private SkinOptionsHook() {
    }

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof SkinCustomizationScreen screen))
            return;
        for (var child : screen.children()) {
            if (child instanceof OptionsList list) {
                list.addSmall(OptionInstance.createBoolean("config.mobexpansion.hide_skin_attack_fx_first_person",
                        MobExClientConfig.HIDE_SKIN_ATTACK_FX_FIRST_PERSON.get(), value -> {
                            MobExClientConfig.HIDE_SKIN_ATTACK_FX_FIRST_PERSON.set(value);
                            MobExClientConfig.HIDE_SKIN_ATTACK_FX_FIRST_PERSON.save();
                        }));
                return;
            }
        }
    }
}

package net.mcskill.mobexpansion.client;

import net.mcskill.mobexpansion.Core;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Клиентская точка входа: регистрирует клиентский конфиг и его экран. */
@Mod(value = Core.MODID, dist = Dist.CLIENT)
public final class MobExClientMod {
    public MobExClientMod(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, MobExClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}

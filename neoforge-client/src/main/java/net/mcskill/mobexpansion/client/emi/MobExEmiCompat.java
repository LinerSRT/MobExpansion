package net.mcskill.mobexpansion.client.emi;

import dev.emi.emi.runtime.EmiReloadManager;

/**
 * Обращения к EMI вне плагина. Вызывать только при загруженном EMI ({@code ModList.isLoaded("emi")}).
 */
public final class MobExEmiCompat {
    private MobExEmiCompat() {
    }

    /** Пришли новые данные мобов — пересобрать рецепты EMI (публичного API для этого нет). */
    public static void reload() {
        try {
            EmiReloadManager.reload();
        } catch (Throwable throwable) {
            if (throwable instanceof VirtualMachineError error)
                throw error;
        }
    }
}

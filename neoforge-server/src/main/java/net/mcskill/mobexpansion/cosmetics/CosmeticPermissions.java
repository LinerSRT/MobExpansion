package net.mcskill.mobexpansion.cosmetics;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/**
 * Права на косметику: операторам (уровень 2) открыто всё, остальным — по правам LuckPerms.
 * Узлы: {@code cosmetics.skin.<id>} (работают и шаблоны LuckPerms: {@code cosmetics.skin.*}, {@code cosmetics.*}).
 * Без LuckPerms (одиночная игра, тестовый сервер) у не-операторов всё закрыто.
 */
public final class CosmeticPermissions {
    public static final String SKIN_PREFIX = "cosmetics.skin.";

    private CosmeticPermissions() {
    }

    public static boolean canUseSkin(ServerPlayer player, String skin) {
        return has(player, SKIN_PREFIX + skin);
    }

    public static boolean has(ServerPlayer player, String node) {
        if (player.hasPermissions(2))
            return true;
        return isLuckPermsLoaded() && LuckPermsBridge.has(player, node);
    }

    public static boolean isLuckPermsLoaded() {
        return ModList.get().isLoaded("luckperms");
    }
}

package net.mcskill.mobexpansion.cosmetics;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.minecraft.server.level.ServerPlayer;

/**
 * Обращения к API LuckPerms. Загружать класс только если мод luckperms есть (см. {@link CosmeticPermissions}),
 * иначе — NoClassDefFoundError.
 */
final class LuckPermsBridge {
    private LuckPermsBridge() {
    }

    static boolean has(ServerPlayer player, String node) {
        try {
            final User user = LuckPermsProvider.get().getUserManager().getUser(player.getUUID());
            return user != null && user.getCachedData().getPermissionData().checkPermission(node).asBoolean();
        } catch (IllegalStateException notLoaded) {
            return false;
        }
    }
}

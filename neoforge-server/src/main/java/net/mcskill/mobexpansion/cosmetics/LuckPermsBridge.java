package net.mcskill.mobexpansion.cosmetics;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

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

    /** Есть ли у игрока личная нода префикса с таким текстом и приоритетом. */
    static boolean hasPrefix(ServerPlayer player, String text, int priority) {
        try {
            final User user = LuckPermsProvider.get().getUserManager().getUser(player.getUUID());
            return user != null && user.getNodes(NodeType.PREFIX).stream()
                    .anyMatch(node -> node.getPriority() == priority && node.getMetaValue().equals(text));
        } catch (IllegalStateException notLoaded) {
            return false;
        }
    }

    /**
     * Оставить у игрока только этот префикс ({@code text == null} — убрать все из {@code managed}).
     * Трогает лишь ноды с текстом из {@code managed} и приоритетом {@code priority}, остальные префиксы не затрагиваются.
     */
    static CompletableFuture<Void> setPrefix(ServerPlayer player, Collection<String> managed, int priority, @Nullable String text) {
        try {
            return LuckPermsProvider.get().getUserManager().modifyUser(player.getUUID(), user -> {
                user.data().clear(node -> node instanceof PrefixNode prefix && prefix.getPriority() == priority
                        && managed.contains(prefix.getMetaValue()));
                if (text != null)
                    user.data().add(PrefixNode.builder(text, priority).build());
            });
        } catch (IllegalStateException notLoaded) {
            return CompletableFuture.completedFuture(null);
        }
    }
}

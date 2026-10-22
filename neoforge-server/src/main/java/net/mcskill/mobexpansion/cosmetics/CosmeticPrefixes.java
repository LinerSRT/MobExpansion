package net.mcskill.mobexpansion.cosmetics;

import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Префиксы чата/таба через LuckPerms. Право на префикс — нода {@code cosmetics.prefix.<id>}
 * (шаблоны LuckPerms {@code cosmetics.prefix.*}, {@code cosmetics.*} работают), группу с префиксом выдавать не нужно.
 * Включён ли префикс, определяет личная нода {@code prefix.<приоритет>.<текст>} у игрока: GUI добавляет и снимает её.
 * Картинка в тексте — эмодзи Emojiful ({@code :chaos:}, см. crafts/emojiful.js).
 * Без LuckPerms префиксов нет. Новый префикс — запись в {@link #ALL}, перевод {@code cosmetics.mobexpansion.prefix.<id>}
 * и (по желанию) иконка {@code textures/gui/prefix/<id>.png}.
 */
public final class CosmeticPrefixes {
    public static final String PERMISSION_PREFIX = "cosmetics.prefix.";
    /** Тот же приоритет, что у префиксов групп: личная нода перекрывает групповую с тем же текстом. */
    public static final int PRIORITY = 5;

    public record Definition(String id, String text) {
    }

    public static final List<Definition> ALL = List.of(
            chaos("chaos", ":chaos:"),
            chaos("chaos2", ":chaos2:"),
            chaos("chaos3", ":chaos3:")
    );

    /** Хаос разных уровней: отличается только эмодзи (по бокам от слова). */
    private static Definition chaos(String id, String emoji) {
        return new Definition(id, "&8[" + emoji + "<hover:show_text:'<red>Получено за прохождение ветки квестов'>"
                + "<gradient:#7e1294:#420469>Хаос</gradient></hover>" + emoji + "&8] &7");
    }

    private static final List<String> TEXTS = ALL.stream().map(Definition::text).toList();

    private CosmeticPrefixes() {
    }

    public static boolean available() {
        return CosmeticPermissions.isLuckPermsLoaded();
    }

    public static Definition get(String id) {
        for (Definition definition : ALL)
            if (definition.id().equals(id))
                return definition;
        return null;
    }

    public static boolean canUse(ServerPlayer player, String id) {
        return CosmeticPermissions.has(player, PERMISSION_PREFIX + id);
    }

    /** id включённого префикса, пустая строка — нет. Префикс без права (право отозвали) снимается. */
    public static String current(ServerPlayer player) {
        if (!available())
            return "";
        for (Definition definition : ALL) {
            if (!LuckPermsBridge.hasPrefix(player, definition.text(), PRIORITY))
                continue;
            if (canUse(player, definition.id()))
                return definition.id();
            LuckPermsBridge.setPrefix(player, TEXTS, PRIORITY, null);
        }
        return "";
    }

    /** Включить префикс ({@code definition == null} — выключить). */
    public static CompletableFuture<Void> set(ServerPlayer player, Definition definition) {
        if (!available())
            return CompletableFuture.completedFuture(null);
        return LuckPermsBridge.setPrefix(player, TEXTS, PRIORITY, definition == null ? null : definition.text());
    }
}

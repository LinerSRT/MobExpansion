package net.mcskill.mobexpansion.cosmetics;

import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Значки после ника в чате/табе через LuckPerms (суффикс; в GUI и коде исторически «префиксы»).
 * Право на значок — нода {@code cosmetics.prefix.<id>} (шаблоны LuckPerms {@code cosmetics.prefix.*}, {@code cosmetics.*}
 * работают), группу выдавать не нужно. Включён ли значок, определяет личная нода {@code suffix.<приоритет>.<текст>} у игрока:
 * GUI добавляет и снимает её. Картинка — эмодзи Emojiful ({@code :chaos:}, см. crafts/emojiful.js).
 * Без LuckPerms значков нет. Новый значок — запись в {@link #ALL}, перевод {@code cosmetics.mobexpansion.prefix.<id>}
 * и (по желанию) иконка {@code textures/gui/prefix/<id>.png}.
 * <p>
 * Хаос выдаёт MSEss ({@code /chaos <ник>}): повторная выдача поднимает уровень — право старого уровня забирается,
 * а включённый значок при входе/открытии GUI сам меняется на новый (см. {@link #current}).
 */
public final class CosmeticPrefixes {
    public static final String PERMISSION_PREFIX = "cosmetics.prefix.";
    public static final int PRIORITY = 5;

    public record Definition(String id, String text) {
    }

    public static final List<Definition> ALL = List.of(
            chaos("chaos", ":chaos:"),
            chaos("chaos2", ":chaos2:"),
            chaos("chaos3", ":chaos3:")
    );

    /** Хаос разных уровней: только эмодзи после ника, с подсказкой при наведении. */
    private static Definition chaos(String id, String emoji) {
        return new Definition(id, " <hover:show_text:'<red>Получено за прохождение ветки квестов'>" + emoji + "</hover>");
    }

    private static final List<String> TEXTS = ALL.stream().map(Definition::text).toList();

    /** Префиксы прошлой версии ({@code [значок Хаос значок]} перед ником) — снимаются и меняются на суффикс. */
    private static final List<String> LEGACY_PREFIX_TEXTS = List.of(legacyChaos(":chaos:"), legacyChaos(":chaos2:"), legacyChaos(":chaos3:"));

    private static String legacyChaos(String emoji) {
        return "&8[" + emoji + "<hover:show_text:'<red>Получено за прохождение ветки квестов'>"
                + "<gradient:#7e1294:#420469>Хаос</gradient></hover>" + emoji + "&8] &7";
    }

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

    /**
     * id включённого значка, пустая строка — нет. Значок без права (уровень подняли или право отозвали) меняется
     * на старший доступный, а если доступных нет — снимается. Старый префикс-хаос переводится в суффикс.
     */
    public static String current(ServerPlayer player) {
        if (!available())
            return "";
        boolean enabled = LuckPermsBridge.hasAnyPrefix(player, LEGACY_PREFIX_TEXTS, PRIORITY);
        for (Definition definition : ALL) {
            if (!LuckPermsBridge.hasSuffix(player, definition.text(), PRIORITY))
                continue;
            if (!enabled && canUse(player, definition.id()))
                return definition.id();
            enabled = true;
        }
        if (!enabled)
            return "";
        final Definition replacement = best(player);
        set(player, replacement);
        return replacement == null ? "" : replacement.id();
    }

    /** Старший доступный игроку значок (уровни хаоса идут в {@link #ALL} по возрастанию). */
    private static Definition best(ServerPlayer player) {
        for (int i = ALL.size() - 1; i >= 0; i--)
            if (canUse(player, ALL.get(i).id()))
                return ALL.get(i);
        return null;
    }

    /** Включить значок ({@code definition == null} — выключить). */
    public static CompletableFuture<Void> set(ServerPlayer player, Definition definition) {
        if (!available())
            return CompletableFuture.completedFuture(null);
        return LuckPermsBridge.setSuffix(player, TEXTS, LEGACY_PREFIX_TEXTS, PRIORITY, definition == null ? null : definition.text());
    }
}

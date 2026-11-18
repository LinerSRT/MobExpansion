package net.mcskill.mobexpansion.cosmetics;

import net.neoforged.fml.ModList;

/**
 * Есть ли GUI кастомизации MobExpansion (клавиша H, команда /cosmetics). На бункере (стоит MsBunker) — нет:
 * скины и шлейфы там во вкладке «Кастомизация» меню игрока (P), пакеты и их серверная логика — общие.
 * Одинаковая копия в обоих подпроектах.
 */
public final class CosmeticsGui {
    public static final String BUNKER_MOD = "msbunker";
    private static Boolean enabled;

    private CosmeticsGui() {
    }

    public static boolean enabled() {
        Boolean value = enabled;
        if (value == null)
            enabled = value = !ModList.get().isLoaded(BUNKER_MOD);
        return value;
    }
}

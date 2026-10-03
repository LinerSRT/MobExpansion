package net.mcskill.mobexpansion.drops;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;


public record LootConditionEntry(
        LootConditionType type,
        String stringValue,
        int intMin,
        int intMax,
        int intExtra,
        byte flagA,
        byte flagB
) {
    public static final byte TRI_ANY = -1;
    public static final byte TRI_FALSE = 0;
    public static final byte TRI_TRUE = 1;

    public LootConditionEntry {
        type = type == null ? LootConditionType.KILLED_BY_PLAYER : type;
        stringValue = stringValue == null ? "" : stringValue.trim();
        intMin = Math.max(0, intMin);
        intMax = Math.max(intMin, intMax);
        intExtra = Math.max(0, intExtra);
        flagA = clampTri(flagA);
        flagB = clampTri(flagB);
    }

    public static LootConditionEntry createDefault() {
        return killedByPlayer();
    }

    public static LootConditionEntry forType(LootConditionType conditionType) {
        return switch (conditionType) {
            case KILLED_BY_PLAYER -> killedByPlayer();
            case MATCH_BIOME -> matchBiome("minecraft:plains");
            case MATCH_DIMENSION -> matchDimension("minecraft:overworld");
            case MATCH_STRUCTURE -> matchStructure("minecraft:village", false);
            case MATCH_WEATHER -> matchWeather(TRI_TRUE, TRI_ANY);
            case MATCH_TIME -> matchTime(0, 12000, 24000);
            case IS_LIGHT_LEVEL -> isLightLevel(0, 7);
            case SURVIVES_EXPLOSION -> survivesExplosion();
            case MATCH_MAIN_HAND -> matchMainHand("#minecraft:swords");
            case MATCH_OFF_HAND -> matchOffHand("minecraft:shield");
        };
    }

    public static LootConditionType nextType(LootConditionType currentType) {
        final LootConditionType[] values = LootConditionType.values();
        return values[(currentType.ordinal() + 1) % values.length];
    }

    public static byte nextTri(byte flag) {
        if (flag == TRI_ANY)
            return TRI_FALSE;
        if (flag == TRI_FALSE)
            return TRI_TRUE;
        return TRI_ANY;
    }

    public static LootConditionEntry killedByPlayer() {
        return new LootConditionEntry(LootConditionType.KILLED_BY_PLAYER, "", 0, 0, 0, TRI_ANY, TRI_ANY);
    }

    public static LootConditionEntry matchBiome(String biomeId) {
        return new LootConditionEntry(LootConditionType.MATCH_BIOME, biomeId == null || biomeId.isEmpty() ? "minecraft:plains" : biomeId, 0, 0, 0, TRI_ANY, TRI_ANY);
    }

    public static LootConditionEntry matchDimension(String dimensionId) {
        return new LootConditionEntry(LootConditionType.MATCH_DIMENSION, dimensionId == null || dimensionId.isEmpty() ? "minecraft:overworld" : dimensionId, 0, 0, 0, TRI_ANY, TRI_ANY);
    }

    public static LootConditionEntry matchStructure(String structureId, boolean exact) {
        return new LootConditionEntry(LootConditionType.MATCH_STRUCTURE, structureId == null || structureId.isEmpty() ? "minecraft:village" : structureId, 0, 0, 0, exact ? TRI_TRUE : TRI_FALSE, TRI_ANY);
    }

    public static LootConditionEntry matchWeather(Byte raining, Byte thundering) {
        return new LootConditionEntry(LootConditionType.MATCH_WEATHER, "", 0, 0, 0, raining == null ? TRI_ANY : clampTri(raining), thundering == null ? TRI_ANY : clampTri(thundering));
    }

    public static LootConditionEntry matchTime(int minTime, int maxTime, int period) {
        return new LootConditionEntry(LootConditionType.MATCH_TIME, "", Math.max(0, minTime), Math.max(minTime, maxTime), period <= 0 ? 24000 : period, TRI_ANY, TRI_ANY);
    }

    public static LootConditionEntry isLightLevel(int minLevel, int maxLevel) {
        return new LootConditionEntry(LootConditionType.IS_LIGHT_LEVEL, "", Mth.clamp(minLevel, 0, 15), Mth.clamp(Math.max(minLevel, maxLevel), 0, 15), 0, TRI_ANY, TRI_ANY);
    }

    public static LootConditionEntry survivesExplosion() {
        return new LootConditionEntry(LootConditionType.SURVIVES_EXPLOSION, "", 0, 0, 0, TRI_ANY, TRI_ANY);
    }

    public static LootConditionEntry matchMainHand(String itemFilter) {
        return new LootConditionEntry(LootConditionType.MATCH_MAIN_HAND, itemFilter == null || itemFilter.isEmpty() ? "#minecraft:swords" : itemFilter, 0, 0, 0, TRI_ANY, TRI_ANY);
    }

    public static LootConditionEntry matchOffHand(String itemFilter) {
        return new LootConditionEntry(LootConditionType.MATCH_OFF_HAND, itemFilter == null || itemFilter.isEmpty() ? "minecraft:shield" : itemFilter, 0, 0, 0, TRI_ANY, TRI_ANY);
    }

    public LootConditionEntry withStringValue(String value) {
        return new LootConditionEntry(type, value, intMin, intMax, intExtra, flagA, flagB);
    }

    public LootConditionEntry withRange(int minValue, int maxValue) {
        return new LootConditionEntry(type, stringValue, minValue, maxValue, intExtra, flagA, flagB);
    }

    public LootConditionEntry withExtra(int extraValue) {
        return new LootConditionEntry(type, stringValue, intMin, intMax, extraValue, flagA, flagB);
    }

    public LootConditionEntry withFlags(byte firstFlag, byte secondFlag) {
        return new LootConditionEntry(type, stringValue, intMin, intMax, intExtra, firstFlag, secondFlag);
    }

    public Component displayLabel() {
        return switch (type) {
            case KILLED_BY_PLAYER -> Component.translatable("gui.mobexpansion.condition.killed_by_player");
            case MATCH_BIOME -> Component.translatable("gui.mobexpansion.condition.match_biome", stringValue);
            case MATCH_DIMENSION -> Component.translatable("gui.mobexpansion.condition.match_dimension", stringValue);
            case MATCH_STRUCTURE -> Component.translatable("gui.mobexpansion.condition.match_structure", stringValue, flagA == TRI_TRUE);
            case MATCH_WEATHER -> Component.translatable("gui.mobexpansion.condition.match_weather", triLabel(flagA), triLabel(flagB));
            case MATCH_TIME -> Component.translatable("gui.mobexpansion.condition.match_time", intMin, intMax, intExtra);
            case IS_LIGHT_LEVEL -> Component.translatable("gui.mobexpansion.condition.is_light_level", intMin, intMax);
            case SURVIVES_EXPLOSION -> Component.translatable("gui.mobexpansion.condition.survives_explosion");
            case MATCH_MAIN_HAND -> Component.translatable("gui.mobexpansion.condition.match_main_hand", stringValue);
            case MATCH_OFF_HAND -> Component.translatable("gui.mobexpansion.condition.match_off_hand", stringValue);
        };
    }

    private static String triLabel(byte flag) {
        if (flag == TRI_TRUE)
            return "yes";
        if (flag == TRI_FALSE)
            return "no";
        return "any";
    }

    private static byte clampTri(byte flag) {
        if (flag < TRI_ANY)
            return TRI_ANY;
        if (flag > TRI_TRUE)
            return TRI_TRUE;
        return flag;
    }
}

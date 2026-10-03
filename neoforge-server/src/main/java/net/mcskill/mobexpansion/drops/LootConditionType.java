package net.mcskill.mobexpansion.drops;

public enum LootConditionType {
    KILLED_BY_PLAYER,
    MATCH_BIOME,
    MATCH_DIMENSION,
    MATCH_STRUCTURE,
    MATCH_WEATHER,
    MATCH_TIME,
    IS_LIGHT_LEVEL,
    SURVIVES_EXPLOSION,
    MATCH_MAIN_HAND,
    MATCH_OFF_HAND;

    public static LootConditionType fromName(String name) {
        if (name == null || name.isEmpty())
            return KILLED_BY_PLAYER;
        try {
            return LootConditionType.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return KILLED_BY_PLAYER;
        }
    }
}

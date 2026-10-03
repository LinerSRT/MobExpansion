package net.mcskill.mobexpansion.drops;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class LootConditionJs {
    private static final Pattern RANDOM_CHANCE = Pattern.compile("\\.randomChance\\(([0-9.]+)\\)");
    private static final Pattern KILLED_BY_PLAYER = Pattern.compile("\\.killedByPlayer\\(\\)");
    private static final Pattern SURVIVES_EXPLOSION = Pattern.compile("\\.survivesExplosion\\(\\)");
    private static final Pattern MATCH_BIOME = Pattern.compile("\\.matchBiome\\(\"([^\"]+)\"\\)");
    private static final Pattern MATCH_DIMENSION = Pattern.compile("\\.matchDimension\\(\"([^\"]+)\"\\)");
    private static final Pattern MATCH_STRUCTURE = Pattern.compile("\\.matchStructure\\(\"([^\"]+)\"(?:,\\s*(true|false))?\\)");
    private static final Pattern MATCH_WEATHER = Pattern.compile("\\.matchWeather\\((true|false|null),\\s*(true|false|null)\\)");
    private static final Pattern MATCH_TIME_PERIOD = Pattern.compile("\\.matchTime\\((\\d+),\\s*(\\d+),\\s*(\\d+)\\)");
    private static final Pattern MATCH_TIME = Pattern.compile("\\.matchTime\\((\\d+),\\s*(\\d+)\\)");
    private static final Pattern IS_LIGHT_LEVEL = Pattern.compile("\\.isLightLevel\\((\\d+),\\s*(\\d+)\\)");
    private static final Pattern MATCH_MAIN_HAND = Pattern.compile("\\.matchMainHand\\(\"([^\"]+)\"\\)");
    private static final Pattern MATCH_OFF_HAND = Pattern.compile("\\.matchOffHand\\(\"([^\"]+)\"\\)");

    private LootConditionJs() {
    }

    public record ParsedWhen(float chance, List<LootConditionEntry> conditions) {
        public static ParsedWhen empty() {
            return new ParsedWhen(1.0F, List.of());
        }
    }

    public static ParsedWhen parseWhenBody(String whenBody) {
        if (whenBody == null || whenBody.isBlank())
            return ParsedWhen.empty();

        float chance = 1.0F;
        final Matcher chanceMatcher = RANDOM_CHANCE.matcher(whenBody);
        if (chanceMatcher.find())
            chance = Float.parseFloat(chanceMatcher.group(1));

        final List<LootConditionEntry> conditions = new ArrayList<>();
        if (KILLED_BY_PLAYER.matcher(whenBody).find())
            conditions.add(LootConditionEntry.killedByPlayer());
        if (SURVIVES_EXPLOSION.matcher(whenBody).find())
            conditions.add(LootConditionEntry.survivesExplosion());

        final Matcher biomeMatcher = MATCH_BIOME.matcher(whenBody);
        while (biomeMatcher.find())
            conditions.add(LootConditionEntry.matchBiome(biomeMatcher.group(1)));

        final Matcher dimensionMatcher = MATCH_DIMENSION.matcher(whenBody);
        while (dimensionMatcher.find())
            conditions.add(LootConditionEntry.matchDimension(dimensionMatcher.group(1)));

        final Matcher structureMatcher = MATCH_STRUCTURE.matcher(whenBody);
        while (structureMatcher.find())
            conditions.add(LootConditionEntry.matchStructure(structureMatcher.group(1), "true".equals(structureMatcher.group(2))));

        final Matcher weatherMatcher = MATCH_WEATHER.matcher(whenBody);
        while (weatherMatcher.find())
            conditions.add(LootConditionEntry.matchWeather(parseTri(weatherMatcher.group(1)), parseTri(weatherMatcher.group(2))));

        final Matcher timePeriodMatcher = MATCH_TIME_PERIOD.matcher(whenBody);
        boolean foundPeriodTime = false;
        while (timePeriodMatcher.find()) {
            foundPeriodTime = true;
            conditions.add(LootConditionEntry.matchTime(
                    Integer.parseInt(timePeriodMatcher.group(2)),
                    Integer.parseInt(timePeriodMatcher.group(3)),
                    Integer.parseInt(timePeriodMatcher.group(1))
            ));
        }
        if (!foundPeriodTime) {
            final Matcher timeMatcher = MATCH_TIME.matcher(whenBody);
            while (timeMatcher.find())
                conditions.add(LootConditionEntry.matchTime(Integer.parseInt(timeMatcher.group(1)), Integer.parseInt(timeMatcher.group(2)), 24000));
        }

        final Matcher lightMatcher = IS_LIGHT_LEVEL.matcher(whenBody);
        while (lightMatcher.find())
            conditions.add(LootConditionEntry.isLightLevel(Integer.parseInt(lightMatcher.group(1)), Integer.parseInt(lightMatcher.group(2))));

        final Matcher mainHandMatcher = MATCH_MAIN_HAND.matcher(whenBody);
        while (mainHandMatcher.find())
            conditions.add(LootConditionEntry.matchMainHand(mainHandMatcher.group(1)));

        final Matcher offHandMatcher = MATCH_OFF_HAND.matcher(whenBody);
        while (offHandMatcher.find())
            conditions.add(LootConditionEntry.matchOffHand(offHandMatcher.group(1)));

        return new ParsedWhen(chance, conditions);
    }


    public static String formatChance(float chance) {
        return String.format(Locale.ROOT, "%.4f", chance);
    }

    private static Byte parseTri(String text) {
        if ("true".equals(text))
            return LootConditionEntry.TRI_TRUE;
        if ("false".equals(text))
            return LootConditionEntry.TRI_FALSE;
        return LootConditionEntry.TRI_ANY;
    }
}

package net.mcskill.mobexpansion.drops;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One-shot import of the old generated KubeJS script into JSON, then the file is deleted.
 */
final class LegacyLootJsScript {
    private static final String SCRIPT_RELATIVE = "kubejs/server_scripts/mobs/mobexpansion_drops.js";

    private static final Pattern TABLE_ENTITY_BLOCK_PATTERN = Pattern.compile(
            "(?:// mobexpansion:entity (\\S+)|event\\.modifyEntityTables\\(\"([^\"]+)\"\\)|event\\.getEntityTable\\(\"([^\"]+)\"\\))([\\s\\S]*?)(?=\\n\\s*(?:// mobexpansion:entity |event\\.modifyEntityTables|event\\.getEntityTable)|\\n}\\);)"
    );
    private static final Pattern TABLE_POOL_ENTRY_PATTERN = Pattern.compile(
            "pool\\.addEntry\\(LootEntry\\.of\\(\"([^\"]+)\"(?:,\\s*\\[(\\d+),\\s*(\\d+)\\])?\\)"
                    + "(?:\\.when\\(c\\s*=>\\s*c((?:\\.\\w+\\([^)]*\\))+)\\))?"
                    + "\\)"
    );
    private static final Pattern MODIFIER_ENTITY_BLOCK_PATTERN = Pattern.compile(
            "event\\.addEntityModifier\\(\"([^\"]+)\"\\)([\\s\\S]*?);"
    );
    private static final Pattern LEGACY_LOOT_ENTRY_PATTERN = Pattern.compile(
            "\\.addLoot\\(LootEntry\\.of\\(\"([^\"]+)\"(?:,\\s*\\[(\\d+),\\s*(\\d+)\\])?\\)"
                    + "(?:\\.setCount\\(\\[(\\d+),\\s*(\\d+)\\]\\))?"
                    + "(?:\\.when\\(c\\s*=>\\s*c((?:\\.\\w+\\([^)]*\\))+)\\))?"
                    + "(?:\\.randomChance\\(([0-9.]+)\\))?"
                    + "\\)"
    );
    private static final Pattern DROP_EXPERIENCE_PATTERN = Pattern.compile(
            "\\.dropExperience\\((?:\\[(\\d+),\\s*(\\d+)\\]|(\\d+))\\)"
    );
    private static final Pattern GROUP_EXPERIENCE_PATTERN = Pattern.compile(
            "\\.group\\(\\s*m\\s*=>\\s*\\{([\\s\\S]*?)}\\s*\\)"
    );

    private LegacyLootJsScript() {
    }

    static Map<ResourceLocation, EntityLootConfig> importIfPresent() {
        final Path scriptFile = scriptPath();
        if (!Files.isRegularFile(scriptFile))
            return Collections.emptyMap();
        try {
            return parse(Files.readString(scriptFile, StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            return Collections.emptyMap();
        }
    }

    static void deleteIfPresent() {
        try {
            Files.deleteIfExists(scriptPath());
        } catch (IOException ignored) {
        }
    }

    private static Path scriptPath() {
        return FMLPaths.GAMEDIR.get().resolve(SCRIPT_RELATIVE);
    }

    private static Map<ResourceLocation, EntityLootConfig> parse(String scriptContent) {
        final Map<ResourceLocation, EntityLootConfig> configsByEntity = new LinkedHashMap<>();
        mergeTableDrops(configsByEntity, scriptContent);
        mergeModifierBlocks(configsByEntity, scriptContent);
        return configsByEntity;
    }

    private static void mergeTableDrops(Map<ResourceLocation, EntityLootConfig> configsByEntity, String scriptContent) {
        final Matcher entityMatcher = TABLE_ENTITY_BLOCK_PATTERN.matcher(scriptContent);
        while (entityMatcher.find()) {
            final String entityIdRaw = firstNonNull(
                    entityMatcher.group(1),
                    entityMatcher.group(2),
                    entityMatcher.group(3)
            );
            if (entityIdRaw == null)
                continue;
            final ResourceLocation entityId = ResourceLocation.tryParse(entityIdRaw);
            if (entityId == null)
                continue;
            final String tableBody = entityMatcher.group(4);
            final List<MobDropEntry> dropEntries = parseTablePoolEntries(tableBody);
            final boolean replaceVanilla = tableBody.contains(".clear()");
            if (dropEntries.isEmpty() && !replaceVanilla)
                continue;
            mergeConfig(configsByEntity, entityId, new EntityLootConfig(dropEntries, List.of(), replaceVanilla));
        }
    }

    private static void mergeModifierBlocks(Map<ResourceLocation, EntityLootConfig> configsByEntity, String scriptContent) {
        final Matcher entityMatcher = MODIFIER_ENTITY_BLOCK_PATTERN.matcher(scriptContent);
        while (entityMatcher.find()) {
            final ResourceLocation entityId = ResourceLocation.tryParse(entityMatcher.group(1));
            if (entityId == null)
                continue;
            final String modifierBody = entityMatcher.group(2);
            mergeConfig(configsByEntity, entityId, new EntityLootConfig(
                    parseLegacyLootEntries(modifierBody),
                    parseExperienceEntries(modifierBody)
            ));
        }
    }

    private static void mergeConfig(Map<ResourceLocation, EntityLootConfig> configsByEntity, ResourceLocation entityId, EntityLootConfig parsedConfig) {
        if (parsedConfig.isEmpty())
            return;
        final EntityLootConfig existingConfig = configsByEntity.get(entityId);
        if (existingConfig == null) {
            configsByEntity.put(entityId, parsedConfig);
            return;
        }
        final List<MobDropEntry> mergedDrops = new ArrayList<>(existingConfig.drops());
        mergedDrops.addAll(parsedConfig.drops());
        final List<MobExperienceEntry> mergedExperience = new ArrayList<>(existingConfig.experience());
        mergedExperience.addAll(parsedConfig.experience());
        configsByEntity.put(entityId, new EntityLootConfig(mergedDrops, mergedExperience, existingConfig.replaceVanilla() || parsedConfig.replaceVanilla()));
    }

    private static List<MobDropEntry> parseTablePoolEntries(String tableBody) {
        final List<MobDropEntry> dropEntries = new ArrayList<>();
        final Matcher lootMatcher = TABLE_POOL_ENTRY_PATTERN.matcher(tableBody);
        while (lootMatcher.find()) {
            final ResourceLocation itemId = ResourceLocation.tryParse(lootMatcher.group(1));
            if (itemId == null)
                continue;
            final int minCount = firstPresentInt(lootMatcher.group(2), null, 1);
            final int maxCount = firstPresentInt(lootMatcher.group(3), null, minCount);
            final LootConditionJs.ParsedWhen parsedWhen = LootConditionJs.parseWhenBody(lootMatcher.group(4));
            dropEntries.add(new MobDropEntry(itemId, minCount, maxCount, parsedWhen.chance(), parsedWhen.conditions()));
        }
        return dropEntries;
    }

    private static List<MobDropEntry> parseLegacyLootEntries(String modifierBody) {
        final List<MobDropEntry> dropEntries = new ArrayList<>();
        final Matcher lootMatcher = LEGACY_LOOT_ENTRY_PATTERN.matcher(modifierBody);
        while (lootMatcher.find()) {
            final ResourceLocation itemId = ResourceLocation.tryParse(lootMatcher.group(1));
            if (itemId == null)
                continue;
            final int minCount = firstPresentInt(lootMatcher.group(2), lootMatcher.group(4), 1);
            final int maxCount = firstPresentInt(lootMatcher.group(3), lootMatcher.group(5), minCount);
            final LootConditionJs.ParsedWhen parsedWhen = LootConditionJs.parseWhenBody(lootMatcher.group(6));
            float chance = parsedWhen.chance();
            if (lootMatcher.group(7) != null && !lootMatcher.group(7).isEmpty())
                chance = Float.parseFloat(lootMatcher.group(7));
            dropEntries.add(new MobDropEntry(itemId, minCount, maxCount, chance, parsedWhen.conditions()));
        }
        return dropEntries;
    }

    private static List<MobExperienceEntry> parseExperienceEntries(String modifierBody) {
        final List<MobExperienceEntry> experienceEntries = new ArrayList<>();
        final Matcher groupMatcher = GROUP_EXPERIENCE_PATTERN.matcher(modifierBody);
        int searchFrom = 0;
        while (groupMatcher.find(searchFrom)) {
            final String groupBody = groupMatcher.group(1);
            final Matcher experienceMatcher = DROP_EXPERIENCE_PATTERN.matcher(groupBody);
            if (!experienceMatcher.find()) {
                searchFrom = groupMatcher.end();
                continue;
            }
            final int minAmount = firstPresentInt(experienceMatcher.group(1), experienceMatcher.group(3), 0);
            final int maxAmount = firstPresentInt(experienceMatcher.group(2), experienceMatcher.group(3), minAmount);
            final LootConditionJs.ParsedWhen parsedWhen = parseGroupConditions(groupBody);
            experienceEntries.add(new MobExperienceEntry(minAmount, maxAmount, parsedWhen.chance(), parsedWhen.conditions()));
            searchFrom = groupMatcher.end();
        }

        final String bodyWithoutGroups = GROUP_EXPERIENCE_PATTERN.matcher(modifierBody).replaceAll("");
        final Matcher experienceMatcher = DROP_EXPERIENCE_PATTERN.matcher(bodyWithoutGroups);
        while (experienceMatcher.find()) {
            final int minAmount = firstPresentInt(experienceMatcher.group(1), experienceMatcher.group(3), 0);
            final int maxAmount = firstPresentInt(experienceMatcher.group(2), experienceMatcher.group(3), minAmount);
            final LootConditionJs.ParsedWhen parsedWhen = LootConditionJs.parseWhenBody(bodyWithoutGroups);
            experienceEntries.add(new MobExperienceEntry(minAmount, maxAmount, parsedWhen.chance(), parsedWhen.conditions()));
        }
        return experienceEntries;
    }

    private static LootConditionJs.ParsedWhen parseGroupConditions(String groupBody) {
        final Matcher legacyWhen = Pattern.compile("\\.when\\(c\\s*=>\\s*c((?:\\.\\w+\\([^)]*\\))+)\\)").matcher(groupBody);
        if (legacyWhen.find())
            return LootConditionJs.parseWhenBody(legacyWhen.group(1));

        final StringBuilder conditionChain = new StringBuilder();
        final Matcher addConditionMatcher = Pattern.compile("m\\.addCondition\\(LootCondition\\.(\\w+)\\(([^)]*)\\)\\)").matcher(groupBody);
        while (addConditionMatcher.find()) {
            final String methodName = addConditionMatcher.group(1);
            final String args = addConditionMatcher.group(2) == null ? "" : addConditionMatcher.group(2).trim();
            conditionChain.append(".").append(methodName).append("(").append(args).append(")");
        }
        return LootConditionJs.parseWhenBody(conditionChain.toString());
    }

    @Nullable
    private static String firstNonNull(@Nullable String first, @Nullable String second, @Nullable String third) {
        if (first != null && !first.isEmpty())
            return first;
        if (second != null && !second.isEmpty())
            return second;
        if (third != null && !third.isEmpty())
            return third;
        return null;
    }

    private static int firstPresentInt(@Nullable String firstValue, @Nullable String secondValue, int fallback) {
        if (firstValue != null && !firstValue.isEmpty())
            return Integer.parseInt(firstValue);
        if (secondValue != null && !secondValue.isEmpty())
            return Integer.parseInt(secondValue);
        return fallback;
    }
}

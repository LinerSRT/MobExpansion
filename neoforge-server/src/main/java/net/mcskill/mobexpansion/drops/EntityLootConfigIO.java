package net.mcskill.mobexpansion.drops;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EntityLootConfigIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String RELATIVE_PATH = "mobexpansion/entity_drops.json";

    private static final Logger LOGGER = LogUtils.getLogger();

    private static Map<ResourceLocation, EntityLootConfig> cached = Collections.emptyMap();
    private static boolean loaded;

    private EntityLootConfigIO() {
    }

    public static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(RELATIVE_PATH);
    }

    public static synchronized void reload() {
        final Path path = configPath();
        if (!Files.exists(path)) {
            // Импорт старого KubeJS-скрипта — только если JSON-конфига ещё нет совсем.
            // Раньше импорт срабатывал и при пустом/битом JSON и мог откатить дроп к старым значениям.
            Map<ResourceLocation, EntityLootConfig> imported = Collections.emptyMap();
            try {
                imported = LegacyLootJsScript.importIfPresent();
            } catch (RuntimeException | Error e) {
                LOGGER.error("[MobExpansion] Failed to import legacy loot script", e);
            }
            if (!imported.isEmpty()) {
                writeAll(imported);
                LOGGER.info("[MobExpansion] Imported {} entity drop configs from legacy KubeJS script", imported.size());
                return;
            }
            cached = Collections.emptyMap();
            loaded = true;
            return;
        }
        final Map<ResourceLocation, EntityLootConfig> fromDisk = loadFromDisk();
        if (fromDisk == null) {
            // Файл есть, но не читается (и .bak тоже) — оставляем то, что уже в памяти, и ничего не перезаписываем.
            LOGGER.error("[MobExpansion] {} is unreadable, keeping {} cached entity drop configs", path, cached.size());
            loaded = true;
            return;
        }
        cached = fromDisk;
        loaded = true;
        try {
            LegacyLootJsScript.deleteIfPresent();
        } catch (RuntimeException | Error ignored) {
        }
    }

    public static synchronized Map<ResourceLocation, EntityLootConfig> getCached() {
        if (!loaded)
            reload();
        return cached;
    }

    public static EntityLootConfig getCached(ResourceLocation entityId) {
        if (entityId == null)
            return EntityLootConfig.EMPTY;
        return getCached().getOrDefault(entityId, EntityLootConfig.EMPTY);
    }

    /**
     * Конфиги с диска; при битом основном файле — из резервной копии. {@code null} — прочитать не удалось.
     */
    @Nullable
    public static Map<ResourceLocation, EntityLootConfig> loadFromDisk() {
        final Path path = configPath();
        try {
            final JsonObject root = SafeJsonFile.read(path, GSON);
            return root == null ? Collections.emptyMap() : parseRoot(root);
        } catch (IOException e) {
            LOGGER.error("[MobExpansion] Failed to read {}: {}", path, e.getMessage());
        }
        final Path backup = SafeJsonFile.backupPath(path);
        try {
            final JsonObject root = SafeJsonFile.read(backup, GSON);
            if (root != null) {
                LOGGER.warn("[MobExpansion] Loaded entity drops from backup {}", backup);
                return parseRoot(root);
            }
        } catch (IOException e) {
            LOGGER.error("[MobExpansion] Failed to read {}: {}", backup, e.getMessage());
        }
        return null;
    }

    private static Map<ResourceLocation, EntityLootConfig> parseRoot(JsonObject root) {
        final Map<ResourceLocation, EntityLootConfig> configs = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            final ResourceLocation entityId = ResourceLocation.tryParse(entry.getKey());
            if (entityId == null || !entry.getValue().isJsonObject())
                continue;
            final EntityLootConfig config = fromJson(entry.getValue().getAsJsonObject());
            if (!config.isEmpty())
                configs.put(entityId, config);
        }
        return configs;
    }

    public static synchronized void writeAll(Map<ResourceLocation, EntityLootConfig> configsByEntity) {
        final Path path = configPath();
        final JsonObject root = new JsonObject();
        final Map<ResourceLocation, EntityLootConfig> nextCached = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, EntityLootConfig> entry : configsByEntity.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty())
                continue;
            root.add(entry.getKey().toString(), toJson(entry.getValue()));
            nextCached.put(entry.getKey(), entry.getValue());
        }
        try {
            SafeJsonFile.write(path, GSON, root);
        } catch (IOException e) {
            LOGGER.error("[MobExpansion] Failed to save {}", path, e);
            return;
        }
        cached = nextCached;
        loaded = true;
        try {
            LegacyLootJsScript.deleteIfPresent();
        } catch (RuntimeException | Error ignored) {
        }
    }

    private static JsonObject toJson(EntityLootConfig config) {
        final JsonObject json = new JsonObject();
        json.addProperty("replaceVanilla", config.replaceVanilla());
        json.add("drops", dropsToJson(config.drops()));
        json.add("experience", experienceToJson(config.experience()));
        return json;
    }

    private static JsonArray dropsToJson(List<MobDropEntry> drops) {
        final JsonArray array = new JsonArray();
        for (MobDropEntry dropEntry : drops) {
            final JsonObject json = new JsonObject();
            json.addProperty("item", dropEntry.itemId().toString());
            json.addProperty("minCount", dropEntry.minCount());
            json.addProperty("maxCount", dropEntry.maxCount());
            json.addProperty("chance", dropEntry.chance());
            json.add("conditions", conditionsToJson(dropEntry.conditions()));
            array.add(json);
        }
        return array;
    }

    private static JsonArray experienceToJson(List<MobExperienceEntry> experience) {
        final JsonArray array = new JsonArray();
        for (MobExperienceEntry experienceEntry : experience) {
            final JsonObject json = new JsonObject();
            json.addProperty("minAmount", experienceEntry.minAmount());
            json.addProperty("maxAmount", experienceEntry.maxAmount());
            json.addProperty("chance", experienceEntry.chance());
            json.add("conditions", conditionsToJson(experienceEntry.conditions()));
            array.add(json);
        }
        return array;
    }

    private static JsonArray conditionsToJson(List<LootConditionEntry> conditions) {
        final JsonArray array = new JsonArray();
        for (LootConditionEntry condition : conditions) {
            final JsonObject json = new JsonObject();
            json.addProperty("type", condition.type().name());
            json.addProperty("stringValue", condition.stringValue());
            json.addProperty("intMin", condition.intMin());
            json.addProperty("intMax", condition.intMax());
            json.addProperty("intExtra", condition.intExtra());
            json.addProperty("flagA", condition.flagA());
            json.addProperty("flagB", condition.flagB());
            array.add(json);
        }
        return array;
    }

    private static EntityLootConfig fromJson(JsonObject json) {
        return new EntityLootConfig(
                parseDrops(json.get("drops")),
                parseExperience(json.get("experience")),
                readBoolean(json, "replaceVanilla", false)
        );
    }

    private static List<MobDropEntry> parseDrops(@Nullable JsonElement element) {
        if (element == null || !element.isJsonArray())
            return List.of();
        final List<MobDropEntry> drops = new ArrayList<>();
        for (JsonElement entry : element.getAsJsonArray()) {
            if (!entry.isJsonObject())
                continue;
            final JsonObject json = entry.getAsJsonObject();
            final ResourceLocation itemId = ResourceLocation.tryParse(readString(json, "item", ""));
            if (itemId == null)
                continue;
            drops.add(new MobDropEntry(
                    itemId,
                    readInt(json, "minCount", 1),
                    readInt(json, "maxCount", 1),
                    readFloat(json, "chance", 1.0F),
                    parseConditions(json.get("conditions"))
            ));
        }
        return drops;
    }

    private static List<MobExperienceEntry> parseExperience(@Nullable JsonElement element) {
        if (element == null || !element.isJsonArray())
            return List.of();
        final List<MobExperienceEntry> experience = new ArrayList<>();
        for (JsonElement entry : element.getAsJsonArray()) {
            if (!entry.isJsonObject())
                continue;
            final JsonObject json = entry.getAsJsonObject();
            experience.add(new MobExperienceEntry(
                    readInt(json, "minAmount", 0),
                    readInt(json, "maxAmount", 0),
                    readFloat(json, "chance", 1.0F),
                    parseConditions(json.get("conditions"))
            ));
        }
        return experience;
    }

    private static List<LootConditionEntry> parseConditions(@Nullable JsonElement element) {
        if (element == null || !element.isJsonArray())
            return List.of();
        final List<LootConditionEntry> conditions = new ArrayList<>();
        for (JsonElement entry : element.getAsJsonArray()) {
            if (!entry.isJsonObject())
                continue;
            final JsonObject json = entry.getAsJsonObject();
            conditions.add(new LootConditionEntry(
                    LootConditionType.fromName(readString(json, "type", "")),
                    readString(json, "stringValue", ""),
                    readInt(json, "intMin", 0),
                    readInt(json, "intMax", 0),
                    readInt(json, "intExtra", 0),
                    readByte(json, "flagA", LootConditionEntry.TRI_ANY),
                    readByte(json, "flagB", LootConditionEntry.TRI_ANY)
            ));
        }
        return conditions;
    }

    private static boolean readBoolean(JsonObject json, String key, boolean fallback) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive())
            return fallback;
        try {
            return json.get(key).getAsBoolean();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static int readInt(JsonObject json, String key, int fallback) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive())
            return fallback;
        try {
            return json.get(key).getAsInt();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static float readFloat(JsonObject json, String key, float fallback) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive())
            return fallback;
        try {
            return json.get(key).getAsFloat();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static byte readByte(JsonObject json, String key, byte fallback) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive())
            return fallback;
        try {
            return json.get(key).getAsByte();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static String readString(JsonObject json, String key, String fallback) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive())
            return fallback;
        try {
            return json.get(key).getAsString();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}

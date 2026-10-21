package net.mcskill.mobexpansion.attributes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.mcskill.mobexpansion.clearance.MobClearance;
import net.mcskill.mobexpansion.drops.DropDestination;
import net.mcskill.mobexpansion.spawner.SpawnerMobCatalog;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

import com.mojang.logging.LogUtils;
import net.mcskill.mobexpansion.drops.SafeJsonFile;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EntityAttributesConfigIO {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String RELATIVE_PATH = "mobexpansion/entity_attributes.json";

    private static Map<ResourceLocation, EntityAttributeConfig> cached = Collections.emptyMap();

    private EntityAttributesConfigIO() {
    }

    public static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(RELATIVE_PATH);
    }

    public static synchronized void reload() {
        final Map<ResourceLocation, EntityAttributeConfig> loaded = loadFromDisk();
        if (loaded == null) {
            // Файл есть, но не читается (и .bak тоже) — не сбрасываем атрибуты на дефолтные.
            LOGGER.error("[MobExpansion] {} is unreadable, keeping cached entity attributes", configPath());
            if (!cached.isEmpty())
                return;
        }
        setCached(mergeWithDefaults(loaded == null ? Collections.emptyMap() : loaded));
    }

    private static void setCached(Map<ResourceLocation, EntityAttributeConfig> configs) {
        cached = configs;
        final Map<ResourceLocation, Integer> clearance = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, EntityAttributeConfig> entry : configs.entrySet())
            clearance.put(entry.getKey(), entry.getValue().clearanceLevel());
        MobClearance.applyConfigured(clearance);
    }

    public static synchronized Map<ResourceLocation, EntityAttributeConfig> getCached() {
        if (cached.isEmpty())
            reload();
        return cached;
    }

    public static EntityAttributeConfig getCached(ResourceLocation entityId) {
        return getCached().getOrDefault(entityId, EntityAttributeDefaults.get(entityId));
    }

    public static boolean hasConfigured(ResourceLocation entityId) {
        return entityId != null && getCached().containsKey(entityId);
    }

    public static Map<ResourceLocation, EntityAttributeConfig> loadMergedWithDefaults() {
        final Map<ResourceLocation, EntityAttributeConfig> loaded = loadFromDisk();
        if (loaded == null && !cached.isEmpty())
            return new LinkedHashMap<>(cached);
        return mergeWithDefaults(loaded == null ? Collections.emptyMap() : loaded);
    }

    private static Map<ResourceLocation, EntityAttributeConfig> mergeWithDefaults(Map<ResourceLocation, EntityAttributeConfig> loaded) {
        final Map<ResourceLocation, EntityAttributeConfig> merged = new LinkedHashMap<>(EntityAttributeDefaults.all());
        for (Map.Entry<ResourceLocation, EntityAttributeConfig> entry : loaded.entrySet())
            if (SpawnerMobCatalog.isSpawnable(entry.getKey()))
                merged.put(entry.getKey(), entry.getValue().sanitized(entry.getKey()));
        return merged;
    }

    /**
     * Атрибуты с диска; при битом основном файле — из резервной копии. {@code null} — прочитать не удалось.
     */
    @Nullable
    public static Map<ResourceLocation, EntityAttributeConfig> loadFromDisk() {
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
                LOGGER.warn("[MobExpansion] Loaded entity attributes from backup {}", backup);
                return parseRoot(root);
            }
        } catch (IOException e) {
            LOGGER.error("[MobExpansion] Failed to read {}: {}", backup, e.getMessage());
        }
        return null;
    }

    private static Map<ResourceLocation, EntityAttributeConfig> parseRoot(JsonObject root) {
        final Map<ResourceLocation, EntityAttributeConfig> configs = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            final ResourceLocation entityId = ResourceLocation.tryParse(entry.getKey());
            if (entityId == null || !entry.getValue().isJsonObject())
                continue;
            configs.put(entityId, fromJson(entry.getValue().getAsJsonObject(), EntityAttributeDefaults.get(entityId), entityId));
        }
        return configs;
    }

    public static synchronized void writeAll(Map<ResourceLocation, EntityAttributeConfig> configsByEntity) {
        final Path path = configPath();
        try {
            final JsonObject root = new JsonObject();
            final Map<ResourceLocation, EntityAttributeConfig> toWrite = new LinkedHashMap<>();
            for (ResourceLocation entityId : EntityAttributeDefaults.all().keySet())
                toWrite.put(entityId, configsByEntity.getOrDefault(entityId, EntityAttributeDefaults.get(entityId)).sanitized(entityId));
            for (Map.Entry<ResourceLocation, EntityAttributeConfig> entry : configsByEntity.entrySet()) {
                if (SpawnerMobCatalog.isSpawnable(entry.getKey()))
                    toWrite.put(entry.getKey(), entry.getValue().sanitized(entry.getKey()));
            }
            for (Map.Entry<ResourceLocation, EntityAttributeConfig> entry : toWrite.entrySet())
                root.add(entry.getKey().toString(), toJson(entry.getValue()));
            SafeJsonFile.write(path, GSON, root);
            setCached(mergeWithDefaults(parseRoot(root)));
        } catch (IOException e) {
            LOGGER.error("[MobExpansion] Failed to save {}", path, e);
        }
    }

    private static JsonObject toJson(EntityAttributeConfig config) {
        final JsonObject json = new JsonObject();
        json.addProperty("maxHealth", config.maxHealth());
        json.addProperty("armor", config.armor());
        json.addProperty("armorToughness", config.armorToughness());
        json.addProperty("attackDamage", config.attackDamage());
        json.addProperty("movementSpeed", config.movementSpeed());
        json.addProperty("knockbackResistance", config.knockbackResistance());
        json.addProperty("followRange", config.followRange());
        if (!config.extras().isEmpty()) {
            final JsonObject extras = new JsonObject();
            for (Map.Entry<String, Double> entry : config.extras().entrySet())
                extras.addProperty(entry.getKey(), entry.getValue());
            json.add("extras", extras);
        }
        json.addProperty("dropDestination", config.dropDestination().name());
        json.addProperty("clearanceLevel", config.clearanceLevel());
        return json;
    }

    private static EntityAttributeConfig fromJson(JsonObject json, EntityAttributeConfig fallback, ResourceLocation entityId) {
        final Map<String, Double> extras = new LinkedHashMap<>(fallback.extras());
        if (json.has("extras") && json.get("extras").isJsonObject()) {
            final JsonObject extrasJson = json.getAsJsonObject("extras");
            for (Map.Entry<String, JsonElement> entry : extrasJson.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    try {
                        extras.put(entry.getKey(), entry.getValue().getAsDouble());
                    } catch (RuntimeException ignored) {
                    }
                }
            }
        }
        return new EntityAttributeConfig(
                readDouble(json, "maxHealth", fallback.maxHealth()),
                readDouble(json, "armor", fallback.armor()),
                readDouble(json, "armorToughness", fallback.armorToughness()),
                readDouble(json, "attackDamage", fallback.attackDamage()),
                readDouble(json, "movementSpeed", fallback.movementSpeed()),
                readDouble(json, "knockbackResistance", fallback.knockbackResistance()),
                readDouble(json, "followRange", fallback.followRange()),
                extras,
                DropDestination.byName(readString(json, "dropDestination", fallback.dropDestination().name())),
                (int) Math.round(readDouble(json, "clearanceLevel", fallback.clearanceLevel()))
        ).sanitized(entityId);
    }

    private static double readDouble(JsonObject json, String key, double fallback) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive())
            return fallback;
        try {
            return json.get(key).getAsDouble();
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

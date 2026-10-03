package net.mcskill.mobexpansion.attributes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.mcskill.mobexpansion.drops.DropDestination;
import net.mcskill.mobexpansion.spawner.SpawnerMobCatalog;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

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
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String RELATIVE_PATH = "mobexpansion/entity_attributes.json";

    private static Map<ResourceLocation, EntityAttributeConfig> cached = Collections.emptyMap();

    private EntityAttributesConfigIO() {
    }

    public static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(RELATIVE_PATH);
    }

    public static synchronized void reload() {
        cached = loadMergedWithDefaults();
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
        final Map<ResourceLocation, EntityAttributeConfig> merged = new LinkedHashMap<>(EntityAttributeDefaults.all());
        final Map<ResourceLocation, EntityAttributeConfig> loaded = loadFromDisk();
        for (Map.Entry<ResourceLocation, EntityAttributeConfig> entry : loaded.entrySet())
            if (SpawnerMobCatalog.isSpawnable(entry.getKey()))
                merged.put(entry.getKey(), entry.getValue().sanitized(entry.getKey()));
        return merged;
    }

    public static Map<ResourceLocation, EntityAttributeConfig> loadFromDisk() {
        final Path path = configPath();
        if (!Files.isRegularFile(path))
            return Collections.emptyMap();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            final JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null)
                return Collections.emptyMap();
            final Map<ResourceLocation, EntityAttributeConfig> configs = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                final ResourceLocation entityId = ResourceLocation.tryParse(entry.getKey());
                if (entityId == null || !entry.getValue().isJsonObject())
                    continue;
                configs.put(entityId, fromJson(entry.getValue().getAsJsonObject(), EntityAttributeDefaults.get(entityId), entityId));
            }
            return configs;
        } catch (IOException | RuntimeException ignored) {
            return Collections.emptyMap();
        }
    }

    public static synchronized void writeAll(Map<ResourceLocation, EntityAttributeConfig> configsByEntity) {
        final Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
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
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            cached = loadMergedWithDefaults();
        } catch (IOException ignored) {
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
                DropDestination.byName(readString(json, "dropDestination", fallback.dropDestination().name()))
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

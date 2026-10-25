package net.mcskill.mobexpansion.info;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.attributes.EntityAttributeConfig;
import net.mcskill.mobexpansion.attributes.EntityAttributesConfigIO;
import net.mcskill.mobexpansion.attributes.EntityExtraParametersCatalog;
import net.mcskill.mobexpansion.clearance.MobClearance;
import net.mcskill.mobexpansion.drops.EntityLootConfig;
import net.mcskill.mobexpansion.drops.EntityLootConfigIO;
import net.mcskill.mobexpansion.drops.MobDropEntry;
import net.mcskill.mobexpansion.drops.MobExperienceEntry;
import net.mcskill.mobexpansion.network.MobInfoPacket;
import net.mcskill.mobexpansion.network.MobSpawnInfoPacket;
import net.mcskill.mobexpansion.network.MobSpawnInfoRequestPacket;
import net.mcskill.mobexpansion.spawner.MobSpawnerIndex;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Данные для страниц мобов в EMI: собирает {@link MobInfo} по всем мобам (характеристики и допуск — из конфига
 * характеристик MobExpansion, дроп — из конфига дропа и ванильной loot-таблицы) и рассылает клиентам:
 * при входе, после сохранения настройщика мобов и после {@code /reload}.
 * Спавнеры с таймерами клиент запрашивает отдельно, когда открывает страницу моба.
 */
@EventBusSubscriber(modid = Core.MODID)
public final class MobInfoSync {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long REQUEST_COOLDOWN_MILLIS = 250L;
    private static final int MAX_NESTED_TABLES = 3;

    private static volatile List<MobInfo> cached;
    private static final Map<UUID, Long> LAST_REQUEST = new HashMap<>();

    private MobInfoSync() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        cached = null;
        MobSpawnInfoRequestPacket.serverHandler = MobInfoSync::sendSpawnPoints;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        cached = null;
        LAST_REQUEST.clear();
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            sendTo(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_REQUEST.remove(event.getEntity().getUUID());
    }

    /** {@code /reload} (игрок не задан) — loot-таблицы могли поменяться. */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null)
            syncToAll();
    }

    public static void sendTo(ServerPlayer player) {
        if (player.connection != null)
            PacketDistributor.sendToPlayer(player, new MobInfoPacket(get(player.server)));
    }

    /** Пересобрать данные и разослать всем (после изменения конфигов). */
    public static void syncToAll() {
        cached = null;
        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return;
        PacketDistributor.sendToAllPlayers(new MobInfoPacket(get(server)));
    }

    private static void sendSpawnPoints(ServerPlayer player, ResourceLocation entityId) {
        final long now = System.currentTimeMillis();
        final Long last = LAST_REQUEST.get(player.getUUID());
        if (last != null && now - last < REQUEST_COOLDOWN_MILLIS)
            return;
        LAST_REQUEST.put(player.getUUID(), now);
        PacketDistributor.sendToPlayer(player, new MobSpawnInfoPacket(entityId, MobSpawnerIndex.pointsFor(player.server, entityId)));
    }

    private static List<MobInfo> get(MinecraftServer server) {
        List<MobInfo> result = cached;
        if (result == null) {
            result = build(server);
            cached = result;
        }
        return result;
    }

    private static List<MobInfo> build(MinecraftServer server) {
        final List<MobInfo> infos = new ArrayList<>();
        for (ResourceLocation entityId : mobIds(server)) {
            try {
                infos.add(buildOne(server, entityId));
            } catch (RuntimeException e) {
                LOGGER.warn("[MobExpansion] Failed to build EMI info for {}: {}", entityId, e.toString());
            }
        }
        return List.copyOf(infos);
    }

    /** Все типы мобов: сначала MobExpansion (в порядке регистрации), затем остальные по id. */
    private static List<ResourceLocation> mobIds(MinecraftServer server) {
        final ServerLevel level = server.overworld();
        final List<ResourceLocation> own = new ArrayList<>();
        final List<ResourceLocation> rest = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (!isMob(type, level))
                continue;
            final ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            (Core.MODID.equals(id.getNamespace()) ? own : rest).add(id);
        }
        rest.sort(Comparator.comparing(ResourceLocation::toString));
        own.addAll(rest);
        return own;
    }

    @SuppressWarnings("unchecked")
    private static boolean isMob(EntityType<?> type, ServerLevel level) {
        if (type == EntityType.PLAYER || type == EntityType.ARMOR_STAND)
            return false;
        if (!DefaultAttributes.hasSupplier((EntityType<? extends LivingEntity>) type))
            return false;
        try {
            final Entity probe = type.create(level);
            if (probe == null)
                return false;
            final boolean mob = probe instanceof Mob;
            probe.discard();
            return mob;
        } catch (Throwable throwable) {
            if (throwable instanceof VirtualMachineError error)
                throw error;
            return false;
        }
    }

    private static MobInfo buildOne(MinecraftServer server, ResourceLocation entityId) {
        final EntityAttributeConfig attributes = EntityAttributesConfigIO.getCached(entityId);
        final Map<String, Float> abilityDamage = new LinkedHashMap<>();
        for (Map.Entry<String, Double> extra : attributes.extras().entrySet()) {
            if (extra.getKey().endsWith(EntityExtraParametersCatalog.DAMAGE_SUFFIX) && extra.getValue() > 0.0D)
                abilityDamage.put(extra.getKey(), extra.getValue().floatValue());
        }

        final EntityLootConfig loot = EntityLootConfigIO.getCached(entityId);
        final List<MobInfo.Drop> drops = new ArrayList<>();
        for (MobDropEntry drop : loot.drops()) {
            if (BuiltInRegistries.ITEM.containsKey(drop.itemId()))
                drops.add(new MobInfo.Drop(drop.itemId(), drop.minCount(), drop.maxCount(), drop.chance(), !drop.conditions().isEmpty(), false));
        }
        if (!loot.replaceVanilla()) {
            final EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entityId);
            collectLootTable(server, type.getDefaultLootTable(), drops, 0);
        }

        int xpMin = 0;
        int xpMax = -1;
        float xpChance = 0.0F;
        if (!loot.experience().isEmpty()) {
            final MobExperienceEntry xp = loot.experience().getFirst();
            xpMin = xp.minAmount();
            xpMax = xp.maxAmount();
            xpChance = xp.chance();
        }
        return new MobInfo(entityId, (float) attributes.maxHealth(), (float) attributes.armor(), (float) attributes.attackDamage(),
                (float) attributes.movementSpeed(), abilityDamage, MobClearance.getRequired(entityId), drops, xpMin, xpMax, xpChance);
    }

    // ---- Ванильная loot-таблица: разбираем JSON-представление таблицы (без доступа к приватным полям) ----

    private static void collectLootTable(MinecraftServer server, ResourceKey<LootTable> key, List<MobInfo.Drop> out, int depth) {
        if (key == null || depth > MAX_NESTED_TABLES)
            return;
        final LootTable table = server.reloadableRegistries().getLootTable(key);
        if (table == LootTable.EMPTY)
            return;
        final JsonElement json = LootTable.DIRECT_CODEC
                .encodeStart(server.registryAccess().createSerializationContext(JsonOps.INSTANCE), table)
                .result().orElse(null);
        if (json == null || !json.isJsonObject())
            return;
        for (JsonElement pool : array(json.getAsJsonObject(), "pools")) {
            if (!pool.isJsonObject())
                continue;
            final Chance poolChance = conditions(pool.getAsJsonObject(), new Chance(1.0F, false));
            for (JsonElement entry : array(pool.getAsJsonObject(), "entries"))
                walkEntry(server, entry, poolChance, out, depth);
        }
    }

    private record Chance(float chance, boolean conditional) {
    }

    private static void walkEntry(MinecraftServer server, JsonElement element, Chance parent, List<MobInfo.Drop> out, int depth) {
        if (!element.isJsonObject())
            return;
        final JsonObject entry = element.getAsJsonObject();
        final Chance chance = conditions(entry, parent);
        final String type = stripNamespace(string(entry, "type"));
        switch (type) {
            case "item" -> {
                final ResourceLocation itemId = ResourceLocation.tryParse(string(entry, "name"));
                if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId))
                    return;
                int min = 1;
                int max = 1;
                for (JsonElement function : array(entry, "functions")) {
                    if (function.isJsonObject() && "set_count".equals(stripNamespace(string(function.getAsJsonObject(), "function")))) {
                        final JsonElement count = function.getAsJsonObject().get("count");
                        min = (int) Math.floor(numberMin(count, 1));
                        max = (int) Math.ceil(numberMax(count, 1));
                    }
                }
                if (max <= 0)
                    return;
                out.add(new MobInfo.Drop(itemId, Math.max(1, min), Math.max(1, max), chance.chance, chance.conditional, true));
            }
            case "alternatives", "group", "sequence" -> {
                for (JsonElement child : array(entry, "children"))
                    walkEntry(server, child, chance, out, depth);
            }
            case "loot_table" -> {
                final JsonElement value = entry.get("value");
                if (value != null && value.isJsonPrimitive()) {
                    final ResourceLocation tableId = ResourceLocation.tryParse(value.getAsString());
                    if (tableId != null)
                        collectLootTable(server, ResourceKey.create(Registries.LOOT_TABLE, tableId), out, depth + 1);
                }
            }
            default -> {
            }
        }
    }

    /** Условия записи/пула: случайный шанс перемножаем, любые другие условия помечают дроп как «с условием». */
    private static Chance conditions(JsonObject json, Chance parent) {
        float chance = parent.chance;
        boolean conditional = parent.conditional;
        for (JsonElement element : array(json, "conditions")) {
            if (!element.isJsonObject())
                continue;
            final JsonObject condition = element.getAsJsonObject();
            switch (stripNamespace(string(condition, "condition"))) {
                case "random_chance" -> chance *= (float) numberMax(condition.get("chance"), 1);
                case "random_chance_with_enchanted_bonus" -> chance *= (float) numberMax(condition.get("unenchanted_chance"), 1);
                default -> conditional = true;
            }
        }
        return new Chance(Math.max(0.0F, Math.min(1.0F, chance)), conditional);
    }

    private static double numberMin(JsonElement element, double fallback) {
        return number(element, fallback, true);
    }

    private static double numberMax(JsonElement element, double fallback) {
        return number(element, fallback, false);
    }

    /** NumberProvider: число, {@code constant} или {@code uniform}/{@code binomial} (берём границы). */
    private static double number(JsonElement element, double fallback, boolean min) {
        if (element == null)
            return fallback;
        if (element.isJsonPrimitive()) {
            try {
                return element.getAsDouble();
            } catch (RuntimeException ignored) {
                return fallback;
            }
        }
        if (!element.isJsonObject())
            return fallback;
        final JsonObject object = element.getAsJsonObject();
        if (object.has("value"))
            return number(object.get("value"), fallback, min);
        if (object.has("min") || object.has("max"))
            return number(object.get(min ? "min" : "max"), fallback, min);
        if (object.has("n"))
            return min ? 0 : number(object.get("n"), fallback, false);
        return fallback;
    }

    private static Iterable<JsonElement> array(JsonObject json, String key) {
        final JsonElement element = json.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : new JsonArray();
    }

    private static String string(JsonObject json, String key) {
        final JsonElement element = json.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : "";
    }

    private static String stripNamespace(String id) {
        return id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
    }
}

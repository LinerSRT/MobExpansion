package net.mcskill.mobexpansion.attributes;

import net.mcskill.mobexpansion.Core;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EntityExtraParametersCatalog {
    private static final Map<ResourceLocation, List<ExtraParameterDefinition>> PARAMS;

    static {
        final Map<ResourceLocation, List<ExtraParameterDefinition>> map = new LinkedHashMap<>();
        map.put(id("rat"), List.of(
                parameter("attackReach", 2.0, 0.5, 16)
        ));
        map.put(id("mosquito"), List.of(
                parameter("drainHeal", 5, 0, 100),
                parameter("attackReach", 2.4, 0.5, 16),
                parameter("hoverCombatRadius", 2.2, 0.5, 16),
                parameter("hoverWanderRadius", 3.0, 0.5, 16),
                parameter("minHoverHeight", 1.2, 0, 16),
                parameter("maxHoverHeight", 2.4, 0, 16),
                parameter("flyingSpeed", 0.35, 0, 4)
        ));
        map.put(id("leech"), List.of(
                parameter("vampHeal", 1, 0, 100),
                parameter("attackReach", 4.5, 0.5, 16),
                parameter("holdDistance", 4.0, 0.5, 16)
        ));
        map.put(id("regular_spider"), List.of(
                parameter("homeRadius", 12, 1, 64),
                parameter("fleeHealthRatio", 0.35, 0.01, 1),
                parameter("maxSilkReserve", 10, 0, 100),
                parameter("combatLeashBuffer", 8, 0, 64),
                parameter("meleePoisonChance", 0.25, 0, 1),
                parameter("meleePoisonDuration", 60, 0, 6000),
                parameter("meleePoisonAmplifier", 0, 0, 10),
                parameter("maxComfortableLight", 12, 0, 15)
        ));
        map.put(id("poison_spider"), List.of(
                parameter("homeRadius", 12, 1, 64),
                parameter("fleeHealthRatio", 0.15, 0.01, 1),
                parameter("maxSilkReserve", 10, 0, 100),
                parameter("combatLeashBuffer", 8, 0, 64),
                parameter("maxComfortableLight", 12, 0, 15),
                parameter("spitSpeed", 1.15, 0.1, 4),
                parameter("spitAttackRadius", 12, 1, 48),
                parameter("spitMinInterval", 120, 1, 1200),
                parameter("spitMaxInterval", 160, 1, 1200),
                parameter("retreatDistance", 6, 1, 48),
                parameter("holdDistance", 8.5, 1, 48),
                parameter("approachDistance", 11, 1, 48),
                parameter("bitePoisonDuration", 160, 0, 6000),
                parameter("bitePoisonAmplifier", 1, 0, 10),
                parameter("biteSlowDuration", 100, 0, 6000),
                parameter("poisonCloudRadius", 3, 0.5, 16),
                parameter("poisonCloudDuration", 160, 0, 6000),
                parameter("poisonCloudAmplifier", 1, 0, 10)
        ));
        map.put(id("spider_spawn"), List.of(
                parameter("minSpiderStock", 25, 0, 200),
                parameter("maxSpiderStock", 50, 0, 200),
                parameter("maxStockCap", 80, 0, 500),
                parameter("maxLivingSpiders", 25, 0, 100),
                parameter("maxOutsideSpiders", 2, 0, 50),
                parameter("maxLivingOnHit", 10, 0, 50),
                parameter("spidersPerHit", 3, 0, 20),
                parameter("spawnIntervalTicks", 80, 1, 1200),
                parameter("activationRange", 16, 1, 64),
                parameter("queenMaxHealth", 60, 1, 1_000_000),
                parameter("queenStockThreshold", 40, 0, 500),
                parameter("queenSpawnDelayTicks", 6000, 0, 240000),
                parameter("nestHealAmount", 4, 0, 100),
                parameter("itemsForStockRefill", 5, 1, 100),
                parameter("experienceForStockRefill", 3, 1, 100),
                parameter("damageForNewSpawner", 60, 0, 1000),
                parameter("cobwebAlarmSpawnFraction", 0.7, 0, 1),
                parameter("spiderHurtSpawnChance", 0.65, 0, 1),
                parameter("newSpawnerMinDistance", 6, 1, 64),
                parameter("patrolHomeRadius", 12, 1, 64)
        ));
        map.put(id("rat_king"), List.of(
                parameter("handHitDamage", 10, 0, 1024),
                parameter("swoopDamage", 12, 0, 1024),
                parameter("potionMeleeDamage", 10, 0, 1024),
                parameter("handHitRange", 3, 0.5, 16),
                parameter("swoopRange", 3.5, 0.5, 16),
                parameter("meleeReach", 3.2, 0.5, 16),
                parameter("rangedPreferredDistance", 5.5, 1, 48),
                parameter("potionIntervalTicks", 160, 1, 12000),
                parameter("summonIntervalTicks", 220, 1, 12000),
                parameter("summonRatCount", 4, 0, 32),
                parameter("summonRadius", 4.5, 0.5, 32),
                parameter("nearShootCooldown", 35, 1, 1200),
                parameter("farShootCooldown", 80, 1, 1200),
                parameter("boltDamage", 5, 0, 1024),
                parameter("boltPassDamage", 5, 0, 1024),
                parameter("potionImpactDamage", 6, 0, 1024),
                parameter("potionImpactRadius", 4, 0.5, 32),
                parameter("poisonAreaRadius", 4, 0.5, 32),
                parameter("poisonAreaLifetime", 100, 1, 6000),
                parameter("poisonDuration", 100, 0, 6000),
                parameter("poisonAmplifier", 1, 0, 10)
        ));
        map.put(id("sentinel_golem"), List.of(
                parameter("meleeDamage", 5, 0, 1024),
                parameter("estocadaDamage", 5, 0, 1024),
                parameter("slamDamage", 7, 0, 1024),
                parameter("slamRadius", 4, 0.5, 32),
                parameter("meleeRange", 4, 0.5, 32),
                parameter("lungeRange", 10, 1, 64),
                parameter("jumpMinRange", 8, 1, 64),
                parameter("jumpMaxRange", 29, 1, 128),
                parameter("attackCooldownTicks", 20, 1, 1200),
                parameter("explosionDamageMultiplier", 0.5, 0, 2)
        ));
        map.put(id("sentinel_statue"), List.of(
                parameter("auraRadius", 3, 0.5, 32),
                parameter("auraIntervalTicks", 10, 1, 200),
                parameter("auraSlowDuration", 20, 0, 6000),
                parameter("auraSlowAmplifier", 0, 0, 10)
        ));
        map.put(id("redstone_engineer"), List.of(
                parameter("wrenchDamage", 7, 0, 1024),
                parameter("projectileSpeed", 1.25, 0.1, 4),
                parameter("summonCooldownTicks", 55, 1, 12000),
                parameter("throwCooldownTicks", 32, 1, 1200),
                parameter("meleeCooldownTicks", 22, 1, 1200),
                parameter("meleeRange", 2.5, 0.5, 16),
                parameter("throwRange", 14, 1, 64),
                parameter("holdDistance", 9, 1, 48),
                parameter("retreatDistance", 6, 1, 48),
                parameter("maxAutomatons", 1, 0, 16),
                parameter("maxDrones", 1, 0, 16),
                parameter("maxTowers", 1, 0, 16),
                parameter("constructLeashDistance", 18, 1, 64)
        ));
        map.put(id("redstone_automaton"), List.of(
                parameter("spinDamageMultiplier", 0.85, 0, 4),
                parameter("spinRadius", 1.5, 0.5, 16),
                parameter("meleeReach", 3, 0.5, 16),
                parameter("attackCooldownTicks", 18, 1, 1200)
        ));
        map.put(id("redstone_drone"), List.of(
                parameter("projectileDamage", 4, 0, 1024),
                parameter("maxShootDistance", 18, 1, 64),
                parameter("shootCooldownTicks", 14, 1, 1200),
                parameter("minHoverHeight", 2, 0, 16),
                parameter("maxHoverHeight", 3, 0, 16),
                parameter("escortRadius", 2, 0.5, 16),
                parameter("combatHoverRadius", 3.5, 0.5, 16)
        ));
        map.put(id("redstone_tower"), List.of(
                parameter("laserDamageMultiplier", 0.22, 0, 4),
                parameter("laserRadius", 6, 0.5, 32),
                parameter("stompDamageMultiplier", 0.35, 0, 4),
                parameter("stompRadius", 2.5, 0.5, 16),
                parameter("projectileDamage", 7, 0, 1024),
                parameter("meleeRange", 2.5, 0.5, 16),
                parameter("rangedRange", 14, 1, 64),
                parameter("attackCooldownTicks", 40, 1, 1200),
                parameter("stompSlowDuration", 80, 0, 6000),
                parameter("stompSlowAmplifier", 2, 0, 10),
                parameter("stompWeakDuration", 60, 0, 6000),
                parameter("stompWeakAmplifier", 0, 0, 10)
        ));
        PARAMS = Collections.unmodifiableMap(map);
    }

    private EntityExtraParametersCatalog() {
    }

    public static List<ExtraParameterDefinition> paramsFor(ResourceLocation entityId) {
        return PARAMS.getOrDefault(entityId, List.of());
    }

    public static Map<String, Double> defaultExtras(ResourceLocation entityId) {
        final Map<String, Double> extras = new LinkedHashMap<>();
        for (ExtraParameterDefinition def : paramsFor(entityId))
            extras.put(def.key(), def.defaultValue());
        return extras;
    }

    public static ExtraParameterDefinition find(ResourceLocation entityId, String key) {
        for (ExtraParameterDefinition def : paramsFor(entityId)) {
            if (def.key().equals(key))
                return def;
        }
        return null;
    }

    public static Map<String, Double> sanitizeExtras(ResourceLocation entityId, Map<String, Double> raw) {
        final Map<String, Double> sanitized = new LinkedHashMap<>();
        for (ExtraParameterDefinition def : paramsFor(entityId)) {
            final double value = raw != null && raw.containsKey(def.key()) ? raw.get(def.key()) : def.defaultValue();
            sanitized.put(def.key(), def.sanitize(value));
        }
        return sanitized;
    }

    private static ResourceLocation id(String path) {
        return Core.loc(path);
    }

    private static ExtraParameterDefinition parameter(String key, double defaultValue, double min, double max) {
        return new ExtraParameterDefinition(key, defaultValue, min, max);
    }
}

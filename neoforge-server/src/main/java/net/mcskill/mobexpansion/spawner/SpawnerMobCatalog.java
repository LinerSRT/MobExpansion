package net.mcskill.mobexpansion.spawner;

import net.mcskill.mobexpansion.init.MobExEntities;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SpawnerMobCatalog {
    private static final List<ResourceLocation> PRIORITY_IDS = List.of(
            MobExEntities.REGULAR_SPIDER.getId(),
            MobExEntities.POISON_SPIDER.getId(),
            MobExEntities.SPIDER_SPAWN.getId(),
            MobExEntities.REDSTONE_ENGINEER.getId(),
            MobExEntities.REDSTONE_AUTOMATON.getId(),
            MobExEntities.REDSTONE_DRONE.getId(),
            MobExEntities.REDSTONE_TOWER.getId(),
            MobExEntities.SENTINEL_STATUE.getId(),
            MobExEntities.SENTINEL_GOLEM.getId(),
            MobExEntities.RAT.getId(),
            MobExEntities.MOSQUITO.getId(),
            MobExEntities.LEECH.getId(),
            MobExEntities.RAT_KING.getId()
    );

    private static volatile List<ResourceLocation> cachedIds;
    private static volatile boolean probedWithLevel;
    private static final Map<EntityType<?>, Boolean> MOB_TYPE_CACHE = new IdentityHashMap<>();

    private SpawnerMobCatalog() {
    }

    public static List<ResourceLocation> getSpawnableIds() {
        final Level probeLevel = probeLevel();
        List<ResourceLocation> ids = cachedIds;
        if (ids == null || (probeLevel != null && !probedWithLevel)) {
            ids = buildSpawnableIds();
            cachedIds = ids;
            probedWithLevel = probeLevel != null;
        }
        return ids;
    }

    public static boolean isSpawnable(ResourceLocation entityId) {
        if (entityId == null)
            return false;
        return isMobType(BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null));
    }

    public static ResourceLocation cycle(ResourceLocation currentId, int direction) {
        return cycle(currentId, direction, getSpawnableIds());
    }

    public static ResourceLocation cycle(ResourceLocation currentId, int direction, List<ResourceLocation> ids) {
        if (ids == null || ids.isEmpty())
            return null;

        if (currentId == null)
            return direction >= 0 ? ids.getFirst() : ids.getLast();

        final int currentIndex = ids.indexOf(currentId);
        if (currentIndex < 0)
            return direction >= 0 ? ids.getFirst() : ids.getLast();

        final int nextIndex = currentIndex + direction;
        if (nextIndex < 0 || nextIndex >= ids.size())
            return null;
        return ids.get(nextIndex);
    }

    public static String displayName(ResourceLocation entityId) {
        if (entityId == null)
            return "None";
        return BuiltInRegistries.ENTITY_TYPE.getOptional(entityId)
                .map(entityType -> entityType.getDescription().getString())
                .orElse(entityId.toString());
    }

    @Nullable
    public static EntityType<?> resolveEntityType(ResourceLocation entityId) {
        if (entityId == null)
            return null;
        return BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null);
    }

    @SuppressWarnings("unchecked")
    public static EntityType<? extends Mob> resolveMobType(ResourceLocation entityId) {
        if (!isSpawnable(entityId))
            return null;
        final EntityType<?> entityType = resolveEntityType(entityId);
        if (entityType == null)
            return null;
        return (EntityType<? extends Mob>) entityType;
    }

    @Nullable
    public static Mob tryCreateMob(ResourceLocation entityId, Level level) {
        return tryCreateMob(resolveEntityType(entityId), level);
    }

    @Nullable
    public static Mob tryCreateMob(@Nullable EntityType<?> entityType, Level level) {
        if (entityType == null || level == null)
            return null;
        try {
            final Entity created = entityType.create(level);
            if (created instanceof Mob mob)
                return mob;
            if (created != null)
                created.discard();
            return null;
        } catch (Throwable throwable) {
            if (throwable instanceof VirtualMachineError)
                throw (VirtualMachineError) throwable;
            return null;
        }
    }

    private static List<ResourceLocation> buildSpawnableIds() {
        final Set<ResourceLocation> seen = new LinkedHashSet<>();
        final List<ResourceLocation> ids = new ArrayList<>();
        for (ResourceLocation priorityId : PRIORITY_IDS) {
            if (isSpawnable(priorityId) && seen.add(priorityId))
                ids.add(priorityId);
        }
        final List<ResourceLocation> rest = new ArrayList<>();
        for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
            if (!isMobType(entityType))
                continue;
            final ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
            if (entityId != null && seen.add(entityId))
                rest.add(entityId);
        }
        rest.sort(Comparator.comparing(ResourceLocation::toString));
        ids.addAll(rest);
        return List.copyOf(ids);
    }

    @SuppressWarnings("unchecked")
    private static boolean isMobType(EntityType<?> entityType) {
        if (entityType == null || entityType == EntityType.PLAYER || entityType == EntityType.ARMOR_STAND)
            return false;
        if (!DefaultAttributes.hasSupplier((EntityType<? extends LivingEntity>) entityType))
            return false;
        final Boolean cached = MOB_TYPE_CACHE.get(entityType);
        if (cached != null)
            return cached;
        final Level level = probeLevel();
        if (level == null)
            return false;
        final Mob probe = tryCreateMob(entityType, level);
        final boolean isMob = probe != null;
        if (probe != null)
            probe.discard();
        MOB_TYPE_CACHE.put(entityType, isMob);
        return isMob;
    }

    @Nullable
    private static Level probeLevel() {
        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.overworld();
    }
}

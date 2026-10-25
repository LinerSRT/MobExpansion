package net.mcskill.mobexpansion.spawner;

import net.mcskill.mobexpansion.blockentity.MobSpawnerBlockEntity;
import net.mcskill.mobexpansion.info.MobSpawnPoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Где стоят спавнеры MobExpansion и когда у них следующая волна — для страницы моба в EMI
 * («Расположение», «До респавна»). Хранится в данных верхнего мира, поэтому известны и спавнеры в выгруженных чанках.
 * Спавнер сам обновляет запись: при загрузке чанка, смене настроек и после каждой волны; удаляется при сломе блока.
 */
public final class MobSpawnerIndex extends SavedData {
    private static final String DATA_NAME = "mobexpansion_spawner_index";
    private static final Factory<MobSpawnerIndex> FACTORY = new Factory<>(MobSpawnerIndex::new, MobSpawnerIndex::load, null);

    private final Map<Key, Spawner> spawners = new LinkedHashMap<>();

    private record Key(ResourceLocation dimension, long pos) {
    }

    private record Entry(ResourceLocation entityId, int maxAlive, int alive) {
    }

    private record Spawner(List<Entry> entries, long lastSpawnTime, int cooldownTicks) {
    }

    @Nullable
    private static MobSpawnerIndex get(@Nullable MinecraftServer server) {
        if (server == null)
            return null;
        final ServerLevel overworld = server.overworld();
        return overworld == null ? null : overworld.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private static Key key(Level level, BlockPos pos) {
        return new Key(level.dimension().location(), pos.asLong());
    }

    /** Обновить запись спавнера. {@code aliveCounts} — свежий подсчёт живых мобов по типам (может быть неполным). */
    public static void update(ServerLevel level, MobSpawnerBlockEntity spawner, Map<ResourceLocation, Integer> aliveCounts) {
        final MobSpawnerIndex index = get(level.getServer());
        if (index == null)
            return;
        final Key key = key(level, spawner.getBlockPos());
        final Spawner previous = index.spawners.get(key);
        final List<Entry> entries = new ArrayList<>();
        for (SpawnerEntityEntry entry : spawner.getEntries()) {
            if (!entry.isEnabled())
                continue;
            Integer alive = aliveCounts.get(entry.getEntityId());
            if (alive == null)
                alive = previous == null ? 0 : previous.entries.stream()
                        .filter(old -> old.entityId.equals(entry.getEntityId()))
                        .mapToInt(Entry::alive).findFirst().orElse(0);
            entries.add(new Entry(entry.getEntityId(), entry.getMaxSimultaneous(), alive));
        }
        final Spawner next = new Spawner(List.copyOf(entries), spawner.getLastSpawnTime(), spawner.getSpawnCooldown());
        if (next.equals(previous))
            return;
        if (entries.isEmpty())
            index.spawners.remove(key);
        else
            index.spawners.put(key, next);
        index.setDirty();
    }

    public static void remove(ServerLevel level, BlockPos pos) {
        final MobSpawnerIndex index = get(level.getServer());
        if (index != null && index.spawners.remove(key(level, pos)) != null)
            index.setDirty();
    }

    /**
     * Спавнеры моба. Для спавнеров в загруженных чанках живые мобы пересчитываются прямо сейчас,
     * для выгруженных берётся последний подсчёт.
     */
    public static List<MobSpawnPoint> pointsFor(MinecraftServer server, ResourceLocation entityId) {
        final MobSpawnerIndex index = get(server);
        if (index == null || entityId == null)
            return List.of();
        final long now = System.currentTimeMillis();
        final List<MobSpawnPoint> points = new ArrayList<>();
        for (Map.Entry<Key, Spawner> mapEntry : index.spawners.entrySet()) {
            final Spawner spawner = mapEntry.getValue();
            for (Entry entry : spawner.entries) {
                if (!entry.entityId.equals(entityId))
                    continue;
                final BlockPos pos = BlockPos.of(mapEntry.getKey().pos);
                int alive = entry.alive;
                final ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, mapEntry.getKey().dimension));
                if (level != null && level.isLoaded(pos) && level.getBlockEntity(pos) instanceof MobSpawnerBlockEntity loaded)
                    alive = loaded.countAlive(level, entityId);
                final long remaining = Math.max(0L, spawner.lastSpawnTime + spawner.cooldownTicks * MobSpawnerBlockEntity.MILLIS_PER_TICK - now);
                points.add(new MobSpawnPoint(mapEntry.getKey().dimension, pos, remaining, alive, entry.maxAlive));
            }
        }
        // Сначала спавнеры, где моба можно встретить сейчас, затем — у которых респавн ближе.
        points.sort(Comparator.comparing((MobSpawnPoint point) -> point.alive() <= 0).thenComparingLong(MobSpawnPoint::remainingMillis));
        return points;
    }

    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        final ListTag list = new ListTag();
        for (Map.Entry<Key, Spawner> mapEntry : spawners.entrySet()) {
            final CompoundTag spawnerTag = new CompoundTag();
            spawnerTag.putString("Dim", mapEntry.getKey().dimension.toString());
            spawnerTag.putLong("Pos", mapEntry.getKey().pos);
            spawnerTag.putLong("LastSpawnTime", mapEntry.getValue().lastSpawnTime);
            spawnerTag.putInt("Cooldown", mapEntry.getValue().cooldownTicks);
            final ListTag entries = new ListTag();
            for (Entry entry : mapEntry.getValue().entries) {
                final CompoundTag entryTag = new CompoundTag();
                entryTag.putString("Id", entry.entityId.toString());
                entryTag.putInt("Max", entry.maxAlive);
                entryTag.putInt("Alive", entry.alive);
                entries.add(entryTag);
            }
            spawnerTag.put("Entries", entries);
            list.add(spawnerTag);
        }
        tag.put("Spawners", list);
        return tag;
    }

    private static MobSpawnerIndex load(CompoundTag tag, HolderLookup.Provider registries) {
        final MobSpawnerIndex index = new MobSpawnerIndex();
        final ListTag list = tag.getList("Spawners", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            final CompoundTag spawnerTag = list.getCompound(i);
            final ResourceLocation dimension = ResourceLocation.tryParse(spawnerTag.getString("Dim"));
            if (dimension == null)
                continue;
            final List<Entry> entries = new ArrayList<>();
            final ListTag entriesTag = spawnerTag.getList("Entries", Tag.TAG_COMPOUND);
            for (int j = 0; j < entriesTag.size(); j++) {
                final CompoundTag entryTag = entriesTag.getCompound(j);
                final ResourceLocation entityId = ResourceLocation.tryParse(entryTag.getString("Id"));
                if (entityId != null)
                    entries.add(new Entry(entityId, entryTag.getInt("Max"), entryTag.getInt("Alive")));
            }
            if (!entries.isEmpty())
                index.spawners.put(new Key(dimension, spawnerTag.getLong("Pos")),
                        new Spawner(List.copyOf(entries), spawnerTag.getLong("LastSpawnTime"), spawnerTag.getInt("Cooldown")));
        }
        return index;
    }
}

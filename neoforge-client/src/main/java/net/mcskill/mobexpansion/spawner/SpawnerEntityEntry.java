package net.mcskill.mobexpansion.spawner;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class SpawnerEntityEntry {
    public static final int MIN_INTERVAL = 20;
    public static final int MAX_INTERVAL = 20 * 60 * 60 * 6;
    public static final int MIN_COUNT = 1;
    public static final int MAX_COUNT = 16;
    public static final int MIN_SIMULTANEOUS = 1;
    public static final int MAX_SIMULTANEOUS = 64;
    public static final float MIN_RADIUS = 1.0F;
    public static final float MAX_RADIUS = 64.0F;
    public static final int MIN_SPAWN_Y = -64;
    public static final int MAX_SPAWN_Y = 320;

    private ResourceLocation entityId;
    private int interval = 200;
    private int countMin = 1;
    private int countMax = 1;
    private int maxSimultaneous = 6;
    private float activationRadius = 16.0F;
    private float spawnRadius = 4.0F;
    private int spawnYMin;
    private int spawnYMax;
    private boolean enabled = true;

    public SpawnerEntityEntry(ResourceLocation entityId) {
        this.entityId = entityId;
    }

    public static SpawnerEntityEntry createDefault(ResourceLocation entityId, int defaultY) {
        final SpawnerEntityEntry entry = new SpawnerEntityEntry(entityId);
        entry.spawnYMin = defaultY;
        entry.spawnYMax = defaultY;
        return entry;
    }

    public SpawnerEntityEntry copy() {
        final SpawnerEntityEntry copy = new SpawnerEntityEntry(entityId);
        copy.interval = interval;
        copy.countMin = countMin;
        copy.countMax = countMax;
        copy.maxSimultaneous = maxSimultaneous;
        copy.activationRadius = activationRadius;
        copy.spawnRadius = spawnRadius;
        copy.spawnYMin = spawnYMin;
        copy.spawnYMax = spawnYMax;
        copy.enabled = enabled;
        return copy;
    }

    public ResourceLocation getEntityId() {
        return entityId;
    }

    public void setEntityId(ResourceLocation entityId) {
        this.entityId = entityId;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = Mth.clamp(interval, MIN_INTERVAL, MAX_INTERVAL);
    }

    public int getCountMin() {
        return countMin;
    }

    public void setCountMin(int countMin) {
        this.countMin = Mth.clamp(countMin, MIN_COUNT, MAX_COUNT);
        if (countMax < countMin)
            countMax = countMin;
    }

    public int getCountMax() {
        return countMax;
    }

    public void setCountMax(int countMax) {
        this.countMax = Mth.clamp(Math.max(countMax, this.countMin), MIN_COUNT, MAX_COUNT);
    }

    public int getMaxSimultaneous() {
        return maxSimultaneous;
    }

    public void setMaxSimultaneous(int maxSimultaneous) {
        this.maxSimultaneous = Mth.clamp(maxSimultaneous, MIN_SIMULTANEOUS, MAX_SIMULTANEOUS);
    }

    public float getActivationRadius() {
        return activationRadius;
    }

    public void setActivationRadius(float activationRadius) {
        this.activationRadius = Mth.clamp(activationRadius, MIN_RADIUS, MAX_RADIUS);
    }

    public float getSpawnRadius() {
        return spawnRadius;
    }

    public void setSpawnRadius(float spawnRadius) {
        this.spawnRadius = Mth.clamp(spawnRadius, MIN_RADIUS, MAX_RADIUS);
    }

    public int getSpawnYMin() {
        return spawnYMin;
    }

    public void setSpawnYMin(int spawnYMin) {
        this.spawnYMin = Mth.clamp(spawnYMin, MIN_SPAWN_Y, MAX_SPAWN_Y);
        if (spawnYMax < spawnYMin)
            spawnYMax = spawnYMin;
    }

    public int getSpawnYMax() {
        return spawnYMax;
    }

    public void setSpawnYMax(int spawnYMax) {
        this.spawnYMax = Mth.clamp(Math.max(spawnYMax, this.spawnYMin), MIN_SPAWN_Y, MAX_SPAWN_Y);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void clampAll() {
        setInterval(interval);
        setCountMin(countMin);
        setCountMax(countMax);
        setMaxSimultaneous(maxSimultaneous);
        setActivationRadius(activationRadius);
        setSpawnRadius(spawnRadius);
        setSpawnYMin(spawnYMin);
        setSpawnYMax(spawnYMax);
    }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putString("EntityId", entityId.toString());
        tag.putInt("Interval", interval);
        tag.putInt("CountMin", countMin);
        tag.putInt("CountMax", countMax);
        tag.putInt("MaxSimultaneous", maxSimultaneous);
        tag.putFloat("ActivationRadius", activationRadius);
        tag.putFloat("SpawnRadius", spawnRadius);
        tag.putInt("SpawnYMin", spawnYMin);
        tag.putInt("SpawnYMax", spawnYMax);
        tag.putBoolean("Enabled", enabled);
        return tag;
    }

    @Nullable
    public static SpawnerEntityEntry load(CompoundTag tag, int fallbackY) {
        final ResourceLocation entityId = ResourceLocation.tryParse(tag.getString("EntityId"));
        if (!SpawnerMobCatalog.isSpawnable(entityId))
            return null;
        final SpawnerEntityEntry entry = createDefault(entityId, fallbackY);
        entry.setInterval(tag.contains("Interval") ? tag.getInt("Interval") : 200);
        entry.setCountMin(tag.contains("CountMin") ? tag.getInt("CountMin") : 1);
        entry.setCountMax(tag.contains("CountMax") ? tag.getInt("CountMax") : entry.getCountMin());
        entry.setMaxSimultaneous(tag.contains("MaxSimultaneous") ? tag.getInt("MaxSimultaneous") : 6);
        entry.setActivationRadius(tag.contains("ActivationRadius") ? tag.getFloat("ActivationRadius") : 16.0F);
        entry.setSpawnRadius(tag.contains("SpawnRadius") ? tag.getFloat("SpawnRadius") : 4.0F);
        entry.setSpawnYMin(tag.contains("SpawnYMin") ? tag.getInt("SpawnYMin") : fallbackY);
        entry.setSpawnYMax(tag.contains("SpawnYMax") ? tag.getInt("SpawnYMax") : entry.getSpawnYMin());
        entry.setEnabled(!tag.contains("Enabled") || tag.getBoolean("Enabled"));
        return entry;
    }
}

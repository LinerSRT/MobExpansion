package net.mcskill.mobexpansion.blockentity;

import net.mcskill.mobexpansion.init.MobExBlockEntities;
import net.mcskill.mobexpansion.init.MobExBlocks;
import net.mcskill.mobexpansion.menu.MobSpawnerMenu;
import net.mcskill.mobexpansion.spawner.SpawnMode;
import net.mcskill.mobexpansion.spawner.SpawnerEntityEntry;
import net.mcskill.mobexpansion.spawner.SpawnerMobCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class MobSpawnerBlockEntity extends BlockEntity implements MenuProvider {
    public static final float MIN_ACTIVATION_DIST = 0.0F;
    public static final float MAX_ACTIVATION_DIST = 64.0F;
    public static final int MIN_ACTIVATION_DELAY = 0;
    public static final int MAX_ACTIVATION_DELAY = 20 * 60;
    public static final int HEIGHT_LIMIT_RANGE = 16;
    private final List<SpawnerEntityEntry> entries = new ArrayList<>();
    private SpawnMode spawnMode = SpawnMode.STANDARD;
    private int activationDelay;
    private boolean requireLos;
    private boolean requireDaylight;
    private boolean ignoreSpectators = true;
    private boolean heightLimit;
    private float activationDistMin;
    private float activationDistMax = 16.0F;
    @Nullable
    private ResourceLocation blockUnder;
    @Nullable
    private ResourceLocation blockAround;
    @Nullable
    private BlockState maskState;

    private int spawnCooldown;
    private int nextEntityIndex;
    private int activationDelayLeft;
    private boolean wasActivated;

    public MobSpawnerBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(MobExBlockEntities.MOB_SPAWNER.get(), blockPos, blockState);
        spawnCooldown = 200;
    }

    public List<SpawnerEntityEntry> getEntries() {
        final List<SpawnerEntityEntry> copies = new ArrayList<>(entries.size());
        for (SpawnerEntityEntry entry : entries)
            copies.add(entry.copy());
        return copies;
    }

    public SpawnMode getSpawnMode() {
        return spawnMode;
    }

    public int getActivationDelay() {
        return activationDelay;
    }

    public boolean isRequireLos() {
        return requireLos;
    }

    public boolean isRequireDaylight() {
        return requireDaylight;
    }

    public boolean isIgnoreSpectators() {
        return ignoreSpectators;
    }

    public boolean isHeightLimit() {
        return heightLimit;
    }

    public float getActivationDistMin() {
        return activationDistMin;
    }

    public float getActivationDistMax() {
        return activationDistMax;
    }

    @Nullable
    public ResourceLocation getBlockUnder() {
        return blockUnder;
    }

    @Nullable
    public ResourceLocation getBlockAround() {
        return blockAround;
    }

    @Nullable
    public ResourceLocation getMaskBlock() {
        return maskState == null ? null : BuiltInRegistries.BLOCK.getKey(maskState.getBlock());
    }

    @Nullable
    public BlockState getMaskBlockState() {
        return maskState;
    }

    @Nullable
    public static BlockState sanitizeMaskState(@Nullable BlockState state) {
        if (state == null || state.isAir())
            return null;
        if (state.getBlock() == MobExBlocks.MOB_SPAWNER.get())
            return null;
        return state;
    }

    @Nullable
    public static BlockState maskStateFromBlockId(@Nullable ResourceLocation blockId) {
        if (!isValidBlockId(blockId))
            return null;
        return sanitizeMaskState(BuiltInRegistries.BLOCK.get(blockId).defaultBlockState());
    }

    public static void saveMaskState(CompoundTag tag, @Nullable BlockState maskState) {
        if (maskState == null)
            return;
        tag.put("MaskState", NbtUtils.writeBlockState(maskState));
        tag.putString("MaskBlock", BuiltInRegistries.BLOCK.getKey(maskState.getBlock()).toString());
    }

    @Nullable
    public static BlockState loadMaskState(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("MaskState", Tag.TAG_COMPOUND))
            return sanitizeMaskState(NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("MaskState")));
        if (tag.contains("MaskBlock", Tag.TAG_STRING))
            return maskStateFromBlockId(ResourceLocation.tryParse(tag.getString("MaskBlock")));
        return null;
    }

    public void applySettings(
            List<SpawnerEntityEntry> nextEntries,
            SpawnMode nextSpawnMode,
            int nextActivationDelay,
            boolean nextRequireLos,
            boolean nextRequireDaylight,
            boolean nextIgnoreSpectators,
            boolean nextHeightLimit,
            float nextActivationDistMin,
            float nextActivationDistMax,
            @Nullable ResourceLocation nextBlockUnder,
            @Nullable ResourceLocation nextBlockAround,
            @Nullable BlockState nextMaskState
    ) {
        entries.clear();
        if (nextEntries != null) {
            for (SpawnerEntityEntry entry : nextEntries) {
                if (entry == null || !SpawnerMobCatalog.isSpawnable(entry.getEntityId()))
                    continue;
                if (containsEntityId(entry.getEntityId()))
                    continue;
                final SpawnerEntityEntry copy = entry.copy();
                copy.clampAll();
                entries.add(copy);
            }
        }

        spawnMode = nextSpawnMode == null ? SpawnMode.STANDARD : nextSpawnMode;
        activationDelay = Mth.clamp(nextActivationDelay, MIN_ACTIVATION_DELAY, MAX_ACTIVATION_DELAY);
        requireLos = nextRequireLos;
        requireDaylight = nextRequireDaylight;
        ignoreSpectators = nextIgnoreSpectators;
        heightLimit = nextHeightLimit;
        activationDistMin = Mth.clamp(nextActivationDistMin, MIN_ACTIVATION_DIST, MAX_ACTIVATION_DIST);
        activationDistMax = Mth.clamp(Math.max(nextActivationDistMax, activationDistMin), MIN_ACTIVATION_DIST, MAX_ACTIVATION_DIST);
        blockUnder = isValidBlockId(nextBlockUnder) ? nextBlockUnder : null;
        blockAround = isValidBlockId(nextBlockAround) ? nextBlockAround : null;
        maskState = sanitizeMaskState(nextMaskState);

        if (nextEntityIndex >= entries.size())
            nextEntityIndex = 0;
        spawnCooldown = Math.min(spawnCooldown, Math.max(SpawnerEntityEntry.MIN_INTERVAL, firstEnabledInterval()));
        setChanged();
        if (level != null)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    private boolean containsEntityId(ResourceLocation entityId) {
        for (SpawnerEntityEntry entry : entries) {
            if (entry.getEntityId().equals(entityId))
                return true;
        }
        return false;
    }

    private int firstEnabledInterval() {
        for (SpawnerEntityEntry entry : entries) {
            if (entry.isEnabled())
                return entry.getInterval();
        }
        return 200;
    }

    private static boolean isValidBlockId(@Nullable ResourceLocation blockId) {
        return blockId != null && BuiltInRegistries.BLOCK.containsKey(blockId);
    }

    public static void serverTick(Level level, BlockPos blockPos, BlockState blockState, MobSpawnerBlockEntity spawner) {
        if (!(level instanceof ServerLevel serverLevel))
            return;
        if (!spawner.hasEnabledEntries())
            return;

        final Player activatingPlayer = spawner.findActivatingPlayer(serverLevel, blockPos);
        if (activatingPlayer == null) {
            spawner.wasActivated = false;
            spawner.activationDelayLeft = spawner.activationDelay;
            return;
        }

        if (!spawner.wasActivated) {
            spawner.wasActivated = true;
            spawner.activationDelayLeft = spawner.activationDelay;
        }
        if (spawner.activationDelayLeft > 0) {
            spawner.activationDelayLeft--;
            return;
        }

        if (--spawner.spawnCooldown > 0)
            return;

        final SpawnerEntityEntry selectedEntry = spawner.selectEntry(serverLevel, blockPos, activatingPlayer);
        if (selectedEntry == null) {
            spawner.spawnCooldown = SpawnerEntityEntry.MIN_INTERVAL;
            return;
        }

        spawner.spawnCooldown = selectedEntry.getInterval();
        spawner.trySpawnWave(serverLevel, blockPos, selectedEntry, activatingPlayer);
    }

    private boolean hasEnabledEntries() {
        for (SpawnerEntityEntry entry : entries) {
            if (entry.isEnabled())
                return true;
        }
        return false;
    }

    @Nullable
    private Player findActivatingPlayer(ServerLevel serverLevel, BlockPos blockPos) {
        final double searchRadius = Math.max(activationDistMax, 1.0F) + 1.0D;
        final AABB searchArea = new AABB(blockPos).inflate(searchRadius);
        final Vec3 spawnerCenter = Vec3.atCenterOf(blockPos);
        Player bestPlayer = null;
        double bestDistance = Double.MAX_VALUE;

        for (Player player : serverLevel.getEntitiesOfClass(Player.class, searchArea, LivingEntity::isAlive)) {
            if (ignoreSpectators && player.isSpectator())
                continue;
            final double distance = Math.sqrt(player.distanceToSqr(spawnerCenter));
            if (distance < activationDistMin || distance > activationDistMax)
                continue;
            if (requireDaylight && !isDaytime(serverLevel))
                continue;
            if (heightLimit && Math.abs(player.getY() - blockPos.getY()) > HEIGHT_LIMIT_RANGE)
                continue;
            if (requireLos && !hasLineOfSight(serverLevel, spawnerCenter, player))
                continue;
            if (distance < bestDistance) {
                bestDistance = distance;
                bestPlayer = player;
            }
        }
        return bestPlayer;
    }

    private static boolean isDaytime(ServerLevel serverLevel) {
        final long dayTime = serverLevel.getDayTime() % 24000L;
        return dayTime < 12000L;
    }

    private static boolean hasLineOfSight(ServerLevel serverLevel, Vec3 from, Player player) {
        final Vec3 to = player.getEyePosition();
        final BlockHitResult hitResult = serverLevel.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hitResult.getType() == HitResult.Type.MISS || hitResult.getBlockPos().closerThan(player.blockPosition(), 1.5D);
    }

    @Nullable
    private SpawnerEntityEntry selectEntry(ServerLevel serverLevel, BlockPos blockPos, Player activatingPlayer) {
        final List<SpawnerEntityEntry> candidates = new ArrayList<>();
        for (SpawnerEntityEntry entry : entries) {
            if (!entry.isEnabled())
                continue;
            if (!isPlayerInEntityRadius(blockPos, activatingPlayer, entry))
                continue;
            if (!matchesBlockUnder(serverLevel, blockPos))
                continue;
            candidates.add(entry);
        }
        if (candidates.isEmpty())
            return null;

        if (spawnMode == SpawnMode.RANDOM)
            return candidates.get(serverLevel.random.nextInt(candidates.size()));
        for (int attempt = 0; attempt < entries.size(); attempt++) {
            nextEntityIndex %= entries.size();
            final SpawnerEntityEntry entry = entries.get(nextEntityIndex);
            nextEntityIndex = (nextEntityIndex + 1) % entries.size();
            for (SpawnerEntityEntry candidate : candidates) {
                if (candidate.getEntityId().equals(entry.getEntityId()))
                    return entry;
            }
        }
        return candidates.getFirst();
    }

    private static boolean isPlayerInEntityRadius(BlockPos blockPos, Player player, SpawnerEntityEntry entry) {
        return player.distanceToSqr(Vec3.atCenterOf(blockPos)) <= (double) entry.getActivationRadius() * entry.getActivationRadius();
    }

    private boolean matchesBlockUnder(ServerLevel serverLevel, BlockPos blockPos) {
        if (blockUnder == null)
            return true;
        final Block expected = BuiltInRegistries.BLOCK.get(blockUnder);
        return serverLevel.getBlockState(blockPos.below()).is(expected);
    }

    private boolean matchesBlockAround(ServerLevel serverLevel, BlockPos spawnFeetPos) {
        if (blockAround == null)
            return true;
        final Block expected = BuiltInRegistries.BLOCK.get(blockAround);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (serverLevel.getBlockState(spawnFeetPos.relative(direction)).is(expected))
                return true;
        }
        return false;
    }

    private void trySpawnWave(ServerLevel serverLevel, BlockPos blockPos, SpawnerEntityEntry entry, Player activatingPlayer) {
        if (!SpawnerMobCatalog.isSpawnable(entry.getEntityId()))
            return;

        final double checkRadius = Math.max(entry.getSpawnRadius(), entry.getActivationRadius()) * 2.0D;
        final AABB nearbyArea = new AABB(blockPos).inflate(checkRadius);
        final int nearbyCount = serverLevel.getEntitiesOfClass(
                Mob.class,
                nearbyArea,
                mob -> mob.isAlive() && entry.getEntityId().equals(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()))
        ).size();
        if (nearbyCount >= entry.getMaxSimultaneous())
            return;

        final int waveCount = entry.getCountMin() + serverLevel.random.nextInt(Math.max(1, entry.getCountMax() - entry.getCountMin() + 1));
        int spawnedAmount = 0;
        for (int attempt = 0; attempt < waveCount * 4 && spawnedAmount < waveCount; attempt++) {
            if (nearbyCount + spawnedAmount >= entry.getMaxSimultaneous())
                break;

            final double offsetX = (serverLevel.random.nextDouble() * 2.0D - 1.0D) * entry.getSpawnRadius();
            final double offsetZ = (serverLevel.random.nextDouble() * 2.0D - 1.0D) * entry.getSpawnRadius();
            final double spawnX = blockPos.getX() + 0.5D + offsetX;
            final double spawnZ = blockPos.getZ() + 0.5D + offsetZ;
            final int spawnY = entry.getSpawnYMin() + serverLevel.random.nextInt(Math.max(1, entry.getSpawnYMax() - entry.getSpawnYMin() + 1));
            final BlockPos spawnFeetPos = BlockPos.containing(spawnX, spawnY, spawnZ);

            if (!matchesBlockAround(serverLevel, spawnFeetPos))
                continue;
            if (heightLimit && Math.abs(spawnY - activatingPlayer.getY()) > HEIGHT_LIMIT_RANGE)
                continue;

            final Mob mob = SpawnerMobCatalog.tryCreateMob(entry.getEntityId(), serverLevel);
            if (mob == null)
                continue;

            mob.moveTo(spawnX, spawnY, spawnZ, serverLevel.random.nextFloat() * 360.0F, 0.0F);
            if (!serverLevel.noCollision(mob)) {
                mob.discard();
                continue;
            }

            mob.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.SPAWNER, null);
            mob.setPersistenceRequired();
            serverLevel.addFreshEntityWithPassengers(mob);
            spawnedAmount++;
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag compoundTag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(compoundTag, registries);
        final ListTag entriesTag = new ListTag();
        for (SpawnerEntityEntry entry : entries)
            entriesTag.add(entry.save());
        compoundTag.put("Entries", entriesTag);
        compoundTag.putString("SpawnMode", spawnMode.name());
        compoundTag.putInt("ActivationDelay", activationDelay);
        compoundTag.putBoolean("RequireLos", requireLos);
        compoundTag.putBoolean("RequireDaylight", requireDaylight);
        compoundTag.putBoolean("IgnoreSpectators", ignoreSpectators);
        compoundTag.putBoolean("HeightLimit", heightLimit);
        compoundTag.putFloat("ActivationDistMin", activationDistMin);
        compoundTag.putFloat("ActivationDistMax", activationDistMax);
        if (blockUnder != null)
            compoundTag.putString("BlockUnder", blockUnder.toString());
        if (blockAround != null)
            compoundTag.putString("BlockAround", blockAround.toString());
        saveMaskState(compoundTag, maskState);
        compoundTag.putInt("SpawnCooldown", spawnCooldown);
        compoundTag.putInt("NextEntityIndex", nextEntityIndex);
        compoundTag.putInt("ActivationDelayLeft", activationDelayLeft);
        compoundTag.putBoolean("WasActivated", wasActivated);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag compoundTag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(compoundTag, registries);
        entries.clear();
        final int fallbackY = worldPosition.getY() + 1;
        if (compoundTag.contains("Entries", Tag.TAG_LIST)) {
            final ListTag entriesTag = compoundTag.getList("Entries", Tag.TAG_COMPOUND);
            for (int entryIndex = 0; entryIndex < entriesTag.size(); entryIndex++) {
                final SpawnerEntityEntry spawnerEntry = SpawnerEntityEntry.load(entriesTag.getCompound(entryIndex), fallbackY);
                if (spawnerEntry != null && !containsEntityId(spawnerEntry.getEntityId()))
                    entries.add(spawnerEntry);
            }
        }
        spawnMode = SpawnMode.byName(compoundTag.getString("SpawnMode"));
        activationDelay = Mth.clamp(compoundTag.getInt("ActivationDelay"), MIN_ACTIVATION_DELAY, MAX_ACTIVATION_DELAY);
        requireLos = compoundTag.getBoolean("RequireLos");
        requireDaylight = compoundTag.getBoolean("RequireDaylight");
        ignoreSpectators = !compoundTag.contains("IgnoreSpectators") || compoundTag.getBoolean("IgnoreSpectators");
        heightLimit = compoundTag.getBoolean("HeightLimit");
        activationDistMin = compoundTag.contains("ActivationDistMin") ? Mth.clamp(compoundTag.getFloat("ActivationDistMin"), MIN_ACTIVATION_DIST, MAX_ACTIVATION_DIST) : 0.0F;
        activationDistMax = compoundTag.contains("ActivationDistMax") ? Mth.clamp(Math.max(compoundTag.getFloat("ActivationDistMax"), activationDistMin), MIN_ACTIVATION_DIST, MAX_ACTIVATION_DIST) : 16.0F;
        blockUnder = compoundTag.contains("BlockUnder", Tag.TAG_STRING) ? ResourceLocation.tryParse(compoundTag.getString("BlockUnder")) : null;
        blockAround = compoundTag.contains("BlockAround", Tag.TAG_STRING) ? ResourceLocation.tryParse(compoundTag.getString("BlockAround")) : null;
        maskState = loadMaskState(compoundTag, registries);
        if (!isValidBlockId(blockUnder))
            blockUnder = null;
        if (!isValidBlockId(blockAround))
            blockAround = null;
        spawnCooldown = Math.max(0, compoundTag.getInt("SpawnCooldown"));
        nextEntityIndex = Math.max(0, compoundTag.getInt("NextEntityIndex"));
        activationDelayLeft = Math.max(0, compoundTag.getInt("ActivationDelayLeft"));
        wasActivated = compoundTag.getBoolean("WasActivated");
        if (!entries.isEmpty())
            nextEntityIndex %= entries.size();
    }

    @Override
    @NotNull
    public CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        final CompoundTag compoundTag = new CompoundTag();
        saveAdditional(compoundTag, registries);
        return compoundTag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    @NotNull
    public Component getDisplayName() {
        return Component.translatable("block.mobexpansion.mob_spawner");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new MobSpawnerMenu(containerId, playerInventory, this);
    }
}

package net.mcskill.mobexpansion.menu;

import net.mcskill.mobexpansion.blockentity.MobSpawnerBlockEntity;
import net.mcskill.mobexpansion.init.MobExBlocks;
import net.mcskill.mobexpansion.init.MobExMenus;
import net.mcskill.mobexpansion.spawner.SpawnMode;
import net.mcskill.mobexpansion.spawner.SpawnerEntityEntry;
import net.mcskill.mobexpansion.spawner.SpawnerMobCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MobSpawnerMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final BlockPos blockPos;
    @Nullable
    private final MobSpawnerBlockEntity blockEntity;

    private final List<SpawnerEntityEntry> entries = new ArrayList<>();
    private SpawnMode spawnMode;
    private int activationDelay;
    private boolean requireLos;
    private boolean requireDaylight;
    private boolean ignoreSpectators;
    private boolean heightLimit;
    private float activationDistMin;
    private float activationDistMax;
    @Nullable
    private ResourceLocation blockUnder;
    @Nullable
    private ResourceLocation blockAround;
    @Nullable
    private BlockState maskState;

    public MobSpawnerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory, readFromBuffer(buffer), null);
    }

    public MobSpawnerMenu(int containerId, Inventory playerInventory, MobSpawnerBlockEntity blockEntity) {
        this(containerId, playerInventory, Snapshot.from(blockEntity), blockEntity);
    }

    private MobSpawnerMenu(int containerId, Inventory playerInventory, Snapshot snapshot, @Nullable MobSpawnerBlockEntity blockEntity) {
        super(MobExMenus.MOB_SPAWNER.get(), containerId);
        blockPos = snapshot.blockPos();
        this.blockEntity = blockEntity;
        access = ContainerLevelAccess.create(playerInventory.player.level(), snapshot.blockPos());
        for (SpawnerEntityEntry entry : snapshot.entries())
            entries.add(entry.copy());
        spawnMode = snapshot.spawnMode();
        activationDelay = snapshot.activationDelay();
        requireLos = snapshot.requireLos();
        requireDaylight = snapshot.requireDaylight();
        ignoreSpectators = snapshot.ignoreSpectators();
        heightLimit = snapshot.heightLimit();
        activationDistMin = snapshot.activationDistMin();
        activationDistMax = snapshot.activationDistMax();
        blockUnder = snapshot.blockUnder();
        blockAround = snapshot.blockAround();
        maskState = snapshot.maskState();
    }

    public static void writeInitialData(FriendlyByteBuf buffer, MobSpawnerBlockEntity spawnerBlockEntity) {
        buffer.writeBlockPos(spawnerBlockEntity.getBlockPos());
        final List<SpawnerEntityEntry> entries = spawnerBlockEntity.getEntries();
        buffer.writeVarInt(entries.size());
        for (SpawnerEntityEntry entry : entries)
            writeEntry(buffer, entry);
        buffer.writeUtf(spawnerBlockEntity.getSpawnMode().name());
        buffer.writeVarInt(spawnerBlockEntity.getActivationDelay());
        buffer.writeBoolean(spawnerBlockEntity.isRequireLos());
        buffer.writeBoolean(spawnerBlockEntity.isRequireDaylight());
        buffer.writeBoolean(spawnerBlockEntity.isIgnoreSpectators());
        buffer.writeBoolean(spawnerBlockEntity.isHeightLimit());
        buffer.writeFloat(spawnerBlockEntity.getActivationDistMin());
        buffer.writeFloat(spawnerBlockEntity.getActivationDistMax());
        writeOptionalId(buffer, spawnerBlockEntity.getBlockUnder());
        writeOptionalId(buffer, spawnerBlockEntity.getBlockAround());
        writeOptionalState(buffer, spawnerBlockEntity.getMaskBlockState());
    }

    private static Snapshot readFromBuffer(FriendlyByteBuf buffer) {
        final BlockPos blockPos = buffer.readBlockPos();
        final int entryCount = buffer.readVarInt();
        final List<SpawnerEntityEntry> entries = new ArrayList<>();
        for (int entryIndex = 0; entryIndex < entryCount; entryIndex++) {
            final SpawnerEntityEntry entry = readEntry(buffer, blockPos.getY() + 1);
            if (entry != null)
                entries.add(entry);
        }
        return new Snapshot(
                blockPos,
                entries,
                SpawnMode.byName(buffer.readUtf()),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readFloat(),
                buffer.readFloat(),
                readOptionalId(buffer),
                readOptionalId(buffer),
                readOptionalState(buffer)
        );
    }

    public static void writeEntry(FriendlyByteBuf buffer, SpawnerEntityEntry entry) {
        buffer.writeUtf(entry.getEntityId().toString());
        buffer.writeVarInt(entry.getInterval());
        buffer.writeVarInt(entry.getCountMin());
        buffer.writeVarInt(entry.getCountMax());
        buffer.writeVarInt(entry.getMaxSimultaneous());
        buffer.writeFloat(entry.getActivationRadius());
        buffer.writeFloat(entry.getSpawnRadius());
        buffer.writeVarInt(entry.getSpawnYMin());
        buffer.writeVarInt(entry.getSpawnYMax());
        buffer.writeBoolean(entry.isEnabled());
    }

    @Nullable
    public static SpawnerEntityEntry readEntry(FriendlyByteBuf buffer, int fallbackY) {
        final ResourceLocation entityId = ResourceLocation.tryParse(buffer.readUtf());
        final int interval = buffer.readVarInt();
        final int countMin = buffer.readVarInt();
        final int countMax = buffer.readVarInt();
        final int maxSimultaneous = buffer.readVarInt();
        final float activationRadius = buffer.readFloat();
        final float spawnRadius = buffer.readFloat();
        final int spawnYMin = buffer.readVarInt();
        final int spawnYMax = buffer.readVarInt();
        final boolean enabled = buffer.readBoolean();
        if (!SpawnerMobCatalog.isSpawnable(entityId))
            return null;
        final SpawnerEntityEntry entry = SpawnerEntityEntry.createDefault(entityId, fallbackY);
        entry.setInterval(interval);
        entry.setCountMin(countMin);
        entry.setCountMax(countMax);
        entry.setMaxSimultaneous(maxSimultaneous);
        entry.setActivationRadius(activationRadius);
        entry.setSpawnRadius(spawnRadius);
        entry.setSpawnYMin(spawnYMin);
        entry.setSpawnYMax(spawnYMax);
        entry.setEnabled(enabled);
        return entry;
    }

    @SuppressWarnings("deprecation")
    public static void writeOptionalState(FriendlyByteBuf buffer, @Nullable BlockState state) {
        final BlockState sanitized = MobSpawnerBlockEntity.sanitizeMaskState(state);
        buffer.writeBoolean(sanitized != null);
        if (sanitized != null)
            buffer.writeVarInt(Block.getId(sanitized));
    }

    @Nullable
    @SuppressWarnings("deprecation")
    public static BlockState readOptionalState(FriendlyByteBuf buffer) {
        if (!buffer.readBoolean())
            return null;
        return MobSpawnerBlockEntity.sanitizeMaskState(Block.stateById(buffer.readVarInt()));
    }

    private static void writeOptionalId(FriendlyByteBuf buffer, @Nullable ResourceLocation id) {
        buffer.writeBoolean(id != null);
        if (id != null)
            buffer.writeUtf(id.toString());
    }

    @Nullable
    private static ResourceLocation readOptionalId(FriendlyByteBuf buffer) {
        if (!buffer.readBoolean())
            return null;
        final ResourceLocation id = ResourceLocation.tryParse(buffer.readUtf());
        return id != null && BuiltInRegistries.BLOCK.containsKey(id) ? id : null;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public List<SpawnerEntityEntry> getEntries() {
        return entries;
    }

    public SpawnMode getSpawnMode() {
        return spawnMode;
    }

    public void setSpawnMode(SpawnMode spawnMode) {
        this.spawnMode = spawnMode;
    }

    public int getActivationDelay() {
        return activationDelay;
    }

    public void setActivationDelay(int activationDelay) {
        this.activationDelay = activationDelay;
    }

    public boolean isRequireLos() {
        return requireLos;
    }

    public void setRequireLos(boolean requireLos) {
        this.requireLos = requireLos;
    }

    public boolean isRequireDaylight() {
        return requireDaylight;
    }

    public void setRequireDaylight(boolean requireDaylight) {
        this.requireDaylight = requireDaylight;
    }

    public boolean isIgnoreSpectators() {
        return ignoreSpectators;
    }

    public void setIgnoreSpectators(boolean ignoreSpectators) {
        this.ignoreSpectators = ignoreSpectators;
    }

    public boolean isHeightLimit() {
        return heightLimit;
    }

    public void setHeightLimit(boolean heightLimit) {
        this.heightLimit = heightLimit;
    }

    public float getActivationDistMin() {
        return activationDistMin;
    }

    public void setActivationDistMin(float activationDistMin) {
        this.activationDistMin = activationDistMin;
    }

    public float getActivationDistMax() {
        return activationDistMax;
    }

    public void setActivationDistMax(float activationDistMax) {
        this.activationDistMax = activationDistMax;
    }

    @Nullable
    public ResourceLocation getBlockUnder() {
        return blockUnder;
    }

    public void setBlockUnder(@Nullable ResourceLocation blockUnder) {
        this.blockUnder = blockUnder;
    }

    @Nullable
    public ResourceLocation getBlockAround() {
        return blockAround;
    }

    public void setBlockAround(@Nullable ResourceLocation blockAround) {
        this.blockAround = blockAround;
    }

    @Nullable
    public ResourceLocation getMaskBlock() {
        return maskState == null ? null : BuiltInRegistries.BLOCK.getKey(maskState.getBlock());
    }

    @Nullable
    public BlockState getMaskBlockState() {
        return maskState;
    }

    public void setMaskBlockState(@Nullable BlockState maskState) {
        this.maskState = MobSpawnerBlockEntity.sanitizeMaskState(maskState);
    }

    @Nullable
    public MobSpawnerBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    @NotNull
    public ItemStack quickMoveStack(@NotNull Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(access, player, MobExBlocks.MOB_SPAWNER.get());
    }

    private record Snapshot(
            BlockPos blockPos,
            List<SpawnerEntityEntry> entries,
            SpawnMode spawnMode,
            int activationDelay,
            boolean requireLos,
            boolean requireDaylight,
            boolean ignoreSpectators,
            boolean heightLimit,
            float activationDistMin,
            float activationDistMax,
            @Nullable ResourceLocation blockUnder,
            @Nullable ResourceLocation blockAround,
            @Nullable BlockState maskState
    ) {
        private static Snapshot from(MobSpawnerBlockEntity blockEntity) {
            return new Snapshot(
                    blockEntity.getBlockPos(),
                    blockEntity.getEntries(),
                    blockEntity.getSpawnMode(),
                    blockEntity.getActivationDelay(),
                    blockEntity.isRequireLos(),
                    blockEntity.isRequireDaylight(),
                    blockEntity.isIgnoreSpectators(),
                    blockEntity.isHeightLimit(),
                    blockEntity.getActivationDistMin(),
                    blockEntity.getActivationDistMax(),
                    blockEntity.getBlockUnder(),
                    blockEntity.getBlockAround(),
                    blockEntity.getMaskBlockState()
            );
        }
    }
}

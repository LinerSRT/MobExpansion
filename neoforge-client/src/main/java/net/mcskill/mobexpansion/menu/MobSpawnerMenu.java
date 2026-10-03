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

    public static void writeOptionalState(FriendlyByteBuf buffer, @Nullable BlockState state) {
        final BlockState sanitized = MobSpawnerBlockEntity.sanitizeMaskState(state);
        buffer.writeBoolean(sanitized != null);
        if (sanitized != null)
            buffer.writeVarInt(Block.getId(sanitized));
    }

    @Nullable
    public static BlockState readOptionalState(FriendlyByteBuf buffer) {
        if (!buffer.readBoolean())
            return null;
        return MobSpawnerBlockEntity.sanitizeMaskState(Block.stateById(buffer.readVarInt()));
    }

    @Nullable
    private static ResourceLocation readOptionalId(FriendlyByteBuf buffer) {
        if (!buffer.readBoolean())
            return null;
        final ResourceLocation id = ResourceLocation.tryParse(buffer.readUtf());
        return id != null && BuiltInRegistries.BLOCK.containsKey(id) ? id : null;
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    public List<SpawnerEntityEntry> getEntries() {
        return this.entries;
    }

    public SpawnMode getSpawnMode() {
        return this.spawnMode;
    }

    public int getActivationDelay() {
        return this.activationDelay;
    }

    public boolean isRequireLos() {
        return this.requireLos;
    }

    public boolean isRequireDaylight() {
        return this.requireDaylight;
    }

    public boolean isIgnoreSpectators() {
        return this.ignoreSpectators;
    }

    public boolean isHeightLimit() {
        return this.heightLimit;
    }

    public float getActivationDistMin() {
        return this.activationDistMin;
    }

    public float getActivationDistMax() {
        return this.activationDistMax;
    }

    @Nullable
    public ResourceLocation getBlockUnder() {
        return this.blockUnder;
    }

    @Nullable
    public ResourceLocation getBlockAround() {
        return this.blockAround;
    }

    @Nullable
    public ResourceLocation getMaskBlock() {
        return this.maskState == null ? null : BuiltInRegistries.BLOCK.getKey(this.maskState.getBlock());
    }

    @Nullable
    public BlockState getMaskBlockState() {
        return this.maskState;
    }
    @Nullable
    public MobSpawnerBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    @Override
    @NotNull
    public ItemStack quickMoveStack(@NotNull Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(this.access, player, MobExBlocks.MOB_SPAWNER.get());
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

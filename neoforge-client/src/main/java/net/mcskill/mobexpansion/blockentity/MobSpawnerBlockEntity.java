package net.mcskill.mobexpansion.blockentity;

import net.mcskill.mobexpansion.client.MobSpawnerRevealHandler;
import net.mcskill.mobexpansion.client.model.MobSpawnerCamoBakedModel;
import net.mcskill.mobexpansion.init.MobExBlockEntities;
import net.mcskill.mobexpansion.init.MobExBlocks;
import net.mcskill.mobexpansion.menu.MobSpawnerMenu;
import net.mcskill.mobexpansion.spawner.SpawnMode;
import net.mcskill.mobexpansion.spawner.SpawnerEntityEntry;
import net.minecraft.core.BlockPos;
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
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MobSpawnerBlockEntity extends BlockEntity implements MenuProvider {
    public static final float MIN_ACTIVATION_DIST = 0.0F;
    public static final float MAX_ACTIVATION_DIST = 64.0F;
    public static final int MIN_ACTIVATION_DELAY = 0;
    public static final int MAX_ACTIVATION_DELAY = 20 * 60;
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

    @Override
    @NotNull
    public ModelData getModelData() {
        if (MobSpawnerRevealHandler.isRevealing())
            return ModelData.EMPTY;
        final BlockState maskState = getMaskBlockState();
        if (maskState == null)
            return ModelData.EMPTY;
        return ModelData.builder().with(MobSpawnerCamoBakedModel.MASK, maskState).build();
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

    private boolean containsEntityId(ResourceLocation entityId) {
        for (SpawnerEntityEntry entry : entries) {
            if (entry.getEntityId().equals(entityId))
                return true;
        }
        return false;
    }

    private static boolean isValidBlockId(@Nullable ResourceLocation blockId) {
        return blockId != null && BuiltInRegistries.BLOCK.containsKey(blockId);
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
        requestModelDataUpdate();
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

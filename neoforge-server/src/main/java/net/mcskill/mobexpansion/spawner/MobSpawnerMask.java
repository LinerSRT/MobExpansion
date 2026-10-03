package net.mcskill.mobexpansion.spawner;

import io.netty.buffer.Unpooled;
import net.mcskill.mobexpansion.blockentity.MobSpawnerBlockEntity;
import net.mcskill.mobexpansion.init.MobExBlockEntities;
import net.mcskill.mobexpansion.init.MobExBlocks;
import net.mcskill.mobexpansion.init.MobExItems;
import net.mcskill.mobexpansion.mixin.ChunkBlockEntityInfoAccessor;
import net.mcskill.mobexpansion.mixin.LevelChunkSectionAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("resource")
public final class MobSpawnerMask {
    private static final ThreadLocal<ServerPlayer> TARGET = new ThreadLocal<>();

    private MobSpawnerMask() {
    }

    public static boolean canReveal(ServerPlayer player) {
        return player != null && player.hasPermissions(4) && player.getInventory().getSelected().is(MobExItems.MOB_SPAWNER.get());
    }

    public static void pushTarget(ServerPlayer player) {
        TARGET.set(player);
    }

    public static void popTarget() {
        TARGET.remove();
    }

    @Nullable
    public static ServerPlayer currentTarget() {
        return TARGET.get();
    }

    public static boolean shouldHide() {
        final ServerPlayer player = TARGET.get();
        return player != null && !canReveal(player);
    }

    @Nullable
    public static Packet<?> filterOutgoing(ServerPlayer player, Packet<?> packet) {
        if (canReveal(player))
            return packet;
        if (packet instanceof ClientboundBlockUpdatePacket blockUpdate) {
            final BlockState blockState = maskOriginalState(player.serverLevel(), blockUpdate.getPos(), blockUpdate.getBlockState());
            if (blockState == blockUpdate.getBlockState())
                return packet;
            return new ClientboundBlockUpdatePacket(blockUpdate.getPos(), blockState);
        }
        if (packet instanceof ClientboundBlockEntityDataPacket blockEntityData && isMaskedSpawnerPacket(player.serverLevel(), blockEntityData))
            return null;
        return packet;
    }

    public static BlockState maskOriginalState(BlockGetter level, BlockPos pos, BlockState originalState) {
        if (!originalState.is(MobExBlocks.MOB_SPAWNER.get()))
            return originalState;
        final BlockState maskState = maskState(level, pos);
        return maskState == null ? originalState : maskState;
    }

    @Nullable
    public static BlockState maskState(BlockGetter level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof MobSpawnerBlockEntity spawner))
            return null;
        return spawner.getMaskBlockState();
    }

    @Nullable
    public static VoxelShape maskShape(BlockGetter level, BlockPos pos, CollisionContext context) {
        final BlockState maskState = maskState(level, pos);
        return maskState == null ? null : maskState.getShape(level, pos, context);
    }

    @Nullable
    public static VoxelShape maskCollisionShape(BlockGetter level, BlockPos pos, CollisionContext context) {
        final BlockState maskState = maskState(level, pos);
        return maskState == null ? null : maskState.getCollisionShape(level, pos, context);
    }

    public static boolean isMaskedSpawnerInfo(Object blockEntityInfo) {
        final ChunkBlockEntityInfoAccessor accessor = (ChunkBlockEntityInfoAccessor) blockEntityInfo;
        final BlockEntityType<?> type = accessor.entityType();
        if (type != MobExBlockEntities.MOB_SPAWNER.get())
            return false;
        final CompoundTag compoundTag = accessor.entityTag();
        return compoundTag != null && (compoundTag.contains("MaskState", Tag.TAG_COMPOUND) || compoundTag.contains("MaskBlock", Tag.TAG_STRING));
    }

    public static void writeCamouflagedChunk(FriendlyByteBuf buffer, LevelChunk chunk) {
        final LevelChunkSection[] sections = chunk.getSections();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            final LevelChunkSection section = sections[sectionIndex];
            final int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex);
            writeCamouflagedSection(buffer, chunk, section, sectionY);
        }
    }

    private static void writeCamouflagedSection(FriendlyByteBuf buffer, LevelChunk chunk, LevelChunkSection section, int sectionY) {
        final LevelChunkSectionAccessor accessor = (LevelChunkSectionAccessor) section;
        if (!section.maybeHas(state -> state.is(MobExBlocks.MOB_SPAWNER.get()))) {
            section.write(buffer);
            return;
        }
        final PalettedContainer<BlockState> states = copyStatesForWrite(accessor.chunkStates());
        boolean changed = false;
        final int originX = chunk.getPos().getMinBlockX();
        final int originY = sectionY << 4;
        final int originZ = chunk.getPos().getMinBlockZ();
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    final BlockState state = states.get(x, y, z);
                    if (!state.is(MobExBlocks.MOB_SPAWNER.get()))
                        continue;
                    final BlockState maskState = maskState(chunk, new BlockPos(originX + x, originY + y, originZ + z));
                    if (maskState == null)
                        continue;
                    states.set(x, y, z, maskState);
                    changed = true;
                }
            }
        }
        if (!changed) {
            section.write(buffer);
            return;
        }
        buffer.writeShort(accessor.chunNonEmptyBlockCount());
        states.write(buffer);
        final PalettedContainerRO<Holder<Biome>> biomes = accessor.chunkBiomes();
        biomes.write(buffer);
    }

    private static PalettedContainer<BlockState> copyStatesForWrite(PalettedContainer<BlockState> original) {
        final PalettedContainer<BlockState> copy = original.recreate();
        final FriendlyByteBuf packed = new FriendlyByteBuf(Unpooled.buffer());
        try {
            original.write(packed);
            copy.read(packed);
            return copy;
        } finally {
            packed.release();
        }
    }

    private static boolean isMaskedSpawnerPacket(ServerLevel level, ClientboundBlockEntityDataPacket packet) {
        if (packet.getType() != MobExBlockEntities.MOB_SPAWNER.get())
            return false;
        return maskState(level, packet.getPos()) != null;
    }

    public static void resyncTrackedSpawners(ServerPlayer player, boolean reveal) {
        final ServerLevel level = player.serverLevel();
        player.getChunkTrackingView().forEach(chunkPos -> {
            if (!level.hasChunk(chunkPos.x, chunkPos.z))
                return;
            final LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
            for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                if (!(blockEntity instanceof MobSpawnerBlockEntity spawner))
                    continue;
                final BlockState maskState = spawner.getMaskBlockState();
                if (maskState == null)
                    continue;
                final BlockPos pos = spawner.getBlockPos();
                if (reveal) {
                    player.connection.send(new ClientboundBlockUpdatePacket(pos, spawner.getBlockState()));
                    player.connection.send(ClientboundBlockEntityDataPacket.create(spawner));
                } else
                    player.connection.send(new ClientboundBlockUpdatePacket(pos, maskState));
            }
        });
    }
}

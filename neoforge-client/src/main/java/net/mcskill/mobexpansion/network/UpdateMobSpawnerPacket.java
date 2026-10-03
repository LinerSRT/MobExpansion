package net.mcskill.mobexpansion.network;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.blockentity.MobSpawnerBlockEntity;
import net.mcskill.mobexpansion.menu.MobSpawnerMenu;
import net.mcskill.mobexpansion.spawner.SpawnMode;
import net.mcskill.mobexpansion.spawner.SpawnerEntityEntry;
import net.mcskill.mobexpansion.spawner.SpawnerMobCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record UpdateMobSpawnerPacket(
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
) implements CustomPacketPayload {
    public static final Type<UpdateMobSpawnerPacket> TYPE = new Type<>(Core.loc("update_mob_spawner"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateMobSpawnerPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                buffer.writeBlockPos(packet.blockPos());
                buffer.writeVarInt(packet.entries().size());
                for (SpawnerEntityEntry entry : packet.entries())
                    MobSpawnerMenu.writeEntry(buffer, entry);
                buffer.writeUtf(packet.spawnMode().name());
                buffer.writeVarInt(packet.activationDelay());
                buffer.writeBoolean(packet.requireLos());
                buffer.writeBoolean(packet.requireDaylight());
                buffer.writeBoolean(packet.ignoreSpectators());
                buffer.writeBoolean(packet.heightLimit());
                buffer.writeFloat(packet.activationDistMin());
                buffer.writeFloat(packet.activationDistMax());
                writeOptionalId(buffer, packet.blockUnder());
                writeOptionalId(buffer, packet.blockAround());
                MobSpawnerMenu.writeOptionalState(buffer, packet.maskState());
            },
            buffer -> {
                final BlockPos blockPos = buffer.readBlockPos();
                final int entryCount = buffer.readVarInt();
                final List<SpawnerEntityEntry> entries = new ArrayList<>();
                for (int entryIndex = 0; entryIndex < entryCount; entryIndex++) {
                    final SpawnerEntityEntry entry = MobSpawnerMenu.readEntry(buffer, blockPos.getY() + 1);
                    if (entry != null && SpawnerMobCatalog.isSpawnable(entry.getEntityId()))
                        entries.add(entry);
                }
                return new UpdateMobSpawnerPacket(
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
                        MobSpawnerMenu.readOptionalState(buffer)
                );
            }
    );

    private static void writeOptionalId(RegistryFriendlyByteBuf buffer, @Nullable ResourceLocation id) {
        buffer.writeBoolean(id != null);
        if (id != null)
            buffer.writeUtf(id.toString());
    }

    @Nullable
    private static ResourceLocation readOptionalId(RegistryFriendlyByteBuf buffer) {
        if (!buffer.readBoolean())
            return null;
        final ResourceLocation id = ResourceLocation.tryParse(buffer.readUtf());
        return id != null && BuiltInRegistries.BLOCK.containsKey(id) ? id : null;
    }

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateMobSpawnerPacket packet, IPayloadContext context) {

    }
}

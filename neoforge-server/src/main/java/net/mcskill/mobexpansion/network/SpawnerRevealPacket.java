package net.mcskill.mobexpansion.network;

import net.mcskill.mobexpansion.Core;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SpawnerRevealPacket(boolean allowed) implements CustomPacketPayload {
    public static final Type<SpawnerRevealPacket> TYPE = new Type<>(Core.loc("spawner_reveal"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpawnerRevealPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SpawnerRevealPacket::allowed,
            SpawnerRevealPacket::new
    );

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SpawnerRevealPacket packet, IPayloadContext context) {
    }
}

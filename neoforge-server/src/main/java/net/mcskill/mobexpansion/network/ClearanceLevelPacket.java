package net.mcskill.mobexpansion.network;

import net.mcskill.mobexpansion.Core;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClearanceLevelPacket(int level) implements CustomPacketPayload {
    public static final Type<ClearanceLevelPacket> TYPE = new Type<>(Core.loc("clearance_level"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClearanceLevelPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ClearanceLevelPacket::level,
            ClearanceLevelPacket::new
    );

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClearanceLevelPacket packet, IPayloadContext context) {
    }
}

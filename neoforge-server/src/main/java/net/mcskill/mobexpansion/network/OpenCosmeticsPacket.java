package net.mcskill.mobexpansion.network;

import net.mcskill.mobexpansion.Core;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * Сервер просит клиента открыть GUI кастомизации (команда /cosmetics). Клиент открывает экран и сам запрашивает
 * состояние пакетом {@link CosmeticsRequestPacket}.
 */
public record OpenCosmeticsPacket() implements CustomPacketPayload {
    public static final Type<OpenCosmeticsPacket> TYPE = new Type<>(Core.loc("open_cosmetics"));
    public static final OpenCosmeticsPacket INSTANCE = new OpenCosmeticsPacket();
    public static final StreamCodec<ByteBuf, OpenCosmeticsPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenCosmeticsPacket packet, IPayloadContext context) {
    }
}

package net.mcskill.mobexpansion.network;

import net.mcskill.mobexpansion.Core;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Клиент открыл GUI кастомизации и просит состояние ({@link CosmeticsStatePacket}).
 * Обработчик ставит серверная часть ({@code cosmetics.Cosmetics}) при старте сервера — так пакет работает и на
 * выделенном сервере, и в одиночной игре (там клиентская копия класса перекрывает серверную).
 */
public record CosmeticsRequestPacket() implements CustomPacketPayload {
    public static final Type<CosmeticsRequestPacket> TYPE = new Type<>(Core.loc("cosmetics_request"));
    public static final CosmeticsRequestPacket INSTANCE = new CosmeticsRequestPacket();
    public static final StreamCodec<ByteBuf, CosmeticsRequestPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static Consumer<ServerPlayer> serverHandler = player -> {
    };

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CosmeticsRequestPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player)
                serverHandler.accept(player);
        });
    }
}

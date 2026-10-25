package net.mcskill.mobexpansion.network;

import net.mcskill.mobexpansion.Core;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

/**
 * Клиент открыл страницу моба в EMI и просит актуальные спавнеры/таймеры ({@link MobSpawnInfoPacket}).
 * Обработчик ставит серверная часть ({@code info.MobInfoSync}) при старте сервера — так пакет работает и на
 * выделенном сервере, и в одиночной игре (там клиентская копия класса перекрывает серверную).
 */
public record MobSpawnInfoRequestPacket(ResourceLocation entityId) implements CustomPacketPayload {
    public static final Type<MobSpawnInfoRequestPacket> TYPE = new Type<>(Core.loc("mob_spawn_info_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MobSpawnInfoRequestPacket> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, MobSpawnInfoRequestPacket::entityId,
            MobSpawnInfoRequestPacket::new
    );

    public static BiConsumer<ServerPlayer, ResourceLocation> serverHandler = (player, entityId) -> {
    };

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MobSpawnInfoRequestPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player)
                serverHandler.accept(player, packet.entityId());
        });
    }
}

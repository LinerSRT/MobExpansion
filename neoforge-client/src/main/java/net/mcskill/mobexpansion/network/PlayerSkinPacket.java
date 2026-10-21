package net.mcskill.mobexpansion.network;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.client.skin.ClientSkins;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Скин игрока {@code player} (пустая строка — обычный вид). Сервер шлёт его самому игроку и всем, кто его видит.
 */
public record PlayerSkinPacket(UUID player, String skin) implements CustomPacketPayload {
    public static final Type<PlayerSkinPacket> TYPE = new Type<>(Core.loc("player_skin"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerSkinPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, PlayerSkinPacket::player,
            ByteBufCodecs.STRING_UTF8, PlayerSkinPacket::skin,
            PlayerSkinPacket::new
    );

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlayerSkinPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientSkins.set(packet.player(), packet.skin()));
    }
}

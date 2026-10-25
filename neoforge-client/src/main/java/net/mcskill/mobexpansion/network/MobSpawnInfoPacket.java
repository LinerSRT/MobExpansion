package net.mcskill.mobexpansion.network;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.client.info.ClientMobInfo;
import net.mcskill.mobexpansion.info.MobSpawnPoint;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Ответ на {@link MobSpawnInfoRequestPacket}: спавнеры моба с таймерами.
 */
public record MobSpawnInfoPacket(ResourceLocation entityId, List<MobSpawnPoint> points) implements CustomPacketPayload {
    public static final Type<MobSpawnInfoPacket> TYPE = new Type<>(Core.loc("mob_spawn_info"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MobSpawnInfoPacket> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, MobSpawnInfoPacket::entityId,
            MobSpawnPoint.STREAM_CODEC.apply(ByteBufCodecs.list()), MobSpawnInfoPacket::points,
            MobSpawnInfoPacket::new
    );

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MobSpawnInfoPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientMobInfo.setSpawnPoints(packet.entityId(), packet.points()));
    }
}

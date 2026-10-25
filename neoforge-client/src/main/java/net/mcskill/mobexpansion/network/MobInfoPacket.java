package net.mcskill.mobexpansion.network;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.client.info.ClientMobInfo;
import net.mcskill.mobexpansion.info.MobInfo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Сводки всех мобов для EMI (характеристики, допуск, дроп). Уходит при входе и после сохранения настройщика мобов.
 */
public record MobInfoPacket(List<MobInfo> mobs) implements CustomPacketPayload {
    public static final Type<MobInfoPacket> TYPE = new Type<>(Core.loc("mob_info"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MobInfoPacket> STREAM_CODEC = StreamCodec.composite(
            MobInfo.STREAM_CODEC.apply(ByteBufCodecs.list()), MobInfoPacket::mobs,
            MobInfoPacket::new
    );

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MobInfoPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientMobInfo.setMobs(packet.mobs()));
    }
}

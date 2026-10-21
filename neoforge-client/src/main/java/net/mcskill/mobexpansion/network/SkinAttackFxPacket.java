package net.mcskill.mobexpansion.network;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.client.skin.SkinAttackFx;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * Игрок со скином ударил сущность: клиенты рисуют эффект удара скина.
 *
 * @param strength заряд удара 0..1 (как {@code Player#getAttackStrengthScale})
 * @param critical критический удар (в прыжке)
 */
public record SkinAttackFxPacket(int entityId, float strength, boolean critical) implements CustomPacketPayload {
    public static final Type<SkinAttackFxPacket> TYPE = new Type<>(Core.loc("skin_attack_fx"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SkinAttackFxPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SkinAttackFxPacket::entityId,
            ByteBufCodecs.FLOAT, SkinAttackFxPacket::strength,
            ByteBufCodecs.BOOL, SkinAttackFxPacket::critical,
            SkinAttackFxPacket::new
    );

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SkinAttackFxPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> SkinAttackFx.onAttack(packet.entityId(), packet.strength(), packet.critical()));
    }
}

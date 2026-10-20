package net.mcskill.mobexpansion.network;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.client.ClientClearance;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Таблица «id типа моба → требуемый уровень допуска» с сервера.
 */
public record MobClearancePacket(Map<ResourceLocation, Integer> required) implements CustomPacketPayload {
    public static final Type<MobClearancePacket> TYPE = new Type<>(Core.loc("mob_clearance"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MobClearancePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.VAR_INT), MobClearancePacket::required,
            MobClearancePacket::new
    );

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MobClearancePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientClearance.setRequirements(packet.required()));
    }
}

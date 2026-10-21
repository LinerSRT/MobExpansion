package net.mcskill.mobexpansion.network;

import net.mcskill.mobexpansion.client.cosmetics.CosmeticsClient;
import net.mcskill.mobexpansion.Core;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Что игрок может выбрать в GUI кастомизации: скины и шлейфы (id + открыт ли), что сейчас надето и есть ли шлейфы
 * на сервере вообще (их даёт MSEss).
 */
public record CosmeticsStatePacket(List<Entry> skins, List<Entry> trails, String skin, String trail, boolean trailsAvailable)
        implements CustomPacketPayload {
    public static final Type<CosmeticsStatePacket> TYPE = new Type<>(Core.loc("cosmetics_state"));

    /** Карточка: id и доступна ли игроку. */
    public record Entry(String id, boolean unlocked) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Entry::id,
                ByteBufCodecs.BOOL, Entry::unlocked,
                Entry::new
        );
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, CosmeticsStatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, Entry.STREAM_CODEC), CosmeticsStatePacket::skins,
            ByteBufCodecs.collection(ArrayList::new, Entry.STREAM_CODEC), CosmeticsStatePacket::trails,
            ByteBufCodecs.STRING_UTF8, CosmeticsStatePacket::skin,
            ByteBufCodecs.STRING_UTF8, CosmeticsStatePacket::trail,
            ByteBufCodecs.BOOL, CosmeticsStatePacket::trailsAvailable,
            CosmeticsStatePacket::new
    );

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CosmeticsStatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> CosmeticsClient.onState(packet));
    }
}

package net.mcskill.mobexpansion.network;

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
 * на сервере вообще (их даёт MSEss), префиксы LuckPerms и какой включён.
 */
public record CosmeticsStatePacket(List<Entry> skins, List<Entry> trails, String skin, String trail, boolean trailsAvailable,
                                   List<Entry> prefixes, String prefix, boolean prefixesAvailable)
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

    private static final StreamCodec<RegistryFriendlyByteBuf, List<Entry>> ENTRIES = ByteBufCodecs.collection(ArrayList::new, Entry.STREAM_CODEC);

    // composite() держит максимум 6 полей, здесь их 8
    public static final StreamCodec<RegistryFriendlyByteBuf, CosmeticsStatePacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                ENTRIES.encode(buf, packet.skins);
                ENTRIES.encode(buf, packet.trails);
                ByteBufCodecs.STRING_UTF8.encode(buf, packet.skin);
                ByteBufCodecs.STRING_UTF8.encode(buf, packet.trail);
                buf.writeBoolean(packet.trailsAvailable);
                ENTRIES.encode(buf, packet.prefixes);
                ByteBufCodecs.STRING_UTF8.encode(buf, packet.prefix);
                buf.writeBoolean(packet.prefixesAvailable);
            },
            buf -> new CosmeticsStatePacket(ENTRIES.decode(buf), ENTRIES.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf), buf.readBoolean(),
                    ENTRIES.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf), buf.readBoolean())
    );

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CosmeticsStatePacket packet, IPayloadContext context) {
    }
}

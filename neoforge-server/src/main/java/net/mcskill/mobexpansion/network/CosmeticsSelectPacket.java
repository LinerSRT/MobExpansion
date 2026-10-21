package net.mcskill.mobexpansion.network;

import net.mcskill.mobexpansion.Core;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

/**
 * Игрок выбрал карточку в GUI кастомизации. {@code id} пустой — снять.
 * Обработчик ставит серверная часть ({@code cosmetics.Cosmetics}) — см. {@link CosmeticsRequestPacket}.
 */
public record CosmeticsSelectPacket(Category category, String id) implements CustomPacketPayload {
    public static final Type<CosmeticsSelectPacket> TYPE = new Type<>(Core.loc("cosmetics_select"));

    /** Строки GUI. Новые категории (скины предметов, питомцы) — добавлять в конец, порядок = номер в пакете. */
    public enum Category {
        SKIN, TRAIL
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, CosmeticsSelectPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(index -> Category.values()[index], Category::ordinal), CosmeticsSelectPacket::category,
            ByteBufCodecs.STRING_UTF8, CosmeticsSelectPacket::id,
            CosmeticsSelectPacket::new
    );

    public static BiConsumer<ServerPlayer, CosmeticsSelectPacket> serverHandler = (player, packet) -> {
    };

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CosmeticsSelectPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player)
                serverHandler.accept(player, packet);
        });
    }
}

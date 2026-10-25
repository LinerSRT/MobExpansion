package net.mcskill.mobexpansion.info;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * Спавнер MobExpansion, в котором есть моб (для строк «Расположение» и «До респавна» в EMI).
 * Класс одинаковый в {@code neoforge-server} и {@code neoforge-client}.
 *
 * @param remainingMillis сколько осталось до следующей волны спавнера на момент отправки (0 — КД прошёл)
 * @param alive           живых мобов этого типа у спавнера (последний известный подсчёт)
 * @param maxAlive        лимит одновременных мобов этого типа у спавнера
 */
public record MobSpawnPoint(ResourceLocation dimension, BlockPos pos, long remainingMillis, int alive, int maxAlive) {
    public static final StreamCodec<RegistryFriendlyByteBuf, MobSpawnPoint> STREAM_CODEC = StreamCodec.of(
            (buf, point) -> {
                buf.writeResourceLocation(point.dimension);
                buf.writeBlockPos(point.pos);
                buf.writeVarLong(Math.max(0L, point.remainingMillis));
                buf.writeVarInt(point.alive);
                buf.writeVarInt(point.maxAlive);
            },
            buf -> new MobSpawnPoint(buf.readResourceLocation(), buf.readBlockPos(), buf.readVarLong(), buf.readVarInt(), buf.readVarInt())
    );

    public boolean isFull() {
        return maxAlive > 0 && alive >= maxAlive;
    }
}

package net.mcskill.mobexpansion.clearance;

import net.mcskill.mobexpansion.init.MobExAttachments;
import net.mcskill.mobexpansion.network.ClearanceLevelPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Серверное API уровня допуска игрока.
 * <p>
 * Уровень — целое число {@code 0..MAX_LEVEL}. Моб с требуемым уровнем выше уровня игрока не получает от игрока урон
 * (см. {@link MobClearance}). Любое изменение уровня через {@link #set}/{@link #add} сразу синхронизируется клиенту.
 */
public final class Clearance {
    public static final int MIN_LEVEL = 0;
    public static final int MAX_LEVEL = 5;

    private Clearance() {
    }

    public static int clamp(int level) {
        return Mth.clamp(level, MIN_LEVEL, MAX_LEVEL);
    }

    /**
     * Уровень допуска игрока. На клиенте у чужих/локальных игроков атачмент не синхронизируется —
     * для клиента используйте клиентское состояние, пришедшее пакетом.
     */
    public static int get(Player player) {
        return clamp(player.getData(MobExAttachments.CLEARANCE_LEVEL));
    }

    /**
     * Устанавливает уровень допуска (с ограничением {@code 0..MAX_LEVEL}) и отправляет его клиенту.
     *
     * @return итоговый уровень
     */
    public static int set(ServerPlayer player, int level) {
        final int clamped = clamp(level);
        player.setData(MobExAttachments.CLEARANCE_LEVEL, clamped);
        sync(player);
        return clamped;
    }

    /**
     * Прибавляет {@code delta} (может быть отрицательным) к уровню допуска.
     *
     * @return итоговый уровень
     */
    public static int add(ServerPlayer player, int delta) {
        return set(player, get(player) + delta);
    }

    /**
     * Повышает уровень до {@code level}, если текущий ниже. Удобно для выдачи допуска за квесты/предметы.
     *
     * @return итоговый уровень
     */
    public static int raiseTo(ServerPlayer player, int level) {
        final int current = get(player);
        return level > current ? set(player, level) : current;
    }

    public static boolean hasAccess(Player player, int requiredLevel) {
        return get(player) >= requiredLevel;
    }

    public static boolean canDamage(Player player, Entity target) {
        return hasAccess(player, MobClearance.getRequired(target));
    }

    public static void sync(ServerPlayer player) {
        if (player.connection != null)
            PacketDistributor.sendToPlayer(player, new ClearanceLevelPacket(get(player)));
    }
}

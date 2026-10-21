package net.mcskill.mobexpansion.skin;

import net.mcskill.mobexpansion.init.MobExAttachments;
import net.mcskill.mobexpansion.network.PlayerSkinPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Серверное API скинов игрока: игрок со скином рисуется у всех клиентов другой моделью (см. клиентский
 * {@code client.skin.SkinRenderers}). Сам скин — только внешний вид: хитбокс, характеристики и броня не меняются.
 * <p>
 * Синхронизация: при смене скина — самому игроку и всем, кто его видит; новым наблюдателям — при начале
 * отслеживания (см. {@code MobExSkinEvents}).
 */
public final class PlayerSkins {
    public static final String NONE = "";
    /** Доступные скины. Клиент должен знать каждый (модель, текстура, анимации). */
    public static final List<String> SKINS = List.of("capra", "lillith");

    private PlayerSkins() {
    }

    public static boolean exists(String skin) {
        return SKINS.contains(skin);
    }

    /** Скин игрока или {@link #NONE}. На клиенте атачмент не синхронизируется — там спрашивайте ClientSkins. */
    public static String get(Player player) {
        return player.getData(MobExAttachments.PLAYER_SKIN);
    }

    public static boolean hasSkin(Player player) {
        return !get(player).isEmpty();
    }

    /** Ставит скин ({@link #NONE} — снять) и рассылает его игроку и всем, кто его видит. */
    public static void set(ServerPlayer player, String skin) {
        player.setData(MobExAttachments.PLAYER_SKIN, skin);
        broadcast(player);
    }

    public static void broadcast(ServerPlayer player) {
        if (player.connection != null)
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, packet(player));
    }

    /** Отправить скин игрока {@code owner} одному наблюдателю. */
    public static void sendTo(ServerPlayer owner, ServerPlayer viewer) {
        if (viewer.connection != null)
            PacketDistributor.sendToPlayer(viewer, packet(owner));
    }

    private static PlayerSkinPacket packet(ServerPlayer player) {
        return new PlayerSkinPacket(player.getUUID(), get(player));
    }
}

package net.mcskill.mobexpansion.client.skin;

import net.mcskill.mobexpansion.Core;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Клиентская копия скинов игроков (приходят с сервера пакетом PlayerSkinPacket). Ключ — UUID, а не id сущности:
 * пакет может прийти раньше, чем сущность появится, и id меняется после смерти/смены измерения.
 */
@EventBusSubscriber(modid = Core.MODID, value = Dist.CLIENT)
public final class ClientSkins {
    private static final Map<UUID, String> SKINS = new ConcurrentHashMap<>();

    private ClientSkins() {
    }

    public static void set(UUID player, String skin) {
        if (skin.isEmpty())
            SKINS.remove(player);
        else
            SKINS.put(player, skin);
    }

    /** Скин игрока или пустая строка (во время {@link #renderWithSkin} — скин превью). */
    public static String get(Player player) {
        final UUID preview = previewPlayer;
        if (preview != null && preview.equals(player.getUUID()))
            return previewSkin;
        return SKINS.getOrDefault(player.getUUID(), "");
    }

    private static UUID previewPlayer;
    private static String previewSkin = "";

    /**
     * Нарисовать игрока в другом скине (превью в GUI кастомизации): пока выполняется {@code render},
     * {@link #get} для этого игрока возвращает {@code skin} (пустая строка — обычный вид).
     */
    public static void renderWithSkin(Player player, String skin, Runnable render) {
        previewPlayer = player.getUUID();
        previewSkin = skin;
        try {
            render.run();
        } finally {
            previewPlayer = null;
            previewSkin = "";
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SKINS.clear();
    }
}

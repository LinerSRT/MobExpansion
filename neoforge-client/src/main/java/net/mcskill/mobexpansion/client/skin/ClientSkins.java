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

    /** Скин игрока или пустая строка. */
    public static String get(Player player) {
        return SKINS.getOrDefault(player.getUUID(), "");
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SKINS.clear();
    }
}

package net.mcskill.mobexpansion.client.info;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.client.emi.MobExEmiCompat;
import net.mcskill.mobexpansion.info.MobInfo;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import java.util.List;

/**
 * Клиентская копия сводок мобов для EMI ({@code MobInfoPacket}).
 */
@EventBusSubscriber(modid = Core.MODID, value = Dist.CLIENT)
public final class ClientMobInfo {
    private static volatile List<MobInfo> mobs = List.of();

    private ClientMobInfo() {
    }

    public static List<MobInfo> mobs() {
        return mobs;
    }

    public static void setMobs(List<MobInfo> newMobs) {
        final List<MobInfo> copy = List.copyOf(newMobs);
        if (copy.equals(mobs))
            return;
        mobs = copy;
        if (ModList.get().isLoaded("emi"))
            MobExEmiCompat.reload();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        mobs = List.of();
    }
}

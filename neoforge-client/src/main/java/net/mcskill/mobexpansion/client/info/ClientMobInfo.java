package net.mcskill.mobexpansion.client.info;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.client.emi.MobExEmiCompat;
import net.mcskill.mobexpansion.info.MobInfo;
import net.mcskill.mobexpansion.info.MobSpawnPoint;
import net.mcskill.mobexpansion.network.MobSpawnInfoRequestPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Клиентская копия сводок мобов для EMI ({@code MobInfoPacket}) и спавнеров с таймерами ({@code MobSpawnInfoPacket}).
 */
@EventBusSubscriber(modid = Core.MODID, value = Dist.CLIENT)
public final class ClientMobInfo {
    private static final long REQUEST_INTERVAL_MILLIS = 1000L;

    private static volatile List<MobInfo> mobs = List.of();
    private static final Map<ResourceLocation, SpawnPoints> SPAWNS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Long> LAST_REQUEST = new ConcurrentHashMap<>();

    /** Спавнеры моба и момент получения (таймеры отсчитываются от него). */
    public record SpawnPoints(List<MobSpawnPoint> points, long receivedAt) {
        public long remainingMillis(MobSpawnPoint point) {
            return Math.max(0L, point.remainingMillis() - (System.currentTimeMillis() - receivedAt));
        }
    }

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

    @Nullable
    public static SpawnPoints spawnPoints(ResourceLocation entityId) {
        return SPAWNS.get(entityId);
    }

    public static void setSpawnPoints(ResourceLocation entityId, List<MobSpawnPoint> points) {
        SPAWNS.put(entityId, new SpawnPoints(List.copyOf(points), System.currentTimeMillis()));
    }

    /** Запросить у сервера свежие спавнеры моба (не чаще раза в секунду на моба). */
    public static void requestSpawnPoints(ResourceLocation entityId) {
        if (Minecraft.getInstance().getConnection() == null)
            return;
        final long now = System.currentTimeMillis();
        final Long last = LAST_REQUEST.get(entityId);
        if (last != null && now - last < REQUEST_INTERVAL_MILLIS)
            return;
        LAST_REQUEST.put(entityId, now);
        PacketDistributor.sendToServer(new MobSpawnInfoRequestPacket(entityId));
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        mobs = List.of();
        SPAWNS.clear();
        LAST_REQUEST.clear();
    }
}

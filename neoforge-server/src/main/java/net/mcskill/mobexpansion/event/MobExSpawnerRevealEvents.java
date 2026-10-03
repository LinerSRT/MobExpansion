package net.mcskill.mobexpansion.event;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.network.SpawnerRevealPacket;
import net.mcskill.mobexpansion.spawner.MobSpawnerMask;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = Core.MODID)
public final class MobExSpawnerRevealEvents {
    private static final Map<ServerPlayer, Boolean> LAST_REVEAL = new WeakHashMap<>();

    private MobExSpawnerRevealEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        final boolean reveal = MobSpawnerMask.canReveal(player);
        final Boolean previous = LAST_REVEAL.put(player, reveal);
        if (previous != null && previous == reveal)
            return;
        PacketDistributor.sendToPlayer(player, new SpawnerRevealPacket(reveal));
        if (previous != null)
            MobSpawnerMask.resyncTrackedSpawners(player, reveal);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            LAST_REVEAL.remove(player);
    }
}

package net.mcskill.mobexpansion.init;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.network.SpawnerRevealPacket;
import net.mcskill.mobexpansion.network.UpdateMobDropsPacket;
import net.mcskill.mobexpansion.network.UpdateMobSpawnerPacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Core.MODID)
public class MobExNetwork {
    private MobExNetwork() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(Core.MODID);
        registrar.playToServer(UpdateMobSpawnerPacket.TYPE, UpdateMobSpawnerPacket.STREAM_CODEC, UpdateMobSpawnerPacket::handle);
        registrar.playToServer(UpdateMobDropsPacket.TYPE, UpdateMobDropsPacket.STREAM_CODEC, UpdateMobDropsPacket::handle);
        registrar.playToClient(SpawnerRevealPacket.TYPE, SpawnerRevealPacket.STREAM_CODEC, SpawnerRevealPacket::handle);
    }
}

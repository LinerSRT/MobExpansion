package net.mcskill.mobexpansion.init;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.network.ClearanceLevelPacket;
import net.mcskill.mobexpansion.network.CosmeticsRequestPacket;
import net.mcskill.mobexpansion.network.CosmeticsSelectPacket;
import net.mcskill.mobexpansion.network.CosmeticsStatePacket;
import net.mcskill.mobexpansion.network.MobClearancePacket;
import net.mcskill.mobexpansion.network.MobInfoPacket;
import net.mcskill.mobexpansion.network.OpenCosmeticsPacket;
import net.mcskill.mobexpansion.network.PlayerSkinPacket;
import net.mcskill.mobexpansion.network.SkinAttackFxPacket;
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
        registrar.playToClient(ClearanceLevelPacket.TYPE, ClearanceLevelPacket.STREAM_CODEC, ClearanceLevelPacket::handle);
        registrar.playToClient(MobClearancePacket.TYPE, MobClearancePacket.STREAM_CODEC, MobClearancePacket::handle);
        registrar.playToClient(PlayerSkinPacket.TYPE, PlayerSkinPacket.STREAM_CODEC, PlayerSkinPacket::handle);
        registrar.playToClient(SkinAttackFxPacket.TYPE, SkinAttackFxPacket.STREAM_CODEC, SkinAttackFxPacket::handle);
        registrar.playToClient(OpenCosmeticsPacket.TYPE, OpenCosmeticsPacket.STREAM_CODEC, OpenCosmeticsPacket::handle);
        registrar.playToClient(CosmeticsStatePacket.TYPE, CosmeticsStatePacket.STREAM_CODEC, CosmeticsStatePacket::handle);
        registrar.playToServer(CosmeticsRequestPacket.TYPE, CosmeticsRequestPacket.STREAM_CODEC, CosmeticsRequestPacket::handle);
        registrar.playToServer(CosmeticsSelectPacket.TYPE, CosmeticsSelectPacket.STREAM_CODEC, CosmeticsSelectPacket::handle);
        registrar.playToClient(MobInfoPacket.TYPE, MobInfoPacket.STREAM_CODEC, MobInfoPacket::handle);
    }
}

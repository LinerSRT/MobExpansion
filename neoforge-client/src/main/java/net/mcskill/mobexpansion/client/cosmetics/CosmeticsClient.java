package net.mcskill.mobexpansion.client.cosmetics;

import com.mojang.blaze3d.platform.InputConstants;
import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.network.CosmeticsRequestPacket;
import net.mcskill.mobexpansion.network.CosmeticsSelectPacket;
import net.mcskill.mobexpansion.network.CosmeticsStatePacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * GUI кастомизации на клиенте: клавиша (по умолчанию K), открытие экрана по команде /cosmetics,
 * последнее состояние с сервера и отправка выбора.
 */
@EventBusSubscriber(modid = Core.MODID, value = Dist.CLIENT)
public final class CosmeticsClient {
    public static final KeyMapping OPEN_KEY = new KeyMapping("key.mobexpansion.cosmetics", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.mobexpansion");

    @Nullable
    private static CosmeticsStatePacket state;

    private CosmeticsClient() {
    }

    @Nullable
    public static CosmeticsStatePacket state() {
        return state;
    }

    /** Открыть экран и запросить у сервера актуальное состояние. */
    public static void open() {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null)
            return;
        if (!(mc.screen instanceof CosmeticsScreen))
            mc.setScreen(new CosmeticsScreen());
        PacketDistributor.sendToServer(CosmeticsRequestPacket.INSTANCE);
    }

    public static void onState(CosmeticsStatePacket packet) {
        state = packet;
        if (Minecraft.getInstance().screen instanceof CosmeticsScreen screen)
            screen.onState(packet);
    }

    public static void select(CosmeticsSelectPacket.Category category, String id) {
        PacketDistributor.sendToServer(new CosmeticsSelectPacket(category, id));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (OPEN_KEY.consumeClick())
            open();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        state = null;
    }

    /** Событие шины мода — EventBusSubscriber сам направляет его туда. */
    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_KEY);
    }
}

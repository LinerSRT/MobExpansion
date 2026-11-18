package net.mcskill.mobexpansion.client.cosmetics;

import com.mojang.blaze3d.platform.InputConstants;
import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.cosmetics.CosmeticsGui;
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
 * GUI кастомизации на клиенте: клавиша (по умолчанию H), открытие экрана по команде /cosmetics,
 * последнее состояние с сервера и отправка выбора.
 * <p>
 * На бункере (стоит MsBunker) своего GUI нет — ни клавиши, ни экрана: скины и шлейфы там во вкладке «Кастомизация»
 * меню игрока (P), она берёт состояние отсюда через {@link CosmeticsApi}.
 */
@EventBusSubscriber(modid = Core.MODID, value = Dist.CLIENT)
public final class CosmeticsClient {
    @Nullable
    private static CosmeticsStatePacket state;
    /** Растёт с каждым состоянием с сервера: по нему чужие экраны замечают обновление. */
    private static int version;

    private CosmeticsClient() {
    }

    /** Клавиша создаётся, только если GUI есть: иначе неназначаемая привязка висела бы на H. */
    private static final class Keys {
        static final KeyMapping OPEN = new KeyMapping("key.mobexpansion.cosmetics", KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.mobexpansion");
    }

    public static KeyMapping openKey() {
        return Keys.OPEN;
    }

    @Nullable
    public static CosmeticsStatePacket state() {
        return state;
    }

    static int version() {
        return version;
    }

    /** Открыть экран и запросить у сервера актуальное состояние. */
    public static void open() {
        if (!CosmeticsGui.enabled())
            return;
        final Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null)
            return;
        if (!(mc.screen instanceof CosmeticsScreen))
            mc.setScreen(new CosmeticsScreen());
        request();
    }

    /** Запросить состояние без экрана (ответ — {@link #onState}). */
    static void request() {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.getConnection() != null)
            PacketDistributor.sendToServer(CosmeticsRequestPacket.INSTANCE);
    }

    public static void onState(CosmeticsStatePacket packet) {
        state = packet;
        version++;
        if (Minecraft.getInstance().screen instanceof CosmeticsScreen screen)
            screen.onState(packet);
    }

    public static void select(CosmeticsSelectPacket.Category category, String id) {
        PacketDistributor.sendToServer(new CosmeticsSelectPacket(category, id));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!CosmeticsGui.enabled())
            return;
        while (openKey().consumeClick())
            open();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        state = null;
        version++;
    }

    /** Событие шины мода — EventBusSubscriber сам направляет его туда. */
    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        if (CosmeticsGui.enabled())
            event.register(openKey());
    }
}

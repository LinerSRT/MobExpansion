package net.mcskill.mobexpansion.client.skin;

import net.mcskill.mobexpansion.Core;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Скины игроков на клиенте: вместо ванильного рендера игрока со скином рисуется его модель.
 * Новый скин — добавить {@link SkinDefinition} в {@link #DEFINITIONS} и его id в серверный PlayerSkins.SKINS.
 */
@EventBusSubscriber(modid = Core.MODID, value = Dist.CLIENT)
public final class SkinRenderers {
    private static final Map<String, SkinDefinition> DEFINITIONS = Map.of(
            SkinDefinition.CAPRA.id(), SkinDefinition.CAPRA,
            SkinDefinition.LILLITH.id(), SkinDefinition.LILLITH
    );
    private static final Map<String, PlayerSkinRenderer> RENDERERS = new HashMap<>();

    private SkinRenderers() {
    }

    @Nullable
    public static SkinDefinition definition(String skin) {
        return DEFINITIONS.get(skin);
    }

    @Nullable
    public static PlayerSkinRenderer get(String skin) {
        final SkinDefinition definition = DEFINITIONS.get(skin);
        if (definition == null)
            return null;
        return RENDERERS.computeIfAbsent(skin, id -> new PlayerSkinRenderer(context(), definition));
    }

    private static EntityRendererProvider.Context context() {
        final Minecraft mc = Minecraft.getInstance();
        return new EntityRendererProvider.Context(mc.getEntityRenderDispatcher(), mc.getItemRenderer(), mc.getBlockRenderer(),
                mc.getEntityRenderDispatcher().getItemInHandRenderer(), mc.getResourceManager(), mc.getEntityModels(), mc.font);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player))
            return;
        final PlayerSkinRenderer renderer = get(ClientSkins.get(player));
        if (renderer == null)
            return;
        event.setCanceled(true);
        final PlayerRenderer vanilla = event.getRenderer();
        renderer.renderPlayer(player, vanilla, event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
    }

    /** Рука от первого лица (пустая рука, рука с картой) — рука скина вместо ванильной. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderArm(RenderArmEvent event) {
        final AbstractClientPlayer player = event.getPlayer();
        final PlayerSkinRenderer renderer = get(ClientSkins.get(player));
        if (renderer == null
                || !(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer vanilla))
            return;
        event.setCanceled(true);
        renderer.renderFirstPersonArm(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), player, event.getArm(), vanilla);
    }
}

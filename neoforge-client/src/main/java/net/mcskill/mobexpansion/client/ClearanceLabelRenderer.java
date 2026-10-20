package net.mcskill.mobexpansion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mcskill.mobexpansion.Core;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import org.joml.Matrix4f;

/**
 * Надпись «Нужен уровень допуска N» над мобом, если уровень локального игрока ниже требуемого.
 * Рисуется рядом с мобом или когда игрок на него смотрит. RenderNameTagEvent приходит для всех сущностей,
 * включая GeckoLib-мобов (GeoEntityRenderer#renderFinal вызывает EntityRenderer#render).
 */
@EventBusSubscriber(modid = Core.MODID, value = Dist.CLIENT)
public final class ClearanceLabelRenderer {
    private static final double NEAR_DISTANCE_SQR = 12.0D * 12.0D;
    private static final double LOOK_DISTANCE_SQR = 48.0D * 48.0D;

    private ClearanceLabelRenderer() {
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        final Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity living) || !living.isAlive())
            return;
        final int required = ClientClearance.getRequired(entity);
        if (required <= ClientClearance.getLevel())
            return;
        final Minecraft minecraft = Minecraft.getInstance();
        final Player player = minecraft.player;
        if (player == null || entity == minecraft.getCameraEntity() || entity.isInvisibleTo(player))
            return;
        final double distanceSqr = minecraft.getEntityRenderDispatcher().distanceToSqr(entity);
        final boolean lookedAt = minecraft.crosshairPickEntity == entity;
        if (distanceSqr > (lookedAt ? LOOK_DISTANCE_SQR : NEAR_DISTANCE_SQR))
            return;

        final Component text = Component.translatable("label.mobexpansion.clearance_required", required)
                .withStyle(ChatFormatting.RED);
        // Если над мобом виден его собственный неймтег — поднимаемся строкой выше.
        final float lineOffset = entity.hasCustomName() ? -10.0F : 0.0F;
        render(minecraft, entity, text, lineOffset, event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), event.getPartialTick());
    }

    private static void render(Minecraft minecraft, Entity entity, Component text, float lineOffset, PoseStack poseStack,
                               MultiBufferSource buffer, int packedLight, float partialTick) {
        Vec3 anchor = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getViewYRot(partialTick));
        if (anchor == null)
            anchor = new Vec3(0.0D, entity.getBbHeight(), 0.0D);
        poseStack.pushPose();
        poseStack.translate(anchor.x, anchor.y + 0.5D, anchor.z);
        poseStack.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(0.025F, -0.025F, 0.025F);
        final Matrix4f pose = poseStack.last().pose();
        final Font font = minecraft.font;
        final float x = -font.width(text) / 2.0F;
        final int background = (int) (minecraft.options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
        font.drawInBatch(text, x, lineOffset, 0x20FFFFFF, false, pose, buffer, Font.DisplayMode.SEE_THROUGH, background, packedLight);
        font.drawInBatch(text, x, lineOffset, -1, false, pose, buffer, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }
}

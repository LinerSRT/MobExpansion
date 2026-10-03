package net.mcskill.mobexpansion.client.renderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mcskill.mobexpansion.entity.SpiderSpawnerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class SpiderSpawnerCountLayer extends GeoRenderLayer<SpiderSpawnerEntity> {
    private static final float TEXT_SCALE = 0.025F;
    private static final double HEIGHT_OFFSET = 3.35D;

    public SpiderSpawnerCountLayer(GeoRenderer<SpiderSpawnerEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, SpiderSpawnerEntity animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (!animatable.hasRolledStock() || animatable.isDeadOrDying())
            return;
        final String countText = animatable.getSpiderCountText();
        final Font font = Minecraft.getInstance().font;
        final float textWidth = font.width(countText);
        poseStack.pushPose();
        poseStack.translate(0.0D, HEIGHT_OFFSET, 0.0D);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
        final Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(Component.literal(countText), -textWidth / 2.0F, 0.0F, 0xFFFFFF, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }
}

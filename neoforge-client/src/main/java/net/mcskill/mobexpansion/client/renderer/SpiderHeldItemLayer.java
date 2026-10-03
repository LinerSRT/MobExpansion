package net.mcskill.mobexpansion.client.renderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mcskill.mobexpansion.entity.RegularSpiderEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class SpiderHeldItemLayer extends GeoRenderLayer<RegularSpiderEntity> {
    private final ItemRenderer itemRenderer;

    public SpiderHeldItemLayer(GeoRenderer<RegularSpiderEntity> renderer, ItemRenderer itemRenderer) {
        super(renderer);
        this.itemRenderer = itemRenderer;
    }

    @Override
    public void render(PoseStack poseStack, RegularSpiderEntity animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (animatable.getCarriedExperience() <= 0)
            return;
        final ItemStack renderStack = new ItemStack(Items.EXPERIENCE_BOTTLE);
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.55D, -0.05D);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(0.7F, 0.7F, 0.7F);
        this.itemRenderer.renderStatic(renderStack, ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, animatable.level(), animatable.getId());
        poseStack.popPose();
    }
}

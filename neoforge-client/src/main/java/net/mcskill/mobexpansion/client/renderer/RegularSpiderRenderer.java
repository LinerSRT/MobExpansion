package net.mcskill.mobexpansion.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.entity.RegularSpiderEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class RegularSpiderRenderer extends MobExGeoRenderer<RegularSpiderEntity> {
    private static final ResourceLocation EMISSIVE_TEXTURE = ResourceLocation.fromNamespaceAndPath(Core.MODID, "textures/entity/regular_spider_emissive.png");

    public RegularSpiderRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);
        this.shadowRadius = 0.9F;
        addRenderLayer(new EmissiveGeoLayer<>(this, EMISSIVE_TEXTURE));
        addRenderLayer(new SpiderHeldItemLayer(this, renderManager.getItemRenderer()));
    }

    @Override
    public void preRender(PoseStack poseStack, RegularSpiderEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        if (!isReRender)
            SpiderClimbRenderHelper.applyClimbOrientation(poseStack, animatable);
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }
}

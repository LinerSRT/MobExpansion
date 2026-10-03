package net.mcskill.mobexpansion.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.entity.PoisonSpiderEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class PoisonSpiderRenderer extends MobExGeoRenderer<PoisonSpiderEntity> {
    private static final ResourceLocation EMISSIVE_TEXTURE = ResourceLocation.fromNamespaceAndPath(Core.MODID, "textures/entity/poison_spider_emissive.png");

    public PoisonSpiderRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);
        this.shadowRadius = 0.9F;
        addRenderLayer(new EmissiveGeoLayer<>(this, EMISSIVE_TEXTURE));
    }

    @Override
    public void preRender(PoseStack poseStack, PoisonSpiderEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        if (!isReRender)
            SpiderClimbRenderHelper.applyClimbOrientation(poseStack, animatable);
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }
}

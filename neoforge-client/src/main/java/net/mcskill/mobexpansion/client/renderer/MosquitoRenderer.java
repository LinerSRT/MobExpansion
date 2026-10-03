package net.mcskill.mobexpansion.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mcskill.mobexpansion.entity.MosquitoEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class MosquitoRenderer extends MobExGeoRenderer<MosquitoEntity> {
    public MosquitoRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);
        this.shadowRadius = 0.25F;
    }

    @Override
    public void preRender(PoseStack poseStack, MosquitoEntity animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        this.entityRenderTranslations = new Matrix4f(poseStack.last().pose());
        scaleModelForRender(0.8f, 0.8f, poseStack, animatable, model, isReRender, partialTick, packedLight, packedOverlay);
        poseStack.translate(0, -0.5, 0);
    }
}
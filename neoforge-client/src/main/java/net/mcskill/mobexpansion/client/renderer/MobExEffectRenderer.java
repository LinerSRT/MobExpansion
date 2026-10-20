package net.mcskill.mobexpansion.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mcskill.mobexpansion.client.model.MobExGeoModel;
import net.mcskill.mobexpansion.entity.effect.MobExEffectEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Рисует {@link MobExEffectEntity}: полупрозрачная модель эффекта + автосвечение, с поворотом и масштабом.
 */
public class MobExEffectRenderer extends GeoEntityRenderer<MobExEffectEntity> {
    public MobExEffectRenderer(EntityRendererProvider.Context context) {
        super(context, new MobExGeoModel<>());
        this.shadowRadius = 0.0F;
        addRenderLayer(new AutoEmissiveLayer<>(this));
    }

    @Override
    public void render(MobExEffectEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // эффект без модели — невидимая зона урона (огненный след и т.п.), рисуют её только частицы
        if (entity.modelName().isEmpty())
            return;
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, entity.isGlowingEffect() ? LightTexture.FULL_BRIGHT : packedLight);
    }

    @Override
    public RenderType getRenderType(MobExEffectEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucentCull(texture);
    }

    @Override
    public void preRender(PoseStack poseStack, MobExEffectEntity animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        final float scale = animatable.getScale();
        if (!isReRender && scale != 1.0F)
            poseStack.scale(scale, scale, scale);
    }

    @Override
    protected void applyRotations(MobExEffectEntity animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        // у эффектов yRot как у мобов (поворот корпуса заклинателя)
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - Mth.rotLerp(partialTick, animatable.yRotO, animatable.getYRot())));
    }
}

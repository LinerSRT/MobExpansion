package net.mcskill.mobexpansion.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.mcskill.mobexpansion.entity.sea.SeaCreatureEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/**
 * Рендерер морских существ: поверх общего — наклон модели по тангажу (нос вверх/вниз при всплытии и погружении).
 * Поворот вокруг середины хитбокса, чтобы длинная акула не «садилась» хвостом.
 */
public class SeaCreatureRenderer<T extends SeaCreatureEntity> extends AbilityMobRenderer<T> {
    public SeaCreatureRenderer(EntityRendererProvider.Context context, float modelScale, float shadowRadius) {
        super(context, modelScale, shadowRadius, true);
    }

    public static <T extends SeaCreatureEntity> EntityRendererProvider<T> sea(float modelScale, float shadowRadius) {
        return context -> new SeaCreatureRenderer<>(context, modelScale, shadowRadius);
    }

    @Override
    protected void applyRotations(T animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
        if (!animatable.pitchesModel() || animatable.isDeadOrDying())
            return;
        final float pitch = Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot());
        if (Math.abs(pitch) < 0.01F)
            return;
        final float pivot = animatable.getBbHeight() * 0.5F;
        poseStack.translate(0.0F, pivot, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.translate(0.0F, -pivot, 0.0F);
    }
}

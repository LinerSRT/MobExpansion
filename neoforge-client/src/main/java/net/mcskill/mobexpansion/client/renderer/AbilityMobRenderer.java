package net.mcskill.mobexpansion.client.renderer;

import net.mcskill.mobexpansion.entity.ability.AbilityMobEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Общий рендерер мобов со способностями: масштаб модели, тень, автосвечение (_emissive)
 * и без ванильного переворота на бок при смерти — у моделей своя анимация смерти.
 * <p>
 * cullBackFaces: модели из ModelEngine (Java) рассчитаны на отсечение задних граней — в них есть
 * «вывернутые» кубы-оболочки (обводка, inverted hull), которые без отсечения закрывают всю модель.
 * Bedrock-модели рисуются без отсечения, как и в оригинале.
 */
public class AbilityMobRenderer<T extends AbilityMobEntity> extends MobExGeoRenderer<T> {
    private final boolean cullBackFaces;

    public AbilityMobRenderer(EntityRendererProvider.Context context, float modelScale, float shadowRadius, boolean cullBackFaces) {
        super(context);
        this.shadowRadius = shadowRadius;
        this.cullBackFaces = cullBackFaces;
        if (modelScale != 1.0F)
            withScale(modelScale);
        addRenderLayer(new AutoEmissiveLayer<>(this));
    }

    public static <T extends AbilityMobEntity> EntityRendererProvider<T> of(float modelScale, float shadowRadius) {
        return context -> new AbilityMobRenderer<>(context, modelScale, shadowRadius, true);
    }

    public static <T extends AbilityMobEntity> EntityRendererProvider<T> noCull(float modelScale, float shadowRadius) {
        return context -> new AbilityMobRenderer<>(context, modelScale, shadowRadius, false);
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return cullBackFaces ? RenderType.entityCutout(texture) : RenderType.entityCutoutNoCull(texture);
    }

    @Override
    protected float getDeathMaxRotation(T animatable) {
        return 0.0F;
    }

    /** Без ванильного красного оттенка, пока моб умирает: смерть показывает только своя анимация. */
    @Override
    public int getPackedOverlay(T animatable, float u, float partialTick) {
        if (animatable.isDeadOrDying())
            return OverlayTexture.NO_OVERLAY;
        return super.getPackedOverlay(animatable, u, partialTick);
    }
}

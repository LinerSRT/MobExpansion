package net.mcskill.mobexpansion.client.renderer;

import net.mcskill.mobexpansion.entity.ability.AbilityMobEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Общий рендерер мобов со способностями: масштаб модели, тень, автосвечение (_emissive)
 * и без ванильного переворота на бок при смерти — у моделей своя анимация смерти.
 */
public class AbilityMobRenderer<T extends AbilityMobEntity> extends MobExGeoRenderer<T> {
    public AbilityMobRenderer(EntityRendererProvider.Context context, float modelScale, float shadowRadius) {
        super(context);
        this.shadowRadius = shadowRadius;
        if (modelScale != 1.0F)
            withScale(modelScale);
        addRenderLayer(new AutoEmissiveLayer<>(this));
    }

    public static <T extends AbilityMobEntity> EntityRendererProvider<T> of(float modelScale, float shadowRadius) {
        return context -> new AbilityMobRenderer<>(context, modelScale, shadowRadius);
    }

    @Override
    protected float getDeathMaxRotation(T animatable) {
        return 0.0F;
    }
}

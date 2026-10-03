package net.mcskill.mobexpansion.client.renderer;

import net.mcskill.mobexpansion.entity.RatKingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class RatKingRenderer extends MobExGeoRenderer<RatKingEntity> {
    public RatKingRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);
        this.shadowRadius = 0.95F;
    }
}
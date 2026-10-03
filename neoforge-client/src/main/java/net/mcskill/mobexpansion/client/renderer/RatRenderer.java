package net.mcskill.mobexpansion.client.renderer;

import net.mcskill.mobexpansion.entity.RatEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class RatRenderer extends MobExGeoRenderer<RatEntity> {
    public RatRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);
        this.shadowRadius = 0.35F;
    }
}
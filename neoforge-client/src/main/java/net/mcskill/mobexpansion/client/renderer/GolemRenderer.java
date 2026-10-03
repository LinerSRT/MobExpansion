package net.mcskill.mobexpansion.client.renderer;

import net.mcskill.mobexpansion.entity.SentinelGolemEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class GolemRenderer extends MobExGeoRenderer<SentinelGolemEntity> {
    public GolemRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);
        this.shadowRadius = 0.9F;
    }
}
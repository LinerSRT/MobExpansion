package net.mcskill.mobexpansion.client.renderer;

import net.mcskill.mobexpansion.entity.SentinelStatueEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class StatueRenderer extends MobExGeoRenderer<SentinelStatueEntity> {
    public StatueRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);
        this.shadowRadius = 0.9F;
    }
}
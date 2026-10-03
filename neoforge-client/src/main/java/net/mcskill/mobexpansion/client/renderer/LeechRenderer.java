package net.mcskill.mobexpansion.client.renderer;

import net.mcskill.mobexpansion.entity.LeechEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class LeechRenderer extends MobExGeoRenderer<LeechEntity> {
    public LeechRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);
        this.shadowRadius = 0.4F;
    }
}
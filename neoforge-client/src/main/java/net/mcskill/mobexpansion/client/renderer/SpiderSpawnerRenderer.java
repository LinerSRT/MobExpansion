package net.mcskill.mobexpansion.client.renderer;

import net.mcskill.mobexpansion.entity.SpiderSpawnerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class SpiderSpawnerRenderer extends MobExGeoRenderer<SpiderSpawnerEntity> {
    public SpiderSpawnerRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);
        this.shadowRadius = 0.8F;
        addRenderLayer(new SpiderSpawnerCountLayer(this));
    }
}

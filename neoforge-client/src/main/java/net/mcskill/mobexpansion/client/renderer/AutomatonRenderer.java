package net.mcskill.mobexpansion.client.renderer;

import net.mcskill.mobexpansion.entity.redstone.RedstoneAutomatonEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class AutomatonRenderer extends MobExGeoRenderer<RedstoneAutomatonEntity> {
    public AutomatonRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);
        this.shadowRadius = 0.75F;
    }
}
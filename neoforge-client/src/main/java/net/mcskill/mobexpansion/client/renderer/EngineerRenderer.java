package net.mcskill.mobexpansion.client.renderer;

import net.mcskill.mobexpansion.entity.redstone.RedstoneEngineerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class EngineerRenderer extends MobExGeoRenderer<RedstoneEngineerEntity> {
    public EngineerRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);
        this.shadowRadius = 0.55F;
    }
}
package net.mcskill.mobexpansion.client.renderer;

import net.mcskill.mobexpansion.entity.SewerPotionProjectileEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class PotionProjectileRenderer extends MobExGeoRenderer<SewerPotionProjectileEntity> {
    public PotionProjectileRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);
        this.shadowRadius = 0.0F;
    }
}
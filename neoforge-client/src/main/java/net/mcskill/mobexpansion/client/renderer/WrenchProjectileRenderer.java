package net.mcskill.mobexpansion.client.renderer;


import net.mcskill.mobexpansion.entity.redstone.WrenchProjectileEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class WrenchProjectileRenderer extends MobExGeoRenderer<WrenchProjectileEntity> {
    public WrenchProjectileRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);
        this.shadowRadius = 0.15F;
    }
}
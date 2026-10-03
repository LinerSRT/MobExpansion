package net.mcskill.mobexpansion.client.renderer;

import net.mcskill.mobexpansion.entity.redstone.RedstoneProjectileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public class ProjectileRenderer extends MobExGeoRenderer<RedstoneProjectileEntity> {
    public ProjectileRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);
        this.shadowRadius = 0.0F;
    }

    @Override
    public RenderType getRenderType(RedstoneProjectileEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.eyes(texture);
    }
}
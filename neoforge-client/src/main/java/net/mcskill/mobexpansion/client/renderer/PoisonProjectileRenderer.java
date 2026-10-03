package net.mcskill.mobexpansion.client.renderer;

import net.mcskill.mobexpansion.entity.PoisonProjectileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class PoisonProjectileRenderer extends MobExGeoRenderer<PoisonProjectileEntity> {
    public PoisonProjectileRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);
        this.shadowRadius = 0.2F;
    }

    @Override
    @Nullable
    public RenderType getRenderType(PoisonProjectileEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.eyes(texture);
    }
}

package net.mcskill.mobexpansion.client.renderer;


import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class InvisibleRenderer<T extends Entity> extends EntityRenderer<T> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/fishing_hook.png");

    public InvisibleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(@NotNull T entity, @NotNull Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return false;
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(@NotNull T entity) {
        return TEXTURE;
    }
}
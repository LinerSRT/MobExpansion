package net.mcskill.mobexpansion.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public final class GuiBlockPreview {
    private GuiBlockPreview() {
    }

    public static void render(GuiGraphics guiGraphics, @Nullable BlockState state, int x, int y, int size) {
        render(guiGraphics, state, x, y, size, 225.0F);
    }

    public static void render(GuiGraphics guiGraphics, @Nullable BlockState state, int x, int y, int size, float yawDegrees) {
        if (state == null)
            return;
        final Minecraft minecraft = Minecraft.getInstance();
        guiGraphics.enableScissor(x, y, x + size, y + size);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x + size / 2.0F, y + size / 2.0F, 150.0F);
        guiGraphics.pose().scale(size * 0.62F, -size * 0.62F, size * 0.62F);
        guiGraphics.pose().mulPose(Axis.XP.rotationDegrees(30.0F));
        guiGraphics.pose().mulPose(Axis.YP.rotationDegrees(yawDegrees));
        guiGraphics.pose().translate(-0.5F, -0.5F, -0.5F);

        guiGraphics.flush();
        Lighting.setupFor3DItems();
        RenderSystem.enableDepthTest();
        final MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        minecraft.getBlockRenderer().renderSingleBlock(
                state,
                guiGraphics.pose(),
                bufferSource,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY
        );
        bufferSource.endBatch();
        Lighting.setupFor3DItems();
        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();
    }
}

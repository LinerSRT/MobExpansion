package net.mcskill.mobexpansion.mixin;

import net.mcskill.mobexpansion.client.screen.GuiFitScale;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsFitScissorMixin {
    @ModifyVariable(method = "enableScissor", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int mobexpansion$fitScissorX1(int x1) {
        return GuiFitScale.screenX(x1);
    }

    @ModifyVariable(method = "enableScissor", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int mobexpansion$fitScissorY1(int y1) {
        return GuiFitScale.screenY(y1);
    }

    @ModifyVariable(method = "enableScissor", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private int mobexpansion$fitScissorX2(int x2) {
        return GuiFitScale.screenX(x2);
    }

    @ModifyVariable(method = "enableScissor", at = @At("HEAD"), argsOnly = true, ordinal = 3)
    private int mobexpansion$fitScissorY2(int y2) {
        return GuiFitScale.screenY(y2);
    }
}

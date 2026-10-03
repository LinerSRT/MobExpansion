package net.mcskill.mobexpansion.client.renderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.mcskill.mobexpansion.entity.RegularSpiderEntity;

public final class SpiderClimbRenderHelper {
    private SpiderClimbRenderHelper() {
    }

    public static void applyClimbOrientation(PoseStack poseStack, RegularSpiderEntity spider) {
        if (!spider.isOnCeiling())
            return;
        poseStack.translate(0.0D, spider.getBbHeight(), 0.0D);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
    }
}

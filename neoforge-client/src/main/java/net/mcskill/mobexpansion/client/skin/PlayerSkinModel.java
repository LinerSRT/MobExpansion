package net.mcskill.mobexpansion.client.skin;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

/**
 * Модель скина: ресурсы из {@link SkinDefinition} и перенос позы ванильной модели игрока на кости скина.
 */
public class PlayerSkinModel extends GeoModel<PlayerSkinAnimatable> {
    private final SkinDefinition definition;
    /** Поза ванильной модели для игрока, которого сейчас рисуют (ставит {@link PlayerSkinRenderer}). */
    @Nullable
    private PlayerModel<?> pose;

    public PlayerSkinModel(SkinDefinition definition) {
        this.definition = definition;
    }

    void setPose(@Nullable PlayerModel<?> pose) {
        this.pose = pose;
    }

    @Override
    public ResourceLocation getModelResource(PlayerSkinAnimatable animatable) {
        return definition.model();
    }

    @Override
    public ResourceLocation getTextureResource(PlayerSkinAnimatable animatable) {
        return definition.texture();
    }

    @Override
    public ResourceLocation getAnimationResource(PlayerSkinAnimatable animatable) {
        return definition.animations();
    }

    /**
     * Вызывается после анимаций GeckoLib: кости тела, головы, рук и ног повторяют ванильную модель. У ванильного
     * игрока голова/руки/ноги не дети тела, а в скине — дети, поэтому поворот тела из них вычитается.
     */
    @Override
    public void setCustomAnimations(PlayerSkinAnimatable animatable, long instanceId, AnimationState<PlayerSkinAnimatable> state) {
        final PlayerModel<?> model = pose;
        if (model == null)
            return;
        final ModelPart body = model.body;
        apply(definition.body(), body, null);
        apply(definition.head(), model.head, body);
        apply(definition.rightArm(), model.rightArm, body);
        apply(definition.leftArm(), model.leftArm, body);
        apply(definition.rightLeg(), model.rightLeg, body);
        apply(definition.leftLeg(), model.leftLeg, body);
    }

    private void apply(String boneName, ModelPart part, @Nullable ModelPart parent) {
        final GeoBone bone = getAnimationProcessor().getBone(boneName);
        if (bone == null)
            return;
        float x = part.xRot, y = part.yRot, z = part.zRot;
        if (parent != null) {
            x -= parent.xRot;
            y -= parent.yRot;
            z -= parent.zRot;
        }
        // оси как в RenderUtil#matchModelPartRot: у GeckoLib X и Y повёрнуты наоборот
        final BoneSnapshot rest = bone.getInitialSnapshot();
        if (rest == null)
            bone.updateRotation(-x, -y, z);
        else
            bone.updateRotation(rest.getRotX() - x, rest.getRotY() - y, rest.getRotZ() + z);
    }
}

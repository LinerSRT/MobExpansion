package net.mcskill.mobexpansion.client.skin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mcskill.mobexpansion.client.renderer.EmissiveGeoLayer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;

/**
 * Рисует игрока моделью скина вместо ванильной. Броня, плащ, элитры и стрелы в теле не рисуются (скин полностью
 * заменяет внешний вид); предметы в руках — {@link SkinHeldItemLayer}.
 */
public class PlayerSkinRenderer extends GeoReplacedEntityRenderer<AbstractClientPlayer, PlayerSkinAnimatable> {
    private final SkinDefinition definition;
    private final PlayerSkinModel skinModel;
    /** Модель, с которой сняты копии рук (после перезагрузки ресурсов копии делаются заново). */
    @Nullable
    private BakedGeoModel armSource;
    @Nullable
    private GeoBone rightArmCopy, leftArmCopy;

    public PlayerSkinRenderer(EntityRendererProvider.Context context, SkinDefinition definition) {
        this(context, definition, new PlayerSkinModel(definition));
    }

    private PlayerSkinRenderer(EntityRendererProvider.Context context, SkinDefinition definition, PlayerSkinModel model) {
        super(context, model, new PlayerSkinAnimatable(definition));
        this.definition = definition;
        this.skinModel = model;
        this.shadowRadius = 0.5F;
        withScale(definition.scale());
        if (definition.emissive())
            addRenderLayer(new EmissiveGeoLayer<>(this, definition.emissiveTexture()));
        addRenderLayer(new SkinHeldItemLayer(this, definition));
    }

    public SkinDefinition definition() {
        return definition;
    }

    /**
     * Рисует игрока. {@code vanilla} — его обычный рендерер: его PlayerModel уже настроена под позу рук
     * (PlayerRenderer#setModelProperties вызывается до RenderPlayerEvent.Pre), остаётся посчитать setupAnim
     * так же, как LivingEntityRenderer#render, и перенести позу на кости скина.
     */
    public void renderPlayer(AbstractClientPlayer player, PlayerRenderer vanilla, float partialTick, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight) {
        final PlayerModel<AbstractClientPlayer> model = vanilla.getModel();
        setupVanillaPose(model, player, partialTick);
        skinModel.setPose(model);
        try {
            render(player, 0, partialTick, poseStack, bufferSource, packedLight);
        } finally {
            skinModel.setPose(null);
        }
    }

    /**
     * Рука от первого лица вместо ванильной (из RenderArmEvent: пустая рука, рука с картой). PoseStack — тот, в котором
     * ваниль рисует ModelPart руки. Рисуется цепочка костей скина плечо → предплечье → кисть в нулевой позе, поставленная
     * так, чтобы кончики пальцев совпали с концом ванильной руки.
     */
    public void renderFirstPersonArm(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                     AbstractClientPlayer player, HumanoidArm arm, PlayerRenderer vanilla) {
        // как PlayerRenderer#renderHand: поза «рука опущена» + лёгкое покачивание
        final PlayerModel<AbstractClientPlayer> vanillaModel = vanilla.getModel();
        vanillaModel.attackTime = 0.0F;
        vanillaModel.crouching = false;
        vanillaModel.swimAmount = 0.0F;
        vanillaModel.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        final boolean right = arm == HumanoidArm.RIGHT;
        final ModelPart part = right ? vanillaModel.rightArm : vanillaModel.leftArm;
        part.xRot = 0.0F;

        final GeoBone shoulder = firstPersonArm(right);
        if (shoulder == null)
            return;

        final float scale = definition.firstPersonScale();
        final Vector3f hand = definition.firstPersonHand();
        final float side = right ? 1.0F : -1.0F;
        poseStack.pushPose();
        part.translateAndRotate(poseStack);
        // система ванильной руки: Y вниз, центр руки по X = -1 (правая) / +1 (левая), кончики пальцев на Y = 10
        poseStack.translate(side * (-1.0F + hand.x() * scale) / 16.0F, (10.0F + hand.y() * scale) / 16.0F, -hand.z() * scale / 16.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F)); // оси ванильной модели -> оси GeckoLib
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-shoulder.getPivotX() / 16.0F, -shoulder.getPivotY() / 16.0F, -shoulder.getPivotZ() / 16.0F);

        final RenderType base = RenderType.entityCutoutNoCull(definition.texture());
        renderRecursively(poseStack, getAnimatable(), shoulder, base, bufferSource, bufferSource.getBuffer(base), true,
                0.0F, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        if (definition.emissive()) {
            final RenderType glow = RenderType.eyes(definition.emissiveTexture());
            renderRecursively(poseStack, getAnimatable(), shoulder, glow, bufferSource, bufferSource.getBuffer(glow), true,
                    0.0F, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        }
        poseStack.popPose();
    }

    /**
     * Цепочка костей руки для вида от первого лица — копия в позе покоя. Общие кости модели трогать нельзя: в остальных
     * кадрах тика GeckoLib не пересчитывает позу игрока (время анимации скина меняется раз в тик, см.
     * PlayerSkinAnimatable#getTick), и рука других игроков в этом скине дёргалась бы в позу покоя.
     */
    @Nullable
    private GeoBone firstPersonArm(boolean right) {
        final BakedGeoModel baked = getGeoModel().getBakedModel(definition.model());
        if (baked != armSource) {
            armSource = baked;
            rightArmCopy = baked.getBone(definition.rightArm()).map(bone -> GeoBoneCopies.copyOf(bone, null)).orElse(null);
            leftArmCopy = baked.getBone(definition.leftArm()).map(bone -> GeoBoneCopies.copyOf(bone, null)).orElse(null);
        }
        return right ? rightArmCopy : leftArmCopy;
    }

    /** Копия расчёта позы из LivingEntityRenderer#render (1.21.1). */
    private static void setupVanillaPose(PlayerModel<AbstractClientPlayer> model, AbstractClientPlayer player, float partialTick) {
        model.attackTime = player.getAttackAnim(partialTick);
        final boolean shouldSit = player.isPassenger() && player.getVehicle() != null && player.getVehicle().shouldRiderSit();
        model.riding = shouldSit;
        model.young = player.isBaby();
        float bodyRot = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        final float headRot = Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
        float netHeadYaw = headRot - bodyRot;
        if (shouldSit && player.getVehicle() instanceof LivingEntity vehicle) {
            bodyRot = Mth.rotLerp(partialTick, vehicle.yBodyRotO, vehicle.yBodyRot);
            netHeadYaw = headRot - bodyRot;
            float clamped = Mth.clamp(Mth.wrapDegrees(netHeadYaw), -85.0F, 85.0F);
            bodyRot = headRot - clamped;
            if (clamped * clamped > 2500.0F)
                bodyRot += clamped * 0.2F;
            netHeadYaw = headRot - bodyRot;
        }
        float headPitch = Mth.lerp(partialTick, player.xRotO, player.getXRot());
        if (LivingEntityRenderer.isEntityUpsideDown(player)) {
            headPitch *= -1.0F;
            netHeadYaw *= -1.0F;
        }
        netHeadYaw = Mth.wrapDegrees(netHeadYaw);
        float limbSpeed = 0.0F;
        float limbPos = 0.0F;
        if (!shouldSit && player.isAlive()) {
            limbSpeed = Math.min(player.walkAnimation.speed(partialTick), 1.0F);
            limbPos = player.walkAnimation.position(partialTick);
            if (player.isBaby())
                limbPos *= 3.0F;
        }
        final float ageInTicks = player.tickCount + partialTick;
        model.prepareMobModel(player, limbPos, limbSpeed, partialTick);
        model.setupAnim(player, limbPos, limbSpeed, ageInTicks, netHeadYaw, headPitch);
    }

    @Override
    public void preRender(PoseStack poseStack, PlayerSkinAnimatable animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource,
                          @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        if (!isReRender && definition.lift() != 0)
            poseStack.translate(0, definition.lift() / 16F, 0);
    }

    /** Повороты всего тела как у PlayerRenderer#setupRotations: полёт на элитрах и плавание. */
    @Override
    protected void applyRotations(PlayerSkinAnimatable animatable, PoseStack poseStack, float ageInTicks, float rotationYaw,
                                  float partialTick, float nativeScale) {
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
        final AbstractClientPlayer player = getCurrentEntity();
        final float swimAmount = player.getSwimAmount(partialTick);
        if (player.isFallFlying()) {
            final float flyTicks = player.getFallFlyingTicks() + partialTick;
            final float progress = Mth.clamp(flyTicks * flyTicks / 100.0F, 0.0F, 1.0F);
            if (!player.isAutoSpinAttack())
                poseStack.mulPose(Axis.XP.rotationDegrees(progress * (-90.0F - player.getViewXRot(partialTick))));
            final Vec3 view = player.getViewVector(partialTick);
            final Vec3 motion = player.getDeltaMovementLerped(partialTick);
            final double motionSqr = motion.horizontalDistanceSqr();
            final double viewSqr = view.horizontalDistanceSqr();
            if (motionSqr > 0.0 && viewSqr > 0.0) {
                final double cos = (motion.x * view.x + motion.z * view.z) / Math.sqrt(motionSqr * viewSqr);
                final double side = motion.x * view.z - motion.z * view.x;
                poseStack.mulPose(Axis.YP.rotation((float) (Math.signum(side) * Math.acos(Mth.clamp(cos, -1.0, 1.0)))));
            }
        } else if (swimAmount > 0.0F) {
            final float target = player.isInWater() || player.isInFluidType((fluidType, height) -> player.canSwimInFluidType(fluidType))
                    ? -90.0F - player.getXRot() : -90.0F;
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(swimAmount, 0.0F, target)));
            if (player.isVisuallySwimming())
                poseStack.translate(0.0F, -1.0F, 0.3F);
        }
    }

    /**
     * GeoReplacedEntityRenderer рисует имя дважды: в postRender (ещё внутри масштаба модели — табличка выходит
     * мелкой и низко) и в renderFinal (уже без масштаба). Оставляем только renderFinal.
     */
    @Override
    public void postRender(PoseStack poseStack, PlayerSkinAnimatable animatable, BakedGeoModel model, MultiBufferSource bufferSource,
                           VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
    }
}

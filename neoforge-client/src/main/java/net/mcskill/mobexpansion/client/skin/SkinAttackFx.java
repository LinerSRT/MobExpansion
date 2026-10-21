package net.mcskill.mobexpansion.client.skin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.client.MobExClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Эффект удара игрока со скином (приходит с сервера пакетом SkinAttackFxPacket, поэтому его видят все рядом).
 * Как он выглядит — {@link SkinAttackStyle} скина: взмах-дуга перед игроком или вихрь-кольцо вокруг, частицы по дуге,
 * на крите — крупнее, всплеск частиц, звук и (если есть) печать перед игроком.
 * Плюс время удара для всплеска крыльев ({@link PlayerSkinAnimatable}).
 */
@EventBusSubscriber(modid = Core.MODID, value = Dist.CLIENT)
public final class SkinAttackFx {
    /** Время жизни эффекта, тиков. */
    private static final float LIFE = 7.0F;
    private static final float SEAL_LIFE = 12.0F;
    /** Удары слабее этого заряда (закликивание) — только пара частиц, без взмаха. */
    private static final float MIN_SLASH_STRENGTH = 0.35F;

    private static final List<Effect> EFFECTS = new ArrayList<>();
    private static final Int2LongMap LAST_ATTACK = new Int2LongOpenHashMap();
    private static final RandomSource RANDOM = RandomSource.create();
    private static int counter;

    static {
        LAST_ATTACK.defaultReturnValue(-1);
    }

    private SkinAttackFx() {
    }

    /** Время (game time) последнего удара игрока или -1. */
    public static long lastAttackTick(int entityId) {
        return LAST_ATTACK.get(entityId);
    }

    public static void onAttack(int entityId, float strength, boolean critical) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !(level.getEntity(entityId) instanceof Player player))
            return;
        final SkinDefinition definition = SkinRenderers.definition(ClientSkins.get(player));
        if (definition == null)
            return;
        final SkinAttackStyle style = definition.attack();
        LAST_ATTACK.put(entityId, level.getGameTime());
        if (hiddenInFirstPerson(player))
            return;
        if (strength < MIN_SLASH_STRENGTH) {
            particles(level, player, style, 3, 0.6F);
            return;
        }
        // взмахи чередуются справа налево и слева направо, наклон — случайный
        final boolean mirrored = (counter++ & 1) == 1;
        final float size = critical ? 3.0F : 1.7F + 0.7F * strength;
        final float roll = (mirrored ? 1 : -1) * (12.0F + RANDOM.nextFloat() * 22.0F);
        final long now = level.getGameTime();
        EFFECTS.add(new Effect(Kind.SLASH, style, entityId, now, size, roll, mirrored));
        particles(level, player, style, critical ? 18 : 9, size * 0.5F);
        if (!critical)
            return;
        if (style.critSeal() != null)
            EFFECTS.add(new Effect(Kind.SEAL, style, entityId, now, 1.6F, RANDOM.nextFloat() * 72.0F, mirrored));
        final Vec3 chest = chest(player, 1.0F);
        for (int i = 0; i < 6; i++)
            level.addParticle(style.critBurst(), chest.x + RANDOM.nextGaussian() * 0.3, chest.y + RANDOM.nextGaussian() * 0.3,
                    chest.z + RANDOM.nextGaussian() * 0.3, 0, 0.05, 0);
        level.playLocalSound(player.getX(), player.getY(), player.getZ(), style.critSound(), SoundSource.PLAYERS,
                0.6F, 1.2F + RANDOM.nextFloat() * 0.3F, false);
    }

    /** Свой эффект удара отключён настройкой и камера от первого лица. */
    private static boolean hiddenInFirstPerson(Entity owner) {
        final Minecraft mc = Minecraft.getInstance();
        return MobExClientConfig.HIDE_SKIN_ATTACK_FX_FIRST_PERSON.get() && owner == mc.player
                && mc.options.getCameraType().isFirstPerson();
    }

    /** Частицы по дуге перед игроком (у вихря — по кругу вокруг). */
    private static void particles(ClientLevel level, Player player, SkinAttackStyle style, int count, float radius) {
        final Vec3 chest = chest(player, 1.0F);
        final float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        final float spread = style.spin() ? Mth.TWO_PI : Mth.PI * 0.9F;
        for (int i = 0; i < count; i++) {
            final float angle = yaw + (RANDOM.nextFloat() - 0.5F) * spread;
            final double dx = -Mth.sin(angle), dz = Mth.cos(angle);
            final double r = radius * (0.6 + RANDOM.nextDouble() * 0.5);
            level.addParticle(RANDOM.nextInt(style.accentChance()) == 0 ? style.accent() : style.trail(),
                    chest.x + dx * r, chest.y + (RANDOM.nextDouble() - 0.5) * 0.6, chest.z + dz * r,
                    dx * 0.04, 0.02 + RANDOM.nextDouble() * 0.03, dz * 0.04);
        }
    }

    private static Vec3 chest(Entity entity, float partialTick) {
        return entity.getPosition(partialTick).add(0, entity.getBbHeight() * 0.6, 0);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || EFFECTS.isEmpty())
            return;
        final Minecraft mc = Minecraft.getInstance();
        final ClientLevel level = mc.level;
        if (level == null)
            return;
        final float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        final float now = level.getGameTime() + partialTick;
        final Vec3 camera = event.getCamera().getPosition();
        final PoseStack poseStack = event.getPoseStack();
        final MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        final Set<RenderType> used = new LinkedHashSet<>();
        for (Iterator<Effect> it = EFFECTS.iterator(); it.hasNext(); ) {
            final Effect effect = it.next();
            final float age = now - effect.start;
            final float life = effect.kind == Kind.SEAL ? SEAL_LIFE : LIFE;
            final Entity owner = level.getEntity(effect.entityId);
            if (age >= life || age < 0 || owner == null || owner.isRemoved()) {
                it.remove();
                continue;
            }
            if (hiddenInFirstPerson(owner))
                continue;
            final RenderType type = RenderType.entityTranslucentEmissive(effect.kind == Kind.SEAL ? effect.style.critSeal() : effect.style.slash());
            used.add(type);
            effect.render(poseStack, buffers.getBuffer(type), owner, camera, partialTick, age / life);
        }
        for (RenderType type : used)
            buffers.endBatch(type);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        EFFECTS.clear();
        LAST_ATTACK.clear();
    }

    private enum Kind {SLASH, SEAL}

    private record Effect(Kind kind, SkinAttackStyle style, int entityId, long start, float size, float roll, boolean mirrored) {
        void render(PoseStack poseStack, VertexConsumer consumer, Entity owner, Vec3 camera, float partialTick, float progress) {
            final Vec3 chest = chest(owner, partialTick);
            poseStack.pushPose();
            poseStack.translate(chest.x - camera.x, chest.y - camera.y, chest.z - camera.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(-owner.getViewYRot(partialTick)));
            if (kind == Kind.SEAL)
                renderSeal(poseStack, consumer, owner, partialTick, progress);
            else if (style.spin())
                renderSwirl(poseStack, consumer, owner, partialTick, progress);
            else
                renderSlash(poseStack, consumer, owner, partialTick, progress);
            poseStack.popPose();
        }

        /**
         * Дуга перед игроком в «горизонтальной» относительно взгляда плоскости. На текстуре дуга лежит справа
         * (u → вперёд), голова взмаха сверху (v → влево от игрока).
         */
        private void renderSlash(PoseStack poseStack, VertexConsumer consumer, Entity owner, float partialTick, float progress) {
            final int frame = Math.min(style.frames() - 1, (int) (progress * style.frames()));
            final float alpha = progress < 0.6F ? 1.0F : 1.0F - (progress - 0.6F) / 0.4F;
            // взмах проходит дугу: плоскость доворачивается вокруг своей нормали
            final float sweep = (mirrored ? -1 : 1) * Mth.lerp(progress, 35.0F, -25.0F);
            final float scale = size * (0.85F + 0.15F * Math.min(1.0F, progress * 3.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(owner.getViewXRot(partialTick) * 0.6F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
            poseStack.mulPose(Axis.YP.rotationDegrees(sweep));
            poseStack.translate(0, 0, 0.15F);
            poseStack.scale(scale, scale, scale);
            final float v0 = (float) frame / style.frames(), v1 = (float) (frame + 1) / style.frames();
            final float side = mirrored ? -0.5F : 0.5F;
            final int color = color(alpha);
            final PoseStack.Pose pose = poseStack.last();
            horizontal(consumer, pose, side, -0.5F, 0, v0, color);
            horizontal(consumer, pose, side, 0.5F, 1, v0, color);
            horizontal(consumer, pose, -side, 0.5F, 1, v1, color);
            horizontal(consumer, pose, -side, -0.5F, 0, v1, color);
        }

        /** Кольцо вокруг игрока, проворачивается и расходится. */
        private void renderSwirl(PoseStack poseStack, VertexConsumer consumer, Entity owner, float partialTick, float progress) {
            final int frame = Math.min(style.frames() - 1, (int) (progress * style.frames()));
            final float alpha = progress < 0.5F ? 1.0F : 1.0F - (progress - 0.5F) / 0.5F;
            final float scale = size * Mth.lerp(Math.min(1.0F, progress * 2.0F), 0.6F, 1.1F);
            poseStack.mulPose(Axis.XP.rotationDegrees(owner.getViewXRot(partialTick) * 0.6F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
            poseStack.mulPose(Axis.YP.rotationDegrees((mirrored ? -1 : 1) * progress * 220.0F));
            poseStack.scale(scale, scale, scale);
            final float v0 = (float) frame / style.frames(), v1 = (float) (frame + 1) / style.frames();
            final int color = color(alpha);
            final PoseStack.Pose pose = poseStack.last();
            horizontal(consumer, pose, 0.5F, -0.5F, 0, v0, color);
            horizontal(consumer, pose, 0.5F, 0.5F, 1, v0, color);
            horizontal(consumer, pose, -0.5F, 0.5F, 1, v1, color);
            horizontal(consumer, pose, -0.5F, -0.5F, 0, v1, color);
        }

        /** Печать перед игроком, лицом к взгляду: вспыхивает, проворачивается и гаснет. */
        private void renderSeal(PoseStack poseStack, VertexConsumer consumer, Entity owner, float partialTick, float progress) {
            final float alpha = progress < 0.25F ? progress / 0.25F : 1.0F - (progress - 0.25F) / 0.75F;
            final float scale = size * Mth.lerp(Math.min(1.0F, progress * 3.0F), 0.5F, 1.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(owner.getViewXRot(partialTick)));
            poseStack.translate(0, 0, 1.1F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(roll + (mirrored ? -1 : 1) * progress * 90.0F));
            poseStack.scale(scale, scale, scale);
            final int color = ((int) (alpha * 255) << 24) | 0xFFFFFF;
            final PoseStack.Pose pose = poseStack.last();
            vertex(consumer, pose, -0.5F, -0.5F, 0, 0, 1, color);
            vertex(consumer, pose, 0.5F, -0.5F, 0, 1, 1, color);
            vertex(consumer, pose, 0.5F, 0.5F, 0, 1, 0, color);
            vertex(consumer, pose, -0.5F, 0.5F, 0, 0, 0, color);
        }

        private int color(float alpha) {
            return ((int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255) << 24) | (style.tint() & 0xFFFFFF);
        }

        private static void horizontal(VertexConsumer consumer, PoseStack.Pose pose, float x, float z, float u, float v, int color) {
            vertex(consumer, pose, x, 0, z, u, v, color);
        }

        private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int color) {
            consumer.addVertex(pose, x, y, z)
                    .setColor(color)
                    .setUv(u, v)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(pose, 0, 1, 0);
        }
    }
}

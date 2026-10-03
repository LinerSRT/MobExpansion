package net.mcskill.mobexpansion.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.blockentity.MobSpawnerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4fStack;

import java.util.OptionalDouble;

@EventBusSubscriber(modid = Core.MODID, value = Dist.CLIENT)
public final class MobSpawnerRevealHandler {
    private static final RenderType XRAY_LINES = RenderType.create(
            "mobexpansion_spawner_xray_lines",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            1536,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(2.0D)))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(RenderStateShard.MAIN_TARGET)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false)
    );

    private static boolean revealing;

    private MobSpawnerRevealHandler() {
    }

    public static boolean isRevealing() {
        return revealing;
    }

    public static void setServerAllowed(boolean allowed) {
        if (allowed == revealing)
            return;
        revealing = allowed;
        refreshMaskedSpawners(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL)
            return;
        if (!revealing)
            return;
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null)
            return;
        final Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.mul(event.getModelViewMatrix());
        RenderSystem.applyModelViewMatrix();
        final PoseStack poseStack = new PoseStack();
        final Vec3 camera = event.getCamera().getPosition();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        final MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        final VertexConsumer consumer = bufferSource.getBuffer(XRAY_LINES);
        forEachMaskedSpawner(minecraft, spawner -> {
            final AABB box = new AABB(spawner.getBlockPos()).inflate(0.002D);
            if (!event.getFrustum().isVisible(box))
                return;
            LevelRenderer.renderLineBox(poseStack, consumer, box, 0.35F, 0.9F, 1.0F, 0.9F);
        });
        bufferSource.endBatch(XRAY_LINES);

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();

        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    private static void refreshMaskedSpawners(Minecraft minecraft) {
        if (minecraft.level == null)
            return;
        forEachMaskedSpawner(minecraft, spawner -> {
            spawner.requestModelDataUpdate();
            final BlockPos pos = spawner.getBlockPos();
            final BlockState state = spawner.getBlockState();
            minecraft.level.sendBlockUpdated(pos, state, state, 8);
            minecraft.levelRenderer.setBlocksDirty(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
        });
    }

    private static void forEachMaskedSpawner(Minecraft minecraft, java.util.function.Consumer<MobSpawnerBlockEntity> consumer) {
        final ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null)
            return;
        final int radius = minecraft.options.renderDistance().get() + 1;
        final int playerChunkX = minecraft.player.chunkPosition().x;
        final int playerChunkZ = minecraft.player.chunkPosition().z;
        for (int chunkX = playerChunkX - radius; chunkX <= playerChunkX + radius; chunkX++) {
            for (int chunkZ = playerChunkZ - radius; chunkZ <= playerChunkZ + radius; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ))
                    continue;
                final LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof MobSpawnerBlockEntity spawner)
                        consumer.accept(spawner);
                }
            }
        }
    }
}

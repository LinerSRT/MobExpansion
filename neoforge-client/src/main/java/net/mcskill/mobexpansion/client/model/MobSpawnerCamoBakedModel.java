package net.mcskill.mobexpansion.client.model;

import net.mcskill.mobexpansion.client.MobSpawnerRevealHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MobSpawnerCamoBakedModel extends BakedModelWrapper<BakedModel> {
    public static final ModelProperty<BlockState> MASK = new ModelProperty<>();

    public MobSpawnerCamoBakedModel(BakedModel originalModel) {
        super(originalModel);
    }

    @Nullable
    private static BlockState maskOf(ModelData data) {
        if (MobSpawnerRevealHandler.isRevealing())
            return null;
        return data.get(MASK);
    }

    private static BakedModel modelOf(BlockState maskState) {
        return Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getBlockModel(maskState);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, @Nullable RenderType renderType) {
        final BlockState maskState = maskOf(extraData);
        if (maskState != null)
            return modelOf(maskState).getQuads(maskState, side, rand, ModelData.EMPTY, renderType);
        return super.getQuads(state, side, rand, extraData, renderType);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        final BlockState maskState = maskOf(data);
        if (maskState != null)
            return modelOf(maskState).getRenderTypes(maskState, rand, ModelData.EMPTY);
        return super.getRenderTypes(state, rand, data);
    }

    @Override
    public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType) {
        final BlockState maskState = maskOf(data);
        if (maskState != null)
            return modelOf(maskState).useAmbientOcclusion(maskState, ModelData.EMPTY, renderType);
        return super.useAmbientOcclusion(state, data, renderType);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        final BlockState maskState = maskOf(data);
        if (maskState != null)
            return modelOf(maskState).getParticleIcon(ModelData.EMPTY);
        return super.getParticleIcon(data);
    }
}

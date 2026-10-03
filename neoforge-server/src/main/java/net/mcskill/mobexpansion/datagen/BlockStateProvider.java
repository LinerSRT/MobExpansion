package net.mcskill.mobexpansion.datagen;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.init.MobExBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class BlockStateProvider extends net.neoforged.neoforge.client.model.generators.BlockStateProvider {
    public BlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Core.MODID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlockWithItem(MobExBlocks.MOB_SPAWNER.get(), models().cubeAll("mob_spawner", mcLoc("block/spawner")).renderType("cutout"));
    }
}

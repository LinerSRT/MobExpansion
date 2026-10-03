package net.mcskill.mobexpansion.datagen;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.init.MobExSounds;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;

import java.util.stream.IntStream;

public class SoundsProvider extends SoundDefinitionsProvider {
    protected SoundsProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, Core.MODID, helper);
    }

    @Override
    public void registerSounds() {
        add(MobExSounds.SENTINEL_GM1.get(), Core.loc("sentinel/gm1"));
        add(MobExSounds.SENTINEL_GM2.get(), Core.loc("sentinel/gm2"));
        add(MobExSounds.SENTINEL_GM3.get(), Core.loc("sentinel/gm3"));
        add(MobExSounds.SENTINEL_GM4.get(), Core.loc("sentinel/gm4"));
        add(MobExSounds.SENTINEL_GM5.get(), Core.loc("sentinel/gm5"));
        add(MobExSounds.SENTINEL_GM6.get(), Core.loc("sentinel/gm6"));
        add(MobExSounds.SENTINEL_GM7.get(), Core.loc("sentinel/gm7"));
    }

    protected void add(SoundEvent soundEvent, ResourceLocation location) {
        add(soundEvent.getLocation(), definition().with(sound(location).volume(1).pitch(1)));
    }

    protected void add(SoundEvent soundEvent, ResourceLocation location, String subtitle) {
        add(soundEvent.getLocation(), definition().with(sound(location).volume(1).pitch(1)).subtitle(subtitle));
    }

    protected void add(SoundEvent soundEvent, ResourceLocation location, int variants) {
        add(soundEvent.getLocation(), definition().with(IntStream.range(1, variants + 1).mapToObj(value -> sound(location.withSuffix(String.valueOf(value))).volume(1).pitch(1)).toList().toArray(new SoundDefinition.Sound[0])));
    }

    protected void add(SoundEvent soundEvent, ResourceLocation location, int variants, String subtitle) {
        add(soundEvent.getLocation(), definition().with(IntStream.range(1, variants + 1).mapToObj(value -> sound(location.withSuffix(String.valueOf(value))).volume(1).pitch(1)).toList().toArray(new SoundDefinition.Sound[0])).subtitle(subtitle));
    }
}

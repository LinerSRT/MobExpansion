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
        add(MobExSounds.APOCALYPSE_BASIC_ATTACK1.get(), Core.loc("apocalypse/basic_attack1"));
        add(MobExSounds.APOCALYPSE_BASIC_ATTACK2.get(), Core.loc("apocalypse/basic_attack2"));
        add(MobExSounds.APOCALYPSE_BEAM.get(), Core.loc("apocalypse/beam"));
        add(MobExSounds.APOCALYPSE_BLACK_HOLE.get(), Core.loc("apocalypse/black_hole"));
        add(MobExSounds.APOCALYPSE_BLACK_STAR.get(), Core.loc("apocalypse/black_star"));
        add(MobExSounds.APOCALYPSE_BLACKBOX_BUILD.get(), Core.loc("apocalypse/blackbox_build"));
        add(MobExSounds.APOCALYPSE_BUILDINGS_COLLAPSE.get(), Core.loc("apocalypse/buildings_collapse"));
        add(MobExSounds.APOCALYPSE_COMBO1.get(), Core.loc("apocalypse/combo1"));
        add(MobExSounds.APOCALYPSE_COMBO2.get(), Core.loc("apocalypse/combo2"));
        add(MobExSounds.APOCALYPSE_COMBO3.get(), Core.loc("apocalypse/combo3"));
        add(MobExSounds.APOCALYPSE_COMBO_X.get(), Core.loc("apocalypse/combo_x"));
        add(MobExSounds.APOCALYPSE_GRAB.get(), Core.loc("apocalypse/grab"));
        add(MobExSounds.APOCALYPSE_SWORDS_WHOOSH.get(), Core.loc("apocalypse/swords_whoosh"));
        add(MobExSounds.APOCALYPSE_TELEPORT.get(), Core.loc("apocalypse/teleport"));
        add(MobExSounds.APOCALYPSE_THROW.get(), Core.loc("apocalypse/throw"));
        add(MobExSounds.APOCALYPSE_UPPERCUT.get(), Core.loc("apocalypse/uppercut"));
        add(MobExSounds.APOCALYPSE_VANISH.get(), Core.loc("apocalypse/vanish"));
        add(MobExSounds.APOCALYPSE_VOID_STORM.get(), Core.loc("apocalypse/void_storm"));
        add(MobExSounds.APOCALYPSE_WHITE_VOID.get(), Core.loc("apocalypse/white_void"));
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

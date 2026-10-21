package net.mcskill.mobexpansion.client.skin;

import net.mcskill.mobexpansion.Core;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Как выглядит удар игрока со скином (см. {@link SkinAttackFx}).
 *
 * @param slash       текстура взмаха: вертикальная полоса из {@code frames} кадров
 * @param tint        цвет, на который умножается текстура взмаха (RGB)
 * @param spin        {@code false} — дуга проходит перед игроком (взмах), {@code true} — кольцо вокруг игрока
 *                    проворачивается (вихрь)
 * @param trail       основные частицы по дуге удара
 * @param accent      редкие частицы по дуге (каждая {@code accentChance}-я)
 * @param critBurst   частицы у груди на крите
 * @param critSound   звук крита
 * @param critSeal    печать, вспыхивающая перед игроком на крите (квадратная текстура), или null
 */
public record SkinAttackStyle(ResourceLocation slash, int frames, int tint, boolean spin,
                              ParticleOptions trail, ParticleOptions accent, int accentChance,
                              ParticleOptions critBurst, SoundEvent critSound, @Nullable ResourceLocation critSeal) {

    /** Огненный взмах косой. */
    public static final SkinAttackStyle CAPRA = new SkinAttackStyle(Core.loc("textures/effect/capra_slash.png"), 5, 0xFFFFFF, false,
            ParticleTypes.FLAME, ParticleTypes.SMALL_FLAME, 3, ParticleTypes.LAVA, SoundEvents.FIRECHARGE_USE, null);

    /** Розовый вихрь, искры и сердечки; на крите — красная пентаграмма. */
    public static final SkinAttackStyle LILLITH = new SkinAttackStyle(Core.loc("textures/effect/lillith_swirl.png"), 1, 0xFF6FB4, true,
            new DustParticleOptions(new Vector3f(1.0F, 0.35F, 0.65F), 1.0F), ParticleTypes.HEART, 4,
            ParticleTypes.HEART, SoundEvents.EVOKER_CAST_SPELL, Core.loc("textures/effect/lillith_seal.png"));
}

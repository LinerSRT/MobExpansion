package net.mcskill.mobexpansion.init;

import com.mojang.serialization.MapCodec;
import net.mcskill.mobexpansion.Core;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;

public class MobExParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, Core.MODID);

    private static <T extends ParticleOptions> DeferredHolder<ParticleType<?>, ParticleType<T>> register(String name, boolean overrideLimiter, final Function<ParticleType<T>, MapCodec<T>> codecGetter, final Function<ParticleType<T>, StreamCodec<? super RegistryFriendlyByteBuf, T>> streamCodecGetter) {
        return PARTICLE_TYPES.register(name, new Supplier<>() {
            @Override
            public ParticleType<T> get() {
                return new ParticleType<>(overrideLimiter) {
                    @NotNull
                    public MapCodec<T> codec() {
                        return codecGetter.apply(this);
                    }

                    @NotNull
                    public StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec() {
                        return streamCodecGetter.apply(this);
                    }
                };
            }
        });
    }

    public static void register(IEventBus bus) {
        PARTICLE_TYPES.register(bus);
    }
}

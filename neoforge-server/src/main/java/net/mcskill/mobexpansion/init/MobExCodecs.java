package net.mcskill.mobexpansion.init;

import com.mojang.serialization.MapCodec;
import net.mcskill.mobexpansion.block.MobSpawnerBlock;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class MobExCodecs {
    public static final MapCodec<MobSpawnerBlock> MOB_SPAWNER_CODEC = BlockBehaviour.simpleCodec(MobSpawnerBlock::new);


    public static <B, C, T1, T2, T3, T4, T5, T6, T7> StreamCodec<B, C> composite(
            final StreamCodec<? super B, T1> codec1, final Function<C, T1> getter1,
            final StreamCodec<? super B, T2> codec2, final Function<C, T2> getter2,
            final StreamCodec<? super B, T3> codec3, final Function<C, T3> getter3,
            final StreamCodec<? super B, T4> codec4, final Function<C, T4> getter4,
            final StreamCodec<? super B, T5> codec5, final Function<C, T5> getter5,
            final StreamCodec<? super B, T6> codec6, final Function<C, T6> getter6,
            final StreamCodec<? super B, T7> codec7, final Function<C, T7> getter7,
            final Function7<T1, T2, T3, T4, T5, T6, T7, C> factory) {
        return new StreamCodec<>() {
            @Override
            @NotNull
            public C decode(@NotNull B buffer) {
                T1 t1 = codec1.decode(buffer);
                T2 t2 = codec2.decode(buffer);
                T3 t3 = codec3.decode(buffer);
                T4 t4 = codec4.decode(buffer);
                T5 t5 = codec5.decode(buffer);
                T6 t6 = codec6.decode(buffer);
                T7 t7 = codec7.decode(buffer);
                return factory.apply(t1, t2, t3, t4, t5, t6, t7);
            }

            @Override
            public void encode(@NotNull B buffer, @NotNull C data) {
                codec1.encode(buffer, getter1.apply(data));
                codec2.encode(buffer, getter2.apply(data));
                codec3.encode(buffer, getter3.apply(data));
                codec4.encode(buffer, getter4.apply(data));
                codec5.encode(buffer, getter5.apply(data));
                codec6.encode(buffer, getter6.apply(data));
                codec7.encode(buffer, getter7.apply(data));
            }
        };
    }

    public interface Function7<T1, T2, T3, T4, T5, T6, T7, R> {
        R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7);
    }
}

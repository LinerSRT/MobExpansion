package net.mcskill.mobexpansion.spawner;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;

/**
 * «Дом» моба: центр (спавнер) и радиус, дальше которого моб не уходит. Хранится в persistent-данных самого моба
 * (сохраняется вместе с ним, спавнеру не нужно следить за мобами — нагрузка только на проверку раз в несколько тиков).
 */
public final class MobHome {
    private static final String KEY = "mobexpansion_home";
    private static final String POS = "Pos";
    private static final String RADIUS = "Radius";

    private final BlockPos center;
    private final int radius;
    private final double radiusSqr;

    private MobHome(BlockPos center, int radius) {
        this.center = center;
        this.radius = radius;
        this.radiusSqr = (double) radius * radius;
    }

    public static void set(Mob mob, BlockPos center, int radius) {
        if (radius < SpawnerEntityEntry.MIN_LEASH + 1)
            return;
        final CompoundTag tag = new CompoundTag();
        tag.putLong(POS, center.asLong());
        tag.putInt(RADIUS, Math.min(radius, SpawnerEntityEntry.MAX_LEASH));
        mob.getPersistentData().put(KEY, tag);
        apply(mob, center, radius);
    }

    @Nullable
    public static MobHome get(Mob mob) {
        final CompoundTag data = mob.getPersistentData();
        if (!data.contains(KEY, Tag.TAG_COMPOUND))
            return null;
        final CompoundTag tag = data.getCompound(KEY);
        final int radius = tag.getInt(RADIUS);
        if (radius <= 0)
            return null;
        return new MobHome(BlockPos.of(tag.getLong(POS)), radius);
    }

    /** Ванильное ограничение направляет блуждание (RandomStroll и т.п.); жёсткий предел держит {@code MobHomeEvents}. */
    public static void applyTo(Mob mob) {
        final MobHome home = get(mob);
        if (home != null)
            apply(mob, home.center, home.radius);
    }

    private static void apply(Mob mob, BlockPos center, int radius) {
        mob.restrictTo(center, radius);
    }

    public BlockPos center() {
        return center;
    }

    public int radius() {
        return radius;
    }

    /** Точка в пределах дома (по центру блока спавнера). */
    public boolean contains(double x, double y, double z) {
        final double dx = x - (center.getX() + 0.5D);
        final double dy = y - (center.getY() + 0.5D);
        final double dz = z - (center.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz <= radiusSqr;
    }
}

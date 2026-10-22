package net.mcskill.mobexpansion.spawner;

import net.mcskill.mobexpansion.attributes.EntityExtraParametersCatalog;
import net.mcskill.mobexpansion.init.MobExParameters;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;

/**
 * «Дом» моба: центр (спавнер) и радиус, дальше которого моб не уходит. Центр хранится в persistent-данных самого моба
 * (сохраняется вместе с ним, спавнеру не нужно следить за мобами — нагрузка только на проверку раз в несколько тиков).
 * Радиус задаётся для типа моба в конфигураторе мобов (доп. параметр {@code spawnerHomeRadius}) и читается на лету,
 * так что изменения сразу действуют и на уже заспавненных мобов. 0 — привязка выключена.
 */
public final class MobHome {
    private static final String KEY = "mobexpansion_home";
    private static final String POS = "Pos";

    private final BlockPos center;
    private final int radius;
    private final double radiusSqr;

    private MobHome(BlockPos center, int radius) {
        this.center = center;
        this.radius = radius;
        this.radiusSqr = (double) radius * radius;
    }

    /** Радиус дома для типа моба из конфигуратора (0 — без привязки). */
    public static int radiusFor(@Nullable ResourceLocation entityId) {
        final double radius = MobExParameters.get(entityId, EntityExtraParametersCatalog.SPAWNER_HOME_RADIUS, 0.0D);
        return Mth.clamp((int) Math.round(radius), 0, EntityExtraParametersCatalog.MAX_SPAWNER_HOME_RADIUS);
    }

    public static int radiusFor(Mob mob) {
        return radiusFor(MobExParameters.idOf(mob));
    }

    /** Запомнить спавнер как дом моба. Привязка работает, пока у типа моба в конфигураторе радиус больше 0. */
    public static void set(Mob mob, BlockPos center) {
        final CompoundTag tag = new CompoundTag();
        tag.putLong(POS, center.asLong());
        mob.getPersistentData().put(KEY, tag);
        applyTo(mob);
    }

    public static boolean hasHome(Mob mob) {
        return mob.getPersistentData().contains(KEY, Tag.TAG_COMPOUND);
    }

    @Nullable
    public static MobHome get(Mob mob) {
        if (!hasHome(mob))
            return null;
        final int radius = radiusFor(mob);
        if (radius <= 0)
            return null;
        return new MobHome(BlockPos.of(mob.getPersistentData().getCompound(KEY).getLong(POS)), radius);
    }

    /** Ванильное ограничение направляет блуждание (RandomStroll и т.п.); жёсткий предел держит {@code MobExHomeEvents}. */
    public static void applyTo(Mob mob) {
        final MobHome home = get(mob);
        if (home != null)
            mob.restrictTo(home.center, home.radius);
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

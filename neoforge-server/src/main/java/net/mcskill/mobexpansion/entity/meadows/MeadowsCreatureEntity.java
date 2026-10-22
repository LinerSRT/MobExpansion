package net.mcskill.mobexpansion.entity.meadows;

import net.mcskill.mobexpansion.entity.ability.AbilityMobEntity;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/**
 * База существ Луга (Meadows): общая фракция, имена ассетов по id и безмолвность оригинала
 * («Silent: true» — у существ только звуки способностей).
 */
public abstract class MeadowsCreatureEntity extends AbilityMobEntity {
    public static final String FACTION = "meadows";

    protected MeadowsCreatureEntity(EntityType<? extends MeadowsCreatureEntity> entityType, Level level) {
        super(entityType, level);
    }

    /** id моба = имя geo/animation/texture. */
    protected abstract String id();

    @Override
    public String faction() {
        return FACTION;
    }

    @Override
    protected String idleAnimation() {
        return "waiting";
    }

    @Override
    protected String walkAnimation() {
        return "walking";
    }

    @Override
    @Nullable
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    public String modelName() {
        return id();
    }

    @Override
    public String textureName() {
        return id();
    }

    @Override
    public String animationName() {
        return id();
    }

    protected static List<AbilityMobEntity> alliesNear(AbilityMobEntity mob) {
        return mob.level().getEntitiesOfClass(AbilityMobEntity.class, mob.getBoundingBox().inflate(25.0D), ally -> ally.isAlive() && mob.isAlly(ally));
    }

    /** Розово-зелёная пыльца Луга. */
    protected static DustColorTransitionOptions pollen(float size) {
        return new DustColorTransitionOptions(new Vector3f(0.875F, 0.52F, 0.647F), new Vector3f(0.384F, 0.675F, 0.298F), size);
    }

    protected static DustParticleOptions dust(float red, float green, float blue, float size) {
        return new DustParticleOptions(new Vector3f(red, green, blue), size);
    }
}

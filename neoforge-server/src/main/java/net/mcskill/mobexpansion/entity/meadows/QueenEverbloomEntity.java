package net.mcskill.mobexpansion.entity.meadows;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.mcskill.mobexpansion.init.MobExEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Королева Эвербллум — босс Луга, первая форма. Павшая, не умирает насовсем: на месте тела расцветает
 * алый цветок, и из него поднимается {@link ScarletQueenEverbloomEntity}.
 */
public class QueenEverbloomEntity extends EverbloomQueenEntity {
    private boolean transformed;

    public QueenEverbloomEntity(EntityType<? extends QueenEverbloomEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 200;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected boolean angry() {
        return false;
    }

    @Override
    protected void tickDeath() {
        if (!level().isClientSide() && !transformed) {
            // цветок расцветает под телом, а в конце анимации смерти встаёт алая королева
            if (deathTime == 40)
                spawnEffect("scarletbloom", position(), 80).animation("spawn").glowing(false);
            if (deathTime + 1 >= deathTicks()) {
                transformed = true;
                particles(ParticleTypes.CHERRY_LEAVES, position().add(0.0D, 1.5D, 0.0D), 60, 1.5, 0.1);
                summon(MobExEntities.SCARLET_QUEEN_EVERBLOOM.get(), position());
            }
        }
        super.tickDeath();
    }

    @Override
    protected int deathTicks() {
        return Ability.ticks(3.0);
    }

    @Override
    @Nullable
    protected BossEvent.BossBarColor bossBarColor() {
        return BossEvent.BossBarColor.PINK;
    }

    @Override
    protected String id() {
        return "queen_everbloom";
    }
}

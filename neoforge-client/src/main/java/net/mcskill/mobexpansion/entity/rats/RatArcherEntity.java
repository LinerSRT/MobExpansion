package net.mcskill.mobexpansion.entity.rats;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.mcskill.mobexpansion.entity.effect.MobExProjectileEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Крыса-лучник: держит дистанцию и бьёт из лука (стрела — четверной урон), подошедших — бьёт луком.
 * Стрела в руке видна только во время выстрела.
 */
public class RatArcherEntity extends RatClanEntity {
    private static final String ARROW_BONE = "bow_arrow";
    private final Ability spawn = Ability.builder("spawn").seconds(1.0).cooldown(0).when((mob, target) -> false).unstoppable().build();

    public RatArcherEntity(EntityType<? extends RatArcherEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D);
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        abilities.add(Ability.builder("shoot").seconds(2.5).range(4.0, 20.0).cooldown(80).weight(3)
                .at(30, (mob, target) -> mob.sound(SoundEvents.ARROW_SHOOT, 1.5F, 1.0F))
                .at(31, (mob, target) -> {
                    if (target == null)
                        return;
                    final MobExProjectileEntity arrow = mob.projectile("", mob.localPos(0.3D, 1.5D, 0.8D))
                            .item(new ItemStack(Items.ARROW))
                            .damage(mob.damage(4.0D))
                            .life(60)
                            .trail(ParticleTypes.CRIT, 1)
                            .impact(ParticleTypes.CRIT, SoundEvents.ARROW_HIT);
                    mob.shootAt(arrow, target, 1.6F, 1.0F);
                })
                .build());

        abilities.add(Ability.builder("right_attack").seconds(1.75).range(0, 4.0).cooldown(80).weight(3)
                .at(12, (mob, target) -> woosh(200))
                .every(10, 14, 1, (mob, target, tick) -> step(0.3D))
                .at(13, (mob, target) -> slash(mob.localPos(0.5D, 1.1D, 1.5D), 2.0D, 2.0D, 50, 0.3D, 0.1D))
                .build());
    }

    @Override
    @Nullable
    protected Ability spawnAbility() {
        return spawn;
    }

    @Override
    protected CombatStyle combatStyle() {
        return CombatStyle.KITE;
    }

    @Override
    public double preferredMinRange() {
        return 6.0D;
    }

    @Override
    public double preferredMaxRange() {
        return 14.0D;
    }

    @Override
    protected int deathTicks() {
        return Ability.ticks(3.0);
    }

    @Override
    public List<String> toggledBones() {
        return List.of(ARROW_BONE);
    }

    @Override
    public boolean isBoneVisible(String bone) {
        return !isDeadOrDying() && "shoot".equals(playingAnimation());
    }
}

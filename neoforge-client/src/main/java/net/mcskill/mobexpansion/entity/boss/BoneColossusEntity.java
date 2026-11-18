package net.mcskill.mobexpansion.entity.boss;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.mcskill.mobexpansion.entity.ability.AbilityMobEntity;
import net.mcskill.mobexpansion.init.MobExEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Костяной колосс — медленный скелет-гигант ростом ~4,5 блока (модель из пака Skeleton Boss).
 * <ul>
 *     <li>взмахи левой и правой рукой — конус 180° перед собой, сильно отбрасывает и подбрасывает;</li>
 *     <li>топот — удар конусом и костяной капкан под целью: несколько секунд колет и замедляет;</li>
 *     <li>ядовитое дыхание — облако вокруг колосса на 9,5 с: яд и урон каждые полсекунды;</li>
 *     <li>рёв (и при появлении) — отталкивает всех вокруг и поднимает {@link BoneServantEntity}
 *     (не больше {@link #MAX_SERVANTS} рядом).</li>
 * </ul>
 */
public class BoneColossusEntity extends AbilityMobEntity {
    public static final String FACTION = "bone";
    private static final int MAX_SERVANTS = 6;

    public BoneColossusEntity(EntityType<? extends BoneColossusEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 180;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.ATTACK_DAMAGE, 18.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    public String faction() {
        return FACTION;
    }

    // ---------------------------------------------------------------- способности

    private static Ability swipe(String name) {
        return Ability.builder(name).seconds(2.5).range(0, 6.5).cooldown(80).weight(4)
                .at(25, (mob, target) -> {
                    mob.hitCone(6.0, 180.0F, mob.damage(1.0), 1.6, 1.0);
                    mob.particles(ParticleTypes.SWEEP_ATTACK, mob.forwardPos(3.0D, 1.5D), 4, 1.2, 0.0);
                    mob.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.5F);
                })
                .build();
    }

    private static Ability stomp(String name) {
        return Ability.builder(name).seconds(3.0).range(0, 6.5).cooldown(240).weight(2)
                .at(25, (mob, target) -> {
                    mob.hitCone(6.0, 180.0F, mob.damage(1.2), 0.8, 0.5);
                    mob.particleRing(ParticleTypes.CAMPFIRE_COSY_SMOKE, mob.forwardPos(2.5D, 0.2D), 3.0D, 24);
                    mob.sound(SoundEvents.GENERIC_EXPLODE.value(), 1.4F, 0.6F);
                    final Vec3 spot = mob.groundAt(target != null ? target.position() : mob.forwardPos(3.0D, 0.0D));
                    final float spike = mob.damage(0.5);
                    mob.spawnEffect("bone_trap", spot, 80).animation("spawn").glowing(false).ticker((effect, age) -> {
                        if (age >= 22 && age % 10 == 2)
                            for (LivingEntity hit : mob.hitArea(effect.position().add(0.0D, 0.5D, 0.0D), 1.5D, spike, 0.0, 0.3))
                                mob.effect(hit, MobEffects.MOVEMENT_SLOWDOWN, 40, 2);
                    });
                })
                .build();
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        abilities.add(swipe("atk_rswipe"));
        abilities.add(swipe("atk_lswipe"));
        abilities.add(stomp("skl_rstomp"));
        abilities.add(stomp("skl_lstomp"));
        abilities.add(Ability.builder("skl_poisonbreath").seconds(3.0).range(0, 8.0).cooldown(300).weight(2)
                .at(15, (mob, target) -> {
                    final float tickDamage = mob.damage(0.25);
                    mob.spawnEffect("bone_poison_cloud", mob.position(), 190).animation("spawn").glowing(false).ticker((effect, age) -> {
                        if (age % 10 != 0)
                            return;
                        for (LivingEntity hit : mob.hitArea(effect.position().add(0.0D, 1.0D, 0.0D), 6.0D, tickDamage, 0.4, 0.2))
                            mob.effect(hit, MobEffects.POISON, 60, 1);
                        mob.particles(ParticleTypes.SNEEZE, effect.position().add(0.0D, 1.0D, 0.0D), 20, 3.0, 0.02);
                    });
                    mob.sound(SoundEvents.ENDER_DRAGON_SHOOT, 1.6F, 0.6F);
                })
                .build());
        abilities.add(roar());
    }

    private static Ability roar() {
        return Ability.builder("skl_roar").seconds(4.5).range(0, 16.0).cooldown(320).weight(1).ignoreSight()
                .at(20, (mob, target) -> mob.sound(SoundEvents.RAVAGER_ROAR, 2.0F, 0.5F))
                .every(26, 66, 10, (mob, target, tick) -> {
                    for (LivingEntity hit : mob.enemiesNear(mob.position().add(0.0D, 1.0D, 0.0D), 6.0D))
                        AbilityMobEntity.push(hit, mob.position(), 1.2D, 0.6D);
                    mob.particleRing(ParticleTypes.POOF, mob.position().add(0.0D, 0.3D, 0.0D), 5.0D, 30);
                })
                .at(30, (mob, target) -> {
                    if (!(mob instanceof BoneColossusEntity colossus))
                        return;
                    for (int i = 0; i < 2 && colossus.servantsNearby() < MAX_SERVANTS; i++) {
                        final double angle = mob.getRandom().nextDouble() * Math.PI * 2.0D;
                        mob.summon(MobExEntities.BONE_SERVANT.get(), mob.groundAt(mob.position().add(Math.cos(angle) * 4.0D, 0.0D, Math.sin(angle) * 4.0D)));
                    }
                })
                .build();
    }

    private int servantsNearby() {
        return level().getEntitiesOfClass(BoneServantEntity.class, getBoundingBox().inflate(32.0D)).size();
    }

    @Override
    @Nullable
    protected Ability spawnAbility() {
        return abilities().stream().filter(ability -> "skl_roar".equals(ability.name())).findFirst().orElse(null);
    }

    // ---------------------------------------------------------------- прочее

    @Override
    protected int deathTicks() {
        return Ability.ticks(6.25) + 10;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public String modelName() {
        return "bone_colossus";
    }

    @Override
    public String textureName() {
        return "bone_colossus";
    }

    @Override
    public String animationName() {
        return "bone_colossus";
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_AMBIENT;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F;
    }

    @Override
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.WITHER_HURT;
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return SoundEvents.SKELETON_DEATH;
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState state) {
        playSound(SoundEvents.SKELETON_STEP, 1.2F, 0.4F);
    }
}

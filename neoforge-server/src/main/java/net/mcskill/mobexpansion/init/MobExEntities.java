package net.mcskill.mobexpansion.init;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.entity.IModel;
import net.mcskill.mobexpansion.entity.PoisonProjectileEntity;
import net.mcskill.mobexpansion.entity.PoisonSpiderEntity;
import net.mcskill.mobexpansion.entity.RegularSpiderEntity;
import net.mcskill.mobexpansion.entity.SpiderSpawnerEntity;
import net.mcskill.mobexpansion.entity.redstone.RedstoneAutomatonEntity;
import net.mcskill.mobexpansion.entity.redstone.RedstoneDroneEntity;
import net.mcskill.mobexpansion.entity.redstone.RedstoneEngineerEntity;
import net.mcskill.mobexpansion.entity.redstone.RedstoneProjectileEntity;
import net.mcskill.mobexpansion.entity.redstone.RedstoneTowerEntity;
import net.mcskill.mobexpansion.entity.redstone.WrenchProjectileEntity;
import net.mcskill.mobexpansion.entity.SentinelGolemEntity;
import net.mcskill.mobexpansion.entity.SentinelStatueEntity;
import net.mcskill.mobexpansion.entity.LeechEntity;
import net.mcskill.mobexpansion.entity.MosquitoEntity;
import net.mcskill.mobexpansion.entity.RatEntity;
import net.mcskill.mobexpansion.entity.RatKingEntity;
import net.mcskill.mobexpansion.entity.SewerBoltProjectileEntity;
import net.mcskill.mobexpansion.entity.SewerPoisonAreaEntity;
import net.mcskill.mobexpansion.entity.SewerPotionProjectileEntity;
import net.mcskill.mobexpansion.entity.robot.SwarmerAntEntity;
import net.mcskill.mobexpansion.entity.robot.BeetleTrooperEntity;
import net.mcskill.mobexpansion.entity.robot.GunshipMothEntity;
import net.mcskill.mobexpansion.entity.robot.HuntsmanSpiderEntity;
import net.mcskill.mobexpansion.entity.robot.LadybugMortarEntity;
import net.mcskill.mobexpansion.entity.robot.WoodlouseTankEntity;
import net.mcskill.mobexpansion.entity.sculk.SculkCrabbyEntity;
import net.mcskill.mobexpansion.entity.sculk.SculkedCrustaceanEntity;
import net.mcskill.mobexpansion.entity.sculk.SculkScreamerEntity;
import net.mcskill.mobexpansion.entity.sculk.SculkSentinelEntity;
import net.mcskill.mobexpansion.entity.sculk.EchoEnshrouderEntity;
import net.mcskill.mobexpansion.entity.sculk.SoulBearerEntity;
import net.mcskill.mobexpansion.entity.possessed.PossessedCinderArmorEntity;
import net.mcskill.mobexpansion.entity.possessed.PossessedRuinArmorEntity;
import net.mcskill.mobexpansion.entity.possessed.PossessedVikingArmorEntity;
import net.mcskill.mobexpansion.entity.apocalypse.ApocalypseTypeAEntity;
import net.mcskill.mobexpansion.entity.meadows.BlossomDrakeEntity;
import net.mcskill.mobexpansion.entity.meadows.FloralSerpentEntity;
import net.mcskill.mobexpansion.entity.meadows.GlimmeringUnicornEntity;
import net.mcskill.mobexpansion.entity.meadows.SerenitySentinelEntity;
import net.mcskill.mobexpansion.entity.meadows.QueenEverbloomEntity;
import net.mcskill.mobexpansion.entity.meadows.ScarletQueenEverbloomEntity;
import net.mcskill.mobexpansion.entity.effect.MobExEffectEntity;
import net.mcskill.mobexpansion.entity.effect.MobExProjectileEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import software.bernie.geckolib.animatable.GeoAnimatable;

@EventBusSubscriber(modid = Core.MODID)
public class MobExEntities {
    public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Core.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<RegularSpiderEntity>> REGULAR_SPIDER = register("regular_spider", RegularSpiderEntity::new, 1.75F, 1.125F);
    public static final DeferredHolder<EntityType<?>, EntityType<PoisonSpiderEntity>> POISON_SPIDER = register("poison_spider", PoisonSpiderEntity::new, 1.75F, 1.125F);
    public static final DeferredHolder<EntityType<?>, EntityType<SpiderSpawnerEntity>> SPIDER_SPAWN = register("spider_spawn", SpiderSpawnerEntity::new, 1.5F, 2.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<PoisonProjectileEntity>> POISON_PROJECTILE = registerProjectile("poison_projectile", PoisonProjectileEntity::new, 0.4F, 0.4F);

    public static final DeferredHolder<EntityType<?>, EntityType<RedstoneEngineerEntity>> REDSTONE_ENGINEER = register("redstone_engineer", RedstoneEngineerEntity::new, 0.6F, 1.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<RedstoneAutomatonEntity>> REDSTONE_AUTOMATON = register("redstone_automaton", RedstoneAutomatonEntity::new, 0.9F, 2.2F);
    public static final DeferredHolder<EntityType<?>, EntityType<RedstoneDroneEntity>> REDSTONE_DRONE = register("redstone_drone", RedstoneDroneEntity::new, 0.8F, 0.8F);
    public static final DeferredHolder<EntityType<?>, EntityType<RedstoneTowerEntity>> REDSTONE_TOWER = register("redstone_tower", RedstoneTowerEntity::new, 1.1F, 2.8F);
    public static final DeferredHolder<EntityType<?>, EntityType<RedstoneProjectileEntity>> REDSTONE_PROJECTILE = registerProjectile("redstone_projectile", RedstoneProjectileEntity::new, 0.35F, 0.35F);
    public static final DeferredHolder<EntityType<?>, EntityType<WrenchProjectileEntity>> WRENCH_PROJECTILE = registerProjectile("wrench_projectile", WrenchProjectileEntity::new, 0.35F, 0.35F);

    public static final DeferredHolder<EntityType<?>, EntityType<SentinelStatueEntity>> SENTINEL_STATUE = register("sentinel_statue", SentinelStatueEntity::new, 1.2F, 2.9F);
    public static final DeferredHolder<EntityType<?>, EntityType<SentinelGolemEntity>> SENTINEL_GOLEM = register("sentinel_golem", SentinelGolemEntity::new, 1.2F, 2.9F);

    public static final DeferredHolder<EntityType<?>, EntityType<RatEntity>> RAT = register("rat", RatEntity::new, 0.7F, 0.55F);
    public static final DeferredHolder<EntityType<?>, EntityType<MosquitoEntity>> MOSQUITO = register("mosquito", MosquitoEntity::new, 0.7F, 0.7F);
    public static final DeferredHolder<EntityType<?>, EntityType<LeechEntity>> LEECH = register("leech", LeechEntity::new, 0.8F, 0.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<RatKingEntity>> RAT_KING = register("rat_king", RatKingEntity::new, 1.3F, 2.1F);
    public static final DeferredHolder<EntityType<?>, EntityType<SewerPotionProjectileEntity>> SEWER_POTION_PROJECTILE = registerProjectile("sewer_potion_projectile", SewerPotionProjectileEntity::new, 0.4F, 0.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<SewerBoltProjectileEntity>> SEWER_BOLT_PROJECTILE = registerMisc("sewer_bolt_projectile", SewerBoltProjectileEntity::new, 0.35F, 0.35F);
    public static final DeferredHolder<EntityType<?>, EntityType<SewerPoisonAreaEntity>> SEWER_POISON_AREA = registerMisc("sewer_poison_area", SewerPoisonAreaEntity::new, 0.5F, 0.5F);

    public static final DeferredHolder<EntityType<?>, EntityType<SwarmerAntEntity>> SWARMER_ANT = register("swarmer_ant", SwarmerAntEntity::new, 1.0F, 0.8F);
    public static final DeferredHolder<EntityType<?>, EntityType<BeetleTrooperEntity>> BEETLE_TROOPER = register("beetle_trooper", BeetleTrooperEntity::new, 2.5F, 1.2F);
    public static final DeferredHolder<EntityType<?>, EntityType<GunshipMothEntity>> GUNSHIP_MOTH = register("gunship_moth", GunshipMothEntity::new, 7.5F, 8.25F);
    public static final DeferredHolder<EntityType<?>, EntityType<HuntsmanSpiderEntity>> HUNTSMAN_SPIDER = register("huntsman_spider", HuntsmanSpiderEntity::new, 2.6F, 1.8F);
    public static final DeferredHolder<EntityType<?>, EntityType<LadybugMortarEntity>> LADYBUG_MORTAR = register("ladybug_mortar", LadybugMortarEntity::new, 2.2F, 1.2F);
    public static final DeferredHolder<EntityType<?>, EntityType<WoodlouseTankEntity>> WOODLOUSE_TANK = register("woodlouse_tank", WoodlouseTankEntity::new, 3.0F, 1.7F);
    public static final DeferredHolder<EntityType<?>, EntityType<SculkCrabbyEntity>> SCULK_CRABBY = register("sculk_crabby", SculkCrabbyEntity::new, 0.9F, 1.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<SculkedCrustaceanEntity>> SCULKED_CRUSTACEAN = register("sculked_crustacean", SculkedCrustaceanEntity::new, 0.75F, 2.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<SculkScreamerEntity>> SCULK_SCREAMER = register("sculk_screamer", SculkScreamerEntity::new, 1.75F, 2.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<SculkSentinelEntity>> SCULK_SENTINEL = register("sculk_sentinel", SculkSentinelEntity::new, 1.25F, 3.6F);
    public static final DeferredHolder<EntityType<?>, EntityType<EchoEnshrouderEntity>> ECHO_ENSHROUDER = register("echo_enshrouder", EchoEnshrouderEntity::new, 0.9F, 3.6F);
    public static final DeferredHolder<EntityType<?>, EntityType<SoulBearerEntity>> SOUL_BEARER = register("soul_bearer", SoulBearerEntity::new, 0.65F, 1.75F);
    public static final DeferredHolder<EntityType<?>, EntityType<PossessedCinderArmorEntity>> POSSESSED_CINDER_ARMOR = register("possessed_cinder_armor", PossessedCinderArmorEntity::new, 0.9F, 2.25F);
    public static final DeferredHolder<EntityType<?>, EntityType<PossessedRuinArmorEntity>> POSSESSED_RUIN_ARMOR = register("possessed_ruin_armor", PossessedRuinArmorEntity::new, 0.9F, 2.25F);
    public static final DeferredHolder<EntityType<?>, EntityType<PossessedVikingArmorEntity>> POSSESSED_VIKING_ARMOR = register("possessed_viking_armor", PossessedVikingArmorEntity::new, 0.9F, 2.25F);
    public static final DeferredHolder<EntityType<?>, EntityType<ApocalypseTypeAEntity>> APOCALYPSE_TYPE_A = register("apocalypse_type_a", ApocalypseTypeAEntity::new, 1.0F, 3.6F);
    public static final DeferredHolder<EntityType<?>, EntityType<BlossomDrakeEntity>> BLOSSOM_DRAKE = register("blossom_drake", BlossomDrakeEntity::new, 2.4F, 2.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<FloralSerpentEntity>> FLORAL_SERPENT = register("floral_serpent", FloralSerpentEntity::new, 1.8F, 1.7F);
    public static final DeferredHolder<EntityType<?>, EntityType<GlimmeringUnicornEntity>> GLIMMERING_UNICORN = register("glimmering_unicorn", GlimmeringUnicornEntity::new, 1.3F, 2.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<SerenitySentinelEntity>> SERENITY_SENTINEL = register("serenity_sentinel", SerenitySentinelEntity::new, 2.8F, 6.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<QueenEverbloomEntity>> QUEEN_EVERBLOOM = register("queen_everbloom", QueenEverbloomEntity::new, 1.2F, 3.8F);
    public static final DeferredHolder<EntityType<?>, EntityType<ScarletQueenEverbloomEntity>> SCARLET_QUEEN_EVERBLOOM = register("scarlet_queen_everbloom", ScarletQueenEverbloomEntity::new, 1.2F, 3.8F);

    public static final DeferredHolder<EntityType<?>, EntityType<MobExProjectileEntity>> ABILITY_PROJECTILE = registerProjectile("ability_projectile", MobExProjectileEntity::new, 0.3F, 0.3F);
    public static final DeferredHolder<EntityType<?>, EntityType<MobExEffectEntity>> ABILITY_EFFECT = registerMisc("ability_effect", MobExEffectEntity::new, 0.5F, 0.5F);

    public static void register(IEventBus bus) {
        REGISTRY.register(bus);
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(REGULAR_SPIDER.get(), RegularSpiderEntity.createAttributes().build());
        event.put(POISON_SPIDER.get(), PoisonSpiderEntity.createAttributes().build());
        event.put(SPIDER_SPAWN.get(), SpiderSpawnerEntity.createAttributes().build());
        event.put(REDSTONE_ENGINEER.get(), RedstoneEngineerEntity.createAttributes().build());
        event.put(REDSTONE_AUTOMATON.get(), RedstoneAutomatonEntity.createAttributes().build());
        event.put(REDSTONE_DRONE.get(), RedstoneDroneEntity.createAttributes().build());
        event.put(REDSTONE_TOWER.get(), RedstoneTowerEntity.createAttributes().build());
        event.put(SENTINEL_STATUE.get(), SentinelStatueEntity.createAttributes().build());
        event.put(SENTINEL_GOLEM.get(), SentinelGolemEntity.createAttributes().build());
        event.put(RAT.get(), RatEntity.createAttributes().build());
        event.put(MOSQUITO.get(), MosquitoEntity.createAttributes().build());
        event.put(LEECH.get(), LeechEntity.createAttributes().build());
        event.put(RAT_KING.get(), RatKingEntity.createAttributes().build());
        event.put(SWARMER_ANT.get(), SwarmerAntEntity.createAttributes().build());
        event.put(BEETLE_TROOPER.get(), BeetleTrooperEntity.createAttributes().build());
        event.put(GUNSHIP_MOTH.get(), GunshipMothEntity.createAttributes().build());
        event.put(HUNTSMAN_SPIDER.get(), HuntsmanSpiderEntity.createAttributes().build());
        event.put(LADYBUG_MORTAR.get(), LadybugMortarEntity.createAttributes().build());
        event.put(WOODLOUSE_TANK.get(), WoodlouseTankEntity.createAttributes().build());
        event.put(SCULK_CRABBY.get(), SculkCrabbyEntity.createAttributes().build());
        event.put(SCULKED_CRUSTACEAN.get(), SculkedCrustaceanEntity.createAttributes().build());
        event.put(SCULK_SCREAMER.get(), SculkScreamerEntity.createAttributes().build());
        event.put(SCULK_SENTINEL.get(), SculkSentinelEntity.createAttributes().build());
        event.put(ECHO_ENSHROUDER.get(), EchoEnshrouderEntity.createAttributes().build());
        event.put(SOUL_BEARER.get(), SoulBearerEntity.createAttributes().build());
        event.put(POSSESSED_CINDER_ARMOR.get(), PossessedCinderArmorEntity.createAttributes().build());
        event.put(POSSESSED_RUIN_ARMOR.get(), PossessedRuinArmorEntity.createAttributes().build());
        event.put(POSSESSED_VIKING_ARMOR.get(), PossessedVikingArmorEntity.createAttributes().build());
        event.put(APOCALYPSE_TYPE_A.get(), ApocalypseTypeAEntity.createAttributes().build());
        event.put(BLOSSOM_DRAKE.get(), BlossomDrakeEntity.createAttributes().build());
        event.put(FLORAL_SERPENT.get(), FloralSerpentEntity.createAttributes().build());
        event.put(GLIMMERING_UNICORN.get(), GlimmeringUnicornEntity.createAttributes().build());
        event.put(SERENITY_SENTINEL.get(), SerenitySentinelEntity.createAttributes().build());
        event.put(QUEEN_EVERBLOOM.get(), QueenEverbloomEntity.createAttributes().build());
        event.put(SCARLET_QUEEN_EVERBLOOM.get(), ScarletQueenEverbloomEntity.createAttributes().build());
    }

    private static <T extends Entity & GeoAnimatable & IModel> DeferredHolder<EntityType<?>, EntityType<T>> register(String name, EntityType.EntityFactory<T> factory, float width, float height) {
        return REGISTRY.register(name, () -> EntityType.Builder.of(factory, MobCategory.MONSTER)
                .sized(width, height)
                .clientTrackingRange(10)
                .build(ResourceLocation.fromNamespaceAndPath(Core.MODID, name).toString()));
    }

    private static <T extends Entity & GeoAnimatable & IModel> DeferredHolder<EntityType<?>, EntityType<T>> registerProjectile(String name, EntityType.EntityFactory<T> factory, float width, float height) {
        return REGISTRY.register(name, () -> EntityType.Builder.of(factory, MobCategory.MISC)
                .sized(width, height)
                .clientTrackingRange(8)
                .updateInterval(1)
                .build(ResourceLocation.fromNamespaceAndPath(Core.MODID, name).toString()));
    }

    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> registerMisc(String name, EntityType.EntityFactory<T> factory, float width, float height) {
        return REGISTRY.register(name, () -> EntityType.Builder.of(factory, MobCategory.MISC)
                .sized(width, height)
                .clientTrackingRange(8)
                .updateInterval(1)
                .build(ResourceLocation.fromNamespaceAndPath(Core.MODID, name).toString()));
    }
}

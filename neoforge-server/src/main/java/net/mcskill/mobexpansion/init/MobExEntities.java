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

package net.mcskill.mobexpansion.drops;

import com.almostreliable.lootjs.LootEvents;
import com.almostreliable.lootjs.LootJS;
import com.almostreliable.lootjs.core.LootType;
import com.almostreliable.lootjs.core.entry.LootEntry;
import com.almostreliable.lootjs.core.filters.ItemFilter;
import com.almostreliable.lootjs.loot.LootConditionsContainer;
import com.almostreliable.lootjs.loot.LootModificationEvent;
import com.almostreliable.lootjs.loot.LootTableEvent;
import com.almostreliable.lootjs.loot.modifier.LootModifier;
import com.almostreliable.lootjs.loot.table.MutableLootTable;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class LootJsLootApplier {
    private LootJsLootApplier() {
    }

    public static void register() {
        LootEvents.listen(LootJsLootApplier::onLootTables);
        LootEvents.listenModifiers(LootJsLootApplier::onModifiers);
    }

    private static void onLootTables(WritableRegistry<LootTable> registry) {
        EntityLootConfigIO.reload();
        final LootTableEvent event = new LootTableEvent(registry);
        for (Map.Entry<ResourceLocation, EntityLootConfig> entry : EntityLootConfigIO.getCached().entrySet()) {
            try {
                applyEntityTable(event, entry.getKey(), entry.getValue());
            } catch (RuntimeException ignored) {
            }
        }
    }

    private static void onModifiers(Map<ResourceLocation, IGlobalLootModifier> modifiers) {
        final LootModificationEvent event = new LootModificationEvent(modifiers);
        for (Map.Entry<ResourceLocation, EntityLootConfig> entry : EntityLootConfigIO.getCached().entrySet()) {
            try {
                applyEntityExperience(event, entry.getKey(), entry.getValue());
            } catch (RuntimeException ignored) {
            }
        }
        event.storeModifiers(error -> {
        });
    }

    private static void applyEntityTable(LootTableEvent event, ResourceLocation entityId, EntityLootConfig config) {
        if (config.drops().isEmpty() && !config.replaceVanilla() && config.experience().isEmpty())
            return;
        final MutableLootTable table = entityTable(event, entityId);
        if (config.replaceVanilla())
            table.clear();
        if (config.drops().isEmpty()) {
            if (!config.replaceVanilla())
                table.createPool(pool -> pool.addEntry(LootEntry.empty()));
            return;
        }
        for (MobDropEntry dropEntry : config.drops()) {
            final LootEntry lootEntry = itemEntry(dropEntry);
            if (lootEntry == null)
                continue;
            table.createPool(pool -> pool.addEntry(lootEntry));
        }
    }

    private static void applyEntityExperience(LootModificationEvent event, ResourceLocation entityId, EntityLootConfig config) {
        if (config.experience().isEmpty())
            return;
        final Optional<Holder.Reference<EntityType<?>>> entityHolder = BuiltInRegistries.ENTITY_TYPE.getHolder(
                ResourceKey.create(Registries.ENTITY_TYPE, entityId)
        );
        if (entityHolder.isEmpty())
            return;
        final HolderSet<EntityType<?>> entities = HolderSet.direct(entityHolder.get());
        for (MobExperienceEntry experienceEntry : config.experience()) {
            final LootModifier.Builder builder = event.addEntityModifier(entities);
            applyConditions(builder, experienceEntry.chance(), experienceEntry.conditions());
            builder.dropExperience(countProvider(experienceEntry.minAmount(), experienceEntry.maxAmount()));
        }
    }

    private static MutableLootTable entityTable(LootTableEvent event, ResourceLocation entityId) {
        final ResourceLocation tableId = entityLootTableId(entityId);
        if (event.hasLootTable(tableId))
            return event.getLootTable(tableId);
        return event.create(tableId, LootType.ENTITY);
    }

    private static ResourceLocation entityLootTableId(ResourceLocation entityId) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(entityId)
                .map(EntityType::getDefaultLootTable)
                .map(lootTableKey -> lootTableKey.location())
                .filter(tableId -> !"empty".equals(tableId.getPath()))
                .orElseGet(() -> ResourceLocation.fromNamespaceAndPath(entityId.getNamespace(), "entities/" + entityId.getPath()));
    }

    @Nullable
    private static LootEntry itemEntry(MobDropEntry dropEntry) {
        final Item item = BuiltInRegistries.ITEM.getOptional(dropEntry.itemId()).orElse(Items.AIR);
        if (item == Items.AIR)
            return null;
        final LootEntry lootEntry = LootEntry.of(item, countProvider(dropEntry.minCount(), dropEntry.maxCount()));
        lootEntry.when(conditions -> applyConditions(conditions, dropEntry.chance(), dropEntry.conditions()));
        return lootEntry;
    }

    private static NumberProvider countProvider(int min, int max) {
        if (min == max)
            return ConstantValue.exactly(min);
        return UniformGenerator.between(min, max);
    }

    private static <C extends LootConditionsContainer<C>> C applyConditions(C container, float chance, List<LootConditionEntry> conditions) {
        C next = container;
        if (chance < 0.999F)
            next = next.randomChance(ConstantValue.exactly(chance));
        if (conditions == null)
            return next;
        for (LootConditionEntry condition : conditions)
            next = applyCondition(next, condition);
        return next;
    }

    private static <C extends LootConditionsContainer<C>> C applyCondition(C container, LootConditionEntry condition) {
        return switch (condition.type()) {
            case KILLED_BY_PLAYER -> container.killedByPlayer();
            case SURVIVES_EXPLOSION -> container.survivesExplosion();
            case MATCH_BIOME -> biomeCondition(container, condition.stringValue());
            case MATCH_DIMENSION -> dimensionCondition(container, condition.stringValue());
            case MATCH_STRUCTURE -> structureCondition(container, condition.stringValue(), condition.flagA() == LootConditionEntry.TRI_TRUE);
            case MATCH_WEATHER -> container.matchWeather(triBoolean(condition.flagA()), triBoolean(condition.flagB()));
            case MATCH_TIME -> container.matchTime(condition.intExtra() <= 0 ? 24000L : condition.intExtra(), condition.intMin(), condition.intMax());
            case IS_LIGHT_LEVEL -> container.isLightLevel(condition.intMin(), condition.intMax());
            case MATCH_MAIN_HAND -> container.matchMainHand(itemFilter(condition.stringValue()));
            case MATCH_OFF_HAND -> container.matchOffHand(itemFilter(condition.stringValue()));
        };
    }

    private static <C extends LootConditionsContainer<C>> C biomeCondition(C container, String biomeId) {
        final Optional<Holder.Reference<Biome>> holder = holderOf(Registries.BIOME, biomeId);
        return holder.map(value -> container.matchBiome(HolderSet.direct(value))).orElse(container);
    }

    private static <C extends LootConditionsContainer<C>> C dimensionCondition(C container, String dimensionId) {
        final ResourceLocation id = ResourceLocation.tryParse(dimensionId);
        if (id == null)
            return container;
        return container.matchDimension(id);
    }

    private static <C extends LootConditionsContainer<C>> C structureCondition(C container, String structureId, boolean exact) {
        final Optional<Holder.Reference<Structure>> holder = holderOf(Registries.STRUCTURE, structureId);
        return holder.map(value -> container.matchStructure(HolderSet.direct(value), exact)).orElse(container);
    }

    private static <T> Optional<Holder.Reference<T>> holderOf(ResourceKey<net.minecraft.core.Registry<T>> registry, String idText) {
        final ResourceLocation id = ResourceLocation.tryParse(idText);
        if (id == null)
            return Optional.empty();
        return LootJS.lookup().lookup(registry).flatMap(lookup -> lookup.get(ResourceKey.create(registry, id)));
    }

    private static ItemFilter itemFilter(String value) {
        if (value != null && value.startsWith("#"))
            return ItemFilter.tag(value);
        final ResourceLocation itemId = ResourceLocation.tryParse(value);
        final Item item = itemId == null ? Items.AIR : BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
        return ItemFilter.item(new ItemStack(item), false);
    }

    @Nullable
    private static Boolean triBoolean(byte flag) {
        if (flag == LootConditionEntry.TRI_TRUE)
            return Boolean.TRUE;
        if (flag == LootConditionEntry.TRI_FALSE)
            return Boolean.FALSE;
        return null;
    }
}

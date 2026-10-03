package net.mcskill.mobexpansion.network;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.attributes.EntityAttributeConfig;
import net.mcskill.mobexpansion.drops.LootConditionEntry;
import net.mcskill.mobexpansion.drops.LootConditionType;
import net.mcskill.mobexpansion.drops.MobDropEntry;
import net.mcskill.mobexpansion.drops.MobExperienceEntry;
import net.mcskill.mobexpansion.init.MobExCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record UpdateMobDropsPacket(List<EntityDropsData> entities) implements CustomPacketPayload {
    public static final Type<UpdateMobDropsPacket> TYPE = new Type<>(Core.loc("update_mob_drops"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConditionData> CONDITION_CODEC = MobExCodecs.composite(
            ByteBufCodecs.STRING_UTF8, ConditionData::type,
            ByteBufCodecs.STRING_UTF8, ConditionData::stringValue,
            ByteBufCodecs.VAR_INT, ConditionData::intMin,
            ByteBufCodecs.VAR_INT, ConditionData::intMax,
            ByteBufCodecs.VAR_INT, ConditionData::intExtra,
            ByteBufCodecs.BYTE, ConditionData::flagA,
            ByteBufCodecs.BYTE, ConditionData::flagB,
            ConditionData::new
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, DropData> DROP_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, DropData::itemId,
            ByteBufCodecs.VAR_INT, DropData::minCount,
            ByteBufCodecs.VAR_INT, DropData::maxCount,
            ByteBufCodecs.FLOAT, DropData::chance,
            ByteBufCodecs.collection(ArrayList::new, CONDITION_CODEC), DropData::conditions,
            DropData::new
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ExperienceData> EXPERIENCE_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ExperienceData::minAmount,
            ByteBufCodecs.VAR_INT, ExperienceData::maxAmount,
            ByteBufCodecs.FLOAT, ExperienceData::chance,
            ByteBufCodecs.collection(ArrayList::new, CONDITION_CODEC), ExperienceData::conditions,
            ExperienceData::new
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, AttributeData> ATTRIBUTE_CODEC = new StreamCodec<>() {
        @Override
        @NotNull
        public AttributeData decode(@NotNull RegistryFriendlyByteBuf buffer) {
            final double maxHealth = buffer.readDouble();
            final double armor = buffer.readDouble();
            final double armorToughness = buffer.readDouble();
            final double attackDamage = buffer.readDouble();
            final double movementSpeed = buffer.readDouble();
            final double knockbackResistance = buffer.readDouble();
            final double followRange = buffer.readDouble();
            final int extraCount = buffer.readVarInt();
            final Map<String, Double> extras = new LinkedHashMap<>();
            for (int index = 0; index < extraCount; index++)
                extras.put(buffer.readUtf(), buffer.readDouble());
            return new AttributeData(maxHealth, armor, armorToughness, attackDamage, movementSpeed, knockbackResistance, followRange, extras, buffer.readBoolean());
        }

        @Override
        public void encode(@NotNull RegistryFriendlyByteBuf buffer, @NotNull AttributeData data) {
            buffer.writeDouble(data.maxHealth());
            buffer.writeDouble(data.armor());
            buffer.writeDouble(data.armorToughness());
            buffer.writeDouble(data.attackDamage());
            buffer.writeDouble(data.movementSpeed());
            buffer.writeDouble(data.knockbackResistance());
            buffer.writeDouble(data.followRange());
            buffer.writeVarInt(data.extras().size());
            for (Map.Entry<String, Double> entry : data.extras().entrySet()) {
                buffer.writeUtf(entry.getKey());
                buffer.writeDouble(entry.getValue());
            }
            buffer.writeBoolean(data.dropToKiller());
        }
    };

    public static final StreamCodec<RegistryFriendlyByteBuf, EntityDropsData> ENTITY_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, EntityDropsData::entityTypeId,
            ByteBufCodecs.collection(ArrayList::new, DROP_CODEC), EntityDropsData::drops,
            ByteBufCodecs.collection(ArrayList::new, EXPERIENCE_CODEC), EntityDropsData::experience,
            ATTRIBUTE_CODEC, EntityDropsData::attributes,
            ByteBufCodecs.BOOL, EntityDropsData::replaceVanilla,
            EntityDropsData::new
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateMobDropsPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, ENTITY_CODEC), UpdateMobDropsPacket::entities,
            UpdateMobDropsPacket::new
    );

    public record ConditionData(String type, String stringValue, int intMin, int intMax, int intExtra, byte flagA, byte flagB) {
        public static ConditionData from(LootConditionEntry condition) {
            return new ConditionData(
                    condition.type().name(),
                    condition.stringValue(),
                    condition.intMin(),
                    condition.intMax(),
                    condition.intExtra(),
                    condition.flagA(),
                    condition.flagB()
            );
        }

        public LootConditionEntry toEntry() {
            return new LootConditionEntry(LootConditionType.fromName(this.type), this.stringValue, this.intMin, this.intMax, this.intExtra, this.flagA, this.flagB);
        }
    }

    public record DropData(String itemId, int minCount, int maxCount, float chance, List<ConditionData> conditions) {
        public static DropData from(MobDropEntry dropEntry) {
            final List<ConditionData> conditionDataList = new ArrayList<>();
            for (LootConditionEntry condition : dropEntry.conditions())
                conditionDataList.add(ConditionData.from(condition));
            return new DropData(dropEntry.itemId().toString(), dropEntry.minCount(), dropEntry.maxCount(), dropEntry.chance(), conditionDataList);
        }

        public MobDropEntry toEntry() {
            final ResourceLocation itemId = ResourceLocation.tryParse(this.itemId);
            final List<LootConditionEntry> conditionEntries = new ArrayList<>();
            for (ConditionData conditionData : this.conditions)
                conditionEntries.add(conditionData.toEntry());
            return new MobDropEntry(itemId, this.minCount, this.maxCount, this.chance, conditionEntries);
        }
    }

    public record ExperienceData(int minAmount, int maxAmount, float chance, List<ConditionData> conditions) {
        public static ExperienceData from(MobExperienceEntry experienceEntry) {
            final List<ConditionData> conditionDataList = new ArrayList<>();
            for (LootConditionEntry condition : experienceEntry.conditions())
                conditionDataList.add(ConditionData.from(condition));
            return new ExperienceData(experienceEntry.minAmount(), experienceEntry.maxAmount(), experienceEntry.chance(), conditionDataList);
        }
    }

    public record AttributeData(
            double maxHealth,
            double armor,
            double armorToughness,
            double attackDamage,
            double movementSpeed,
            double knockbackResistance,
            double followRange,
            Map<String, Double> extras,
            boolean dropToKiller
    ) {
        public AttributeData {
            extras = extras == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(extras));
        }

        public static AttributeData from(EntityAttributeConfig config) {
            return new AttributeData(
                    config.maxHealth(),
                    config.armor(),
                    config.armorToughness(),
                    config.attackDamage(),
                    config.movementSpeed(),
                    config.knockbackResistance(),
                    config.followRange(),
                    config.extras(),
                    config.dropDestination().givesToKiller()
            );
        }
    }

    public record EntityDropsData(String entityTypeId, List<DropData> drops, List<ExperienceData> experience, AttributeData attributes, boolean replaceVanilla) {
    }

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateMobDropsPacket packet, IPayloadContext context) {
    }
}

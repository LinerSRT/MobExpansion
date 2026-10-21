package net.mcskill.mobexpansion.init;

import com.mojang.serialization.Codec;
import net.mcskill.mobexpansion.Core;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class MobExAttachments {
    private static final DeferredRegister<AttachmentType<?>> REGISTRY = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Core.MODID);

    /**
     * Уровень допуска игрока. Хранится в NBT игрока (neoforge:attachments), переживает смерть и перезаход.
     * Читать/менять только через {@link net.mcskill.mobexpansion.clearance.Clearance}.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> CLEARANCE_LEVEL = REGISTRY.register("clearance_level",
            () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT, level -> level != 0)
                    .copyOnDeath()
                    .build());

    /**
     * Скин игрока (id из {@code PlayerSkins}, пустая строка — обычный вид). Хранится в NBT игрока, переживает смерть.
     * Клиентам рассылается пакетом {@code PlayerSkinPacket}: на клиенте атачмент не синхронизируется.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<String>> PLAYER_SKIN = REGISTRY.register("player_skin",
            () -> AttachmentType.builder(() -> "")
                    .serialize(Codec.STRING, skin -> !skin.isEmpty())
                    .copyOnDeath()
                    .build());

    private MobExAttachments() {
    }

    public static void register(IEventBus bus) {
        REGISTRY.register(bus);
    }
}

package net.mcskill.mobexpansion.client;

import net.mcskill.mobexpansion.Core;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

/**
 * Фирменный босс-бар Apocalypse Type A (из ресурспака Apocalypse_Bossbar): пурпурная рамка с головой босса
 * в центре вместо ванильной полоски и имени. Внутри рамки — заполнение по здоровью.
 */
@EventBusSubscriber(modid = Core.MODID, value = Dist.CLIENT)
public final class ApocalypseBossBarRenderer {
    private static final String NAME_KEY = "entity.mobexpansion.apocalypse_type_a";
    private static final ResourceLocation TEXTURE = Core.loc("textures/gui/apocalypse_bossbar.png");

    // рамка в текстуре 256x256: x 38..224, y 0..44
    private static final int FRAME_U = 38;
    private static final int FRAME_WIDTH = 187;
    private static final int FRAME_HEIGHT = 45;
    // прозрачное окно под полоску внутри рамки
    private static final int SLOT_X = 4;
    private static final int SLOT_Y = 19;
    private static final int SLOT_WIDTH = 179;
    private static final int SLOT_HEIGHT = 5;
    // vanilla рисует первую полоску на y=12, а у нашей рамки окно на 19-м пикселе
    private static final int FRAME_OFFSET_Y = 12;

    private ApocalypseBossBarRenderer() {
    }

    private static boolean isApocalypse(LerpingBossEvent event) {
        return event.getName().getContents() instanceof TranslatableContents contents && NAME_KEY.equals(contents.getKey());
    }

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        final LerpingBossEvent boss = event.getBossEvent();
        if (!isApocalypse(boss))
            return;
        event.setCanceled(true);

        final GuiGraphics graphics = event.getGuiGraphics();
        final int left = graphics.guiWidth() / 2 - FRAME_WIDTH / 2;
        final int top = Math.max(0, event.getY() - FRAME_OFFSET_Y);
        final int slotLeft = left + SLOT_X;
        final int slotTop = top + SLOT_Y;
        final int filled = Math.round(SLOT_WIDTH * Math.max(0.0F, Math.min(1.0F, boss.getProgress())));

        graphics.fill(slotLeft, slotTop, slotLeft + SLOT_WIDTH, slotTop + SLOT_HEIGHT, 0xFF0A0614);
        if (filled > 0) {
            graphics.fill(slotLeft, slotTop, slotLeft + filled, slotTop + SLOT_HEIGHT, 0xFF9B2BD6);
            graphics.fill(slotLeft, slotTop, slotLeft + filled, slotTop + 1, 0xFFD738FA);
            graphics.fill(slotLeft, slotTop + SLOT_HEIGHT - 1, slotLeft + filled, slotTop + SLOT_HEIGHT, 0xFF5A1488);
        }
        graphics.blit(TEXTURE, left, top, FRAME_U, 0, FRAME_WIDTH, FRAME_HEIGHT, 256, 256);
        event.setIncrement(FRAME_HEIGHT + 4);
    }
}

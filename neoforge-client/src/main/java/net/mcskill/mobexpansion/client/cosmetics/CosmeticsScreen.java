package net.mcskill.mobexpansion.client.cosmetics;

import com.mojang.blaze3d.systems.RenderSystem;
import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.client.screen.GuiFitScale;
import net.mcskill.mobexpansion.client.screen.oreui.OreUIAtlas;
import net.mcskill.mobexpansion.client.screen.oreui.OreUIPanel;
import net.mcskill.mobexpansion.client.skin.ClientSkins;
import net.mcskill.mobexpansion.network.CosmeticsSelectPacket;
import net.mcskill.mobexpansion.network.CosmeticsStatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI кастомизации персонажа в стиле Bedrock (OreUI, как конфигуратор мобов): слева превью игрока со скином и
 * шлейфом на фоне, справа строки карточек — скины и шлейфы. Закрытые карточки — с замком; превью показывает
 * карточку под курсором, даже закрытую. Клик по открытой карточке — надеть.
 * <p>
 * Новая строка (скины предметов, питомцы): категория в {@link CosmeticsSelectPacket.Category}, список в
 * {@link CosmeticsStatePacket}, {@link Row} здесь и отрисовка иконки в {@link #renderIcon}.
 */
public class CosmeticsScreen extends Screen {
    private static final int GUI_W = 452;
    private static final int GUI_H = 256;

    private static final int PREVIEW_X = 10;
    private static final int PREVIEW_Y = 28;
    private static final int PREVIEW_W = 140;
    private static final int PREVIEW_H = 218;
    private static final int STAGE_INSET = 5;
    private static final int STAGE_H = 168;

    private static final int ROWS_X = 160;
    private static final int ROWS_W = 282;
    private static final int ROW_H = 106;
    private static final int ROW_GAP = 6;
    private static final int ARROW_W = 16;
    private static final int CARD_W = 56;
    private static final int CARD_H = 80;
    private static final int CARD_GAP = 6;
    private static final int VISIBLE_CARDS = 4;

    private static final int COLOR_TEXT = 0xFFFEFEFE;
    private static final int COLOR_MUTED = 0xFFAAAAAA;
    private static final int COLOR_ACCENT = 0xFF8DF1FF;
    private static final int COLOR_GOOD = 0xFF7EE07E;
    private static final int COLOR_LOCKED = 0xFFFF8A80;

    private static final ResourceLocation ARROW_LEFT = Core.loc("textures/gui/arrows/left.png");
    private static final ResourceLocation ARROW_LEFT_HOVER = Core.loc("textures/gui/arrows/left_hovered.png");
    private static final ResourceLocation ARROW_RIGHT = Core.loc("textures/gui/arrows/right.png");
    private static final ResourceLocation ARROW_RIGHT_HOVER = Core.loc("textures/gui/arrows/right_hovered.png");

    private final List<Row> rows = new ArrayList<>();
    private final Map<ResourceLocation, Boolean> textureExists = new HashMap<>();
    private int left;
    /** Сдвиг строк вверх в пикселях: в окно влезают две строки, остальные — за прокруткой. */
    private int rowsScroll;
    private int top;
    @Nullable
    private CosmeticsStatePacket state;

    public CosmeticsScreen() {
        super(Component.translatable("gui.mobexpansion.cosmetics.title"));
        rows.add(new Row(CosmeticsSelectPacket.Category.SKIN, "gui.mobexpansion.cosmetics.skins", "gui.mobexpansion.cosmetics.no_skin"));
        rows.add(new Row(CosmeticsSelectPacket.Category.TRAIL, "gui.mobexpansion.cosmetics.trails", "gui.mobexpansion.cosmetics.no_trail"));
        rows.add(new Row(CosmeticsSelectPacket.Category.PREFIX, "gui.mobexpansion.cosmetics.prefixes", "gui.mobexpansion.cosmetics.no_prefix"));
        state = CosmeticsClient.state();
        if (state != null)
            onState(state);
    }

    void onState(CosmeticsStatePacket packet) {
        state = packet;
        rows.get(0).set(packet.skins(), packet.skin(), true);
        rows.get(1).set(packet.trails(), packet.trail(), packet.trailsAvailable());
        rows.get(2).set(packet.prefixes(), packet.prefix(), packet.prefixesAvailable());
    }

    @Override
    protected void init() {
        left = (width - GUI_W) / 2;
        top = (height - GUI_H) / 2;
        GuiFitScale.update(width, height, GUI_W, GUI_H);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------- отрисовка ----------

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int rawMouseX, int rawMouseY, float partialTick) {
        renderBackground(guiGraphics, rawMouseX, rawMouseY, partialTick);
        final int mouseX = GuiFitScale.unscaleMouseX(rawMouseX);
        final int mouseY = GuiFitScale.unscaleMouseY(rawMouseY);
        GuiFitScale.push(guiGraphics, width, height, GUI_W, GUI_H);
        try {
            OreUIPanel.renderDark(guiGraphics, left, top, GUI_W, GUI_H);
            guiGraphics.drawString(font, title, left + 10, top + 10, COLOR_TEXT, false);
            final Hover hover = hover(mouseX, mouseY);
            renderPreview(guiGraphics, mouseX, mouseY, hover, partialTick);
            guiGraphics.enableScissor(left + ROWS_X, top + PREVIEW_Y, left + ROWS_X + ROWS_W, top + PREVIEW_Y + PREVIEW_H);
            for (int index = 0; index < rows.size(); index++) {
                final int y = rowY(index);
                if (y + ROW_H > top + PREVIEW_Y && y < top + PREVIEW_Y + PREVIEW_H)
                    renderRow(guiGraphics, rows.get(index), y, mouseX, mouseY, hover);
            }
            guiGraphics.disableScissor();
            renderScrollbar(guiGraphics);
            if (hover != null && hover.entry != null && !hover.entry.unlocked())
                guiGraphics.renderTooltip(font, List.of(Component.translatable("gui.mobexpansion.cosmetics.locked_tooltip")),
                        java.util.Optional.empty(), mouseX, mouseY);
        } finally {
            GuiFitScale.pop(guiGraphics);
        }
    }

    private int rowY(int index) {
        return top + PREVIEW_Y + index * (ROW_H + ROW_GAP) - rowsScroll;
    }

    private int maxRowsScroll() {
        return Math.max(0, rows.size() * (ROW_H + ROW_GAP) - ROW_GAP - PREVIEW_H);
    }

    /** Мышь над областью строк (всё, что вне неё, обрезано прокруткой). */
    private boolean inRowsViewport(int mouseX, int mouseY) {
        return isOver(mouseX, mouseY, left + ROWS_X, top + PREVIEW_Y, ROWS_W, PREVIEW_H);
    }

    private void renderScrollbar(GuiGraphics guiGraphics) {
        final int max = maxRowsScroll();
        if (max <= 0)
            return;
        final int x = left + ROWS_X + ROWS_W + 3, y = top + PREVIEW_Y;
        final int thumb = Math.max(16, PREVIEW_H * PREVIEW_H / (PREVIEW_H + max));
        final int thumbY = y + (PREVIEW_H - thumb) * rowsScroll / max;
        guiGraphics.fill(x, y, x + 4, y + PREVIEW_H, 0x66000000);
        guiGraphics.fill(x, thumbY, x + 4, thumbY + thumb, 0xFFAAAAAA);
    }

    private void renderPreview(GuiGraphics guiGraphics, int mouseX, int mouseY, @Nullable Hover hover, float partialTick) {
        final int x = left + PREVIEW_X;
        final int y = top + PREVIEW_Y;
        OreUIPanel.renderLight(guiGraphics, x, y, PREVIEW_W, PREVIEW_H);
        final int sx = x + STAGE_INSET, sy = y + STAGE_INSET, sw = PREVIEW_W - STAGE_INSET * 2;
        OreUIPanel.renderDark(guiGraphics, sx, sy, sw, STAGE_H);

        final LocalPlayer player = Minecraft.getInstance().player;
        if (player == null)
            return;
        // что показать: карточка под курсором, иначе надетое
        String skin = rows.get(0).current;
        String trail = rows.get(1).current;
        Row focusRow = null;
        CosmeticsStatePacket.Entry focus = null;
        if (hover != null && hover.row != null) {
            focusRow = hover.row;
            focus = hover.entry;
            final String id = focus == null ? "" : focus.id();
            // префикс на превью игрока не виден — меняется только подпись
            if (hover.row.category == CosmeticsSelectPacket.Category.SKIN)
                skin = id;
            else if (hover.row.category == CosmeticsSelectPacket.Category.TRAIL)
                trail = id;
        }

        guiGraphics.enableScissor(sx + 2, sy + 2, sx + sw - 2, sy + STAGE_H - 2);
        if (!trail.isEmpty())
            renderTrailRibbon(guiGraphics, trailTexture(trail), sx, sy, sw, STAGE_H, partialTick);
        guiGraphics.disableScissor();
        final String shownSkin = skin;
        ClientSkins.renderWithSkin(player, shownSkin, () -> InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics,
                sx + 2, sy + 2, sx + sw - 2, sy + STAGE_H - 2, 52, 0.12F, mouseX, mouseY, player));

        // подпись: что в превью и можно ли надеть
        final int textY = sy + STAGE_H + 6;
        if (focusRow == null) {
            guiGraphics.drawCenteredString(font, Component.translatable("gui.mobexpansion.cosmetics.preview_hint"), x + PREVIEW_W / 2, textY, COLOR_MUTED);
            guiGraphics.drawCenteredString(font, Component.translatable("gui.mobexpansion.cosmetics.key_hint",
                    CosmeticsClient.OPEN_KEY.getTranslatedKeyMessage()), x + PREVIEW_W / 2, textY + 12, COLOR_MUTED);
            return;
        }
        guiGraphics.drawCenteredString(font, focusRow.name(focus), x + PREVIEW_W / 2, textY, COLOR_TEXT);
        final Component status;
        final int color;
        if (focusRow.isCurrent(focus)) {
            status = Component.translatable("gui.mobexpansion.cosmetics.equipped");
            color = COLOR_GOOD;
        } else if (focus != null && !focus.unlocked()) {
            status = Component.translatable("gui.mobexpansion.cosmetics.locked");
            color = COLOR_LOCKED;
        } else {
            status = Component.translatable("gui.mobexpansion.cosmetics.click_to_equip");
            color = COLOR_ACCENT;
        }
        guiGraphics.drawCenteredString(font, status, x + PREVIEW_W / 2, textY + 12, color);
    }

    /** Шлейф в превью: волнистая лента с текстурой шлейфа, бегущая за игроком. */
    private void renderTrailRibbon(GuiGraphics guiGraphics, @Nullable ResourceLocation texture, int x, int y, int w, int h, float partialTick) {
        if (texture == null)
            return;
        final float time = (Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0) + partialTick;
        final int slice = 3;
        final int band = 30;
        final int length = w + 20;
        final int uWidth = Math.max(1, 1024 * slice / length);
        RenderSystem.enableBlend();
        for (int i = 0; i < length; i += slice) {
            final float t = i / (float) length;
            // от низа слева к центру за спиной игрока, лёгкая волна
            final float cy = y + h * (0.86F - 0.5F * t) + Mth.sin(time * 0.15F + i * 0.06F) * 6.0F;
            final float alpha = Mth.clamp(t * 3.0F, 0.0F, 1.0F) * Mth.clamp((1.0F - t) * 4.0F, 0.0F, 1.0F) * 0.9F;
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);
            final float u = ((i * 1024.0F / length) - time * 18.0F) % 1024.0F;
            guiGraphics.blit(texture, x + i - 10, Math.round(cy - band / 2.0F), slice, band, u < 0 ? u + 1024 : u, 0.0F, uWidth, 1024, 1024, 1024);
        }
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private void renderRow(GuiGraphics guiGraphics, Row row, int y, int mouseX, int mouseY, @Nullable Hover hover) {
        final int x = left + ROWS_X;
        OreUIPanel.renderLight(guiGraphics, x, y, ROWS_W, ROW_H);
        guiGraphics.drawString(font, row.title, x + 8, y + 7, COLOR_TEXT, false);
        if (row.loading) {
            guiGraphics.drawCenteredString(font, Component.translatable("gui.mobexpansion.cosmetics.loading"), x + ROWS_W / 2, y + ROW_H / 2, COLOR_MUTED);
            return;
        }
        if (!row.available) {
            guiGraphics.drawCenteredString(font, Component.translatable("gui.mobexpansion.cosmetics.unavailable"), x + ROWS_W / 2, y + ROW_H / 2, COLOR_MUTED);
            return;
        }
        final int cardsY = y + 20;
        // стрелки
        final boolean canLeft = row.scroll > 0;
        final boolean canRight = row.scroll + VISIBLE_CARDS < row.size();
        final int arrowY = cardsY + CARD_H / 2 - 8;
        if (canLeft)
            guiGraphics.blit(isOver(mouseX, mouseY, x + 2, arrowY, ARROW_W, 16) ? ARROW_LEFT_HOVER : ARROW_LEFT, x + 2, arrowY, ARROW_W, 16, 0.0F, 0.0F, 32, 32, 32, 32);
        if (canRight)
            guiGraphics.blit(isOver(mouseX, mouseY, x + ROWS_W - 2 - ARROW_W, arrowY, ARROW_W, 16) ? ARROW_RIGHT_HOVER : ARROW_RIGHT,
                    x + ROWS_W - 2 - ARROW_W, arrowY, ARROW_W, 16, 0.0F, 0.0F, 32, 32, 32, 32);
        for (int slot = 0; slot < VISIBLE_CARDS; slot++) {
            final int index = row.scroll + slot;
            if (index >= row.size())
                break;
            final int cx = cardX(slot);
            final CosmeticsStatePacket.Entry entry = row.entry(index);
            final boolean hovered = hover != null && hover.row == row && hover.index == index;
            renderCard(guiGraphics, row, entry, cx, cardsY, hovered);
        }
    }

    private int cardX(int slot) {
        final int cardsW = VISIBLE_CARDS * CARD_W + (VISIBLE_CARDS - 1) * CARD_GAP;
        return left + ROWS_X + (ROWS_W - cardsW) / 2 + slot * (CARD_W + CARD_GAP);
    }

    private void renderCard(GuiGraphics guiGraphics, Row row, @Nullable CosmeticsStatePacket.Entry entry, int x, int y, boolean hovered) {
        final boolean current = row.isCurrent(entry);
        final boolean locked = entry != null && !entry.unlocked();
        final OreUIAtlas.ButtonStyle style = current ? OreUIAtlas.ButtonStyle.PRIMARY : OreUIAtlas.ButtonStyle.TERTIARY;
        OreUIAtlas.blitNineSlice(guiGraphics, x, y, CARD_W, CARD_H,
                OreUIAtlas.buttonU(style, hovered && !locked, false, true), style.v, OreUIAtlas.BUTTON_SRC, style.sourceHeight, OreUIAtlas.BUTTON_SLICE);
        // окно иконки
        final int ix = x + 4, iy = y + 4, iw = CARD_W - 8, ih = CARD_H - 26;
        guiGraphics.fill(ix, iy, ix + iw, iy + ih, 0xFF1A1A1C);
        renderIcon(guiGraphics, row, entry, ix, iy, iw, ih);
        if (locked) {
            // фигурка скина рисуется со сдвигом по Z — затемнение и замок поднимаем выше неё
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0F, 0.0F, 200.0F);
            guiGraphics.fill(ix, iy, ix + iw, iy + ih, 0x99000000);
            renderLock(guiGraphics, ix + iw / 2, iy + ih / 2);
            guiGraphics.pose().popPose();
        }
        // название — в две строки максимум, мелко
        final Component name = row.name(entry);
        final List<net.minecraft.util.FormattedCharSequence> lines = font.split(name, CARD_W - 6);
        final int textColor = locked ? COLOR_MUTED : COLOR_TEXT;
        for (int line = 0; line < Math.min(2, lines.size()); line++)
            guiGraphics.drawString(font, lines.get(line), x + (CARD_W - font.width(lines.get(line))) / 2,
                    y + CARD_H - 20 + line * 9, textColor, false);
    }

    /** Иконка карточки: скин — мини-фигурка игрока в этом скине, шлейф — его текстура. */
    private void renderIcon(GuiGraphics guiGraphics, Row row, @Nullable CosmeticsStatePacket.Entry entry, int x, int y, int w, int h) {
        final LocalPlayer player = Minecraft.getInstance().player;
        switch (row.category) {
            case SKIN -> {
                if (player == null)
                    return;
                final String skin = entry == null ? "" : entry.id();
                ClientSkins.renderWithSkin(player, skin, () -> InventoryScreen.renderEntityInInventoryFollowsAngle(guiGraphics,
                        x, y, x + w, y + h, 22, 0.1F, 0.6F, -0.1F, player));
            }
            case TRAIL -> {
                if (entry == null) {
                    renderNone(guiGraphics, x + w / 2, y + h / 2);
                    return;
                }
                final ResourceLocation texture = trailTexture(entry.id());
                if (texture == null) {
                    guiGraphics.drawCenteredString(font, "?", x + w / 2, y + h / 2 - 4, COLOR_MUTED);
                    return;
                }
                RenderSystem.enableBlend();
                guiGraphics.blit(texture, x + 2, y + 2, w - 4, h - 4, 0.0F, 0.0F, 1024, 1024, 1024, 1024);
                RenderSystem.disableBlend();
            }
            case PREFIX -> {
                if (entry == null) {
                    renderNone(guiGraphics, x + w / 2, y + h / 2);
                    return;
                }
                final ResourceLocation texture = prefixTexture(entry.id());
                if (texture == null) {
                    // иконки нет — показываем название префикса
                    final List<net.minecraft.util.FormattedCharSequence> lines = font.split(row.name(entry), w - 4);
                    final int count = Math.min(3, lines.size());
                    for (int line = 0; line < count; line++)
                        guiGraphics.drawString(font, lines.get(line), x + (w - font.width(lines.get(line))) / 2,
                                y + h / 2 - 4 + (line - (count - 1) / 2) * 9, COLOR_ACCENT, false);
                    return;
                }
                RenderSystem.enableBlend();
                guiGraphics.blit(texture, x + (w - 32) / 2, y + (h - 32) / 2, 32, 32, 0.0F, 0.0F, 128, 128, 128, 128);
                RenderSystem.disableBlend();
            }
        }
    }

    /** Значок «ничего»: перечёркнутый круг. */
    private static void renderNone(GuiGraphics guiGraphics, int cx, int cy) {
        final int color = 0xFF8A8A8A;
        for (int a = 0; a < 360; a += 6) {
            final int px = cx + Math.round(Mth.cos(a * Mth.DEG_TO_RAD) * 10);
            final int py = cy + Math.round(Mth.sin(a * Mth.DEG_TO_RAD) * 10);
            guiGraphics.fill(px, py, px + 2, py + 2, color);
        }
        for (int i = -7; i <= 7; i++)
            guiGraphics.fill(cx + i, cy - i, cx + i + 2, cy - i + 2, color);
    }

    /** Пиксельный замок по центру. */
    private static void renderLock(GuiGraphics guiGraphics, int cx, int cy) {
        final int body = 0xFFE8B54A, dark = 0xFF8C5A1A, shackle = 0xFFC9C9C9;
        // дужка
        guiGraphics.fill(cx - 5, cy - 9, cx + 5, cy - 7, shackle);
        guiGraphics.fill(cx - 5, cy - 9, cx - 3, cy - 2, shackle);
        guiGraphics.fill(cx + 3, cy - 9, cx + 5, cy - 2, shackle);
        // корпус
        guiGraphics.fill(cx - 7, cy - 2, cx + 7, cy + 9, dark);
        guiGraphics.fill(cx - 6, cy - 1, cx + 6, cy + 8, body);
        // скважина
        guiGraphics.fill(cx - 1, cy + 1, cx + 1, cy + 6, dark);
    }

    @Nullable
    private ResourceLocation trailTexture(String id) {
        final ResourceLocation texture = ResourceLocation.tryParse("msess:trails/" + id + ".png");
        if (texture == null)
            return null;
        final boolean exists = textureExists.computeIfAbsent(texture,
                location -> Minecraft.getInstance().getResourceManager().getResource(location).isPresent());
        return exists ? texture : null;
    }

    /** Иконка префикса: {@code textures/gui/prefix/<id>.png} в мод-ассетах, 128x128; нет файла — null. */
    @Nullable
    private ResourceLocation prefixTexture(String id) {
        final ResourceLocation texture = Core.loc("textures/gui/prefix/" + id + ".png");
        final boolean exists = textureExists.computeIfAbsent(texture,
                location -> Minecraft.getInstance().getResourceManager().getResource(location).isPresent());
        return exists ? texture : null;
    }

    // ---------- ввод ----------

    /** Что под курсором: карточка (row + index, entry == null — карточка «без») или стрелка. */
    private record Hover(@Nullable Row row, int index, @Nullable CosmeticsStatePacket.Entry entry, int arrow) {
    }

    @Nullable
    private Hover hover(int mouseX, int mouseY) {
        if (!inRowsViewport(mouseX, mouseY))
            return null;
        for (int r = 0; r < rows.size(); r++) {
            final Row row = rows.get(r);
            if (row.loading || !row.available)
                continue;
            final int y = rowY(r);
            final int cardsY = y + 20;
            final int arrowY = cardsY + CARD_H / 2 - 8;
            final int x = left + ROWS_X;
            if (row.scroll > 0 && isOver(mouseX, mouseY, x + 2, arrowY, ARROW_W, 16))
                return new Hover(row, -1, null, -1);
            if (row.scroll + VISIBLE_CARDS < row.size() && isOver(mouseX, mouseY, x + ROWS_W - 2 - ARROW_W, arrowY, ARROW_W, 16))
                return new Hover(row, -1, null, 1);
            for (int slot = 0; slot < VISIBLE_CARDS; slot++) {
                final int index = row.scroll + slot;
                if (index >= row.size())
                    break;
                if (isOver(mouseX, mouseY, cardX(slot), cardsY, CARD_W, CARD_H))
                    return new Hover(row, index, row.entry(index), 0);
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double rawMouseX, double rawMouseY, int button) {
        if (button != 0)
            return super.mouseClicked(rawMouseX, rawMouseY, button);
        final Hover hover = hover(GuiFitScale.unscaleMouseX(rawMouseX), GuiFitScale.unscaleMouseY(rawMouseY));
        if (hover == null || hover.row == null)
            return super.mouseClicked(rawMouseX, rawMouseY, button);
        if (hover.arrow != 0) {
            hover.row.scrollBy(hover.arrow);
            click(1.0F);
            return true;
        }
        if (hover.entry != null && !hover.entry.unlocked()) {
            click(0.6F);
            return true;
        }
        if (!hover.row.isCurrent(hover.entry)) {
            final String id = hover.entry == null ? "" : hover.entry.id();
            CosmeticsClient.select(hover.row.category, id);
            hover.row.current = id; // сразу показать выбор; сервер пришлёт подтверждение
            click(1.3F);
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double rawMouseX, double rawMouseY, double scrollX, double scrollY) {
        final int mouseX = GuiFitScale.unscaleMouseX(rawMouseX), mouseY = GuiFitScale.unscaleMouseY(rawMouseY);
        if (!inRowsViewport(mouseX, mouseY))
            return super.mouseScrolled(rawMouseX, rawMouseY, scrollX, scrollY);
        // Shift — карточки внутри строки, обычное колесо — строки вверх/вниз
        if (hasShiftDown() || scrollX != 0) {
            for (int r = 0; r < rows.size(); r++) {
                if (isOver(mouseX, mouseY, left + ROWS_X, rowY(r), ROWS_W, ROW_H)) {
                    rows.get(r).scrollBy((scrollY != 0 ? scrollY : scrollX) > 0 ? -1 : 1);
                    return true;
                }
            }
            return true;
        }
        rowsScroll = Mth.clamp(rowsScroll - (int) Math.signum(scrollY) * 28, 0, maxRowsScroll());
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (CosmeticsClient.OPEN_KEY.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static boolean isOver(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private static void click(float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }

    // ---------- строка карточек ----------

    /** Строка GUI: первая карточка — «без» (entry == null), дальше записи с сервера. */
    private static final class Row {
        final CosmeticsSelectPacket.Category category;
        final Component title;
        final Component noneName;
        List<CosmeticsStatePacket.Entry> entries = List.of();
        String current = "";
        boolean available = true;
        boolean loading = true;
        int scroll;

        Row(CosmeticsSelectPacket.Category category, String titleKey, String noneKey) {
            this.category = category;
            this.title = Component.translatable(titleKey);
            this.noneName = Component.translatable(noneKey);
        }

        void set(List<CosmeticsStatePacket.Entry> entries, String current, boolean available) {
            // открытые — первыми, порядок внутри групп как пришёл
            final List<CosmeticsStatePacket.Entry> sorted = new ArrayList<>(entries);
            sorted.sort((a, b) -> Boolean.compare(b.unlocked(), a.unlocked()));
            this.entries = sorted;
            this.current = current;
            this.available = available;
            this.loading = false;
            this.scroll = Mth.clamp(this.scroll, 0, Math.max(0, size() - VISIBLE_CARDS));
        }

        int size() {
            return entries.size() + 1;
        }

        @Nullable
        CosmeticsStatePacket.Entry entry(int index) {
            return index == 0 ? null : entries.get(index - 1);
        }

        boolean isCurrent(@Nullable CosmeticsStatePacket.Entry entry) {
            return entry == null ? current.isEmpty() : entry.id().equals(current);
        }

        void scrollBy(int delta) {
            scroll = Mth.clamp(scroll + delta, 0, Math.max(0, size() - VISIBLE_CARDS));
        }

        Component name(@Nullable CosmeticsStatePacket.Entry entry) {
            if (entry == null)
                return noneName;
            final String key = "cosmetics.mobexpansion." + category.name().toLowerCase(java.util.Locale.ROOT) + "." + entry.id();
            return Component.translatableWithFallback(key, entry.id());
        }
    }
}

package dev.visualutils;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

/**
 * Menu NullClient (klawisz . ): cztery kolumny z modulami (Swiat, Widok, HUD, PVP), wyszukiwarka,
 * zakladki Opcje i Wyglad, przelaczniki, wybor motywu. Klikniecia czytamy z GLFW w render(),
 * wiec nie zaleza od zmian w obsludze zdarzen w nowszych wersjach gry.
 */
public class MenuScreen extends Screen {

    private static final Module.Category[] COLS = {
            Module.Category.SWIAT, Module.Category.WIDOK, Module.Category.HUD, Module.Category.PVP};
    private static final String[] TAB_NAMES = {"Moduły", "Opcje", "Wygląd"};

    private static final int GAP = 8;
    private static final int HEAD = 40;
    private static final int TABS = 28;
    private static final int COLHEAD = 20;
    private static final int FOOT = 42;

    private int tab = 0; // 0 = moduly, 1 = opcje, 2 = wyglad
    private EditBox search;
    private final EnumMap<Module, Float> anim = new EnumMap<>(Module.class);
    private boolean mouseWasDown = true; // zeby klik otwierajacy menu nie przelaczyl modulu
    private boolean rightWasDown = true;
    private boolean rightClick = false;
    private long lastNanos = System.nanoTime();

    public MenuScreen() {
        super(Component.literal("NullClient"));
        for (Module m : Module.values()) {
            anim.put(m, Config.isOn(m) ? 1f : 0f);
        }
    }

    @Override
    protected void init() {
        search = new EditBox(this.font, 0, 0, 130, 16, Component.literal("Szukaj"));
        search.setHint(Component.literal("Szukaj modułów..."));
        search.setMaxLength(24);
        this.addWidget(search);
    }

    private static List<Module> modulesOf(Module.Category c) {
        List<Module> list = new ArrayList<>();
        for (Module m : Module.values()) {
            if (m.category() == c) list.add(m);
        }
        return list;
    }

    private static List<Setting> settingsOf(Module.Category c) {
        List<Setting> list = new ArrayList<>();
        for (Setting s : Setting.values()) {
            if (s.tab() == c) list.add(s);
        }
        return list;
    }

    private static int maxModuleRows() {
        int max = 1;
        for (Module.Category c : COLS) {
            max = Math.max(max, modulesOf(c).size());
        }
        return max;
    }

    private static int maxSettingRows() {
        return Math.max(settingsOf(Module.Category.OPCJE).size(), settingsOf(Module.Category.WYGLAD).size());
    }

    private boolean matches(Module m, String q) {
        if (q.isEmpty()) return true;
        return m.label().toLowerCase(Locale.ROOT).contains(q) || m.description().toLowerCase(Locale.ROOT).contains(q);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;

        // stan przyciskow myszy czytamy bezposrednio z GLFW
        long window = GLFW.glfwGetCurrentContext();
        boolean down = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (!down) {
            down = Minecraft.getInstance().mouseHandler.isLeftPressed();
        }
        boolean click = down && !mouseWasDown;
        mouseWasDown = down;
        boolean rdown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        rightClick = rdown && !rightWasDown;
        rightWasDown = rdown;

        int accent = Theme.accent();

        // wymiary panelu
        int pw = Math.min(this.width - 16, 560);
        int colW = (pw - GAP * (COLS.length + 1)) / COLS.length;
        int modRows = maxModuleRows();
        int setRows = maxSettingRows();
        int availBody = this.height - 16 - HEAD - TABS - FOOT;
        int modRowH = Math.max(18, Math.min(24, (availBody - COLHEAD) / modRows));
        int setRowH = Math.max(22, Math.min(28, availBody / setRows));
        int body = Math.max(COLHEAD + modRows * modRowH, setRows * setRowH);
        int ph = HEAD + TABS + body + FOOT;
        int px = (this.width - pw) / 2;
        int py = (this.height - ph) / 2;

        // cien, poswiata w kolorze motywu i tlo
        Draw.roundRect(g, px - 3, py - 1, pw + 6, ph + 6, 10, 0x40000000);
        Draw.roundRect(g, px - 1, py - 1, pw + 2, ph + 2, 8, Theme.withAlpha(accent, 0x80));
        Draw.roundRect(g, px, py, pw, ph, 7, 0xF2101018);
        for (int i = 0; i < 22; i++) {
            int a = Math.max(0, 0x38 - i * 0x38 / 22);
            g.fill(px + 6, py + 1 + i, px + pw - 6, py + 2 + i, Theme.withAlpha(accent, a));
        }

        // naglowek
        Component namePart1 = Component.literal("Null").withStyle(ChatFormatting.BOLD);
        Component namePart2 = Component.literal("Client").withStyle(ChatFormatting.BOLD);
        g.drawString(this.font, namePart1, px + 14, py + 13, 0xFFFFFFFF, false);
        g.drawString(this.font, namePart2, px + 14 + this.font.width(namePart1), py + 13, accent, false);
        int enabled = 0;
        for (Module m : Module.values()) {
            if (Config.isOn(m)) enabled++;
        }
        String countText = enabled + "/" + Module.values().length + " włączone";
        g.drawString(this.font, countText, px + 14 + this.font.width(namePart1) + this.font.width(namePart2) + 10,
                py + 13, 0xFF8A8A9A, false);

        // wyszukiwarka (wlasne tlo, zeby pasowala do stylu)
        int sw = 130;
        int sx = px + pw - 12 - sw;
        int sy = py + 9;
        Draw.roundRect(g, sx - 1, sy - 1, sw + 2, 18, 6, Theme.withAlpha(accent, 0x60));
        Draw.roundRect(g, sx, sy, sw, 16, 5, 0xFF1A1A24);
        search.setPosition(sx + 4, sy);
        search.setWidth(sw - 8);
        search.setBordered(false);
        search.render(g, mouseX, mouseY, partialTick);
        String query = search.getValue().trim().toLowerCase(Locale.ROOT);

        // zakladki
        int tabY = py + HEAD - 2;
        int tx = px + 12;
        for (int i = 0; i < TAB_NAMES.length; i++) {
            int w = this.font.width(TAB_NAMES[i]) + 22;
            boolean active = tab == i;
            boolean hover = Draw.inside(mouseX, mouseY, tx, tabY, w, 18);
            int bg = active ? accent : (hover ? 0x45FFFFFF : 0x22FFFFFF);
            Draw.roundRect(g, tx, tabY, w, 18, 9, bg);
            Component label = active ? Component.literal(TAB_NAMES[i]).withStyle(ChatFormatting.BOLD)
                    : Component.literal(TAB_NAMES[i]);
            g.drawString(this.font, label, tx + (w - this.font.width(label)) / 2, tabY + 5,
                    active ? 0xFFFFFFFF : 0xFFC8C8D4, false);
            if (click && hover) {
                tab = i;
            }
            tx += w + 6;
        }

        int bodyY = py + HEAD + TABS - 4;
        Module hovered = null;
        if (tab == 0) {
            hovered = drawColumns(g, mouseX, mouseY, click, dt, px, bodyY, colW, modRowH, accent, query);
        } else {
            drawSettings(g, mouseX, mouseY, click, px, bodyY, pw, setRowH, accent,
                    tab == 1 ? Module.Category.OPCJE : Module.Category.WYGLAD);
        }

        // stopka
        int fy = py + HEAD + TABS + body + 10;
        g.fill(px + 12, fy - 6, px + pw - 12, fy - 5, 0x22FFFFFF);
        g.drawString(this.font, Component.literal("Motyw").withStyle(ChatFormatting.BOLD), px + 14, fy + 3,
                0xFFB0B0C0, false);
        for (int i = 0; i < Theme.ACCENTS.length; i++) {
            int swx = px + 58 + i * 20;
            boolean selected = i == Math.floorMod(Config.getTheme(), Theme.ACCENTS.length);
            boolean hover = Draw.inside(mouseX, mouseY, swx - 2, fy - 2, 16, 16);
            if (selected) {
                Draw.roundRect(g, swx - 2, fy - 2, 16, 16, 5, 0xFFFFFFFF);
            } else if (hover) {
                Draw.roundRect(g, swx - 2, fy - 2, 16, 16, 5, 0x80FFFFFF);
            }
            Draw.roundRect(g, swx, fy, 12, 12, 4, Theme.swatch(i));
            if (click && hover) {
                Config.setTheme(i);
                Config.save();
            }
        }

        String offLabel = "Wyłącz wszystko";
        int offW = this.font.width(offLabel) + 14;
        int offX = px + pw - 12 - offW;
        int offY = fy - 2;
        boolean offHover = Draw.inside(mouseX, mouseY, offX, offY, offW, 16);
        Draw.roundRect(g, offX, offY, offW, 16, 5, offHover ? 0x80FF5555 : 0x45FF5555);
        g.drawCenteredString(this.font, offLabel, offX + offW / 2, offY + 4, 0xFFFFFFFF);
        if (click && offHover) {
            for (Module m : Module.values()) {
                if (Config.isOn(m)) Config.toggle(m);
            }
            Config.save();
        }

        String resetLabel = "Reset opcji";
        int resetW = this.font.width(resetLabel) + 14;
        int resetX = offX - 4 - resetW;
        boolean resetHover = Draw.inside(mouseX, mouseY, resetX, offY, resetW, 16);
        Draw.roundRect(g, resetX, offY, resetW, 16, 5, resetHover ? 0x80FFFFFF : 0x30FFFFFF);
        g.drawCenteredString(this.font, resetLabel, resetX + resetW / 2, offY + 4, 0xFFFFFFFF);
        if (click && resetHover) {
            Config.resetSettings();
            Config.save();
        }

        // linia podpowiedzi: opis modulu pod kursorem albo skroty
        String hint;
        if (hovered != null) {
            String key = VisualUtilsClient.keyHint(hovered);
            hint = hovered.label() + ": " + hovered.description() + (key != null ? "   [" + key + "]" : "");
        } else {
            hint = "Menu [" + VisualUtilsClient.menuKeyName() + "]   Friend [" + VisualUtilsClient.friendKeyName()
                    + "]   Klawisze modułów: Opcje > Sterowanie > NullClient";
        }
        g.drawString(this.font, hint, px + 14, fy + 22, 0xFF7C7C8E, false);
    }

    /** Rysuje cztery kolumny modulow. Zwraca modul pod kursorem (do podpowiedzi) albo null. */
    private Module drawColumns(GuiGraphics g, int mouseX, int mouseY, boolean click, float dt,
                               int px, int bodyY, int colW, int rowH, int accent, String query) {
        Module hovered = null;
        for (int c = 0; c < COLS.length; c++) {
            Module.Category cat = COLS[c];
            List<Module> list = modulesOf(cat);
            int cx = px + GAP + c * (colW + GAP);
            int colH = COLHEAD + maxModuleRows() * rowH;

            Draw.roundRect(g, cx, bodyY, colW, colH, 6, 0x26000000);
            Draw.roundRect(g, cx, bodyY, colW, colH, 6, 0x0CFFFFFF);

            int on = 0;
            for (Module m : list) {
                if (Config.isOn(m)) on++;
            }
            g.drawString(this.font, Component.literal(cat.label()).withStyle(ChatFormatting.BOLD),
                    cx + 8, bodyY + 6, accent, false);
            String badge = on + "/" + list.size();
            g.drawString(this.font, badge, cx + colW - 8 - this.font.width(badge), bodyY + 6, 0xFF7C7C8E, false);
            g.fill(cx + 6, bodyY + COLHEAD - 3, cx + colW - 6, bodyY + COLHEAD - 2, Theme.withAlpha(accent, 0x50));

            for (int i = 0; i < list.size(); i++) {
                Module m = list.get(i);
                int y = bodyY + COLHEAD + i * rowH;
                int rx = cx + 4;
                int rw = colW - 8;
                int rh = rowH - 2;
                boolean hover = Draw.inside(mouseX, mouseY, rx, y, rw, rh);
                if (hover) hovered = m;
                boolean match = matches(m, query);

                boolean isOn = Config.isOn(m);
                float a = anim.get(m);
                a += ((isOn ? 1f : 0f) - a) * Math.min(1f, dt * 16f);
                anim.put(m, a);

                int bg = hover ? 0x34FFFFFF : (!query.isEmpty() && match ? Theme.withAlpha(accent, 0x30) : 0x14FFFFFF);
                Draw.roundRect(g, rx, y, rw, rh, 4, bg);
                if (isOn) {
                    g.fill(rx, y + 3, rx + 2, y + rh - 3, accent);
                }

                int textAlpha = match ? 0xFF : 0x60;
                int textColor = Theme.withAlpha(isOn ? 0xFFFFFF : 0xD0D0DC, textAlpha);
                String label = m.label();
                int maxLabel = rw - 36;
                while (this.font.width(label) > maxLabel && label.length() > 3) {
                    label = label.substring(0, label.length() - 2);
                }
                g.drawString(this.font, label, rx + 8, y + (rh - 8) / 2, textColor, false);

                // maly przelacznik
                int swW = 22;
                int swH = 11;
                int swX = rx + rw - 6 - swW;
                int swY = y + (rh - swH) / 2;
                Draw.roundRect(g, swX, swY, swW, swH, 5, Theme.lerp(0xFF3A3A4A, accent, a));
                Draw.roundRect(g, swX + 1 + Math.round(a * 11f), swY + 1, 9, 9, 4, 0xFFFFFFFF);

                // kropka, gdy modul ma przypisany klawisz
                if (VisualUtilsClient.keyHint(m) != null) {
                    g.fill(swX - 5, y + rh / 2 - 1, swX - 3, y + rh / 2 + 1, Theme.withAlpha(accent, 0xFF));
                }

                if (click && hover) {
                    Config.toggle(m);
                    Config.save();
                }
            }
        }
        return hovered;
    }

    private void drawSettings(GuiGraphics g, int mouseX, int mouseY, boolean click,
                              int px, int ry, int pw, int rowH, int accent, Module.Category cat) {
        List<Setting> settings = settingsOf(cat);
        for (int i = 0; i < settings.size(); i++) {
            Setting s = settings.get(i);
            int y = ry + i * rowH;
            int rx = px + 8;
            int rw = pw - 16;
            int rh = rowH - 2;
            boolean hover = Draw.inside(mouseX, mouseY, rx, y, rw, rh);

            Draw.roundRect(g, rx, y, rw, rh, 4, hover ? 0x32FFFFFF : 0x16FFFFFF);
            g.fill(rx, y + 3, rx + 2, y + rh - 3, Theme.withAlpha(accent, 0xB0));

            int textY = y + (rh - 17) / 2;
            g.drawString(this.font, Component.literal(s.label()).withStyle(ChatFormatting.BOLD), rx + 10, textY,
                    0xFFFFFFFF, false);
            g.drawString(this.font, s.description() + "  (PPM = wstecz)", rx + 10, textY + 10, 0xFF9A9AAC, false);

            // pole z aktualna wartoscia - klik przelacza na nastepna, prawy klik na poprzednia
            String value = Config.settingText(s);
            int pillW = this.font.width(value) + 18;
            int pillH = 14;
            int pillX = rx + rw - 10 - pillW;
            int pillY = y + (rh - pillH) / 2;
            boolean pillHover = Draw.inside(mouseX, mouseY, pillX, pillY, pillW, pillH);
            Draw.roundRect(g, pillX, pillY, pillW, pillH, 7,
                    Theme.withAlpha(accent, pillHover ? 0xA0 : 0x70));
            g.drawCenteredString(this.font, value, pillX + pillW / 2, pillY + 3, 0xFFFFFFFF);

            if (hover && (click || rightClick)) {
                if (rightClick) Config.cyclePrev(s);
                else Config.cycle(s);
                Config.save();
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Config.save();
        super.onClose();
    }
}

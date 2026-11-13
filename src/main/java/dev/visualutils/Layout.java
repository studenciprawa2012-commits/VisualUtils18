package dev.visualutils;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Antykostka: zapis ukladu paska (hotbar) osobno dla kazdego serwera i przywracanie go jednym klawiszem.
 * Uklad zostaje zapisany w pliku, wiec dziala tez po ponownym wejsciu na serwer. Jedna zamiana na tick.
 */
public final class Layout {

    private static final Properties STORE = new Properties();
    private static boolean loaded = false;
    private static String[] active = null;
    private static boolean restoring = false;
    private static int steps = 0;

    private Layout() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("visualutils_layouts.properties");
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        Path f = file();
        if (Files.exists(f)) {
            try (InputStream in = Files.newInputStream(f)) {
                STORE.load(in);
            } catch (IOException ignored) {
                // brak zapisanych ukladow to nie blad
            }
        }
    }

    private static void persist() {
        try (OutputStream out = Files.newOutputStream(file())) {
            STORE.store(out, "NullClient - Antykostka (uklady paska per serwer)");
        } catch (IOException ignored) {
            // uklad zostaje w pamieci do konca sesji
        }
    }

    private static String serverKey(Minecraft mc) {
        var server = mc.getCurrentServer();
        return server != null ? server.ip.toLowerCase() : "singleplayer";
    }

    private static String key(ItemStack stack) {
        if (stack.isEmpty()) return "";
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static void say(LocalPlayer p, String text) {
        p.displayClientMessage(Component.literal(text), true);
    }

    public static void save(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null) return;
        if (!Config.isOn(Module.ANTYKOSTKA)) {
            say(p, "Antykostka jest wyłączona (włącz w menu: PVP)");
            return;
        }
        ensureLoaded();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            if (i > 0) sb.append(',');
            sb.append(key(p.getInventory().getItem(i)));
        }
        STORE.setProperty(serverKey(mc), sb.toString());
        persist();
        say(p, "Antykostka: układ paska zapisany dla " + serverKey(mc));
    }

    public static void restore(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null) return;
        if (!Config.isOn(Module.ANTYKOSTKA)) {
            say(p, "Antykostka jest wyłączona (włącz w menu: PVP)");
            return;
        }
        ensureLoaded();
        String raw = STORE.getProperty(serverKey(mc));
        if (raw == null) {
            say(p, "Antykostka: brak zapisanego układu dla tego serwera (klawisz zapisu)");
            return;
        }
        String[] parts = raw.split(",", -1);
        active = new String[9];
        for (int i = 0; i < 9; i++) {
            active[i] = i < parts.length ? parts[i] : "";
        }
        restoring = true;
        steps = 0;
    }

    public static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (!restoring || p == null || mc.gameMode == null || active == null) {
            restoring = false;
            return;
        }

        var menu = p.inventoryMenu;
        for (int h = 0; h < 9; h++) {
            String want = active[h];
            if (want.isEmpty()) continue;
            if (key(menu.getSlot(36 + h).getItem()).equals(want)) continue;

            int from = -1;
            // najpierw glowny ekwipunek, potem inne miejsca paska
            for (int s = 9; s <= 35 && from < 0; s++) {
                if (key(menu.getSlot(s).getItem()).equals(want)) from = s;
            }
            for (int other = 0; other < 9 && from < 0; other++) {
                if (other == h) continue;
                String have = key(menu.getSlot(36 + other).getItem());
                if (have.equals(want) && !want.equals(active[other])) from = 36 + other;
            }
            if (from < 0) continue;

            mc.gameMode.handleInventoryMouseClick(menu.containerId, from, h, ClickType.SWAP, p);
            if (++steps > 24) {
                restoring = false;
                say(p, "Antykostka: pasek ułożony (część przedmiotów nie znaleziona)");
            }
            return;
        }
        restoring = false;
        say(p, "Antykostka: pasek ułożony");
    }
}

package dev.visualutils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Gracze lecacy na elytrze w poblizu: dystans, kierunek i predkosc (alert z boku ekranu). */
public final class ElytraAlert {

    public record Entry(String name, double dist, String dir, int speed) {}

    private static long since = 0L;

    private static List<Entry> entries = List.of();
    private static final Map<UUID, Vec3> LAST = new HashMap<>();
    private static Set<UUID> announced = new HashSet<>();

    private ElytraAlert() {}

    /** Od kiedy trwa alert (do animacji wejscia). */
    public static long since() {
        return since;
    }

    public static List<Entry> entries() {
        return entries;
    }

    public static void tick(Minecraft mc) {
        LocalPlayer me = mc.player;
        if (me == null || mc.level == null || !Config.isOn(Module.ELYTRAALERT)) {
            entries = List.of();
            LAST.clear();
            announced = new HashSet<>();
            return;
        }

        double range = Config.settingInt(Setting.ELYTRA_RANGE);
        List<Entry> list = new ArrayList<>();
        Set<UUID> now = new HashSet<>();
        boolean alert = false;
        for (AbstractClientPlayer other : mc.level.players()) {
            if (other == me || !other.isAlive() || !other.isFallFlying()) continue;
            if (Friends.isFriend(other)) continue;
            double dist = other.distanceTo(me);
            if (dist > range) continue;

            Vec3 pos = other.position();
            Vec3 prev = LAST.put(other.getUUID(), pos);
            int speed = prev == null ? 0 : (int) Math.round(pos.distanceTo(prev) * 20.0);

            double yawTo = Math.toDegrees(Math.atan2(-(pos.x - me.getX()), pos.z - me.getZ()));
            double rel = wrap(yawTo - me.getYRot());
            String dir;
            if (Math.abs(rel) < 45) dir = "przed tobą";
            else if (rel >= 45 && rel <= 135) dir = "z prawej";
            else if (rel <= -45 && rel >= -135) dir = "z lewej";
            else dir = "za tobą";

            list.add(new Entry(other.getName().getString(), dist, dir, speed));
            now.add(other.getUUID());
            if (!announced.contains(other.getUUID())) {
                alert = true;
            }
        }
        LAST.keySet().retainAll(now);
        announced = now;
        list.sort(Comparator.comparingDouble(Entry::dist));
        if (entries.isEmpty() && !list.isEmpty()) {
            since = System.currentTimeMillis();
        }
        entries = list;

        if (alert && Config.setting(Setting.ALERT_SOUND) == 0) {
            // krotki sygnal tylko wtedy, gdy ktos NOWY zaczyna lot w poblizu
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6f, 0.8f));
        }
    }

    private static double wrap(double a) {
        a %= 360.0;
        if (a > 180.0) a -= 360.0;
        if (a < -180.0) a += 360.0;
        return a;
    }
}

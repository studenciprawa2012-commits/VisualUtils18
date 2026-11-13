package dev.visualutils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Wykrywacz pulapek: szuka w poblizu skupisk widocznych dla klienta encji, ktore zwykle oznaczaja pulapke
 * (kilka wagonikow w jednym miejscu, TNT, wagoniki z TNT, wiele spadajacych blokow).
 * Skanuje tylko encje, ktore serwer i tak wysyla klientowi - nie podglada blokow za scianami.
 */
public final class TrapScan {

    public record Mark(Vec3 pos, String label, double dist) {}

    private static final double CLUSTER_RADIUS = 7.0;
    private static final double RANGE = 64.0;

    private static List<Mark> marks = List.of();
    private static int ticks = 0;
    private static int lastCount = 0;

    private TrapScan() {}

    public static List<Mark> marks() {
        return marks;
    }

    private static boolean isMinecart(EntityType<?> t) {
        return t == EntityType.MINECART || t == EntityType.CHEST_MINECART || t == EntityType.HOPPER_MINECART
                || t == EntityType.FURNACE_MINECART || t == EntityType.SPAWNER_MINECART
                || t == EntityType.COMMAND_BLOCK_MINECART;
    }

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null || !Config.isOn(Module.TRAPDETECT)) {
            marks = List.of();
            lastCount = 0;
            return;
        }
        if (++ticks % 10 != 0) return;

        List<Entity> suspects = new ArrayList<>();
        for (Entity e : mc.level.entitiesForRendering()) {
            EntityType<?> t = e.getType();
            if (e.distanceTo(mc.player) > RANGE) continue;
            if (isMinecart(t) || t == EntityType.TNT_MINECART || t == EntityType.TNT || t == EntityType.FALLING_BLOCK) {
                suspects.add(e);
            }
        }

        List<Mark> result = new ArrayList<>();
        boolean[] used = new boolean[suspects.size()];
        for (int i = 0; i < suspects.size(); i++) {
            if (used[i]) continue;
            Entity first = suspects.get(i);
            int carts = 0;
            int tntCarts = 0;
            int tnt = 0;
            int falling = 0;
            double sx = 0;
            double sy = 0;
            double sz = 0;
            int n = 0;
            for (int j = i; j < suspects.size(); j++) {
                if (used[j]) continue;
                Entity other = suspects.get(j);
                double dx = other.getX() - first.getX();
                double dz = other.getZ() - first.getZ();
                if (Math.hypot(dx, dz) > CLUSTER_RADIUS || Math.abs(other.getY() - first.getY()) > 8.0) continue;
                used[j] = true;
                EntityType<?> t = other.getType();
                if (t == EntityType.TNT_MINECART) tntCarts++;
                else if (isMinecart(t)) carts++;
                else if (t == EntityType.TNT) tnt++;
                else falling++;
                sx += other.getX();
                sy += other.getY();
                sz += other.getZ();
                n++;
            }
            List<String> parts = new ArrayList<>();
            if (carts + tntCarts >= 2) parts.add((carts + tntCarts) + " wagoniki");
            if (tntCarts >= 1) parts.add("TNT w wagoniku");
            if (tnt >= 1) parts.add("TNT");
            if (falling >= 3) parts.add(falling + " spadających bloków");
            if (parts.isEmpty()) continue;

            Vec3 center = new Vec3(sx / n, sy / n, sz / n);
            result.add(new Mark(center, String.join(", ", parts), center.distanceTo(mc.player.position())));
        }
        marks = result;

        if (result.size() > lastCount && Config.setting(Setting.ALERT_SOUND) == 0) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5f, 0.8f));
        }
        lastCount = result.size();
    }
}

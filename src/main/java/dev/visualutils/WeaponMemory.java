package dev.visualutils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Serwer nie wysyla klientowi calego ekwipunku innych graczy - widzimy tylko to, co trzymaja w rekach i maja na sobie.
 * Dlatego zapamietujemy ostatni miecz (albo topor), ktory dany gracz mial w reku, kiedy na niego patrzylismy.
 */
public final class WeaponMemory {

    private static final Map<UUID, ItemStack> LAST = new HashMap<>();

    private WeaponMemory() {}

    private static boolean isWeapon(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES));
    }

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null || !Config.isOn(Module.SETINFO)) {
            LAST.clear();
            return;
        }
        Set<UUID> present = new HashSet<>();
        for (AbstractClientPlayer p : mc.level.players()) {
            present.add(p.getUUID());
            ItemStack main = p.getItemBySlot(EquipmentSlot.MAINHAND);
            ItemStack off = p.getItemBySlot(EquipmentSlot.OFFHAND);
            if (isWeapon(main)) {
                LAST.put(p.getUUID(), main.copy());
            } else if (isWeapon(off)) {
                LAST.put(p.getUUID(), off.copy());
            }
        }
        LAST.keySet().retainAll(present);
    }

    /** Miecz trzymany teraz albo ostatni zapamietany (null, gdy nigdy nie widzielismy go z mieczem). */
    public static ItemStack weaponOf(LivingEntity e) {
        ItemStack held = e.getItemBySlot(EquipmentSlot.MAINHAND);
        if (isWeapon(held)) return held;
        ItemStack remembered = LAST.get(e.getUUID());
        return remembered != null ? remembered : held;
    }
}

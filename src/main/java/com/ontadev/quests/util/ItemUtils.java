// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public final class ItemUtils {

    private ItemUtils() {
    }

    public static boolean isAir(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    public static boolean isSameItem(ItemStack item, ItemStack target, boolean ignoreDurability) {
        if (item == null || target == null) return false;
        if (item.getType() != target.getType()) return false;

        ItemMeta metaA = item.getItemMeta();
        ItemMeta metaB = target.getItemMeta();
        if (metaA == null && metaB == null) return true;
        if (metaA == null || metaB == null) return false;

        if (ignoreDurability && metaA instanceof Damageable && metaB instanceof Damageable) {
            ItemMeta cloneA = metaA.clone();
            ItemMeta cloneB = metaB.clone();
            ((Damageable) cloneA).setDamage(0);
            ((Damageable) cloneB).setDamage(0);
            return cloneA.equals(cloneB);
        }

        return metaA.equals(metaB);
    }

    /** Предмет с таким типом подходит под целевой, если у него есть все зачарования цели не ниже нужного уровня. */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean enchantsMatch(ItemStack target, Material type, Map<Enchantment, Integer> enchants) {
        if (target.getType() != type) return false;

        for (Map.Entry<Enchantment, Integer> wanted : target.getEnchantments().entrySet()) {
            Integer level = enchants.get(wanted.getKey());
            if (level == null || level < wanted.getValue()) return false;
        }
        return true;
    }
}

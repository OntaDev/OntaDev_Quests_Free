// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.util;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class InventorySpace {

    private InventorySpace() {
    }

    public static boolean fits(ItemStack[] storage, List<ItemStack> items) {
        ItemStack[] kinds = storage.clone();
        int[] amounts = new int[storage.length];
        for (int i = 0; i < storage.length; i++) {
            amounts[i] = storage[i] == null || storage[i].getType().isAir() ? 0 : storage[i].getAmount();
        }

        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) continue;

            int max = item.getMaxStackSize();
            int remaining = item.getAmount();

            for (int i = 0; i < kinds.length && remaining > 0; i++) {
                if (amounts[i] == 0 || amounts[i] >= max || !kinds[i].isSimilar(item)) continue;
                int moved = Math.min(max - amounts[i], remaining);
                amounts[i] += moved;
                remaining -= moved;
            }
            for (int i = 0; i < kinds.length && remaining > 0; i++) {
                if (amounts[i] != 0) continue;
                int moved = Math.min(max, remaining);
                kinds[i] = item;
                amounts[i] = moved;
                remaining -= moved;
            }
            if (remaining > 0) return false;
        }
        return true;
    }
}

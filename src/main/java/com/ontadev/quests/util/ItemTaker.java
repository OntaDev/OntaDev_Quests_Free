// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.util;

import org.bukkit.inventory.ItemStack;

import java.util.function.Predicate;

public final class ItemTaker {

    private ItemTaker() {
    }

    /**
     * Забирает до {@code max} предметов, подходящих под условие, из массива ячеек (на месте).
     *
     * @return сколько предметов реально забрано
     */
    public static int take(ItemStack[] slots, Predicate<ItemStack> matches, int max) {
        int taken = 0;

        for (int i = 0; i < slots.length && taken < max; i++) {
            ItemStack item = slots[i];
            if (ItemUtils.isAir(item) || !matches.test(item)) continue;

            int amount = Math.min(item.getAmount(), max - taken);
            taken += amount;

            if (amount >= item.getAmount()) {
                slots[i] = null;
            } else {
                item.setAmount(item.getAmount() - amount);
            }
        }

        return taken;
    }
}

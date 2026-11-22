// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ItemTakerTest {

    private ItemStack stack(Material type, int amount) {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(type);
        when(item.getAmount()).thenReturn(amount);
        return item;
    }

    @Test
    void takesWholeStacksAcrossSlotsUntilTheLimit() {
        ItemStack[] slots = {stack(Material.STONE, 10), stack(Material.STONE, 10), stack(Material.STONE, 10)};

        int taken = ItemTaker.take(slots, item -> true, 25);

        assertEquals(25, taken);
        assertNull(slots[0]);
        assertNull(slots[1]);
        verify(slots[2]).setAmount(5);
    }

    @Test
    void aPartiallyTakenStackIsCountedByWhatWasTakenNotItsFullSize() {
        ItemStack[] slots = {stack(Material.STONE, 64)};

        int taken = ItemTaker.take(slots, item -> true, 10);

        assertEquals(10, taken, "раньше засчитывался весь стак (64), а не 10");
        verify(slots[0]).setAmount(54);
    }

    @Test
    void onlyMatchingItemsAreTakenAndAirIsSkipped() {
        ItemStack stone = stack(Material.STONE, 5);
        ItemStack dirt = stack(Material.DIRT, 5);
        ItemStack[] slots = {null, dirt, stack(Material.AIR, 0), stone};

        int taken = ItemTaker.take(slots, item -> item.getType() == Material.STONE, 100);

        assertEquals(5, taken);
        assertEquals(dirt, slots[1]);
        assertNull(slots[3]);
    }

    @Test
    void takesNothingWhenNothingMatches() {
        ItemStack[] slots = {stack(Material.DIRT, 5)};

        assertEquals(0, ItemTaker.take(slots, item -> false, 10));
    }
}

// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InventorySpaceTest {

    private static ItemStack occupied(Material type, int amount) {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(type);
        when(item.getAmount()).thenReturn(amount);
        when(item.isSimilar(org.mockito.ArgumentMatchers.any())).thenReturn(true);
        return item;
    }

    private static ItemStack[] filled(int slots, ItemStack with) {
        ItemStack[] storage = new ItemStack[slots];
        Arrays.fill(storage, with);
        return storage;
    }

    @Test
    void itemsFitIntoEmptySlots() {
        assertTrue(InventorySpace.fits(new ItemStack[3],
                Arrays.asList(new ItemStack(Material.DIAMOND), new ItemStack(Material.GOLD_INGOT), new ItemStack(Material.STONE, 64))));
    }

    @Test
    void notEnoughEmptySlotsMeansNoFit() {
        assertFalse(InventorySpace.fits(new ItemStack[2],
                Arrays.asList(new ItemStack(Material.DIAMOND), new ItemStack(Material.GOLD_INGOT), new ItemStack(Material.EMERALD))));
    }

    @Test
    void aBigStackNeedsAsManySlotsAsItSpansAcrossFullStacks() {
        assertFalse(InventorySpace.fits(new ItemStack[1], Collections.singletonList(new ItemStack(Material.STONE, 65))));
        assertTrue(InventorySpace.fits(new ItemStack[2], Collections.singletonList(new ItemStack(Material.STONE, 65))));
    }

    @Test
    void partialStacksOfTheSameItemAreTopped() {
        ItemStack[] storage = filled(1, occupied(Material.STONE, 60));
        assertTrue(InventorySpace.fits(storage, Collections.singletonList(new ItemStack(Material.STONE, 4))));
        assertFalse(InventorySpace.fits(storage, Collections.singletonList(new ItemStack(Material.STONE, 5))));
    }

    @Test
    void fullInventoryAcceptsNothingButAnEmptyReward() {
        ItemStack[] storage = filled(36, occupied(Material.DIRT, 64));
        assertFalse(InventorySpace.fits(storage, Collections.singletonList(new ItemStack(Material.DIAMOND))));
        assertTrue(InventorySpace.fits(storage, Collections.emptyList()));
    }

    @Test
    void theInventoryItselfIsNeverModified() {
        ItemStack[] storage = new ItemStack[2];
        InventorySpace.fits(storage, Collections.singletonList(new ItemStack(Material.DIAMOND)));
        assertFalse(storage[0] != null || storage[1] != null);
    }
}

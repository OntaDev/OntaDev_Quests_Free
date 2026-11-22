// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RewardTest {

    @Test
    void aRewardHoldsAtMostTwentySevenItems() {
        List<ItemStack> many = new ArrayList<>();
        for (int i = 0; i < 40; i++) many.add(new ItemStack(Material.DIAMOND, i + 1));

        Reward reward = new Reward(1, null, many);

        assertEquals(27, reward.getItems().size());
        assertEquals(27, reward.getItems().get(26).getAmount());
    }

    @Test
    void emptyStacksAreDroppedWhenItemsAreSet() {
        Reward reward = new Reward(1, null, Arrays.asList(null, new ItemStack(Material.AIR), new ItemStack(Material.STONE)));

        assertEquals(1, reward.getItems().size());
        assertEquals(Material.STONE, reward.getItems().get(0).getType());
    }

    @Test
    void theRewardKeepsItsOwnCopiesOfTheItems() {
        ItemStack original = new ItemStack(Material.DIAMOND, 3);
        Reward reward = new Reward(1, null, original);

        original.setAmount(64);

        assertNotSame(original, reward.getItems().get(0));
        assertEquals(3, reward.getItems().get(0).getAmount());
    }

    @Test
    void aSingleNullItemMeansNoItems() {
        assertTrue(new Reward(1, null, (ItemStack) null).getItems().isEmpty());
    }
}

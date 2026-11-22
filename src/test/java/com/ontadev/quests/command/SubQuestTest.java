// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.command;

import com.ontadev.quests.config.MessagesConfig;
import com.ontadev.quests.model.Quest;
import com.ontadev.quests.service.QuestService;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SubQuestTest {
    private QuestService quests;
    private MessagesConfig messages;
    private Player player;
    private PlayerInventory inventory;
    private SubQuest command;

    @BeforeEach
    void setUp() {
        quests = mock(QuestService.class);
        messages = mock(MessagesConfig.class, RETURNS_DEEP_STUBS);
        player = mock(Player.class);
        inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(inventory.getItemInOffHand()).thenReturn(new ItemStack(Material.STONE));
        when(inventory.getItemInMainHand()).thenReturn(new ItemStack(Material.AIR));
        command = new SubQuest(quests, messages);
    }

    @Test
    void emptyMainHandCannotCreateAQuestEvenWhenItsTargetHasAnIcon() {
        command.execute(player, null, "quests", "create", "BREAK_BLOCKS", "64", "Камень");
        command.execute(player, null, "quests", "confirm");

        verify(messages.getNoQuestIcon()).send((CommandSender) player);
        verify(messages.getNoQuestToConfirm()).send((CommandSender) player);
        verifyNoInteractions(quests);
    }

    @Test
    void creatingAQuestUsesACopyOfTheExplicitIcon() {
        ItemStack icon = new ItemStack(Material.DIAMOND_PICKAXE);
        when(inventory.getItemInMainHand()).thenReturn(icon);
        when(quests.save(any())).thenAnswer(inv -> CompletableFuture.completedFuture(inv.getArgument(0)));

        command.execute(player, null, "quests", "create", "BREAK_BLOCKS", "64", "Камень");
        command.execute(player, null, "quests", "confirm");

        ArgumentCaptor<Quest> saved = ArgumentCaptor.forClass(Quest.class);
        verify(quests).save(saved.capture());
        assertEquals(Material.DIAMOND_PICKAXE, saved.getValue().getIcon().getType());
        assertNotSame(icon, saved.getValue().getIcon());
    }

    @Test
    void automaticIconCommandIsRejectedAndNotSuggested() {
        when(quests.all()).thenReturn(Collections.singletonMap(1, mock(Quest.class)));

        assertEquals(Collections.singletonList("1"), command.complete(player, "icon", ""));
        command.execute(player, null, "quests", "icon", "auto", "1");

        verify(messages.getQuestIconUsage()).send((CommandSender) player);
        verify(quests, never()).changeIcon(anyInt(), any());
    }

    @Test
    void manualRangeStillAppliesTheHeldIconToEveryExistingQuest() {
        ItemStack icon = new ItemStack(Material.DIAMOND);
        when(inventory.getItemInMainHand()).thenReturn(icon);
        Quest quest = mock(Quest.class);
        when(quests.get(1)).thenReturn(quest);
        when(quests.get(2)).thenReturn(quest);
        when(quests.changeIcon(anyInt(), any())).thenReturn(CompletableFuture.completedFuture(quest));

        command.execute(player, null, "quests", "icon", "1-2");

        verify(quests).changeIcon(1, icon);
        verify(quests).changeIcon(2, icon);
    }
}

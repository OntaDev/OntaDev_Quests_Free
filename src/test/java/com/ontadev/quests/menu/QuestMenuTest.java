// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.menu;

import com.ontadev.libs.item.ItemModel;
import com.ontadev.libs.menu.AbstractMenu;
import com.ontadev.libs.menu.manager.MenuManager;
import com.ontadev.libs.player.PlayerResolver;
import com.ontadev.libs.player.PlayerSnapshot;
import com.ontadev.quests.config.QuestsConfig;
import com.ontadev.quests.model.Quest;
import com.ontadev.quests.model.QuestPlayer;
import com.ontadev.quests.model.QuestType;
import com.ontadev.quests.model.Reward;
import com.ontadev.quests.service.PlayerService;
import com.ontadev.quests.service.QuestService;
import com.ontadev.quests.service.RewardService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("FieldCanBeLocal")
class QuestMenuTest {
    private final PlayerSnapshot snapshot = new PlayerSnapshot(UUID.randomUUID(), "Alex");
    private final QuestPlayer progress = new QuestPlayer(snapshot.uuid(), 3);
    private final Quest quest = new Quest(9, "Принести предметы", null, 10,
            new ItemStack(Material.STONE), QuestType.BRING_ITEMS, null);
    private final Reward reward = new Reward(1, new ItemStack(Material.CHEST), new ItemStack(Material.DIAMOND));
    private MockedStatic<Bukkit> bukkit;
    private Player player;
    private PlayerService players;
    private QuestService quests;
    private RewardService rewards;
    private MenuItemCache cache;
    private QuestMenu menu;
    private MenuManager manager;
    private PlayerResolver resolver;

    @BeforeEach
    void setUp() {
        player = mock(Player.class);
        when(player.hasPermission(anyString())).thenReturn(true);
        when(player.getUniqueId()).thenReturn(snapshot.uuid());
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(() -> Bukkit.getPlayer(snapshot.uuid())).thenReturn(player);
        progress.setQuestId(9);
        players = mock(PlayerService.class);
        quests = mock(QuestService.class);
        rewards = mock(RewardService.class);
        cache = mock(MenuItemCache.class);
        when(players.cached(snapshot.uuid())).thenReturn(progress);
        when(players.canOpenPage(player, progress, 1)).thenReturn(true);
        when(quests.page(1)).thenReturn(Collections.singletonMap(9, quest));
        when(rewards.page(1)).thenReturn(Collections.singletonMap(1, reward));
        when(cache.current(quest, progress, true, true)).thenReturn(new ItemStack(Material.STONE));
        when(cache.reward(eq(reward), any())).thenReturn(new ItemStack(Material.CHEST));
        manager = mock(MenuManager.class);
        resolver = mock(PlayerResolver.class);
        menu = new QuestMenu(new QuestsConfig(), quests, rewards, players, cache, new MenuState(), manager, resolver);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    private InventoryClickEvent click(ClickType type) {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getClick()).thenReturn(type);
        when(event.getWhoClicked()).thenReturn(player);
        return event;
    }

    @Test
    void oldAdminGesturesDoNotEditOrGrantAnything() {
        Map<Integer, ItemModel> items = menu.dynamicItems(snapshot);
        for (ClickType type : new ClickType[]{ClickType.RIGHT, ClickType.SHIFT_RIGHT}) {
            InventoryClickEvent event = click(type);
            when(event.getCursor()).thenReturn(new ItemStack(Material.DIAMOND));
            items.get(18).handleClick(event);
            items.get(17).handleClick(event);
        }
        verify(quests, never()).changeIcon(anyInt(), any());
        verify(players, never()).giveReward(any(), anyInt(), any());
        verify(players, never()).addProgress(any(), any(), anyInt());
        verify(player, never()).getInventory();
    }

    @Test
    void leftClickStillSubmitsItemsForTheCurrentQuest() {
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getStorageContents()).thenReturn(new ItemStack[]{new ItemStack(Material.DIAMOND, 16)});

        menu.dynamicItems(snapshot).get(18).handleClick(click(ClickType.LEFT));

        verify(players).addProgress(progress, quest, 10);
        ArgumentCaptor<ItemStack[]> remaining = ArgumentCaptor.forClass(ItemStack[].class);
        verify(inventory).setStorageContents(remaining.capture());
        assertEquals(6, remaining.getValue()[0].getAmount());
        verify(quests, never()).changeIcon(anyInt(), any());
    }

    @Test
    void leftClickClaimsAnExistingReward() {
        menu.dynamicItems(snapshot).get(17).handleClick(click(ClickType.LEFT));
        verify(players).giveReward(progress, 1, player);
    }

    @Test
    void shiftLeftClickStillUsesTheNormalSkipPermissionCheck() {
        menu.dynamicItems(snapshot).get(18).handleClick(click(ClickType.SHIFT_LEFT));
        verify(players).skipQuest(player, progress);
        verify(players, never()).addProgress(any(), any(), anyInt());
    }

    @Test
    void anUnsetRewardHasNoCreationAction() {
        when(rewards.page(1)).thenReturn(Collections.emptyMap());
        ItemModel slot = menu.dynamicItems(snapshot).get(17);
        assertEquals(Material.AIR, slot.getMaterial());
        assertNull(slot.getFallbackInteraction());
    }

    @Test
    void operatorsCannotMoveItemsIntoTheMenu() {
        InventoryClickEvent event = click(ClickType.LEFT);
        when(event.getRawSlot()).thenReturn(54);
        assertFalse(menu.onClick(snapshot, event));
        assertFalse(menu.onDrag(snapshot, mock(InventoryDragEvent.class)));
        assertFalse(menu.isEditable());
    }

    @Test
    void staleMenuClicksCannotConsumeItemsOrSkipsAfterAReset() {
        Map<Integer, ItemModel> items = menu.dynamicItems(snapshot);
        when(players.generation()).thenReturn(1L);

        items.get(18).handleClick(click(ClickType.LEFT));
        items.get(18).handleClick(click(ClickType.SHIFT_LEFT));
        items.get(17).handleClick(click(ClickType.LEFT));

        verify(player, never()).getInventory();
        verify(players, never()).skipQuest(any(), any());
        verify(players, never()).giveReward(any(), anyInt(), any());
    }

    @Test
    void reloadingReopensOnlyQuestMenusAndPreservesTheSelectedPage() {
        Player other = mock(Player.class);
        Player withoutMenu = mock(Player.class);
        PlayerSnapshot otherSnapshot = new PlayerSnapshot(UUID.randomUUID(), "Other");
        PlayerSnapshot emptySnapshot = new PlayerSnapshot(UUID.randomUUID(), "Empty");
        bukkit.when(Bukkit::getOnlinePlayers).thenReturn(java.util.List.of(player, other, withoutMenu));
        when(resolver.getPlayerSnapshot(player)).thenReturn(snapshot);
        when(resolver.getPlayerSnapshot(other)).thenReturn(otherSnapshot);
        when(resolver.getPlayerSnapshot(withoutMenu)).thenReturn(emptySnapshot);
        when(manager.activeMenu(snapshot)).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(java.util.Optional.of(menu)));
        when(manager.activeMenu(otherSnapshot)).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(
                java.util.Optional.of(mock(AbstractMenu.class))));
        when(manager.activeMenu(emptySnapshot)).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(java.util.Optional.empty()));
        when(manager.open(menu, snapshot)).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));

        menu.reopenActiveMenus().join();

        verify(manager).open(menu, snapshot);
        verify(manager, never()).open(any(AbstractMenu.class), eq(otherSnapshot));
        verify(manager, never()).open(any(AbstractMenu.class), eq(emptySnapshot));
    }
}

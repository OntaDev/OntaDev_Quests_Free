// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.service;

import com.ontadev.quests.config.MessagesConfig;
import com.ontadev.quests.config.QuestsConfig;
import com.ontadev.quests.metrics.QuestMetrics;
import com.ontadev.quests.model.Quest;
import com.ontadev.quests.model.QuestPlayer;
import com.ontadev.quests.model.QuestType;
import com.ontadev.quests.model.Reward;
import com.ontadev.quests.storage.PlayerRecord;
import com.ontadev.quests.storage.PlayerRepository;
import com.ontadev.quests.storage.PlayerRewardRepository;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PlayerServiceTest {

    private MockedStatic<Bukkit> bukkit;
    private PlayerRepository players;
    private PlayerRewardRepository playerRewards;
    private StorageExecutor storage;
    private QuestService quests;
    private RewardService rewards;
    private MessagesConfig messages;
    private PlayerService service;
    private QuestsConfig config;
    private QuestMetrics metrics;
    private ResetState resets;

    private final UUID uuid = UUID.randomUUID();
    private QuestPlayer questPlayer;
    private Player player;
    private PlayerInventory inventory;

    @BeforeEach
    void setUp() {
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(scheduler.runTask(any(JavaPlugin.class), any(Runnable.class))).thenAnswer(inv -> {
            ((Runnable) inv.getArgument(1)).run();
            return null;
        });
        bukkit = Mockito.mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkit.when(Bukkit::getConsoleSender).thenReturn(mock(ConsoleCommandSender.class));

        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        players = mock(PlayerRepository.class);
        playerRewards = mock(PlayerRewardRepository.class);
        // Bukkit замокан только в потоке теста, поэтому записи в БД выполняются тут же, без фонового потока
        storage = new StorageExecutor(plugin) {
            @Override
            public <T> java.util.concurrent.CompletableFuture<T> supply(java.util.function.Supplier<T> task) {
                return java.util.concurrent.CompletableFuture.completedFuture(task.get());
            }
        };
        quests = mock(QuestService.class);
        rewards = mock(RewardService.class);
        messages = mock(MessagesConfig.class, Mockito.RETURNS_DEEP_STUBS);
        config = new QuestsConfig();
        metrics = mock(QuestMetrics.class);
        resets = new ResetState();
        service = new PlayerService(plugin, players, playerRewards, storage, quests, rewards, config, messages, metrics, resets);

        questPlayer = new QuestPlayer(uuid, 3);
        player = mock(Player.class);
        inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getName()).thenReturn("Alex");
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.hasPermission("quests.skip")).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        storage.shutdown();
        bukkit.close();
    }

    private Quest quest(int id, int goal) {
        return new Quest(id, "Квест", null, goal, null, QuestType.BREAK_BLOCKS, null);
    }

    @Test
    void progressBelowTheGoalOnlyAccumulatesAndIsSavedLater() {
        questPlayer.setQuestId(2);

        service.addProgress(questPlayer, quest(2, 10), 3);
        service.addProgress(questPlayer, quest(2, 10), 4);

        assertEquals(7, questPlayer.getProgress());
        assertEquals(2, questPlayer.getQuestId());
        verify(players, never()).save(any(PlayerRecord.class));
        verifyNoInteractions(metrics);
    }

    @Test
    void reachingTheGoalCompletesTheQuestAndSavesImmediately() {
        questPlayer.setQuestId(2);
        questPlayer.setProgress(8);

        service.addProgress(questPlayer, quest(2, 10), 5);

        assertEquals(3, questPlayer.getQuestId());
        assertEquals(0, questPlayer.getProgress());
        verify(messages.getQuestCompleted()).send(uuid);
        storage.shutdown();
        ArgumentCaptor<PlayerRecord> saved = ArgumentCaptor.forClass(PlayerRecord.class);
        verify(players).save(saved.capture());
        assertEquals(3, saved.getValue().getQuestId());
        verify(metrics).questCompleted();
        verify(metrics, never()).questSkipped();
    }

    @Test
    void progressForAQuestThePlayerIsNotOnIsIgnored() {
        questPlayer.setQuestId(5);

        service.addProgress(questPlayer, quest(4, 1), 1);
        service.addProgress(questPlayer, quest(5, 3), 0);
        service.addProgress(questPlayer, quest(5, 3), -2);

        assertEquals(5, questPlayer.getQuestId());
        assertEquals(0, questPlayer.getProgress());
        verifyNoInteractions(metrics);
    }

    @Test
    void skippingSpendsASkipAndMovesOn() {
        when(quests.get(1)).thenReturn(quest(1, 5));
        questPlayer.setProgress(2);

        assertTrue(service.skipQuest(player, questPlayer));

        assertEquals(2, questPlayer.getSkips());
        assertEquals(2, questPlayer.getQuestId());
        assertEquals(0, questPlayer.getProgress());
        verify(metrics).questSkipped();
        verify(metrics, never()).questCompleted();
    }

    @Test
    void skippingNeedsThePermissionEvenWhenThePlayerHasSkipsLeft() {
        when(player.hasPermission("quests.skip")).thenReturn(false);
        when(quests.get(1)).thenReturn(quest(1, 5));

        assertFalse(service.skipQuest(player, questPlayer));

        verify(messages.getQuestSkipNotAvailable()).send(player);
        assertEquals(3, questPlayer.getSkips(), "пропуск не должен тратиться");
        assertEquals(1, questPlayer.getQuestId());
        verifyNoInteractions(metrics);
    }

    @Test
    void skippingWithoutSkipsOrWithoutAQuestChangesNothing() {
        questPlayer.setSkips(0);
        when(quests.get(1)).thenReturn(quest(1, 5));
        assertFalse(service.skipQuest(player, questPlayer));
        verify(messages.getQuestSkipsEnded()).send(player);

        questPlayer.setSkips(2);
        when(quests.get(1)).thenReturn(null);
        assertFalse(service.skipQuest(player, questPlayer));
        verify(messages.getQuestsEnded()).send(player);
        assertEquals(1, questPlayer.getQuestId());
        assertEquals(2, questPlayer.getSkips());
    }

    @Test
    void aRewardGivesItsItemRunsItsCommandsAndCannotBeTakenTwice() {
        Reward reward = new Reward(1, null, new ItemStack(org.bukkit.Material.DIAMOND));
        reward.getCommands().add("eco give %player% 100");
        when(rewards.get(1)).thenReturn(reward);
        when(inventory.getStorageContents()).thenReturn(new ItemStack[36]);
        questPlayer.setQuestId(9);

        service.giveReward(questPlayer, 1, player);
        service.giveReward(questPlayer, 1, player);
        storage.shutdown();

        bukkit.verify(() -> Bukkit.dispatchCommand(any(ConsoleCommandSender.class), Mockito.eq("eco give Alex 100")), Mockito.times(1));
        verify(inventory, Mockito.times(1)).addItem(any(ItemStack.class));
        verify(messages.getRewardAlreadyGot()).send(player);
        assertTrue(questPlayer.getRewardIds().contains(1));
        verify(metrics).rewardCollected();
    }

    @Test
    void aRewardWithSeveralItemsGivesAllOfThemAtOnce() {
        Reward reward = new Reward(1, null, java.util.Arrays.asList(
                new ItemStack(org.bukkit.Material.DIAMOND), new ItemStack(org.bukkit.Material.GOLD_INGOT, 5)));
        when(rewards.get(1)).thenReturn(reward);
        when(inventory.getStorageContents()).thenReturn(new ItemStack[36]);
        questPlayer.setQuestId(9);

        service.giveReward(questPlayer, 1, player);
        storage.shutdown();

        verify(inventory).addItem(any(ItemStack[].class));
        assertTrue(questPlayer.getRewardIds().contains(1));
    }

    @Test
    void aRewardThatIsMissingOrNotYetUnlockedGivesNothing() {
        when(rewards.get(1)).thenReturn(new Reward(1, null, new ItemStack(org.bukkit.Material.DIAMOND)));
        questPlayer.setQuestId(8);

        service.giveReward(questPlayer, 1, player);
        service.giveReward(questPlayer, 7, player);

        verify(messages.getRewardNotAvailable(), Mockito.times(2)).send(player);
        assertTrue(questPlayer.getRewardIds().isEmpty());
        verify(inventory, never()).addItem(any(ItemStack.class));
        verifyNoInteractions(metrics);
    }

    @Test
    void severalRewardItemsNeedSpaceForEveryStack() {
        Reward reward = new Reward(1, null, java.util.Arrays.asList(
                new ItemStack(org.bukkit.Material.DIAMOND), new ItemStack(org.bukkit.Material.GOLD_INGOT)));
        when(rewards.get(1)).thenReturn(reward);
        ItemStack[] almostFull = new ItemStack[36];
        for (int i = 0; i < 35; i++) {
            almostFull[i] = mock(ItemStack.class);
            when(almostFull[i].getType()).thenReturn(org.bukkit.Material.STONE);
            when(almostFull[i].getAmount()).thenReturn(64);
        }
        when(inventory.getStorageContents()).thenReturn(almostFull);
        questPlayer.setQuestId(9);

        service.giveReward(questPlayer, 1, player);

        verify(messages.getNotEnoughSpace()).send(player);
        verify(inventory, never()).addItem(any(ItemStack[].class));
        assertTrue(questPlayer.getRewardIds().isEmpty());
    }

    @Test
    void givingItemsByCommandDoesNotRunRewardCommandsOrMarkItClaimed() {
        Reward reward = new Reward(1, null, new ItemStack(org.bukkit.Material.DIAMOND));
        reward.getCommands().add("eco give %player% 100");
        when(inventory.getStorageContents()).thenReturn(new ItemStack[36]);

        assertTrue(service.giveRewardItems(reward, player));

        verify(inventory).addItem(any(ItemStack.class));
        bukkit.verify(() -> Bukkit.dispatchCommand(any(ConsoleCommandSender.class), anyString()), never());
        assertTrue(questPlayer.getRewardIds().isEmpty());
        verifyNoInteractions(metrics);
    }

    @Test
    void aFullInventoryKeepsTheRewardClaimable() {
        when(rewards.get(1)).thenReturn(new Reward(1, null, new ItemStack(org.bukkit.Material.DIAMOND)));
        ItemStack[] full = new ItemStack[36];
        for (int i = 0; i < full.length; i++) {
            full[i] = mock(ItemStack.class);
            when(full[i].getType()).thenReturn(org.bukkit.Material.STONE);
            when(full[i].getAmount()).thenReturn(64);
        }
        when(inventory.getStorageContents()).thenReturn(full);
        questPlayer.setQuestId(9);

        service.giveReward(questPlayer, 1, player);

        verify(messages.getNotEnoughSpace()).send(player);
        assertTrue(questPlayer.getRewardIds().isEmpty());
        verifyNoInteractions(metrics);
    }

    @Test
    void pagesOpenByProgressOrByPermission() {
        questPlayer.setQuestId(41);

        assertTrue(service.canOpenPage(player, questPlayer, 1));
        assertTrue(service.canOpenPage(player, questPlayer, 2));
        assertFalse(service.canOpenPage(player, questPlayer, 3));
        assertFalse(service.canOpenPage(player, questPlayer, 0));

        when(player.hasPermission("quests.open.3")).thenReturn(true);
        assertTrue(service.canOpenPage(player, questPlayer, 3));
    }

    @Test
    void progressResetCancelsAnAlreadyQueuedRewardDelivery() {
        java.util.List<Runnable> deliveries = queueRewardDelivery();
        service.cache(questPlayer);
        resets.begin(ResetMode.PROGRESS);
        service.applyReset(ResetMode.PROGRESS, 3);
        resets.finish(true);

        deliveries.forEach(Runnable::run);

        verify(inventory, never()).addItem(any(ItemStack[].class));
        verify(metrics, never()).rewardCollected();
        assertTrue(questPlayer.getRewardIds().isEmpty());
    }

    @Test
    void skipsResetDoesNotLoseAnAlreadyEarnedRewardDelivery() {
        java.util.List<Runnable> deliveries = queueRewardDelivery();
        resets.begin(ResetMode.SKIPS);
        resets.finish(true);

        deliveries.forEach(Runnable::run);

        verify(inventory).addItem(any(ItemStack.class));
        verify(metrics).rewardCollected();
        assertTrue(questPlayer.getRewardIds().contains(1));
    }

    private java.util.List<Runnable> queueRewardDelivery() {
        java.util.List<Runnable> pending = new java.util.ArrayList<>();
        when(Bukkit.getScheduler().runTask(any(JavaPlugin.class), any(Runnable.class))).thenAnswer(call -> {
            pending.add(call.getArgument(1));
            return null;
        });
        when(rewards.get(1)).thenReturn(new Reward(1, null, new ItemStack(org.bukkit.Material.DIAMOND)));
        when(inventory.getStorageContents()).thenReturn(new ItemStack[36]);
        questPlayer.setQuestId(9);
        service.giveReward(questPlayer, 1, player);
        return pending;
    }

    @Test
    void aLateSnapshotCannotRestoreProgressFromBeforeTheReset() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        java.util.List<Runnable> pending = new java.util.ArrayList<>();
        StorageExecutor delayed = new StorageExecutor(plugin) {
            @Override
            public <T> java.util.concurrent.CompletableFuture<T> supply(java.util.function.Supplier<T> task) {
                java.util.concurrent.CompletableFuture<T> future = new java.util.concurrent.CompletableFuture<>();
                pending.add(() -> future.complete(task.get()));
                return future;
            }
        };
        try {
            PlayerService delayedService = new PlayerService(plugin, players, playerRewards, delayed, quests, rewards,
                    config, messages, metrics, resets);
            questPlayer.setQuestId(99);
            delayedService.save(questPlayer);
            resets.begin(ResetMode.PROGRESS);
            resets.finish(true);

            pending.forEach(Runnable::run);

            verify(players, never()).save(any(PlayerRecord.class));
            verify(playerRewards, never()).deleteByPlayer(anyString());
        } finally {
            delayed.shutdown();
        }
    }

    @Test
    void reloadingTheSaveIntervalReplacesTheTimerAndKeepsPlayerProgress() {
        BukkitTask oldTimer = mock(BukkitTask.class);
        BukkitTask newTimer = mock(BukkitTask.class);
        when(Bukkit.getScheduler().runTaskTimerAsynchronously(any(JavaPlugin.class), any(Runnable.class),
                Mockito.anyLong(), Mockito.anyLong())).thenReturn(oldTimer, newTimer);
        service.cache(questPlayer);
        questPlayer.setProgress(12);
        service.rescheduleSaving();
        config.setSaveIntervalSeconds(7);

        service.rescheduleSaving();

        verify(oldTimer).cancel();
        verify(newTimer, never()).cancel();
        verify(Bukkit.getScheduler()).runTaskTimerAsynchronously(any(JavaPlugin.class), any(Runnable.class),
                Mockito.eq(140L), Mockito.eq(140L));
        assertEquals(12, service.cached(uuid).getProgress());
        assertEquals(3, service.cached(uuid).getSkips());
    }
}

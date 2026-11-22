// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.service;

import com.ontadev.libs.metrics.PluginMetrics;
import com.ontadev.quests.config.MessagesConfig;
import com.ontadev.quests.config.QuestsConfig;
import com.ontadev.quests.metrics.QuestMetrics;
import com.ontadev.quests.model.Quest;
import com.ontadev.quests.model.QuestPlayer;
import com.ontadev.quests.model.QuestType;
import com.ontadev.quests.storage.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ResetServiceTest {
    private final UUID online = UUID.randomUUID();
    private final UUID offline = UUID.randomUUID();
    private Jdbi database;
    private StorageExecutor storage;
    private ResetState state;
    private PlayerService players;
    private QuestService quests;
    private RewardService rewards;
    private ResetService resets;
    private QuestMetrics metrics;
    private final Map<String, Callable<Integer>> charts = new HashMap<>();
    private MockedStatic<Bukkit> bukkit;

    @BeforeEach
    void setUp() throws Exception {
        Class.forName("org.h2.Driver");
        database = Jdbi.create("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        database.useHandle(h -> {
            h.execute("CREATE TABLE quest_players(uuid VARCHAR(36) PRIMARY KEY, quest_id INT, progress INT, skips INT)");
            h.execute("CREATE TABLE quest_player_rewards(player_uuid VARCHAR(36), reward_id INT)");
            h.execute("CREATE TABLE quests(id INT PRIMARY KEY)");
            h.execute("CREATE TABLE quest_rewards(id INT PRIMARY KEY)");
            h.execute("CREATE TABLE quest_reward_commands(reward_id INT, command VARCHAR(100))");
            h.execute("INSERT INTO quest_players VALUES (?, 5, 12, 2)", online.toString());
            h.execute("INSERT INTO quest_players VALUES (?, 41, 9, 7)", offline.toString());
            h.execute("INSERT INTO quest_player_rewards VALUES (?, 1)", online.toString());
            h.execute("INSERT INTO quest_player_rewards VALUES (?, 2)", offline.toString());
            h.execute("INSERT INTO quests VALUES (1)");
            h.execute("INSERT INTO quest_rewards VALUES (1)");
            h.execute("INSERT INTO quest_reward_commands VALUES (1, 'eco give %player% 100')");
        });
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(mock(BukkitScheduler.class));
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("reset-test"));
        storage = new StorageExecutor(plugin);
        state = new ResetState();

        PlayerRepository playerRepository = mock(PlayerRepository.class);
        when(playerRepository.findByUuid(anyString())).thenAnswer(call -> database.withHandle(h ->
                h.createQuery("SELECT * FROM quest_players WHERE uuid = :id").bind("id", call.<String>getArgument(0))
                        .map((rs, ctx) -> {
                            PlayerRecord record = new PlayerRecord();
                            record.setUuid(rs.getString("uuid"));
                            record.setQuestId(rs.getInt("quest_id"));
                            record.setProgress(rs.getInt("progress"));
                            record.setSkips(rs.getInt("skips"));
                            return record;
                        }).findOne()));
        doAnswer(call -> {
            PlayerRecord record = call.getArgument(0);
            database.useHandle(h -> h.execute("MERGE INTO quest_players KEY(uuid) VALUES (?, ?, ?, ?)",
                    record.getUuid(), record.getQuestId(), record.getProgress(), record.getSkips()));
            return null;
        }).when(playerRepository).save(any(PlayerRecord.class));
        PlayerRewardRepository claimed = mock(PlayerRewardRepository.class);
        when(claimed.findByPlayer(anyString())).thenAnswer(call -> database.withHandle(h ->
                h.createQuery("SELECT reward_id FROM quest_player_rewards WHERE player_uuid = :id")
                        .bind("id", call.<String>getArgument(0)).map((rs, ctx) ->
                                new PlayerRewardRecord(call.getArgument(0), rs.getInt("reward_id"))).list()));
        doAnswer(call -> {
            database.useHandle(h -> h.execute("DELETE FROM quest_player_rewards WHERE player_uuid = ?", call.<String>getArgument(0)));
            return null;
        }).when(claimed).deleteByPlayer(anyString());
        doAnswer(call -> {
            PlayerRewardRecord record = call.getArgument(0);
            database.useHandle(h -> h.execute("INSERT INTO quest_player_rewards VALUES (?, ?)", record.getPlayerUuid(), record.getRewardId()));
            return null;
        }).when(claimed).save(any(PlayerRewardRecord.class));

        QuestRepository questRepository = mock(QuestRepository.class);
        QuestRecord quest = new QuestRecord();
        quest.setId(1);
        quest.setQuestType("BREAK_BLOCKS");
        when(questRepository.findAllOrdered()).thenReturn(Collections.singletonList(quest));
        quests = new QuestService(plugin, questRepository, storage);
        RewardRepository rewardRepository = mock(RewardRepository.class);
        RewardRecord reward = new RewardRecord();
        reward.setId(1);
        when(rewardRepository.findAllOrdered()).thenReturn(Collections.singletonList(reward));
        rewards = new RewardService(plugin, rewardRepository, mock(RewardCommandRepository.class), storage);
        QuestsConfig config = new QuestsConfig();
        PluginMetrics bStats = mock(PluginMetrics.class);
        when(bStats.addSingleLineChart(anyString(), any())).thenAnswer(call -> {
            charts.put(call.getArgument(0), call.getArgument(1));
            return bStats;
        });
        metrics = new QuestMetrics(bStats);
        players = new PlayerService(plugin, playerRepository, claimed, storage, quests, rewards, config,
                mock(MessagesConfig.class, RETURNS_DEEP_STUBS), metrics, state);
        players.loadOrCreate(online);
        resets = new ResetService(database, storage, players, quests, rewards, config, state);
    }

    @AfterEach
    void tearDown() {
        if (storage != null) storage.shutdown();
        if (bukkit != null) bukkit.close();
        if (database != null) database.useHandle(h -> h.execute("DROP ALL OBJECTS"));
    }

    private int value(UUID uuid, String field) {
        return database.withHandle(h -> h.createQuery("SELECT " + field + " FROM quest_players WHERE uuid = :id")
                .bind("id", uuid.toString()).mapTo(Integer.class).one());
    }

    private int count(String table) {
        return database.withHandle(h -> h.createQuery("SELECT COUNT(*) FROM " + table).mapTo(Integer.class).one());
    }

    @Test
    void progressResetIncludesOfflinePlayersAndPreservesSkipsAndDefinitions() {
        resets.reset(ResetMode.PROGRESS, 3).join();

        for (UUID uuid : new UUID[]{online, offline}) {
            assertEquals(1, value(uuid, "quest_id"));
            assertEquals(0, value(uuid, "progress"));
        }
        assertEquals(2, value(online, "skips"));
        assertEquals(7, value(offline, "skips"));
        assertEquals(0, count("quest_player_rewards"));
        QuestPlayer cached = players.cached(online);
        assertEquals(1, cached.getQuestId());
        assertEquals(0, cached.getProgress());
        assertTrue(cached.getRewardIds().isEmpty());
        players.save(cached).join();
        assertEquals(0, count("quest_player_rewards"));
        assertEquals(1, quests.all().size());
        assertEquals(1, count("quest_rewards"));
        assertFalse(state.isResetting());
    }

    @Test
    void skipsResetPreservesUnsavedProgressAndClaimedRewards() {
        QuestPlayer cached = players.cached(online);
        players.addProgress(cached, new Quest(5, "Камень", null, 100, null, QuestType.BREAK_BLOCKS, null), 3);
        assertEquals(12, value(online, "progress"), "прогресс ещё не сброшен фоновым сохранением");

        resets.reset(ResetMode.SKIPS, 0).join();

        assertEquals(0, value(online, "skips"));
        assertEquals(0, value(offline, "skips"));
        assertEquals(0, cached.getSkips());
        assertEquals(15, cached.getProgress());
        assertEquals(15, value(online, "progress"));
        assertEquals(41, value(offline, "quest_id"));
        assertEquals(2, count("quest_player_rewards"));
        assertEquals(1, count("quests"));
    }

    @Test
    void fullResetClearsDefinitionsAndCachesAndRestoresSkips() {
        resets.reset(ResetMode.ALL, resets.defaultSkips()).join();

        for (String table : new String[]{"quests", "quest_rewards", "quest_reward_commands", "quest_player_rewards"})
            assertEquals(0, count(table), table);
        assertTrue(quests.all().isEmpty());
        assertTrue(rewards.all().isEmpty());
        assertEquals(1, quests.nextFreeId());
        assertEquals(1, rewards.nextFreeId());
        assertEquals(3, value(online, "skips"));
        assertEquals(3, value(offline, "skips"));
        assertEquals(1, players.cached(online).getQuestId());
        players.save(players.cached(online)).join();
        assertEquals(1, value(online, "quest_id"));
    }

    @Test
    void aDatabaseErrorRollsBackAllResetStatementsAndKeepsCaches() {
        database.useHandle(h -> h.execute("DROP TABLE quests"));

        assertThrows(CompletionException.class, () -> resets.reset(ResetMode.ALL, 3).join());

        assertEquals(5, value(online, "quest_id"));
        assertEquals(2, value(online, "skips"));
        assertEquals(2, count("quest_player_rewards"));
        assertEquals(1, count("quest_rewards"));
        assertEquals(1, count("quest_reward_commands"));
        assertEquals(5, players.cached(online).getQuestId());
        assertEquals(1, quests.all().size());
        assertEquals(1, rewards.all().size());
        assertEquals(0, state.generation());
        assertFalse(state.isResetting());
    }

    @Test
    void overlappingResetsAreRejectedAndTheBarrierIsReleased() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        storage.run(() -> {
            try {
                if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("test timed out");
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(error);
            }
        });
        CompletableFuture<Void> first = resets.reset(ResetMode.PROGRESS, 3);
        try {
            assertTrue(players.isResetting());
            assertThrows(CompletionException.class, () -> resets.reset(ResetMode.ALL, 3).join());
        } finally {
            release.countDown();
        }
        first.get(5, TimeUnit.SECONDS);
        assertFalse(players.isResetting());
    }

    @Test
    void everyResetModePreservesAllAccumulatedMetrics() throws Exception {
        for (ResetMode mode : ResetMode.values()) {
            metrics.questCompleted();
            metrics.rewardCollected();
            metrics.questSkipped();
            resets.reset(mode, 3).join();
        }

        assertEquals(3, charts.get("completed_quests").call());
        assertEquals(3, charts.get("collected_rewards").call());
        assertEquals(3, charts.get("skipped_quests").call());
    }
}

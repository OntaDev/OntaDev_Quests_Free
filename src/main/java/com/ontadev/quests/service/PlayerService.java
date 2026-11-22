// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.service;

import com.ontadev.libs.ioc.annotation.lifecycle.Shutdown;
import com.ontadev.libs.ioc.annotation.stereotype.Service;
import com.ontadev.quests.QuestPermissions;
import com.ontadev.quests.config.MessagesConfig;
import com.ontadev.quests.config.QuestsConfig;
import com.ontadev.quests.metrics.QuestMetrics;
import com.ontadev.quests.model.Quest;
import com.ontadev.quests.model.QuestPlayer;
import com.ontadev.quests.model.QuestRules;
import com.ontadev.quests.model.Reward;
import com.ontadev.quests.util.InventorySpace;
import com.ontadev.quests.storage.PlayerRecord;
import com.ontadev.quests.storage.PlayerRepository;
import com.ontadev.quests.storage.PlayerRewardRecord;
import com.ontadev.quests.storage.PlayerRewardRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class PlayerService {

    private final JavaPlugin plugin;
    private final PlayerRepository players;
    private final PlayerRewardRepository playerRewards;
    private final StorageExecutor storage;
    private final QuestService questService;
    private final RewardService rewardService;
    private final QuestsConfig config;
    private final MessagesConfig messages;
    private final QuestMetrics metrics;
    private final ResetState resets;
    private BukkitTask saveTask;

    private final Map<UUID, QuestPlayer> cache = new ConcurrentHashMap<>();
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();

    public PlayerService(JavaPlugin plugin, PlayerRepository players, PlayerRewardRepository playerRewards,
                         StorageExecutor storage, QuestService questService, RewardService rewardService,
                         QuestsConfig config, MessagesConfig messages, QuestMetrics metrics, ResetState resets) {
        this.plugin = plugin;
        this.players = players;
        this.playerRewards = playerRewards;
        this.storage = storage;
        this.questService = questService;
        this.rewardService = rewardService;
        this.config = config;
        this.messages = messages;
        this.metrics = metrics;
        this.resets = resets;

        rescheduleSaving();
    }

    public void rescheduleSaving() {
        long period = Math.max(1, config.getSaveIntervalSeconds()) * 20L;
        BukkitTask next = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::flushDirty, period, period);
        if (saveTask != null) saveTask.cancel();
        saveTask = next;
    }

    public QuestPlayer cached(UUID uuid) {
        return cache.get(uuid);
    }

    public void cache(QuestPlayer player) {
        cache.put(player.getUuid(), player);
    }

    public void unload(UUID uuid) {
        cache.remove(uuid);
        dirty.remove(uuid);
    }

    /** Читает игрока из БД, а нового создаёт и сохраняет. Блокирует поток: вызывать не с основного. */
    @SuppressWarnings("UnusedReturnValue")
    public QuestPlayer loadOrCreate(UUID uuid) {
        return storage.supply(() -> {
            QuestPlayer player = read(uuid);
            if (player == null) {
                player = new QuestPlayer(uuid, config.getSkipsPerPlayer());
                persist(player);
            }
            cache(player);
            return player;
        }).join();
    }

    public CompletableFuture<Optional<QuestPlayer>> find(UUID uuid) {
        QuestPlayer cached = cache.get(uuid);
        if (cached != null) return CompletableFuture.completedFuture(Optional.of(cached));
        return storage.supply(() -> Optional.ofNullable(read(uuid)));
    }

    public CompletableFuture<Void> save(QuestPlayer player) {
        if (isResetting()) return CompletableFuture.completedFuture(null);
        dirty.remove(player.getUuid());
        Snapshot snapshot = new Snapshot(player, resets.generation());
        return storage.run(() -> write(snapshot));
    }

    public void markDirty(QuestPlayer player) {
        dirty.add(player.getUuid());
    }

    private void flushDirty() {
        if (isResetting()) return;
        for (UUID uuid : new ArrayList<>(dirty)) {
            QuestPlayer player = cache.get(uuid);
            dirty.remove(uuid);
            if (player != null) {
                Snapshot snapshot = new Snapshot(player, resets.generation());
                storage.run(() -> write(snapshot));
            }
        }
    }

    public boolean isResetting() {
        return resets.isResetting();
    }

    public long generation() {
        return resets.generation();
    }

    CompletableFuture<Void> flushBeforeReset() {
        List<Snapshot> snapshots = new ArrayList<>();
        for (QuestPlayer player : cache.values()) snapshots.add(new Snapshot(player, resets.generation()));
        return storage.run(() -> snapshots.forEach(this::write));
    }

    void applyReset(ResetMode mode, int skips) {
        for (QuestPlayer player : cache.values()) {
            if (mode.resetsProgress()) {
                player.setQuestId(1);
                player.setProgress(0);
                player.getRewardIds().clear();
            }
            if (mode.resetsSkips()) player.setSkips(skips);
        }
        dirty.clear();
    }

    public boolean canOpenPage(Player player, QuestPlayer questPlayer, int page) {
        if (page < 1) return false;
        return page <= QuestRules.pageOfQuest(questPlayer.getQuestId())
                || player.hasPermission("quests.open." + page)
                || player.hasPermission("quests.open.*");
    }

    public boolean skipQuest(Player player, QuestPlayer questPlayer) {
        if (isResetting()) return false;
        if (!player.hasPermission(QuestPermissions.SKIP)) {
            messages.getQuestSkipNotAvailable().send(player);
            return false;
        }
        if (questPlayer.getSkips() <= 0) {
            messages.getQuestSkipsEnded().send(player);
            return false;
        }
        if (questService.get(questPlayer.getQuestId()) == null) {
            messages.getQuestsEnded().send(player);
            return false;
        }

        questPlayer.setSkips(questPlayer.getSkips() - 1);
        advance(questPlayer);
        save(questPlayer).thenRun(() -> {
            metrics.questSkipped();
            messages.getQuestSkipped().send(player);
        });
        return true;
    }

    public void addProgress(QuestPlayer player, Quest quest, int amount) {
        if (isResetting()) return;
        if (amount <= 0 || player.getQuestId() != quest.getId()) return;

        int progress = player.getProgress() + amount;
        if (progress < quest.getGoal()) {
            player.setProgress(progress);
            markDirty(player);
            return;
        }

        advance(player);
        messages.getQuestCompleted().send(player.getUuid());
        save(player).thenRun(metrics::questCompleted);
    }

    private void advance(QuestPlayer player) {
        player.setQuestId(player.getQuestId() + 1);
        player.setProgress(0);
    }

    public void giveReward(QuestPlayer questPlayer, int rewardId, Player player) {
        if (isResetting()) return;
        Reward reward = rewardService.get(rewardId);
        if (reward == null || !QuestRules.isRewardUnlocked(rewardId, questPlayer.getQuestId())) {
            messages.getRewardNotAvailable().send(player);
            return;
        }
        if (questPlayer.getRewardIds().contains(rewardId)) {
            messages.getRewardAlreadyGot().send(player);
            return;
        }

        List<ItemStack> delivery = deliveryItems(reward);

        PlayerInventory inventory = player.getInventory();
        if (!InventorySpace.fits(inventory.getStorageContents(), delivery)) {
            messages.getNotEnoughSpace().send(player);
            return;
        }

        questPlayer.getRewardIds().add(rewardId);
        long generation = resets.progressGeneration();
        save(questPlayer).thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (!resets.canDeliverReward(generation)) return;
            for (String command : reward.getCommands()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
            }
            inventory.addItem(delivery.toArray(new ItemStack[0]));
            metrics.rewardCollected();
            messages.getRewardCollected().send(player);
        }));
    }

    private List<ItemStack> deliveryItems(Reward reward) {
        return reward.getItems().stream().map(ItemStack::clone).collect(Collectors.toList());
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean giveRewardItems(Reward reward, Player player) {
        if (isResetting()) return false;
        List<ItemStack> delivery = deliveryItems(reward);
        if (!InventorySpace.fits(player.getInventory().getStorageContents(), delivery)) return false;

        player.getInventory().addItem(delivery.toArray(new ItemStack[0]));
        return true;
    }

    @Shutdown
    public void shutdown() {
        if (saveTask != null) saveTask.cancel();
        List<CompletableFuture<Void>> pending = new ArrayList<>();
        for (QuestPlayer player : new ArrayList<>(cache.values())) {
            Snapshot snapshot = new Snapshot(player, resets.generation());
            pending.add(storage.run(() -> write(snapshot)));
        }
        dirty.clear();
        CompletableFuture.allOf(pending.toArray(new CompletableFuture[0])).join();
    }

    private QuestPlayer read(UUID uuid) {
        Optional<PlayerRecord> record = players.findByUuid(uuid.toString());
        if (record.isEmpty()) return null;

        QuestPlayer player = new QuestPlayer(uuid, record.get().getSkips() == null ? 0 : record.get().getSkips());
        player.setProgress(record.get().getProgress() == null ? 0 : record.get().getProgress());
        player.setQuestId(record.get().getQuestId() == null ? 1 : record.get().getQuestId());

        Set<Integer> rewardIds = new HashSet<>();
        for (PlayerRewardRecord reward : playerRewards.findByPlayer(uuid.toString())) {
            rewardIds.add(reward.getRewardId());
        }
        player.setRewardIds(rewardIds);
        return player;
    }

    private void persist(QuestPlayer player) {
        write(new Snapshot(player, resets.generation()));
    }

    private void write(Snapshot snapshot) {
        if (snapshot.generation != resets.generation()) return;
        PlayerRecord record = new PlayerRecord();
        record.setUuid(snapshot.uuid.toString());
        record.setSkips(snapshot.skips);
        record.setProgress(snapshot.progress);
        record.setQuestId(snapshot.questId);
        players.save(record);

        playerRewards.deleteByPlayer(snapshot.uuid.toString());
        for (Integer rewardId : snapshot.rewardIds) {
            playerRewards.save(new PlayerRewardRecord(snapshot.uuid.toString(), rewardId));
        }
    }

    private static final class Snapshot {
        private final long generation;
        private final UUID uuid;
        private final int skips;
        private final int progress;
        private final int questId;
        private final List<Integer> rewardIds;

        private Snapshot(QuestPlayer player, long generation) {
            this.generation = generation;
            this.uuid = player.getUuid();
            this.skips = player.getSkips();
            this.progress = player.getProgress();
            this.questId = player.getQuestId();
            this.rewardIds = new ArrayList<>(player.getRewardIds());
        }
    }
}

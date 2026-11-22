// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.service;

import com.ontadev.libs.ioc.annotation.stereotype.Service;
import com.ontadev.quests.model.QuestRules;
import com.ontadev.quests.model.Reward;
import com.ontadev.quests.storage.RewardCommandRecord;
import com.ontadev.quests.storage.RewardCommandRepository;
import com.ontadev.quests.storage.RewardRecord;
import com.ontadev.quests.storage.RewardRepository;
import com.ontadev.quests.util.ItemCodec;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class RewardService {

    private final RewardRepository rewards;
    private final RewardCommandRepository commands;
    private final StorageExecutor storage;
    private final Map<Integer, Reward> cache = new ConcurrentHashMap<>();

    public RewardService(JavaPlugin plugin, RewardRepository rewards, RewardCommandRepository commands, StorageExecutor storage) {
        this.rewards = rewards;
        this.commands = commands;
        this.storage = storage;

        Map<Integer, List<String>> commandsByReward = commands.findAllOrdered().stream()
                .collect(Collectors.groupingBy(RewardCommandRecord::getRewardId,
                        Collectors.mapping(RewardCommandRecord::getCommand, Collectors.toList())));

        for (RewardRecord record : rewards.findAllOrdered()) {
            try {
                Reward reward = new Reward(record.getId(), ItemCodec.decode(record.getIcon()), ItemCodec.decodeAll(record.getRewardItem()));
                reward.setCommands(commandsByReward.getOrDefault(record.getId(), new ArrayList<>()));
                cache.put(reward.getId(), reward);
            } catch (RuntimeException e) {
                plugin.getLogger().warning("Награда " + record.getId() + " повреждена и пропущена: " + e.getMessage());
            }
        }
    }

    public Reward get(int id) {
        return cache.get(id);
    }

    public Map<Integer, Reward> all() {
        return new HashMap<>(cache);
    }

    public Map<Integer, Reward> page(int page) {
        Map<Integer, Reward> result = new HashMap<>();
        if (page < 1) return result;

        int start = QuestRules.firstRewardOfPage(page);
        for (int id = start; id < start + QuestRules.REWARDS_PER_PAGE; id++) {
            Reward reward = cache.get(id);
            if (reward != null) result.put(id, reward);
        }
        return result;
    }

    public int nextFreeId() {
        int id = 1;
        while (cache.containsKey(id)) id++;
        return id;
    }

    public CompletableFuture<Reward> save(Reward reward) {
        if (reward.getId() == Reward.NO_ID) {
            reward.setId(nextFreeId());
        }
        cache.put(reward.getId(), reward);
        return persist(reward).thenApply(ignored -> reward);
    }

    public CompletableFuture<Void> addCommand(Reward reward, String command) {
        reward.getCommands().add(command);
        return storage.run(() -> commands.save(new RewardCommandRecord(reward.getId(), command)));
    }

    public CompletableFuture<Void> clearCommands(Reward reward) {
        reward.getCommands().clear();
        return storage.run(() -> commands.deleteByReward(reward.getId()));
    }

    public CompletableFuture<Void> delete(int id) {
        cache.remove(id);
        return storage.run(() -> {
            commands.deleteByReward(id);
            rewards.deleteById(id);
        });
    }

    void clearCache() {
        cache.clear();
    }

    private CompletableFuture<Void> persist(Reward reward) {
        RewardRecord record = new RewardRecord();
        record.setId(reward.getId());
        record.setIcon(ItemCodec.encode(reward.getIcon()));
        record.setRewardItem(ItemCodec.encodeAll(reward.getItems()));
        List<String> snapshot = new ArrayList<>(reward.getCommands());

        return storage.run(() -> {
            rewards.save(record);
            commands.deleteByReward(record.getId());
            for (String command : snapshot) {
                commands.save(new RewardCommandRecord(record.getId(), command));
            }
        });
    }
}

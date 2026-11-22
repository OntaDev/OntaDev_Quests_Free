// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.service;

import com.ontadev.libs.ioc.annotation.stereotype.Service;
import com.ontadev.quests.model.Quest;
import com.ontadev.quests.model.QuestRules;
import com.ontadev.quests.model.QuestType;
import com.ontadev.quests.storage.QuestRecord;
import com.ontadev.quests.storage.QuestRepository;
import com.ontadev.quests.util.ItemCodec;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class QuestService {

    private final QuestRepository repository;
    private final StorageExecutor storage;
    private final Map<Integer, Quest> quests = new ConcurrentHashMap<>();

    public QuestService(JavaPlugin plugin, QuestRepository repository, StorageExecutor storage) {
        this.repository = repository;
        this.storage = storage;

        for (QuestRecord record : repository.findAllOrdered()) {
            try {
                Quest quest = fromRecord(record);
                quests.put(quest.getId(), quest);
            } catch (RuntimeException e) {
                plugin.getLogger().warning("Квест " + record.getId() + " повреждён и пропущен: " + e.getMessage());
            }
        }
    }

    public Quest get(int id) {
        return quests.get(id);
    }

    public Map<Integer, Quest> all() {
        return new HashMap<>(quests);
    }

    public Map<Integer, Quest> page(int page) {
        Map<Integer, Quest> result = new HashMap<>();
        if (page < 1) return result;

        int start = QuestRules.firstQuestOfPage(page);
        for (int id = start; id < start + QuestRules.QUESTS_PER_PAGE; id++) {
            Quest quest = quests.get(id);
            if (quest != null) result.put(id, quest);
        }
        return result;
    }

    public int nextFreeId() {
        int id = 1;
        while (quests.containsKey(id)) id++;
        return id;
    }

    public int lastId() {
        return quests.keySet().stream().max(Integer::compareTo).orElse(0);
    }

    public CompletableFuture<Quest> save(Quest quest) {
        if (quest.getId() == Quest.NO_ID) {
            quest.setId(nextFreeId());
        }
        quests.put(quest.getId(), quest);

        QuestRecord record = toRecord(quest);
        return storage.run(() -> repository.save(record)).thenApply(ignored -> quest);
    }

    public CompletableFuture<Quest> changeIcon(int id, ItemStack icon) {
        Quest quest = quests.get(id);
        if (quest == null) return CompletableFuture.completedFuture(null);

        ItemStack single = icon.clone();
        single.setAmount(1);
        return save(quest.withIcon(single));
    }

    public CompletableFuture<Void> delete(int id) {
        quests.remove(id);
        return storage.run(() -> repository.deleteById(id));
    }

    void clearCache() {
        quests.clear();
    }

    private QuestRecord toRecord(Quest quest) {
        QuestRecord record = new QuestRecord();
        record.setId(quest.getId());
        record.setTitle(quest.getName());
        record.setQuestType(quest.getType().name());
        record.setGoal(quest.getGoal());
        record.setTarget(ItemCodec.encode(quest.getTarget()));
        record.setTargetEntity(quest.getTargetEntity() == null ? null : quest.getTargetEntity().name());
        record.setIcon(ItemCodec.encode(quest.getIcon()));
        return record;
    }

    private Quest fromRecord(QuestRecord record) {
        QuestType type = QuestType.from(record.getQuestType());
        if (type == null) throw new IllegalStateException("неизвестный тип " + record.getQuestType());

        EntityType entity = null;
        if (record.getTargetEntity() != null) {
            try {
                entity = EntityType.valueOf(record.getTargetEntity());
            } catch (IllegalArgumentException ignored) {
            }
        }

        return new Quest(
                record.getId(),
                record.getTitle(),
                ItemCodec.decode(record.getTarget()),
                record.getGoal() == null ? 1 : record.getGoal(),
                ItemCodec.decode(record.getIcon()),
                type,
                entity
        );
    }
}

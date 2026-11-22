// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class QuestPlayer {

    private final UUID uuid;
    private int skips;
    private int progress = 0;
    private int questId = 1;
    private final Set<Integer> rewardIds = ConcurrentHashMap.newKeySet();

    public QuestPlayer(UUID uuid, int skips) {
        this.uuid = uuid;
        this.skips = skips;
    }

    public void setRewardIds(Set<Integer> ids) {
        rewardIds.clear();
        rewardIds.addAll(ids);
    }

    @SuppressWarnings("unused")
    public Set<Integer> rewardIdsSnapshot() {
        return Set.copyOf(rewardIds);
    }
}

// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.service;

import com.ontadev.libs.ioc.annotation.stereotype.Service;
import com.ontadev.quests.model.QuestRules;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PageService {

    private final QuestService quests;
    private final RewardService rewards;

    public PageService(QuestService quests, RewardService rewards) {
        this.quests = quests;
        this.rewards = rewards;
    }

    public int lastPage() {
        int lastId = quests.lastId();
        return lastId == 0 ? 0 : QuestRules.pageOfQuest(lastId);
    }

    public int totalPages() {
        return Math.max(1, lastPage());
    }

    public PageInfo info(int page) {
        return new PageInfo(page, quests.page(page).size(), missingQuestIds(page), missingRewardIds(page));
    }

    public List<Integer> missingQuestIds(int page) {
        List<Integer> missing = new ArrayList<>();
        Map<Integer, ?> existing = quests.page(page);
        int start = QuestRules.firstQuestOfPage(page);
        for (int id = start; id < start + QuestRules.QUESTS_PER_PAGE; id++) {
            if (!existing.containsKey(id)) missing.add(id);
        }
        return missing;
    }

    public List<Integer> missingRewardIds(int page) {
        List<Integer> missing = new ArrayList<>();
        Map<Integer, ?> existing = rewards.page(page);
        int start = QuestRules.firstRewardOfPage(page);
        for (int id = start; id < start + QuestRules.REWARDS_PER_PAGE; id++) {
            if (!existing.containsKey(id)) missing.add(id);
        }
        return missing;
    }

    @Getter
    public static final class PageInfo {
        private final int page;
        private final int quests;
        private final List<Integer> missingQuests;
        private final List<Integer> missingRewards;

        PageInfo(int page, int quests, List<Integer> missingQuests, List<Integer> missingRewards) {
            this.page = page;
            this.quests = quests;
            this.missingQuests = missingQuests;
            this.missingRewards = missingRewards;
        }

        public int getRewards() {
            return QuestRules.REWARDS_PER_PAGE - missingRewards.size();
        }
    }
}

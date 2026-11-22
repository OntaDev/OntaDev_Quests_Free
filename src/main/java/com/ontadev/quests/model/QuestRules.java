// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.model;

public final class QuestRules {

    public static final int QUESTS_PER_PAGE = 40;
    public static final int QUESTS_PER_REWARD = 8;
    public static final int REWARDS_PER_PAGE = QUESTS_PER_PAGE / QUESTS_PER_REWARD;

    private QuestRules() {
    }

    public static int pageOfQuest(int questId) {
        return 1 + (Math.max(questId, 1) - 1) / QUESTS_PER_PAGE;
    }

    public static int rewardOfQuest(int questId) {
        return 1 + (Math.max(questId, 1) - 1) / QUESTS_PER_REWARD;
    }

    public static boolean isRewardUnlocked(int rewardId, int currentQuestId) {
        return currentQuestId > rewardId * QUESTS_PER_REWARD;
    }

    public static int firstQuestOfPage(int page) {
        return (page - 1) * QUESTS_PER_PAGE + 1;
    }

    public static int firstRewardOfPage(int page) {
        return (page - 1) * REWARDS_PER_PAGE + 1;
    }
}

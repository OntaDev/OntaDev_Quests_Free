// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestRulesTest {

    @Test
    void questsFallOntoPagesOfForty() {
        assertEquals(1, QuestRules.pageOfQuest(1));
        assertEquals(1, QuestRules.pageOfQuest(40));
        assertEquals(2, QuestRules.pageOfQuest(41));
        assertEquals(1, QuestRules.pageOfQuest(0), "некорректный id не даёт нулевую страницу");
    }

    @Test
    void aRewardOpensAfterEightQuestsPerRewardNumber() {
        assertFalse(QuestRules.isRewardUnlocked(1, 8));
        assertTrue(QuestRules.isRewardUnlocked(1, 9));
        assertFalse(QuestRules.isRewardUnlocked(5, 40));
        assertTrue(QuestRules.isRewardUnlocked(5, 41));
    }

    @Test
    void pagesStartAtTheRightIds() {
        assertEquals(1, QuestRules.firstQuestOfPage(1));
        assertEquals(41, QuestRules.firstQuestOfPage(2));
        assertEquals(1, QuestRules.firstRewardOfPage(1));
        assertEquals(6, QuestRules.firstRewardOfPage(2));
        assertEquals(5, QuestRules.REWARDS_PER_PAGE);
    }

    @Test
    void questTypesAreFoundByTranslationOrName() {
        assertEquals(QuestType.KILL_MOBS, QuestType.from("убить"));
        assertEquals(QuestType.BRING_ITEMS, QuestType.from("BRING_ITEMS"));
        assertEquals(QuestType.CRAFT_ITEMS, QuestType.from("craft_items"));
        assertNull(QuestType.from("что-то"));
        assertNull(QuestType.from(null));
    }

    @Test
    void everyQuestBelongsToTheRewardAtTheEndOfItsEightQuestRow() {
        assertEquals(1, QuestRules.rewardOfQuest(1));
        assertEquals(1, QuestRules.rewardOfQuest(8));
        assertEquals(2, QuestRules.rewardOfQuest(9));
        assertEquals(5, QuestRules.rewardOfQuest(40));
        assertEquals(6, QuestRules.rewardOfQuest(41));
    }
}

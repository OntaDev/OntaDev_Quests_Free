// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.service;

import com.ontadev.quests.model.Quest;
import com.ontadev.quests.model.Reward;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PageServiceTest {

    private final Map<Integer, Quest> quests = new HashMap<>();
    private final Map<Integer, Reward> rewards = new HashMap<>();
    private PageService service;

    @BeforeEach
    void setUp() {
        QuestService questService = mock(QuestService.class);
        RewardService rewardService = mock(RewardService.class);

        when(questService.lastId()).thenAnswer(i -> quests.keySet().stream().mapToInt(Integer::intValue).max().orElse(0));
        when(questService.page(anyInt())).thenAnswer(i -> {
            int page = i.getArgument(0);
            return quests.entrySet().stream()
                    .filter(e -> (e.getKey() - 1) / 40 + 1 == page)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        });
        when(rewardService.page(anyInt())).thenAnswer(i -> {
            int page = i.getArgument(0);
            return rewards.entrySet().stream()
                    .filter(e -> (e.getKey() - 1) / 5 + 1 == page)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        });
        service = new PageService(questService, rewardService);
    }

    private void addQuests(int from, int to) {
        for (int id = from; id <= to; id++) quests.put(id, mock(Quest.class));
    }

    private void addRewards(int from, int to) {
        for (int id = from; id <= to; id++) rewards.put(id, mock(Reward.class));
    }

    @Test
    void emptyServerStillShowsOnePage() {
        assertEquals(0, service.lastPage());
        assertEquals(1, service.totalPages());
    }

    @Test
    void lastPageIsThePageOfTheHighestQuestId() {
        addQuests(1, 41);
        assertEquals(2, service.lastPage());
    }

    @Test
    void infoReportsWhatIsMissingOnAPage() {
        addQuests(1, 38);
        addRewards(1, 3);

        PageService.PageInfo info = service.info(1);

        assertEquals(38, info.getQuests());
        assertEquals(Arrays.asList(39, 40), info.getMissingQuests());
        assertEquals(3, info.getRewards());
        assertEquals(Arrays.asList(4, 5), info.getMissingRewards());
        assertTrue(service.info(2).getRewards() == 0 && service.info(2).getQuests() == 0);
    }
}

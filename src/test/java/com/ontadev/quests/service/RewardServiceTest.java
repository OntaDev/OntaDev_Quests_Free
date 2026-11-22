// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.service;

import com.ontadev.quests.model.Reward;
import com.ontadev.quests.storage.RewardCommandRecord;
import com.ontadev.quests.storage.RewardCommandRepository;
import com.ontadev.quests.storage.RewardRecord;
import com.ontadev.quests.storage.RewardRepository;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RewardServiceTest {

    private RewardRepository rewards;
    private RewardCommandRepository commands;
    private StorageExecutor storage;
    private RewardService service;

    private static RewardRecord reward(int id) {
        RewardRecord record = new RewardRecord();
        record.setId(id);
        return record;
    }

    @BeforeEach
    void setUp() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        rewards = mock(RewardRepository.class);
        commands = mock(RewardCommandRepository.class);
        storage = new StorageExecutor(plugin);

        List<RewardRecord> records = new ArrayList<>();
        for (int id = 1; id <= 12; id++) records.add(reward(id));
        when(rewards.findAllOrdered()).thenReturn(records);
        when(commands.findAllOrdered()).thenReturn(Arrays.asList(
                new RewardCommandRecord(1, "give %player% diamond"), new RewardCommandRecord(1, "say hi"),
                new RewardCommandRecord(3, "eco give %player% 100")));
        service = new RewardService(plugin, rewards, commands, storage);
    }

    @Test
    void commandsAreLoadedWithTheirRewardsInOrder() {
        assertEquals(Arrays.asList("give %player% diamond", "say hi"), service.get(1).getCommands());
        assertTrue(service.get(2).getCommands().isEmpty());
        assertEquals(1, service.get(3).getCommands().size());
    }

    @Test
    void aPageHoldsExactlyFiveRewardsAndPagesDoNotOverlap() {
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), new ArrayList<>(new java.util.TreeSet<>(service.page(1).keySet())));
        assertEquals(Arrays.asList(6, 7, 8, 9, 10), new ArrayList<>(new java.util.TreeSet<>(service.page(2).keySet())));
        assertEquals(2, service.page(3).size());
        assertEquals(0, service.page(0).size());
    }

    @Test
    void aNewRewardTakesTheFirstFreeIdAndIsPersistedWithItsCommands() {
        service.delete(4).join();
        assertEquals(4, service.nextFreeId());

        Reward reward = new Reward(null, null);
        reward.getCommands().add("say new");
        service.save(reward).join();

        assertEquals(4, reward.getId());
        storage.shutdown();
        ArgumentCaptor<RewardCommandRecord> saved = ArgumentCaptor.forClass(RewardCommandRecord.class);
        verify(commands, times(1)).save(saved.capture());
        assertEquals(4, saved.getValue().getRewardId());
        assertEquals("say new", saved.getValue().getCommand());
    }

    @Test
    void commandsCanBeAddedAndCleared() {
        Reward reward = service.get(2);

        service.addCommand(reward, "say one").join();
        assertEquals(Arrays.asList("say one"), reward.getCommands());

        service.clearCommands(reward).join();
        assertTrue(reward.getCommands().isEmpty());
        storage.shutdown();
        verify(commands).deleteByReward(2);
    }

    @Test
    void deletingARewardRemovesItsCommandsToo() {
        service.delete(1).join();

        assertNull(service.get(1));
        storage.shutdown();
        verify(commands).deleteByReward(1);
        verify(rewards).deleteById(1);
    }
}

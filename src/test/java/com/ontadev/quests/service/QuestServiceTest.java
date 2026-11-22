// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.service;

import com.ontadev.quests.model.Quest;
import com.ontadev.quests.model.QuestType;
import com.ontadev.quests.storage.QuestRecord;
import com.ontadev.quests.storage.QuestRepository;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuestServiceTest {

    private QuestRepository repository;
    private StorageExecutor storage;
    private QuestService service;

    private static QuestRecord record(int id, String type) {
        QuestRecord record = new QuestRecord();
        record.setId(id);
        record.setTitle("Квест " + id);
        record.setQuestType(type);
        record.setGoal(5);
        return record;
    }

    @BeforeEach
    void setUp() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        repository = mock(QuestRepository.class);
        storage = new StorageExecutor(plugin);
        when(repository.findAllOrdered()).thenReturn(Arrays.asList(
                record(1, "KILL_MOBS"), record(2, "BREAK_BLOCKS"), record(4, "CRAFT_ITEMS"), record(5, "ЧТО-ТО")));
        service = new QuestService(plugin, repository, storage);
    }

    @Test
    void loadsStoredQuestsAndSkipsBrokenOnes() {
        assertEquals(3, service.all().size());
        assertEquals(QuestType.BREAK_BLOCKS, service.get(2).getType());
        assertNull(service.get(5), "запись с неизвестным типом не должна ронять загрузку");
    }

    @Test
    void aPageHoldsTheFortyQuestsOfItsRange() {
        assertEquals(3, service.page(1).size());
        assertEquals(0, service.page(2).size());
        assertEquals(0, service.page(0).size());
    }

    @Test
    void aNewQuestTakesTheFirstFreeId() {
        assertEquals(3, service.nextFreeId());

        Quest quest = new Quest("Новый", null, 3, null, QuestType.BREAK_BLOCKS, null);
        service.save(quest).join();

        assertEquals(3, quest.getId());
        assertNotNull(service.get(3));
        assertEquals(5, service.nextFreeId());
        ArgumentCaptor<QuestRecord> saved = ArgumentCaptor.forClass(QuestRecord.class);
        storage.shutdown();
        verify(repository).save(saved.capture());
        assertEquals(3, saved.getValue().getId());
        assertEquals("BREAK_BLOCKS", saved.getValue().getQuestType());
        assertEquals("Новый", saved.getValue().getTitle());
    }

    @Test
    void deletingRemovesTheQuestFromTheCacheAndTheDatabase() {
        service.delete(2).join();

        assertNull(service.get(2));
        assertEquals(2, service.lastId() - 2, "последний id остаётся прежним (4)");
        storage.shutdown();
        verify(repository).deleteById(2);
    }

    @Test
    void changingTheIconReplacesTheQuestObjectKeepsItsDataAndPersistsTheNewIcon() {
        org.bukkit.inventory.ItemStack icon = mock(org.bukkit.inventory.ItemStack.class);
        when(icon.clone()).thenReturn(icon);
        when(icon.getType()).thenReturn(org.bukkit.Material.DIAMOND);
        when(icon.serializeAsBytes()).thenReturn(new byte[]{1, 2, 3});
        Quest before = service.get(2);

        Quest after = service.changeIcon(2, icon).join();

        assertNotSame(before, after, "новый объект нужен, чтобы кеш предметов меню сбросился сам");
        assertSame(after, service.get(2));
        assertEquals(before.getName(), after.getName());
        assertEquals(before.getGoal(), after.getGoal());
        assertEquals(before.getType(), after.getType());
        verify(icon).setAmount(1);
        ArgumentCaptor<QuestRecord> saved = ArgumentCaptor.forClass(QuestRecord.class);
        storage.shutdown();
        verify(repository).save(saved.capture());
        assertNotNull(saved.getValue().getIcon());
    }

    @Test
    void changingTheIconOfAMissingQuestDoesNothing() {
        assertNull(service.changeIcon(3, mock(org.bukkit.inventory.ItemStack.class)).join());
        storage.shutdown();
        verify(repository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any(QuestRecord.class));
    }
}

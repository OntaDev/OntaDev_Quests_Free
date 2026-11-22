// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.service;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StorageExecutorTest {

    private StorageExecutor executor() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        return new StorageExecutor(plugin);
    }

    @Test
    void writesRunOneAtATimeInTheOrderTheyWereSubmitted() {
        StorageExecutor storage = executor();
        List<Integer> order = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < 50; i++) {
            int n = i;
            storage.run(() -> order.add(n));
        }
        storage.shutdown();

        assertEquals(50, order.size());
        for (int i = 0; i < 50; i++) assertEquals(i, order.get(i));
    }

    @Test
    void aTaskSubmittedAfterShutdownStillRunsSoNothingIsLostOnDisable() {
        StorageExecutor storage = executor();
        storage.shutdown();
        List<String> ran = new ArrayList<>();

        storage.run(() -> ran.add("saved")).join();

        assertEquals(List.of("saved"), ran);
    }

    @Test
    void aFailingTaskFailsItsFutureButNotTheOnesAfterIt() {
        StorageExecutor storage = executor();

        assertThrows(CompletionException.class, () -> storage.run(() -> {
            throw new IllegalStateException("db down");
        }).join());
        assertEquals("ok", storage.supply(() -> "ok").join());
        storage.shutdown();
    }
}

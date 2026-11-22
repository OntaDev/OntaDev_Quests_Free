// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.service;

import com.ontadev.libs.ioc.annotation.lifecycle.Shutdown;
import com.ontadev.libs.ioc.annotation.stereotype.Component;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
public class StorageExecutor {

    private final JavaPlugin plugin;
    private final ExecutorService writer = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "OntaDev_Quests-storage");
        thread.setDaemon(true);
        return thread;
    });

    public StorageExecutor(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Void> run(Runnable task) {
        return supply(() -> {
            task.run();
            return null;
        });
    }

    public <T> CompletableFuture<T> supply(Supplier<T> task) {
        if (writer.isShutdown()) {
            return CompletableFuture.completedFuture(guarded(task));
        }
        return CompletableFuture.supplyAsync(() -> guarded(task), writer);
    }

    private <T> T guarded(Supplier<T> task) {
        try {
            return task.get();
        } catch (RuntimeException e) {
            plugin.getLogger().severe("Ошибка работы с БД: " + e.getMessage());
            throw e;
        }
    }

    @Shutdown
    public void shutdown() {
        writer.shutdown();
        try {
            if (!writer.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Не все записи в БД успели завершиться");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

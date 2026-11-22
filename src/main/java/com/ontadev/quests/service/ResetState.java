// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.service;

import com.ontadev.libs.ioc.annotation.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/** Барьер массового сброса и поколения данных для отложенных сохранений/выдач. */
@Component
public class ResetState {
    private volatile ResetMode active;
    private final AtomicLong generation = new AtomicLong();
    private final AtomicLong progressGeneration = new AtomicLong();

    public synchronized boolean begin(ResetMode mode) {
        if (active != null) return false;
        active = mode;
        return true;
    }

    public synchronized void finish(boolean success) {
        if (success) {
            generation.incrementAndGet();
            if (active.resetsProgress()) progressGeneration.incrementAndGet();
        }
        active = null;
    }

    public boolean isResetting() {
        return active != null;
    }

    public long generation() {
        return generation.get();
    }

    public long progressGeneration() {
        return progressGeneration.get();
    }

    public boolean canDeliverReward(long expected) {
        ResetMode mode = active;
        return progressGeneration.get() == expected && (mode == null || !mode.resetsProgress());
    }
}

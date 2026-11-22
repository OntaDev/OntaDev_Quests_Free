// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.metrics;

import com.ontadev.libs.ioc.annotation.stereotype.Service;
import com.ontadev.libs.metrics.PluginMetrics;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class QuestMetrics {
    private final AtomicInteger completedQuests = new AtomicInteger();
    private final AtomicInteger collectedRewards = new AtomicInteger();
    private final AtomicInteger skippedQuests = new AtomicInteger();

    public QuestMetrics(PluginMetrics metrics) {
        metrics.addSingleLineChart("completed_quests", () -> completedQuests.getAndSet(0));
        metrics.addSingleLineChart("collected_rewards", () -> collectedRewards.getAndSet(0));
        metrics.addSingleLineChart("skipped_quests", () -> skippedQuests.getAndSet(0));
    }

    public void questCompleted() {
        completedQuests.incrementAndGet();
    }

    public void rewardCollected() {
        collectedRewards.incrementAndGet();
    }

    public void questSkipped() {
        skippedQuests.incrementAndGet();
    }
}

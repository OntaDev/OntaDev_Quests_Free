// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.metrics;

import com.ontadev.libs.metrics.PluginMetrics;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class QuestMetricsTest {
    @Test
    void chartsReportSeparateIntervalsWithoutRepeatingOldEvents() throws Exception {
        Map<String, Callable<Integer>> charts = new HashMap<>();
        PluginMetrics bStats = mock(PluginMetrics.class);
        when(bStats.addSingleLineChart(anyString(), any())).thenAnswer(call -> {
            charts.put(call.getArgument(0), call.getArgument(1));
            return bStats;
        });
        QuestMetrics metrics = new QuestMetrics(bStats);

        metrics.questCompleted();
        metrics.questCompleted();
        metrics.rewardCollected();
        metrics.questSkipped();

        assertEquals(2, charts.get("completed_quests").call());
        assertEquals(1, charts.get("collected_rewards").call());
        assertEquals(1, charts.get("skipped_quests").call());
        for (Callable<Integer> chart : charts.values()) assertEquals(0, chart.call());

        metrics.questSkipped();
        assertEquals(0, charts.get("completed_quests").call());
        assertEquals(0, charts.get("collected_rewards").call());
        assertEquals(1, charts.get("skipped_quests").call());
    }
}

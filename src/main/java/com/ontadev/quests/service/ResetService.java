// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.service;

import com.ontadev.libs.ioc.annotation.stereotype.Service;
import com.ontadev.quests.config.QuestsConfig;
import org.jdbi.v3.core.Jdbi;

import java.util.concurrent.CompletableFuture;

@Service
public class ResetService {
    private final Jdbi database;
    private final StorageExecutor storage;
    private final PlayerService players;
    private final QuestService quests;
    private final RewardService rewards;
    private final QuestsConfig config;
    private final ResetState state;

    public ResetService(Jdbi database, StorageExecutor storage, PlayerService players, QuestService quests,
                        RewardService rewards, QuestsConfig config, ResetState state) {
        this.database = database;
        this.storage = storage;
        this.players = players;
        this.quests = quests;
        this.rewards = rewards;
        this.config = config;
        this.state = state;
    }

    public int defaultSkips() {
        return Math.max(0, config.getSkipsPerPlayer());
    }

    public CompletableFuture<Void> reset(ResetMode mode, int skips) {
        if (skips < 0) return CompletableFuture.failedFuture(new IllegalArgumentException("skips < 0"));
        if (!state.begin(mode)) return CompletableFuture.failedFuture(new IllegalStateException("Reset in progress"));

        // Сначала дописываем накопленный прогресс, чтобы сброс только пропусков его не потерял.
        return players.flushBeforeReset().thenCompose(ignored -> storage.run(() -> {
            database.useTransaction(handle -> {
                if (mode.resetsProgress()) {
                    handle.execute("DELETE FROM quest_player_rewards");
                    handle.execute("UPDATE quest_players SET quest_id = 1, progress = 0");
                }
                if (mode.resetsSkips()) handle.execute("UPDATE quest_players SET skips = ?", skips);
                if (mode == ResetMode.ALL) {
                    handle.execute("DELETE FROM quest_reward_commands");
                    handle.execute("DELETE FROM quest_rewards");
                    handle.execute("DELETE FROM quests");
                }
            });
            // Кеш меняется только после успешного COMMIT; при ошибке БД остаётся прежним.
            players.applyReset(mode, skips);
            if (mode == ResetMode.ALL) {
                quests.clearCache();
                rewards.clearCache();
            }
        })).whenComplete((ignored, error) -> state.finish(error == null));
    }
}

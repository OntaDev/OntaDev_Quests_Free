// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.listener.quest;

import com.ontadev.libs.ioc.annotation.stereotype.Component;
import com.ontadev.quests.model.Quest;
import com.ontadev.quests.model.QuestPlayer;
import com.ontadev.quests.model.QuestType;
import com.ontadev.quests.service.PlayerService;
import com.ontadev.quests.service.QuestService;

import java.util.UUID;
import java.util.function.BiConsumer;

@Component
public class QuestFlow {

    private final QuestService questService;
    private final PlayerService playerService;

    public QuestFlow(QuestService questService, PlayerService playerService) {
        this.questService = questService;
        this.playerService = playerService;
    }

    public void process(UUID uuid, QuestType type, BiConsumer<QuestPlayer, Quest> action) {
        QuestPlayer player = playerService.cached(uuid);
        if (player == null) return;

        Quest quest = questService.get(player.getQuestId());
        if (quest == null || quest.getType() != type) return;

        action.accept(player, quest);
    }
}

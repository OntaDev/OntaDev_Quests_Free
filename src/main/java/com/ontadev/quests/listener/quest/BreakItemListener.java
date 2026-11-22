// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.listener.quest;

import com.ontadev.libs.ioc.annotation.AutoListener;
import com.ontadev.libs.ioc.annotation.stereotype.Component;

import com.ontadev.quests.model.QuestType;
import com.ontadev.quests.service.PlayerService;
import com.ontadev.quests.util.ItemUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemBreakEvent;

@Component
@AutoListener
public class BreakItemListener implements Listener {

    private final QuestFlow flow;
    private final PlayerService playerService;

    public BreakItemListener(QuestFlow flow, PlayerService playerService) {
        this.flow = flow;
        this.playerService = playerService;
    }

    @EventHandler
    public void onItemBreak(PlayerItemBreakEvent event) {
        flow.process(event.getPlayer().getUniqueId(), QuestType.BREAK_ITEMS, (player, quest) -> {
            if (quest.hasTarget() && !ItemUtils.isSameItem(event.getBrokenItem(), quest.getTarget(), true)) return;

            playerService.addProgress(player, quest, 1);
        });
    }
}

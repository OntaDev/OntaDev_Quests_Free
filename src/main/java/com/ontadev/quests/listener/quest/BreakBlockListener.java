// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.listener.quest;

import com.ontadev.libs.ioc.annotation.AutoListener;
import com.ontadev.libs.ioc.annotation.stereotype.Component;

import com.ontadev.quests.model.QuestType;
import com.ontadev.quests.service.PlayerService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

@Component
@AutoListener
public class BreakBlockListener implements Listener {

    private final QuestFlow flow;
    private final PlayerService playerService;

    public BreakBlockListener(QuestFlow flow, PlayerService playerService) {
        this.flow = flow;
        this.playerService = playerService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        flow.process(event.getPlayer().getUniqueId(), QuestType.BREAK_BLOCKS, (player, quest) -> {
            if (quest.hasTarget() && event.getBlock().getType() != quest.getTarget().getType()) return;

            playerService.addProgress(player, quest, 1);
        });
    }
}

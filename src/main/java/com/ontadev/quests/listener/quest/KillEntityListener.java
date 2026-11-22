// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.listener.quest;

import com.ontadev.libs.ioc.annotation.AutoListener;
import com.ontadev.libs.ioc.annotation.stereotype.Component;

import com.ontadev.quests.model.QuestType;
import com.ontadev.quests.service.PlayerService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

@Component
@AutoListener
public class KillEntityListener implements Listener {

    private final QuestFlow flow;
    private final PlayerService playerService;

    public KillEntityListener(QuestFlow flow, PlayerService playerService) {
        this.flow = flow;
        this.playerService = playerService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null || event.getEntity() instanceof Player) return;

        flow.process(killer.getUniqueId(), QuestType.KILL_MOBS, (player, quest) -> {
            if (quest.getTargetEntity() != null && quest.getTargetEntity() != event.getEntityType()) return;

            playerService.addProgress(player, quest, 1);
        });
    }
}

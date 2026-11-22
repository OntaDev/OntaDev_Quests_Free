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
import org.bukkit.event.enchantment.EnchantItemEvent;

@Component
@AutoListener
public class EnchantListener implements Listener {

    private final QuestFlow flow;
    private final PlayerService playerService;

    public EnchantListener(QuestFlow flow, PlayerService playerService) {
        this.flow = flow;
        this.playerService = playerService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        flow.process(event.getEnchanter().getUniqueId(), QuestType.ENCHANT_ITEMS, (player, quest) -> {
            if (quest.hasTarget() && !ItemUtils.enchantsMatch(quest.getTarget(), event.getItem().getType(), event.getEnchantsToAdd())) return;

            playerService.addProgress(player, quest, 1);
        });
    }
}

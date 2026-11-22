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
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

@Component
@AutoListener
public class CraftListener implements Listener {

    private final QuestFlow flow;
    private final PlayerService playerService;

    private static final int MAX_STACK_SIZE = 64;

    public CraftListener(QuestFlow flow, PlayerService playerService) {
        this.flow = flow;
        this.playerService = playerService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getRecipe() instanceof ShapedRecipe || event.getRecipe() instanceof ShapelessRecipe)) return;

        flow.process(event.getWhoClicked().getUniqueId(), QuestType.CRAFT_ITEMS, (player, quest) -> {
            ItemStack result = event.getRecipe().getResult();
            if (quest.hasTarget() && !ItemUtils.isSameItem(result, quest.getTarget(), true)) return;

            playerService.addProgress(player, quest, craftedAmount(event, result.getAmount()));
        });
    }

    private int craftedAmount(CraftItemEvent event, int perCraft) {
        if (!event.isShiftClick()) return perCraft;

        int crafts = MAX_STACK_SIZE;
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (ingredient.getAmount() <= 0) continue;
            crafts = Math.min(crafts, ingredient.getAmount());
        }

        long total = (long) perCraft * crafts;
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }
}

// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.listener.quest;

import com.ontadev.libs.ioc.annotation.AutoListener;
import com.ontadev.libs.ioc.annotation.stereotype.Component;

import com.ontadev.quests.model.QuestType;
import com.ontadev.quests.service.PlayerService;
import com.ontadev.quests.util.ItemUtils;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

@Component
@AutoListener
public class AnvilListener implements Listener {

    private final QuestFlow flow;
    private final PlayerService playerService;

    public AnvilListener(QuestFlow flow, PlayerService playerService) {
        this.flow = flow;
        this.playerService = playerService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top instanceof AnvilInventory)) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        AnvilInventory anvil = (AnvilInventory) top;
        ItemStack result = event.getCurrentItem();
        if (ItemUtils.isAir(result) || !canTake(player, anvil, event)) return;

        flow.process(player.getUniqueId(), QuestType.ENCHANT_ITEMS, (questPlayer, quest) -> {
            if (!addsEnchantments(anvil.getItem(0), result)) return;
            if (quest.hasTarget() && !ItemUtils.enchantsMatch(quest.getTarget(), result.getType(), result.getEnchantments())) return;

            playerService.addProgress(questPlayer, quest, 1);
        });
    }

    /** Клик по результату срабатывает и когда забрать его нельзя (мало опыта, занят курсор) - такие клики не считаем. */
    private boolean canTake(Player player, AnvilInventory anvil, InventoryClickEvent event) {
        boolean affordable = player.getGameMode() == GameMode.CREATIVE || player.getLevel() >= anvil.getRepairCost();
        boolean handFree = event.isShiftClick() || ItemUtils.isAir(event.getCursor());
        return affordable && handFree;
    }

    /** Переименование и починка зачарованием не считаются: у результата должны появиться новые чары или вырасти уровень. */
    private boolean addsEnchantments(ItemStack input, ItemStack result) {
        if (result.getEnchantments().isEmpty() && !result.hasItemMeta()) return false;

        for (java.util.Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : result.getEnchantments().entrySet()) {
            int before = input == null ? 0 : input.getEnchantmentLevel(entry.getKey());
            if (entry.getValue() > before) return true;
        }
        return false;
    }
}

// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.listener.quest;

import com.ontadev.libs.ioc.annotation.AutoListener;
import com.ontadev.libs.ioc.annotation.stereotype.Component;

import com.ontadev.quests.model.QuestType;
import com.ontadev.quests.service.PlayerService;
import com.ontadev.quests.util.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@AutoListener
public class BrewListener implements Listener {

    private final QuestFlow flow;
    private final PlayerService playerService;

    private static final int POTION_SLOTS = 3;

    private final JavaPlugin plugin;
    private final Map<Location, UUID> standOwners = new ConcurrentHashMap<>();

    public BrewListener(JavaPlugin plugin, QuestFlow flow, PlayerService playerService) {
        this.flow = flow;
        this.playerService = playerService;
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBrewingClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getView().getTopInventory().getType() != InventoryType.BREWING) return;

        BrewingStand stand = ((BrewerInventory) event.getView().getTopInventory()).getHolder();
        if (stand == null) return;

        standOwners.put(stand.getLocation(), event.getWhoClicked().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();
        UUID owner = standOwners.get(location);
        if (owner == null) return;

        ItemStack[] before = new ItemStack[POTION_SLOTS];
        for (int slot = 0; slot < POTION_SLOTS; slot++) {
            ItemStack item = event.getContents().getItem(slot);
            before[slot] = item == null ? null : item.clone();
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!(block.getState() instanceof BrewingStand)) return;

            BrewerInventory inventory = ((BrewingStand) block.getState()).getInventory();
            flow.process(owner, QuestType.BREW_POTIONS, (player, quest) -> {
                int brewed = 0;
                for (int slot = 0; slot < POTION_SLOTS; slot++) {
                    ItemStack after = inventory.getItem(slot);
                    if (after == null || !isPotion(after) || after.isSimilar(before[slot])) continue;
                    if (matches(after, quest.getTarget())) brewed += after.getAmount();
                }
                playerService.addProgress(player, quest, brewed);
            });
            standOwners.remove(location);
        });
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isPotion(ItemStack item) {
        Material type = item.getType();
        return type == Material.POTION || type == Material.SPLASH_POTION || type == Material.LINGERING_POTION;
    }

    private boolean matches(ItemStack potion, ItemStack target) {
        if (ItemUtils.isAir(target)) return true;
        if (!isPotion(target) || potion.getType() != target.getType()) return false;
        if (!(potion.getItemMeta() instanceof PotionMeta) || !(target.getItemMeta() instanceof PotionMeta)) return false;

        PotionMeta have = (PotionMeta) potion.getItemMeta();
        PotionMeta want = (PotionMeta) target.getItemMeta();
        if (!have.getBasePotionData().equals(want.getBasePotionData())) return false;

        return new HashSet<>(have.getCustomEffects()).containsAll(want.getCustomEffects());
    }
}

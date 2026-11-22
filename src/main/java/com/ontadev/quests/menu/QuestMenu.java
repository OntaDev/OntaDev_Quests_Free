// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.menu;

import com.ontadev.libs.ioc.annotation.menu.Menu;
import com.ontadev.libs.item.ItemModel;
import com.ontadev.libs.menu.AbstractMenu;
import com.ontadev.libs.menu.manager.MenuManager;
import com.ontadev.libs.message.Message;
import com.ontadev.libs.player.PlayerResolver;
import com.ontadev.libs.player.PlayerSnapshot;
import com.ontadev.quests.QuestPermissions;
import com.ontadev.quests.config.QuestsConfig;
import com.ontadev.quests.model.*;
import com.ontadev.quests.service.PlayerService;
import com.ontadev.quests.service.QuestService;
import com.ontadev.quests.service.RewardService;
import com.ontadev.quests.util.ItemTaker;
import com.ontadev.quests.util.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

@Menu
public class QuestMenu extends AbstractMenu {

    private static final int QUEST_COLUMNS = 8;
    private static final String ID_PERMISSION = "quests.quest.id";

    private final QuestsConfig config;
    private final QuestService questService;
    private final RewardService rewardService;
    private final PlayerService playerService;
    private final MenuItemCache cache;
    private final MenuState state;
    private final MenuManager menuManager;
    private final PlayerResolver playerResolver;

    public QuestMenu(QuestsConfig config, QuestService questService, RewardService rewardService,
                     PlayerService playerService,
                     MenuItemCache cache, MenuState state,
                     MenuManager menuManager, PlayerResolver playerResolver) {
        this.config = config;
        this.questService = questService;
        this.rewardService = rewardService;
        this.playerService = playerService;
        this.cache = cache;
        this.state = state;
        this.menuManager = menuManager;
        this.playerResolver = playerResolver;
    }

    @Override
    public Message title(PlayerSnapshot snapshot) {
        return config.getMenuTitle();
    }

    @Override
    public int size() {
        return 54;
    }

    @Override
    public Map<Integer, ItemModel> dynamicItems(PlayerSnapshot snapshot) {
        Map<Integer, ItemModel> items = new HashMap<>();
        if (playerService.isResetting()) return blankSlots();

        Player player = Bukkit.getPlayer(snapshot.uuid());
        QuestPlayer questPlayer = playerService.cached(snapshot.uuid());
        if (player == null || questPlayer == null) return items;

        int page = state.page(snapshot.uuid(), QuestRules.pageOfQuest(questPlayer.getQuestId()));
        if (!playerService.canOpenPage(player, questPlayer, page)) return items;

        items.putAll(blankSlots());
        items.putAll(questItems(player, page, questPlayer));
        items.putAll(rewardItems(page, questPlayer));
        return items;
    }

    /** Libs при обновлении меню не очищает слоты, которых нет в новой карте, поэтому пустые слоты страницы забиваем воздухом. */
    private Map<Integer, ItemModel> blankSlots() {
        Map<Integer, ItemModel> blank = new HashMap<>();
        for (int index = 0; index < QuestRules.QUESTS_PER_PAGE; index++) {
            blank.put(questSlot(index), air());
        }
        for (int index = 0; index < QuestRules.REWARDS_PER_PAGE; index++) {
            blank.put(rewardSlot(index), air());
        }
        return blank;
    }

    private static ItemModel air() {
        return ItemModel.builder().material(Material.AIR).build();
    }

    private static int questSlot(int index) {
        return 9 + 9 * (index / QUEST_COLUMNS) + index % QUEST_COLUMNS;
    }

    private static int rewardSlot(int index) {
        return 17 + index * 9;
    }

    private Map<Integer, ItemModel> questItems(Player viewer, int page, QuestPlayer questPlayer) {
        long generation = playerService.generation();
        boolean showId = viewer.hasPermission(ID_PERMISSION);
        Map<Integer, ItemModel> items = new HashMap<>();
        int current = questPlayer.getQuestId();

        questService.page(page).forEach((id, quest) -> {
            int index = (id - 1) % QuestRules.QUESTS_PER_PAGE;
            int slot = questSlot(index);

            ItemModel item;
            if (id < current) {
                item = ItemModel.of(cache.completed(quest, showId).clone());
            } else if (id == current) {
                item = ItemModel.of(cache.current(quest, questPlayer, showId, viewer.hasPermission(QuestPermissions.SKIP)));
            } else {
                item = ItemModel.of(cache.closed(quest, showId).clone());
            }
            item.onAnyInteraction((player, event) -> {
                if (playerService.isResetting() || playerService.generation() != generation) return;
                if (id != current) return;

                if (isSkipClick(event)) {
                    playerService.skipQuest(player, questPlayer);
                    refresh(player);
                } else if (isLeftClick(event)) {
                    onQuestClick(player, questPlayer, quest);
                }
            });
            items.put(slot, item);
        });
        return items;
    }

    private Map<Integer, ItemModel> rewardItems(int page, QuestPlayer questPlayer) {
        long generation = playerService.generation();
        Map<Integer, ItemModel> items = new HashMap<>();
        Map<Integer, Reward> rewards = rewardService.page(page);

        for (int index = 0; index < QuestRules.REWARDS_PER_PAGE; index++) {
            int id = QuestRules.firstRewardOfPage(page) + index;
            Reward reward = rewards.get(id);

            if (reward == null) continue;

            MenuItemCache.RewardState state;
            if (questPlayer.getRewardIds().contains(id)) {
                state = MenuItemCache.RewardState.CLAIMED;
            } else if (QuestRules.isRewardUnlocked(id, questPlayer.getQuestId())) {
                state = MenuItemCache.RewardState.AVAILABLE;
            } else {
                state = MenuItemCache.RewardState.LOCKED;
            }

            ItemModel item = ItemModel.of(cache.reward(reward, state).clone());
            item.onAnyInteraction((clicker, event) -> {
                if (playerService.isResetting() || playerService.generation() != generation) return;
                if (isLeftClick(event)) onRewardClick(clicker, questPlayer, reward);
            });
            items.put(rewardSlot(index), item);
        }
        return items;
    }

    private void onRewardClick(Player player, QuestPlayer questPlayer, Reward reward) {
        playerService.giveReward(questPlayer, reward.getId(), player);
        refresh(player);
    }

    private void onQuestClick(Player player, QuestPlayer questPlayer, Quest quest) {
        if (quest.getType() != QuestType.BRING_ITEMS || questPlayer.getQuestId() != quest.getId()) return;

        int missing = quest.getGoal() - questPlayer.getProgress();
        if (missing <= 0) return;

        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getStorageContents();
        Predicate<ItemStack> matches = quest.hasTarget()
                ? item -> ItemUtils.isSameItem(item, quest.getTarget(), false)
                : item -> true;

        int taken = ItemTaker.take(contents, matches, missing);
        if (taken == 0) return;

        inventory.setStorageContents(contents);
        player.updateInventory();
        playerService.addProgress(questPlayer, quest, taken);
        refresh(player);
    }

    @Override
    public boolean onClick(PlayerSnapshot snapshot, InventoryClickEvent event) {
        return false;
    }

    private boolean isSkipClick(InventoryInteractEvent event) {
        if (!(event instanceof InventoryClickEvent)) return false;

        return ((InventoryClickEvent) event).getClick() == ClickType.SHIFT_LEFT;
    }

    private boolean isLeftClick(InventoryInteractEvent event) {
        return event instanceof InventoryClickEvent
                && ((InventoryClickEvent) event).getClick() == ClickType.LEFT;
    }

    public void refreshAll(boolean resetPages) {
        if (resetPages) state.clear();
        for (Player player : Bukkit.getOnlinePlayers()) refresh(player);
    }

    public CompletableFuture<Void> reopenActiveMenus() {
        List<CompletableFuture<Void>> reopened = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerSnapshot snapshot = playerResolver.getPlayerSnapshot(player);
            reopened.add(menuManager.activeMenu(snapshot).thenCompose(active ->
                    active.filter(menu -> menu == this).isPresent()
                            ? menuManager.open(this, snapshot)
                            : CompletableFuture.completedFuture(null)));
        }
        return CompletableFuture.allOf(reopened.toArray(new CompletableFuture[0]));
    }

    private void refresh(Player player) {
        menuManager.refresh(playerResolver.getPlayerSnapshot(player));
    }
}

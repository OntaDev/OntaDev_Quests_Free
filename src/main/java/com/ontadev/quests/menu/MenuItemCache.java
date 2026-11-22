// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.menu;

import com.ontadev.libs.ioc.annotation.stereotype.Component;
import com.ontadev.libs.item.ItemModel;
import com.ontadev.libs.message.Message;
import com.ontadev.libs.message.Placeholders;
import com.ontadev.quests.config.QuestsConfig;
import com.ontadev.quests.model.Quest;
import com.ontadev.quests.model.QuestPlayer;
import com.ontadev.quests.model.QuestType;
import com.ontadev.quests.model.Reward;
import com.ontadev.quests.reward.RewardText;
import com.ontadev.quests.service.LocalizeService;
import com.ontadev.quests.util.RevisionCache;
import com.ontadev.quests.util.Texts;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@Component
public class MenuItemCache {

    public enum RewardState {
        CLAIMED, AVAILABLE, LOCKED
    }

    private final QuestsConfig config;
    private final LocalizeService localize;
    private final RewardText text;

    private final RevisionCache<String, Quest, ItemStack> completed = new RevisionCache<>();
    private final RevisionCache<String, Quest, ItemStack> closedForAdmin = new RevisionCache<>();
    private final RevisionCache<String, Reward, ItemStack> rewards = new RevisionCache<>();
    private volatile ItemStack closed;

    public MenuItemCache(QuestsConfig config, LocalizeService localize, RewardText text) {
        this.config = config;
        this.localize = localize;
        this.text = text;
    }

    public void clear() {
        completed.clear();
        closedForAdmin.clear();
        rewards.clear();
        closed = null;
    }

    public ItemStack completed(Quest quest, boolean showId) {
        return completed.get(quest.getId() + ":" + showId, quest, () -> render(quest, null, config.getCompletedQuest(), showId, null));
    }

    public ItemStack current(Quest quest, QuestPlayer player, boolean showId, boolean canSkip) {
        return render(quest, player, config.getCurrentQuest(), showId, withBind(canSkip ? config.getSkipAvailableHint() : config.getSkipLockedHint()));
    }

    public ItemStack closed(Quest quest, boolean admin) {
        if (admin) return closedForAdmin.get(quest.getId() + ":closed", quest, () -> render(quest, null, config.getAdminClosedQuest(), true, null));

        ItemStack item = closed;
        if (item == null) {
            ItemModel model = ItemModel.builder().material(Material.BARRIER).build();
            model.setName(config.getQuestClosedTitle());
            model.setLore(config.getClosedQuest());
            item = hideDetails(model).toItemStack();
            closed = item;
        }
        return item;
    }

    private Message withBind(Message hint) {
        List<String> lines = new ArrayList<>();
        for (String line : hint.getRawMessage()) lines.add(line.replace("<bind>", config.getSkipBind()));
        return new Message(lines);
    }

    private static ItemModel hideDetails(ItemModel model) {
        model.setItemFlags(EnumSet.allOf(ItemFlag.class));
        return model;
    }

    private List<String> idLines(Quest quest) {
        return Texts.render(config.getQuestIdLore(), new Placeholders().add("quest_id", quest.getId())).getRawMessage();
    }

    public ItemStack reward(Reward reward, RewardState state) {
        String key = reward.getId() + ":" + state + ":" + reward.getItems().hashCode();
        return rewards.get(key, reward, () -> {
            ItemModel model = ItemModel.of(reward.getIcon().clone());
            model.setName(config.getRewardTitle());

            List<String> lore = new ArrayList<>(contentsLore(reward).getRawMessage());
            lore.addAll(rewardLore(state).getRawMessage());
            model.setLore(new Message(lore));
            return hideDetails(model).toItemStack();
        });
    }

    private Message contentsLore(Reward reward) {
        return Texts.render(config.getRewardContents(), new Placeholders().add("reward_items", text.lines(reward.getItems())));
    }

    private Message rewardLore(RewardState state) {
        switch (state) {
            case CLAIMED:
                return config.getRewardClaimed();
            case AVAILABLE:
                return config.getRewardAvailableLore();
            default:
                return config.getRewardNotAvailableLore();
        }
    }

    private ItemStack render(Quest quest, QuestPlayer player, Message template, boolean showId, Message extra) {
        Placeholders placeholders = new Placeholders();
        placeholders.add("quest_title", quest.getName());
        placeholders.add("quest_type", quest.getType().getTranslation());
        placeholders.add("quest_requirement", String.valueOf(quest.getGoal()));
        placeholders.add("quest_target", targetName(quest));
        placeholders.add("enchantments", enchantments(quest));
        if (player != null) {
            placeholders.add("quest_progress", String.valueOf(player.getProgress()));
            placeholders.add("quest_skips", String.valueOf(player.getSkips()));
        }

        ItemModel model = ItemModel.of(quest.getIcon().clone());
        model.setName(Texts.render(config.getQuestTitle(), placeholders));
        List<String> lore = new ArrayList<>(Texts.render(template, placeholders).getRawMessage());
        if (extra != null) lore.addAll(extra.getRawMessage());
        if (showId) lore.addAll(idLines(quest));
        model.setLore(new Message(lore));
        return hideDetails(model).toItemStack();
    }

    private String targetName(Quest quest) {
        if (quest.getType() == QuestType.KILL_MOBS) {
            EntityType entity = quest.getTargetEntity();
            return entity == null ? "Любой моб" : localize.entity(entity);
        }
        return quest.hasTarget() ? localize.material(quest.getTarget().getType()) : "Любой предмет";
    }

    private List<String> enchantments(Quest quest) {
        List<String> lines = new ArrayList<>();
        if (!quest.hasTarget()) return lines;

        for (Map.Entry<Enchantment, Integer> entry : quest.getTarget().getEnchantments().entrySet()) {
            lines.add(localize.enchantment(entry.getKey(), entry.getValue()));
        }
        return lines;
    }
}

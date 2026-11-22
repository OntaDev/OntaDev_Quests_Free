// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.reward;

import com.ontadev.libs.ioc.annotation.stereotype.Component;
import com.ontadev.quests.config.QuestsConfig;
import com.ontadev.quests.service.LocalizeService;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RewardText {

    private final QuestsConfig config;
    private final LocalizeService localize;

    public RewardText(QuestsConfig config, LocalizeService localize) {
        this.config = config;
        this.localize = localize;
    }

    public List<String> lines(List<ItemStack> items) {
        List<String> lines = new ArrayList<>();
        for (ItemStack item : items) {
            lines.add(config.getRewardItemLine()
                    .replace("<name>", itemName(item))
                    .replace("<amount>", String.valueOf(item.getAmount())));
            for (Map.Entry<Enchantment, Integer> enchantment : enchantmentsOf(item).entrySet()) {
                lines.add(config.getRewardEnchantLine()
                        .replace("<enchantment>", localize.enchantment(enchantment.getKey(), enchantment.getValue())));
            }
        }
        if (lines.isEmpty()) lines.add(config.getRewardNoItemsLine());
        return lines;
    }

    private Map<Enchantment, Integer> enchantmentsOf(ItemStack item) {
        Map<Enchantment, Integer> all = new LinkedHashMap<>(item.getEnchantments());
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof EnchantmentStorageMeta) all.putAll(((EnchantmentStorageMeta) meta).getStoredEnchants());
        return all;
    }

    @SuppressWarnings("deprecation")
    private String itemName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) return ChatColor.stripColor(meta.getDisplayName());
        return localize.material(item.getType());
    }
}

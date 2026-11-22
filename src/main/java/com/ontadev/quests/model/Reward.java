// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.model;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public class Reward {

    public static final int NO_ID = -1;

    /** Максимум предметов в сохранённой награде; сохраняет совместимость формата данных. */
    public static final int MAX_ITEMS = 27;

    @Setter
    private int id;
    private final ItemStack icon;
    private final List<ItemStack> items = new CopyOnWriteArrayList<>();
    private final List<String> commands = new CopyOnWriteArrayList<>();

    public Reward(int id, ItemStack icon, List<ItemStack> items) {
        this.id = id;
        this.icon = icon;
        setItems(items);
    }

    public Reward(int id, ItemStack icon, ItemStack item) {
        this(id, icon, item == null ? new ArrayList<>() : Collections.singletonList(item));
    }

    public Reward(ItemStack icon, ItemStack item) {
        this(NO_ID, icon, item);
    }

    public void setItems(List<ItemStack> items) {
        List<ItemStack> kept = new ArrayList<>();
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) continue;
            if (kept.size() == MAX_ITEMS) break;
            kept.add(item.clone());
        }
        this.items.clear();
        this.items.addAll(kept);
    }

    public void setCommands(List<String> commands) {
        this.commands.clear();
        this.commands.addAll(new ArrayList<>(commands));
    }
}

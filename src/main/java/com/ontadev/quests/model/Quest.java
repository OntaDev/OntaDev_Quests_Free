// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.model;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

@Getter
public class Quest {

    public static final int NO_ID = -1;

    @Setter
    private int id;
    private final String name;
    private final ItemStack target;
    private final int goal;
    private final ItemStack icon;
    private final QuestType type;
    private final EntityType targetEntity;

    public Quest(int id, String name, ItemStack target, int goal, ItemStack icon, QuestType type, EntityType targetEntity) {
        this.id = id;
        this.name = name;
        this.target = isAir(target) ? null : target;
        this.goal = goal;
        this.icon = icon;
        this.type = type;
        this.targetEntity = targetEntity;
    }

    public Quest(String name, ItemStack target, int goal, ItemStack icon, QuestType type, EntityType targetEntity) {
        this(NO_ID, name, target, goal, icon, type, targetEntity);
    }

    /** Копия с другой иконкой: объект квеста неизменяем, а новый объект сам сбрасывает кеш предметов меню. */
    public Quest withIcon(ItemStack newIcon) {
        return new Quest(id, name, target, goal, newIcon, type, targetEntity);
    }

    public boolean hasTarget() {
        return target != null;
    }

    private static boolean isAir(ItemStack item) {
        return item == null || item.getType().isAir();
    }
}

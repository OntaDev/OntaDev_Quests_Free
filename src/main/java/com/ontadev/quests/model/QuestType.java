// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.model;

import lombok.Getter;

public enum QuestType {
    KILL_MOBS("убить"),
    BRING_ITEMS("принести"),
    BREAK_ITEMS("поломать"),
    BREAK_BLOCKS("сломать"),
    CRAFT_ITEMS("скрафтить"),
    BREW_POTIONS("сварить"),
    ENCHANT_ITEMS("зачаровать");

    @Getter
    private final String translation;

    QuestType(String translation) {
        this.translation = translation;
    }

    public static QuestType from(String text) {
        if (text == null) return null;

        for (QuestType type : values()) {
            if (type.translation.equalsIgnoreCase(text) || type.name().equalsIgnoreCase(text)) {
                return type;
            }
        }
        return null;
    }
}

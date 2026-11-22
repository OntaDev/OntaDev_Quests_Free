// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.util;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ItemCodec {

    private ItemCodec() {
    }

    public static String encode(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        return Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    public static ItemStack decode(String text) {
        if (text == null || text.isEmpty()) return null;
        return ItemStack.deserializeBytes(Base64.getDecoder().decode(text));
    }

    /** Base64 не содержит запятой, поэтому список хранится через неё; одиночный предмет старого формата читается как список из одного. */
    public static String encodeAll(List<ItemStack> items) {
        String joined = items.stream().map(ItemCodec::encode).filter(Objects::nonNull).collect(Collectors.joining(","));
        return joined.isEmpty() ? null : joined;
    }

    public static List<ItemStack> decodeAll(String text) {
        List<ItemStack> items = new ArrayList<>();
        if (text == null || text.isEmpty()) return items;

        for (String part : text.split(",")) {
            ItemStack item = decode(part);
            if (item != null) items.add(item);
        }
        return items;
    }

}

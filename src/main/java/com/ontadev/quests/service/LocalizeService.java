// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ontadev.libs.ioc.annotation.stereotype.Service;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class LocalizeService {

    private static final String FILE_NAME = "ru_ru.json";

    private final File file;
    private volatile Map<String, String> translations = Map.of();

    public LocalizeService(JavaPlugin plugin) {
        file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }

        try {
            reload();
        } catch (RuntimeException e) {
            plugin.getLogger().warning("Не удалось загрузить " + FILE_NAME + ", названия будут на английском: " + e.getMessage());
        }
    }

    public void reload() {
        Map<String, String> loaded = new HashMap<>();
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            JsonObject json = new Gson().fromJson(reader, JsonObject.class);
            if (json == null) throw new IllegalArgumentException(FILE_NAME + ": ожидается JSON-объект");
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                loaded.put(entry.getKey(), entry.getValue().getAsString());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        translations = Map.copyOf(loaded);
    }

    public String material(Material material) {
        return translations.getOrDefault(material.getTranslationKey(), pretty(material.name()));
    }

    public String entity(EntityType type) {
        return translations.getOrDefault("entity.minecraft." + type.name().toLowerCase(Locale.ROOT), pretty(type.name()));
    }

    public String enchantment(Enchantment enchantment, int level) {
        String key = "enchantment." + enchantment.getKey().getNamespace() + "." + enchantment.getKey().getKey();
        String name = translations.getOrDefault(key, pretty(enchantment.getKey().getKey()));
        String roman = translations.getOrDefault("enchantment.level." + level, String.valueOf(level));
        return name + " " + roman;
    }

    private static String pretty(String name) {
        String lower = name.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}

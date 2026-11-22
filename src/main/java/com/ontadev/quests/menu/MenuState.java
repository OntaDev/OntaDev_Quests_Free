// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.menu;

import com.ontadev.libs.ioc.annotation.AutoListener;
import com.ontadev.libs.ioc.annotation.stereotype.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Меню в Libs - синглтоны, поэтому страница, которую смотрит игрок, хранится отдельно. */
@Component
@AutoListener
public class MenuState implements Listener {

    private final Map<UUID, Integer> pages = new ConcurrentHashMap<>();

    public int page(UUID player, int fallback) {
        return pages.getOrDefault(player, fallback);
    }

    public void setPage(UUID player, int page) {
        pages.put(player, page);
    }

    public void clear() {
        pages.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pages.remove(event.getPlayer().getUniqueId());
    }
}

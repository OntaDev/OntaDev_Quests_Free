// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.listener;

import com.ontadev.libs.ioc.annotation.AutoListener;
import com.ontadev.libs.ioc.annotation.stereotype.Component;
import com.ontadev.quests.model.QuestPlayer;
import com.ontadev.quests.service.PlayerService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

@Component
@AutoListener
public class PlayerListener implements Listener {

    private final PlayerService playerService;

    public PlayerListener(PlayerService playerService) {
        this.playerService = playerService;
    }

    /** Данные грузятся до входа на сервер: события игрока не теряются в первые секунды после join. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;

        playerService.loadOrCreate(event.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        QuestPlayer player = playerService.cached(uuid);
        if (player == null) return;

        playerService.save(player);
        playerService.unload(uuid);
    }
}

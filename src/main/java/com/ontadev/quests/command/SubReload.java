// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.command;

import com.ontadev.libs.command.SubCommand;
import com.ontadev.libs.ioc.annotation.stereotype.Component;
import com.ontadev.quests.config.MessagesConfig;
import com.ontadev.quests.config.QuestsConfig;
import com.ontadev.quests.menu.MenuItemCache;
import com.ontadev.quests.menu.QuestMenu;
import com.ontadev.quests.service.LocalizeService;
import com.ontadev.quests.service.PlayerService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

@Component
public class SubReload extends SubCommand {
    private final QuestsConfig config;
    private final MessagesConfig messages;
    private final LocalizeService localize;
    private final MenuItemCache cache;
    private final QuestMenu menu;
    private final PlayerService players;
    private final JavaPlugin plugin;

    public SubReload(QuestsConfig config, MessagesConfig messages, LocalizeService localize, MenuItemCache cache,
                     QuestMenu menu, PlayerService players, JavaPlugin plugin) {
        super("reload");
        this.config = config;
        this.messages = messages;
        this.localize = localize;
        this.cache = cache;
        this.menu = menu;
        this.players = players;
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, Command command, String label, String... args) {
        if (!sender.hasPermission("quests.reload")) {
            messages.getNoPermission().send(sender);
            return;
        }
        if (args.length != 0) {
            sendUsage(sender, command, label, args);
            return;
        }
        if (players.isResetting()) {
            messages.getResetInProgress().send(sender);
            return;
        }

        QuestsConfig previousConfig = new QuestsConfig();
        previousConfig.applyFrom(config);
        MessagesConfig previousMessages = new MessagesConfig();
        previousMessages.applyFrom(messages);
        try {
            config.reload();
            messages.reload();
            localize.reload();
        } catch (RuntimeException error) {
            // Ошибка в одном файле не должна оставлять смесь старых и новых настроек.
            config.applyFrom(previousConfig);
            messages.applyFrom(previousMessages);
            plugin.getLogger().warning("Не удалось перечитать конфиги квестов: " + error.getMessage());
            messages.getReloadFailed().send(sender);
            return;
        }

        cache.clear();
        players.rescheduleSaving();
        menu.reopenActiveMenus().exceptionally(error -> {
            plugin.getLogger().warning("Конфиги загружены, но не все меню обновлены: " + error.getMessage());
            return null;
        });
        messages.getReloadDone().send(sender);
    }

    @Override
    public String getPermission(CommandSender sender, Command command, String label, String... args) {
        return "quests.reload";
    }

    @Override
    public void noPermission(CommandSender sender, Command command, String label, String... args) {
        messages.getNoPermission().send(sender);
    }

    @Override
    public void sendUsage(CommandSender sender, Command command, String label, String... args) {
        sender.sendMessage("/" + label + " reload");
    }
}

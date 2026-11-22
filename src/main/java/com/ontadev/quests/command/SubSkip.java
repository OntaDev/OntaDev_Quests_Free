// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.command;

import com.ontadev.libs.command.SubCommand;
import com.ontadev.libs.ioc.annotation.stereotype.Component;
import com.ontadev.quests.config.MessagesConfig;
import com.ontadev.quests.model.QuestPlayer;
import com.ontadev.quests.service.PlayerService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Component
public class SubSkip extends SubCommand {

    private final PlayerService playerService;
    private final MessagesConfig messages;

    public SubSkip(PlayerService playerService, MessagesConfig messages) {
        super("skip");
        this.playerService = playerService;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSender sender, Command command, String label, String... args) {
        if (!(sender instanceof Player)) {
            messages.getOnlyPlayers().send(sender);
            return;
        }

        Player player = (Player) sender;
        QuestPlayer questPlayer = playerService.cached(player.getUniqueId());
        if (questPlayer == null) {
            messages.getError().send(player);
            return;
        }

        playerService.skipQuest(player, questPlayer);
    }

    @Override
    public String getPermission(CommandSender sender, Command command, String label, String... args) {
        return "quests.open";
    }

    @Override
    public void noPermission(CommandSender sender, Command command, String label, String... args) {
        messages.getNoPermission().send(sender);
    }

    @Override
    public void sendUsage(CommandSender sender, Command command, String label, String... args) {
        sender.sendMessage("/" + label + " skip");
    }
}

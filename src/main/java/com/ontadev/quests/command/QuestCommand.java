// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.command;

import com.ontadev.libs.command.AbstractCommand;
import com.ontadev.libs.ioc.annotation.stereotype.RegisterCommand;
import com.ontadev.quests.config.MessagesConfig;
import com.ontadev.quests.menu.MenuState;
import com.ontadev.quests.menu.QuestMenu;
import com.ontadev.quests.model.QuestPlayer;
import com.ontadev.quests.model.QuestRules;
import com.ontadev.quests.service.PlayerService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RegisterCommand
public class QuestCommand extends AbstractCommand {

    private final QuestMenu menu;
    private final MenuState state;
    private final PlayerService playerService;
    private final MessagesConfig messages;

    public QuestCommand(QuestMenu menu, MenuState state, PlayerService playerService, MessagesConfig messages,
                        SubQuest subQuest, SubReward subReward, SubRewardCommands subRewardCommands,
                        SubUser subUser, SubSkip subSkip, SubPage subPage, SubReset subReset, SubReload subReload) {
        this.menu = menu;
        this.state = state;
        this.playerService = playerService;
        this.messages = messages;

        registerSubCommand(subQuest);
        registerSubCommand(subReward);
        registerSubCommand(subRewardCommands);
        registerSubCommand(subUser);
        registerSubCommand(subSkip);
        registerSubCommand(subPage);
        registerSubCommand(subReset);
        registerSubCommand(subReload);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (playerService.isResetting()) {
            messages.getResetInProgress().send(sender);
            return true;
        }
        return super.execute(sender, label, args);
    }

    @Override
    public @NotNull String getName() {
        return "quests";
    }

    @Override
    public @NotNull List<String> getAliases() {
        return Arrays.asList("квест", "квесты");
    }

    @Override
    protected void handle(CommandSender sender, Command command, String label, String[] args) {
        boolean open = args.length > 0 && args[0].equalsIgnoreCase("open");

        Player player;
        if (open && args.length > 2) {
            if (!sender.hasPermission("quests.open.others")) {
                messages.getNoPermission().send(sender);
                return;
            }
            player = Bukkit.getPlayerExact(args[2]);
            if (player == null) {
                messages.getPlayerNotFound().send(sender);
                return;
            }
        } else if (sender instanceof Player) {
            player = (Player) sender;
            if (!sender.hasPermission("quests.open")) {
                messages.getNoPermission().send(sender);
                return;
            }
        } else {
            messages.getOnlyPlayers().send(sender);
            return;
        }

        QuestPlayer questPlayer = playerService.cached(player.getUniqueId());
        if (questPlayer == null) {
            messages.getError().send(sender);
            return;
        }

        int page = QuestRules.pageOfQuest(questPlayer.getQuestId());
        if (open && args.length > 1) {
            Optional<Integer> requested = Args.integer(args[1]);
            if (requested.isEmpty()) {
                messages.getNotNumber().send(sender);
                return;
            }
            page = requested.get();
        }

        if (!playerService.canOpenPage(player, questPlayer, page)) {
            messages.getPageNotAvailable().send(sender);
            return;
        }

        state.setPage(player.getUniqueId(), page);
        menu.open(player);
    }

    @Override
    public List<String> complete(CommandSender sender, String label, String... args) {
        List<String> result = new ArrayList<>(super.complete(sender, label, args));
        if (args.length == 1) result.add("open");
        return result;
    }
}

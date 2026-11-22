// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.command;

import com.ontadev.libs.command.SubCommand;
import com.ontadev.libs.ioc.annotation.stereotype.Component;
import com.ontadev.libs.message.Placeholders;
import com.ontadev.quests.config.MessagesConfig;
import com.ontadev.quests.model.Reward;
import com.ontadev.quests.service.RewardService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class SubRewardCommands extends SubCommand {

    private final RewardService rewardService;
    private final MessagesConfig messages;

    public SubRewardCommands(RewardService rewardService, MessagesConfig messages) {
        super("commands");
        this.rewardService = rewardService;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSender sender, Command command, String label, String... args) {
        if (args.length < 2) {
            messages.getCommandsUsage().send(sender);
            return;
        }

        Optional<Integer> id = Args.integer(args[1]);
        if (id.isEmpty()) {
            messages.getNotNumber().send(sender);
            return;
        }

        Reward reward = rewardService.get(id.get());
        if (reward == null) {
            messages.getRewardNotFound().send(sender);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "add":
                if (args.length < 3) {
                    messages.getCommandsUsage().send(sender);
                    return;
                }
                rewardService.addCommand(reward, Args.join(args, 2)).thenRun(() -> messages.getCommandAdded().send(sender));
                break;
            case "clear":
                rewardService.clearCommands(reward).thenRun(() -> messages.getCommandsCleared().send(sender));
                break;
            case "list":
                list(sender, reward);
                break;
            default:
                messages.getCommandsUsage().send(sender);
        }
    }

    private void list(CommandSender sender, Reward reward) {
        List<String> commands = new ArrayList<>(reward.getCommands());
        if (commands.isEmpty()) {
            messages.getCommandsEmpty().send(sender);
            return;
        }

        messages.getCommandsList().send(sender, Placeholders.of("id", String.valueOf(reward.getId())));
        for (int i = 0; i < commands.size(); i++) {
            sender.sendMessage(ChatColor.GOLD.toString() + (i + 1) + ". " + commands.get(i));
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String... args) {
        if (args.length == 1) {
            return Stream.of("add", "list", "clear").filter(action -> sender.hasPermission(permissionOf(action))).collect(Collectors.toList());
        }
        if (args.length == 2) {
            return rewardService.all().keySet().stream().map(String::valueOf).collect(Collectors.toList());
        }
        if (args.length >= 3 && args[0].equalsIgnoreCase("add")) {
            return Stream.of("%player%").collect(Collectors.toList());
        }
        return Args.none();
    }

    @Override
    public String getPermission(CommandSender sender, Command command, String label, String... args) {
        return args.length == 0 ? "quests.commands.list" : permissionOf(args[0]);
    }

    private String permissionOf(String action) {
        switch (action.toLowerCase()) {
            case "add":
                return "quests.commands.add";
            case "clear":
                return "quests.commands.clear";
            default:
                return "quests.commands.list";
        }
    }

    @Override
    public void noPermission(CommandSender sender, Command command, String label, String... args) {
        messages.getNoPermission().send(sender);
    }

    @Override
    public void sendUsage(CommandSender sender, Command command, String label, String... args) {
        messages.getCommandsUsage().send(sender);
    }
}

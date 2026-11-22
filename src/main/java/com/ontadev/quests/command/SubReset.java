// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.command;

import com.ontadev.libs.command.SubCommand;
import com.ontadev.libs.ioc.annotation.stereotype.Component;
import com.ontadev.libs.message.Placeholders;
import com.ontadev.quests.config.MessagesConfig;
import com.ontadev.quests.menu.QuestMenu;
import com.ontadev.quests.service.ResetMode;
import com.ontadev.quests.service.ResetService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class SubReset extends SubCommand {
    private final ResetService resets;
    private final SubQuest quests;
    private final QuestMenu menu;
    private final MessagesConfig messages;
    private final JavaPlugin plugin;

    public SubReset(ResetService resets, SubQuest quests, QuestMenu menu, MessagesConfig messages, JavaPlugin plugin) {
        super("reset");
        this.resets = resets;
        this.quests = quests;
        this.menu = menu;
        this.messages = messages;
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, Command command, String label, String... args) {
        if (!sender.hasPermission("quests.reset")) {
            messages.getNoPermission().send(sender);
            return;
        }
        if (args.length == 0) {
            messages.getResetUsage().send(sender);
            return;
        }
        ResetMode mode;
        try {
            mode = ResetMode.valueOf(args[0].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            messages.getResetUsage().send(sender);
            return;
        }
        boolean confirmed = args[args.length - 1].equalsIgnoreCase("confirm");
        int arguments = args.length - (confirmed ? 1 : 0);
        int skips = resets.defaultSkips();
        if (arguments == 2 && mode == ResetMode.SKIPS) {
            Optional<Integer> amount = Args.integer(args[1]);
            if (amount.isEmpty() || amount.get() < 0) {
                messages.getNotNumber().send(sender);
                return;
            }
            skips = amount.get();
        } else if (arguments != 1) {
            messages.getResetUsage().send(sender);
            return;
        }
        if (!confirmed) {
            String confirmation = "/" + label + " reset " + args[0].toLowerCase(Locale.ROOT)
                    + (mode == ResetMode.SKIPS ? " " + skips : "") + " confirm";
            switch (mode) {
                case PROGRESS:
                    messages.getResetProgressWarning().send(sender);
                    break;
                case SKIPS:
                    messages.getResetSkipsWarning().send(sender, Placeholders.of("skips", String.valueOf(skips)));
                    break;
                default:
                    messages.getResetAllWarning().send(sender, Placeholders.of("skips", String.valueOf(skips)));
            }
            messages.getResetConfirmation().send(sender, Placeholders.of("command", confirmation));
            return;
        }

        messages.getResetStarted().send(sender);
        if (mode == ResetMode.ALL) quests.clearPending();
        resets.reset(mode, skips).whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (error != null) {
                plugin.getLogger().severe("Массовый сброс не выполнен: " + error.getMessage());
                messages.getResetFailed().send(sender);
                return;
            }
            menu.refreshAll(mode.resetsProgress());
            messages.getResetDone().send(sender, Placeholders.of("mode", mode.name().toLowerCase(Locale.ROOT)));
        }));
    }

    @Override
    public List<String> complete(CommandSender sender, String... args) {
        if (!sender.hasPermission("quests.reset")) return Args.none();
        if (args.length == 1) return Arrays.asList("progress", "skips", "all");
        if (args.length == 2) return args[0].equalsIgnoreCase("skips")
                ? Arrays.asList("0", String.valueOf(resets.defaultSkips()), "confirm") : List.of("confirm");
        if (args.length == 3 && args[0].equalsIgnoreCase("skips")) return List.of("confirm");
        return Args.none();
    }

    @Override
    public String getPermission(CommandSender sender, Command command, String label, String... args) {
        return "quests.reset";
    }

    @Override
    public void noPermission(CommandSender sender, Command command, String label, String... args) {
        messages.getNoPermission().send(sender);
    }

    @Override
    public void sendUsage(CommandSender sender, Command command, String label, String... args) {
        messages.getResetUsage().send(sender);
    }
}

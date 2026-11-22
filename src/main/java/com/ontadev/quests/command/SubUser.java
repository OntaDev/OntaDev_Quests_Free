// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.command;

import com.ontadev.libs.command.SubCommand;
import com.ontadev.libs.ioc.annotation.stereotype.Component;
import com.ontadev.libs.message.Placeholders;
import com.ontadev.quests.config.MessagesConfig;
import com.ontadev.quests.model.QuestPlayer;
import com.ontadev.quests.service.PlayerService;
import com.ontadev.quests.service.ResetState;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class SubUser extends SubCommand {

    private final PlayerService playerService;
    private final MessagesConfig messages;
    private final JavaPlugin plugin;
    private final ResetState resets;

    public SubUser(PlayerService playerService, MessagesConfig messages, JavaPlugin plugin, ResetState resets) {
        super("user");
        this.playerService = playerService;
        this.messages = messages;
        this.plugin = plugin;
        this.resets = resets;
    }

    @Override
    public void execute(CommandSender sender, Command command, String label, String... args) {
        if (args.length < 3) {
            messages.getUserUsage().send(sender);
            return;
        }

        String action = args[1].toLowerCase();

        UUID uuid = resolve(args[0]);
        if (uuid == null) {
            messages.getPlayerOffline().send(sender);
            return;
        }

        long generation = resets.generation();
        playerService.find(uuid).thenAccept(found -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (resets.isResetting() || resets.generation() != generation) {
                messages.getResetInProgress().send(sender);
                return;
            }
            if (found.isEmpty()) {
                messages.getPlayerOffline().send(sender);
                return;
            }
            apply(sender, found.get(), action, args);
        }));
    }

    private void apply(CommandSender sender, QuestPlayer player, String action, String[] args) {
        switch (action) {
            case "skips":
            case "quests":
            case "progress":
                setValue(sender, player, action, args[2]);
                break;
            case "rewards":
                rewards(sender, player, args);
                break;
            default:
                messages.getUnknownAction().send(sender, Placeholders.of("action", action));
        }
    }

    private void setValue(CommandSender sender, QuestPlayer player, String field, String text) {
        Optional<Integer> value = Args.integer(text);
        if (value.isEmpty() || value.get() < (field.equals("quests") ? 1 : 0)) {
            messages.getNotNumber().send(sender);
            return;
        }

        switch (field) {
            case "skips":
                player.setSkips(value.get());
                break;
            case "quests":
                player.setQuestId(value.get());
                player.setProgress(0);
                break;
            default:
                player.setProgress(value.get());
        }
        playerService.save(player).thenRun(() -> messages.getPlayerDataUpdated().send(sender));
    }

    private void rewards(CommandSender sender, QuestPlayer player, String[] args) {
        String action = args[2].toLowerCase();
        if (action.equals("list")) {
            Set<Integer> sorted = new TreeSet<>(player.getRewardIds());
            messages.getPlayerRewardsList().send(sender, Placeholders.of("rewards", sorted.toString()));
            return;
        }

        Optional<Integer> id = args.length > 3 ? Args.integer(args[3]) : Optional.empty();
        if (id.isEmpty()) {
            messages.getNotNumber().send(sender);
            return;
        }

        if (action.equals("add")) {
            if (!player.getRewardIds().add(id.get())) {
                messages.getPlayerHasReward().send(sender);
                return;
            }
            playerService.save(player).thenRun(() -> messages.getPlayerRewardAdded().send(sender));
        } else if (action.equals("remove")) {
            if (!player.getRewardIds().remove(id.get())) {
                messages.getPlayerHasNoReward().send(sender);
                return;
            }
            playerService.save(player).thenRun(() -> messages.getPlayerRewardRemoved().send(sender));
        } else {
            messages.getUnknownAction().send(sender, Placeholders.of("action", action));
        }
    }

    @SuppressWarnings("deprecation")
    private UUID resolve(String text) {
        Player online = Bukkit.getPlayerExact(text);
        if (online != null) return online.getUniqueId();

        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException ignored) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(text);
            return offline.hasPlayedBefore() ? offline.getUniqueId() : null;
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String... args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        if (args.length == 2) {
            return Stream.of("skips", "quests", "progress", "rewards").collect(Collectors.toList());
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("rewards")) {
            return Stream.of("list", "add", "remove").collect(Collectors.toList());
        }
        return Args.none();
    }

    @Override
    public String getPermission(CommandSender sender, Command command, String label, String... args) {
        return "quests.user.edit";
    }

    @Override
    public void noPermission(CommandSender sender, Command command, String label, String... args) {
        messages.getNoPermission().send(sender);
    }

    @Override
    public void sendUsage(CommandSender sender, Command command, String label, String... args) {
        messages.getUserUsage().send(sender);
    }
}

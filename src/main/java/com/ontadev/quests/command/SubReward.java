// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.command;

import com.ontadev.libs.command.SubCommand;
import com.ontadev.libs.ioc.annotation.stereotype.Component;
import com.ontadev.libs.message.Placeholders;
import com.ontadev.quests.config.MessagesConfig;
import com.ontadev.quests.model.Reward;
import com.ontadev.quests.service.PlayerService;
import com.ontadev.quests.service.RewardService;
import com.ontadev.quests.util.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class SubReward extends SubCommand {

    private final RewardService rewardService;
    private final PlayerService playerService;
    private final MessagesConfig messages;

    public SubReward(RewardService rewardService, PlayerService playerService, MessagesConfig messages) {
        super("reward");
        this.rewardService = rewardService;
        this.playerService = playerService;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSender sender, Command command, String label, String... args) {
        if (args.length == 0) {
            messages.getRewardUsage().send(sender);
            return;
        }

        Integer id = null;
        if (args.length >= 2) {
            Optional<Integer> parsed = Args.integer(args[1]);
            if (parsed.isEmpty()) {
                messages.getNotNumber().send(sender);
                return;
            }
            id = parsed.get();
        }

        switch (args[0].toLowerCase()) {
            case "create":
                create(sender, id);
                break;
            case "delete":
                delete(sender, id);
                break;
            case "give":
                give(sender, id, args);
                break;
            default:
                messages.getRewardUsage().send(sender);
        }
    }

    private void create(CommandSender sender, Integer id) {
        if (!(sender instanceof Player)) {
            messages.getOnlyPlayers().send(sender);
            return;
        }

        PlayerInventory inventory = ((Player) sender).getInventory();
        ItemStack icon = inventory.getItemInMainHand().clone();
        ItemStack item = inventory.getItemInOffHand().clone();
        if (ItemUtils.isAir(icon)) {
            messages.getNoRewardIcon().send(sender);
            return;
        }
        if (ItemUtils.isAir(item)) {
            messages.getNoRewardItem().send(sender);
            return;
        }

        Reward reward = id == null ? new Reward(icon, item) : new Reward(id, icon, item);
        rewardService.save(reward).thenRun(() -> messages.getRewardCreated().send(sender));
    }

    private void give(CommandSender sender, Integer id, String[] args) {
        Player target = args.length > 2 ? Bukkit.getPlayerExact(args[2]) : sender instanceof Player ? (Player) sender : null;
        if (id == null || target == null) {
            messages.getRewardGiveUsage().send(sender);
            return;
        }

        Reward reward = rewardService.get(id);
        if (reward == null) {
            messages.getRewardNotFound().send(sender);
            return;
        }
        if (!playerService.giveRewardItems(reward, target)) {
            messages.getNotEnoughSpace().send(sender);
            return;
        }
        messages.getRewardGiven().send(sender, Placeholders.of("id", String.valueOf(id)).add("player", target.getName()));
    }

    private void delete(CommandSender sender, Integer id) {
        int rewardId;
        if (id != null) {
            rewardId = id;
        } else {
            Optional<Integer> last = rewardService.all().keySet().stream().max(Integer::compareTo);
            if (last.isEmpty()) {
                messages.getRewardIdNeeded().send(sender);
                return;
            }
            rewardId = last.get();
        }

        if (rewardService.get(rewardId) == null) {
            messages.getRewardNotFound().send(sender);
            return;
        }
        rewardService.delete(rewardId).thenRun(() -> messages.getRewardDeleted().send(sender));
    }

    @Override
    public List<String> complete(CommandSender sender, String... args) {
        if (args.length == 1) return Stream.of("create", "delete", "give").collect(Collectors.toList());
        if (args.length == 2) return rewardService.all().keySet().stream().map(String::valueOf).collect(Collectors.toList());
        return Args.none();
    }

    @Override
    public String getPermission(CommandSender sender, Command command, String label, String... args) {
        return args.length > 0 && args[0].equalsIgnoreCase("give") ? "quests.reward.give" : "quests.reward";
    }

    @Override
    public void noPermission(CommandSender sender, Command command, String label, String... args) {
        messages.getNoPermission().send(sender);
    }

    @Override
    public void sendUsage(CommandSender sender, Command command, String label, String... args) {
        messages.getRewardUsage().send(sender);
    }
}

// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.command;

import com.ontadev.libs.command.SubCommand;
import com.ontadev.libs.ioc.annotation.stereotype.Component;
import com.ontadev.libs.message.Placeholders;
import com.ontadev.quests.config.MessagesConfig;
import com.ontadev.quests.model.Quest;
import com.ontadev.quests.model.QuestType;
import com.ontadev.quests.service.QuestService;
import com.ontadev.quests.util.ItemUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class SubQuest extends SubCommand {

    private static final long CONFIRM_TIMEOUT_MILLIS = 60_000L;

    private final QuestService questService;
    private final MessagesConfig messages;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public SubQuest(QuestService questService, MessagesConfig messages) {
        super("quest");
        this.questService = questService;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSender sender, Command command, String label, String... args) {
        if (args.length == 0) {
            messages.getQuestCreateUsage().send(sender);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                create(sender, args);
                break;
            case "confirm":
                confirm(sender);
                break;
            case "cancel":
                cancel(sender);
                break;
            case "delete":
                delete(sender, args);
                break;
            case "icon":
                icon(sender, args);
                break;
            default:
                messages.getQuestCreateUsage().send(sender);
        }
    }

    private void create(CommandSender sender, String[] args) {
        Player player = player(sender);
        if (player == null) return;
        if (args.length < 4) {
            messages.getQuestCreateUsage().send(sender);
            return;
        }

        QuestType type = QuestType.from(args[1]);
        if (type == null) {
            messages.getQuestTypeNotFound().send(sender);
            return;
        }

        Optional<Integer> goal = Args.integer(args[2]);
        if (goal.isEmpty() || goal.get() <= 0) {
            messages.getNotNumber().send(sender);
            return;
        }

        EntityType entity = null;
        int nameFrom = 3;
        if (type == QuestType.KILL_MOBS) {
            if (args.length < 5) {
                messages.getBadArguments().send(sender);
                return;
            }
            entity = parseEntity(args[3]);
            if (entity == null) messages.getEntityNotFound().send(sender);
            nameFrom = 4;
        }

        PlayerInventory inventory = player.getInventory();
        ItemStack target = inventory.getItemInOffHand().clone();
        if (ItemUtils.isAir(target)) {
            if (type != QuestType.KILL_MOBS) messages.getNoQuestTarget().send(sender);
            target = null;
        }

        ItemStack icon = inventory.getItemInMainHand().clone();
        if (ItemUtils.isAir(icon)) {
            messages.getNoQuestIcon().send(sender);
            return;
        }

        Quest quest = new Quest(Args.join(args, nameFrom), target, goal.get(), icon, type, entity);
        pending.put(player.getUniqueId(), new Pending(quest, System.currentTimeMillis() + CONFIRM_TIMEOUT_MILLIS));

        messages.getQuestPreview().send(sender, new Placeholders()
                .add("name", quest.getName())
                .add("type", type.getTranslation())
                .add("goal", quest.getGoal())
                .add("target", quest.hasTarget() ? quest.getTarget().getType().name() : "Любой предмет")
                .add("entity", entity == null ? "Любой моб" : entity.name())
                .add("icon", icon.getType().name()));
    }

    private void confirm(CommandSender sender) {
        Player player = player(sender);
        if (player == null) return;

        Pending quest = takePending(player);
        if (quest == null) {
            messages.getNoQuestToConfirm().send(sender);
            return;
        }

        questService.save(quest.quest).thenRun(() -> messages.getQuestCreated().send(sender));
    }

    private void cancel(CommandSender sender) {
        Player player = player(sender);
        if (player == null) return;

        if (takePending(player) == null) {
            messages.getNoQuestToConfirm().send(sender);
            return;
        }
        messages.getQuestCancelled().send(sender);
    }

    private void icon(CommandSender sender, String[] args) {
        Player player = player(sender);
        if (player == null) return;

        int[] range = args.length == 2 ? Args.range(args[1]) : null;
        if (range == null) {
            messages.getQuestIconUsage().send(sender);
            return;
        }

        ItemStack icon = player.getInventory().getItemInMainHand();
        if (ItemUtils.isAir(icon)) {
            messages.getNoQuestIcon().send(sender);
            return;
        }

        List<CompletableFuture<Quest>> changes = new ArrayList<>();
        for (int id = range[0]; id <= range[1]; id++) {
            Quest quest = questService.get(id);
            if (quest != null) changes.add(questService.changeIcon(id, icon));
        }
        if (changes.isEmpty()) {
            messages.getQuestNotFound().send(sender);
            return;
        }

        CompletableFuture.allOf(changes.toArray(new CompletableFuture[0])).thenRun(() ->
                messages.getQuestIconChanged().send(sender, new Placeholders()
                        .add("count", changes.size())
                        .add("from", range[0])
                        .add("to", range[1])));
    }

    private void delete(CommandSender sender, String[] args) {
        int id;
        if (args.length > 1) {
            Optional<Integer> parsed = Args.integer(args[1]);
            if (parsed.isEmpty()) {
                messages.getNotNumber().send(sender);
                return;
            }
            id = parsed.get();
        } else {
            id = questService.lastId();
        }

        if (questService.get(id) == null) {
            messages.getQuestNotFound().send(sender);
            return;
        }
        questService.delete(id).thenRun(() -> messages.getQuestDeleted().send(sender));
    }

    private Pending takePending(Player player) {
        Pending quest = pending.remove(player.getUniqueId());
        if (quest == null || quest.expiresAt < System.currentTimeMillis()) return null;
        return quest;
    }

    public void clearPending() {
        pending.clear();
    }

    private Player player(CommandSender sender) {
        if (sender instanceof Player) return (Player) sender;
        messages.getOnlyPlayers().send(sender);
        return null;
    }

    private EntityType parseEntity(String name) {
        try {
            return EntityType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String... args) {
        if (args.length == 1) {
            return Stream.of("create", "confirm", "cancel", "delete", "icon").collect(Collectors.toList());
        }

        String action = args[0].toLowerCase();

        if (args.length == 2) {
            switch (action.toLowerCase()) {
                case "create":
                    return Arrays.stream(QuestType.values())
                            .map(type -> type.name().toLowerCase())
                            .collect(Collectors.toList());
                case "icon":
                case "delete":
                    return questService.all().keySet().stream()
                            .map(String::valueOf)
                            .collect(Collectors.toList());
                default:
                    return Args.none();
            }
        }

        if (args.length == 3 && action.equalsIgnoreCase("create")) {
            return List.of("{Цель кол-во}");
        }

        if (args.length == 4 && action.equalsIgnoreCase("create")
                && QuestType.from(args[1]) == QuestType.KILL_MOBS) {
            return Arrays.stream(EntityType.values())
                    .map(type -> type.name().toLowerCase())
                    .collect(Collectors.toList());
        }

        return Args.none();
    }

    @Override
    public String getPermission(CommandSender sender, Command command, String label, String... args) {
        if (args.length == 0) return "quests.quest.create";
        if (args[0].equalsIgnoreCase("delete")) return "quests.quest.delete";
        return args[0].equalsIgnoreCase("icon") ? "quests.quest.icon" : "quests.quest.create";
    }

    @Override
    public void noPermission(CommandSender sender, Command command, String label, String... args) {
        messages.getNoPermission().send(sender);
    }

    @Override
    public void sendUsage(CommandSender sender, Command command, String label, String... args) {
        messages.getQuestCreateUsage().send(sender);
    }

    private static final class Pending {
        private final Quest quest;
        private final long expiresAt;

        private Pending(Quest quest, long expiresAt) {
            this.quest = quest;
            this.expiresAt = expiresAt;
        }
    }
}

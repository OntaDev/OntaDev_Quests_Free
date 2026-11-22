// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.command;

import com.ontadev.libs.command.SubCommand;
import com.ontadev.libs.ioc.annotation.stereotype.Component;
import com.ontadev.libs.message.Placeholders;
import com.ontadev.quests.config.MessagesConfig;
import com.ontadev.quests.model.QuestRules;
import com.ontadev.quests.service.PageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Component
public class SubPage extends SubCommand {

    private final PageService pages;
    private final MessagesConfig messages;

    public SubPage(PageService pages, MessagesConfig messages) {
        super("page");
        this.pages = pages;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSender sender, Command command, String label, String... args) {
        if (args.length == 0) {
            messages.getPageUsage().send(sender);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "list":
                list(sender);
                break;
            case "info":
                info(sender, args);
                break;
            default:
                messages.getPageUsage().send(sender);
        }
    }

    private void list(CommandSender sender) {
        messages.getPageListHeader().send(sender, Placeholders.of("pages", String.valueOf(pages.lastPage())));
        for (int page = 1; page <= pages.lastPage(); page++) {
            PageService.PageInfo info = pages.info(page);
            messages.getPageListLine().send(sender, lineOf(info));
        }
    }

    private void info(CommandSender sender, String[] args) {
        Optional<Integer> page = args.length > 1 ? Args.integer(args[1]) : Optional.empty();
        if (page.isEmpty() || page.get() < 1) {
            messages.getPageUsage().send(sender);
            return;
        }

        PageService.PageInfo info = pages.info(page.get());
        messages.getPageInfo().send(sender, lineOf(info)
                .add("missing_quests", ids(info.getMissingQuests()))
                .add("missing_rewards", ids(info.getMissingRewards())));
    }

    private Placeholders lineOf(PageService.PageInfo info) {
        return new Placeholders()
                .add("page", info.getPage())
                .add("quests", info.getQuests())
                .add("max_quests", QuestRules.QUESTS_PER_PAGE)
                .add("rewards", info.getRewards())
                .add("max_rewards", QuestRules.REWARDS_PER_PAGE);
    }

    private String ids(List<Integer> ids) {
        if (ids.isEmpty()) return "-";
        return ids.stream().limit(20).map(String::valueOf).collect(Collectors.joining(", "))
                + (ids.size() > 20 ? " ... (+" + (ids.size() - 20) + ")" : "");
    }

    @Override
    public List<String> complete(CommandSender sender, String... args) {
        if (args.length == 1) return Stream.of("list", "info").collect(Collectors.toList());
        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            return IntStream.rangeClosed(1, pages.totalPages()).mapToObj(String::valueOf).collect(Collectors.toList());
        }
        return Args.none();
    }

    @Override
    public String getPermission(CommandSender sender, Command command, String label, String... args) {
        return "quests.page";
    }

    @Override
    public void noPermission(CommandSender sender, Command command, String label, String... args) {
        messages.getNoPermission().send(sender);
    }

    @Override
    public void sendUsage(CommandSender sender, Command command, String label, String... args) {
        messages.getPageUsage().send(sender);
    }
}

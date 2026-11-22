// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.command;

import com.ontadev.quests.config.MessagesConfig;
import com.ontadev.quests.menu.QuestMenu;
import com.ontadev.quests.service.ResetMode;
import com.ontadev.quests.service.ResetService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SubResetTest {
    private ResetService resets;
    private SubQuest quests;
    private QuestMenu menu;
    private MessagesConfig messages;
    private CommandSender console;
    private SubReset command;
    private MockedStatic<Bukkit> bukkit;

    @BeforeEach
    void setUp() {
        resets = mock(ResetService.class);
        when(resets.defaultSkips()).thenReturn(3);
        when(resets.reset(any(), anyInt())).thenReturn(CompletableFuture.completedFuture(null));
        quests = mock(SubQuest.class);
        menu = mock(QuestMenu.class);
        messages = mock(MessagesConfig.class, RETURNS_DEEP_STUBS);
        console = mock(CommandSender.class);
        when(console.hasPermission("quests.reset")).thenReturn(true);
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("reset-command-test"));
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(scheduler.runTask(eq(plugin), any(Runnable.class))).thenAnswer(call -> {
            ((Runnable) call.getArgument(1)).run();
            return null;
        });
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        command = new SubReset(resets, quests, menu, messages, plugin);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void everyModeRequiresAnExplicitConfirmation() {
        command.execute(console, null, "quests", "progress");
        command.execute(console, null, "quests", "skips", "0");
        command.execute(console, null, "quests", "all");

        verify(resets, never()).reset(any(), anyInt());
        verifyNoInteractions(quests, menu);
        verify(messages, times(3)).getResetConfirmation();
    }

    @Test
    void permissionIsCheckedEvenWithConfirm() {
        when(console.hasPermission("quests.reset")).thenReturn(false);
        command.execute(console, null, "quests", "all", "confirm");
        verify(messages.getNoPermission()).send(console);
        verifyNoInteractions(resets, quests, menu);
    }

    @Test
    void progressResetKeepsDefinitionsAndRefreshesTheCurrentPage() {
        command.execute(console, null, "quests", "progress", "confirm");
        verify(resets).reset(ResetMode.PROGRESS, 3);
        verify(quests, never()).clearPending();
        verify(menu).refreshAll(true);
    }

    @Test
    void fullResetAlsoDiscardsPendingQuestCreation() {
        command.execute(console, null, "quests", "all", "confirm");
        verify(resets).reset(ResetMode.ALL, 3);
        verify(quests).clearPending();
        verify(menu).refreshAll(true);
    }

    @Test
    void skipsAcceptBothConfiguredDefaultAndExplicitZero() {
        command.execute(console, null, "quests", "skips", "confirm");
        command.execute(console, null, "quests", "skips", "0", "confirm");
        verify(resets).reset(ResetMode.SKIPS, 3);
        verify(resets).reset(ResetMode.SKIPS, 0);
        verify(menu, times(2)).refreshAll(false);
    }

    @Test
    void invalidArgumentsNeverStartAReset() {
        for (String value : new String[]{"-1", "abc", "2147483648"})
            command.execute(console, null, "quests", "skips", value, "confirm");
        command.execute(console, null, "quests", "unknown", "confirm");
        command.execute(console, null, "quests", "all", "extra", "confirm");
        verify(resets, never()).reset(any(), anyInt());
    }

    @Test
    void failedResetDoesNotReportSuccessOrRefreshMenus() {
        when(resets.reset(any(), anyInt())).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("db down")));
        command.execute(console, null, "quests", "all", "confirm");
        verify(messages.getResetFailed()).send(console);
        verify(messages, never()).getResetDone();
        verifyNoInteractions(menu);
    }
}

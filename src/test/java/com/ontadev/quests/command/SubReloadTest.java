// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.command;

import com.ontadev.libs.config.YamlConfigLoader;
import com.ontadev.libs.message.Message;
import com.ontadev.libs.serialization.adapters.MessageAdapter;
import com.ontadev.quests.config.MessagesConfig;
import com.ontadev.quests.config.QuestsConfig;
import com.ontadev.quests.menu.MenuItemCache;
import com.ontadev.quests.menu.QuestMenu;
import com.ontadev.quests.service.LocalizeService;
import com.ontadev.quests.service.PlayerService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubReloadTest {
    @TempDir Path directory;
    private QuestsConfig config;
    private MessagesConfig messages;
    private LocalizeService localize;
    private MenuItemCache cache;
    private QuestMenu menu;
    private PlayerService players;
    private CommandSender console;
    private SubReload command;
    private MockedStatic<Bukkit> bukkit;

    @BeforeEach
    void setUp() throws Exception {
        Files.writeString(directory.resolve("config.yml"), "skipsPerPlayer: 3\nmenuTitle: Old title\n");
        Files.writeString(directory.resolve("messages.yml"), "reloadDone: Old message\n");
        Files.writeString(directory.resolve("ru_ru.json"), "{\"entity.minecraft.zombie\":\"Old zombie\"}");
        YamlConfigLoader loader = new YamlConfigLoader(directory, Map.of(Message.class, new MessageAdapter()));
        config = loader.loadFromClass(QuestsConfig.class);
        messages = loader.loadFromClass(MessagesConfig.class);
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("reload-test"));
        localize = new LocalizeService(plugin);
        cache = mock(MenuItemCache.class);
        menu = mock(QuestMenu.class);
        when(menu.reopenActiveMenus()).thenReturn(CompletableFuture.completedFuture(null));
        players = mock(PlayerService.class);
        console = mock(CommandSender.class);
        when(console.hasPermission("quests.reload")).thenReturn(true);
        command = new SubReload(config, messages, localize, cache, menu, players, plugin);
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        if (bukkit != null) bukkit.close();
    }

    private void changeFiles() throws Exception {
        Files.writeString(directory.resolve("config.yml"), "skipsPerPlayer: 7\nsaveIntervalSeconds: 30\nmenuTitle: New title\n");
        Files.writeString(directory.resolve("messages.yml"), "reloadDone: New message\n");
        Files.writeString(directory.resolve("ru_ru.json"), "{\"entity.minecraft.zombie\":\"New zombie\"}");
    }

    @Test
    void reloadsTheExistingConfigInstancesAndTranslationsFromConsole() throws Exception {
        changeFiles();
        command.execute(console, null, "quests");

        assertEquals(7, config.getSkipsPerPlayer());
        assertEquals(30, config.getSaveIntervalSeconds());
        assertEquals(List.of("New title"), config.getMenuTitle().getRawMessage());
        assertEquals(List.of("New message"), messages.getReloadDone().getRawMessage());
        assertEquals("New zombie", localize.entity(EntityType.ZOMBIE));
        verify(console).sendMessage("New message");
        verify(cache).clear();
        verify(players).rescheduleSaving();
        verify(menu).reopenActiveMenus();
    }

    @Test
    void invalidMessagesYamlRollsBackTheAlreadyReloadedConfig() throws Exception {
        changeFiles();
        Files.writeString(directory.resolve("messages.yml"), "reloadDone: [broken\n");
        command.execute(console, null, "quests");

        assertEquals(3, config.getSkipsPerPlayer());
        assertEquals(List.of("Old title"), config.getMenuTitle().getRawMessage());
        assertEquals(List.of("Old message"), messages.getReloadDone().getRawMessage());
        assertEquals("Old zombie", localize.entity(EntityType.ZOMBIE));
        verifyNoInteractions(cache, menu);
        verify(players, never()).rescheduleSaving();
    }

    @Test
    void invalidTranslationsKeepTheEntirePreviousConfiguration() throws Exception {
        changeFiles();
        Files.writeString(directory.resolve("ru_ru.json"), "{\"entity.minecraft.zombie\":{}}");
        command.execute(console, null, "quests");

        assertEquals(3, config.getSkipsPerPlayer());
        assertEquals(List.of("Old message"), messages.getReloadDone().getRawMessage());
        assertEquals("Old zombie", localize.entity(EntityType.ZOMBIE));
        verifyNoInteractions(cache, menu);
        verify(players, never()).rescheduleSaving();
    }

    @Test
    void permissionIsRequired() throws Exception {
        changeFiles();
        when(console.hasPermission("quests.reload")).thenReturn(false);
        command.execute(console, null, "quests");

        assertEquals(3, config.getSkipsPerPlayer());
        assertEquals("Old zombie", localize.entity(EntityType.ZOMBIE));
        verifyNoInteractions(players, cache, menu);
    }

    @Test
    void cannotReloadDuringAMassReset() throws Exception {
        changeFiles();
        when(players.isResetting()).thenReturn(true);
        command.execute(console, null, "quests");

        assertEquals(3, config.getSkipsPerPlayer());
        verifyNoInteractions(cache, menu);
        verify(players, never()).rescheduleSaving();
    }
}

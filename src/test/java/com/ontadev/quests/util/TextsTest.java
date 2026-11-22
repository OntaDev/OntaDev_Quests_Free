// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.util;

import com.ontadev.libs.message.Message;
import com.ontadev.libs.message.Placeholders;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextsTest {

    private List<String> render(Message template, Placeholders placeholders) {
        return Texts.render(template, placeholders).getRawMessage();
    }

    @Test
    void replacesPlaceholdersAndKeepsBlankSeparatorLines() {
        Message template = new Message("Прогресс: <progress>/<goal>", "", "Конец");

        assertEquals(Arrays.asList("Прогресс: 3/10", "", "Конец"),
                render(template, new Placeholders().add("progress", 3).add("goal", 10)));
    }

    @Test
    void aListPlaceholderExpandsIntoSeveralLines() {
        Message template = new Message("Нужно:", "<enchantments>");

        assertEquals(Arrays.asList("Нужно:", "Острота V", "Прочность III"),
                render(template, new Placeholders().add("enchantments", Arrays.asList("Острота V", "Прочность III"))));
    }

    @Test
    void anEmptyListPlaceholderRemovesItsLine() {
        Message template = new Message("Нужно:", "<enchantments>", "Статус");

        assertEquals(Arrays.asList("Нужно:", "Статус"),
                render(template, new Placeholders().add("enchantments", Collections.<String>emptyList())));
    }
}

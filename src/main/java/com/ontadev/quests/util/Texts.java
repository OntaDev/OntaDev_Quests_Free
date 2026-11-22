// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.util;

import com.ontadev.libs.message.Message;
import com.ontadev.libs.message.Placeholders;

import java.util.ArrayList;
import java.util.List;

public final class Texts {

    private Texts() {
    }

    /** Подставляет плейсхолдеры в шаблон; список-плейсхолдер раскрывается в несколько строк, пустой убирает строку. */
    public static Message render(Message template, Placeholders placeholders) {
        List<String> lines = new ArrayList<>();
        for (String line : template.getRawMessage()) {
            if (line.isEmpty()) {
                lines.add(line);
            } else {
                lines.addAll(placeholders.apply(line));
            }
        }
        return new Message(lines);
    }
}

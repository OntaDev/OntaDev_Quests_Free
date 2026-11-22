// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.command;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

final class Args {

    private Args() {
    }

    static Optional<Integer> integer(String text) {
        try {
            return Optional.of(Integer.parseInt(text));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    static List<String> none() {
        return Collections.emptyList();
    }

    static String join(String[] args, int from) {
        return String.join(" ", Arrays.copyOfRange(args, from, args.length));
    }

    public static int[] range(String text) {
        String[] parts = text.split("-", 2);
        Optional<Integer> from = integer(parts[0]);
        Optional<Integer> to = parts.length == 2 ? integer(parts[1]) : from;
        if (from.isEmpty() || to.isEmpty() || from.get() < 1 || to.get() < from.get()) return null;
        return new int[]{from.get(), to.get()};
    }
}

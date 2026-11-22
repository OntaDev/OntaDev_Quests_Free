// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class RevisionCacheTest {

    @Test
    void valueIsBuiltOncePerSourceObject() {
        RevisionCache<Integer, Object, String> cache = new RevisionCache<>();
        AtomicInteger builds = new AtomicInteger();
        Object source = new Object();

        String first = cache.get(1, source, () -> "v" + builds.incrementAndGet());
        String second = cache.get(1, source, () -> "v" + builds.incrementAndGet());

        assertSame(first, second);
        assertEquals(1, builds.get());
    }

    @Test
    void replacedSourceObjectRebuildsTheValue() {
        RevisionCache<Integer, Object, String> cache = new RevisionCache<>();

        cache.get(1, new Object(), () -> "old");
        String rebuilt = cache.get(1, new Object(), () -> "new");

        assertEquals("new", rebuilt);
        assertEquals(1, cache.size());
    }

    @Test
    void differentKeysAreIndependent() {
        RevisionCache<String, Object, String> cache = new RevisionCache<>();
        Object source = new Object();

        cache.get("a", source, () -> "A");
        cache.get("b", source, () -> "B");

        assertEquals("A", cache.get("a", source, () -> "x"));
        assertEquals(2, cache.size());
    }

    @Test
    void clearingRebuildsValuesEvenWhenTheQuestObjectDidNotChange() {
        RevisionCache<Integer, Object, String> cache = new RevisionCache<>();
        Object quest = new Object();
        cache.get(1, quest, () -> "old config");

        cache.clear();

        assertEquals(0, cache.size());
        assertEquals("new config", cache.get(1, quest, () -> "new config"));
    }
}

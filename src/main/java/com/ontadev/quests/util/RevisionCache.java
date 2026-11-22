// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Кеш готовых значений по ключу с проверкой, что значение собрано из того же исходного объекта: если квест или награду
 * пересохранили (в кеше уже другой объект), значение пересобирается само, без ручной инвалидации.
 */
public final class RevisionCache<K, S, V> {

    private final Map<K, Entry<S, V>> entries = new ConcurrentHashMap<>();

    public V get(K key, S source, Supplier<V> factory) {
        Entry<S, V> entry = entries.get(key);
        if (entry != null && entry.source == source) return entry.value;

        V value = factory.get();
        entries.put(key, new Entry<>(source, value));
        return value;
    }

    public int size() {
        return entries.size();
    }

    public void clear() {
        entries.clear();
    }

    private static final class Entry<S, V> {
        private final S source;
        private final V value;

        private Entry(S source, V value) {
            this.source = source;
            this.value = value;
        }
    }
}

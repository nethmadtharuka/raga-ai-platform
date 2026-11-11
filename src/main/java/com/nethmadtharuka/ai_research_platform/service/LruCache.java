package com.nethmadtharuka.ai_research_platform.service;

import java.util.LinkedHashMap;
import java.util.Map;

/** Small thread-safe LRU map, used to avoid repeat calls against the free-tier quota. */
final class LruCache<K, V> {
    private final Map<K, V> map;

    LruCache(int capacity) {
        this.map = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > capacity;
            }
        };
    }

    synchronized V get(K key) {
        return map.get(key);
    }

    synchronized void put(K key, V value) {
        map.put(key, value);
    }
}

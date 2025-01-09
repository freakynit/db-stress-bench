package com.freakynit.sql.db.stress.bench.utils.backports;

import java.util.HashMap;
import java.util.Map;

public class MapUtils {
    public static <K, V> Map<K, V> mapOf(Object... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid number of arguments. Key-value pairs must be even.");
        }

        Map<K, V> map = new HashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            map.put((K) entries[i], (V) entries[i + 1]);
        }

        return map;
    }

    // overloaded for faster construction for 1 k-v pair
    public static <K, V> Map<K, V> mapOf(K key, V value) {
        Map m = new HashMap();
        m.put(key, value);
        return m;
    }

    // overloaded for faster construction for 2 k-v pairs
    public static <K, V> Map<K, V> mapOf(K key1, V value1, K key2, V value2) {
        Map m = new HashMap();
        m.put(key1, value1);
        m.put(key2, value2);
        return m;
    }
}

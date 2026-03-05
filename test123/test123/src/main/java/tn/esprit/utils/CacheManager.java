package tn.esprit.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe minimal caching system for performance enhancements.
 */
public class CacheManager {

    private static final Map<String, Object> CACHE = new ConcurrentHashMap<>();

    public static void put(String key, Object value) {
        if (key != null && value != null) {
            CACHE.put(key, value);
        }
    }

    public static Object get(String key) {
        return CACHE.get(key);
    }

    public static void evict(String key) {
        if (key != null) {
            CACHE.remove(key);
        }
    }

    public static void clear() {
        CACHE.clear();
    }

    public static boolean contains(String key) {
        return CACHE.containsKey(key);
    }
}

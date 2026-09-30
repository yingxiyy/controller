package net.flex.dci.otn.controller.system.config.cache.util;

import lombok.extern.slf4j.Slf4j;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

/**
 * @version 1.0
 * @date 2022/3/14 17:38
 */
@Slf4j
public class EhcacheUtil {

    private static CacheManager manager;

    public static void setCacheManager(CacheManager cacheManager) {
        manager = cacheManager;
    }

    public static Object getObject(String cacheName, String key) {
        Cache cache = manager.getCache(cacheName);
        return cache.get(key) == null ? null : cache.get(key).getObjectValue();
    }

    public static Object get(String cacheName, Long key) {
        Cache cache = manager.getCache(cacheName);
        return cache.get(key) == null ? null : cache.get(key).getObjectValue();
    }

    public static String getString(String cacheName, String key) {
        Cache cache = manager.getCache(cacheName);
        return cache.get(key) == null ? null : (String) cache.get(key).getObjectValue();
    }

    public static void put(String cacheName, String key, Object value) {
        Cache cache = manager.getCache(cacheName);
        Element element = new Element(key, value);
        cache.put(element);
        cache.flush();
    }

    public static void remove(String cacheName, String key) {
        Cache cache = manager.getCache(cacheName);
        cache.remove(key);
    }

}

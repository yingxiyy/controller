package net.flex.dci.otn.controller.implement.nbi;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;

import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RestfulHashCodes {

    private static final int MAX_SIZE = 40;

    // 每个 path 一个缓存
    private static final Cache<String, Cache<String, Boolean>> CACHE =
            Caffeine.newBuilder()
                    .maximumSize(10_000) // path 数量上限
                    .build();

    public boolean add(String pathInfo, String json) {
        if (pathInfo == null || json == null) {
            log.debug("PathInfo or JSON is null, cannot add to cache.");
            return false;
        }
        String code = getCode(json);

        Cache<String, Boolean> pathCache = CACHE.get(pathInfo, k ->
                Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.SECONDS)
                        .maximumSize(MAX_SIZE)
                        .build()
        );

        // putIfAbsent 原子操作
        Boolean existing = pathCache.asMap().putIfAbsent(code, Boolean.TRUE);

        return existing == null;
    }

    private String getCode(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String s = Integer.toHexString(0xff & b);
                if (s.length() == 1) hex.append('0');
                hex.append(s);
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
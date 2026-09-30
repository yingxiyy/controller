package net.flex.dci.otc.controller.ne.manager.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 2026/5/23
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
@Component
@RequiredArgsConstructor
public class RegisterNeCache {

    private final Cache<String, Boolean> registerNeCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

    private final Cache<String, Boolean> syncNeCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

    public boolean tryRegisterLock(String neId) {
        return registerNeCache.asMap().putIfAbsent(neId, Boolean.TRUE) == null;
    }

    public void unlockRegister(String neId) {
        registerNeCache.invalidate(neId);
    }

    public boolean trySyncLock(String neId) {
        return syncNeCache.asMap().putIfAbsent(neId, Boolean.TRUE) == null;
    }

    public void unlockSync(String neId) {
        syncNeCache.invalidate(neId);
    }

    public boolean tryLock(String neId) {
        Boolean previous = registerNeCache.asMap().putIfAbsent(neId, Boolean.TRUE);
        return previous == null;
    }


    public void unlock(String neId) {
        registerNeCache.invalidate(neId);
    }
}

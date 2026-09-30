package net.flex.dci.otc.controller.ne.manager.monitor.health;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 12/11/2025 2:26 PM
 */
@Component
public class HealthStatusCache {

    private final Map<String, Boolean> cache = new ConcurrentHashMap<>();

    public boolean isConfirmedDead(String applicationId) {
        return Boolean.FALSE.equals(cache.get(applicationId));
    }

    public void markAsAlive(String applicationId) {
        cache.put(applicationId, Boolean.TRUE);
    }

    public void markAsDead(String applicationId) {
        cache.put(applicationId, Boolean.FALSE);
    }

    public void removeDead(String applicationId) {
        cache.remove(applicationId);
    }

    public boolean isAlive(String applicationId) {
        return Boolean.TRUE.equals(cache.get(applicationId));
    }
}

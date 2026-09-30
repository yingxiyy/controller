package net.flex.dci.otc.controller.ne.manager.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.TelemetryServerConfigDao;
import net.flex.dci.otc.mongo.mdoel.telemetry.TelemetryConfig;
import org.springframework.stereotype.Component;

/**
 * @author musa
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelemetryConfigCache {

    private static final String CACHE_KEY = "telemetryConfig";

    private final TelemetryServerConfigDao telemetryServerConfigDao;

    private final Cache<String, TelemetryConfig> cache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(5)
            .build();

    public TelemetryConfig getTelemetryConfig() {
        TelemetryConfig cached = cache.getIfPresent(CACHE_KEY);
        if (cached == null) {
            synchronized (this) {
                cached = cache.getIfPresent(CACHE_KEY);
                if (cached == null) {
                    cached = telemetryServerConfigDao.getTelemetryConfig();
                    cache.put(CACHE_KEY, cached);
                    log.debug("Refreshed TelemetryConfig from DB");
                }
            }
        }
        return cached;
    }
}
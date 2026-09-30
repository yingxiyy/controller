package net.flex.dci.otc.controller.ne.manager.core.kafka.service.impl;

import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.NeImplementState.ALLOCATE;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.components.balancer.TelemetryBalancer;
import net.flex.dci.otc.controller.ne.manager.core.kafka.model.ElementChange;
import net.flex.dci.otc.controller.ne.manager.core.kafka.service.NeImplementStateService;
import net.flex.dci.otc.controller.ne.manager.service.NeManager;
import org.springframework.stereotype.Component;

/**
 * 2025/6/21
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class NeImplementStateServiceImpl implements NeImplementStateService {


    private final TelemetryBalancer telemetryBalancer;
    private final NeManager neManager;

    private final LoadingCache<String, Boolean> hasTelemetryCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Boolean>() {
                @Override
                public Boolean load(String neId) {
                    return telemetryBalancer.getTelemetryServerByNeId(neId) != null;
                }
            });

    private final Semaphore semaphore = new Semaphore(5);

    private final Set<String> processingNeIds = ConcurrentHashMap.newKeySet();

    @Override
    public void handleStateChange(String neId, String implementState) {
        handleStateChanges(Collections.singletonList(
                createElementChange(neId, implementState)
        ));

//        log.info("ne:{} implement state change the implementState:{}", neId, implementState);
//        if (implementState.equals(ALLOCATE)) {
//            log.info("current ne implement state is allocate,current nothing to do");
//            return;
//        }
//
//        TelemetryServer telemetryServer = telemetryBalancer.getTelemetryServerByNeId(neId);
//        if (telemetryServer == null) {
//            log.info("current ne:{} not connected telemetry collector,assign a new telemetry",
//                    neId);
//            AsynchronousExecutor.execute(() -> {
//                log.debug("assign telemetry to ne:{}", neId);
//                neManager.assignTelemetry2NeByNeIds(Collections.singleton(neId));
//            });
//        }

    }

    @Override
    public void handleStateChanges(Map<String, String> latestStates) {
        
    }

    public void handleStateChanges(List<ElementChange> changes) {
        if (changes == null || changes.isEmpty()) {
            return;
        }

        log.info("Batch processing {} implement state changes", changes.size());

        Map<String, String> latestStates = new LinkedHashMap<>();
        for (ElementChange change : changes) {
            String neId = change.getNeState().getNodeId();
            String state = change.getNeState().getImplementState();

            if (state.equals(ALLOCATE)) {
                continue;
            }
            latestStates.put(neId, state);
        }

        log.info("After dedup: {} neIds", latestStates.size());

        Set<String> needConfig = new HashSet<>();
        for (String neId : latestStates.keySet()) {
            if (!processingNeIds.add(neId)) {
                log.debug("ne:{} is already being processed, skip", neId);
                continue;
            }

            try {
                Boolean hasTelemetry = hasTelemetryCache.get(neId);
                if (!hasTelemetry) {
                    needConfig.add(neId);
                }
            } catch (Exception e) {
                log.error("Failed to check cache for ne:{}", neId, e);
                processingNeIds.remove(neId);
            }
        }
        if (!needConfig.isEmpty()) {
            processBatch(needConfig);
        }

        latestStates.keySet().forEach(processingNeIds::remove);
    }


    private void processBatch(Set<String> neIds) {
        try {
            if (!semaphore.tryAcquire(30, TimeUnit.SECONDS)) {
                log.warn("Too many tasks, skip {} neIds", neIds.size());
                return;
            }

            try {
                log.info("Batch assigning telemetry to {} neIds", neIds.size());
                neManager.assignTelemetry2NeByNeIds(neIds);
                neIds.forEach(hasTelemetryCache::refresh);
            } finally {
                semaphore.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private ElementChange createElementChange(String neId, String implementState) {
        ElementChange change = new ElementChange();
        // 设置 neId 和 implementState
        return change;
    }
}

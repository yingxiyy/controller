package net.flex.dci.otc.controller.ne.manager.core.kafka.handler.impl;

import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.NeImplementState.IMPLEMENT;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.NeImplementState.NE_STATE_CHANGE;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.core.kafka.handler.NeChangeHandler;
import net.flex.dci.otc.controller.ne.manager.core.kafka.model.ElementChange;
import net.flex.dci.otc.controller.ne.manager.core.kafka.model.NeState;
import net.flex.dci.otc.controller.ne.manager.service.NeManager;
import net.flex.dci.otc.mongo.dao.TelemetryDao;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 2025/6/21
 *
 * when ne implement state is implement,detect config telemetry or not
 *
 * @author musa
 * @version 1.0
 **/
@Component
@RequiredArgsConstructor
@Slf4j
public class NeImplementChangeHandler implements NeChangeHandler {

    private final NeManager neManager;

    private final TelemetryDao telemetryDao;


    private final Cache<String, Boolean> connectedNeCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(10000)
            .build();

    @Override
    public void handleStateChange(ElementChange elementChange) {
        log.debug("handle ne implement state change ");

        handleStateChanges(Collections.singletonList(elementChange));
    }

    @Override
    public String msgType() {
        return NE_STATE_CHANGE;
    }

    @Override
    public void handleStateChanges(List<ElementChange> elementChanges) {
        if (elementChanges == null || elementChanges.isEmpty()) {
            return;
        }

        Set<String> latestNeIds = new HashSet<>();
        for (ElementChange change : elementChanges) {
            NeState state = change.getNeState();
            if (state == null || !StringUtils.hasText(state.getNodeId())
                    || state.getImplementState() == null || !IMPLEMENT.equals(
                    state.getImplementState())) {
                continue;
            }
            latestNeIds.add(state.getNodeId());
        }

        if (latestNeIds.isEmpty()) {
            log.debug("No valid implement state changes in batch of {}", elementChanges.size());
            return;
        }

        log.info("NeImplementChangeHandler batch: {} -> {} after dedup",
                elementChanges.size(), latestNeIds.size());
        Set<String> needConfig = getNeedConfig(latestNeIds);
        if (!needConfig.isEmpty()) {
            neManager.assignTelemetry2NeByNeIds(needConfig);
            for (String neId : needConfig) {
                connectedNeCache.put(neId, true);
            }
        }

    }

    private Set<String> getNeedConfig(Set<String> latestNeIds) {
        Set<String> needConfig = new HashSet<>();
        Set<String> missingInCache = new HashSet<>();

        for (String neId : latestNeIds) {
            Boolean cached = connectedNeCache.getIfPresent(neId);
            if (cached == null) {
//                connectedNeCache.put(neId, false);
                missingInCache.add(neId);
            } else if (Boolean.FALSE.equals(cached)) {
                needConfig.add(neId);
            }
        }

        if (!missingInCache.isEmpty()) {
            try {
                Map<String, String> telemetryServers = telemetryDao.batchGetTelemetryByNodeIds(
                        new ArrayList<>(missingInCache));

                for (String neId : missingInCache) {
                    boolean alreadyConfigured = telemetryServers.containsKey(neId);
                    connectedNeCache.put(neId, alreadyConfigured);
                    if (!alreadyConfigured) {
                        needConfig.add(neId);
                    }
                }
                log.debug("Refreshed telemetry cache from DB, configured={}, needConfig={}",
                        telemetryServers.size(), needConfig.size());
            } catch (Exception e) {
                log.error("batch query telemetry failed", e);
            }
        }

        return needConfig;
    }
}

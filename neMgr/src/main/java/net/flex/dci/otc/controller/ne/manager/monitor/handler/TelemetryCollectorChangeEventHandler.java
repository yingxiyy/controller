package net.flex.dci.otc.controller.ne.manager.monitor.handler;

import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils.convert2TelemetryServer;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.enums.MapperType;
import net.flex.dci.otc.controller.ne.manager.monitor.health.HealthDelayCheckQueue;
import net.flex.dci.otc.controller.ne.manager.monitor.health.HealthStatusCache;
import net.flex.dci.otc.controller.ne.manager.monitor.health.TelemetryServerHealthChecker;
import net.flex.dci.otc.controller.ne.manager.service.NeManager;
import net.flex.dci.otc.controller.ne.manager.utils.AsynchronousExecutor;
import net.flex.dci.otc.controller.ne.manager.utils.FriendlyNameGenerator;
import net.flex.dci.otc.controller.rpc.client.enums.HealthStatus;
import net.flex.dci.otc.mongo.dao.TelemetryDao;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.telemetry.manager.TelemetryServer;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 6/9/2025 5:08 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TelemetryCollectorChangeEventHandler extends AbstractMapperChangeEventHandler {

    private final TelemetryDao telemetryDao;

    private final NeManager neManager;

    private final TelemetryServerHealthChecker telemetryServerHealthChecker;

    private final HealthStatusCache healthStatusCache;

    private final HealthDelayCheckQueue healthDelayCheckQueue;

    private final ScheduledExecutorService rebalanceScheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> pendingRebalanceTask;
    private static final long REBALANCE_DELAY_MS = 3000;

    @Override
    public MapperType mapperType() {
        return MapperType.TELEMETRY_COLLECTOR;
    }

    @Override
    public void handleNodeRemove(InstanceDetails instanceDetails) {
        log.info("current telemetry collector removed,the instance is:{}", instanceDetails);
        if (!leaderElector.isLeaderShip()) {
            log.debug("not leader, skip telemetry server remove handling");
            return;
        }
        String telemetryCollectorId = FriendlyNameGenerator.generateTeleMapperName(instanceDetails);
        boolean isExisted = telemetryDao.existsServer(telemetryCollectorId);
        if (!isExisted) {
            healthStatusCache.removeDead(telemetryCollectorId);
            log.warn("current telemetry collector:{} is already deleted,do nothing and discard it",
                    telemetryCollectorId);
            return;
        }
        if (healthStatusCache.isConfirmedDead(telemetryCollectorId)) {
            performCleanup(telemetryCollectorId);
            return;
        }
        String taskId = healthDelayCheckQueue.submit(telemetryCollectorId,
                () -> performCleanup(telemetryCollectorId), 30, TimeUnit.SECONDS);
        log.info("Submitted delayed cleanup task {} for telemetry {}", taskId,
                telemetryCollectorId);
        AsynchronousExecutor.execute(() -> {
            TelemetryServer telemetryServer = telemetryDao.getTelemetryServerById(
                    telemetryCollectorId);
            if (telemetryServer == null) {
                healthDelayCheckQueue.cancel(taskId);
                return;
            }

            HealthStatus healthStatus = telemetryServerHealthChecker.checkHealth(instanceDetails);
            if (healthStatus == HealthStatus.UP) {
                log.warn("telemetry :{} still alive,cancel cleanup", telemetryCollectorId);
                healthDelayCheckQueue.cancel(taskId);
                healthStatusCache.markAsAlive(telemetryCollectorId);

            } else {
                log.info("Health check confirm telemetry {} is deade", telemetryCollectorId);
                healthStatusCache.markAsDead(telemetryCollectorId);
            }

        });


    }

    private void performCleanup(String telemetryCollectorId) {
        if (healthStatusCache.isAlive(telemetryCollectorId)) {
            log.info("telemetry server {} is alive again,skip clean up", telemetryCollectorId);
            return;
        }
        if (!telemetryDao.existsServer(telemetryCollectorId)) {
            log.warn("Telemetry {} already deleted, skip cleanup.", telemetryCollectorId);
            healthStatusCache.removeDead(telemetryCollectorId);
            return;
        }
        TelemetryServer telemetryServer = telemetryDao.getTelemetryServerById(telemetryCollectorId);
        telemetryDao.deleteTelemetryServer(telemetryCollectorId);
        Set<String> connectedNeIds = Optional.ofNullable(telemetryServer.getNe()).orElse(
                        Collections.emptyList()).stream().map(ne -> ne.getNodeId().getValue())
                .collect(Collectors.toSet());
        reAssignTelemetryServer2Ne(connectedNeIds);

        healthStatusCache.removeDead(telemetryCollectorId);
        log.info("Cleanup finished for telemetry: {}", telemetryCollectorId);
    }

    private void reAssignTelemetryServer2Ne(Set<String> connectedNeIds) {
        log.info("reAssign Telemetry Server to ne:{}", connectedNeIds);
        neManager.assignTelemetry2NeByNeIds(connectedNeIds);

    }


    @Override
    public void handleNodeAdded(InstanceDetails instanceDetails) {
        log.info("current telemetry collector added the instance is:{}", instanceDetails);
        if (!leaderElector.isLeaderShip()) {
            log.debug("not leader, skip telemetry server added handling");
            return;
        }
        String telemetryCollectorId = FriendlyNameGenerator.generateTeleMapperName(instanceDetails);
        String pendingTaskId = healthDelayCheckQueue.getPendingTaskId(telemetryCollectorId);
        if (pendingTaskId != null) {
            log.info("telemetry collector {} re-registered, cancel pending cleanup task {}",
                    telemetryCollectorId, pendingTaskId);
            healthDelayCheckQueue.cancel(pendingTaskId);
            healthStatusCache.markAsAlive(telemetryCollectorId);
        } else if (telemetryDao.existsServer(telemetryCollectorId)) {
            log.warn("current Telemetry sever is already existed ,do nothing and discard it");
            return;
        }
        TelemetryServer telemetryServer = convert2TelemetryServer(instanceDetails);
        log.debug("record a new Telemetry Server:{} ", telemetryServer);
        telemetryDao.createTelemetryServer(telemetryServer);
        log.info("finish to add a new telemetry Server");
        //star to rebalance the telemetry Server assign
        triggerDefferedTelemetryRebalance(instanceDetails, telemetryCollectorId);
    }

    private synchronized void triggerDefferedTelemetryRebalance(InstanceDetails instanceDetails,
            String telemetryCollectorId) {
        if (pendingRebalanceTask != null && !pendingRebalanceTask.isDone()) {
            pendingRebalanceTask.cancel(false);
        }

        pendingRebalanceTask = rebalanceScheduler.schedule(() -> {
            HealthStatus healthStatus = telemetryServerHealthChecker.checkHealth(instanceDetails);
            if (healthStatus != HealthStatus.UP) {
                log.warn("New telemetry {} health check failed, skip rebalance",
                        telemetryCollectorId);
                return;
            }
            neManager.rebalanceAllTelemetryServers();
            log.info("Rebalance finished after new telemetry {} joined", telemetryCollectorId);
        }, REBALANCE_DELAY_MS, TimeUnit.MILLISECONDS);
    }


    @PreDestroy
    public void shutdown() {
        log.info("Shutting down telemetry rebalance scheduler");
        rebalanceScheduler.shutdown();
        try {
            if (!rebalanceScheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                rebalanceScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            rebalanceScheduler.shutdownNow();
        }
    }

}

package net.flex.dci.otc.controller.ne.manager.monitor.handler;

import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils.convert2Adapter;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils.partitionList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.ne.NeStatus;
import net.flex.dci.otc.common.model.ne.NeStatusMessage;
import net.flex.dci.otc.common.model.type.NeStatusType;
import net.flex.dci.otc.controller.ne.manager.cache.RegisterNeCache;
import net.flex.dci.otc.controller.ne.manager.core.NeManagerService;
import net.flex.dci.otc.controller.ne.manager.enums.MapperType;
import net.flex.dci.otc.controller.ne.manager.monitor.health.AdapterHealthChecker;
import net.flex.dci.otc.controller.ne.manager.monitor.health.HealthDelayCheckQueue;
import net.flex.dci.otc.controller.ne.manager.monitor.health.HealthStatusCache;
import net.flex.dci.otc.controller.ne.manager.service.NeManager;
import net.flex.dci.otc.controller.ne.manager.utils.AsynchronousExecutor;
import net.flex.dci.otc.controller.rpc.client.enums.HealthStatus;
import net.flex.dci.otc.mongo.dao.AdapterDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dto.NodeInfoDto;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import net.flex.dci.otn.controller.tools.kafka.service.StatusMessageSender;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CommunicationStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.SupervisionStatusType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 6/9/2025 5:08 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AdapterChangeEventHandler extends AbstractMapperChangeEventHandler {

    private final AdapterDao adapterDao;

    private final PhyNodeDao phyNodeDao;

    private final NeManager neManager;

    private final NeManagerService neManagerService;

    private final AdapterHealthChecker adapterHealthChecker;

    private final HealthStatusCache healthStatusCache;

    private final HealthDelayCheckQueue healthDelayCheckQueue;

    private final RegisterNeCache registerNeCache;

    private final Semaphore REGISITER_SEMAPHORE = new Semaphore(10);

    private final int BatchSize = 10;

    private final ScheduledExecutorService rebalanceScheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> pendingRebalanceTask;
    private static final long REBALANCE_DELAY_MS = 3000;

    @SneakyThrows
    @Override
    public void handleNodeAdded(InstanceDetails instance) {

        log.info("a adapter added,the adapter instance is :{}", instance);
        if (!leaderElector.isLeaderShip()) {
            log.debug("not leader, skip adapter added handling");
            return;
        }
        String adapterName = instance.getId();
        String pendingTaskId = healthDelayCheckQueue.getPendingTaskId(adapterName);
        if (null != pendingTaskId) {
            log.info("adapter {} re-registered,cancel pending cleanup task:{}", adapterName,
                    pendingTaskId);
            healthDelayCheckQueue.cancel(pendingTaskId);
            healthStatusCache.markAsAlive(adapterName);
        } else if (adapterDao.existsServer(adapterName)) {
            log.warn("duplicating adapter:{},discard it and do nothing", adapterName);
            return;
        }
        Adapter adapter = convert2Adapter(instance);
        log.debug("load adapter:{} into db", adapter);
        adapterDao.saveAdapter(adapter);
//        neManagerService.manageNe();
        triggerDeferredManageNe();
        log.info("finish to add a new adapter");
    }

    /**
     * trigger deferred manage ne
     */
    private void triggerDeferredManageNe() {
        if (pendingRebalanceTask != null && !pendingRebalanceTask.isDone()) {
            pendingRebalanceTask.cancel(false);
        }
        pendingRebalanceTask = rebalanceScheduler.schedule(() -> {
            try {
                log.info("Start global rebalance after adapter change debounce");
                neManagerService.manageNe();
                log.info("Global rebalance finished successfully");
            } catch (Exception e) {
                log.error("Global rebalance failed", e);
            }
        }, REBALANCE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void handleNodeRemove(InstanceDetails instance) {
        log.info("a adapter remove,the adapter instance is:{}", instance);
        if (!leaderElector.isLeaderShip()) {
            log.debug("not leader, skip adapter added handling");
            return;
        }
        String adapterId = instance.getId();
        if (healthStatusCache.isConfirmedDead(adapterId)) {
            log.info("Adapter {} is confirmed dead,proceed with cleanup", adapterId);
            performCleanup(instance);
            return;
        }

        String taskId = healthDelayCheckQueue.submit(adapterId, () -> performCleanup(instance),
                30,
                TimeUnit.SECONDS);
        log.info("Submitted delayed cleanup task id :{} for adapter id:{}", taskId, adapterId);

        AsynchronousExecutor.execute(() -> {
            Adapter adapter = adapterDao.getAdapterById(instance.getId());
            if (adapter == null) {
                healthDelayCheckQueue.cancel(taskId);
                return;
            }

            HealthStatus healthStatus = adapterHealthChecker.checkHealth(adapter);
            if (healthStatus == HealthStatus.UP) {
                log.warn("Adapter {} is alive,cancel cleanup", adapter);
                healthDelayCheckQueue.cancel(taskId);
                healthStatusCache.markAsAlive(adapterId);
            } else {
                log.info("Health check confirms adapter {} is dead ", adapterId);
                healthStatusCache.markAsDead(adapterId);
            }
        });

    }

    private void performCleanup(InstanceDetails instance) {
        log.debug("adapter performing cleanup");
        String adapterId = instance.getId();
        if (healthStatusCache.isAlive(adapterId)) {
            log.info("adapter {} is alive again,skip clean up", adapterId);
            return;
        }
        Adapter adapter = adapterDao.getAdapterById(instance.getId());
        if (adapter == null) {
            log.warn("the adapter:{} is already deleted,discard it and do noting",
                    instance.getId());
            healthStatusCache.removeDead(instance.getId());
            return;
        }
        Set<String> connectedNeIds = Optional.ofNullable(adapter.getNe())
                .orElse(Collections.emptyList())
                .stream().map(ne -> ne.getNodeId().getValue())
                .collect(Collectors.toSet());

        offlineAndReconnectedNeByIds(connectedNeIds);
        adapterDao.deleteAdapter(instance.getId());
        healthStatusCache.removeDead(instance.getId());
        log.info("finish to handle the adapter delete");
    }

    private void offlineAndReconnectedNeByIds(Set<String> connectedNeIds) {
        log.debug("offline current ne and reconnected the ne");
        if (connectedNeIds.isEmpty()) {
            return;
        }
        List<NodeInfoDto> phyNodeInfoDto = phyNodeDao.listConfigNodeINfoByIds(
                new ArrayList<>(connectedNeIds));
        Map<String, String> neFriendlyNameMap = phyNodeInfoDto.stream().collect(
                Collectors.toMap(NodeInfoDto::getId,
                        NodeInfoDto::getFriendlyName));
        List<String> neIds = phyNodeInfoDto.stream().map(NodeInfoDto::getId)
                .collect(Collectors.toList());
        AsynchronousExecutor.execute(() -> {
            sendNeLossTrackNotif(connectedNeIds);
            startReRegisterNe(neIds, neFriendlyNameMap);
        });
    }

    private void startReRegisterNe(List<String> neIds, Map<String, String> neFriendlyNameMap) {
        List<List<String>> batches = partitionList(neIds, BatchSize);
        phyNodeDao.batchUpdateConfigPhyNodeSupervisionStateAndAdminStateAndCommunicateStatus(neIds,
                SupervisionStatusType.Monitoring, AdminStatus.Up,
                CommunicationStatusType.Broken);
        for (int i = 0; i < batches.size(); i++) {
            List<String> batch = batches.get(i);
            log.info("Processing reRegister batch {}/{}: {} ne(s)", i + 1, batches.size(),
                    batch.size());

            List<CompletableFuture<Void>> batchFutures = new ArrayList<>();
            for (String neId : batch) {
                if (!registerNeCache.tryRegisterLock(neId)) {
                    log.info("[reRegister duplicate] NE {} is already processing, skip", neId);
                    continue;
                }
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        REGISITER_SEMAPHORE.acquire();
                        log.debug("reRegister ne id:{}", neId);
//                        phyNodeDao.updateConfigPhyNodeCommunicateStatus(neId,
//                                CommunicationStatusType.Broken);
                        neManager.reRegisterNe(neId, neFriendlyNameMap.get(neId));
                        log.debug("reRegister ne id:{} completed", neId);
                    } catch (Exception e) {
                        log.error("Failed to reRegister ne id:{} the reason is:{}",
                                neId, e.getMessage(), e);
                    } finally {
                        REGISITER_SEMAPHORE.release();
                        registerNeCache.unlockRegister(neId);
                    }
                }, AsynchronousExecutor.getRegisterExecutor());

                batchFutures.add(future);
            }

            CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0]))
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Some re-registration tasks failed", ex);
                        } else {
                            log.info("All {} NEs re-registered successfully", neIds.size());
                        }
                    });
        }


    }


    private void sendNeLossTrackNotif(Set<String> neIds) {
        log.debug("send ne loss Track notification ,neIds:{}", neIds);
        List<NeStatus> neStatuses = neIds.stream().map(neId -> NeStatus.builder().neStatus(
                NeStatusType.LOSS_TRACK).neId(neId).build()).collect(Collectors.toList());
        StatusMessageSender.sendMessage(NeStatusMessage.builder().neStatuses(neStatuses).build());
    }


    @Override
    public MapperType mapperType() {
        return MapperType.ADAPTER;
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down adapter rebalance scheduler");
        rebalanceScheduler.shutdown();
        try {
            if (!rebalanceScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                rebalanceScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            rebalanceScheduler.shutdownNow();
        }
    }

}

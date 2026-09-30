package net.flex.dci.otc.controller.ne.manager.monitor.state;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.cache.RegisterNeCache;
import net.flex.dci.otc.controller.ne.manager.components.balancer.AdapterBalancer;
import net.flex.dci.otc.controller.ne.manager.components.balancer.TelemetryBalancer;
import net.flex.dci.otc.controller.ne.manager.components.communicateState.CommunicateStateUpdater;
import net.flex.dci.otc.controller.ne.manager.core.service.NeResourceService;
import net.flex.dci.otc.controller.ne.manager.dto.NeRegisteredInfo;
import net.flex.dci.otc.controller.ne.manager.service.NeManager;
import net.flex.dci.otc.controller.ne.manager.utils.AsynchronousExecutor;
import net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dto.NodeInfoDto;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.SupervisionStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 6/12/2025 2:22 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NeStateMonitorImpl implements NeStateMonitor {

    private final PhyNodeDao phyNodeDao;

    private final AdapterBalancer adapterBalancer;

    private final TelemetryBalancer telemetryBalancer;

    private final NeManager neManager;

    private final NeResourceService neResourceService;

    private final RegisterNeCache registerNeCache;

    @Value("${ne.state.monitor.max-concurrent:20}")
    private int maxConcurrentTasks;

    private final CommunicateStateUpdater communicateStateUpdater;


    private Semaphore stateMonitorSemaphore;


    @PostConstruct
    public void init() {
        stateMonitorSemaphore = new Semaphore(maxConcurrentTasks);
        log.info("State monitor semaphore initialized with max concurrent: {}", maxConcurrentTasks);
    }

    @Override
    public void checkAndSynchronizeState() {
        log.info("check current system service managed ne state and synchronize ne state");
        List<String> monitoringNeIds = phyNodeDao.listAllExistIpAndMonitoringNeIds();
        if (CollectionUtils.isEmpty(monitoringNeIds)) {
            log.trace("no monitoring ne to check, do nothing");
            return;
        }

        //check adapter connect and reconnect
        NeRegisteredInfo neRegisteredInfo = adapterBalancer.getNeRegisteredInfo(monitoringNeIds);
        ListenableFuture<?> reconnectFuture = AsynchronousExecutor.submit(() -> {
            checkAndReconnectAdapter(neRegisteredInfo);
        }, AsynchronousExecutor.getRegisterExecutor());
        ListenableFuture<?> assignTelemetryFuture = AsynchronousExecutor.submit(() -> {
            checkAndReAssignTelemetry(neRegisteredInfo.getRegisteredNes());
        }, AsynchronousExecutor.getTelemetryExecutor());
        ListenableFuture<List<Object>> allFutures = Futures.allAsList(
                Arrays.asList(reconnectFuture, assignTelemetryFuture)
        );
        Futures.addCallback(allFutures, new FutureCallback<List<Object>>() {
            @Override
            public void onSuccess(@Nullable List<Object> result) {
                log.info("Both check and redo tasks completely successfully");
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("One or both check tasks failed", t);
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    public void checkAndSynchronizeStateBySlice(int sliceIndex, int sliceCount) {
        log.info("Slice [{}] started inspection,total slice:{}", sliceIndex, sliceCount);
        long startTime = System.currentTimeMillis();
        List<String> monitoringNeIds = phyNodeDao.listAllExistIpAndMonitoringNeIds();
        if (CollectionUtils.isEmpty(monitoringNeIds)) {
            log.trace("no monitoring ne to check, do nothing");
            return;
        }
        List<String> sliceNeIds = monitoringNeIds.stream()
                .filter(neId -> Math.abs(neId.hashCode() % sliceCount) == sliceIndex)
                .collect(Collectors.toList());
        if (sliceNeIds.isEmpty()) {
            log.warn("slice [{}] have no ne ,skip", sliceIndex);
            return;
        }
        NeRegisteredInfo neRegisteredInfo = adapterBalancer.getNeRegisteredInfo(sliceNeIds);
        try {
            checkAndReconnectAdapter(neRegisteredInfo);
            checkAndReAssignTelemetry(neRegisteredInfo.getRegisteredNes());
            log.info("slice [{}]inspection finish，cost：{}ms", sliceIndex,
                    System.currentTimeMillis() - startTime);
        } catch (Throwable t) {
            log.error("slice [{}]inspection failed", sliceIndex, t);
        }

    }

    @Override
    public void checkAndSynchronizeStateBySlice(int sliceIndex, int sliceCount,
            NeRegisteredInfo neRegisteredInfo) {
        log.info("Slice [{}] started inspection,total slice:{}", sliceIndex, sliceCount);
        long startTime = System.currentTimeMillis();
        if (neRegisteredInfo.getRegisteredNes().isEmpty()
                && neRegisteredInfo.getNotRegisteredNes().isEmpty()) {
            log.trace("slice [{}] no ne to process, skip", sliceIndex);
            return;
        }
        try {
            checkAndReconnectAdapter(neRegisteredInfo);
            checkAndReAssignTelemetry(neRegisteredInfo.getRegisteredNes());
            log.info("slice [{}]inspection finish，cost：{}ms", sliceIndex,
                    System.currentTimeMillis() - startTime);
        } catch (Throwable t) {
            log.error("slice [{}]inspection failed", sliceIndex, t);
        }
    }

    /**
     * when ne implement state is implement should check and config the telemetry configuration
     *
     * @param registeredNeIds
     */
    private void checkAndReAssignTelemetry(Set<String> registeredNeIds) {
        try {
            log.debug("check current ne:{} is assign telemetry or not and reassign telemetry to ne",
                    registeredNeIds);

            List<String> implementNeIds = phyNodeDao.listImplementNeIds(registeredNeIds);
            List<String> connectNeIds = telemetryBalancer.getConnectNeIds();
            log.debug("current connect ne ids:{}", connectNeIds);
            Set<String> needConfigNeIds = NeManagerUtils.getDifferenceSetByGuava(
                    new HashSet<>(implementNeIds), new HashSet<>(connectNeIds));
            log.debug("need config ne ids:{}", needConfigNeIds);
            Set<String> registeredNeedConfigNeIds = NeManagerUtils.getIntersectionSetByGuava(
                    needConfigNeIds, new HashSet<>(registeredNeIds));
            Set<String> realNeedRegisterNeIds = registeredNeedConfigNeIds.stream()
                    .filter(neId -> adapterBalancer.getAdapterForNe(neId) != null).collect(
                            Collectors.toSet());
            log.debug("need config ne Ids:{}", registeredNeedConfigNeIds);

            if (!realNeedRegisterNeIds.isEmpty()) {
                AsynchronousExecutor.execute(() -> {
                    log.debug("assign telemetry to ne:{}", realNeedRegisterNeIds);
                    neManager.assignTelemetry2NeByNeIds(realNeedRegisterNeIds);
                }, AsynchronousExecutor.getTelemetryExecutor());
            }
        } catch (Exception ex) {
            log.warn("failed to check and reassign telemetry:{}", ex.getMessage(), ex);
        }
    }

    private void checkAndReconnectAdapter(NeRegisteredInfo neRegisteredInfo) {
        Set<String> needRegisterId = neRegisteredInfo.getNotRegisteredNes();
        Set<String> registeredNeIds = neRegisteredInfo.getRegisteredNes();

        log.debug("need registered neId:{} and registeredNeIds:{}", needRegisterId,
                registeredNeIds);
        if (needRegisterId.isEmpty() && registeredNeIds.isEmpty()) {
            log.info("there no need reconnect ne to connect adapter,do nothing");
            return;
        }

        Set<String> allNeIdsNeedingFriendlyName = new HashSet<>();
        allNeIdsNeedingFriendlyName.addAll(needRegisterId);
        allNeIdsNeedingFriendlyName.addAll(neRegisteredInfo.getNeedSynchronizedNes());
        Map<String, String> friendlyNameMap = new HashMap<>();
        if (!allNeIdsNeedingFriendlyName.isEmpty()) {
//            log.info("all config node is:{}", allNeIdsNeedingFriendlyName);
            List<NodeInfoDto> nodes = phyNodeDao.listConfigNodeINfoByIds(
                    new ArrayList<>(allNeIdsNeedingFriendlyName));
            for (NodeInfoDto node : nodes) {
//                log.info("current node :{} friendlyName:{}", node.getId(), node.getFriendlyName());
                friendlyNameMap.put(node.getId(), node.getFriendlyName());
            }
        }
        List<String> needRegisterList = new ArrayList<>(needRegisterId);
        List<List<String>> registerBatches = partitionList(needRegisterList, maxConcurrentTasks);
        for (List<String> batch : registerBatches) {
            List<CompletableFuture<Void>> batchFutures = new ArrayList<>();
            for (String neId : batch) {
                if (!registerNeCache.tryRegisterLock(neId)) {
                    log.info("[supervision duplicate] NE {} is on registering，skip", neId);
                    continue;
                }

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        stateMonitorSemaphore.acquire();
                        log.debug("reRegister ne:{} start", neId);
                        neManager.reRegisterNe(neId, friendlyNameMap.getOrDefault(neId, neId));
                        log.debug("reRegister ne:{} end", neId);
                    } catch (Exception e) {
                        log.error("reRegister ne:{} failed", neId, e);
                    } finally {
                        stateMonitorSemaphore.release();
                        registerNeCache.unlockRegister(neId);
                    }
                }, AsynchronousExecutor.getRegisterExecutor());
                batchFutures.add(future);
            }
            if (!batchFutures.isEmpty()) {
                try {
                    CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0]))
                            .get(5, TimeUnit.MINUTES);
                } catch (Exception e) {
                    log.error("Register batch failed", e);
                }
            }
        }

//        List<String> registeredNeIdList = new ArrayList<>(registeredNeIds);
        Set<String> needSyncNeIds = neRegisteredInfo.getNeedSynchronizedNes();

//        List<Node> nodesToSync = phyNodeDao.listConfigPhyNodeByIds(needSyncNeIds);
        List<List<String>> syncBatches = partitionList(new ArrayList<>(needSyncNeIds),
                maxConcurrentTasks);
        for (List<String> batch : syncBatches) {
            List<CompletableFuture<Void>> batchFutures = new ArrayList<>();
            for (String neId : batch) {
                if (!registerNeCache.trySyncLock(neId)) {
                    log.info("[sync] NE {} is on synchronizing，skip", neId);
                    continue;
                }

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        stateMonitorSemaphore.acquire();
                        log.debug("sync ne:{} start", neId);
                        processNodeSync(neId, friendlyNameMap.get(neId));
                        log.debug("sync ne:{} end", neId);
                    } catch (Exception e) {
                        log.error("sync ne:{} failed", neId, e);
//                        phyNodeDao.updateConfigPhyNodeCommunicateStatus(neId,
//                                CommunicationStatusType.SyncFailed);
                        communicateStateUpdater.syncFailed(neId);
                    } finally {
                        stateMonitorSemaphore.release();
                        registerNeCache.unlockSync(neId);
                    }
                }, AsynchronousExecutor.getSyncExecutor());
                batchFutures.add(future);
            }
            if (!batchFutures.isEmpty()) {
                try {
                    CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0]))
                            .get(5, TimeUnit.MINUTES);
                } catch (Exception e) {
                    log.error("Sync batch failed", e);
                }
            }
        }


    }

    private void processNodeSync(String neId, String friendlyName) {
//        boolean hasOp = phyNodeDao.existsOpNode(neId);
//        Physical physical = node.getAugmentation(Node1.class).getPhysical();
//        CommunicationStatusType commStatus = physical.getCommunicationStatus();
//        OperStatus operStatus = physical.getOperationalState();
        try {
//            if (hasOp) {
//                if (operStatus == OperStatus.NeCommunicationException
//                        || commStatus != CommunicationStatusType.SyncFinished) {
//                    log.debug("NE {} has OP; adapter reports fully synced. Updating status only.",
//                            neId);
//                    neResourceService.SyncNe(neId);
//                } else {
//                    log.trace("NE {} has OP and status already SyncFinished; nothing to do.", neId);
//                }
//                return;
//            } else {
            log.debug("NE {} has NO OP; performing full sync.", neId);
            neResourceService.SyncNe(neId, friendlyName);
//            }
            log.debug("NE {} full sync completed and status set to SyncFinished.", neId);
        } catch (Exception e) {
            log.error("Failed to process NE {}. Reason: {}", neId, e.getMessage(), e);
            communicateStateUpdater.syncFailed(neId);
        }
    }

    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            partitions.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return partitions;
    }

    /**
     * update change the status for the node
     *
     * @param configPhyNode
     */
    private void updateNodeStatus(Node configPhyNode) {
        String neId = configPhyNode.getNodeId().getValue();
        log.debug("update config ne node status ,the neId is :{}", neId);
        Node realNode = phyNodeDao.getOpPhyNodeById(neId);
        Physical emlNePhysical = realNode.getAugmentation(Node1.class).getPhysical();
        phyNodeDao.updateConfigNodeState(neId, emlNePhysical.getAlarmState(),
                emlNePhysical.getOperationalState(), null,
                AdminStatus.Up, emlNePhysical.getAlignmentStatus());
//        phyNodeDao.updateConfigPhyNodeCommunicateStatus(neId, CommunicationStatusType.SyncFinished);
        communicateStateUpdater.synced(neId);
    }

//    /**
//     * special to handle the config have op do not have and adapter already have registered ne id
//     * check and change the ne state
//     *
//     * @param nodeIds
//     */
//    private void checkAndChangeNeState(List<String> nodeIds) {
//        log.trace("check if the ne have op data and is the ne have registered");
//        List<String> noneOpNeIds = nodeIds.stream().filter(neId -> !phyNodeDao.existesOpNode(neId))
//                .collect(Collectors.toList());
//        Map<String, String> noneOpNeConnectMap = noneOpNeIds.stream()
//                .filter(neId -> adapterBalancer.getAdapterForNe(neId) != null)
//                .collect(HashMap::new, (map, neId) -> map.put(neId,
//                                adapterBalancer.getAdapterForNe(neId).getName().getValue()),
//                        HashMap::putAll);
//        for (Map.Entry<String, String> entry : noneOpNeConnectMap.entrySet()) {
//            String neId = entry.getKey();
//            String adapterId = entry.getValue();
//            phyNodeDao.updateConfigPhyNodePhysicalState(neId,
//                    PhyNodePhysicalStateDto.builder().alarmSeverity(AlarmSeverity.Unknown)
//                            .alarmSeverity(AlarmSeverity.Unknown).operStatus(OperStatus.Unknown)
//                            .build());
//            adapterBalancer.removeAdapterRegisteredNe(adapterId, neId);
//        }
//
//    }

    private List<Node> getWaitCheckNes() {
        log.debug("get wait check nes");
        List<String> neIds = phyNodeDao.listAllExistIpInConfigNodeIds();
        List<Node> nes = phyNodeDao.listConfigPhyNodeByIds(neIds);
        List<Node> supervisionNes = nes.stream().filter(ne ->
                ne.getAugmentation(Node1.class).getPhysical().getSupervisionStatus()
                        == SupervisionStatusType.Monitoring
        ).collect(Collectors.toList());
        return supervisionNes;
    }
}

/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.ne.manager.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.cache.RegisterNeCache;
import net.flex.dci.otc.controller.ne.manager.components.balancer.AdapterBalancer;
import net.flex.dci.otc.controller.ne.manager.core.service.NeResourceService;
import net.flex.dci.otc.controller.ne.manager.dto.NeRegisteredInfo;
import net.flex.dci.otc.controller.ne.manager.service.impl.NeManagerImpl;
import net.flex.dci.otc.controller.ne.manager.utils.AsynchronousExecutor;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dto.NodeInfoDto;
import net.flex.dci.otc.mongo.dto.PhyNodePhysicalStateDto;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.SupervisionStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NeManagerService {

    private static final Semaphore REGISTER_SEMAPHORE = new Semaphore(10);
    private static final Semaphore SYNC_SEMAPHORE = new Semaphore(10);

    private static final int BATCH_SIZE = 10;

    @Autowired
    private NeManagerImpl neManager;

    @Autowired
    private PhyNodeDao phyNodeDao;

    @Autowired
    private NeResourceService neResourceService;

    @Autowired
    private RegisterNeCache registerNeCache;


    @Autowired
    private AdapterBalancer adapterBalancer;

    public void manageNe() throws Exception {
        log.trace("back ground manage ne");
        List<String> nodeIds = phyNodeDao.listAllExistIpInConfigNodeIds();
        if (nodeIds.isEmpty()) {
            log.trace("ne with ip is empty,do noting");
            return;
        }
        //todo: special to handle the config have op do not have and adapter already have registered ne id
        List<String> noneOpNeIds = checkAndChangeNeState(nodeIds);
        log.debug("none operational data ne ids:{}", noneOpNeIds);
//        Set<String> needRegisteredNe = getNeedRegisteredNeId(nodeIds);
        NeRegisteredInfo neRegisteredInfo = adapterBalancer.getNeRegisteredInfo(nodeIds);
        CompletableFuture<Void> registerFuture = executeRegistrationTask(
                neRegisteredInfo.getNotRegisteredNes());
        CompletableFuture<Void> syncFuture = executeSyncTasks(
                neRegisteredInfo.getNeedSynchronizedNes());
        CompletableFuture<Void> allTasks = CompletableFuture.allOf(registerFuture, syncFuture);

        try {
            allTasks.get(15, TimeUnit.MINUTES);
            log.info("All NE management tasks complete successfully");
        } catch (TimeoutException e) {
            log.error("NE management tasks timed out", e);
            throw e;
        } catch (ExecutionException e) {
            log.error("Error during NE management tasks", e);
            throw new Exception("NE management failed", e);
        }

    }

    private CompletableFuture<Void> executeSyncTasks(Set<String> needSyncNeIds) {
        log.debug("execute sync ne concurrent the registered ne Ids :{}", needSyncNeIds);
        Map<String, String> friendlyNameMap = new HashMap<>();
        if (!needSyncNeIds.isEmpty()) {
            List<NodeInfoDto> needSynchronizedNe = phyNodeDao.listConfigNodeINfoByIds(
                    new ArrayList<>(needSyncNeIds));
            for (NodeInfoDto node : needSynchronizedNe) {
                friendlyNameMap.put(node.getId(), node.getFriendlyName());

            }
        }
        List<String> syncList = new ArrayList<>(needSyncNeIds);
        List<List<String>> batches = partitionList(syncList, BATCH_SIZE);
        log.info("Start processing {} sync batches, total {} ne(s)", batches.size(),
                syncList.size());

        for (int i = 0; i < batches.size(); i++) {
            List<String> batch = batches.get(i);
            log.info("Processing sync batch {}/{}: {} ne(s)", i + 1, batches.size(), batch.size());

            List<CompletableFuture<Void>> batchFutures = new ArrayList<>();
            for (String neId : batch) {
                if (!registerNeCache.trySyncLock(neId)) {
                    log.info("[sync duplicate] NE {} is already processing, skip", neId);
                    continue;
                }
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {

                    try {
                        SYNC_SEMAPHORE.acquire();
                        log.debug("reSynchronize ne id:{}", neId);
                        doReSyncNe(neId, friendlyNameMap.getOrDefault(neId, neId));
                        log.debug("reSynchronize ne id:{} completed", neId);
                    } catch (Exception e) {
                        log.error("Failed to sync ne id:{} the reason is:{}",
                                neId, e.getMessage(), e);
                        // 移除不必要的RuntimeException，不影响其他批次执行
                    } finally {
                        SYNC_SEMAPHORE.release();
                        registerNeCache.unlockSync(neId);
                    }
                }, AsynchronousExecutor.getSyncExecutor());

                batchFutures.add(future);
            }

            try {
                CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0]))
                        .get(15, TimeUnit.MINUTES);
                log.info("Sync batch {}/{} completed", i + 1, batches.size());
            } catch (Exception e) {
                log.error("Sync batch {}/{} failed", i + 1, batches.size(), e);
            }
        }

        log.info("All sync tasks completed successfully");
        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<Void> executeRegistrationTask(Set<String> notRegisteredNes) {
        log.debug("execute registration task the need registered ne:{}", notRegisteredNes);
//        List<Node> needRegisteredNeIds = phyNodeDao.listConfigPhyNodeByIds(notRegisteredNes);
//        List<CompletableFuture<Void>> futures = needRegisteredNeIds.stream()
////                .map(nodeId -> phyNodeDao.getConfigPhyNodeById(nodeId))
//                .filter(node ->
//                        node.getAugmentation(Node1.class).getPhysical().getSupervisionStatus()
//                                == null || !node.getAugmentation(Node1.class).getPhysical()
//                                .getSupervisionStatus().equals(SupervisionStatusType.Unmonitored))
//                .filter(this::isManagedNe)
//                .map(node -> CompletableFuture.runAsync(() -> {
//                    String neId = node.getNodeId().getValue();
//                    if (!registerNeCache.tryRegisterLock(neId)) {  // 新增去重
//                        log.info("[register duplicate] NE {} is already syncing, skip", neId);
//                        return;
//                    }
//                    try {
//                        REGISTER_SEMAPHORE.acquire();
//                        log.debug("register ne id:{}", node.getNodeId().getValue());
//                        String friendlyName = node.getAugmentation(Node1.class).getPhysical()
//                                .getFriendlyName();
//                        doRegisterNe(node.getNodeId().getValue(), friendlyName);
//                        log.debug("register ne id:{} completed", node.getNodeId().getValue());
//                    } catch (Exception e) {
//                        log.error("Failed to register ne id:{} the error reason is:{}",
//                                node.getNodeId().getValue(), e.getMessage(), e);
//                        throw new RuntimeException(e);
//                    } finally {
//                        REGISTER_SEMAPHORE.release();
//                        registerNeCache.unlockRegister(neId);
//                    }
//                }, AsynchronousExecutor.getRegisterExecutor()))
//                .collect(Collectors.toList());
//        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        log.debug("execute registration task the need registered ne:{}", notRegisteredNes);
        if (notRegisteredNes.isEmpty()) {
            log.info("No registration tasks to execute");
            return CompletableFuture.completedFuture(null);
        }

        List<Node> needRegisteredNodes = phyNodeDao.listConfigPhyNodeByIds(notRegisteredNes);
        List<Node> filteredNodes = needRegisteredNodes.stream()
                .filter(node ->
                        node.getAugmentation(Node1.class).getPhysical().getSupervisionStatus()
                                == null || !node.getAugmentation(Node1.class).getPhysical()
                                .getSupervisionStatus().equals(SupervisionStatusType.Unmonitored))
                .filter(this::isManagedNe)
                .collect(Collectors.toList());

        if (filteredNodes.isEmpty()) {
            log.info("No valid nodes to register after filtering");
            return CompletableFuture.completedFuture(null);
        }

        List<List<Node>> batches = partitionList(filteredNodes, BATCH_SIZE);
        log.info("Start processing {} register batches, total {} ne(s)", batches.size(),
                filteredNodes.size());

        for (int i = 0; i < batches.size(); i++) {
            List<Node> batch = batches.get(i);
            log.info("Processing register batch {}/{}: {} ne(s)", i + 1, batches.size(),
                    batch.size());

            List<CompletableFuture<Void>> batchFutures = new ArrayList<>();
            for (Node node : batch) {
                String neId = node.getNodeId().getValue();
                if (!registerNeCache.tryRegisterLock(neId)) {
                    log.info("[register duplicate] NE {} is already processing, skip", neId);
                    continue;
                }
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {

                    try {
                        REGISTER_SEMAPHORE.acquire();
                        log.debug("register ne id:{}", neId);
                        String friendlyName = node.getAugmentation(Node1.class).getPhysical()
                                .getFriendlyName();
                        doRegisterNe(neId, friendlyName);
                        log.debug("register ne id:{} completed", neId);
                    } catch (Exception e) {
                        log.error("Failed to register ne id:{} the error reason is:{}",
                                neId, e.getMessage(), e);
                    } finally {
                        REGISTER_SEMAPHORE.release();
                        registerNeCache.unlockRegister(neId);
                    }
                }, AsynchronousExecutor.getRegisterExecutor());

                batchFutures.add(future);
            }

            try {
                CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0]))
                        .get(15, TimeUnit.MINUTES);
                log.info("Register batch {}/{} completed", i + 1, batches.size());
            } catch (Exception e) {
                log.error("Register batch {}/{} failed", i + 1, batches.size(), e);
            }
        }

        log.info("All registration tasks completed successfully");
        return CompletableFuture.completedFuture(null);
    }

    private void doReSyncNe(String neId, String friendlyName) {
        log.info("synchronizing the ne data,the ne id is:{}", neId);
        try {
            neResourceService.SyncNe(neId, friendlyName);
        } catch (Exception e) {
            log.error("Failed to synchronize ne {}:{}", neId, e.getMessage(), e);
        }
    }

//    private Set<String> getNeedRegisteredNeId(List<String> nodeIds) {
//        log.debug("filter need registered ");
//        List<String> registeredNeId = adapterBalancer.getManagedNeIds();
//        Set<String> result = CommonUtil.getDifferenceSetByGuava(new HashSet<>(nodeIds),
//                new HashSet<>(registeredNeId));
//        return result;
//    }

    /**
     * special to handle the config have op do not have and adapter already have registered ne id
     * check and change the ne state
     *
     * @param nodeIds
     */
    private List<String> checkAndChangeNeState(List<String> nodeIds) {
        log.trace("check if the ne have op data and is the ne have registered");

        List<String> opNeIds = phyNodeDao.listAllOpNeIdByNeIds(nodeIds);
        Set<String> opNeIdSet = new HashSet<>(opNeIds);
        List<String> noneOpNeIds = nodeIds.stream()
                .filter(neId -> !opNeIdSet.contains(neId))
                .collect(Collectors.toList());

        Map<String, String> noneOpNeConnectMap = noneOpNeIds.stream()
                .filter(neId -> adapterBalancer.getAdapterForNe(neId) != null)
                .collect(HashMap::new, (map, neId) -> map.put(neId,
                                adapterBalancer.getAdapterForNe(neId).getName().getValue()),
                        HashMap::putAll);
        for (Map.Entry<String, String> entry : noneOpNeConnectMap.entrySet()) {
            String neId = entry.getKey();
            String adapterId = entry.getValue();
            phyNodeDao.updateConfigPhyNodePhysicalState(neId,
                    PhyNodePhysicalStateDto.builder().alarmSeverity(AlarmSeverity.Unknown)
                            .alarmSeverity(AlarmSeverity.Unknown).operStatus(OperStatus.Unknown)
                            .build());
            adapterBalancer.removeAdapterRegisteredNe(adapterId, neId);
        }
        return noneOpNeIds;
    }


    private void doRegisterNe(String neId, String friendlyName) {
        try {
            log.info("re register the ne neId:{}", neId);
            neManager.reRegisterNe(neId, friendlyName);
        } catch (Exception e) {
            log.error("Failed to register ne {}", neId, e);
        }

    }

    private boolean isManagedNe(Node node) {
        if (node.getAugmentation(Node1.class) != null
                && node.getAugmentation(Node1.class).getPhysical() != null) {
            Physical phy = node.getAugmentation(Node1.class).getPhysical();
            return phy.getIp() != null && phy.getPort() != null && phy.getLoginName() != null
                    && phy.getLoginPasswd() != null && phy.getAdminState() != null
                    && phy.getAdminState() != AdminStatus.Down;
        }
        return false;
    }


    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> partitions = new ArrayList<>();
        int size = list.size();
        for (int i = 0; i < size; i += batchSize) {
            partitions.add(list.subList(i, Math.min(size, i + batchSize)));
        }
        return partitions;
    }


}

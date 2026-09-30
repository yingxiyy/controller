/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.ne.manager.components.balancer;

import static net.flex.dci.otc.common.constants.Constants.COMMA;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.CommonUtil;
import net.flex.dci.otc.controller.ne.manager.dto.ManageNeSynchroInfo;
import net.flex.dci.otc.controller.ne.manager.dto.NeRegisteredInfo;
import net.flex.dci.otc.controller.ne.manager.enums.NeSynchronizedState;
import net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils;
import net.flex.dci.otc.controller.rpc.client.rpcs.AdapterRpc;
import net.flex.dci.otc.mongo.dao.AdapterDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.attribute.Ne;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/8/26 14:12
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AdapterBalancer {

    private final AdapterDao adapterDao;

    private final PhyNodeDao neDao;

    private final AdapterRpc adapterRpc;


    @Value("${sbi_version:null}")
    private String sbiVersion;

    public Adapter pickupActiveAdapter(String neId) throws CommonException {
        List<Adapter> adapters = adapterDao.getAdapters();
        if (CollectionUtils.isEmpty(adapters)) {
            String name = neDao.getFriendlyName(neId);
            throw new CommonException(CommonExceptionType.CANNOT_FIND_COOPERATOR,
                    String.format("There is no active adapter for the ne: %s (%s) to registered",
                            name, neId));
        }
        Adapter boundAdapter = adapterDao.getAdapterByNeId(neId);
        if (boundAdapter != null) {
            log.debug("NE {} stick to existing adapter {}", neId,
                    boundAdapter.getName().getValue());
            return boundAdapter;
        }
        return this.pickLeastLoadAdapter(adapters);
    }


    public Adapter getAdapterForNeWithException(String neId) throws CommonException {
        Adapter adapter = this.getAdapterForNe(neId);
        if (adapter == null) {
            String name = neDao.getFriendlyName(neId);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    String.format("Failed to find adapter for ne: %s (%s).", name, neId));
        }
        return adapter;
    }

    public Adapter getAdapterForNe(String neId) throws CommonException {
        log.debug("get adapters for the ne,neId: {}", neId);
        Adapter adapter = adapterDao.getAdapterByNeId(neId);
//        if (adapter == null) {
//            //load from the adapter connection info
//            adapter = findAdapterByNeId(neId);
//        }
        return adapter;
    }

    private Adapter findAdapterByNeId(String neId) {
        List<Adapter> adapters = adapterDao.getAdapters();

        for (Adapter adapter : adapters) {
            try {
                List<NodeId> managedNeIds = Optional.ofNullable(adapterRpc.getManagedNes(adapter))
                        .orElse(Collections.emptyList());

                boolean isManaged = managedNeIds.stream()
                        .map(NodeId::getValue)
                        .anyMatch(id -> id.contains(neId));

                if (isManaged) {
                    log.info("Found NE {} in adapter {}", neId, adapter.getName().getValue());
                    return adapter;
                }
            } catch (Exception e) {
                log.warn("Failed to query adapter {}: {}", adapter.getName().getValue(),
                        e.getMessage());
            }
        }

        log.warn("NE {} not found in any adapter", neId);
        return null;
    }

//    private List<Adapter> getValidAdapter() throws CommonException {
//        log.debug("start to get validated adapter");
//        List<Adapter> adapters = adapterDao.getAdapters();
//        if (CollectionUtils.isEmpty(adapters)) {
//            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
//                    "There is no active adapters.");
//        }

    /// /        List<Adapter> apiSupportedList = new ArrayList<Adapter>(); /        for (Adapter
    /// adapter : adapters) { /            String adapterId = adapter.getName().getValue(); / String
    /// adapterApiVersion = adapter.getApiVersion(); /            if
    /// (!StringUtils.isBlank(adapterApiVersion)) { /                String
    /// supportedAdapterApiVersionStr = sbiVersion; /                List<String> adapterVersionList
    /// = new ArrayList<>( / Arrays.asList(supportedAdapterApiVersionStr.split(","))); / if
    /// (adapterVersionList.contains(adapterApiVersion)) { / apiSupportedList.add(adapter); / } else
    /// { / log.debug("adapter to be chosen {} api version {} is not supported{}", / adapterId,
    /// adapterApiVersion, supportedAdapterApiVersionStr); / } / } else { / log.debug("adapter {}
    /// api version is null.", adapterId); / } /        }
//        return adapters;
//    }
    private Adapter pickLeastLoadAdapter(List<Adapter> adapters) {
        log.debug("Begin to pick up least load adapter.");
        int leastNeCount = 0;
        Adapter leastLoadAdapter = null;
        if (adapters != null) {
            for (Adapter adapter : adapters) {
                int neCount = adapter.getNe() == null ? 0 : adapter.getNe().size();
                log.debug("Adapter {} with load {}", adapter.getName().getValue(), neCount);
                if (leastLoadAdapter == null
                        || neCount < leastNeCount) {
                    leastLoadAdapter = adapter;
                    leastNeCount = neCount;
                }
            }
        }
        return leastLoadAdapter;
    }

    public void registered2Adapter(Adapter adapter, Node node) {
        String adapterId = adapter.getName().getValue();
        String neId = node.getNodeId().getValue();
        log.info("add the ne:{} to the adapter:{}", node.getNodeId(), adapter.getName());
        removeNeFromOtherAdapters(adapterId, neId);
        Adapter currentAdapter = adapterDao.getAdapterById(adapterId);
        if (currentAdapter != null && !isNeRegisteredToAdapter(currentAdapter, neId)) {
            adapterDao.addRegisteredNeId(adapterId, neId);
        }
    }

    private void removeNeFromOtherAdapters(String targetAdapterId, String neId) {
//        List<Adapter> adapters = Optional.ofNullable(adapterDao.getAdapters())
//                .orElse(Collections.emptyList());
//        adapters.stream()
//                .filter(adapter -> adapter.getName() != null)
//                .filter(adapter -> !targetAdapterId.equals(adapter.getName().getValue()))
//                .filter(adapter -> isNeRegisteredToAdapter(adapter, neId))
//                .forEach(adapter -> removeAdapterRegisteredNe(adapter.getName().getValue(), neId));
        log.debug("clean the remove ne from other adapter record");
        List<Adapter> adapters = adapterDao.getConnectAdapterByNeId(neId);
        List<Adapter> anotherRegisterAdapters = adapters.stream()
                .filter(adapter -> !adapter.getName().getValue().equals(targetAdapterId)).collect(
                        Collectors.toList());
        anotherRegisterAdapters.forEach(
                adapter -> removeAdapterRegisteredNe(adapter.getName().getValue(), neId));
    }

    private boolean isNeRegisteredToAdapter(Adapter adapter, String neId) {
        return Optional.ofNullable(adapter.getNe())
                .orElse(Collections.emptyList())
                .stream()
                .map(Ne::getNodeId)
                .filter(Objects::nonNull)
                .map(Uri::getValue)
                .anyMatch(neId::equals);
    }

//    public void removeAdapterRegisteredNe(Adapter adapter, String neId) {
//        log.debug("remove the registered ne :{}", neId);
//        adapterDao.removeRegisteredNeId(adapter.getName().getValue(), neId);
//        log.debug("finish to registered ne");
//    }

    public void removeAdapterRegisteredNe(String adapterId, String neId) {
        log.debug("remove the registered ne :{}", neId);
        adapterDao.removeRegisteredNeId(adapterId, neId);
        log.debug("finish to registered ne");
    }


    /**
     * get managed ne ids
     *
     * @return
     */
//    public List<String> getManagedNeIds() {
//        log.debug("list all registered ne id");
//        List<Adapter> adapters = adapterDao.getAdapters();
//        List<String> manageNe = new ArrayList<>();
//        for (Adapter adapter : adapters) {
//            List<NodeId> managedNeIds = adapterRpc.getManagedNes(adapter);
//            List<String> neIds = managedNeIds.stream().map(NodeId::getValue)
//                    .collect(Collectors.toList());
//            manageNe.addAll(neIds);
//        }
////        List<String> result = adapters.stream().map(AdapterAttribute::getNe)
////                .filter(Objects::nonNull)
////                .flatMap(Collection::stream).map(ne -> ne.getNodeId().getValue()).collect(
////                        Collectors.toList());
//        return manageNe;
//    }
    public List<ManageNeSynchroInfo> getManagedNeIds() {
        log.debug("list all registered ne id");
        List<Adapter> adapters = adapterDao.getAdapters();
        List<ManageNeSynchroInfo> allManagedNeIds = new ArrayList<>();
        Map<String, Set<String>> cleanupMap = new HashMap<>();
//        Map<String, Set<String>> addRegisterMap = new HashMap<>();
        for (Adapter adapter : adapters) {
            processSingleAdapter(adapter, allManagedNeIds, cleanupMap);
        }
        batchCleanupInvalidRegistrations(cleanupMap);
//        batchAddRegisterNe(addRegisterMap);
        return allManagedNeIds;

    }

//    private void batchAddRegisterNe(Map<String, Set<String>> addRegisterMap) {
//        addRegisterMap.forEach((adapterName, neIds) -> {
//            neIds.forEach(neId -> {
//                log.debug("sync registered ne id:{} to adapter:{} or not", neId, adapterName);
//                boolean isExitedIp = neDao.nodeExistIp(neId);
//                if (isExitedIp) {
//                    log.debug("add registered ne id:{} to adapter:{}", neId, adapterName);
//                    adapterDao.addRegisteredNeId(adapterName, neId);
//                    neDao.updateConfigPhyNodeCommunicateStatus(neId,
//                            CommunicationStatusType.Syncing);
//                }
//            });
//        });
//    }

    private void processSingleAdapter(Adapter adapter, List<ManageNeSynchroInfo> allManagedNeIds,
            Map<String, Set<String>> cleanupMap) {
        try {
            List<NodeId> managedNeIds = Optional.ofNullable(adapterRpc.getManagedNes(adapter))
                    .orElse(Collections.emptyList());
            //As assemble the neId and synchronize state on one status
            List<String> manageNeWithStatus = managedNeIds.stream().map(NodeId::getValue).collect(
                    Collectors.toList());
            List<ManageNeSynchroInfo> manageNeSynchroInfos = manageNeWithStatus.stream()
                    .map(this::getManagedNeStatus)
                    .collect(Collectors.toList());

            Set<String> dbNeIds = Optional.ofNullable(adapter.getNe())
                    .orElse(Collections.emptyList())
                    .stream()
                    .map(Ne::getNodeId)
                    .filter(Objects::nonNull)
                    .map(Uri::getValue)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Set<String> actualNeIds = manageNeSynchroInfos.stream()
                    .map(ManageNeSynchroInfo::getNeId)
                    .collect(Collectors.toSet());
            Set<String> neIdsToRemove = NeManagerUtils.getDifferenceSetByGuava(dbNeIds,
                    actualNeIds);

            if (!neIdsToRemove.isEmpty()) {
                cleanupMap.put(adapter.getName().getValue(), neIdsToRemove);
            }

            allManagedNeIds.addAll(manageNeSynchroInfos);
        } catch (Exception e) {
            log.warn("Failed to query managed NEs from adapter {}: {}",
                    adapter.getName().getValue(), e.getMessage(), e);
        }
    }

    private ManageNeSynchroInfo getManagedNeStatus(String managedNeStatus) {
        log.debug("get managed ne status:{}", managedNeStatus);
        String[] manageNeStatusArray = managedNeStatus.split(COMMA);
        return new ManageNeSynchroInfo(manageNeStatusArray[0],
                NeSynchronizedState.safeValueOf(manageNeStatusArray[1]));
    }

    private void batchCleanupInvalidRegistrations(Map<String, Set<String>> cleanupMap) {
        cleanupMap.forEach((adapterName, neIds) -> {
            neIds.forEach(neId -> {
                log.debug("remove adapter:{} register neId:{}", adapterName, neId);
                removeAdapterRegisteredNe(adapterName, neId);
            });
        });
    }


    public boolean isManagedNeId(String neId) {
        log.debug("is ne:{} manged or not", neId);
        List<ManageNeSynchroInfo> manageNeInfos = getManagedNeIds();
        List<String> managedNeIds = manageNeInfos.stream().map(ManageNeSynchroInfo::getNeId)
                .collect(
                        Collectors.toList());
        return managedNeIds.contains(neId);
    }


    public NeRegisteredInfo getNeRegisteredInfo(List<String> nodeIds) {
        log.debug("get current ne system registered info");
        List<ManageNeSynchroInfo> manageNeInfos = getManagedNeIds();
        List<String> managedNeIds = manageNeInfos.stream().map(ManageNeSynchroInfo::getNeId)
                .collect(Collectors.toList());
        Map<NeSynchronizedState, Set<String>> manageNeSyncMap = getManageNeSyncStateMap(
                manageNeInfos);
        Set<String> adapterManagedNeSet = new HashSet<>(managedNeIds);

        Set<String> managedNes = CommonUtil.getIntersectionSetByGuava(new HashSet<>(nodeIds),
                adapterManagedNeSet);
        Set<String> needRegisteredNes = CommonUtil.getDifferenceSetByGuava(new HashSet<>(nodeIds),
                adapterManagedNeSet);

        Set<String> needSynchronizedNeIds = Stream.concat(
                        manageNeSyncMap.get(NeSynchronizedState.NOT_SYNCED).stream(),
                        manageNeSyncMap.get(NeSynchronizedState.SYNC_FAILED).stream())
                .filter(nodeIds::contains)
                .collect(Collectors.toSet());
        //in db
        List<String> brokenNeIds = neDao.listUnSynchronizedMonitoredNeIds(managedNes);
        needSynchronizedNeIds.addAll(brokenNeIds);
        return NeRegisteredInfo.builder().registeredNes(managedNes)
                .needSynchronizedNes(needSynchronizedNeIds)
                .notRegisteredNes(needRegisteredNes)
                .build();
    }

    private Map<NeSynchronizedState, Set<String>> getManageNeSyncStateMap(
            List<ManageNeSynchroInfo> manageNeInfos) {
        Map<NeSynchronizedState, Set<String>> result = new EnumMap<>(NeSynchronizedState.class);
        for (NeSynchronizedState state : NeSynchronizedState.values()) {
            result.put(state, new HashSet<>());
        }

        for (ManageNeSynchroInfo info : manageNeInfos) {
            NeSynchronizedState state = info.getSynState();
            String neId = info.getNeId();
            result.get(state).add(neId);
        }

        return result;
    }


}

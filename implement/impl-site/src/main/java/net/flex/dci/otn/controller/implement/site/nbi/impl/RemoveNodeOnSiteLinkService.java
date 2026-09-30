package net.flex.dci.otn.controller.implement.site.nbi.impl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.allocate.link.site.SiteLinkNodeOperationState;
import net.flex.dci.otn.controller.allocate.link.site.removenode.RemoveNodeInSiteLinkImpl;
import net.flex.dci.otn.controller.implement.common.config.ImplConfig;
import net.flex.dci.otn.controller.implement.common.impl.OcmUpdator;
import net.flex.dci.otn.controller.implement.common.impl.StepResult;
import net.flex.dci.otn.controller.implement.common.lifecycle.LifeCycleSevice;
import net.flex.dci.otn.controller.implement.common.recorder.StepRecord;
import net.flex.dci.otn.controller.implement.common.service.MyExecutor;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.RemoveResourceOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.UnregisteNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.ne.result.FailObj;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CommunicationStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.SupervisionStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.attributes.OCMGripGroups;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.attributes.OCMGripGroupsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.attributes.OCMGripGroupsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RemoveNodeInSiteLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RemoveNodeInSiteLinkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RemoveNodeInSiteLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RemoveNodeOnSiteLinkService {

    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAILURE = "failure";
    private static final String FORMAT_IL = "link@%s@%s";
    private static final String FORMAT_OCM = "ocm@%s@%s@%s";
    private static final long UNREGISTER_TIMEOUT_SECONDS = 60L;
    private static final long UNREGISTER_POLL_INTERVAL_MILLIS = 500L;

    private final SiteLinkDao siteLinkDao;
    private final OchLinkDao ochLinkDao;
    private final PhyNodeDao phyNodeDao;
    private final RemoveNodeInSiteLinkImpl removeNodeInSiteLink;
    private final MultipleTransaction mongoTransaction;
    private final NeManagerRpc neManagerRpc;
    private final ImplConfig implConfig;
    private final MyExecutor executor;

    /**
     * Start device-side remove-node action. DB changes are prepared first but saved only after
     * A/B internalLink remove/create operations succeed on devices.
     */
    public RemoveNodeInSiteLinkOutput start(RemoveNodeInSiteLinkInput input, String who) {
        BigInteger taskInfoId = input.getTaskInfoId();
        String taskDetail = taskInfoId == null ? null : input.getTaskDetail();
        if(taskInfoId !=null && taskDetail == null){
            return failed("task-detail is required when task-info-id is provided");
        }

        Link siteLink = siteLinkDao.getSiteLinkById(input.getSiteLinkId());
        if (siteLink == null) {
            return failed("siteLink not found: " + input.getSiteLinkId());
        }
        Site siteAttr = siteLink.getAugmentation(Link1.class).getSite();
        try {
            validateRemovalImplementState(taskInfoId, siteAttr.getImplementState());
        } catch (CommonException e) {
            return failed(e.getMessage());
        }

        String nodeOperationState = SiteLinkNodeOperationState.get(siteLink);
        if (!canRemoveNode(nodeOperationState)) {
            return failed("siteLink is processing a node operation: " + nodeOperationState);
        }
        if (SiteLinkNodeOperationState.WAITING_INSERT_APPLY.equals(nodeOperationState)
                || SiteLinkNodeOperationState.INSERT_APPLY_FAILED_BEFORE_DEVICE.equals(
                        nodeOperationState)) {
            String operationNodeId = SiteLinkNodeOperationState.getNodeId(siteLink);
            if (!Objects.equals(operationNodeId, input.getNodeId())) {
                return failed("siteLink node operation belongs to " + operationNodeId
                        + ", cannot remove node " + input.getNodeId());
            }
        }
        LifeCycleSevice lifeService = taskInfoId == null ? new LifeCycleSevice() : null;
        try {
            if (lifeService != null) {
                lifeService.buildLifeService(siteLink.getLinkId().getValue(),
                        TaskInfoMessage.ResourceType.siteLink, siteAttr.getFriendlyName(),
                        ActionType.removeNode, who);
                lifeService.getTaskInfoMessage().setResourceName(siteAttr.getFriendlyName()
                        + ": remove " + buildRemovedNodeRecord(input.getNodeId()).getFriendlyName());
                lifeService.sendMsg();
            }

            executor.lazyDo(() -> {
                String msg = null;
                try {
                    new RemoveNodeOnSiteLinkTask(input.getSiteLinkId(), input.getNodeId(),
                            taskInfoId, taskDetail, lifeService).run();
                } catch (Exception e) {
                    log.error("remove inserted node on siteLink error", e);
                    msg = ExceptionUtils.getRootCauseMessage(e);
                } finally {
                    if (lifeService != null) {
                        lifeService.logEndLinkImpl(msg);
                    }
                }
            });
        } catch (RuntimeException e) {
            if(taskInfoId !=null){
                failOriginalInsertTaskInfoSafely(taskInfoId, taskDetail, ExceptionUtils.getRootCauseMessage(e));
            }
            return failed(ExceptionUtils.getRootCauseMessage(e));
        }

        return new RemoveNodeInSiteLinkOutputBuilder()
                .setReturnCode(RpcResultType.AcceptAndStartAsync)
                .build();
    }

    private void recordRemovedNode(String nodeId, LifeCycleSevice lifeService) {
        if (lifeService == null) {
            return;
        }
        lifeService.logStatusChanged(buildRemovedNodeRecord(nodeId));
    }

    private StepRecord buildRemovedNodeRecord(String nodeId) {
        String nodeName = nodeId;
        String ip = "";
        try {
            Node node = phyNodeDao.getConfigPhyNodeById(nodeId);
            Node1 augmentation = node == null ? null : node.getAugmentation(Node1.class);
            Physical physical = augmentation == null ? null : augmentation.getPhysical();
            if (physical != null) {
                String friendlyName = physical.getFriendlyName();
                if (friendlyName != null && !friendlyName.trim().isEmpty()) {
                    nodeName = friendlyName;
                }
                ip = physical.getIp() == null ? "" : physical.getIp();
            }
        } catch (RuntimeException e) {
            log.warn("cannot load removed node details, record node ID only: {}", nodeId, e);
        }
        return new StepRecord(nodeId, nodeName, ip);
    }

    private RemoveNodeInSiteLinkOutput failed(String message) {
        return new RemoveNodeInSiteLinkOutputBuilder()
                .setReturnCode(RpcResultType.OtherError)
                .setReturnMessage(message)
                .build();
    }

    static TaskInfoMessage buildCompletedOriginalInsertTaskInfo(BigInteger taskInfoId, String taskDetail, long endTime) {
        if (taskInfoId == null) {
            return null;
        }
        return TaskInfoMessage.builder()
                .id(taskInfoId.longValue())
                .detail(taskDetail)
                .successfully(true)
                .errorReason("cancelled")
                .endTime(endTime)
                .root(true)
                .build();
    }

    static TaskInfoMessage buildFailedOriginalInsertTaskInfo(BigInteger taskInfoId,
            String taskDetail, long endTime, Long groupId, String errorReason) {
        if (taskInfoId == null) {
            return null;
        }
        return TaskInfoMessage.builder()
                .id(taskInfoId.longValue())
                .detail(taskDetail)
                .successfully(false)
                .errorReason(errorReason)
                .endTime(endTime)
                .groupId(groupId)
                .root(true)
                .build();
    }

    private void completeOriginalInsertTaskInfoSafely(BigInteger taskInfoId, String taskDetail) {
        TaskInfoMessage message = buildCompletedOriginalInsertTaskInfo(taskInfoId, taskDetail, System.currentTimeMillis());
        if (message == null) {
            return;
        }
        try {
            log.info("complete cancelled insert-node-in-site-link taskInfo {}", taskInfoId);
            TaskInfoMessager.sendMessage(message);
        } catch (RuntimeException e) {
            log.error("complete cancelled insert-node-in-site-link taskInfo {} failed",
                    taskInfoId, e);
        }
    }

    private void failOriginalInsertTaskInfoSafely(BigInteger taskInfoId, String taskDetail,String errorReason) {
        TaskInfoMessage message = buildFailedOriginalInsertTaskInfo(
                taskInfoId, taskDetail, System.currentTimeMillis(), null, errorReason);
        if (message == null) {
            return;
        }
        try {
            log.info("fail insert-node-in-site-link taskInfo {} while removing node", taskInfoId);
            TaskInfoMessager.sendMessage(message);
        } catch (RuntimeException e) {
            log.error("fail insert-node-in-site-link taskInfo {} while removing node failed",
                    taskInfoId, e);
        }
    }

    static void refreshSiteLinkSnapshot(ChangedObject changedObject, String siteLinkId) {
        Link preparedSiteLink = changedObject.getChangedSiteLink(siteLinkId);
        // The direct Doimplementing write changed the DB baseline, but not the prepared route.
        changedObject.unsetSiteLink(siteLinkId);
        if (changedObject.getChangedSiteLink(siteLinkId) == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "cannot reload siteLink after updating implement state: " + siteLinkId);
        }
        changedObject.addChangedSiteLink(preparedSiteLink);
    }

    private class RemoveNodeOnSiteLinkTask implements Runnable {

        private final String siteLinkId;
        private final String nodeId;
        private final BigInteger taskInfoId;
        private final String taskDetail;
        private final LifeCycleSevice lifeService;
        private final ZkResourceLock locker = new ZkResourceLock();

        private ChangedObject changedObject;
        private List<String> removedSplitLinkIds;
        private List<String> mergedLinkIds;
        private List<String> azNodeIds;
        private String siteNodeId;
        private List<String> affectedOchLinkIds;
        private Link originalSiteLink;
        private List<Link> originalOchLinks;
        private Map<String, FrequencySnapshot> originalOchFrequencies;
        private ImplementState originalSiteLinkState;
        private String originalNodeOperationState;
        private AdminStatus originalSiteLinkAdminState;
        private AdminStatus originalMergedOtsAdminState;
        private final Map<String, Set<OCMGripGroupsKey>> affectedOcmKeys = new HashMap<>();
        private final Map<String, List<OCMGripGroups>> oldAffectedOcmGroups = new HashMap<>();
        private final Map<String, List<OCMGripGroups>> newAffectedOcmGroups = new HashMap<>();

        RemoveNodeOnSiteLinkTask(String siteLinkId, String nodeId, BigInteger taskInfoId,
                String taskDetail, LifeCycleSevice lifeService) {
            this.siteLinkId = siteLinkId;
            this.nodeId = nodeId;
            this.taskInfoId = taskInfoId;
            this.taskDetail = taskDetail;
            this.lifeService = lifeService;
        }

        /**
         * Remove is the reverse of apply-node: only A/B internalLinks are changed on devices.
         * N is being removed from DB, so it must not receive any config/remove payload here.
         */
        @Override
        public void run() {
            boolean deviceConfigurationStarted = false;
            try {
                // Keep the removal target first, before any A/B device operation records.
                recordRemovedNode(nodeId, lifeService);
                // 先从当前 siteLink 推导 split OTS 和 A/B，确保真正修改前能锁住 siteLink、N、A、B。
                removedSplitLinkIds = resolveSplitLinkIdsFromCurrentSiteLink();
                azNodeIds = resolveAzNodeIds(removedSplitLinkIds);
                siteNodeId = PhysicalNodeIdNamingRule.getSiteId(nodeId);
                affectedOchLinkIds = resolveAffectedOchLinkIds();
                lockResources();

                snapshotOriginalTopology();
                validateRemovalImplementState(taskInfoId, originalSiteLinkState);
                boolean releaseRemovedNodeIp = shouldReleaseRemovedNodeIpFromTopology();
                changedObject = removeNodeInSiteLink.prepareChanges(siteLinkId, nodeId);
                mergedLinkIds = resolveMergedLinkIds();
                validatePreparedChanges();
                validateOchFrequenciesUnchanged();
                rebuildAffectedOcmGroups();
                boolean configureDevices = shouldConfigureDevices(originalNodeOperationState, originalSiteLinkState);
                if (configureDevices) {
                    siteLinkDao.updateSiteLinkImplementState(siteLinkId,
                            ImplementState.Doimplementing, AdminStatus.Up);
                    deviceConfigurationStarted = true;
                    refreshSiteLinkSnapshot(changedObject, siteLinkId);
                    removeSplitInternalLinksOnAZ();
                    createMergedInternalLinksOnAZ();
                    removeOldOcmGroups();
                    createNewOcmGroups();
                } else {
                    log.info("skip device configuration for remove-node-in-site-link because "
                                    + "siteLink state does not require device rollback: "
                                    + "siteLink={}, removedNode={}, nodeOperationState={}, implementState={}",
                            siteLinkId, nodeId, originalNodeOperationState, originalSiteLinkState);
                }
                if (releaseRemovedNodeIp) {
                    unregisterRemovedNodeAndWait();
                }
                applyFinalImplementStates();
                applyFinalOcmToChangedObject(configureDevices);
                changedObject.siteLinkRouteChange(siteLinkId);
                mongoTransaction.save(changedObject);
                if(taskInfoId != null){
                    completeOriginalInsertTaskInfoSafely(taskInfoId, taskDetail);
                }
            } catch (RuntimeException e) {
                if (deviceConfigurationStarted) {
                    markRemoveNodePartialImplementSafely(e);
                }
                if(taskInfoId != null){
                failOriginalInsertTaskInfoSafely(taskInfoId, taskDetail, ExceptionUtils.getRootCauseMessage(e));
                }
                throw e;
            } finally {
                locker.unlock();
            }
        }

        private void unregisterRemovedNodeAndWait() {
            Physical physical = getRemovedNodePhysical();
            if (isUnregistered(physical.getSupervisionStatus(),
                    physical.getCommunicationStatus())) {
                log.info("removed node is already unregistered: {}", nodeId);
                return;
            }

            UnregisteNeOutput output = neManagerRpc.unregisteredNe(
                    NodeId.getDefaultInstance(nodeId));
            if (output == null || !RpcResultType.Success.equals(output.getReturnCode())) {
                throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                        "failed to submit unregister request for removed node: " + nodeId);
            }

            log.info("submitted unregister request for removed node: {}", nodeId);
            waitUntilRemovedNodeUnregistered();
        }

        private boolean shouldReleaseRemovedNodeIpFromTopology() {
            Node configNode = phyNodeDao.getConfigPhyNodeById(nodeId);
            Node opNode = phyNodeDao.getOpPhyNodeById(nodeId);
            Physical configPhysical = getPhysical(configNode);
            Physical opPhysical = getPhysical(opNode);
            boolean shouldRelease = RemoveNodeOnSiteLinkService.shouldReleaseRemovedNodeIp(
                    configPhysical, opPhysical);
            if (!shouldRelease) {
                log.info("skip unregistering removed node because config/op IP or supervision state "
                                + "does not satisfy release conditions: node={}",
                        nodeId);
            }
            return shouldRelease;
        }

        private Physical getPhysical(Node node) {
            if (node == null || node.getAugmentation(Node1.class) == null) {
                return null;
            }
            return node.getAugmentation(Node1.class).getPhysical();
        }

        private void waitUntilRemovedNodeUnregistered() {
            long deadline = System.nanoTime()
                    + TimeUnit.SECONDS.toNanos(UNREGISTER_TIMEOUT_SECONDS);
            while (System.nanoTime() < deadline) {
                Physical physical = getRemovedNodePhysical();
                if (isUnregistered(physical.getSupervisionStatus(),
                        physical.getCommunicationStatus())) {
                    log.info("removed node unregister completed: {}", nodeId);
                    return;
                }

                try {
                    TimeUnit.MILLISECONDS.sleep(UNREGISTER_POLL_INTERVAL_MILLIS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                            "interrupted while waiting for removed node unregister: " + nodeId,
                            e);
                }
            }

            throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                    "timeout waiting " + UNREGISTER_TIMEOUT_SECONDS
                            + " seconds for removed node unregister: " + nodeId);
        }

        private Physical getRemovedNodePhysical() {
            Node node = phyNodeDao.getConfigPhyNodeById(nodeId);
            if (node == null || node.getAugmentation(Node1.class) == null
                    || node.getAugmentation(Node1.class).getPhysical() == null) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "config phy node is missing while unregistering: " + nodeId);
            }
            return node.getAugmentation(Node1.class).getPhysical();
        }

        private void markRemoveNodePartialImplementSafely(RuntimeException originalError) {
            try {
                siteLinkDao.updateSiteLinkImplementState(siteLinkId,
                        ImplementState.PartialImplement, AdminStatus.Up);
            } catch (RuntimeException stateError) {
                originalError.addSuppressed(stateError);
                log.error("failed to mark remove-node-in-site-link state for siteLink {}",
                        siteLinkId, stateError);
            }
        }

        private void lockResources() {
            Set<String> resources = new HashSet<>();
            resources.add(siteLinkId);
            resources.add(nodeId);
            resources.add(siteNodeId);
            resources.addAll(azNodeIds);
            resources.addAll(removedSplitLinkIds);
            resources.addAll(affectedOchLinkIds);
            resources.stream().filter(Objects::nonNull).sorted().forEach(locker::addResource);
            locker.getLock();
            log.info("locked resources for remove-node-in-site-link: siteLink={}, removedNode={}, resources={}",
                    siteLinkId, nodeId, resources);
        }

        private List<String> resolveAffectedOchLinkIds() {
            List<Link> ochLinks = ochLinkDao.getAllOchLinksUnderSiteLinkIds(
                    Collections.singletonList(siteLinkId));
            if (ochLinks == null) {
                return Collections.emptyList();
            }
            return ochLinks.stream()
                    .filter(Objects::nonNull)
                    .map(link -> link.getLinkId().getValue())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        }

        private List<String> resolveSplitLinkIdsFromCurrentSiteLink() {
            ChangedObject readObject = new ChangedObject();
            Link siteLink = readObject.getChangedSiteLink(siteLinkId);
            if (siteLink == null || siteLink.getSupportingLink() == null) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "siteLink has no supporting-link: " + siteLinkId);
            }
            List<String> splitLinkIds = new ArrayList<>();
            for (SupportingLink supportingLink : siteLink.getSupportingLink()) {
                String linkId = supportingLink.getLinkRef().getValue();
                if (!PhysicalLinkIdNamingRule.isOtsLink(linkId)) {
                    continue;
                }
                Link phyLink = readObject.getChangedPhyLink(linkId);
                if (phyLink != null && isOnRemovedNode(phyLink)) {
                    splitLinkIds.add(linkId);
                }
            }
            if (splitLinkIds.size() != 2) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "remove node must find exactly two split OTS links, actual "
                                + splitLinkIds.size());
            }
            return splitLinkIds;
        }

        private boolean isOnRemovedNode(Link link) {
            return nodeId.equals(link.getSource().getSourceNode().getValue())
                    || nodeId.equals(link.getDestination().getDestNode().getValue());
        }

        private List<String> resolveAzNodeIds(List<String> splitLinkIds) {
            Set<String> azNodes = new HashSet<>();
            for (String linkId : splitLinkIds) {
                String aNode = PhysicalLinkIdNamingRule.getNodeAId(linkId);
                String zNode = PhysicalLinkIdNamingRule.getNodeZId(linkId);
                if (!nodeId.equals(aNode)) {
                    azNodes.add(aNode);
                }
                if (!nodeId.equals(zNode)) {
                    azNodes.add(zNode);
                }
            }
            if (azNodes.size() != 2) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "cannot infer A/Z nodes from split OTS links: " + splitLinkIds);
            }
            return new ArrayList<>(azNodes);
        }

        private void snapshotOriginalTopology() {
            ChangedObject snapshot = new ChangedObject();
            originalSiteLink = snapshot.getChangedSiteLink(siteLinkId);
            if (originalSiteLink == null || originalSiteLink.getAugmentation(Link1.class) == null
                    || originalSiteLink.getAugmentation(Link1.class).getSite() == null) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "cannot snapshot siteLink before remove: " + siteLinkId);
            }
            Site originalSite = originalSiteLink.getAugmentation(Link1.class).getSite();
            originalSiteLinkState = originalSite.getImplementState();
            originalNodeOperationState = SiteLinkNodeOperationState.get(originalSiteLink);
            originalSiteLinkAdminState = originalSite.getAdminState();

            ImplementState splitState = null;
            AdminStatus splitAdminState = null;
            for (String splitLinkId : removedSplitLinkIds) {
                Link splitLink = snapshot.getChangedPhyLink(splitLinkId);
                if (splitLink == null || splitLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class) == null) {
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                            "cannot snapshot split OTS link: " + splitLinkId);
                }
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical
                        physical = splitLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class)
                        .getPhysical();
                if (physical == null) {
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                            "split OTS link has no physical attributes: " + splitLinkId);
                }
                if (splitState == null) {
                    splitState = physical.getImplementState();
                    splitAdminState = physical.getAdminState();
                } else if (!Objects.equals(splitState, physical.getImplementState())
                        || !Objects.equals(splitAdminState, physical.getAdminState())) {
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                            "split OTS links have inconsistent implement/admin states");
                }
            }
            originalMergedOtsAdminState = splitAdminState;

            List<Link> links = ochLinkDao.getAllOchLinksUnderSiteLinkIds(
                    Collections.singletonList(siteLinkId));
            originalOchLinks = links == null ? new ArrayList<>() : new ArrayList<>(links);
            originalOchFrequencies = new LinkedHashMap<>();
            for (Link ochLink : originalOchLinks) {
                originalOchFrequencies.put(ochLink.getLinkId().getValue(),
                        FrequencySnapshot.from(ochLink));
            }
        }

        private List<String> resolveMergedLinkIds() {
            return changedObject.getChangedPhyLinkList().values().stream()
                    .map(link -> link.getLinkId().getValue())
                    .filter(PhysicalLinkIdNamingRule::isOtsLink)
                    .filter(linkId -> !removedSplitLinkIds.contains(linkId))
                    .filter(this::connectsEdgeNodes)
                    .collect(Collectors.toList());
        }

        private boolean connectsEdgeNodes(String linkId) {
            Set<String> nodes = new HashSet<>();
            nodes.add(PhysicalLinkIdNamingRule.getNodeAId(linkId));
            nodes.add(PhysicalLinkIdNamingRule.getNodeZId(linkId));
            return nodes.containsAll(azNodeIds);
        }

        private void validatePreparedChanges() {
            if (!changedObject.getRemovedPhyLinkIdList().containsAll(removedSplitLinkIds)) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "prepared DB change does not remove expected split OTS links");
            }
            if (mergedLinkIds.size() != 1) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "prepared DB change should create exactly one merged OTS link, actual "
                                + mergedLinkIds.size());
            }
        }

        private void validateOchFrequenciesUnchanged() {
            for (Map.Entry<String, FrequencySnapshot> entry : originalOchFrequencies.entrySet()) {
                Link preparedOch = changedObject.getChangedOchLink(entry.getKey());
                if (preparedOch == null) {
                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                            "prepared DB change removes original OCH link: " + entry.getKey());
                }
                FrequencySnapshot preparedFrequency = FrequencySnapshot.from(preparedOch);
                if (!entry.getValue().equals(preparedFrequency)) {
                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                            "remove-node-in-site-link changes OCH frequency: " + entry.getKey()
                                    + ", old=" + entry.getValue() + ", new=" + preparedFrequency);
                }
            }
        }

        private void rebuildAffectedOcmGroups() {
            List<Link> workingOchLinks = originalOchLinks.stream()
                    .map(link -> changedObject.getChangedOchLink(link.getLinkId().getValue()))
                    .filter(Objects::nonNull)
                    .filter(this::shouldUseForOcm)
                    .collect(Collectors.toList());

            Map<String, Map<OCMGripGroupsKey, OCMGripGroups>> oldGroups =
                    calculateOcmGroups(originalSiteLink, workingOchLinks);
            Map<String, Map<OCMGripGroupsKey, OCMGripGroups>> newGroups =
                    calculateOcmGroups(changedObject.getChangedSiteLink(siteLinkId), workingOchLinks);

            Set<String> nodeIds = new HashSet<>(oldGroups.keySet());
            nodeIds.addAll(newGroups.keySet());
            for (String currentNodeId : nodeIds) {
                Map<OCMGripGroupsKey, OCMGripGroups> oldNodeGroups =
                        oldGroups.getOrDefault(currentNodeId, Collections.emptyMap());
                Map<OCMGripGroupsKey, OCMGripGroups> newNodeGroups =
                        newGroups.getOrDefault(currentNodeId, Collections.emptyMap());
                Set<OCMGripGroupsKey> keys = new HashSet<>(oldNodeGroups.keySet());
                keys.addAll(newNodeGroups.keySet());
                for (OCMGripGroupsKey key : keys) {
                    OCMGripGroups oldGroup = oldNodeGroups.get(key);
                    OCMGripGroups newGroup = newNodeGroups.get(key);
                    if (Objects.equals(oldGroup, newGroup)) {
                        continue;
                    }
                    affectedOcmKeys.computeIfAbsent(currentNodeId, ignored -> new HashSet<>())
                            .add(key);
                    if (oldGroup != null) {
                        oldAffectedOcmGroups.computeIfAbsent(currentNodeId,
                                ignored -> new ArrayList<>()).add(oldGroup);
                    }
                    if (newGroup != null) {
                        newAffectedOcmGroups.computeIfAbsent(currentNodeId,
                                ignored -> new ArrayList<>()).add(newGroup);
                    }
                }
            }
            log.info("rebuilt OCM groups for remove-node-in-site-link: affectedNodes={}, affectedSlots={}",
                    affectedOcmKeys.size(), affectedOcmKeys.values().stream()
                            .mapToInt(Set::size).sum());
        }

        private boolean shouldUseForOcm(Link ochLink) {
            if (ochLink == null || ochLink.getLinkId() == null) {
                return false;
            }
            Site site = originalSiteLink.getAugmentation(Link1.class).getSite();
            if (site.getDummyLink() != null
                    && site.getDummyLink().contains(ochLink.getLinkId().getValue())) {
                return true;
            }
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1 ochAug =
                    ochLink.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class);
            return ochAug != null && ochAug.getOch() != null
                    && !ImplementState.Allocate.equals(ochAug.getOch().getImplementState());
        }

        private Map<String, Map<OCMGripGroupsKey, OCMGripGroups>> calculateOcmGroups(
                Link siteLink, List<Link> workingOchLinks) {
            OcmUpdator updater = new OcmUpdator();
            updater.buildOcmGroup(siteLink, new ArrayList<>(workingOchLinks));
            Map<String, Map<OCMGripGroupsKey, OCMGripGroups>> result = new HashMap<>();
            updater.getOcmNodeMap().forEach((currentNodeId, currentNode) -> {
                Physical physical = currentNode.getAugmentation(Node1.class).getPhysical();
                Map<OCMGripGroupsKey, OCMGripGroups> byKey = new LinkedHashMap<>();
                if (physical.getOCMGripGroups() != null) {
                    physical.getOCMGripGroups().forEach(group -> byKey.put(group.getKey(), group));
                }
                result.put(currentNodeId, byKey);
            });
            return result;
        }

        /**
         * Delete A-N/N-B internalLinks from A/B devices. The split links still exist in opNode,
         * while the prepared config change has already removed them from changed config nodes.
         */
        private void removeSplitInternalLinksOnAZ() {
            for (String azNodeId : azNodeIds) {
                Node opNode = changedObject.getChangedPhyOpNode(azNodeId);
                if (opNode == null) {
                    log.error("opNode not found for A/Z node: {}", azNodeId);
                    continue;
                }
                List<InternalLinks> splitInternalLinks = findInternalLinks(opNode, removedSplitLinkIds);
                if (splitInternalLinks.isEmpty()) {
                    log.info("no split internalLink found on {}", azNodeId);
                    continue;
                }
                Node removeNode = buildInternalLinkOnlyNode(changedObject.getChangedPhyNode(azNodeId), splitInternalLinks);
                StepRecord stepRecord = newStepRecord(removeNode);
                StepResult result = removeResource(removeNode);
                updateStepRecord(stepRecord, splitInternalLinks, result);
                recordStep(stepRecord);
                failIfNeeded(result);
                changedObject.addChangedPhyOpNode(removeInternalLinks(opNode, splitInternalLinks));
            }
        }

        /**
         * Create merged A-B internalLinks on A/B devices. These are generated by the prepared DB
         * change and exist only on the changed config nodes until this device action succeeds.
         */
        private void createMergedInternalLinksOnAZ() {
            for (String azNodeId : azNodeIds) {
                Node phyNode = changedObject.getChangedPhyNode(azNodeId);
                List<InternalLinks> mergedInternalLinks = findMergedInternalLinks(phyNode);
                if (mergedInternalLinks.isEmpty()) {
                    log.info("no merged internalLink found on {}", azNodeId);
                    continue;
                }
                Node writeNode = buildInternalLinkOnlyNode(phyNode, mergedInternalLinks);
                StepRecord stepRecord = newStepRecord(writeNode);
                StepResult result = configNe(writeNode);
                updateStepRecord(stepRecord, mergedInternalLinks, result);
                recordStep(stepRecord);
                failIfNeeded(result);
                Node opNode = changedObject.getChangedPhyOpNode(azNodeId);
                if (opNode == null) {
                    log.error("opNode not found : {}", azNodeId);
                    continue;
                }
                changedObject.addChangedPhyOpNode(addInternalLinks(opNode, mergedInternalLinks));
            }
        }

        private void removeOldOcmGroups() {
            for (Map.Entry<String, List<OCMGripGroups>> entry : oldAffectedOcmGroups.entrySet()) {
                if (nodeId.equals(entry.getKey())) {
                    log.info("skip old OCM removal on removed node: {}", nodeId);
                    continue;
                }
                Node baseNode = changedObject.getChangedPhyOpNode(entry.getKey());
                if (baseNode == null) {
                    baseNode = changedObject.getChangedPhyNode(entry.getKey());
                }
                if (baseNode == null) {
                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                            "cannot find node for old OCM removal: " + entry.getKey());
                }
                for (OCMGripGroups oldGroup : entry.getValue()) {
                    OCMGripGroups emptyGroup = new OCMGripGroupsBuilder(oldGroup)
                            .setChannels(new ArrayList<>())
                            .build();
                    Node removeNode = buildOcmOnlyNode(baseNode, emptyGroup);
                    StepRecord stepRecord = newStepRecord(removeNode);
                    StepResult result = removeResource(removeNode);
                    updateOcmStepRecord(stepRecord, emptyGroup, result);
                    recordStep(stepRecord);
                    failIfNeeded(result);
                }
            }
        }

        private void createNewOcmGroups() {
            for (Map.Entry<String, List<OCMGripGroups>> entry : newAffectedOcmGroups.entrySet()) {
                if (nodeId.equals(entry.getKey())) {
                    log.warn("skip new OCM creation on removed node found in rebuilt route: {}", nodeId);
                    continue;
                }
                Node baseNode = changedObject.getChangedPhyNode(entry.getKey());
                if (baseNode == null) {
                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                            "cannot find node for new OCM creation: " + entry.getKey());
                }
                for (OCMGripGroups newGroup : entry.getValue()) {
                    Node writeNode = buildOcmOnlyNode(baseNode, newGroup);
                    StepRecord stepRecord = newStepRecord(writeNode);
                    StepResult result = configNe(writeNode);
                    updateOcmStepRecord(stepRecord, newGroup, result);
                    recordStep(stepRecord);
                    failIfNeeded(result);
                }
            }
        }

        private Node buildOcmOnlyNode(Node node, OCMGripGroups group) {
            Physical physical = node.getAugmentation(Node1.class).getPhysical();
            return new NodeBuilder(node)
                    .setTerminationPoint(new ArrayList<TerminationPoint>())
                    .addAugmentation(Node1.class, new Node1Builder()
                            .setPhysical(new PhysicalBuilder(physical)
                                    .setSystem(null)
                                    .setDcn(null)
                                    .setInternalLinks(new ArrayList<InternalLinks>())
                                    .setCrossConnections(new ArrayList<CrossConnections>())
                                    .setEquipments(new ArrayList<Equipments>())
                                    .setOCMGripGroups(Collections.singletonList(group))
                                    .setProperties(null)
                                    .build())
                            .build())
                    .build();
        }

        private void applyFinalImplementStates() {
            ImplementState finalImplementState = finalImplementState(originalSiteLinkState);
            Link preparedSiteLink = changedObject.getChangedSiteLink(siteLinkId);
            Site preparedSite = preparedSiteLink.getAugmentation(Link1.class).getSite();
            Link restoredSiteLink = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder(
                    preparedSiteLink)
                    .addAugmentation(Link1.class,
                            new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder()
                                    .setSite(new SiteBuilder(preparedSite)
                                            .setImplementState(finalImplementState)
                                            .setAdminState(originalSiteLinkAdminState)
                                            .build())
                                    .build())
                    .build();
            changedObject.addChangedSiteLink(restoredSiteLink);

            String mergedLinkId = mergedLinkIds.get(0);
            Link mergedLink = changedObject.getChangedPhyLink(mergedLinkId);
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1 mergedAug =
                    mergedLink.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class);
            Link restoredMergedLink = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder(
                    mergedLink)
                    .addAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class,
                            new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1Builder()
                                    .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder(
                                            mergedAug.getPhysical())
                                            .setImplementState(finalImplementState)
                                            .setAdminState(originalMergedOtsAdminState)
                                            .build())
                                    .build())
                    .build();
            changedObject.addChangedPhyLink(restoredMergedLink);
        }

        private void applyFinalOcmToChangedObject(boolean updateOperationalState) {
            for (Map.Entry<String, Set<OCMGripGroupsKey>> entry : affectedOcmKeys.entrySet()) {
                String currentNodeId = entry.getKey();
                List<OCMGripGroups> desiredGroups = newAffectedOcmGroups.getOrDefault(
                        currentNodeId, Collections.emptyList());
                Node cfgNode = changedObject.getChangedPhyNode(currentNodeId);
                if (cfgNode != null && !nodeId.equals(currentNodeId)) {
                    changedObject.addChangedPhyNode(withOcmGroups(cfgNode,
                            mergeOcmGroups(cfgNode, entry.getValue(), desiredGroups)));
                }
                if (updateOperationalState) {
                    Node opNode = changedObject.getChangedPhyOpNode(currentNodeId);
                    if (opNode != null && !nodeId.equals(currentNodeId)) {
                        changedObject.addChangedPhyOpNode(withOcmGroups(opNode,
                                mergeOcmGroups(opNode, entry.getValue(), desiredGroups)));
                    }
                }
            }
        }

        private Node withOcmGroups(Node node, List<OCMGripGroups> groups) {
            Physical physical = node.getAugmentation(Node1.class).getPhysical();
            return new NodeBuilder(node)
                    .addAugmentation(Node1.class, new Node1Builder()
                            .setPhysical(new PhysicalBuilder(physical)
                                    .setOCMGripGroups(groups)
                                    .build())
                            .build())
                    .build();
        }

        private List<InternalLinks> findMergedInternalLinks(Node cfgNode) {
            Physical physical = cfgNode.getAugmentation(Node1.class).getPhysical();
            if (physical.getInternalLinks() == null) {
                return new ArrayList<>();
            }
            return physical.getInternalLinks().stream()
                    .filter(il -> il.getLinkRef() != null)
                    .filter(il -> PhysicalLinkIdNamingRule.isOtsLink(il.getLinkRef()))
                    .filter(il -> mergedLinkIds.contains(il.getLinkRef()))
                    .collect(Collectors.toList());
        }

        private List<InternalLinks> findInternalLinks(Node node, List<String> linkIds) {
            Physical physical = node.getAugmentation(Node1.class).getPhysical();
            if (physical.getInternalLinks() == null) {
                return new ArrayList<>();
            }
            return physical.getInternalLinks().stream()
                    .filter(il -> il.getLinkRef() != null && linkIds.contains(il.getLinkRef()))
                    .collect(Collectors.toList());
        }

        private Node buildInternalLinkOnlyNode(Node node, List<InternalLinks> internalLinks) {
            Physical physical = node.getAugmentation(Node1.class).getPhysical();
            return new NodeBuilder(node)
                    .setTerminationPoint(new ArrayList<TerminationPoint>())
                    .addAugmentation(Node1.class, new Node1Builder()
                            .setPhysical(new PhysicalBuilder(physical)
                                    .setSystem(null)
                                    .setDcn(null)
                                    .setInternalLinks(internalLinks)
                                    .setCrossConnections(new ArrayList<CrossConnections>())
                                    .setEquipments(new ArrayList<Equipments>())
                                    .setOCMGripGroups(new ArrayList<OCMGripGroups>())
                                    .setProperties(new PropertiesBuilder().setProperty(new ArrayList<>()).build())
                                    .build())
                            .build())
                    .build();
        }

        private Node removeInternalLinks(Node node, List<InternalLinks> removed) {
            Set<String> removedLinkNames = removed.stream()
                    .map(InternalLinks::getLinkName)
                    .collect(Collectors.toSet());
            Physical physical = node.getAugmentation(Node1.class).getPhysical();
            List<InternalLinks> newList = physical.getInternalLinks() == null
                    ? new ArrayList<>()
                    : physical.getInternalLinks().stream()
                            .filter(il -> !removedLinkNames.contains(il.getLinkName()))
                            .collect(Collectors.toList());
            return withInternalLinks(node, newList);
        }

        private Node addInternalLinks(Node node, List<InternalLinks> added) {
            Physical physical = node.getAugmentation(Node1.class).getPhysical();
            Map<String, InternalLinks> merged = new HashMap<>();
            if (physical.getInternalLinks() != null) {
                physical.getInternalLinks().forEach(il -> merged.put(il.getLinkName(), il));
            }
            added.forEach(il -> merged.put(il.getLinkName(), il));
            return withInternalLinks(node, new ArrayList<>(merged.values()));
        }

        private Node withInternalLinks(Node node, List<InternalLinks> internalLinks) {
            Physical physical = node.getAugmentation(Node1.class).getPhysical();
            return new NodeBuilder(node)
                    .addAugmentation(Node1.class, new Node1Builder()
                            .setPhysical(new PhysicalBuilder(physical)
                                    .setInternalLinks(internalLinks)
                                    .build())
                            .build())
                    .build();
        }

        private StepRecord newStepRecord(Node node) {
            Physical physical = node.getAugmentation(Node1.class).getPhysical();
            return new StepRecord(node.getNodeId().getValue(), physical.getFriendlyName(),
                    physical.getIp() == null ? "" : physical.getIp());
        }

        private void recordStep(StepRecord stepRecord) {
            if (lifeService != null) {
                lifeService.logStatusChanged(stepRecord);
            }
        }

        private StepResult configNe(Node node) {
            StepResult result = new StepResult(node.getNodeId().getValue());
            if (isEmpty(node)) {
                return result;
            }
            Physical physical = node.getAugmentation(Node1.class).getPhysical();
            if (physical.getIp() == null && implConfig.isWriteWithoutIP()) {
                return result;
            }
            try {
                ConfigNeOutput output = neManagerRpc.configNe(node);
                return extractFailObj(result, output.getFailObj());
            } catch (Exception e) {
                result.addError(node.getNodeId().getValue(),
                        new CommonException(CommonExceptionType.CANNOT_FIND_COOPERATOR,
                                "internal calling error when neMgr.configNe"));
                return result;
            }
        }

        private StepResult removeResource(Node node) {
            StepResult result = new StepResult(node.getNodeId().getValue());
            if (isEmpty(node)) {
                return result;
            }
            Physical physical = node.getAugmentation(Node1.class).getPhysical();
            if (physical.getIp() == null && implConfig.isWriteWithoutIP()) {
                return result;
            }
            try {
                RemoveResourceOutput output = neManagerRpc.removeResource(node);
                return extractFailObj(result, output.getFailObj());
            } catch (Exception e) {
                result.addError(node.getNodeId().getValue(),
                        new CommonException(CommonExceptionType.CANNOT_FIND_COOPERATOR,
                                "internal calling error when neMgr.removeResource"));
                return result;
            }
        }

        private boolean isEmpty(Node node) {
            Physical physical = node.getAugmentation(Node1.class).getPhysical();
            return (node.getTerminationPoint() == null || node.getTerminationPoint().isEmpty())
                    && (physical.getInternalLinks() == null || physical.getInternalLinks().isEmpty())
                    && (physical.getCrossConnections() == null || physical.getCrossConnections().isEmpty())
                    && (physical.getEquipments() == null || physical.getEquipments().isEmpty())
                    && (physical.getOCMGripGroups() == null || physical.getOCMGripGroups().isEmpty());
        }

        private StepResult extractFailObj(StepResult result, FailObj failObj) {
            if (failObj == null || failObj.getObject() == null || failObj.getObject().isEmpty()) {
                return result;
            }
            failObj.getObject().forEach(obj -> {
                if (obj.getMessageInfo() != null && !obj.getMessageInfo().isEmpty()) {
                    result.addError(obj.getObjectId(),
                            new CommonException(CommonExceptionType.DEVICE_ERROR, obj.getMessageInfo()));
                }
            });
            return result;
        }

        private void updateStepRecord(StepRecord stepRecord, List<InternalLinks> links,
                StepResult result) {
            for (InternalLinks link : links) {
                String name = String.format(FORMAT_IL, link.getLinkRef(), link.getLinkName());
                Iterator<StepResult.ErrorInfo> iter = result.getError().iterator();
                boolean found = false;
                while (iter.hasNext()) {
                    StepResult.ErrorInfo error = iter.next();
                    if (error.getObjId().equals(link.getLinkName())) {
                        stepRecord.updatePropertyWithError(name, STATUS_FAILURE,
                                error.getException().getMessage());
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    stepRecord.updateProperty(name, STATUS_SUCCESS);
                }
            }
            if (result.hasError() && !links.isEmpty()) {
                InternalLinks first = links.get(0);
                stepRecord.updatePropertyWithError(
                        String.format(FORMAT_IL, first.getLinkRef(), first.getLinkName()),
                        STATUS_FAILURE, result.getError().get(0).getException().getMessage());
            }
        }

        private void updateOcmStepRecord(StepRecord stepRecord, OCMGripGroups group,
                StepResult result) {
            String name = String.format(FORMAT_OCM, stepRecord.getNodeId(), group.getIndex(),
                    group.getSlot());
            if (result.hasError()) {
                stepRecord.updatePropertyWithError(name, STATUS_FAILURE,
                        result.getError().get(0).getException().getMessage());
            } else {
                stepRecord.updateProperty(name, STATUS_SUCCESS);
            }
        }

        private void failIfNeeded(StepResult result) {
            if (result.hasError()) {
                throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                        result.getError().get(0).getException().getMessage());
            }
        }
    }

    static List<OCMGripGroups> mergeOcmGroups(Node node, Set<OCMGripGroupsKey> affectedKeys,
            List<OCMGripGroups> desiredGroups) {
        Physical physical = node.getAugmentation(Node1.class).getPhysical();
        Map<OCMGripGroupsKey, OCMGripGroups> merged = new LinkedHashMap<>();
        if (physical.getOCMGripGroups() != null) {
            physical.getOCMGripGroups().stream()
                    .filter(group -> !affectedKeys.contains(group.getKey()))
                    .forEach(group -> merged.put(group.getKey(), group));
        }
        desiredGroups.forEach(group -> merged.put(group.getKey(), group));
        return new ArrayList<>(merged.values());
    }

    static boolean shouldConfigureDevices(String nodeOperationState,
            ImplementState originalSiteLinkState) {
        return nodeOperationState == null
                && !ImplementState.Allocate.equals(originalSiteLinkState);
    }

    static void validateRemovalImplementState(BigInteger taskInfoId, ImplementState implementState) {
        if (taskInfoId == null
                && !ImplementState.Allocate.equals(implementState)
                && !ImplementState.Implement.equals(implementState)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "siteLink must be Allocate or Implement to remove a node without task-info-id: "
                            + implementState);
        }
    }

    static boolean canRemoveNode(String nodeOperationState) {
        return nodeOperationState == null
                || SiteLinkNodeOperationState.WAITING_INSERT_APPLY.equals(nodeOperationState)
                || SiteLinkNodeOperationState.INSERT_APPLY_FAILED_BEFORE_DEVICE.equals(
                        nodeOperationState);
    }

    static boolean shouldReleaseRemovedNodeIp(Physical configPhysical, Physical opPhysical) {
        return configPhysical != null
                && opPhysical != null
                && hasIp(configPhysical)
                && hasIp(opPhysical)
                && SupervisionStatusType.Monitoring.equals(configPhysical.getSupervisionStatus());
    }

    private static boolean hasIp(Physical physical) {
        return physical.getIp() != null && !physical.getIp().trim().isEmpty();
    }

    static boolean isUnregistered(SupervisionStatusType supervisionStatus,
            CommunicationStatusType communicationStatus) {
        return SupervisionStatusType.Unmonitored.equals(supervisionStatus)
                && CommunicationStatusType.Broken.equals(communicationStatus);
    }

    static ImplementState finalImplementState(ImplementState originalSiteLinkState) {
        return ImplementState.Allocate.equals(originalSiteLinkState)
                ? ImplementState.Allocate : ImplementState.Implement;
    }

    static final class FrequencySnapshot {
        private final Long lower;
        private final Long upper;

        private FrequencySnapshot(Long lower, Long upper) {
            this.lower = lower;
            this.upper = upper;
        }

        static FrequencySnapshot from(Link ochLink) {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1 ochAug =
                    ochLink.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class);
            if (ochAug == null || ochAug.getOch() == null) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "OCH link has no OCH attributes: " + ochLink.getLinkId().getValue());
            }
            Long lower = ochAug.getOch().getLowerFrequency() == null ? null
                    : ochAug.getOch().getLowerFrequency().getValue().longValue();
            Long upper = ochAug.getOch().getUpperFrequency() == null ? null
                    : ochAug.getOch().getUpperFrequency().getValue().longValue();
            return new FrequencySnapshot(lower, upper);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof FrequencySnapshot)) {
                return false;
            }
            FrequencySnapshot that = (FrequencySnapshot) other;
            return Objects.equals(lower, that.lower) && Objects.equals(upper, that.upper);
        }

        @Override
        public int hashCode() {
            return Objects.hash(lower, upper);
        }

        @Override
        public String toString() {
            return "[" + lower + "," + upper + "]";
        }
    }
}

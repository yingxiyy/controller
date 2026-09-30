package net.flex.dci.otn.controller.implement.site.nbi.impl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.implement.common.config.ImplConfig;
import net.flex.dci.otn.controller.implement.common.impl.ConfigNeSequence;
import net.flex.dci.otn.controller.implement.common.impl.ImplActionType;
import net.flex.dci.otn.controller.implement.common.impl.OcmUpdator;
import net.flex.dci.otn.controller.implement.common.impl.PhysicalNode;
import net.flex.dci.otn.controller.implement.common.impl.StepResult;
import net.flex.dci.otn.controller.implement.common.lifecycle.LifeCycleSevice;
import net.flex.dci.otn.controller.implement.common.recorder.StepRecord;
import net.flex.dci.otn.controller.implement.common.service.MyExecutor;
import net.flex.dci.otn.controller.implement.common.utils.OpNodeMerger;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import net.flex.dci.otn.controller.allocate.link.site.SiteLinkNodeOperationState;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.RemoveResourceOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.ne.result.FailObj;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CommunicationStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinksBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.attributes.OCMGripGroups;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ApplyNodeOnSiteLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ApplyNodeOnSiteLinkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ApplyNodeOnSiteLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplyNodeOnSiteLinkService {

    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAILURE = "failure";
    private static final String FORMAT_IL = "link@%s@%s";
    private static final String CHASSIS = "CHASSIS";
    private static final String OSC_SUFFIX = "OSC";
    private static final Set<String> INSERTED_NODE_IGNORED_EQUIPMENT_TYPES =
            new HashSet<>(Arrays.asList("FAN", "PSU", "CU", "PANEL"));

    private final SiteLinkDao siteLinkDao;
    private final OchLinkDao ochLinkDao;
    private final MultipleTransaction mongoTransaction;
    private final NeManagerRpc neManagerRpc;
    private final ImplConfig implConfig;
    private final MyExecutor executor;

    private static final AtomicLong TASK_GROUP_ID_GENERATOR = new AtomicLong(System.currentTimeMillis());
    /**
     * Start the device-side action after allocate insert-node has already changed the DB.
     * The RPC returns async immediately, consistent with the normal siteLink implement flow.
     */
    public ApplyNodeOnSiteLinkOutput start(ApplyNodeOnSiteLinkInput input, String who) {
        if (input.getSiteLinkId() == null || input.getSiteLinkId().trim().isEmpty()) {
            return failed("site-link-id is required");
        }
        if (input.getNodeId() == null || input.getNodeId().trim().isEmpty()) {
            return failed("node-id is required");
        }

        Link siteLink;
        try {
            siteLink = transitionToInsertApplying(input.getSiteLinkId(), input.getNodeId());
        } catch (CommonException e) {
            return failed(e.getMessage());
        }

        Site siteAttr = siteLink.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                .getSite();
        LifeCycleSevice lifeService = new LifeCycleSevice();
        try {
            lifeService.logStartLinkImpl(siteLink.getLinkId().getValue(),
                    TaskInfoMessage.ResourceType.siteLink, siteAttr.getFriendlyName(),
                    ActionType.implement, who, TASK_GROUP_ID_GENERATOR.incrementAndGet());

            executor.lazyDo(() -> {
                String msg = null;
                try {
                    new ApplyNodeOnSiteLinkTask(input.getSiteLinkId(), input.getNodeId(),
                            input.getTaskInfoId(), input.getTaskDetail(), lifeService).run();
                } catch (Exception e) {
                    log.error("apply inserted node on siteLink error", e);
                    msg = ExceptionUtils.getRootCauseMessage(e);
                } finally {
                    lifeService.logEndLinkImpl(msg);
                }
            });
        } catch (RuntimeException e) {
            markInsertApplyFailedSafely(input.getSiteLinkId(), input.getNodeId(), e);
            return failed(ExceptionUtils.getRootCauseMessage(e));
        }

        return new ApplyNodeOnSiteLinkOutputBuilder()
                .setReturnCode(RpcResultType.AcceptAndStartAsync)
                .build();
    }

    private ApplyNodeOnSiteLinkOutput failed(String message) {
        return new ApplyNodeOnSiteLinkOutputBuilder()
                .setReturnCode(RpcResultType.OtherError)
                .setReturnMessage(message)
                .build();
    }

    private Link transitionToInsertApplying(String siteLinkId, String insertedNodeId) {
        ZkResourceLock stateLocker = new ZkResourceLock();
        try {
            stateLocker.addResource(siteLinkId);
            stateLocker.getLock();
            Link siteLink = siteLinkDao.getSiteLinkById(siteLinkId);
            if (siteLink == null) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "siteLink not found: " + siteLinkId);
            }
            if (!SiteLinkNodeOperationState.canStartInsertApply(siteLink)) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "siteLink is not waiting for inserted node apply, current state: "
                                + SiteLinkNodeOperationState.get(siteLink));
            }
            validateOperationNodeId(siteLink, insertedNodeId);
            ChangedObject validationContext = new ChangedObject();
            if (validationContext.getChangedPhyNode(insertedNodeId) == null) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "inserted node not found: " + insertedNodeId);
            }
            Link applyingSiteLink = SiteLinkNodeOperationState.set(
                    siteLink, SiteLinkNodeOperationState.INSERT_APPLYING, insertedNodeId);
            ChangedObject stateChange = new ChangedObject();
            stateChange.addChangedSiteLink(applyingSiteLink);
            mongoTransaction.save(stateChange);
            return applyingSiteLink;
        } finally {
            stateLocker.unlock();
        }
    }

    private String markInsertApplyFailedSafely(String siteLinkId, String insertedNodeId,
            RuntimeException originalError) {
        try {
            return transitionApplyingToFailed(siteLinkId, insertedNodeId);
        } catch (RuntimeException stateError) {
            originalError.addSuppressed(stateError);
            log.error("failed to mark inserted node apply state for siteLink {}",
                    siteLinkId, stateError);
            return null;
        }
    }

    private String transitionApplyingToFailed(String siteLinkId, String insertedNodeId) {
        ZkResourceLock stateLocker = new ZkResourceLock();
        try {
            stateLocker.addResource(siteLinkId);
            stateLocker.getLock();
            Link siteLink = siteLinkDao.getSiteLinkById(siteLinkId);
            String applyingState = SiteLinkNodeOperationState.get(siteLink);
            String failedState = failedStateFor(applyingState);
            if (siteLink == null || failedState == null) {
                return null;
            }
            validateOperationNodeId(siteLink, insertedNodeId);
            ChangedObject stateChange = new ChangedObject();
            Link failedSiteLink = SiteLinkNodeOperationState.set(
                    siteLink, failedState, insertedNodeId);
            if (SiteLinkNodeOperationState.INSERT_APPLY_FAILED_AFTER_DEVICE_STARTED.equals(
                    failedState)) {
                failedSiteLink = withSiteLinkImplementState(failedSiteLink,
                        ImplementState.PartialImplement, AdminStatus.Up);
            }
            stateChange.addChangedSiteLink(failedSiteLink);
            mongoTransaction.save(stateChange);
            return failedState;
        } finally {
            stateLocker.unlock();
        }
    }

    private Link transitionToDeviceApplyStarted(Link siteLink, String insertedNodeId) {
        if (!SiteLinkNodeOperationState.INSERT_APPLYING.equals(
                SiteLinkNodeOperationState.get(siteLink))) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "siteLink is not in pre-device insert apply state: "
                            + SiteLinkNodeOperationState.get(siteLink));
        }
        validateOperationNodeId(siteLink, insertedNodeId);
        Link deviceStartedSiteLink = SiteLinkNodeOperationState.set(siteLink,
                SiteLinkNodeOperationState.INSERT_APPLYING_DEVICE_STARTED, insertedNodeId);
        deviceStartedSiteLink = withSiteLinkImplementState(deviceStartedSiteLink,
                ImplementState.Doimplementing, AdminStatus.Up);
        ChangedObject stateChange = new ChangedObject();
        stateChange.addChangedSiteLink(deviceStartedSiteLink);
        mongoTransaction.save(stateChange);
        return deviceStartedSiteLink;
    }

    static String failedStateFor(String applyingState) {
        if (SiteLinkNodeOperationState.INSERT_APPLYING.equals(applyingState)) {
            return SiteLinkNodeOperationState.INSERT_APPLY_FAILED_BEFORE_DEVICE;
        }
        if (SiteLinkNodeOperationState.INSERT_APPLYING_DEVICE_STARTED.equals(applyingState)) {
            return SiteLinkNodeOperationState.INSERT_APPLY_FAILED_AFTER_DEVICE_STARTED;
        }
        return null;
    }

    private void validateOperationNodeId(Link siteLink, String insertedNodeId) {
        String operationNodeId = SiteLinkNodeOperationState.getNodeId(siteLink);
        if (operationNodeId == null || operationNodeId.trim().isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "siteLink node-operation-node-id is missing");
        }
        if (!operationNodeId.equals(insertedNodeId)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "siteLink node operation belongs to " + operationNodeId
                            + ", not requested node " + insertedNodeId);
        }
    }

    static Link withSiteLinkImplementState(Link siteLink, ImplementState implementState,
            AdminStatus adminStatus) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1 augmentation =
                siteLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class);
        Site site = augmentation.getSite();
        return new LinkBuilder(siteLink)
                .addAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class,
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder(
                                augmentation)
                                .setSite(new SiteBuilder(site)
                                        .setImplementState(implementState)
                                        .setAdminState(adminStatus)
                                        .build())
                                .build())
                .build();
    }


    static Node normalizeInsertedNodeOcmGroups(Node node, List<OCMGripGroups> ignoredOcmGroups) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical physical =
                node.getAugmentation(Node1.class).getPhysical();
        if (physical.getOCMGripGroups() != null || !isIlaOrDgeNode(physical)) {
            return node;
        }
        return withOcmGroups(node, new ArrayList<OCMGripGroups>());
    }

    static Node filterDgeOchXcsForCurrentApply(Node node, Set<String> activeOchXcIds) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical physical =
                node.getAugmentation(Node1.class).getPhysical();
        if (!hasEquipment(physical, EquipType.DGE)
                || physical.getCrossConnections() == null
                || physical.getCrossConnections().isEmpty()) {
            return node;
        }
        Set<String> activeIds = activeOchXcIds == null ? new HashSet<>() : activeOchXcIds;
        List<CrossConnections> filteredXcs = physical.getCrossConnections().stream()
                .filter(xc -> xc.getWssChannel() == null
                        || (xc.getCrossConnectionId() != null
                                && activeIds.contains(xc.getCrossConnectionId().getValue())))
                .collect(Collectors.toList());
        return new NodeBuilder(node)
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder(physical)
                                .setCrossConnections(filteredXcs)
                                .build())
                        .build())
                .build();
    }

    static Node prepareInsertedNodeEquipmentPayload(Node node, Set<String> routeTpIds) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical physical =
                node.getAugmentation(Node1.class).getPhysical();
        List<Equipments> configuredEquipments = new ArrayList<>();
        Set<String> ignoredEquipmentIds = new HashSet<>();
        if (physical.getEquipments() != null) {
            for (Equipments equipment : physical.getEquipments()) {
                if (EquipType.EMPTY.equals(equipment.getEquipType())
                        || INSERTED_NODE_IGNORED_EQUIPMENT_TYPES.contains(
                                equipment.getEquipTypeConfiged())) {
                    ignoredEquipmentIds.add(equipment.getEquipmentId());
                    continue;
                }
                if (CHASSIS.equals(equipment.getEquipTypeConfiged())) {
                    configuredEquipments.add(new EquipmentsBuilder(equipment)
                            .setAdminState(null)
                            .build());
                } else if (EquipType.TRANSCEIVER.equals(equipment.getEquipType())
                        && equipment.getEquipmentId() != null
                        && equipment.getEquipmentId().endsWith(OSC_SUFFIX)) {
                    configuredEquipments.add(new EquipmentsBuilder(equipment)
                            .setAdminState(AdminStatus.Up)
                            .setImplementState(ImplementState.Implement)
                            .build());
                } else {
                    configuredEquipments.add(equipment);
                }
            }
        }
        // Only TPs used by the inserted node's two split OTS links belong in the device payload.
        List<TerminationPoint> configuredTps = new ArrayList<>();
        Set<String> effectiveRouteTpIds = routeTpIds == null ? Collections.emptySet() : routeTpIds;
        if (node.getTerminationPoint() != null) {
            for (TerminationPoint tp : node.getTerminationPoint()) {
                if (tp == null || tp.getTpId() == null
                        || !effectiveRouteTpIds.contains(tp.getTpId().getValue())) {
                    continue;
                }
                String equipmentRef = getTpEquipmentRef(tp);
                if (equipmentRef == null || !ignoredEquipmentIds.contains(equipmentRef)) {
                    configuredTps.add(tp);
                }
            }
        }
        return new NodeBuilder(node)
                .setTerminationPoint(configuredTps)
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder(physical)
                                .setEquipments(configuredEquipments)
                                .build())
                        .build())
                .build();
    }

    private static String getTpEquipmentRef(TerminationPoint tp) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1 tpAug =
                tp.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1.class);
        if (tpAug == null || tpAug.getPhysical() == null) {
            return null;
        }
        return tpAug.getPhysical().getEquipmentRef();
    }


    private static Node withOcmGroups(Node node, List<OCMGripGroups> ocmGroups) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical physical =
                node.getAugmentation(Node1.class).getPhysical();
        return new NodeBuilder(node)
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder(physical)
                                .setOCMGripGroups(ocmGroups)
                                .build())
                        .build())
                .build();
    }

    static Node applyRebuiltOcmGroups(Node insertedNode, Node rebuiltOcmNode) {
        List<OCMGripGroups> rebuiltGroups = new ArrayList<>();
        if (rebuiltOcmNode != null && rebuiltOcmNode.getAugmentation(Node1.class) != null
                && rebuiltOcmNode.getAugmentation(Node1.class).getPhysical() != null
                && rebuiltOcmNode.getAugmentation(Node1.class).getPhysical()
                        .getOCMGripGroups() != null) {
            rebuiltGroups.addAll(rebuiltOcmNode.getAugmentation(Node1.class).getPhysical()
                    .getOCMGripGroups());
        }
        return withOcmGroups(insertedNode, rebuiltGroups);
    }

    private static boolean isIlaOrDgeNode(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical physical) {
        return hasEquipment(physical, EquipType.ILA) || hasEquipment(physical, EquipType.DGE);
    }

    private static boolean hasEquipment(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical physical,
            EquipType equipType) {
        return physical.getEquipments() != null
                && physical.getEquipments().stream()
                        .anyMatch(equipment -> equipType.equals(equipment.getEquipType()));
    }

    static TaskInfoMessage buildCompletedInsertTaskInfo(BigInteger taskInfoId, String taskDetail, long endTime,
            Long groupId) {
        if (taskInfoId == null) {
            return null;
        }
        return TaskInfoMessage.builder()
                .id(taskInfoId.longValue())
                .detail(taskDetail)
                .successfully(true)
                .errorReason("confirmed")
                .endTime(endTime)
                .groupId(groupId)
                .root(true)
                .build();
    }

    static TaskInfoMessage buildFailedInsertTaskInfo(BigInteger taskInfoId, String taskDetail, Long endTime,
            Long groupId, String errorReason) {
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

    static String getInsertedNodeApplyError(StepRecord stepRecord, Node changedNode) {
        if (stepRecord != null && stepRecord.getProperties() != null
                && stepRecord.getProperties().getPropertyList() != null) {
            for (StepRecord.Property property : stepRecord.getProperties().getPropertyList()) {
                if (STATUS_FAILURE.equals(property.getValue())) {
                    return property.getErrorMsg() == null || property.getErrorMsg().isEmpty()
                            ? property.getName() + " failed"
                            : property.getErrorMsg();
                }
            }
        }
        if (changedNode == null || changedNode.getAugmentation(Node1.class) == null
                || changedNode.getAugmentation(Node1.class).getPhysical() == null) {
            return "inserted node result is missing";
        }
        if (ImplementState.PartialImplement.equals(
                changedNode.getAugmentation(Node1.class).getPhysical().getImplementState())) {
            return "inserted node implementation is partial: "
                    + changedNode.getNodeId().getValue();
        }
        return null;
    }

    private void completeInsertTaskInfo(BigInteger taskInfoId, String taskDetail, Long groupId) {
        TaskInfoMessage message = buildCompletedInsertTaskInfo(
                taskInfoId, taskDetail, System.currentTimeMillis(), groupId);
        if (message == null) {
            return;
        }
        log.info("complete insert-node-in-site-link taskInfo {}", taskInfoId);
        TaskInfoMessager.sendMessage(message);
    }

    private void failInsertTaskInfo(BigInteger taskInfoId, String taskDetail, Long groupId,
            String errorReason, String failedState) {
        TaskInfoMessage message = buildFailedInsertTaskInfo(
                taskInfoId, taskDetail, failedTaskEndTime(failedState, System.currentTimeMillis()),
                groupId, errorReason);
        if (message == null) {
            return;
        }
        log.info("fail insert-node-in-site-link taskInfo {}", taskInfoId);
        TaskInfoMessager.sendMessage(message);
    }

    static Long failedTaskEndTime(String failedState, long endTime) {
        return SiteLinkNodeOperationState.INSERT_APPLY_FAILED_BEFORE_DEVICE.equals(failedState)
                ? null : endTime;
    }

    private class ApplyNodeOnSiteLinkTask implements Runnable {

        private final String siteLinkId;
        private final String insertedNodeId;
        private final BigInteger taskInfoId;
        private final LifeCycleSevice lifeService;
        private final ZkResourceLock locker = new ZkResourceLock();
        private final ChangedObject changedObject = new ChangedObject();
        private final String taskDetail;

        private InsertedNodeRoute route;
        private boolean deviceApplyStarted;

        ApplyNodeOnSiteLinkTask(String siteLinkId, String insertedNodeId, BigInteger taskInfoId, String taskDetail,
                LifeCycleSevice lifeService) {
            this.siteLinkId = siteLinkId;
            this.insertedNodeId = insertedNodeId;
            this.taskInfoId = taskInfoId;
            this.lifeService = lifeService;
            this.taskDetail = taskDetail;
        }

        /**
         * Apply the inserted node to real devices in the only order that preserves resources:
         * remove old A-B internalLink from A/B, create new A-N/N-B internalLinks, then write N.
         */
        @Override
        public void run() {
            boolean resourcesLocked = false;
            try {
                Link siteLink = changedObject.getChangedSiteLink(siteLinkId);
                route = resolveInsertedRoute(siteLink);
                lockResources();
                resourcesLocked = true;

                siteLink = siteLinkDao.getSiteLinkById(siteLinkId);
                if (!SiteLinkNodeOperationState.INSERT_APPLYING.equals(
                        SiteLinkNodeOperationState.get(siteLink))) {
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                            "siteLink inserted node apply state changed: "
                                    + SiteLinkNodeOperationState.get(siteLink));
                }
                validateOperationNodeId(siteLink, insertedNodeId);

                validateInsertedNodeReadiness();
                removeOldInternalLinksOnAZ();
                createNewInternalLinksOnAZ();
                implementInsertedNode();
                markTopologyImplemented(siteLink);
                store2DB();
                completeInsertTaskInfo(taskInfoId, taskDetail, lifeService.getGroupId());
            } catch (RuntimeException e) {
                if (resourcesLocked) {
                    locker.unlock();
                    resourcesLocked = false;
                }
                String failedState = markInsertApplyFailedSafely(siteLinkId, insertedNodeId, e);
                failInsertTaskInfoSafely(e, failedState);
                throw e;
            } finally {
                if (resourcesLocked) {
                    locker.unlock();
                }
            }
        }


        private void failInsertTaskInfoSafely(RuntimeException originalError, String failedState) {
            try {
                failInsertTaskInfo(taskInfoId, taskDetail, lifeService.getGroupId(),
                        ExceptionUtils.getRootCauseMessage(originalError), failedState);
            } catch (RuntimeException taskError) {
                originalError.addSuppressed(taskError);
                log.error("fail insert-node-in-site-link taskInfo failed", taskError);
            }
        }

        private void lockResources() {
            locker.addResource(siteLinkId);
            locker.addResource(route.insertedNodeId);
            route.azNodeIds.forEach(locker::addResource);
            locker.getLock();
            log.info("locked resources for apply-node-on-site-link: siteLink={}, nodes={}",
                    siteLinkId, route.allNodeIds());
        }

        /**
         * Validate that the inserted node is ready before any device operation.
         */
        private void validateInsertedNodeReadiness() {
            String nodeId = route.insertedNodeId;
            Physical configPhysical = requirePhysical(changedObject.getChangedPhyNode(nodeId), nodeId, null);
            if (implConfig.isWriteWithoutIP()) {
                return;
            }
            validateIp(configPhysical, nodeId, "config");
            Physical opPhysical = requirePhysical(changedObject.getChangedPhyOpNode(nodeId), nodeId, configPhysical.getFriendlyName());
            validateIp(opPhysical, nodeId, "operational");
            if (!CommunicationStatusType.SyncFinished.equals(configPhysical.getCommunicationStatus())) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "The " + configPhysical.getFriendlyName() + " is not in a synchronized completion state");
            }
        }

        private Physical requirePhysical(Node node, String nodeId, String friendlyName) {
            Node1 nodeAugmentation = node == null ? null : node.getAugmentation(Node1.class);
            if (nodeAugmentation == null || nodeAugmentation.getPhysical() == null) {
                if(friendlyName != null){
                    log.error("inserted op-phy-node physical data not found: {}", nodeId);
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "The IP address of " + friendlyName + " network element has not been bound yet");
                }else{
                    log.error("inserted config-phy-node physical data not found: {}", nodeId);
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "inserted node physical data not found: " + nodeId);
                }

            }
            return nodeAugmentation.getPhysical();
        }

        private void validateIp(Physical physical, String nodeId, String datastore) {
            if (physical.getIp() != null && !physical.getIp().trim().isEmpty()) {
                return;
            }
            log.error("inserted {} node has no IP, cannot write to device: {} ({})",
                    datastore, nodeId, physical.getFriendlyName());
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "The IP address of " +  physical.getFriendlyName() + " network element has not been bound yet");
        }

        private InsertedNodeRoute resolveInsertedRoute(Link siteLink) {
            Node insertedNode = changedObject.getChangedPhyNode(insertedNodeId);
            if (insertedNode == null) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "inserted node not found: " + insertedNodeId);
            }

            ApplyNodeOnSiteLinkRouteResolver.Result result = ApplyNodeOnSiteLinkRouteResolver
                    .resolve(siteLink, insertedNodeId, changedObject::getChangedPhyLink);
            return new InsertedNodeRoute(result.insertedNodeId, result.azNodeIds,
                    result.splitOtsLinks, result.insertedNodeTpIds);
        }
        /**
         * Delete the old A-B internalLink from A/B devices.
         * Allocate has already removed it from config DB, so opNode is the only reliable source
         * for discovering what still exists on the physical devices.
         */
        private void removeOldInternalLinksOnAZ() {
            route.azNodeIds.forEach(nodeId -> {
                Node opNode = changedObject.getChangedPhyOpNode(nodeId);
                if (opNode == null) {
                    log.error("opNode not found: {}", nodeId);
                    return;
                }
                // 配置库在 allocate 阶段已经删除老 A-B internalLink，只能从 opNode 找出设备上仍存在的老资源并删除。
                List<InternalLinks> oldInternalLinks = findOldInternalLinks(opNode, nodeId);
                if (oldInternalLinks.isEmpty()) {
                    log.info("no old A-B internalLink found on {}", nodeId);
                    return;
                }

                Node removeNode = buildInternalLinkOnlyNode(changedObject.getChangedPhyNode(nodeId), oldInternalLinks);
                StepRecord stepRecord = newStepRecord(removeNode);
                StepResult result = removeResource(removeNode);
                updateStepRecord(stepRecord, oldInternalLinks, result);
                lifeService.logStatusChanged(stepRecord);
                failIfNeeded(result);
                // Config DB has removed the old A-B internalLink; remove the existing device-side resource from opNode.
                changedObject.addChangedPhyOpNode(removeInternalLinks(opNode, oldInternalLinks));
            });
        }

        /**
         * Pick old OTS internalLinks that connect the two original edge nodes and are not one of
         * the new split OTS links. This avoids deleting unrelated OTS resources on the same NE.
         */
        private List<InternalLinks> findOldInternalLinks(Node opNode, String nodeId) {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical physical =
                    opNode.getAugmentation(Node1.class).getPhysical();
            if (physical.getInternalLinks() == null) {
                return new ArrayList<>();
            }

            Set<String> splitLinkIds = route.splitOtsLinks.stream()
                    .map(link -> link.getLinkId().getValue())
                    .collect(Collectors.toSet());
            String peerNodeId = route.azNodeIds.stream()
                    .filter(id -> !id.equals(nodeId))
                    .findFirst()
                    .orElse("");

            return physical.getInternalLinks().stream()
                    .filter(il -> il.getLinkRef() != null)
                    .filter(il -> PhysicalLinkIdNamingRule.isOtsLink(il.getLinkRef()))
                    .filter(il -> !splitLinkIds.contains(il.getLinkRef()))
                    .filter(il -> linkConnects(il.getLinkRef(), nodeId, peerNodeId))
                    .collect(Collectors.toList());
        }

        private boolean linkConnects(String linkId, String firstNodeId, String secondNodeId) {
            Set<String> nodes = new HashSet<>();
            nodes.add(PhysicalLinkIdNamingRule.getNodeAId(linkId));
            nodes.add(PhysicalLinkIdNamingRule.getNodeZId(linkId));
            return nodes.contains(firstNodeId) && nodes.contains(secondNodeId);
        }

        /**
         * Create only the new split internalLinks on A/B.
         * A/B are existing devices, so writing their full node data would be unnecessarily broad.
         */
        private void createNewInternalLinksOnAZ() {
            route.azNodeIds.forEach(nodeId -> {
                Node cfgNode = changedObject.getChangedPhyNode(nodeId);
                List<InternalLinks> newInternalLinks = findSplitInternalLinks(cfgNode, nodeId);
                if (newInternalLinks.isEmpty()) {
                    log.info("no new split internalLink found on {}", nodeId);
                    return;
                }
                newInternalLinks = toImplementedInternalLinks(newInternalLinks);

                // Write only the new A-N/N-B internalLinks to A/B devices and opNode.
                Node writeNode = buildInternalLinkOnlyNode(cfgNode, newInternalLinks);
                StepRecord stepRecord = newStepRecord(writeNode);
                StepResult result = configNe(writeNode);
                updateStepRecord(stepRecord, newInternalLinks, result);
                lifeService.logStatusChanged(stepRecord);
                failIfNeeded(result);

                Node opNode = changedObject.getChangedPhyOpNode(nodeId);
                if (opNode == null) {
                    log.error("opPhyNode not found : {}", nodeId);
                } else {
                    changedObject.addChangedPhyOpNode(addInternalLinks(opNode, newInternalLinks));
                }
                // After the device write succeeds, advance only these new internalLinks to implement state.
                changedObject.addChangedPhyNode(updateInternalLinks(cfgNode, newInternalLinks));
            });
        }

        private List<InternalLinks> toImplementedInternalLinks(List<InternalLinks> internalLinks) {
            return internalLinks.stream()
                    .map(il -> new InternalLinksBuilder(il)
                            .setImplementState(ImplementState.Implement)
                            .build())
                    .collect(Collectors.toList());
        }

        private List<InternalLinks> findSplitInternalLinks(Node node, String nodeId) {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical physical =
                    node.getAugmentation(Node1.class).getPhysical();
            if (physical.getInternalLinks() == null) {
                return new ArrayList<>();
            }
            Set<String> splitLinkIds = route.splitOtsLinks.stream()
                    .filter(link -> link.getLinkId().getValue().contains(nodeId))
                    .map(link -> link.getLinkId().getValue())
                    .collect(Collectors.toSet());
            return physical.getInternalLinks().stream()
                    .filter(il -> il.getLinkRef() != null && splitLinkIds.contains(il.getLinkRef()))
                    .collect(Collectors.toList());
        }


        private Set<String> collectActiveOchXcIdsOnInsertedDge() {
            Link siteLink = changedObject.getChangedSiteLink(siteLinkId);
            Set<String> dummyLinkIds = getDummyLinkIds(siteLink);
            List<Link> ochLinks = ochLinkDao.getAllOchLinksUnderSiteLinkIds(Collections.singletonList(siteLinkId));
            if (ochLinks == null || ochLinks.isEmpty()) {
                return new HashSet<>();
            }
            return ochLinks.stream()
                    .filter(ochLink -> shouldApplyOch(ochLink, dummyLinkIds))
                    .flatMap(ochLink -> collectInsertedNodeXcIds(ochLink).stream())
                    .collect(Collectors.toSet());
        }

        private Set<String> getDummyLinkIds(Link siteLink) {
            if (siteLink == null) {
                return new HashSet<>();
            }
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1 siteAug =
                    siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class);
            if (siteAug == null || siteAug.getSite() == null || siteAug.getSite().getDummyLink() == null) {
                return new HashSet<>();
            }
            return new HashSet<>(siteAug.getSite().getDummyLink());
        }

        private boolean shouldApplyOch(Link ochLink, Set<String> dummyLinkIds) {
            if (ochLink == null || ochLink.getLinkId() == null) {
                return false;
            }
            if (dummyLinkIds.contains(ochLink.getLinkId().getValue())) {
                return true;
            }
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och och = getOch(ochLink);
            return och != null && !ImplementState.Allocate.equals(och.getImplementState());
        }

        private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och getOch(Link ochLink) {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1 ochAug =
                    ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class);
            if (ochAug == null) {
                return null;
            }
            return ochAug.getOch();
        }

        private Node rebuildInsertedNodeOcm(Node insertedNode) {
            Link siteLink = changedObject.getChangedSiteLink(siteLinkId);
            Set<String> dummyLinkIds = getDummyLinkIds(siteLink);
            List<Link> ochLinks = ochLinkDao.getAllOchLinksUnderSiteLinkIds(
                    Collections.singletonList(siteLinkId));
            List<Link> workingOchLinks = ochLinks == null
                    ? new ArrayList<>()
                    : ochLinks.stream()
                            .filter(ochLink -> shouldApplyOch(ochLink, dummyLinkIds))
                            .collect(Collectors.toList());

            OcmUpdator ocmUpdator = new OcmUpdator();
            ocmUpdator.buildOcmGroup(siteLink, workingOchLinks);
            Node rebuiltOcmNode = ocmUpdator.getOcmNodeMap().get(route.insertedNodeId);
            Node nodeWithOcm = applyRebuiltOcmGroups(insertedNode, rebuiltOcmNode);
            changedObject.addChangedPhyNode(nodeWithOcm);
            log.info("rebuilt inserted node OCM: node={}, workingOchCount={}, groupCount={}",
                    route.insertedNodeId, workingOchLinks.size(),
                    nodeWithOcm.getAugmentation(Node1.class).getPhysical()
                            .getOCMGripGroups().size());
            return nodeWithOcm;
        }

        private List<String> collectInsertedNodeXcIds(Link ochLink) {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och och = getOch(ochLink);
            if (och == null || och.getExplictRoute() == null || och.getExplictRoute().getRoute() == null) {
                return new ArrayList<>();
            }
            List<String> xcIds = new ArrayList<>();
            for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route ochRoute
                    : och.getExplictRoute().getRoute()) {
                if (ochRoute.getPrimary() != null) {
                    collectInsertedNodeXcIds(ochRoute.getPrimary().getCrossConnections(), xcIds);
                }
                if (ochRoute.getSecondary() != null) {
                    collectInsertedNodeXcIds(ochRoute.getSecondary().getCrossConnections(), xcIds);
                }
                if (ochRoute.getThird() != null) {
                    ochRoute.getThird().forEach(third -> collectInsertedNodeXcIds(third.getCrossConnections(), xcIds));
                }
            }
            return xcIds;
        }

        private void collectInsertedNodeXcIds(
                List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> routeXcs,
                List<String> xcIds) {
            if (routeXcs == null || routeXcs.isEmpty()) {
                return;
            }
            routeXcs.stream()
                    .filter(xc -> xc.getNodeRef() != null
                            && route.insertedNodeId.equals(xc.getNodeRef().getValue())
                            && xc.getCrossConnectionId() != null)
                    .map(xc -> xc.getCrossConnectionId().getValue())
                    .forEach(xcIds::add);
        }


        /**
         * N is a new NE, so reuse the normal NE implementation sequence and write full resources:
         * base info, equipment, TPs, internalLinks, XCs, and OCM data.
         */
        private void implementInsertedNode() {
            Node insertedNode = changedObject.getChangedPhyNode(route.insertedNodeId);
            if (insertedNode == null) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "inserted node not found: " + route.insertedNodeId);
            }

            // New inserted ILA/DGE follows the normal NE implementation sequence.
            Node nodeToImplement = rebuildInsertedNodeOcm(insertedNode);
            nodeToImplement = filterDgeOchXcsForCurrentApply(nodeToImplement,
                    collectActiveOchXcIdsOnInsertedDge());
            nodeToImplement = prepareInsertedNodeEquipmentPayload(nodeToImplement, route.insertedNodeTpIds);
            ConfigNeSequence sequence = new ConfigNeSequence(changedObject, nodeToImplement,
                    ImplActionType.Implement, lifeService);
            try {
                ensureDeviceApplyStarted();
                StepRecord stepRecord = sequence.prepare(new HashMap<String, PhysicalNode.ReadyResource>()).call();
                String applyError = getInsertedNodeApplyError(stepRecord, changedObject.getChangedPhyNode(route.insertedNodeId));
                if (applyError != null) {
                    throw new CommonException(CommonExceptionType.DEVICE_ERROR, applyError);
                }
            } catch (Exception e) {
                throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                        ExceptionUtils.getRootCauseMessage(e));
            }
        }

        private void ensureDeviceApplyStarted() {
            if (deviceApplyStarted) {
                return;
            }
            Link siteLink = siteLinkDao.getSiteLinkById(siteLinkId);
            transitionToDeviceApplyStarted(siteLink, insertedNodeId);
            deviceApplyStarted = true;
        }

        /**
         * Once all device actions succeed, move the DB state forward for the siteLink and the two
         * split OTS links. A/B node state is not changed globally; only their new internalLinks are.
         */
        private void markTopologyImplemented(Link siteLink) {
            Site siteAttr = siteLink.getAugmentation(
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                    .getSite();
            Set<String> currentSplitLinkIds = route.splitOtsLinks.stream()
                    .map(link -> link.getLinkId().getValue())
                    .collect(Collectors.toSet());
            boolean hasOtherAllocateOts = ApplyNodeOnSiteLinkRouteResolver.hasOtherAllocateOts(
                    siteLink, currentSplitLinkIds, changedObject::getChangedPhyLink);
            SiteBuilder siteBuilder = new SiteBuilder(siteAttr);
            if (!hasOtherAllocateOts) {
                siteBuilder.setImplementState(ImplementState.Implement)
                        .setAdminState(AdminStatus.Up);
            }
            Link newSiteLink = new LinkBuilder(siteLink)
                    .addAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class,
                            new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder()
                                    .setSite(siteBuilder.build())
                                    .build())
                    .build();
            newSiteLink = SiteLinkNodeOperationState.remove(newSiteLink);
            changedObject.unsetSiteLink(siteLinkId);
            changedObject.addChangedSiteLink(newSiteLink);

            route.splitOtsLinks.forEach(link -> {
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical physical =
                        link.getAugmentation(Link1.class).getPhysical();
                Link newLink = new LinkBuilder(link)
                        .addAugmentation(Link1.class, new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1Builder()
                                .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder(physical)
                                        .setImplementState(ImplementState.Implement)
                                        .setAdminState(AdminStatus.Up)
                                        .build())
                                .build())
                        .build();
                changedObject.addChangedPhyLink(newLink);
            });
        }

        /**
         * Build a minimal NE payload containing only internalLinks.
         * This prevents A/B device updates from accidentally rewriting equipment, TP, XC, or OCM.
         */
        private Node buildInternalLinkOnlyNode(Node node, List<InternalLinks> internalLinks) {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical physical =
                    node.getAugmentation(Node1.class).getPhysical();
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
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical physical =
                    node.getAugmentation(Node1.class).getPhysical();
            List<InternalLinks> newList = physical.getInternalLinks() == null
                    ? new ArrayList<>()
                    : physical.getInternalLinks().stream()
                            .filter(il -> !removedLinkNames.contains(il.getLinkName()))
                            .collect(Collectors.toList());
            return withInternalLinks(node, newList);
        }

        private Node addInternalLinks(Node node, List<InternalLinks> added) {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical physical =
                    node.getAugmentation(Node1.class).getPhysical();
            Map<String, InternalLinks> merged = new HashMap<>();
            if (physical.getInternalLinks() != null) {
                physical.getInternalLinks().forEach(il -> merged.put(il.getLinkName(), il));
            }
            added.forEach(il -> merged.put(il.getLinkName(), il));
            return withInternalLinks(node, new ArrayList<>(merged.values()));
        }

        private Node updateInternalLinks(Node node, List<InternalLinks> updated) {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical physical =
                    node.getAugmentation(Node1.class).getPhysical();
            Map<String, InternalLinks> updatedByName = updated.stream()
                    .collect(Collectors.toMap(InternalLinks::getLinkName, il -> il,
                            (existing, replacement) -> replacement));
            List<InternalLinks> newList = physical.getInternalLinks() == null
                    ? new ArrayList<>()
                    : physical.getInternalLinks().stream()
                            .map(il -> updatedByName.getOrDefault(il.getLinkName(), il))
                            .collect(Collectors.toList());
            return withInternalLinks(node, newList);
        }

        private Node withInternalLinks(Node node, List<InternalLinks> internalLinks) {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical physical =
                    node.getAugmentation(Node1.class).getPhysical();
            return new NodeBuilder(node)
                    .addAugmentation(Node1.class, new Node1Builder()
                            .setPhysical(new PhysicalBuilder(physical)
                                    .setInternalLinks(internalLinks)
                                    .build())
                            .build())
                    .build();
        }

        private StepRecord newStepRecord(Node node) {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical physical =
                    node.getAugmentation(Node1.class).getPhysical();
            return new StepRecord(node.getNodeId().getValue(), physical.getFriendlyName(),
                    physical.getIp() == null ? "" : physical.getIp());
        }

        private StepResult configNe(Node node) {
            StepResult result = new StepResult(node.getNodeId().getValue());
            if (isEmpty(node)) {
                return result;
            }
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical physical =
                    node.getAugmentation(Node1.class).getPhysical();
            if (physical.getIp() == null && implConfig.isWriteWithoutIP()) {
                return result;
            }
            try {
                ensureDeviceApplyStarted();
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
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical physical =
                    node.getAugmentation(Node1.class).getPhysical();
            if (physical.getIp() == null && implConfig.isWriteWithoutIP()) {
                return result;
            }
            try {
                ensureDeviceApplyStarted();
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
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical physical =
                    node.getAugmentation(Node1.class).getPhysical();
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
            failObj.getObject().forEach(obj -> result.addError(obj.getObjectId(),
                    new CommonException(CommonExceptionType.DEVICE_ERROR, obj.getMessageInfo())));
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

        private void failIfNeeded(StepResult result) {
            if (result.hasError()) {
                throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                        result.getError().get(0).getException().getMessage());
            }
        }

        /**
         * Persist config/op changes and merge newly implemented config nodes into OP.
         * A/B opNode changes are already explicit; N still needs the standard config-to-op merge.
         */
        private void store2DB() {
            mongoTransaction.save(changedObject);
            // A/B opNode 已经按 internalLink 做了精确增删，不能再用整节点 merge 覆盖；
            // 这里只把新插入的 N 按普通 NE 实施流程从 config 合并到 OP。
            new OpNodeMerger().merge(route.insertedNodeId);
        }
    }

    private static class InsertedNodeRoute {

        private final String insertedNodeId;
        private final List<String> azNodeIds;
        private final List<Link> splitOtsLinks;
        private final Set<String> insertedNodeTpIds;

        InsertedNodeRoute(String insertedNodeId, List<String> azNodeIds, List<Link> splitOtsLinks,
                Set<String> insertedNodeTpIds) {
            this.insertedNodeId = insertedNodeId;
            this.azNodeIds = azNodeIds;
            this.splitOtsLinks = splitOtsLinks;
            this.insertedNodeTpIds = insertedNodeTpIds;
        }

        private List<String> allNodeIds() {
            List<String> nodeIds = new ArrayList<>(azNodeIds);
            nodeIds.add(insertedNodeId);
            return nodeIds;
        }
    }
}

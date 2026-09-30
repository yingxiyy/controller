package net.flex.dci.otn.controller.implement.tunnel.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.enums.NeSubType;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otc.tools.lock.AbstractResourceLock;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.implement.common.utils.ApsProtectionState;
import net.flex.dci.otn.controller.implement.common.utils.RouteExtractor;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.UnregisteNeOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.OchBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.SupervisionStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.add.drop.link.lists.group.AddDropLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.TunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.PrimaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Secondary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Third;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.aps.attributes.ApsBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Keeps remove-leg retry state and commits the final topology cleanup after device success.
 **/

@Service
@Slf4j
public class ProtectionLegResourceManager {

    public static final String PROTECTION_CHANGE_SITE_LINK_IDS = "protection-change-site-link-ids";
    public static final String PROTECTION_CHANGE_ACTION_REMOVE_LEG = "remove-leg";
    public static final String PROTECTION_CHANGE_FROM_LEG_COUNT = "protection-change-from-leg-count";
    public static final String PROTECTION_CHANGE_TO_LEG_COUNT = "protection-change-to-leg-count";
    public static final String PROTECTION_CHANGE_ACTION = "protection-change-action";
    public static final String PROTECTION_CHANGE_PHASE = "protection-change-phase";
    public static final String REMOVE_LEG_RESTORE_TUNNEL = "removeLegRestoreTunnel";
    private static final String BINDING_3_RD_LEG = "binding3rdLeg";

    private static final String REMOVE_LEG_PHASE_PREPARED = "prepared";
    private static final String REMOVE_LEG_PHASE_DEVICE_DEIMPLEMENTED = "device-deimplemented";
    private static final String REMOVE_LEG_OPERATION_LOCK_PREFIX = "remove-protection-leg-operation:";

    @Autowired
    private TunnelDao tunnelDao;
    @Autowired
    private OchLinkDao ochLinkDao;
    @Autowired
    private PhyLinkDao phyLinkDao;
    @Autowired
    private PhyNodeDao phyNodeDao;
    @Autowired
    private NeManagerRpc neManagerRpc;

    private final MultipleTransaction multipleTransaction;
    private Supplier<AbstractResourceLock> resourceLockFactory = ZkResourceLock::new;

    @Autowired
    public ProtectionLegResourceManager(MultipleTransaction multipleTransaction) {
        this.multipleTransaction = multipleTransaction;
    }

    /**
     * Immutable snapshot of the leg being removed. It keeps the route resources available across marker persistence,
     * device deimplementation, and final DB cleanup.
     */
    private static class RemoveProtectionLegContext {
        private final String legName;
        private final List<String> siteLinkIds;
        private final List<String> phyLinkIds;
        private final List<CrossConnections> routeXcs;
        private final int fromLegCount;
        private final int toLegCount;

        private RemoveProtectionLegContext(String legName, List<String> siteLinkIds, List<String> phyLinkIds,
                                           List<CrossConnections> routeXcs,
                                           int fromLegCount, int toLegCount) {
            this.legName = legName;
            this.siteLinkIds = siteLinkIds == null ? Collections.emptyList() : siteLinkIds;
            this.phyLinkIds = phyLinkIds == null ? Collections.emptyList() : phyLinkIds;
            this.routeXcs = routeXcs == null ? Collections.emptyList() : routeXcs;
            this.fromLegCount = fromLegCount;
            this.toLegCount = toLegCount;
        }
    }

    /**
     * Builds the complete lock scope for one remove-leg operation. Implement owns the lock lifecycle so the
     * prepare, device deimplement and database cleanup phases cannot be interleaved with another operation.
     */
    public AbstractResourceLock newRemoveProtectionLegLocker(Tunnel tunnel) {
        Link ochLink = getOchLinkByTunnel(tunnel);
        AbstractResourceLock locker = resourceLockFactory.get();
        locker.addResource(REMOVE_LEG_OPERATION_LOCK_PREFIX + ochLink.getLinkId().getValue());
        if (isBatchThirdLegAlreadyRemoved(tunnel)) {
            // A completed batch retry needs only the OCH group lock; never reinterpret 3->2 as 2->1.
            locker.addResource(ochLink.getLinkId().getValue());
            tunnelDao.getAllTunnelsUnderOchLink(Collections.singletonList(
                    ochLink.getLinkId().getValue())).forEach(
                    sameOchTunnel -> locker.addResource(sameOchTunnel.getTunnelId().getValue()));
            return locker;
        }
        RemoveProtectionLegContext context = buildRemoveProtectionLegContext(
                ochLink, Collections.emptyList());
        addRemoveProtectionLegLocks(locker, ochLink, context);
        return locker;
    }

    /**
     * Persists the exact leg scope before implement removes device resources. The caller must hold the locker
     * returned by {@link #newRemoveProtectionLegLocker(Tunnel)}.
     *
     * @return {@code true} when implement must execute device deimplementation; {@code false} when the OCH is
     *         Allocate or a previous attempt already completed the device phase
     */
    public boolean prepareProtectionLegRemoval(Tunnel tunnel) {
        Link ochLink = getOchLinkByTunnel(tunnel);
        Och och = ochLink.getAugmentation(Link1.class).getOch();
        // Recheck under the operation lock so add-leg cannot start between validation and execution.
        validateNoBindingThirdLeg(och);
        String action = PropertyTool.getValue(och.getProperties(), PROTECTION_CHANGE_ACTION);
        if (ImplementState.Allocate.equals(och.getImplementState())
                && (action == null || action.trim().isEmpty())) {
            validateAllocateOchTunnels(ochLink);
            return false;
        }
        if ((action == null || action.trim().isEmpty())
                && !ImplementState.Implement.equals(och.getImplementState())) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "OCH cannot remove protection leg in state " + och.getImplementState());
        }
        if (isRemoveLegDeviceDeimplemented(ochLink)) {
            return false;
        }
        if (action == null || action.trim().isEmpty()) {
            RemoveProtectionLegContext context = buildRemoveProtectionLegContext(
                    ochLink, Collections.emptyList());
            persistRemoveProtectionLegMarker(tunnel, ochLink, context);
        }
        return true;
    }

    /**
     * Releases only the removed-leg database resources. For an implemented OCH, the caller reaches this method
     * only after device deimplementation succeeds; an Allocate OCH skips the device phase and trims DB directly.
     * The caller must hold the operation locker in both cases.
     */
    public TaskInfoMessage removeProtectionLegResources(Tunnel tunnel, TaskInfoMessage rootTaskInfo) {
        TaskInfoMessage taskInfo = new TaskInfoMessage(rootTaskInfo.getWho(), rootTaskInfo.getResourceType(),
                TaskInfoMessage.ActionType.unbind, "remove protection leg");
        taskInfo.setGroupId(rootTaskInfo.getGroupId());
        taskInfo.setRoot(false);
        taskInfo.setResourceName("remove protection leg");
        taskInfo.setResourceId("removeProtectionLeg" + System.currentTimeMillis());
        taskInfo.setActionTime(System.currentTimeMillis());

        try {
            Link ochLink = getOchLinkByTunnel(tunnel);
            RemoveProtectionLegContext context = resolveRemoveProtectionLegContext(
                    ochLink, Collections.emptyList());
            appendTaskContext(taskInfo, "context-resolved", ochLink, context);
            log.info("Start remove protection leg resource cleanup, tunnelId={}, ochLinkId={}, context={}",
                tunnel.getTunnelId().getValue(), ochLink.getLinkId().getValue(), contextSummary(context));
            Och och = ochLink.getAugmentation(Link1.class).getOch();
            String action = PropertyTool.getValue(och.getProperties(), PROTECTION_CHANGE_ACTION);
            if (PROTECTION_CHANGE_ACTION_REMOVE_LEG.equals(action)
                    && !isRemoveLegDeviceDeimplemented(ochLink)) {
                // Reaching this point means implement has confirmed that every device action succeeded.
                persistRemoveProtectionLegDeviceDeimplemented(tunnel, ochLink, context);
            }
            trimRemovedProtectionLegDb(tunnel, taskInfo);
            appendTaskContext(taskInfo, "db-trim-completed", ochLink, context);
            log.info("Remove protection leg DB trim succeeded, tunnelId={}, ochLinkId={}",
                tunnel.getTunnelId().getValue(), ochLink.getLinkId().getValue());
            taskInfo.setSuccessfully(true);
            return taskInfo;
        } catch (Exception e) {
            taskInfo.setSuccessfully(false);
            taskInfo.setErrorReason(e.getMessage());
            appendTaskDetail(taskInfo, "failed", e.getClass().getSimpleName() + ": " + e.getMessage());
            log.error("Failed to remove protection leg for tunnel {}", tunnel.getTunnelId().getValue(), e);
            // Return the failed child so the owner and every same-OCH tunnel expose the exact cleanup result.
            return taskInfo;
        } finally {
            taskInfo.setEndTime(System.currentTimeMillis());
        }
    }

    private void validateAllocateOchTunnels(Link ochLink) {
        for (Tunnel sameOchTunnel : tunnelDao.getAllTunnelsUnderOchLink(
                Collections.singletonList(ochLink.getLinkId().getValue()))) {
            if (!ImplementState.Allocate.equals(sameOchTunnel.getImplementState())) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "Allocate OCH contains non-Allocate tunnel: " + sameOchTunnel.getFriendlyName());
            }
        }
    }

    /**
     * Validates every batch OCH before the first owner changes persistent state.
     */
    public void validateRemoveProtectionLeg(Tunnel tunnel) {
        Link ochLink = getOchLinkByTunnel(tunnel);
        Och och = ochLink.getAugmentation(Link1.class).getOch();
        // Reject the conflicting operation before parsing or modifying the removable route.
        validateNoBindingThirdLeg(och);
        resolveRemoveProtectionLegContext(ochLink, Collections.emptyList());
        String action = PropertyTool.getValue(och.getProperties(), PROTECTION_CHANGE_ACTION);
        if (ImplementState.Allocate.equals(och.getImplementState())
                && (action == null || action.trim().isEmpty())) {
            validateAllocateOchTunnels(ochLink);
            return;
        }
        if ((action == null || action.trim().isEmpty())
                && !ImplementState.Implement.equals(och.getImplementState())) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "OCH cannot remove protection leg in state " + och.getImplementState());
        }
        // Validate unfinished marker action and phase before any OCH in the batch is modified.
        isRemoveLegDeviceDeimplemented(ochLink);
    }

    /** Adds the persisted remove-leg XC scope, including legacy REG self XCs, to the device route. */
    public void extendPreparedRemoveLegRouteInfo(String tunnelId, RouteInfo routeInfo) {
        Tunnel tunnel = tunnelDao.getTunnelById(tunnelId);
        if (tunnel == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Tunnel does not exist: " + tunnelId);
        }
        RemoveProtectionLegContext context = resolveRemoveProtectionLegContext(
                getOchLinkByTunnel(tunnel), Collections.emptyList());
        extendRouteWithCrossConnections(routeInfo, context.routeXcs);
    }

    static void extendRouteWithCrossConnections(RouteInfo routeInfo, List<CrossConnections> crossConnections) {
        if (crossConnections == null || crossConnections.isEmpty()) {
            return;
        }
        List<CrossConnections> missingCrossConnections = crossConnections.stream()
                .filter(xc -> !routeInfo.getXcIdList().contains(xc.getCrossConnectionId().getValue()))
                .collect(Collectors.toList());
        if (missingCrossConnections.isEmpty()) {
            return;
        }
        Route route = new RouteBuilder().setPrimary(new PrimaryBuilder()
                .setExplicitRouteObjects(Collections.emptyList())
                .setCrossConnections(missingCrossConnections).build()).build();
        routeInfo.extend(new RouteInfo().parse(Collections.singletonList(route)));
    }

    /** Batch remove-leg is the 3->2 operation; a two-leg OCH is a completed retry, not a new 2->1 request. */
    public boolean isBatchThirdLegAlreadyRemoved(Tunnel tunnel) {
        Link ochLink = getOchLinkByTunnel(tunnel);
        Och och = ochLink.getAugmentation(Link1.class).getOch();
        // This runs before normal batch validation, including for an idempotent two-leg retry.
        validateNoBindingThirdLeg(och);
        String action = PropertyTool.getValue(och.getProperties(), PROTECTION_CHANGE_ACTION);
        int fromLegCount = action == null ? 0
                : getProtectionChangeInt(och.getProperties(), PROTECTION_CHANGE_FROM_LEG_COUNT);
        int toLegCount = action == null ? 0
                : getProtectionChangeInt(och.getProperties(), PROTECTION_CHANGE_TO_LEG_COUNT);
        int currentLegCount = getProtectionLegCount(ochLink);
        if (action != null && (!PROTECTION_CHANGE_ACTION_REMOVE_LEG.equals(action)
                || fromLegCount != 3 || toLegCount != 2)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Batch remove-leg only supports removing the third leg");
        }
        if (action == null && currentLegCount != 2 && currentLegCount != 3) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Batch remove-leg requires a two-leg or three-leg OCH");
        }
        boolean completed = isBatchThirdLegAlreadyRemoved(action, currentLegCount);
        if (completed && !isStableBatchCompletedState(och.getImplementState())) {
            // A two-leg route is idempotently complete only after the previous operation reached a stable state.
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Batch remove-leg cannot accept two-leg OCH in state " + och.getImplementState());
        }
        return completed;
    }

    static void validateNoBindingThirdLeg(Och och) {
        if (PropertyTool.existProperty(och.getProperties(), BINDING_3_RD_LEG)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Cannot remove protection leg while binding third leg is unfinished");
        }
    }

    static boolean isBatchThirdLegAlreadyRemoved(String action, int currentLegCount) {
        return (action == null || action.trim().isEmpty()) && currentLegCount == 2;
    }

    static boolean isStableBatchCompletedState(ImplementState state) {
        return ImplementState.Implement.equals(state) || ImplementState.Allocate.equals(state);
    }

    /**
     * Removes the leg from config DB after the required device phase, if any. This is the inverse of add-leg DB
     * allocation: restore siteLink spectrum/support relations, remove route XCs, remove phy/internal links, trim the
     * OCH route, and clear operation markers from all tunnels under the OCH.
     */
    private void trimRemovedProtectionLegDb(Tunnel tunnel, TaskInfoMessage taskInfo) {
        Link ochLink = getOchLinkByTunnel(tunnel);
            RemoveProtectionLegContext context = resolveRemoveProtectionLegContext(
                    ochLink, Collections.emptyList());
            ChangedObject changedObject = new ChangedObject();
            // The caller holds the complete remove-leg resource lock. Keep this OCH on the same
            // full-rewrite transaction path as its tunnels; a cloned Mongo proxy cannot be used
            // as the baseline when the unkeyed protection route list changes from three legs to two.
            Link lockedOchLink = ochLinkDao.getOchLinkByLinkId(ochLink.getLinkId().getValue());
            context = resolveRemoveProtectionLegContext(lockedOchLink, Collections.emptyList());
            log.info("Trim remove-leg DB start, tunnelId={}, ochLinkId={}, context={}, marker={}, currentLegCount={}",
                tunnel.getTunnelId().getValue(),
                lockedOchLink.getLinkId().getValue(),
                contextSummary(context),
                markerSummary(lockedOchLink.getAugmentation(Link1.class).getOch().getProperties()),
                getProtectionLegCount(lockedOchLink));

            restoreAllocateOchTwoLegApsState(changedObject, lockedOchLink, context);

            // Restore siteLink spectrum/support relation after device deimplementation succeeds.
            for (String siteLinkId : context.siteLinkIds) {
                Link siteLink = changedObject.getChangedSiteLink(siteLinkId);
                changedObject.addChangedSiteLink(
                        ProtectionLegTopologyUpdater.removeOchFromSiteLink(siteLink, lockedOchLink));
            }

            List<Tunnel> sameOchTunnels = tunnelDao.getAllTunnelsUnderOchLink(
                    Arrays.asList(lockedOchLink.getLinkId().getValue()));
            List<CrossConnections> removedRouteXcs = new ArrayList<>(context.routeXcs);
            for (Tunnel sameOchTunnel : sameOchTunnels) {
                removedRouteXcs.addAll(getRemovedTunnelLegXcs(sameOchTunnel, context));
            }
            List<CrossConnections> exclusiveRemovedRouteXcs = filterExclusiveRemovedRouteXcs(
                    lockedOchLink, context, removedRouteXcs);
            log.info("Remove-leg REG/XC reference check, ochLinkId={}, requestedXcs={}, exclusiveXcs={}",
                    lockedOchLink.getLinkId().getValue(), getXcIds(removedRouteXcs),
                    getXcIds(exclusiveRemovedRouteXcs));
            removeRouteXcs(changedObject, exclusiveRemovedRouteXcs);

            Set<String> retainedPhyLinkIds = collectRetainedPhyLinkIds(lockedOchLink, context);
            List<String> exclusivePhyLinkIds = context.phyLinkIds.stream()
                    .filter(id -> !retainedPhyLinkIds.contains(id)).collect(Collectors.toList());
            Set<String> impactedRegEquipmentIds = collectImpactedRegEquipmentIds(changedObject,
                    exclusivePhyLinkIds);
            Set<String> impactedRegTpIds = collectImpactedRegTpIds(changedObject, exclusivePhyLinkIds);
            // 避免删 OmsLink、清 MPO TP/internalLink
            Set<String> siteExternalOmsLinkIds = getSiteExternalOmsLinkIds(changedObject, context.siteLinkIds);
            List<String> deletedPhyLinkIds = new ArrayList<>();
            for (String phyLinkId : exclusivePhyLinkIds) {
                if (siteExternalOmsLinkIds.contains(phyLinkId)) {
                    log.info("Skip remove-leg external OmsLink DB delete, tunnelId={}, ochLinkId={}, phyLinkId={}",
                        tunnel.getTunnelId().getValue(),
                        lockedOchLink.getLinkId().getValue(),
                        phyLinkId);
                    continue;
                }
                ProtectionLegTopologyUpdater.removePhyLinkKeepXc(changedObject, phyLinkId);
                deletedPhyLinkIds.add(phyLinkId);
            }
            RegCleanupResult regCleanup = cleanupUnusedRegEquipment(changedObject, impactedRegEquipmentIds,
                    impactedRegTpIds, lockedOchLink, context);
            Set<String> candidateXcIds = getXcIds(removedRouteXcs);
            Set<String> deletedXcIds = getXcIds(exclusiveRemovedRouteXcs);
            Set<String> retainedXcIds = new HashSet<>(candidateXcIds);
            retainedXcIds.removeAll(deletedXcIds);
            Set<String> candidateRegOsLinkIds = context.phyLinkIds.stream()
                    .filter(PhysicalLinkIdNamingRule::isOsLink).collect(Collectors.toSet());
            Set<String> retainedRegOsLinkIds = candidateRegOsLinkIds.stream()
                    .filter(id -> retainedPhyLinkIds.contains(id) || siteExternalOmsLinkIds.contains(id))
                    .collect(Collectors.toSet());
            Set<String> deletedRegOsLinkIds = deletedPhyLinkIds.stream()
                    .filter(PhysicalLinkIdNamingRule::isOsLink).collect(Collectors.toSet());
            Set<String> retainedRegNodeIds = new HashSet<>(regCleanup.candidateNodeIds);
            retainedRegNodeIds.removeAll(regCleanup.deletedNodeIds);
            appendTaskDetail(taskInfo, "reg-cleanup", String.format(
                    "nodes[candidate=%s,retained=%s,deleted=%s], "
                            + "lPorts[candidate=%s,retained=%s,released=%s], "
                            + "xcs[candidate=%s,retained=%s,deleted=%s], "
                            + "osLinks[candidate=%s,retained=%s,deleted=%s], "
                            + "equipment[candidate=%s,retained=%s,released=%s]",
                    regCleanup.candidateNodeIds, retainedRegNodeIds, regCleanup.deletedNodeIds,
                    regCleanup.candidateTpIds, regCleanup.retainedTpIds, regCleanup.releasedTpIds,
                    candidateXcIds, retainedXcIds, deletedXcIds,
                    candidateRegOsLinkIds, retainedRegOsLinkIds, deletedRegOsLinkIds,
                    impactedRegEquipmentIds, regCleanup.retainedEquipmentIds, regCleanup.releasedEquipmentIds));

            Link updatedOchLink = removeLegFromOchLink(lockedOchLink, context);
            changedObject.addChangedOchLink(updatedOchLink);
            log.info("Prepared remove-leg OCH trim, tunnelId={}, ochLinkId={}, beforeLegCount={}, afterLegCount={}, removedSupportingLinks={}",
                tunnel.getTunnelId().getValue(),
                updatedOchLink.getLinkId().getValue(),
                context.fromLegCount,
                getProtectionLegCount(updatedOchLink),
                removedResourceIds(context));

            log.info("Cleanup remove-leg markers on tunnels, ochLinkId={}, sameOchTunnels={}",
                updatedOchLink.getLinkId().getValue(),
                sameOchTunnels.stream().map(t -> t.getTunnelId().getValue()).collect(Collectors.toList()));
            for (Tunnel sameOchTunnel : sameOchTunnels) {
                changedObject.addChangedTunnel(removeLegFromTunnel(sameOchTunnel, context));
            }

            log.info("Saving remove-leg DB trim, tunnelId={}, changedObject={}",
                tunnel.getTunnelId().getValue(), changedObjectSummary(changedObject));
            multipleTransaction.save(changedObject);
        log.info("Saved remove-leg DB trim, tunnelId={}, ochLinkId={}, finalLegCount={}",
                tunnel.getTunnelId().getValue(),
                updatedOchLink.getLinkId().getValue(),
                getProtectionLegCount(updatedOchLink));
    }

    /** OCH=Allocate has no device phase, but its persisted APS model must still return to two-leg LOP. */
    private void restoreAllocateOchTwoLegApsState(ChangedObject changedObject, Link ochLink,
                                                   RemoveProtectionLegContext context) {
        Och och = ochLink.getAugmentation(Link1.class).getOch();
        if (!ImplementState.Allocate.equals(och.getImplementState()) || context.toLegCount != 2) {
            return;
        }
        java.util.Map<String, String> retainedApsC = ProtectionLegDeimplementor
                .buildRetainedApsCTransitions(2,
                        RouteExtractor.extractorXc(och.getExplictRoute().getRoute()));
        retainedApsC.keySet().forEach(apsXcId -> {
            String nodeId = PhysicalXcIdNamingRule.getNodeId(apsXcId);
            Node node = changedObject.getChangedPhyNode(nodeId);
            changedObject.addChangedPhyNode(
                    ApsProtectionState.restoreTwoLegMemberProperties(node, apsXcId));
        });
    }

    private RemoveProtectionLegContext resolveRemoveProtectionLegContext(Link ochLink, List<String> selectedSiteLinkIds) {
        Och ochAttr = ochLink.getAugmentation(Link1.class).getOch();
        String existingAction = PropertyTool.getValue(ochAttr.getProperties(), PROTECTION_CHANGE_ACTION);
        if (existingAction == null || existingAction.trim().isEmpty()) {
            return buildRemoveProtectionLegContext(ochLink, selectedSiteLinkIds);
        }
        if (!PROTECTION_CHANGE_ACTION_REMOVE_LEG.equals(existingAction)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                "Cannot remove protection leg while another protection change is unfinished: " + existingAction);
        }

        List<String> markerSiteLinkIds = getProtectionChangeSiteLinkIds(ochAttr.getProperties());
        if (!selectedSiteLinkIds.isEmpty() && !new HashSet<>(selectedSiteLinkIds).equals(new HashSet<>(markerSiteLinkIds))) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                "Selected remove siteLink list does not match unfinished remove-leg marker");
        }

        RemoveProtectionLegContext routeContext = buildRemoveProtectionLegContext(ochLink, markerSiteLinkIds);
        int fromLegCount = getProtectionChangeInt(ochAttr.getProperties(), PROTECTION_CHANGE_FROM_LEG_COUNT);
        int toLegCount = getProtectionChangeInt(ochAttr.getProperties(), PROTECTION_CHANGE_TO_LEG_COUNT);
        log.info("Reuse unfinished remove-leg marker, ochLinkId={}, marker={}, routeContext={}",
            ochLink.getLinkId().getValue(), markerSummary(ochAttr.getProperties()), contextSummary(routeContext));
        return new RemoveProtectionLegContext(routeContext.legName, routeContext.siteLinkIds, routeContext.phyLinkIds,
            routeContext.routeXcs, fromLegCount, toLegCount);
    }

    /**
     * Removes only the route XCs owned by the deleted protection leg from config phy nodes.
     */
    private void removeRouteXcs(ChangedObject changedObject, List<CrossConnections> routeXcs) {
        for (CrossConnections xc : routeXcs) {
            String nodeId = xc.getNodeRef().getValue();
            Node node = changedObject.getChangedPhyNode(nodeId);
            if (node != null) {
                changedObject.addChangedPhyNode(ProtectionLegTopologyUpdater.removeXc(node, xc));
            }
        }
    }

    private static class RegCleanupResult {
        private final Set<String> candidateNodeIds = new HashSet<>();
        private final Set<String> candidateTpIds = new HashSet<>();
        private final Set<String> retainedTpIds = new HashSet<>();
        private final Set<String> releasedTpIds = new HashSet<>();
        private final Set<String> retainedEquipmentIds = new HashSet<>();
        private final Set<String> releasedEquipmentIds = new HashSet<>();
        private final Set<String> deletedNodeIds = new HashSet<>();
    }

    /** A route resource is removable only when every retained OCH/Tunnel branch has stopped referencing it. */
    private List<CrossConnections> filterExclusiveRemovedRouteXcs(Link currentOchLink,
            RemoveProtectionLegContext context, List<CrossConnections> removedRouteXcs) {
        Set<String> retainedIds = new HashSet<>();
        collectRetainedRouteResources(currentOchLink, context, retainedIds, new HashSet<>());
        return removedRouteXcs.stream()
                .filter(xc -> xc.getCrossConnectionId() != null)
                .filter(xc -> !retainedIds.contains(xc.getCrossConnectionId().getValue()))
                .collect(Collectors.toList());
    }

    private Set<String> getXcIds(List<CrossConnections> xcs) {
        return xcs.stream().filter(xc -> xc.getCrossConnectionId() != null)
                .map(xc -> xc.getCrossConnectionId().getValue()).collect(Collectors.toSet());
    }

    private Set<String> collectRetainedPhyLinkIds(Link currentOchLink, RemoveProtectionLegContext context) {
        Set<String> retained = new HashSet<>();
        collectRetainedRouteResources(currentOchLink, context, new HashSet<>(), retained);
        return retained;
    }

    private void collectRetainedRouteResources(Link currentOchLink, RemoveProtectionLegContext context,
            Set<String> retainedXcs, Set<String> retainedLinks) {
        List<Link> related = ochLinkDao.getAllOchLinksUnderSiteLinkIds(context.siteLinkIds);
        if (related == null) {
            related = Collections.emptyList();
        }
        // Some DAO implementations do not return the current OCH after its marker changes.
        if (related.stream().noneMatch(link -> link.getLinkId().equals(currentOchLink.getLinkId()))) {
            related = new ArrayList<>(related);
            related.add(currentOchLink);
        }
        for (Link candidate : related) {
            boolean current = currentOchLink.getLinkId().equals(candidate.getLinkId());
            Och och = candidate.getAugmentation(Link1.class).getOch();
            if (och != null && och.getExplictRoute() != null && och.getExplictRoute().getRoute() != null) {
                for (Route route : och.getExplictRoute().getRoute()) {
                    collectRouteResources(current ? trimLegFromRoute(route, context) : route,
                            retainedXcs, retainedLinks);
                }
            }
            List<Tunnel> tunnels = tunnelDao.getAllTunnelsUnderOchLink(
                    Collections.singletonList(candidate.getLinkId().getValue()));
            for (Tunnel candidateTunnel : tunnels) {
                if (candidateTunnel.getExplictRoute() == null
                        || candidateTunnel.getExplictRoute().getRoute() == null) {
                    continue;
                }
                for (Route route : candidateTunnel.getExplictRoute().getRoute()) {
                    collectRouteResources(current ? trimLegFromRoute(route, context) : route,
                            retainedXcs, retainedLinks);
                }
            }
        }
    }

    private void collectRouteResources(Route route, Set<String> xcIds, Set<String> linkIds) {
        if (route.getPrimary() != null) {
            collectBranchResources(route.getPrimary().getCrossConnections(),
                    route.getPrimary().getExplicitRouteObjects(), xcIds, linkIds);
        }
        if (route.getSecondary() != null) {
            collectBranchResources(route.getSecondary().getCrossConnections(),
                    route.getSecondary().getExplicitRouteObjects(), xcIds, linkIds);
        }
        if (route.getThird() != null) {
            route.getThird().forEach(third -> collectBranchResources(third.getCrossConnections(),
                    third.getExplicitRouteObjects(), xcIds, linkIds));
        }
    }

    private void collectBranchResources(List<CrossConnections> xcs, List<ExplicitRouteObjects> eros,
            Set<String> xcIds, Set<String> linkIds) {
        if (xcs != null) {
            xcs.stream().filter(xc -> xc.getCrossConnectionId() != null)
                    .map(xc -> xc.getCrossConnectionId().getValue()).forEach(xcIds::add);
        }
        if (eros != null) {
            linkIds.addAll(getRoutePhyLinkIds(eros));
        }
    }

    private Set<String> collectImpactedRegEquipmentIds(ChangedObject changedObject, List<String> phyLinkIds) {
        Set<String> result = new HashSet<>();
        for (String linkId : phyLinkIds) {
            Link link = changedObject.getChangedPhyLink(linkId);
            if (link == null) {
                continue;
            }
            addRegEquipmentId(changedObject, link.getSource().getSourceTp().getValue(), result);
            addRegEquipmentId(changedObject, link.getDestination().getDestTp().getValue(), result);
        }
        return result;
    }

    private void addRegEquipmentId(ChangedObject changedObject, String tpId, Set<String> result) {
        Node node = changedObject.getChangedPhyNode(PhysicalTpIdNamingRule.getNodeId(tpId));
        if (isRegElectricNode(node)) {
            result.add(PhysicalTpIdNamingRule.getEquipId(tpId));
        }
    }

    private Set<String> collectImpactedRegTpIds(ChangedObject changedObject, List<String> phyLinkIds) {
        Set<String> result = new HashSet<>();
        for (String linkId : phyLinkIds) {
            Link link = changedObject.getChangedPhyLink(linkId);
            if (link == null) {
                continue;
            }
            addRegTpId(changedObject, link.getSource().getSourceTp().getValue(), result);
            addRegTpId(changedObject, link.getDestination().getDestTp().getValue(), result);
        }
        return result;
    }

    private void addRegTpId(ChangedObject changedObject, String tpId, Set<String> result) {
        Node node = changedObject.getChangedPhyNode(PhysicalTpIdNamingRule.getNodeId(tpId));
        if (isRegElectricNode(node)) {
            result.add(tpId);
        }
    }

    private boolean isRegElectricNode(Node node) {
        return node != null && node.getAugmentation(Node1.class) != null
                && node.getAugmentation(Node1.class).getPhysical() != null
                && NeSubType.EPC_REG.getSubTypeName().equals(
                node.getAugmentation(Node1.class).getPhysical().getCustomedType());
    }

    private RegCleanupResult cleanupUnusedRegEquipment(ChangedObject changedObject, Set<String> equipmentIds,
            Set<String> tpIds, Link currentOchLink, RemoveProtectionLegContext context) {
        RegCleanupResult result = new RegCleanupResult();
        result.candidateTpIds.addAll(tpIds);
        Set<String> nodeIds = equipmentIds.stream().map(PhysicalEqpIdNamingRule::getNodeId)
                .collect(Collectors.toSet());
        result.candidateNodeIds.addAll(nodeIds);
        for (String equipmentId : equipmentIds) {
            String nodeId = PhysicalEqpIdNamingRule.getNodeId(equipmentId);
            Node node = changedObject.getChangedPhyNode(nodeId);
            if (!isRegElectricNode(node) || isEquipmentReferenced(node, equipmentId)) {
                result.retainedEquipmentIds.add(equipmentId);
                log.info("Keep shared remove-leg REG equipment, nodeId={}, equipmentId={}", nodeId, equipmentId);
                continue;
            }
            changedObject.addChangedPhyNode(
                    ProtectionLegTopologyUpdater.markRegEquipmentEmpty(node, equipmentId));
            result.releasedEquipmentIds.add(equipmentId);
            log.info("Released remove-leg REG L ports/card, nodeId={}, equipmentId={}", nodeId, equipmentId);
        }
        for (String tpId : tpIds) {
            Node node = changedObject.getChangedPhyNode(PhysicalTpIdNamingRule.getNodeId(tpId));
            if (node != null && isTpReferenced(node, tpId)) {
                result.retainedTpIds.add(tpId);
            } else {
                result.releasedTpIds.add(tpId);
            }
        }
        for (String nodeId : nodeIds) {
            Node node = changedObject.getChangedPhyNode(nodeId);
            if (node == null || !ProtectionLegTopologyUpdater.isEmptyNode(node)
                    || hasRetainedOchReferenceToNode(nodeId, currentOchLink, context)) {
                continue;
            }
            if (ProtectionLegTopologyUpdater.hasIp(node)) {
                // REG management is an implement concern and must finish before topology deletion.
                unregisterAndRemoveIp(nodeId);
                changedObject.unsetPhyNode(nodeId);
            }
            changedObject.addRemovedPhyNode(nodeId);
            String siteId = PhysicalNodeIdNamingRule.getSiteId(nodeId);
            Node siteNode = changedObject.getChangedSiteNode(siteId);
            if (siteNode != null) {
                changedObject.addChangedSiteNode(
                        ProtectionLegTopologyUpdater.removeNodeFromSite(siteNode, nodeId));
            }
            result.deletedNodeIds.add(nodeId);
            log.info("Removed empty remove-leg REG node and rack relation, nodeId={}", nodeId);
        }
        return result;
    }

    private void unregisterAndRemoveIp(String nodeId) {
        UnregisteNeOutput output = neManagerRpc.unregisteredNe(NodeId.getDefaultInstance(nodeId));
        if (output == null || !RpcResultType.Success.equals(output.getReturnCode())) {
            throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                    "Failed to stop supervision for remove-leg REG " + nodeId);
        }
        phyNodeDao.updateConfigPhyNodeSupervisionState(nodeId, SupervisionStatusType.Unmonitored);
        phyNodeDao.removeNeIp(nodeId);
        log.info("Stopped supervision and removed IP for unused remove-leg REG, nodeId={}", nodeId);
    }

    private boolean isTpReferenced(Node node, String tpId) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical physical =
                node.getAugmentation(Node1.class).getPhysical();
        boolean xcUsed = physical.getCrossConnections() != null && physical.getCrossConnections().stream()
                .anyMatch(xc -> (xc.getSourceTp() != null && xc.getSourceTp().stream()
                        .anyMatch(tp -> tp.getTpRef() != null && tpId.equals(tp.getTpRef().getValue())))
                        || (xc.getDestinationTp() != null && xc.getDestinationTp().stream()
                        .anyMatch(tp -> tp.getTpRef() != null && tpId.equals(tp.getTpRef().getValue()))));
        boolean linkUsed = physical.getInternalLinks() != null && physical.getInternalLinks().stream()
                .anyMatch(link -> link.getLinkRef() != null && link.getLinkRef().contains(tpId));
        return xcUsed || linkUsed;
    }

    private boolean isEquipmentReferenced(Node node, String equipmentId) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical physical =
                node.getAugmentation(Node1.class).getPhysical();
        boolean xcUsed = physical.getCrossConnections() != null && physical.getCrossConnections().stream()
                .anyMatch(xc -> xc.getCrossConnectionId() != null
                        && xc.getCrossConnectionId().getValue().contains(equipmentId));
        boolean linkUsed = physical.getInternalLinks() != null && physical.getInternalLinks().stream()
                .anyMatch(link -> link.getLinkRef() != null && link.getLinkRef().contains(equipmentId));
        return xcUsed || linkUsed;
    }

    private boolean hasRetainedOchReferenceToNode(String nodeId, Link currentOchLink,
            RemoveProtectionLegContext context) {
        List<Link> links = ochLinkDao.queryWithNode(nodeId);
        if (links == null) {
            return false;
        }
        for (Link link : links) {
            if (!link.getLinkId().equals(currentOchLink.getLinkId())) {
                return true;
            }
            Set<String> xcs = new HashSet<>();
            Set<String> phyLinks = new HashSet<>();
            Och och = link.getAugmentation(Link1.class).getOch();
            for (Route route : och.getExplictRoute().getRoute()) {
                collectRouteResources(trimLegFromRoute(route, context), xcs, phyLinks);
            }
            if (xcs.stream().anyMatch(id -> id.contains(nodeId))
                    || phyLinks.stream().anyMatch(id -> id.contains(nodeId))) {
                return true;
            }
        }
        return false;
    }

    private List<CrossConnections> getRemovedTunnelLegXcs(Tunnel tunnel, RemoveProtectionLegContext context) {
        if (tunnel.getExplictRoute() == null || tunnel.getExplictRoute().getRoute() == null
                || tunnel.getExplictRoute().getRoute().isEmpty()) {
            return Collections.emptyList();
        }
        Route route = tunnel.getExplictRoute().getRoute().get(0);
        if ("third".equals(context.legName) && route.getThird() != null && !route.getThird().isEmpty()) {
            return route.getThird().get(0).getCrossConnections() == null
                    ? Collections.emptyList() : route.getThird().get(0).getCrossConnections();
        }
        if ("secondary".equals(context.legName) && route.getSecondary() != null) {
            return route.getSecondary().getCrossConnections() == null
                    ? Collections.emptyList() : route.getSecondary().getCrossConnections();
        }
        return Collections.emptyList();
    }

    /**
     * Uses one impact scope for marker persistence, device work and DB trim.  In particular,
     * all affected NE/TP/XC resources are locked before any phase can change their route data.
     */
    private void addRemoveProtectionLegLocks(AbstractResourceLock locker, Link ochLink,
                                              RemoveProtectionLegContext context) {
        List<Tunnel> sameOchTunnels = tunnelDao.getAllTunnelsUnderOchLink(
                Collections.singletonList(ochLink.getLinkId().getValue()));
        sameOchTunnels.forEach(tunnel -> locker.addResource(tunnel.getTunnelId().getValue()));
        locker.addResource(ochLink.getLinkId().getValue());
        context.siteLinkIds.forEach(locker::addResource);
        for (String phyLinkId : context.phyLinkIds) {
            locker.addResource(phyLinkId);
            Link phyLink = phyLinkDao.getPhyLinkById(phyLinkId);
            if (phyLink != null) {
                addRemoveLegTpLocks(locker, phyLink.getSource().getSourceTp().getValue());
                addRemoveLegTpLocks(locker, phyLink.getDestination().getDestTp().getValue());
            }
        }

        // REG XC reuse is limited to OCHs traversing the same SiteLinks. Lock every such OCH and
        // its Tunnels before the later reference check, so an allocation cannot reuse a REG port
        // between "exclusive" calculation and physical resource release.
        List<Link> regReuseCandidateOchs = ochLinkDao.getAllOchLinksUnderSiteLinkIds(context.siteLinkIds);
        if (regReuseCandidateOchs != null) {
            for (Link candidateOch : regReuseCandidateOchs) {
                locker.addResource(candidateOch.getLinkId().getValue());
                tunnelDao.getAllTunnelsUnderOchLink(Collections.singletonList(candidateOch.getLinkId().getValue()))
                        .forEach(candidateTunnel -> locker.addResource(candidateTunnel.getTunnelId().getValue()));
            }
        }

        List<CrossConnections> affectedXcs = new ArrayList<>(context.routeXcs);
        for (Tunnel sameOchTunnel : sameOchTunnels) {
            affectedXcs.addAll(getRemovedTunnelLegXcs(sameOchTunnel, context));
        }
        for (CrossConnections xc : affectedXcs) {
            if (xc.getCrossConnectionId() != null) {
                locker.addResource(xc.getCrossConnectionId().getValue());
            }
            if (xc.getNodeRef() != null) {
                locker.addResource(xc.getNodeRef().getValue());
            }
            if (xc.getSourceTp() != null) {
                xc.getSourceTp().stream().filter(tp -> tp.getTpRef() != null)
                        .forEach(tp -> addRemoveLegTpLocks(locker, tp.getTpRef().getValue()));
            }
            if (xc.getDestinationTp() != null) {
                xc.getDestinationTp().stream().filter(tp -> tp.getTpRef() != null)
                        .forEach(tp -> addRemoveLegTpLocks(locker, tp.getTpRef().getValue()));
            }
        }
    }

    private void addRemoveLegTpLocks(AbstractResourceLock locker, String tpId) {
        locker.addResource(tpId);
        String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
        locker.addResource(nodeId);
        locker.addResource(PhysicalTpIdNamingRule.getEquipId(tpId));
        locker.addResource(PhysicalNodeIdNamingRule.getSiteId(nodeId));
    }


    /**
     * Trims the deleted leg from the OCH route/supportingLink after device deimplementation succeeds.
     */
    private Link removeLegFromOchLink(Link ochLink, RemoveProtectionLegContext context) {
        Och ochAttr = ochLink.getAugmentation(Link1.class).getOch();
        Route route = ochAttr.getExplictRoute().getRoute().get(0);

        Set<String> removedSupportingLinkIds = new HashSet<>();
        removedSupportingLinkIds.addAll(context.siteLinkIds);
        removedSupportingLinkIds.addAll(context.phyLinkIds);
        List<SupportingLink> supportingLinks = ochLink.getSupportingLink().stream()
            .filter(supportingLink -> !removedSupportingLinkIds.contains(supportingLink.getLinkRef().getValue()))
            .collect(Collectors.toList());

        Properties newProp = cleanupRemoveLegProperties(ochAttr.getProperties(), context.toLegCount);

        ImplementState finalState = ImplementState.Allocate.equals(ochAttr.getImplementState())
                ? ImplementState.Allocate : ImplementState.Implement;
        Och updatedOch = new OchBuilder(ochAttr)
            .setExplictRoute(new ExplictRouteBuilder().setRoute(new ArrayList<>(Collections.singletonList(trimLegFromRoute(route, context)))).build())
            .setImplementState(finalState)
            .setProperties(newProp)
            .build();
        return new LinkBuilder(ochLink)
            .addAugmentation(Link1.class, new Link1Builder().setOch(updatedOch).build())
            .setSupportingLink(supportingLinks)
            .build();
    }

    /**
     * Cleans operation markers on the tunnel. Tunnel.explicitRoute is the service/OCH path view,
     * while OCH.explicitRoute owns the protection-leg details.
     */
    private Tunnel removeLegFromTunnel(Tunnel tunnel, RemoveProtectionLegContext context) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRoute route =
                tunnel.getExplictRoute();
        if (route != null && route.getRoute() != null) {
            List<Route> trimmedRoutes = route.getRoute().stream()
                    .map(value -> trimLegFromRoute(value, context)).collect(Collectors.toList());
            route = new ExplictRouteBuilder(route).setRoute(trimmedRoutes).build();
        }
        return restoreTunnelAfterRemoveLeg(tunnel, route, context.toLegCount);
    }

    /** Only tunnels that were Implement participate in the remove-leg restore state machine. */
    static Tunnel markTunnelForRemoveLeg(Tunnel tunnel, Properties operationProperties) {
        if (ImplementState.Allocate.equals(tunnel.getImplementState())) {
            return tunnel;
        }
        boolean restore = PropertyTool.existProperty(tunnel.getProperties(), REMOVE_LEG_RESTORE_TUNNEL);
        if (!ImplementState.Implement.equals(tunnel.getImplementState())
                && !(ImplementState.Doimplementing.equals(tunnel.getImplementState()) && restore)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Tunnel cannot remove protection leg in state " + tunnel.getImplementState());
        }
        Properties properties = operationProperties == null ? tunnel.getProperties() : operationProperties;
        properties = PropertyTool.addProperty(properties, REMOVE_LEG_RESTORE_TUNNEL, "true");
        return new TunnelBuilder(tunnel)
                .setImplementState(ImplementState.Doimplementing)
                .setAdminState(AdminStatus.Up)
                .setProperties(properties)
                .build();
    }

    /** Restore only tunnels marked from Implement; Allocate tunnels keep their original state. */
    static Tunnel restoreTunnelAfterRemoveLeg(Tunnel tunnel,
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRoute route,
            int toLegCount) {
        boolean restore = PropertyTool.existProperty(tunnel.getProperties(), REMOVE_LEG_RESTORE_TUNNEL);
        Properties properties = cleanupRemoveLegProperties(tunnel.getProperties(), toLegCount);
        TunnelBuilder builder = new TunnelBuilder(tunnel)
                .setExplictRoute(route)
                .setProperties(properties);
        if (restore) {
            builder.setImplementState(ImplementState.Implement).setAdminState(AdminStatus.Up);
        }
        return builder.build();
    }

    private Route trimLegFromRoute(Route route, RemoveProtectionLegContext context) {
        RouteBuilder routeBuilder = new RouteBuilder(route);
        if ("third".equals(context.legName)) {
            routeBuilder.setThird(null);
        } else {
            routeBuilder.setSecondary(null);
        }
        if (context.toLegCount == 2 && route.getPrimary() != null
                && route.getPrimary().getCrossConnections() != null) {
            // OCH and Tunnel embed copies of the APS XC; keep them aligned with the physical-node LOP state.
            routeBuilder.setPrimary(new PrimaryBuilder(route.getPrimary())
                    .setCrossConnections(route.getPrimary().getCrossConnections().stream()
                            .map(ProtectionLegResourceManager::restoreTwoLegApsMemberProperties)
                            .collect(Collectors.toList()))
                    .build());
        }
        return routeBuilder.build();
    }

    static CrossConnections restoreTwoLegApsMemberProperties(CrossConnections xc) {
        if (xc.getAps() == null) {
            return xc;
        }
        return new CrossConnectionsBuilder(xc)
                .setAps(new ApsBuilder(xc.getAps())
                        .setProperties(ApsProtectionState.restoreTwoLegMemberProperties(xc))
                        .build())
                .build();
    }

    private static Properties cleanupRemoveLegProperties(Properties properties, int toLegCount) {
        Properties result = cleanupProtectionChangeProperties(properties);
        if (toLegCount < 3) {
            return PropertyTool.addProperty(result, "leg-required", "true");
        }
        return PropertyTool.delProperty(result, "leg-required");
    }

    /**
     * Clears only transient add/remove marker fields after the device and DB work have both completed.
     */
    private static Properties cleanupProtectionChangeProperties(Properties properties) {
        Properties result = PropertyTool.delProperty(properties, PROTECTION_CHANGE_ACTION);
        result = PropertyTool.delProperty(result, PROTECTION_CHANGE_SITE_LINK_IDS);
        result = PropertyTool.delProperty(result, PROTECTION_CHANGE_FROM_LEG_COUNT);
        result = PropertyTool.delProperty(result, PROTECTION_CHANGE_TO_LEG_COUNT);
        result = PropertyTool.delProperty(result, PROTECTION_CHANGE_PHASE);
        result = PropertyTool.delProperty(result, "binding3rdLeg");
        result = PropertyTool.delProperty(result, "binding3rdLegRestoreTunnel");
        result = PropertyTool.delProperty(result, REMOVE_LEG_RESTORE_TUNNEL);
        return result;
    }

    private List<String> removedResourceIds(RemoveProtectionLegContext context) {
        List<String> removed = new ArrayList<>();
        removed.addAll(context.siteLinkIds);
        removed.addAll(context.phyLinkIds);
        return removed;
    }

    /**
     * Builds the removable-leg context from the current OCH route. Remove order is fixed: third first, then secondary.
     * If UI provides a siteLink list, it must match the removable leg as a set.
     */
    private RemoveProtectionLegContext buildRemoveProtectionLegContext(Link ochLink, List<String> selectedSiteLinkIds) {
        Och ochAttr = ochLink.getAugmentation(Link1.class).getOch();
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route route =
            ochAttr.getExplictRoute().getRoute().get(0);
        int fromLegCount = getProtectionLegCount(ochLink);
        if (fromLegCount <= 1) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "Cannot remove protection leg when only primary leg exists");
        }

        RemoveProtectionLegContext context;
        if (route.getThird() != null && !route.getThird().isEmpty()) {
            Third removedThird = route.getThird().get(0);
            context = new RemoveProtectionLegContext("third",
                getRouteSiteLinkIds(removedThird.getExplicitRouteObjects()),
                getRoutePhyLinkIds(removedThird.getExplicitRouteObjects()),
                removedThird.getCrossConnections(),
                fromLegCount,
                2);
        } else if (route.getSecondary() != null) {
            Secondary removedSecondary = route.getSecondary();
            context = new RemoveProtectionLegContext("secondary",
                getRouteSiteLinkIds(removedSecondary.getExplicitRouteObjects()),
                getRoutePhyLinkIds(removedSecondary.getExplicitRouteObjects()),
                removedSecondary.getCrossConnections(),
                fromLegCount,
                1);
        } else {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "No removable protection leg found");
        }

        context = includeRegSelfXcs(context);

        if (!selectedSiteLinkIds.isEmpty() && !new HashSet<>(selectedSiteLinkIds).equals(new HashSet<>(context.siteLinkIds))) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                "Selected remove siteLink list does not match removable " + context.legName + " leg");
        }
        return context;
    }

    /**
     * REG self XCs are persisted on the TPC4 node but are not always emitted into the legacy OCH ERO XC list.
     * Recover them from this leg's four directional OS-link endpoints so add/remove use the same resource set.
     */
    private RemoveProtectionLegContext includeRegSelfXcs(RemoveProtectionLegContext context) {
        Set<String> legTpIds = new HashSet<>();
        Set<String> endpointNodeIds = new HashSet<>();
        for (String linkId : context.phyLinkIds) {
            Link phyLink = phyLinkDao.getPhyLinkById(linkId);
            if (phyLink == null || phyLink.getSource() == null || phyLink.getDestination() == null) {
                continue;
            }
            legTpIds.add(phyLink.getSource().getSourceTp().getValue());
            legTpIds.add(phyLink.getDestination().getDestTp().getValue());
            endpointNodeIds.add(phyLink.getSource().getSourceNode().getValue());
            endpointNodeIds.add(phyLink.getDestination().getDestNode().getValue());
        }

        List<CrossConnections> allXcs = new ArrayList<>(context.routeXcs);
        Set<String> xcIds = allXcs.stream().map(xc -> xc.getCrossConnectionId().getValue())
                .collect(Collectors.toSet());
        for (String nodeId : endpointNodeIds) {
            Node node = phyNodeDao.getConfigPhyNodeById(nodeId);
            Node1 nodeAttr = node == null ? null : node.getAugmentation(Node1.class);
            if (nodeAttr == null || nodeAttr.getPhysical() == null
                    || !NeSubType.EPC_REG.getSubTypeName().equals(nodeAttr.getPhysical().getCustomedType())
                    || nodeAttr.getPhysical().getCrossConnections() == null) {
                continue;
            }
            nodeAttr.getPhysical().getCrossConnections().stream()
                    .filter(xc -> xc.getSourceTp() != null && xc.getDestinationTp() != null)
                    .filter(xc -> xc.getSourceTp().stream()
                            .anyMatch(tp -> legTpIds.contains(tp.getTpRef().getValue()))
                            || xc.getDestinationTp().stream()
                            .anyMatch(tp -> legTpIds.contains(tp.getTpRef().getValue())))
                    .filter(xc -> xcIds.add(xc.getCrossConnectionId().getValue()))
                    .map(xc -> new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515
                            .cross.connection.route.sequence.CrossConnectionsBuilder(xc).build())
                    .forEach(allXcs::add);
        }
        return new RemoveProtectionLegContext(context.legName, context.siteLinkIds, context.phyLinkIds,
                allXcs, context.fromLegCount, context.toLegCount);
    }

    /**
     * Stores the remove operation context before any device call. If device deimplementation fails, the marker stays
     * in DB so retry can identify the exact removed leg instead of guessing from PartialImplement.
     */
    private void persistRemoveProtectionLegMarker(Tunnel tunnel, Link ochLink, RemoveProtectionLegContext context) {
        ChangedObject changedObject = new ChangedObject();
            Link lockedOchLink = changedObject.getChangedOchLink(ochLink.getLinkId().getValue());
            Och ochAttr = lockedOchLink.getAugmentation(Link1.class).getOch();
            RemoveProtectionLegContext lockedContext = resolveRemoveProtectionLegContext(lockedOchLink, context.siteLinkIds);
            log.info("Persist remove-leg marker start, tunnelId={}, ochLinkId={}, lockedContext={}, currentOchState={}, currentLegCount={}",
                tunnel.getTunnelId().getValue(),
                lockedOchLink.getLinkId().getValue(),
                contextSummary(lockedContext),
                ochAttr.getImplementState(),
                getProtectionLegCount(lockedOchLink));
            Properties removeMarker = addProtectionChangeProperties(ochAttr.getProperties(),
                PROTECTION_CHANGE_ACTION_REMOVE_LEG,
                lockedContext.siteLinkIds,
                lockedContext.fromLegCount,
                lockedContext.toLegCount);
            Link markedOchLink = new LinkBuilder(lockedOchLink).addAugmentation(Link1.class, new Link1Builder()
                    .setOch(new OchBuilder(ochAttr)
                        .setImplementState(ImplementState.Doimplementing)
                        .setProperties(removeMarker)
                        .build())
                    .build())
                .build();
            changedObject.addChangedOchLink(markedOchLink);

            List<Tunnel> sameOchTunnels = tunnelDao.getAllTunnelsUnderOchLink(Arrays.asList(markedOchLink.getLinkId().getValue()));
            log.info("Mark implemented remove-leg tunnels[Under this OchLink] Doimplementing, "
                            + "keep allocate tunnels unchanged, ochLinkId={}, sameOchTunnels={}, marker={}",
                markedOchLink.getLinkId().getValue(),
                sameOchTunnels.stream().map(t -> t.getTunnelId().getValue()).collect(Collectors.toList()),
                markerSummary(removeMarker));
            for (Tunnel sameOchTunnel : sameOchTunnels) {
                Properties tunnelMarker = addProtectionChangeProperties(sameOchTunnel.getProperties(),
                        PROTECTION_CHANGE_ACTION_REMOVE_LEG,
                        lockedContext.siteLinkIds,
                        lockedContext.fromLegCount,
                        lockedContext.toLegCount);
                changedObject.addChangedTunnel(markTunnelForRemoveLeg(sameOchTunnel, tunnelMarker));
            }
            log.info("Saving remove-leg marker changes, tunnelId={}, changedObject={}",
                tunnel.getTunnelId().getValue(), changedObjectSummary(changedObject));
            multipleTransaction.save(changedObject);
        log.info("Saved remove-leg marker changes, tunnelId={}, ochLinkId={}, context={}",
                tunnel.getTunnelId().getValue(), markedOchLink.getLinkId().getValue(), contextSummary(lockedContext));
    }

    /**
     * 在删除任何数据库路由或资源前，将“设备侧目标腿已全部撤销”持久化。
     * 重试发现该阶段后必须跳过设备下发、直接继续 DB trim；旧路由在 trim 成功前始终保留，
     * 因而仍可按 marker 定位、检查和修复所有相关资源。
     * 调用方必须持有设备阶段的分布式资源锁，使设备成功与完成阶段落库成为一个不可穿插的临界区。
     */
    private void persistRemoveProtectionLegDeviceDeimplemented(Tunnel tunnel, Link ochLink,
                                                                  RemoveProtectionLegContext context) {
        ChangedObject changedObject = new ChangedObject();
        Link lockedOchLink = changedObject.getChangedOchLink(ochLink.getLinkId().getValue());
        Och ochAttr = lockedOchLink.getAugmentation(Link1.class).getOch();
        RemoveProtectionLegContext lockedContext = resolveRemoveProtectionLegContext(lockedOchLink, context.siteLinkIds);
        String phase = getRemoveLegPhase(ochAttr.getProperties());
        if (REMOVE_LEG_PHASE_DEVICE_DEIMPLEMENTED.equals(phase)) {
            return;
        }
        if (!REMOVE_LEG_PHASE_PREPARED.equals(phase)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                "Cannot complete remove-leg device phase from phase: " + phase);
        }

        Properties completedMarker = addRemoveLegPhase(ochAttr.getProperties(),
            REMOVE_LEG_PHASE_DEVICE_DEIMPLEMENTED);
        Link completedOchLink = new LinkBuilder(lockedOchLink).addAugmentation(Link1.class, new Link1Builder()
                .setOch(new OchBuilder(ochAttr)
                    .setImplementState(ImplementState.Doimplementing)
                    .setProperties(completedMarker)
                    .build())
                .build())
            .build();
        changedObject.addChangedOchLink(completedOchLink);

        List<Tunnel> sameOchTunnels = tunnelDao.getAllTunnelsUnderOchLink(
            Arrays.asList(completedOchLink.getLinkId().getValue()));
        for (Tunnel sameOchTunnel : sameOchTunnels) {
            Properties completedTunnelMarker = addRemoveLegPhase(sameOchTunnel.getProperties(),
                    REMOVE_LEG_PHASE_DEVICE_DEIMPLEMENTED);
            changedObject.addChangedTunnel(markTunnelForRemoveLeg(sameOchTunnel,
                    completedTunnelMarker));
        }
        multipleTransaction.save(changedObject);
        log.info("Persisted remove-leg device-deimplemented phase, tunnelId={}, ochLinkId={}, context={}",
            tunnel.getTunnelId().getValue(), completedOchLink.getLinkId().getValue(), contextSummary(lockedContext));
    }

    /**
     * Writes the durable implement operation marker. The marker is intentionally explicit
     * because implement-state alone cannot distinguish add-leg, remove-leg, or unrelated partial states.
     */
    private Properties addProtectionChangeProperties(Properties properties, String action, List<String> siteLinkIds,
                                                     int fromLegCount, int toLegCount) {
        Properties result = PropertyTool.addProperty(properties, PROTECTION_CHANGE_ACTION, action);
        result = PropertyTool.addProperty(result, PROTECTION_CHANGE_SITE_LINK_IDS, String.join(",", siteLinkIds));
        result = PropertyTool.addProperty(result, PROTECTION_CHANGE_FROM_LEG_COUNT, String.valueOf(fromLegCount));
        result = PropertyTool.addProperty(result, PROTECTION_CHANGE_TO_LEG_COUNT, String.valueOf(toLegCount));
        return addRemoveLegPhase(result, REMOVE_LEG_PHASE_PREPARED);
    }

    private Properties addRemoveLegPhase(Properties properties, String phase) {
        return PropertyTool.addProperty(properties, PROTECTION_CHANGE_PHASE, phase);
    }

    /**
     * 兼容旧 marker：没有阶段信息时一律视为 prepared。这样重试会在资源仍完整的前提下重做设备撤销，
     * 而不会依据未经确认的结果直接裁库。
     */
    private boolean isRemoveLegDeviceDeimplemented(Link ochLink) {
        Och ochAttr = ochLink.getAugmentation(Link1.class).getOch();
        String action = PropertyTool.getValue(ochAttr.getProperties(), PROTECTION_CHANGE_ACTION);
        if (action == null || action.trim().isEmpty()) {
            return false;
        }
        if (!PROTECTION_CHANGE_ACTION_REMOVE_LEG.equals(action)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                "Cannot remove protection leg while another protection change is unfinished: " + action);
        }
        String phase = getRemoveLegPhase(ochAttr.getProperties());
        if (REMOVE_LEG_PHASE_PREPARED.equals(phase)) {
            return false;
        }
        if (REMOVE_LEG_PHASE_DEVICE_DEIMPLEMENTED.equals(phase)) {
            return true;
        }
        throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
            "Unknown remove-leg protection change phase: " + phase);
    }

    private String getRemoveLegPhase(Properties properties) {
        String phase = PropertyTool.getValue(properties, PROTECTION_CHANGE_PHASE);
        return phase == null || phase.trim().isEmpty() ? REMOVE_LEG_PHASE_PREPARED : phase.trim();
    }

    public Link getOchLinkByTunnel(Tunnel tunnel) {
        if (tunnel == null || tunnel.getSupportingLink() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "invalid tunnel, no supporting och link");
        }
        String ochLinkId = tunnel.getSupportingLink().stream()
            .filter(sl -> sl != null && sl.getTopologyRef() != null && sl.getLinkRef() != null)
            .filter(sl -> TopoNameConstants.Och_Topo_Key.equals(sl.getTopologyRef().getValue()))
            .findFirst()
            .map(sl -> sl.getLinkRef().getValue())
            .orElseThrow(() -> new CommonException(CommonExceptionType.INVALID_PARAMETER, "invalid tunnel, no supporting och link"));
        Link ochLink = ochLinkDao.getOchLinkByLinkId(ochLinkId);
        if (ochLink == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "supporting OCH does not exist: " + ochLinkId);
        }
        return ochLink;
    }

    private String contextSummary(RemoveProtectionLegContext context) {
        if (context == null) {
            return "null";
        }
        return String.format("leg=%s, from=%d, to=%d, siteLinks=%s, phyLinks=%s, routeXcs=%d",
            context.legName,
            context.fromLegCount,
            context.toLegCount,
            context.siteLinkIds,
            context.phyLinkIds,
            sizeOf(context.routeXcs));
    }

    private int sizeOf(List<?> items) {
        return items == null ? 0 : items.size();
    }

    private String markerSummary(Properties properties) {
        if (properties == null) {
            return "none";
        }
        String action = PropertyTool.getValue(properties, PROTECTION_CHANGE_ACTION);
        if (action == null) {
            return "none";
        }
        return String.format("action=%s, from=%s, to=%s, siteLinks=%s",
            action,
            PropertyTool.getValue(properties, PROTECTION_CHANGE_FROM_LEG_COUNT),
            PropertyTool.getValue(properties, PROTECTION_CHANGE_TO_LEG_COUNT),
            PropertyTool.getValue(properties, PROTECTION_CHANGE_SITE_LINK_IDS));
    }

    /**
     * Counts protection legs from the OCH route. This is route-based, not inferred from supportingLink.
     */
    private int getProtectionLegCount(Link ochLink) {
        Och ochAttr = ochLink.getAugmentation(Link1.class).getOch();
        int legCount = 0;
        for (Route route : ochAttr.getExplictRoute().getRoute()) {
            if (route.getPrimary() != null) {
                legCount = Math.max(legCount, 1);
            }
            if (route.getSecondary() != null) {
                legCount = Math.max(legCount, 2);
            }
            if (route.getThird() != null && !route.getThird().isEmpty()) {
                legCount = Math.max(legCount, 3);
            }
        }
        return legCount;
    }

    private int getProtectionChangeInt(Properties properties, String propertyName) {
        String value = PropertyTool.getValue(properties, propertyName);
        if (value == null || value.trim().isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                "Unfinished remove-leg marker has no " + propertyName);
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                "Invalid unfinished remove-leg marker " + propertyName + ": " + value, e);
        }
    }


    /**
     * Site external OmsLinks are reusable add/drop links owned by the SiteLink, not by a protection leg.
     */
    private Set<String> getSiteExternalOmsLinkIds(ChangedObject changedObject, List<String> siteLinkIds) {
        Set<String> result = new HashSet<>();
        for (String siteLinkId : siteLinkIds) {
            Link siteLink = changedObject.getChangedSiteLink(siteLinkId);
            if (siteLink == null) {
                continue;
            }
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1 siteLinkAug =
                siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class);
            if (siteLinkAug == null || siteLinkAug.getSite() == null) {
                continue;
            }
            Site site = siteLinkAug.getSite();
            collectExternalOmsLinkIds(site.getAExternal() == null ? null : site.getAExternal().getAddDropLink(), result);
            collectExternalOmsLinkIds(site.getZExternal() == null ? null : site.getZExternal().getAddDropLink(), result);
        }
        return result;
    }

    private void collectExternalOmsLinkIds(List<AddDropLink> addDropLinks, Set<String> result) {
        if (addDropLinks == null) {
            return;
        }
        for (AddDropLink addDropLink : addDropLinks) {
            if (addDropLink.getLinkRef() != null && PhysicalLinkIdNamingRule.isOmsLink(addDropLink.getLinkRef())) {
                result.add(addDropLink.getLinkRef());
            }
        }
    }

    private String changedObjectSummary(ChangedObject changedObject) {
        return String.format("tunnels=%d, ochLinks=%d, siteLinks=%d, phyLinks=%d, phyNodes=%d",
            changedObject.getChangedTunnelList().size(),
            changedObject.getChangedOchLinkList().size(),
            changedObject.getChangedSiteLinkList().size(),
            changedObject.getChangedPhyLinkList().size(),
            changedObject.getChangedPhyNodeList().size());
    }

    private void appendTaskContext(TaskInfoMessage taskInfo, String phase, Link ochLink,
                                   RemoveProtectionLegContext context) {
        List<Tunnel> sameOchTunnels = tunnelDao.getAllTunnelsUnderOchLink(
                Collections.singletonList(ochLink.getLinkId().getValue()));
        List<String> affectedTunnelIds = sameOchTunnels.stream()
                .map(tunnel -> tunnel.getTunnelId().getValue()).collect(Collectors.toList());
        List<String> affectedXcIds = new ArrayList<>();
        context.routeXcs.forEach(xc -> affectedXcIds.add(xc.getCrossConnectionId().getValue()));
        sameOchTunnels.forEach(sameOchTunnel -> getRemovedTunnelLegXcs(sameOchTunnel, context).forEach(xc ->
                affectedXcIds.add(xc.getCrossConnectionId().getValue())));
        appendTaskDetail(taskInfo, phase, String.format(
                "ochLinkId=%s, leg=%s, from=%d, to=%d, tunnels=%s, siteLinks=%s, phyLinks=%s, xcs=%s",
                ochLink.getLinkId().getValue(), context.legName, context.fromLegCount, context.toLegCount,
                affectedTunnelIds, context.siteLinkIds, context.phyLinkIds,
                affectedXcIds.stream().distinct().collect(Collectors.toList())));
    }

    private void appendTaskDetail(TaskInfoMessage taskInfo, String phase, String detail) {
        String entry = "[remove-leg:" + phase + "] " + detail;
        String existing = taskInfo.getDetail();
        taskInfo.setDetail(existing == null || existing.isEmpty() ? entry : existing + System.lineSeparator() + entry);
    }

    private List<String> getProtectionChangeSiteLinkIds(Properties properties) {
        String markerSiteLinks = PropertyTool.getValue(properties, PROTECTION_CHANGE_SITE_LINK_IDS);
        if (markerSiteLinks == null || markerSiteLinks.trim().isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                "Unfinished remove-leg marker has no siteLink ids");
        }
        return Arrays.stream(markerSiteLinks.split(","))
            .map(String::trim)
            .filter(item -> !item.isEmpty())
            .collect(Collectors.toList());
    }

    /**
     * Collects all siteLinks already used by an OCH route so new candidates cannot overlap.
     */
    private List<String> getRouteSiteLinkIds(Link ochLink) {
        List<String> result = new ArrayList<>();
        Och ochAttr = ochLink.getAugmentation(Link1.class).getOch();
        for (Route route : ochAttr.getExplictRoute().getRoute()) {
            if (route.getPrimary() != null) {
                result.addAll(getRouteSiteLinkIds(route.getPrimary().getExplicitRouteObjects()));
            }
            if (route.getSecondary() != null) {
                result.addAll(getRouteSiteLinkIds(route.getSecondary().getExplicitRouteObjects()));
            }
            if (route.getThird() != null) {
                for (Third third : route.getThird()) {
                    result.addAll(getRouteSiteLinkIds(third.getExplicitRouteObjects()));
                }
            }
        }
        return result.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Extracts siteLink IDs from explicit route objects.
     */
    private List<String> getRouteSiteLinkIds(List<ExplicitRouteObjects> explicitRouteObjects) {
        List<String> result = new ArrayList<>();
        for (ExplicitRouteObjects ero : explicitRouteObjects) {
            for (PathRouteObject pathRouteObject : ero.getPathRouteObject()) {
                if (pathRouteObject.getResourceType() instanceof org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) {
                    String linkId = ((org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) pathRouteObject.getResourceType())
                        .getLinkHop()
                        .getLinkRef()
                        .getValue();
                    if (SiteLinkIdNamingRule.isSiteLink(linkId)) {
                        result.add(linkId);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Extracts non-site physical links from a leg route so remove-leg can delete only the phy/internal links
     * introduced by that leg.
     */
    private List<String> getRoutePhyLinkIds(List<ExplicitRouteObjects> explicitRouteObjects) {
        List<String> result = new ArrayList<>();
        for (ExplicitRouteObjects ero : explicitRouteObjects) {
            for (PathRouteObject pathRouteObject : ero.getPathRouteObject()) {
                if (pathRouteObject.getResourceType() instanceof org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) {
                    String linkId = ((org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) pathRouteObject.getResourceType())
                        .getLinkHop()
                        .getLinkRef()
                        .getValue();
                    if (!SiteLinkIdNamingRule.isSiteLink(linkId)) {
                        result.add(linkId);
                    }
                }
            }
        }
        return result.stream().distinct().collect(Collectors.toList());
    }

}

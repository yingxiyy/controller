package net.flex.dci.otn.controller.implement.tunnel.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.OchLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.tools.lock.AbstractResourceLock;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.implement.common.ase.AseInjectModeUpdator;
import net.flex.dci.otn.controller.implement.common.ase.DummyOchAllocatorOverTunnel;
import net.flex.dci.otn.controller.implement.common.ase.ne.bytedance.OdDgeSpecfic;
import net.flex.dci.otn.controller.implement.common.ase.ne.bytedance.OdIlaSpecfic;
import net.flex.dci.otn.controller.implement.common.ase.ne.bytedance.OdOtmSpecfic;
import net.flex.dci.otn.controller.implement.common.ase.ne.bytedance.OdRoadmSpecfic;
import net.flex.dci.otn.controller.implement.common.ase.ne.bytedance.TdOtmSpecfic;
import net.flex.dci.otn.controller.implement.common.ase.ne.bytedance.TdRegSpecfic;
import net.flex.dci.otn.controller.implement.common.config.ImplConfig;
import net.flex.dci.otn.controller.implement.common.impl.ExternalLinkUpdate;
import net.flex.dci.otn.controller.implement.common.impl.Implementor;
import net.flex.dci.otn.controller.implement.common.impl.SharedPhysicalLinkUsage;
import net.flex.dci.otn.controller.implement.common.lifecycle.LifeCycleSevice;
import net.flex.dci.otn.controller.implement.common.utils.BindingThirdLegScope;
import net.flex.dci.otn.controller.implement.common.utils.DebugInfo;
import net.flex.dci.otn.controller.implement.common.utils.RouteExtractor;
import net.flex.dci.otn.controller.implement.common.utils.SiteTypeUtils;
import net.flex.dci.otn.controller.implement.tunnel.impl.util.OchLinkRoute;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.OchBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.SiteType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.PrimaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Third;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.supported.tunnels.SupportedTunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.TunnelBuilder;

@Slf4j
public class TunnelImplementor extends Implementor {

    private final static PhyNodeDao phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);
    private final static TunnelDao tunnelDao = SpringBeanFinder.getBean(TunnelDao.class);
    private final static OchLinkDao ochLinkDao = SpringBeanFinder.getBean(OchLinkDao.class);
    private final static SiteLinkDao siteLinkDao = SpringBeanFinder.getBean(SiteLinkDao.class);
    private final static PhyLinkDao phyLinkDao = SpringBeanFinder.getBean(PhyLinkDao.class);

    private final static ConcurrentHashMap<String, ReentrantLock> siteLinkMutexMap = new ConcurrentHashMap<>();
    private final static String BINDING_3_RD_LEG = "binding3rdLeg";
    private final static String BINDING_3_RD_LEG_RESTORE_TUNNEL = "binding3rdLegRestoreTunnel";
    private final static String OCH_COMMON_STAGE_LOCK_PREFIX = "OCH-COMMON-STAGE:";

    private ImplConfig implConfig;
    private String tunnelId;
    private Tunnel tunnel;
    private Supplier<AbstractResourceLock> resourceLockFactory = ZkResourceLock::new;

    // Runtime scope for one bind-third-leg implement flow; object lifecycle ends with this implementor.
    private BindingThirdLegScope bindingThirdLegScope = BindingThirdLegScope.empty();
    private List<String> newBindingSiteLinkIds = Collections.emptyList();
    private boolean ochCommonStageReadyForFollowers = false;
    private final boolean skipOchOpticalInCurrentBatch;
    private final Runnable ochCommonStageReadyCallback;

    //  private ImplementState targetState;
    private LifeCycleSevice lifeService;

    public TunnelImplementor(String tunnelId, ImplementState implementState,
            LifeCycleSevice lifeService) {
        this(tunnelId, implementState, lifeService, false, null);
    }

    public TunnelImplementor(String tunnelId, ImplementState implementState,
            LifeCycleSevice lifeService, boolean skipOchOpticalInCurrentBatch) {
        this(tunnelId, implementState, lifeService, skipOchOpticalInCurrentBatch, null);
    }

    public TunnelImplementor(String tunnelId, ImplementState implementState,
            LifeCycleSevice lifeService, boolean skipOchOpticalInCurrentBatch,
            Runnable ochCommonStageReadyCallback) {
        super(tunnelId, implementState);

        this.tunnelId = tunnelId;
        this.lifeService = lifeService;
        this.skipOchOpticalInCurrentBatch = skipOchOpticalInCurrentBatch;
        this.ochCommonStageReadyCallback = ochCommonStageReadyCallback;

        implConfig = SpringBeanFinder.getBean(ImplConfig.class);
    }

    public String getTunnelId() {
        return getLinkId();
    }

    public boolean isOchCommonStageReadyForFollowers() {
        return ochCommonStageReadyForFollowers;
    }

    private void notifyOchCommonStageReady() {
        if (ochCommonStageReadyForFollowers) {
            return;
        }
        // Followers may start only after the complete OCH common stage is durable.
        ochCommonStageReadyForFollowers = true;
        if (ochCommonStageReadyCallback != null) {
            try {
                ochCommonStageReadyCallback.run();
            } catch (RuntimeException e) {
                // Scheduling failure must not turn a durable OCH result into a
                // device failure; TunnelImplementSync retries dispatch at task end.
                log.error("Failed to dispatch followers after OCH common stage", e);
            }
        }
    }

    @Override
    public void startSyncAction() {
        ChangedObject cache = new ChangedObject();
        tunnel = cache.getChangedTunnel(tunnelId);

        lifeService.updateTaskInfo(tunnel.getTunnelId().getValue(),
                TaskInfoMessage.ResourceType.tunnel,
                tunnel.getFriendlyName());

        String msg = null;
        try {
            doIt(cache);  //no exception throw out
        } catch (Exception e) {
            log.error("error happen on tunnel sync. ", e);
            msg = ExceptionUtils.getRootCauseMessage(e);

            log.debug("This is partial, reset all cache, target is {}", targetState);
        } finally {
            lifeService.logEndLinkImpl(msg);
            implConfig.setTunnelForceDeimplement(false);
        }
    }

    private RouteInfo getRouteInfo(ChangedObject cache) {
        String supportingOchLinkId = tunnel.getSupportingLink().get(0).getLinkRef().getValue();
        Link supportingOchLink = cache.getChangedOchLink(supportingOchLinkId);
        Och supportingOch = supportingOchLink.getAugmentation(Link1.class).getOch();
        if (isBindingThirdLegImplementCandidate(supportingOch, tunnel)) {
            // A third-leg retry must be rebuilt from the persisted third route;
            // existing primary and secondary resources are carrying traffic.
            return buildBindingThirdLegRouteInfo(cache, supportingOchLink);
        }

        RouteInfo rInfo = new RouteInfo();

        log.debug("start parse route {}", tunnelId);
        rInfo.parse(tunnel.getExplictRoute().getRoute());

        List<String> shoudlRemoved = new ArrayList<>();
        List<String> svrLinkIdList = new ArrayList<>(rInfo.getLogicServerLinkIdList());

        for (String logicSvrLinkId : svrLinkIdList) {
            if (OchLinkIdNamingRule.isOchLink(logicSvrLinkId)) {
                log.debug("start checkActionOnOchLink");

                boolean shouldAction = !skipOchOpticalInCurrentBatch
                        && checkActionOnOchLink(cache, targetState,
                        tunnel.getTunnelId(), logicSvrLinkId, rInfo);
                if (!shouldAction) {
                    shoudlRemoved.add(logicSvrLinkId);
                } else {
                    log.debug("should action on the och link");
                    RouteInfo ochRInfo = new RouteInfo();
                    Link ochLink = cache.getChangedOchLink(logicSvrLinkId);
                    ochRInfo.parse(ochLink.getAugmentation(Link1.class).getOch().getExplictRoute()
                            .getRoute());

                    removeTransceiver(ochRInfo);
                    rInfo.extend(ochRInfo);
                }
                break;
            }
        }

        rInfo.getLogicServerLinkIdList().removeAll(shoudlRemoved);

        removeTransceiver(rInfo); //part of C/L TP hasn't related transceiver

        // A newly bound 3rd leg may cross multiple siteLinks in ROADM networks.
        newBindingSiteLinkIds = check3rdLeg(cache, rInfo, cache.getChangedTunnel(tunnel.getTunnelId().getValue()));

        log.info("actionon on tunnel: {} {} to {} \n\n RouteInfo {}",
                tunnel.getTunnelId(), tunnel.getFriendlyName(),
                targetState, rInfo);

        checkNodeManagement(rInfo.getNodeIdList());

        return rInfo;
    }

    private RouteInfo buildBindingThirdLegRouteInfo(ChangedObject cache, Link ochLink) {
        RouteInfo rInfo = buildRouteInfoFromOchThirdRoute(ochLink);
        // The third route has no APS XC, but binding enables its C member on the existing primary APS XC.
        BindingThirdLegScope.includePrimaryApsXcs(rInfo,
                ochLink.getAugmentation(Link1.class).getOch());
        removeTransceiver(rInfo);
        checkOchSiteLinkAtImpl(cache, rInfo);

        bindingThirdLegScope = BindingThirdLegScope.fromOchThirdRoute(cache, ochLink);
        bindingThirdLegScope.trimLogicServerLinks(rInfo);
        String ochLinkId = ochLink.getLinkId().getValue();
        if (!rInfo.getLogicServerLinkIdList().contains(ochLinkId)) {
            rInfo.getLogicServerLinkIdList().add(ochLinkId);
        }
        if (!BindingThirdLegScope.active(bindingThirdLegScope)) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "cannot identify binding third leg siteLinks");
        }
        newBindingSiteLinkIds = bindingThirdLegScope.getSiteLinkIds();

        log.info("actionon on binding third leg tunnel: {} {} to {} \n\n RouteInfo {}",
                tunnel.getTunnelId(), tunnel.getFriendlyName(), targetState, rInfo);
        checkNodeManagement(rInfo.getNodeIdList());
        return rInfo;
    }

    private RouteInfo buildRouteInfoFromOchThirdRoute(Link ochLink) {
        RouteInfo routeInfo = new RouteInfo();
        Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();
        for (Route route : ochLinkAttr.getExplictRoute().getRoute()) {
            if (route.getThird() == null) {
                continue;
            }
            for (Third third : route.getThird()) {
                // RouteInfo only parses primary/secondary; fake a primary route
                // so binding-3rd-leg never parses and deletes old primary/secondary resources.
                Route thirdOnlyRoute = new RouteBuilder(route)
                        .setPrimary(new PrimaryBuilder()
                                .setExplicitRouteObjects(third.getExplicitRouteObjects())
                                .setCrossConnections(third.getCrossConnections())
                                .build())
                        .setSecondary(null)
                        .setThird(null)
                        .build();
                routeInfo.parse(Collections.singletonList(thirdOnlyRoute));
            }
        }
        return routeInfo;
    }

    private boolean isBindingThirdLegImplementCandidate(Och ochLinkAttr, Tunnel tunnel) {
        return targetState.equals(ImplementState.Implement)
                && tunnel != null
                && BindingThirdLegScope.isBindingThirdLeg(ochLinkAttr)
                && PropertyTool.existProperty(tunnel.getProperties(),
                BINDING_3_RD_LEG_RESTORE_TUNNEL);
    }

    //bytedance L1x12C8, L1x12C8_L hasn't transceiver on L port
    private void removeTransceiver(RouteInfo rInfo) {
        rInfo.getEqIdList().removeIf(eqId -> {
            if (eqId.contains("TRANSCEIVER")) {
                String nodeId = PhysicalEqpIdNamingRule.getNodeId(eqId);
                Physical nodeAttr = phyNodeDao.getConfigPhysicalByNode(nodeId);
                Equipments eq = nodeAttr.getEquipments().stream()
                        .filter(x -> x.getEquipmentId().equals(eqId)).findAny().orElse(null);
                if (eq == null) {
                    return true;
                }
            }
            return false;
        });
    }


    /**
     * this function update rInfo and export which siteLink is new inserted;
     *
     * @param cache
     * @param rInfo
     * @param tunnel
     * @return
     */
    private List<String> check3rdLeg(ChangedObject cache, RouteInfo rInfo, Tunnel tunnel) {
        // Reset per-flow scope before checking current OCH binding state.
        if (targetState.equals(ImplementState.Allocate)) {
            return Collections.emptyList(); //only for implement
        }
        bindingThirdLegScope = BindingThirdLegScope.empty();
        newBindingSiteLinkIds = Collections.emptyList();

        String ochLinkId = tunnel.getSupportingLink().get(0).getLinkRef().getValue();
        Link ochLink = cache.getChangedOchLink(ochLinkId);
        Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();

        log.debug("is partial? {}",
                ochLinkAttr.getImplementState().equals(ImplementState.PartialImplement));
        log.debug("is binding3rdLeg? {}",
                PropertyTool.existProperty(ochLinkAttr.getProperties(), "binding3rdLeg"));
        if (ochLinkAttr.getImplementState().equals(ImplementState.PartialImplement) &&
                PropertyTool.existProperty(ochLinkAttr.getProperties(), "binding3rdLeg")) {
            if (!rInfo.getLogicServerLinkIdList().contains(ochLinkId)) {
                // Another tunnel under the same OCH owns this bind-third-leg optical download.
                // Keep this tunnel on the normal electric-only path and do not activate scope.
                return Collections.emptyList();
            }
            //remove C and L port

            log.debug("processing binding 3rd leg function, remove all has implemented objects");

            //tp
            List<String> hasImplementedObjs = new ArrayList<>();
            for (String tpId : rInfo.getTpIdList()) {
                String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
                Node node = cache.getChangedPhyNode(nodeId);
                TerminationPoint tp = node.getTerminationPoint().stream()
                        .filter(x -> x.getTpId().getValue().equals(tpId))
                        .findAny().orElse(null);
                if (tp == null) {
                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                            "cannot find rInfo related tpId in database " + tpId);
                }
                if (tp.getAugmentation(TerminationPoint1.class).getPhysical().getImplementState()
                        .equals(ImplementState.Implement) &&
                        tp.getAugmentation(TerminationPoint1.class).getPhysical().getAdminState()
                                .equals(AdminStatus.Up)) {
                    hasImplementedObjs.add(tpId);
                }
            }
            hasImplementedObjs.add(tunnel.getSourceTp().get(0).getTpRef().getValue());
            hasImplementedObjs.add(tunnel.getDestinationTp().get(0).getTpRef().getValue());
            hasImplementedObjs.add(ochLink.getSource().getSourceTp().getValue());
            hasImplementedObjs.add(ochLink.getDestination().getDestTp().getValue());
            hasImplementedObjs.forEach(ed -> rInfo.getTpIdList().removeIf(x -> x.equals(ed)));

            //eq
            hasImplementedObjs = new ArrayList<>();
            for (String eqId : rInfo.getEqIdList()) {
                String nodeId = PhysicalEqpIdNamingRule.getNodeId(eqId);
                Node node = cache.getChangedPhyNode(nodeId);
                Equipments eq = node.getAugmentation(Node1.class)
                        .getPhysical().getEquipments().stream()
                        .filter(x -> x.getEquipmentId().equals(eqId))
                        .findAny().orElse(null);
                if (eq == null) {
                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                            "cannot find rInfo related eqId in database " + eqId);
                }
                if (eq.getImplementState().equals(ImplementState.Implement)) {
                    hasImplementedObjs.add(eqId);
                }
            }
            hasImplementedObjs.forEach(eq -> rInfo.getEqIdList().removeIf(x -> x.equals(eq)));

            //internalLink
            hasImplementedObjs = new ArrayList<>();
            for (String ilId : rInfo.getPhyLinkIdList()) {
                String nodeId = PhysicalLinkIdNamingRule.getNodeAId(ilId);
                Node node = cache.getChangedPhyNode(nodeId);
                InternalLinks il = node.getAugmentation(Node1.class)
                        .getPhysical().getInternalLinks().stream()
                        .filter(x -> x.getLinkRef().equals(ilId))
                        .findAny().orElse(null);
                if (il == null) {
                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                            String.format(
                                    "cannot find rInfo related internalLinkId on node in database ",
                                    ilId, nodeId));
                }
                if (il.getImplementState().equals(ImplementState.Implement)) {
                    hasImplementedObjs.add(ilId);
                }

                String zNodeId = PhysicalLinkIdNamingRule.getNodeZId(ilId);
                if (nodeId.equals(zNodeId)) {
                    continue;
                }

                node = cache.getChangedPhyNode(zNodeId);
                il = node.getAugmentation(Node1.class)
                        .getPhysical().getInternalLinks().stream()
                        .filter(x -> x.getLinkRef().equals(ilId))
                        .findAny().orElse(null);
                if (il == null) {
                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                            String.format(
                                    "cannot find rInfo related internalLinkId on node in database ",
                                    ilId, zNodeId));
                }
                if (il.getImplementState().equals(ImplementState.Implement)) {
                    hasImplementedObjs.add(ilId);
                }
            }
            hasImplementedObjs.forEach(il -> rInfo.getPhyLinkIdList().removeIf(x -> x.equals(il)));

            //xc
            hasImplementedObjs = new ArrayList<>();
            for (String xcId : rInfo.getXcIdList()) {
                String nodeId = PhysicalXcIdNamingRule.getNodeId(xcId);
                Node node = cache.getChangedPhyNode(nodeId);
                CrossConnections xc = node.getAugmentation(Node1.class)
                        .getPhysical().getCrossConnections().stream()
                        .filter(x -> x.getCrossConnectionId().getValue().equals(xcId))
                        .findAny().orElse(null);
                if (xc == null) {
                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                            "cannot find rInfo related xcId in database " + xcId);
                }
                if (xc.getImplementState().equals(ImplementState.Implement)) {
                    hasImplementedObjs.add(xcId);
                }
            }
            hasImplementedObjs.forEach(xc -> rInfo.getXcIdList().removeIf(x -> x.equals(xc)));

            BindingThirdLegScope.keepOnlyProtectionCtpWithoutApsXc(rInfo, ochLinkAttr);

            //based on xc remove node
            for (String xcId : hasImplementedObjs) {
                String nodeId = PhysicalXcIdNamingRule.getNodeId(xcId);
                rInfo.getNodeIdList().removeIf(x -> x.equals(nodeId));
            }

            // Binding retry may have implemented TPs while XC/internal-link is still unfinished.
            // Use every remaining resource to identify the affected siteLinks.
            Set<String> impactedNodes =
                    BindingThirdLegScope.trimNodeIdListToImpactedResources(rInfo);

            log.debug("after check 3rd leg, following object will write to device {}", rInfo);

            // The OCH third route is the authoritative new-leg scope; remaining resources
            // may be empty on retry after the third-leg devices were already implemented.
            BindingThirdLegScope scope = BindingThirdLegScope.fromOchThirdRoute(cache, ochLink);
            if (!scope.active()) {
                // Older/incomplete data without a third route falls back to impacted-node inference.
                List<String> siteLinkIds = ochLink.getSupportingLink().stream()
                        .filter(x -> SiteLinkIdNamingRule.isSiteLink(x.getLinkRef().getValue()))
                        .map(x -> x.getLinkRef().getValue())
                        .collect(Collectors.toList());
                List<String> result = siteLinkIds.stream()
                        .filter(siteLink -> impactedNodes.stream().anyMatch(siteLink::contains))
                        .collect(Collectors.toList());
                scope = BindingThirdLegScope.fromSiteLinks(cache, result);
            }

            if (!scope.active()) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "cannot identify which siteLink is just inserted");
            }
            bindingThirdLegScope = scope;
            newBindingSiteLinkIds = bindingThirdLegScope.getSiteLinkIds();
            log.debug("after check 3rd leg, following siteLinks are new inserted {}", newBindingSiteLinkIds);
            // Binding the third leg only needs to touch the newly added siteLink path.
            // Keep the OCH link itself, but remove old primary/secondary siteLinks from
            // later locks/cache/ASE handling so existing legs are not downloaded again.
            bindingThirdLegScope.trimLogicServerLinks(rInfo);

            return newBindingSiteLinkIds;
        }

        return Collections.emptyList();
    }

    /**
     * 一条MPO/WSS物理连接可以支撑多个och link， 但是RouteInfo 提前出来的只是这一条tunnel/och相关的，
     * 这样在deImpl的时候会把其他tunnel/och还在使用的物理连接、端口和单板disable
     *
     * @param rInfo
     */
    private void additionalCheckOnMpo(RouteInfo rInfo) {
        checkPhysicalLinkUsage(rInfo);
    }

    private boolean isAseSpecific(ChangedObject cache) {
        List<String> siteLinkIds = new ArrayList<>();

        Link ochLink = cache.getChangedOchLink(
                tunnel.getSupportingLink().get(0).getLinkRef().getValue());
        siteLinkIds.addAll(
                ochLink.getSupportingLink().stream()
                        .map(sl -> sl.getLinkRef().getValue())
                        .filter(SiteLinkIdNamingRule::isSiteLink)
                        .collect(Collectors.toList())
        );

        boolean oneSiteLinkInjected = false;
        boolean allSiteLinkInjected = true;
        for (String siteLinkId : siteLinkIds) {
            Link siteLink = cache.getChangedSiteLink(siteLinkId);
            Site siteLinkAttr = siteLink.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                    .getSite();

            boolean aseInjected = false;
            if (siteLinkAttr.getDummyLink() != null && !siteLinkAttr.getDummyLink().isEmpty()) {
                aseInjected = true;
                oneSiteLinkInjected = true;
            }
            allSiteLinkInjected = allSiteLinkInjected && aseInjected;
        }

        if (oneSiteLinkInjected) {
            if (allSiteLinkInjected) {
                return true;
            } else {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "The tunnel realted siteLink must be all injected ASE");
            }
        } else {
            return false;
        }
    }

    private void doIt(ChangedObject cache) throws Exception {
        log.info("async update tunnel to {} start...", targetState);

        // Mixed ByteDance/Bone2.0 deployments share this implementor; the persisted
        // dummy-link relation below remains the final per-SiteLink ASE state gate.
        boolean isAseSpec = (implConfig.getYangModel().equals(NeYangModel.ByteDance)
                || implConfig.getYangModel().equals(NeYangModel.Chassis20))
                && isAseSpecific(cache);
        log.debug("is ASE mode {}", isAseSpec);

        if (targetState.equals(ImplementState.Allocate)) {
            toAllocate(isAseSpec);
        } else {
            toImplement(cache, isAseSpec);
        }
    }

    private void toImplement(ChangedObject cache, boolean isAseSpec) throws Exception {
        log.debug("start toImplement");
        String ochLinkId = tunnel.getSupportingLink().get(0).getLinkRef().getValue();
        TunnelElectricFollowerScope privateScope = TunnelElectricFollowerScope.fromTunnel(tunnel);
        boolean bindingThirdLeg = false;
        boolean commonStageAlreadyImplemented = false;

        AbstractResourceLock ochLinkLocker = newResourceLock();
        // Use a dedicated guard key. Nesting a normal OCH resource lock with
        // optical/electric resource locks can deadlock in ZkResourceLock's FIFO queue.
        ochLinkLocker.addResource(OCH_COMMON_STAGE_LOCK_PREFIX + ochLinkId);
        ochLinkLocker.getLock();
        try {
            Link currentOchLink = ochLinkDao.getOchLinkByLinkId(ochLinkId);
            Och currentOch = currentOchLink.getAugmentation(Link1.class).getOch();
            if (ImplementState.Implement.equals(currentOch.getImplementState())
                    && !requiresCompleteImplementFlow(currentOch)) {
                // Another owner or an earlier request has completed the common stage.
                // This tunnel now owns only its customer-side resources.
                commonStageAlreadyImplemented = true;
            } else {
                AbstractResourceLock opticalLocker = null;
            AbstractResourceLock electricLocker = null;
            List<ReentrantLock> siteLinkMutexes = Collections.emptyList();
            RouteInfo opticalRInfo = new RouteInfo();
            RouteInfo electricRInfo = new RouteInfo();
            ChangedObject electricCache = new ChangedObject();
            ChangedObject opticalCache = new ChangedObject();
            try {
                List<String> siteLinkIds = OchLinkRoute.getSiteLinkIds(currentOchLink);
                siteLinkMutexes = lockLocalSiteLinks(siteLinkIds);

                RouteInfo lockedRInfo = getRouteInfo(cache);
                removeImplementedWssLinks(cache, lockedRInfo);
                bindingThirdLeg = isBinding3rdLegImplement();

                // The owner is visible as running while it prepares the OCH common stage.
                log.info("start to doImplementing the tunnel:{}", tunnelId);
                tunnelDao.updateTunnelImplementState(tunnelId, ImplementState.Doimplementing,
                        AdminStatus.Up);

                splitOpticalElectricResource(cache, lockedRInfo, opticalRInfo, electricRInfo);
                // One atomic lock covers the complete optical scope and every
                // SiteLink used by dummy-OCH calculation.
                opticalLocker = lockResourceWithAdditional(opticalRInfo, false, siteLinkIds);
                unlockLocalSiteLinks(siteLinkMutexes);
                siteLinkMutexes = Collections.emptyList();

                log.debug("\n\n===OCH common optical route==\n  {}", opticalRInfo);
                opticalCache.unsetOchLink(ochLinkId);
                Link ochLink = opticalCache.getChangedOchLink(ochLinkId);
                Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();
                boolean routeContainsOch = opticalRInfo.getLogicServerLinkIdList()
                        .contains(ochLinkId);
                boolean ochOpticXcAllImpleted = allOpticalXcAllImpleted(opticalCache,
                        ochLinkAttr);
                boolean shouldImplementOpticalLevel = routeContainsOch
                        && (bindingThirdLeg || !ochOpticXcAllImpleted);

                DummyOchAllocatorOverTunnel injector = new DummyOchAllocatorOverTunnel(tunnel,
                        lifeService.getGroupId(), newBindingSiteLinkIds);
                if (shouldImplementOpticalLevel) {
                    try {
                        loadCache(opticalCache, opticalRInfo);
                        updateDefaultParam(opticalCache, tunnel, opticalRInfo);
                        new ExternalLinkUpdate(opticalCache, opticalRInfo).start();
                        updateTunnelImplementState(opticalRInfo,
                                ImplementState.Doimplementing, false);
                        injector.setCache(opticalCache);

                        if (isAseSpec) {
                            log.info("####to implement, ASE required");
                            injector.releaseAseOch();
                            if (injector.getResultMap() != null) {
                                injector.copyOchXcPrototypeFromDummyOch();
                                injector.actionOnAseOch();
                            }
                            injector.restoreXcAttribute(opticalRInfo, false);
                            updateDefaultParam(opticalCache, tunnel, opticalRInfo);
                        }

                        injector.implementTunnel(opticalRInfo, lifeService);
                        removeAllocatedAseOchLinks(opticalCache);
                        ochImplementStateShouldBe(opticalCache, ochLink,
                                ImplementState.PartialImplement);
                        opticalCache.unsetTunnel(tunnelId);
                        if (isAseSpec) {
                            makeAseAuto(injector);
                        }
                    } catch (Exception e) {
                        updateImplementStateAfterFail(opticalCache, opticalRInfo);
                        throw e;
                    } finally {
                        store2DB(opticalCache);
                        ochLinkDao.updateOchLinkImplementState(ochLinkId,
                                ImplementState.PartialImplement, AdminStatus.Up);
                        DebugInfo.print(opticalCache, opticalRInfo);
                    }
                }

                opticalLocker.unlock();
                opticalLocker = null;

                electricCache.unsetOchLink(ochLinkId);
                Link electricOchLink = electricCache.getChangedOchLink(ochLinkId);
                Och electricOch = electricOchLink.getAugmentation(Link1.class).getOch();
                if (!ImplementState.Implement.equals(electricOch.getImplementState())
                        && !bindingThirdLeg) {
                    updateUnImplementedElectricResourceOverOchLink(electricCache, electricOch,
                            electricRInfo);
                    // Customer-side C ports and service XCs run after the shared OCH stage.
                    privateScope.removeFrom(electricRInfo);
                }

                log.debug("\n\n===OCH common electric route==\n  {}", electricRInfo);
                electricLocker = lockResourceWithAdditional(electricRInfo, true,
                        Collections.singletonList(ochLinkId));
                try {
                    electric2Implement(electricCache, ochLinkId, electricRInfo, injector);
                    ochImplementStateShouldBe(electricCache, electricOchLink,
                            ImplementState.Implement);
                    if (!bindingThirdLeg) {
                        // LinkImplementState uses the tunnel entry to preserve its payload rules;
                        // keep the owner running until its private stage completes.
                        Tunnel commonStageTunnel = electricCache.getChangedTunnel(tunnelId);
                        electricCache.addChangedTunnel(new TunnelBuilder(commonStageTunnel)
                                .setImplementState(ImplementState.Doimplementing)
                                .build());
                    }
                    store2DB(electricCache);

                    if (bindingThirdLeg) {
                        DummyOchAllocatorOverTunnel finalInjector =
                                new DummyOchAllocatorOverTunnel(tunnel,
                                        lifeService.getGroupId(), newBindingSiteLinkIds);
                        finalInjector.setCache(electricCache);
                        finishBinding3rdLeg(electricCache, finalInjector, isAseSpec);
                    }
                } catch (Exception e) {
                    if (bindingThirdLeg) {
                        ochImplementStateShouldBe(electricCache, electricOchLink,
                                ImplementState.PartialImplement);
                    }
                    updateImplementStateAfterFail(electricCache, electricRInfo);
                    throw e;
                } finally {
                    DebugInfo.print(electricCache, electricRInfo);
                }
                } finally {
                    unlockLocalSiteLinks(siteLinkMutexes);
                    if (opticalLocker != null) {
                        opticalLocker.unlock();
                    }
                    if (electricLocker != null) {
                        electricLocker.unlock();
                    }
                }
            }
        } catch (Exception e) {
            // A failed common stage must never leave the owner in Doimplementing;
            // the next batch retries the OCH from the normal owner path.
            tunnelDao.updateTunnelImplementState(tunnelId, ImplementState.PartialImplement,
                    AdminStatus.Up);
            ochLinkDao.updateOchLinkImplementState(ochLinkId,
                    ImplementState.PartialImplement, AdminStatus.Up);
            throw e;
        } finally {
            ochLinkLocker.unlock();
        }

        // The complete common stage, including third-leg finalization, is durable.
        notifyOchCommonStageReady();
        if (commonStageAlreadyImplemented || !bindingThirdLeg) {
            implementPrivateElectric(privateScope);
        }
    }

    private boolean requiresCompleteImplementFlow(Och ochLinkAttr) {
        // An Implement OCH normally needs only tunnel-private resources. The
        // third-leg marker is the exception because its common finalization must retry.
        return isBindingThirdLegImplementCandidate(ochLinkAttr, tunnel);
    }

    private void electric2Implement(ChangedObject electricCache, String ochLinkId,
            RouteInfo electricRInfo, DummyOchAllocatorOverTunnel injector) {
        electricCache.unsetOchLink(ochLinkId);
        electricCache.getChangedOchLink(ochLinkId);

        loadCache(electricCache, electricRInfo);
        updateDefaultParam(electricCache, tunnel, electricRInfo);
        new ExternalLinkUpdate(electricCache, electricRInfo).start();

        injector.setCache(electricCache);
        electricCache.addChangedTunnel(tunnel);

        log.debug("\n\n########### start eletric level logic {}\n\n", electricRInfo);

        injector.implementTunnel(electricRInfo, lifeService);

        log.debug("\n\n########### eletric level logic done\n\n");
    }

    private void preciseElectric2Implement(ChangedObject electricCache,
            TunnelElectricFollowerScope followerScope, DummyOchAllocatorOverTunnel injector) {
        RouteInfo electricRInfo = followerScope.getRouteInfo();
        loadCache(electricCache, electricRInfo);
        // Same-OCH tunnels run their private electric resources concurrently. Keep the
        // pre-action node snapshots so persistence updates only this tunnel's TP/XC fields.
        electricCache.retainOriginalPhyNodes(electricRInfo.getNodeIdList());

        // 精确下发也需要原有 C 口属性预处理；在保留原始快照后修改，供精确存盘计算差异。
        // 不能调用完整 updateDefaultParam，否则会修改未持锁的公共机框、L 口和 APS 资源。
        for (String nodeId : electricRInfo.getNodeIdList()) {
            Node node = electricCache.getChangedPhyNode(nodeId);
            if (NodeType.TD.equals(node.getAugmentation(Node1.class).getPhysical().getNodeType())) {
                new TdOtmSpecfic(electricCache, node, electricRInfo).setPrivateClientPortParams();
            }
        }

        injector.setCache(electricCache);
        electricCache.addChangedTunnel(tunnel);

        log.debug("\n\n########### start precise follower eletric level logic {}\n\n",
                electricRInfo);
        injector.implementTunnel(electricRInfo, lifeService);
        log.debug("\n\n########### precise follower eletric level logic done\n\n");
    }

    private void deImplementPreciseElectricOnly(ChangedObject changedObject,
            TunnelElectricFollowerScope followerScope) throws Exception {
        RouteInfo electricRInfo = followerScope.getRouteInfo();
        loadCache(changedObject, electricRInfo);
        // Deimplementation has the same private-resource concurrency boundary as implement.
        changedObject.retainOriginalPhyNodes(electricRInfo.getNodeIdList());

        DummyOchAllocatorOverTunnel injector = new DummyOchAllocatorOverTunnel(tunnel,
                lifeService.getGroupId(), Collections.emptyList());
        injector.setCache(changedObject);

        boolean electricSuccess = false;
        try {
            log.debug("\n\n########### start precise follower eletric deimplement logic {}\n\n",
                    electricRInfo);
            injector.deImplementTunnel(electricRInfo, lifeService);
            electricSuccess = true;
            log.debug("\n\n########### precise follower eletric deimplement logic done\n\n");
        } catch (Exception e) {
            updatePreciseElectricStateAfterFail(changedObject);
            throw e;
        } finally {
            if (electricSuccess) {
                changedObject.addChangedTunnel(new TunnelBuilder(tunnel)
                        .setImplementState(ImplementState.Allocate)
                        .setAdminState(AdminStatus.Down)
                        .build());
            }
            changedObject.unsetOchLink(tunnel.getSupportingLink().get(0).getLinkRef().getValue());
        }
    }

    private void updatePreciseElectricStateAfterFail(ChangedObject electricCache) {
        electricCache.addChangedTunnel(new TunnelBuilder(tunnel)
                .setImplementState(ImplementState.PartialImplement)
                .build());
    }

    private boolean containsOchLink(RouteInfo rInfo) {
        return rInfo.getLogicServerLinkIdList().stream()
                .anyMatch(OchLinkIdNamingRule::isOchLink);
    }

    private boolean isBinding3rdLegImplement() {
        return targetState.equals(ImplementState.Implement)
                && BindingThirdLegScope.active(bindingThirdLegScope);
    }

    private void implementPrivateElectric(TunnelElectricFollowerScope followerScope)
            throws Exception {
        String ochLinkId = tunnel.getSupportingLink().get(0).getLinkRef().getValue();
        ChangedObject changedObject = new ChangedObject();

        AbstractResourceLock electricLocker = lockExactResource(
                followerScope.lockResourceIds(tunnelId)); //lock 电层精确资源
        try {
            RouteInfo lockedRInfo = followerScope.getRouteInfo();

            log.debug("\n\n===electricRoute==\n  {}", lockedRInfo);

            tunnelDao.updateTunnelImplementState(tunnelId, ImplementState.Doimplementing,
                    AdminStatus.Up); //让tunnel imple状态处于下发中

            DummyOchAllocatorOverTunnel injector = new DummyOchAllocatorOverTunnel(tunnel, lifeService.getGroupId(), newBindingSiteLinkIds);
            try {
                preciseElectric2Implement(changedObject, followerScope, injector);
            } catch (Exception e) {
                updatePreciseElectricStateAfterFail(changedObject);
                throw e;
            } finally {
                changedObject.unsetOchLink(ochLinkId);
                store2DB(changedObject);
                DebugInfo.print(changedObject, lockedRInfo);
            }
        } finally {
            electricLocker.unlock();
        }
    }

    private void finishBinding3rdLeg(ChangedObject changedObject,
            DummyOchAllocatorOverTunnel injector, boolean isAseSpec) {
        if (!BindingThirdLegScope.active(bindingThirdLegScope)) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "cannot identify binding third leg siteLinks");
        }

        if (isAseSpec && ochEndpointLineTpsAdminUp(changedObject)) {
            // 加第3条腿的最后一步：业务和光层资源都成功下发后，关闭新增腿上 IRA 的 ASE 控制模式。
            injector.updateAseControlModeAs(tunnel, lifeService.getGroupId(),
                    AseInjectModeUpdator.ASE_CONTROL_MODE_DISABLE);
        }
        // 只有最后一步成功后才能清 binding3rdLeg；失败时保留标志给下次 toImplement 继续收尾。
        updateOchLinkAttrWhenBind3rdLeg(changedObject);
        store2DB(changedObject);
    }

    private boolean ochEndpointLineTpsAdminUp(ChangedObject changedObject) {
        String ochLinkId = tunnel.getSupportingLink().get(0).getLinkRef().getValue();
        Link ochLink = changedObject.getChangedOchLink(ochLinkId);
        String sourceTpId = ochLink.getSource().getSourceTp().getValue();
        String destTpId = ochLink.getDestination().getDestTp().getValue();
        boolean sourceUp = tpAdminUp(changedObject, sourceTpId);
        boolean destUp = tpAdminUp(changedObject, destTpId);
        // ASE disable is only valid after both endpoint L ports are already admin up in this cache.
        log.info("binding 3rd leg ASE disable endpoint check, ochLink {}, sourceTp {} adminUp {}, destTp {} adminUp {}",
                ochLinkId, sourceTpId, sourceUp, destTpId, destUp);
        return sourceUp && destUp;
    }

    private boolean tpAdminUp(ChangedObject changedObject, String tpId) {
        Node node = changedObject.getChangedPhyNode(PhysicalTpIdNamingRule.getNodeId(tpId));
        TerminationPoint tp = node.getTerminationPoint().stream()
                .filter(x -> x.getTpId().getValue().equals(tpId))
                .findAny()
                .orElse(null);
        if (tp == null || tp.getAugmentation(TerminationPoint1.class) == null
                || tp.getAugmentation(TerminationPoint1.class).getPhysical() == null) {
            log.warn("binding 3rd leg ASE disable endpoint check cannot find physical tp {}", tpId);
            return false;
        }
        return AdminStatus.Up.equals(
                tp.getAugmentation(TerminationPoint1.class).getPhysical().getAdminState());
    }

    private void ochImplementStateShouldBe(ChangedObject opticalCache, Link ochLink,
            ImplementState state) {
        Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();
        Link newOchLink = new LinkBuilder(ochLink).addAugmentation(Link1.class, new Link1Builder()
                        .setOch(new OchBuilder(ochLinkAttr)
                                .setImplementState(state)
                                .setAdminState(AdminStatus.Up)
                                .build())
                        .build())
                .build();

        opticalCache.addChangedOchLink(newOchLink);
    }

    private void loadCache(ChangedObject cache, RouteInfo rInfo) {
        rInfo.getNodeIdList().forEach(cache::getChangedPhyNode);
        rInfo.getPhyLinkIdList().forEach(cache::getChangedPhyLink);
        rInfo.getLogicServerLinkIdList().forEach(svrId -> {
            if (OchLinkIdNamingRule.isOchLink(svrId)) {
                cache.getChangedOchLink(svrId);
            } else if (SiteLinkIdNamingRule.isSiteLink(svrId)) {
                cache.getChangedSiteLink(svrId);
            }
        });
    }

    private void removeImplementedWssLinks(ChangedObject cache, RouteInfo rInfo) {
        // wssLink is shared by multiple business OCH links. If it has already been
        // implemented by another OCH, this tunnel must not mark it Doimplementing,
        // lock it, download it, or update it again. wssChannel XCs are separate OCH
        // resources and remain in the route.
        rInfo.getPhyLinkIdList().removeIf(linkId -> {
            if (!PhysicalLinkIdNamingRule.isWssLink(linkId)) {
                return false;
            }

            Link phyLink = cache.getChangedPhyLink(linkId);
            if (phyLink == null) {
                return false;
            }
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1 phyLinkAttr =
                    phyLink.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class);
            if (phyLinkAttr == null || phyLinkAttr.getPhysical() == null) {
                return false;
            }

            boolean implemented = ImplementState.Implement.equals(
                    phyLinkAttr.getPhysical().getImplementState());
            if (implemented) {
                log.debug("remove implemented wssLink from tunnel route: {}", linkId);
            }
            return implemented;
        });
    }

    private void makeAseManual(DummyOchAllocatorOverTunnel injector) {

        injector.updateAseControlModeAs(tunnel, lifeService.getGroupId(),
                AseInjectModeUpdator.ASE_CONTROL_MODE_MANUAL);
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void makeAseAuto(DummyOchAllocatorOverTunnel injector) {

        injector.updateAseControlModeAs(tunnel, lifeService.getGroupId(),
                AseInjectModeUpdator.ASE_CONTROL_MODE_ENABLE);
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        injector.updateAseControlModeAs(tunnel, lifeService.getGroupId(),
                AseInjectModeUpdator.ASE_CONTROL_MODE_AUTO);

    }


    /**
     * 把ochLink 中的binding3rdLeg 标志删除 同时把这个ochLink 支持的所有tunnel 的implement 状态修改
     *
     * @param changedObject
     */
    private void updateOchLinkAttrWhenBind3rdLeg(ChangedObject changedObject) {
        if (!BindingThirdLegScope.active(bindingThirdLegScope)) {
            return;
        }

        List<String> scopeSiteLinkIds = bindingThirdLegScope.getSiteLinkIds();
        log.debug("binding new 3rd leg has done, remove binding3rdLeg property. {}", scopeSiteLinkIds);
        //如果是绑定了新的3rd leg， 说明之前的binding3rdLeg属性已经没有意义了， 需要删除
        String ochLinkId = tunnel.getSupportingLink().get(0).getLinkRef().getValue();
        if (changedObject.getChangedOchLinkList().containsKey(ochLinkId)) {
            Link ochLink = changedObject.getChangedOchLink(ochLinkId);
            Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();

            Properties newProp = PropertyTool.delProperty(ochLinkAttr.getProperties(),
                    "binding3rdLeg");
            Link newOchLink = new LinkBuilder(ochLink).addAugmentation(Link1.class,
                            new Link1Builder()
                                    .setOch(new OchBuilder(ochLinkAttr)
                                            .setProperties(newProp)
                                            .build())
                                    .build())
                    .build();

            changedObject.addChangedOchLink(newOchLink);
        }

        List<Tunnel> sameOchTunnels = tunnelDao.getAllTunnelsUnderOchLink(
                Arrays.asList(ochLinkId));
        for (Tunnel sameOchTunnel : sameOchTunnels) {
            boolean shouldRestore = PropertyTool.existProperty(sameOchTunnel.getProperties(),
                    BINDING_3_RD_LEG_RESTORE_TUNNEL);
            Properties newProp = PropertyTool.delProperty(sameOchTunnel.getProperties(),
                    "binding3rdLeg");
            newProp = PropertyTool.delProperty(newProp, BINDING_3_RD_LEG_RESTORE_TUNNEL);
            Tunnel newTunnel = new TunnelBuilder(sameOchTunnel)
                    .setProperties(newProp)
                    // Only tunnels changed from Implement to Partial by bind-tunnel are restored here.
                    .setImplementState(shouldRestore ? ImplementState.Implement
                            : sameOchTunnel.getImplementState())
                    .setAdminState(shouldRestore ? AdminStatus.Up : sameOchTunnel.getAdminState())
                    .build();
            changedObject.addChangedTunnel(newTunnel);
        }
    }

    private void toAllocate(boolean isAseSpec) throws Exception {
        log.info("Tunnel change to allocate");

        AbstractResourceLock siteLinkLocker = null;
        boolean siteLinkLocked = false;
        List<ReentrantLock> siteLinkMutexes = Collections.emptyList();
        ChangedObject changedObject = new ChangedObject();
        RouteInfo lockedRInfo;
        TunnelElectricFollowerScope followerScope = null;

        AbstractResourceLock locker = null;
        try {
            String ochLinkId = tunnel.getSupportingLink().get(0).getLinkRef().getValue();
            Link ochLink = ochLinkDao.getOchLinkByLinkId(ochLinkId);
            List<String> siteLinkIds = OchLinkRoute.getSiteLinkIds(ochLink);
            siteLinkMutexes = lockLocalSiteLinks(siteLinkIds);

            siteLinkLocker = newResourceLock();
            siteLinkIds.forEach(siteLinkLocker::addResource);
            siteLinkLocker.getLock();
            siteLinkLocked = true;

            lockedRInfo = getRouteInfo(changedObject);
            if (!containsOchLink(lockedRInfo)) {
                // Non-owner deimplement must only touch this tunnel's endpoint C
                // ports and direct C-L XCs; shared OCH/siteLink resources stay out.
                followerScope = TunnelElectricFollowerScope.fromTunnel(tunnel);
                lockedRInfo = followerScope.getRouteInfo();
            }
            // Keep the OCH delete-owner decision based on stable tunnel states;
            // expose Deimplementing only after getRouteInfo decides whether to include OCH.
            log.info("start to deImplementing the tunnel:{}", tunnelId);
            tunnelDao.updateTunnelImplementState(tunnelId, ImplementState.Deimplementing,
                    AdminStatus.Up);

            siteLinkLocker.unlock();
            siteLinkLocked = false;

            locker = followerScope == null ? lockResource(lockedRInfo, true)
                    : lockExactResource(followerScope.lockResourceIds(tunnelId));
        } finally {
            if (siteLinkLocked && siteLinkLocker != null) {
                siteLinkLocker.unlock();
            }
            unlockLocalSiteLinks(siteLinkMutexes);
        }

        try {
            if (followerScope != null) {
                deImplementPreciseElectricOnly(changedObject, followerScope);
                return;
            }

            loadCache(changedObject, lockedRInfo);

            updateTunnelImplementState(lockedRInfo, ImplementState.Deimplementing, true);
            additionalCheckOnMpo(lockedRInfo);

            DummyOchAllocatorOverTunnel injector = new DummyOchAllocatorOverTunnel(tunnel, lifeService.getGroupId(), Collections.emptyList());
            injector.setCache(changedObject);

            if (isAseSpec && lockedRInfo.getLogicServerLinkIdList() != null
                    && !lockedRInfo.getLogicServerLinkIdList().isEmpty()) {
                log.info("new ASE will be injected");
                //业务OCH 有可能释放，这个应该是先执行Tunnel deImplement
                //检查tunnel使用的OCH释放会被释放, 如果会被释放，需要更新OCM
                //需要先deimple 结束, 释放频段，才开始插入
                //在业务och 释放后需要用dummy och 填充

                //copy 业务波XC 参数到 protoType
                injector.copyOchXcPrototypeFromBusinessOch(
                        tunnel.getSupportingLink().get(0).getLinkRef().getValue());
                updateDefaultParam(changedObject, tunnel, lockedRInfo);
            }

            injector.deImplementTunnel(lockedRInfo, lifeService);

            if (isAseSpec && lockedRInfo.getLogicServerLinkIdList() != null
                    && !lockedRInfo.getLogicServerLinkIdList().isEmpty()) {

                injector.insertAseOch();

                if (injector.getResultMap() != null) {
                    //need update ASE channel....
                    injector.actionOnAseOch();  //default ASE xc param will be copy from prototype
                }

                removeAllocatedAseOchLinks(changedObject);

                log.debug("ASE related OCH info has updated");
            }

            updateSiteLinkImplement(changedObject);
            // A successfully deimplemented restore tunnel must no longer participate in
            // binding-third-leg recovery. The OCH marker is removed only when the OCH
            // itself has reached Allocate, so remaining restore tunnels can still retry.
            clearBindingThirdLegMarkersAfterAllocate(changedObject);
        } catch (Exception e) {
            log.error("error happen, remove all still in allocated ASE ochLink", e);
            if (followerScope == null) {
                updateImplementStateAfterFail(changedObject, lockedRInfo);
            }
            throw e;
        } finally {
            try {
                store2DB(changedObject);

                DebugInfo.print(changedObject, lockedRInfo);
            } finally {
                locker.unlock();
            }
        }
    }

    private void clearBindingThirdLegMarkersAfterAllocate(ChangedObject changedObject) {
        Tunnel allocatedTunnel = changedObject.getChangedTunnelList().get(tunnelId);
        String ochLinkId = allocatedTunnel.getSupportingLink().get(0).getLinkRef().getValue();
        Link allocatedOchLink = changedObject.getChangedOchLinkList().get(ochLinkId);
        Och ochLinkAttr = allocatedOchLink.getAugmentation(Link1.class).getOch();
        boolean tunnelHasMarker = hasBindingThirdLegMarker(allocatedTunnel.getProperties());
        boolean ochHasMarker = hasBindingThirdLegMarker(ochLinkAttr.getProperties());
        if (!tunnelHasMarker && !ochHasMarker) {
            // Normal deimplementation must keep its original query and persistence behavior.
            return;
        }
        
        clearBindingThirdLegMarkers(changedObject, allocatedTunnel);

        boolean ochAllocated = ImplementState.Allocate.equals(ochLinkAttr.getImplementState());
        if (!ochAllocated) {
            return;
        }

        Properties ochProperties = PropertyTool.delProperty(ochLinkAttr.getProperties(),
                BINDING_3_RD_LEG);
        changedObject.addChangedOchLink(new LinkBuilder(allocatedOchLink)
                .addAugmentation(Link1.class, new Link1Builder()
                        .setOch(new OchBuilder(ochLinkAttr)
                                .setProperties(ochProperties)
                                .build())
                        .build())
                .build());
    }

    private void clearBindingThirdLegMarkers(ChangedObject changedObject, Tunnel targetTunnel) {
        if (!hasBindingThirdLegMarker(targetTunnel.getProperties())) {
            return;
        }

        Properties properties = PropertyTool.delProperty(targetTunnel.getProperties(),
                BINDING_3_RD_LEG);
        properties = PropertyTool.delProperty(properties, BINDING_3_RD_LEG_RESTORE_TUNNEL);
        changedObject.addChangedTunnel(new TunnelBuilder(targetTunnel)
                .setProperties(properties)
                .build());
    }

    private boolean hasBindingThirdLegMarker(Properties properties) {
        // OCH carries the operation marker; restore tunnels may carry both markers.
        return PropertyTool.existProperty(properties, BINDING_3_RD_LEG)
                || PropertyTool.existProperty(properties,
                BINDING_3_RD_LEG_RESTORE_TUNNEL);
    }


    private void updateSiteLinkImplement(ChangedObject changedObject) {
        for (String siteLinkId : changedObject.getChangedSiteLinkList().keySet()) {
            Link siteLink = changedObject.getChangedSiteLink(siteLinkId);
            Site siteLinkAttr = siteLink.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                    .getSite();

            if (siteLinkAttr.getImplementState().equals(ImplementState.Allocate)) {
                Link newLink = new LinkBuilder(siteLink)
                        .addAugmentation(
                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class,
                                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder()
                                        .setSite(new SiteBuilder(siteLinkAttr)
                                                .setImplementState(ImplementState.Implement)
                                                .setAdminState(AdminStatus.Up)
                                                .build())
                                        .build())
                        .build();

                changedObject.addChangedSiteLink(newLink);
            }
        }
    }

    //
    //正常tunnel 的下发切割为光层先单独下发， 然后在来电层。 ochLink 归属于光层，因为它上面有光层网元相关的交叉
    //
    private void splitOpticalElectricResource(final ChangedObject changedObject, RouteInfo base,
            RouteInfo opticalRInfo, RouteInfo electricRInfo) {
        // Binding a third leg can leave TD-side C ports and MxDx-C physical links
        // in the remaining resources while nodeIdList only contains optical nodes.
        // Rebuild the node boundary from the remaining objects before splitting.
        BindingThirdLegScope.getImpactedResourceNodeIds(base).forEach(nodeId -> {
            if (!base.getNodeIdList().contains(nodeId)) {
                base.getNodeIdList().add(nodeId);
            }
        });

        base.getNodeIdList().forEach(nodeId -> {
            Node node = changedObject.getChangedPhyNode(nodeId);
            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
            if (nodeAttr.getNodeType().equals(NodeType.TD)) {
                extractRInfo(electricRInfo, base, nodeId);
            } else {
                extractRInfo(opticalRInfo, base, nodeId);
            }
        });

        //由于上面的算法是基于nodeType 过滤，MD--OTU-line 的 物理连接会被关联到opticalRoute, 需要剔除
        base.getNodeIdList().forEach(nodeId -> {
            Node node = changedObject.getChangedPhyNode(nodeId);
            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
            if (nodeAttr.getNodeType().equals(NodeType.TD)) {
                opticalRInfo.getPhyLinkIdList().removeIf(phyLinkId -> {
                    return phyLinkId.contains(nodeId);
                });
            }
        });

        //光层路由ochLink， siteLink 构成
        opticalRInfo.getLogicServerLinkIdList().addAll(base.getLogicServerLinkIdList());
        //电层没有getLogicServerLinkIdList 信息

//        if (base.getLogicServerLinkIdList() != null) {
//            opticalRInfo.getLogicServerLinkIdList().addAll(
//                    base.getLogicServerLinkIdList().stream()
//                            .filter(SiteLinkIdNamingRule::isSiteLink).collect(Collectors.toList()));
//        }
//        List<String> withoutSiteLinkIdList = new ArrayList<>(base.getLogicServerLinkIdList());
//
//        withoutSiteLinkIdList.removeIf(SiteLinkIdNamingRule::isSiteLink);
//        electricRInfo.getLogicServerLinkIdList().addAll(withoutSiteLinkIdList);
    }

    private void extractRInfo(RouteInfo target, RouteInfo base, String nodeId) {
        target.getNodeIdList().add(nodeId);

        base.getEqIdList().forEach(x -> {
            if (x.contains(nodeId)) {
                target.getEqIdList().add(x);
            }
        });

        base.getXcIdList().forEach(x -> {
            if (x.contains(nodeId)) {
                target.getXcIdList().add(x);
            }
        });

        base.getTpIdList().forEach(x -> {
            if (x.contains(nodeId)) {
                target.getTpIdList().add(x);
            }
        });

        base.getPhyLinkIdList().forEach(x -> {
            if (x.contains(nodeId)) {
                target.getPhyLinkIdList().add(x);
            }
        });
    }

    private void removeAllocatedAseOchLinks(ChangedObject changedObject) {
        Set<String> removedDummyOchLinkIds = changedObject.getRemovedOchLinkIdList().stream()
                .filter(this::isDummyOchLinkId)
                .collect(Collectors.toSet());

        List<String> allocatedDummyOchLinkIds = changedObject.getChangedOchLinkList().values()
                .stream()
                .filter(ochLink -> ochLink.getAugmentation(Link1.class).getOch().getImplementState()
                        .equals(ImplementState.Allocate))
                .map(ochLink -> ochLink.getLinkId().getValue())
                .filter(this::isDummyOchLinkId)
                .filter(ochLinkId -> !removedDummyOchLinkIds.contains(ochLinkId))
                .collect(Collectors.toList());

        Set<String> dummyOchLinkIds = new HashSet<>(removedDummyOchLinkIds);
        dummyOchLinkIds.addAll(allocatedDummyOchLinkIds);
        if (dummyOchLinkIds.isEmpty()) {
            return;
        }

        allocatedDummyOchLinkIds.forEach(ochLinkId -> {
            Link ochLink = changedObject.getChangedOchLink(ochLinkId);
            removeAseXcFromNe(changedObject, ochLink);
            changedObject.addRemovedOchLink(ochLinkId);
        });

        //these link should be removed from related siteLink
        for (String siteLinkId : changedObject.getChangedSiteLinkList().keySet()) {
            Link siteLink = changedObject.getChangedSiteLink(siteLinkId);
            Site siteLinkAttr = siteLink.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                    .getSite();

            if (siteLinkAttr.getDummyLink() != null) {
                List<String> newDummyList = siteLinkAttr.getDummyLink().stream()
                        .filter(dummyOchLinkId -> !dummyOchLinkIds.contains(dummyOchLinkId))
                        .collect(Collectors.toList());

                Link newLink = new LinkBuilder(siteLink).addAugmentation(
                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class,
                                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder()
                                        .setSite(new SiteBuilder(siteLinkAttr).setDummyLink(newDummyList)
                                                .build())
                                        .build())
                        .build();

                changedObject.addChangedSiteLink(newLink);
            }
        }
    }

    private boolean isDummyOchLinkId(String ochLinkId) {
        // ByteDance dummy OCH links are identified by EXP33 in the OCH link id.
        // Middle DGE XCs in the dummy route do not contain EXP33, so this helper
        // must only be used for OCH link identity, not per-XC filtering.
        return ochLinkId != null && ochLinkId.contains("EXP33");
    }

    private void removeAseXcFromNe(ChangedObject changedObject, Link ochLink) {
        RouteInfo ochRouteInfo = new RouteInfo();

        try {
            if (ochLink == null) {
                log.error(
                        "cannot remove ASE XC because dummy OCH link is missing in changedObject");
                return;
            }
            Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();
            ochRouteInfo.parse(ochLinkAttr.getExplictRoute().getRoute());
        } catch (java.lang.Exception e) {
            log.error("impossible error.", e);
        }

        for (String xcId : ochRouteInfo.getXcIdList()) {
            try {
                String nodeId = PhysicalXcIdNamingRule.getNodeId(xcId);
                Node node = changedObject.getChangedPhyNode(nodeId);
                Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
                boolean existed = nodeAttr.getCrossConnections().stream()
                        .anyMatch(xc -> xc.getCrossConnectionId().getValue().equals(xcId));
                if (!existed) {
                    log.error(
                            "cannot find ASE XC in changedObject when cleanup dummy OCH, maybe already removed: {}",
                            xcId);
                    continue;
                }
                List<CrossConnections> newXcList = nodeAttr.getCrossConnections().stream()
                        .filter(xc -> !xc.getCrossConnectionId().getValue().equals(xcId))
                        .collect(Collectors.toList());
                node = new NodeBuilder(node).addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder(nodeAttr).setCrossConnections(newXcList)
                                .build())
                        .build()).build();
                changedObject.addChangedPhyNode(node);
            } catch (java.lang.Exception e) {
                log.error("impossible error.", e);
            }
        }
    }

    private void updateDefaultParam(ChangedObject changedObject, Tunnel tunnel,
            RouteInfo tunelRoute) {
        String aTp = tunnel.getSourceTp().get(0).getTpRef().getValue();
        String zTp = tunnel.getDestinationTp().get(0).getTpRef().getValue();
        String aNodeId = PhysicalTpIdNamingRule.getNodeId(aTp);
        String zNodeId = PhysicalTpIdNamingRule.getNodeId(zTp);

        tunelRoute.getNodeIdList().forEach(nodeId -> {
            Node node = changedObject.getChangedPhyNode(nodeId);
            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
            if (nodeAttr == null || nodeAttr.getNodeType() == null) {
                log.debug("nodeAttr or nodeType is null on {}", nodeId);
            }
            if (nodeAttr.getNodeType().equals(NodeType.TD)) {
                if (aNodeId.equals(nodeId) || zNodeId.equals(nodeId)) {
                    new TdOtmSpecfic(changedObject, node, tunelRoute).set();
                } else {
                    // Binding-third-leg does not run AdjustTunnel afterwards; include REG
                    // line-port target power in this implement payload only for that path.
                    new TdRegSpecfic(changedObject, node, tunelRoute,
                            isBinding3rdLegImplement()).set();
                }
            } else {
                if (aTp.contains(nodeId) || zTp.contains(nodeId)) {
                    new OdOtmSpecfic(changedObject, node, tunelRoute).set();
                } else {
                    List<String> linkAZNodes = new ArrayList<>();
                    linkAZNodes.add(PhysicalNodeIdNamingRule.getSiteId(aNodeId));
                    linkAZNodes.add(PhysicalNodeIdNamingRule.getSiteId(zNodeId));

                    SiteType type = SiteTypeUtils.getSiteType(node, tunelRoute,
                            linkAZNodes); //base OA card type find out OTM/DGE/ILA/ROADM
                    switch (type) {
                        case DGE:
                            new OdDgeSpecfic(changedObject, node, tunelRoute).set();
                            break;
                        case ILA:
                            new OdIlaSpecfic(changedObject, node, tunelRoute).set();
                            break;
                        case ROADM:
                            new OdRoadmSpecfic(changedObject, node, tunelRoute).set();
                            break;
                    }
                }
            }
        });
    }

    /**
     * make tunnel and related serverLink, node's implState = required update node, server link as
     * ing status.
     *
     * @param rInfo
     * @param targetState
     */
    private void updateTunnelImplementState(RouteInfo rInfo, ImplementState targetState,
            boolean includeTunnel) throws CommonException {
        log.debug("update tunnel ImplementState {} {}", tunnel.getTunnelId().getValue(),
                targetState.name());

        AdminStatus adminStatus;
        if (targetState.equals(ImplementState.Allocate)) {
            adminStatus = AdminStatus.Down;
        } else {
            adminStatus = AdminStatus.Up;
        }
        if (includeTunnel) {
            tunnelDao.updateTunnelImplementState(tunnel.getTunnelId().getValue(), targetState,
                    adminStatus);
        }
        updateNodeImplementState(rInfo.getNodeIdList(), targetState);

        rInfo.getPhyLinkIdList().forEach(
                phyLinkId -> phyLinkDao.updateLinkImplementState(phyLinkId, targetState,
                        adminStatus));

        for (String linkId : rInfo.getLogicServerLinkIdList()) {
            //tunneL 的server层 是ochLink, phyLink and siteLink
            if (OchLinkIdNamingRule.isOchLink(linkId)) {
                ochLinkDao.updateOchLinkImplementState(linkId, targetState, adminStatus);
            } else if (SiteLinkIdNamingRule.isSiteLink(linkId)) {
                if (targetState.equals(ImplementState.Allocate) || targetState.equals(
                        ImplementState.Implement)) {
                    //allocate 状态下， siteLink 仍然是 implement状态， 只是adminState 变为down
                    siteLinkDao.updateSiteLinkImplementState(linkId, ImplementState.Implement,
                            AdminStatus.Up);
                } else {
                    siteLinkDao.updateSiteLinkImplementState(linkId, targetState, AdminStatus.Up);
                }
            }
        }
    }

    private void updateImplementStateAfterFail(ChangedObject cache, RouteInfo rInfo)
            throws CommonException {
        log.debug("start prepare store fail data into DB ");
        //this only for tunnel, and ochLink if them implementState = targetState
        final AdminStatus adminStatus = AdminStatus.Up;
        final ImplementState fail = ImplementState.PartialImplement;

        cache.addChangedTunnel(new TunnelBuilder(tunnel).setImplementState(fail).build());

        //ochLink 用 summaryLinkState的结果
        List<Link> failOchLinkList = cache.getChangedOchLinkList().values().stream()
                .map(ochLink -> {
                    if (ochLink.getAugmentation(Link1.class).getOch().getImplementState()
                            .equals(ImplementState.Deimplementing) ||
                            ochLink.getAugmentation(Link1.class).getOch().getImplementState()
                                    .equals(ImplementState.Doimplementing)) {
                        return new LinkBuilder(ochLink).addAugmentation(Link1.class,
                                        new Link1Builder()
                                                .setOch(new OchBuilder(
                                                        ochLink.getAugmentation(Link1.class).getOch())
                                                        .setImplementState(fail)
                                                        .build())
                                                .build())
                                .build();
                    }
                    return ochLink;
                }).collect(Collectors.toList());
        failOchLinkList.forEach(cache::addChangedOchLink);

        //siteLink 用 summaryLinkState的结果
        List<Link> failSiteLinkList = cache.getChangedSiteLinkList().values().stream()
                .map(siteLink -> {
                    if (siteLink.getAugmentation(
                                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                            .getSite().getImplementState().equals(ImplementState.Deimplementing) ||
                            siteLink.getAugmentation(
                                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                                    .getSite().getImplementState()
                                    .equals(ImplementState.Doimplementing)) {
                        return new LinkBuilder(siteLink).addAugmentation(
                                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class,
                                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder()
                                                .setSite(new SiteBuilder(siteLink.getAugmentation(
                                                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                                                        .getSite())
                                                        .setImplementState(fail)
                                                        .build())
                                                .build())
                                .build();
                    }
                    return siteLink;
                }).collect(Collectors.toList());
        failSiteLinkList.forEach(cache::addChangedSiteLink);

        //phyLink
        List<Link> failPhyLinkList = cache.getChangedPhyLinkList().values().stream()
                .map(phyLink -> {
                    if (phyLink.getAugmentation(
                                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class)
                            .getPhysical().getImplementState().equals(ImplementState.Deimplementing)
                            ||
                            phyLink.getAugmentation(
                                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class)
                                    .getPhysical().getImplementState()
                                    .equals(ImplementState.Doimplementing)) {
                        return new LinkBuilder(phyLink).addAugmentation(
                                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class,
                                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1Builder()
                                                .setPhysical(
                                                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder(
                                                                phyLink.getAugmentation(
                                                                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class)
                                                                        .getPhysical())
                                                                .setImplementState(fail)
                                                                .build())
                                                .build())
                                .build();
                    }
                    return phyLink;
                }).collect(Collectors.toList());
        failPhyLinkList.forEach(cache::addChangedPhyLink);

        //node
        if (cache.getChangedPhyNodeList().isEmpty()) {
            updateTunnelImplementState(rInfo, fail, false);
        } else {
            List<Node> failNodeList = cache.getChangedPhyNodeList().values().stream()
                    .map(phyNode -> {
                        if (phyNode.getAugmentation(Node1.class).getPhysical().getImplementState()
                                .equals(ImplementState.Deimplementing) ||
                                phyNode.getAugmentation(Node1.class).getPhysical()
                                        .getImplementState()
                                        .equals(ImplementState.Doimplementing)) {
                            return new NodeBuilder(phyNode).addAugmentation(Node1.class,
                                            new Node1Builder()
                                                    .setPhysical(new PhysicalBuilder(
                                                            phyNode.getAugmentation(Node1.class)
                                                                    .getPhysical())
                                                            .setImplementState(fail)
                                                            .build())
                                                    .build())
                                    .build();
                        }
                        return phyNode;
                    }).collect(Collectors.toList());
            failNodeList.forEach(cache::addChangedPhyNode);
        }

        store2DB(cache);
        DebugInfo.print(cache, rInfo);
    }


    private void checkNodeManagement(List<String> nodeIdList) {
        for (String nodeId : nodeIdList) {
            Node cfgNode = phyNodeDao.getConfigPhyNodeById(nodeId);

            if (!implConfig.isWriteWithoutIP()) {
                //OPC 网元需要下发
                Node opNode = phyNodeDao.getOpPhyNodeById(nodeId);
                if (opNode == null) {
                    String errMsg = String.format("网管尚未管理 网元 %s (%s)",
                            cfgNode.getAugmentation(
                                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class)
                                    .getPhysical().getFriendlyName(),
                            cfgNode.getNodeId().getValue());
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER, errMsg);
                }
            }
        }
    }

    private void updateNodeImplementState(Collection<String> nodeIdList, ImplementState targetState)
            throws CommonException {
        log.debug("update Node ImplementState {}({})", targetState.name(), nodeIdList.size());

        ImplementState implementState;
        if (targetState.equals(ImplementState.PartialImplement)) {
            implementState = targetState;
        } else if (targetState.equals(ImplementState.Allocate) || targetState.equals(
                ImplementState.Implement)) {
            implementState = ImplementState.Implement;
        } else {
            implementState = targetState;
        }

        for (String nodeId : nodeIdList) {
            phyNodeDao.updateConfigNodeImplState(nodeId, implementState, AdminStatus.Up);
        }

    }

    /**
     * 看一下对于OCH link 应该做什么
     *
     * @param ochLinkId
     * @return 不需要操作，外面移除
     */
    private boolean checkActionOnOchLink(ChangedObject cache, ImplementState targetState,
            Uri tunnelId, String ochLinkId, RouteInfo rInfo) {
        try {
            log.debug(
                    "check action on ochLink target state:{} and tunnelId:{} reference ochLinkId:{}",
                    targetState, tunnelId, ochLinkId);

            Link ochLink = cache.getChangedOchLink(ochLinkId);
            Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();

            if (targetState.equals(ImplementState.Implement)) {
                boolean ochOpticXcAllImpleted = allOpticalXcAllImpleted(cache, ochLinkAttr);
                if (ochLinkAttr.getImplementState().equals(targetState) && ochOpticXcAllImpleted) {
                    //server has implemented, do nothing
                    log.debug("ochLinkId: has implemented");

                    return false;
                }

                List<SupportedTunnel> supportedTunnelList = ochLink.getAugmentation(
                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class)
                        .getSupportedTunnel();
                List<String> supportedTunnelIds = supportedTunnelList.stream()
                        .map(x -> x.getTunnelRef().getValue())
                        .collect(Collectors.toList());

                List<Tunnel> supportTunnels = tunnelDao.listAllTunnelByIds(supportedTunnelIds);
                List<String> msgs = new ArrayList<>();
                supportTunnels.forEach(tunnel1 -> msgs.add(
                        String.format("%s, %s", tunnel1.getImplementState(),
                                tunnel1.getFriendlyName())));
                log.debug("\n\ntunnels {}\n", String.join("\n", msgs));

                // Keep the 2605 owner decision: before this tunnel is exposed as
                // Doimplementing, only the first all-Allocate OCH user may extend
                // the OCH route and download OCH-level resources.
                boolean allUnImple = supportTunnels.stream().allMatch(
                        supportTunnel -> supportTunnel.getImplementState()
                                .equals(ImplementState.Allocate));

                log.debug("Implement tunnel checking och, allUnImple: {}", allUnImple);
                if (allUnImple) {
                    //implement server too. and extend rInfo
                    RouteInfo ochrInfo = new RouteInfo();
                    ochrInfo.parse(ochLinkAttr.getExplictRoute().getRoute());
                    removeTransceiver(ochrInfo);

                    //检查siteLink是否已经Implement，没有implement 报错
                    checkOchSiteLinkAtImpl(cache, ochrInfo);
                    rInfo.extend(ochrInfo);

                    return true;
                }

                boolean bindingThirdLeg = BindingThirdLegScope.isBindingThirdLeg(ochLinkAttr);
                boolean otherTunnelDoing = supportTunnels.stream().anyMatch(
                        supportTunnel -> !supportTunnel.getTunnelId().getValue()
                                .equals(tunnelId.getValue())
                                && (ImplementState.Doimplementing.equals(
                                supportTunnel.getImplementState())
                                || ImplementState.Deimplementing.equals(
                                supportTunnel.getImplementState())));
                if (bindingThirdLeg && !otherTunnelDoing) {
                    // In batch implement after bind-third-leg, every tunnel under the OCH is Partial.
                    // The first tunnel that reaches this lock owns optical download and final cleanup.
                    RouteInfo ochrInfo = new RouteInfo();
                    ochrInfo.parse(ochLinkAttr.getExplictRoute().getRoute());
                    removeTransceiver(ochrInfo);
                    checkOchSiteLinkAtImpl(cache, ochrInfo);
                    rInfo.extend(ochrInfo);
                    return true;
                }

                log.debug("Implement tunnel retry checking och, ochOpticXcAllImpleted: {}",
                        ochOpticXcAllImpleted);
                // TunnelImplementSync serializes same-OCH implement until the owner
                // finishes. If the OCH is still incomplete, only that owner may retry
                // OCH optical resources while other same-OCH tunnels are non-Allocate.
                boolean ochNeedRetry = !ochLinkAttr.getImplementState().equals(ImplementState.Implement)
                        || !ochOpticXcAllImpleted;
                if (ochNeedRetry) {
                    RouteInfo ochrInfo = new RouteInfo();
                    ochrInfo.parse(ochLinkAttr.getExplictRoute().getRoute());
                    removeTransceiver(ochrInfo);

                    // the ochLink still in partail should write2Ne again
                    // thus extend it's route.
                    checkOchSiteLinkAtImpl(cache, ochrInfo);
                    rInfo.extend(ochrInfo);
                    return true;
                }

                return false;
            } else {
                //must be allocate
                //检查OCH Link 是否需要变为allocate, 依据是OCH link 上的所有tunnel 只有一条是Imple的且就是这条
                List<SupportedTunnel> supportedTunnelList = ochLink.getAugmentation(
                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class)
                        .getSupportedTunnel();
                List<String> supportedTunnelIds = supportedTunnelList.stream()
                        .map(x -> x.getTunnelRef().getValue())
                        .collect(Collectors.toList());

                log.debug("current och Link:{} support tunnel:{}", ochLink.getLinkId(),
                        supportedTunnelIds);

                List<Tunnel> supportTunnels = tunnelDao.listAllTunnelByIds(supportedTunnelIds);
                List<String> msgs = new ArrayList<>();
                supportTunnels.forEach(tunnel1 -> msgs.add(
                        String.format("%s, %s", tunnel1.getImplementState(),
                                tunnel1.getFriendlyName())));
                log.debug("\n\ntunnels {}\n", String.join("\n", msgs));

                boolean hasOther = supportTunnels.stream().anyMatch(
                        supportTunnel ->
                                (supportTunnel.getImplementState().equals(ImplementState.Implement)
                                        ||
                                        supportTunnel.getImplementState()
                                                .equals(ImplementState.PartialImplement) ||
                                        supportTunnel.getImplementState()
                                                .equals(ImplementState.Doimplementing))
                                        && !supportTunnel.getTunnelId().getValue()
                                        .equals(tunnelId.getValue()));

                log.debug("current och Link has other supported impl/partialImpl tunnel {}",
                        hasOther);

                if (!hasOther) {
                    //the OCH link should deImpl too. but siteLink keep implement
                    log.debug("has other tunnels in working");
                    return true;
                } else {
                    //has other tunnel still in working, do nothing
                    return false;
                }
            }
        } catch (CommonException ce) {
            log.error("CommonException occurred in checkActionOnOchLink: {}", ce.getMessage(), ce);
            throw ce; // Re-throw the exception for upper layers to handle
        } catch (Exception e) {
            log.error("Unexpected error in checkActionOnOchLink: {}", e.getMessage(), e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(),
                    e);
        }
    }

    private long getWorkingTunnelNumber(Link ochLink) {
        List<SupportedTunnel> supportedTunnelList = ochLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class)
                .getSupportedTunnel();
        List<String> supportedTunnelIds = supportedTunnelList.stream()
                .map(x -> x.getTunnelRef().getValue())
                .collect(Collectors.toList());

        log.debug("check working tunnel och Link:{} support tunnel:{}", ochLink.getLinkId(),
                supportedTunnelIds);

        List<Tunnel> supportTunnels = tunnelDao.listAllTunnelByIds(supportedTunnelIds);
        supportTunnels.forEach(
                tunnel -> log.debug("all tunnels:\n{} {}", tunnel.getImplementState(),
                        tunnel.getFriendlyName()));

        return supportTunnels.stream().filter(supportTunnel ->
                        supportTunnel.getImplementState().equals(ImplementState.Implement) ||
                                supportTunnel.getImplementState().equals(ImplementState.PartialImplement))
                .count();
    }


    //有MPO存在的情况下一个MPO端口上带了多个OCH，需要检查这个是不是MPO上的最后一个OCH，
    //如何还有其他OCH， 只清除L1/M?D? L--M?D? 的内部连接， ochXC,
    //ROADM 的OCH一样， WSS link 上面应该是最后一个OCH 才需要处理
    private void checkPhysicalLinkUsage(RouteInfo ochrInfo) {
        String ochLinkId = tunnel.getSupportingLink().get(0).getLinkRef().getValue();
        List<String> multiUserLinkId = ochrInfo.getPhyLinkIdList().stream().filter(linkId -> {
            long otherNonAllocateOchCount = ochLinkDao.countBySupportingLinkRefAndNotAllocateExcludeOchLink(
                    linkId, ochLinkId);
            return SharedPhysicalLinkUsage.shouldSkipOnDeimplement(linkId, otherNonAllocateOchCount);
        }).collect(Collectors.toList());

        for (String linkId : multiUserLinkId) {
            restoreSkippedSharedPhyLink(linkId);
            ochrInfo.getPhyLinkIdList().removeIf(x -> x.equals(linkId));

            String aTp = PhysicalLinkIdNamingRule.getTpAId(linkId);
            String zTp = PhysicalLinkIdNamingRule.getTpZId(linkId);

            ochrInfo.getTpIdList().removeIf(x -> x.equals(aTp));
            ochrInfo.getTpIdList().removeIf(x -> x.equals(zTp));

            String aEq = PhysicalTpIdNamingRule.getEquipId(aTp);
            String zEq = PhysicalTpIdNamingRule.getEquipId(zTp);

            ochrInfo.getEqIdList().removeIf(x -> x.equals(aEq));
            ochrInfo.getEqIdList().removeIf(x -> x.equals(zEq));
        }
    }

    private void restoreSkippedSharedPhyLink(String linkId) {
        // updateTunnelImplementState has already exposed this route as Deimplementing.
        // If the shared WSS/OMS/MPO link is still used by another OCH, this action will
        // not download it, so restore the pre-action link/node state before it leaves rInfo.
        phyLinkDao.updateLinkImplementState(linkId, ImplementState.Implement, AdminStatus.Up);
        phyNodeDao.updateConfigNodeImplState(PhysicalLinkIdNamingRule.getNodeAId(linkId),
                ImplementState.Implement, AdminStatus.Up);
        phyNodeDao.updateConfigNodeImplState(PhysicalLinkIdNamingRule.getNodeZId(linkId),
                ImplementState.Implement, AdminStatus.Up);
    }

    private void checkActionOnSiteLinkAtImpl(ChangedObject cache, String siteLinkId) {
        Link siteLink = cache.getChangedSiteLink(siteLinkId);
        Site siteLinkAttr = siteLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                .getSite();

        if (siteLinkAttr.getImplementState().equals(ImplementState.Allocate)) {
            //如果复用段没有impl, 直接报错
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "related sitelink must working in implement");
        }
    }

    private void checkOchSiteLinkAtImpl(ChangedObject cache, RouteInfo ochRInfo) {
        ArrayList<String> svrLinkIds = new ArrayList<>(
                ochRInfo.getLogicServerLinkIdList());
        for (String logicSvrLinkId : svrLinkIds) {
            if (SiteLinkIdNamingRule.isSiteLink(logicSvrLinkId)) {
                checkActionOnSiteLinkAtImpl(cache, logicSvrLinkId);
            }
        }
    }

    private boolean allOpticalXcAllImpleted(ChangedObject cache, Och ochLinkAttr) {
        List<CrossConnectionAttributes> xcList = RouteExtractor.extractorXc(
                ochLinkAttr.getExplictRoute().getRoute());
        List<CrossConnectionAttributes> wssXcList = xcList.stream()
                .filter(x -> x.getWssChannel() != null)
                .collect(Collectors.toList());

        boolean allImplemented = wssXcList.stream().allMatch(routeXc -> {
            String routeXcId = routeXc.getCrossConnectionId().getValue();
            String nodeId = PhysicalXcIdNamingRule.getNodeId(routeXcId);
            Node node = cache.getChangedPhyNode(nodeId);
            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

            CrossConnections nodeXc = nodeAttr.getCrossConnections().stream()
                    .filter(xc -> xc.getCrossConnectionId().getValue().equals(routeXcId))
                    .findAny().orElse(null);

            if (nodeXc == null) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "the XC cannot be found in node " + routeXcId);
            }
            return ImplementState.Implement.equals(nodeXc.getImplementState());
        });

        return allImplemented;
    }

    private boolean allOpticalXcAllAllocate(ChangedObject cache, Och ochLinkAttr) {
        List<CrossConnectionAttributes> xcList = RouteExtractor.extractorXc(
                ochLinkAttr.getExplictRoute().getRoute());
        List<CrossConnectionAttributes> wssXcList = xcList.stream()
                .filter(x -> x.getWssChannel() != null)
                .collect(Collectors.toList());

        boolean allAllocate = wssXcList.stream().allMatch(routeXc -> {
            String routeXcId = routeXc.getCrossConnectionId().getValue();
            String nodeId = PhysicalXcIdNamingRule.getNodeId(routeXcId);
            Node node = cache.getChangedPhyNode(nodeId);
            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

            CrossConnections nodeXc = nodeAttr.getCrossConnections().stream()
                    .filter(xc -> xc.getCrossConnectionId().getValue().equals(routeXcId))
                    .findAny().orElse(null);

            if (nodeXc == null) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "the XC cannot be found in node " + routeXcId);
            }
            return ImplementState.Allocate.equals(nodeXc.getImplementState());
        });

        return allAllocate;
    }


    private void updateUnImplementedElectricResourceOverOchLink(ChangedObject cache,
            Och ochLinkAttr, RouteInfo electricRInfo) {
        RouteInfo ochRouteInfo = new RouteInfo();
        ochRouteInfo.parse(ochLinkAttr.getExplictRoute().getRoute());
        removeTransceiver(ochRouteInfo);

        List<String> tdNodeList = new ArrayList<>();
        ochRouteInfo.getNodeIdList().stream().forEach(nodeId -> {
            Node node = cache.getChangedPhyNode(nodeId);
            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

            if (nodeAttr.getNodeType().equals(NodeType.TD)) {
                tdNodeList.add(nodeId);
            } else {
                //this is OD, unset it
                cache.unsetPhyNode(nodeId);
            }
        });

        List<String> electricXcOnOch = ochRouteInfo.getXcIdList().stream()
                .filter(id -> tdNodeList.contains(PhysicalXcIdNamingRule.getNodeId(id)))
                .collect(Collectors.toList());
        Set<String> xcSet = new HashSet<>(electricRInfo.getXcIdList());
        xcSet.addAll(electricXcOnOch);
        electricRInfo.getXcIdList().clear();
        electricRInfo.getXcIdList().addAll(xcSet);

        List<String> electricEqOnOch = ochRouteInfo.getEqIdList().stream()
                .filter(id -> tdNodeList.contains(PhysicalEqpIdNamingRule.getNodeId(id)))
                .collect(Collectors.toList());
        Set<String> eqSet = new HashSet<>(electricRInfo.getEqIdList());
        eqSet.addAll(electricEqOnOch);
        electricRInfo.getEqIdList().clear();
        electricRInfo.getEqIdList().addAll(eqSet);

        List<String> electricTpOnOch = ochRouteInfo.getTpIdList().stream()
                .filter(id -> tdNodeList.contains(PhysicalTpIdNamingRule.getNodeId(id)))
                .collect(Collectors.toList());
        Set<String> tpSet = new HashSet<>(electricRInfo.getTpIdList());
        tpSet.addAll(electricTpOnOch);
        electricRInfo.getTpIdList().clear();
        electricRInfo.getTpIdList().addAll(tpSet);

        List<String> eletricLinksOnOch = ochRouteInfo.getPhyLinkIdList().stream()
                .filter(id -> tdNodeList.stream().anyMatch(id::contains))
                .collect(Collectors.toList());
        Set<String> ilSet = new HashSet<>(electricRInfo.getPhyLinkIdList());
        ilSet.addAll(eletricLinksOnOch);
        electricRInfo.getPhyLinkIdList().clear();
        electricRInfo.getPhyLinkIdList().addAll(ilSet);

        Set<String> nodeIds = electricRInfo.getTpIdList().stream()
                .map(tpId->PhysicalTpIdNamingRule.getNodeId(tpId))
                .collect(Collectors.toSet());
        electricRInfo.getNodeIdList().clear();
        electricRInfo.getNodeIdList().addAll(nodeIds);
    }

    /**
     * 由于涉及到ASE 需要lock siteLink and related node's
     *
     * @param
     * @throws CommonException
     */
    private synchronized AbstractResourceLock lockResource(RouteInfo rInfo, boolean includeTunnel) {
        AbstractResourceLock locker = newResourceLock();

        if (includeTunnel) {
            locker.addResource(tunnel.getTunnelId().getValue());  //tunnel 电层 没有ochLink
        }
        rInfo.getNodeIdList().forEach(locker::addResource);
        rInfo.getLogicServerLinkIdList().forEach(locker::addResource);

        locker.getLock();
        return locker;
    }

    private synchronized AbstractResourceLock lockResourceWithAdditional(RouteInfo rInfo,
            boolean includeTunnel, Collection<String> additionalResourceIds) {
        AbstractResourceLock locker = newResourceLock();

        if (includeTunnel) {
            locker.addResource(tunnel.getTunnelId().getValue());
        }
        rInfo.getNodeIdList().forEach(locker::addResource);
        rInfo.getLogicServerLinkIdList().forEach(locker::addResource);
        additionalResourceIds.forEach(locker::addResource);

        locker.getLock();
        return locker;
    }

    private synchronized AbstractResourceLock lockExactResource(Collection<String> resourceIds) {
        AbstractResourceLock locker = newResourceLock();

        // Follower tunnel electric actions do not own the whole node/OCH; lock only
        // the resources whose admin/implement leaves will be changed.
        resourceIds.forEach(locker::addResource);

        locker.getLock();
        return locker;
    }

    private List<ReentrantLock> lockLocalSiteLinks(List<String> siteLinkIds) {
        List<ReentrantLock> locks = siteLinkIds.stream()
                .distinct()
                .sorted()
                .map(siteLinkId -> siteLinkMutexMap.computeIfAbsent(siteLinkId,
                        key -> new ReentrantLock()))
                .collect(Collectors.toList());

        locks.forEach(ReentrantLock::lock);
        return locks;
    }

    private void unlockLocalSiteLinks(List<ReentrantLock> locks) {
        for (int i = locks.size() - 1; i >= 0; i--) {
            locks.get(i).unlock();
        }
    }

    private AbstractResourceLock lockNodeResource(List<String> nodeIdList) {
        AbstractResourceLock locker = newResourceLock();

        nodeIdList.forEach(locker::addResource);

        locker.getLock();
        return locker;
    }

    private AbstractResourceLock newResourceLock() {
        return resourceLockFactory.get();
    }

}

package net.flex.dci.otn.controller.implement.common.ase;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.OchLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.implement.common.impl.ImplActionType;
import net.flex.dci.otn.controller.implement.common.impl.LinkImplementState;
import net.flex.dci.otn.controller.implement.common.impl.OcmUpdator;
import net.flex.dci.otn.controller.implement.common.lifecycle.LifeCycleSevice;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Third;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
public class TunnelImplementInSequence {

    private final static TunnelDao tunnelDao = SpringBeanFinder.getBean(TunnelDao.class);
    private Map<Tunnel, RouteInfo> tunnelMap;
    private LifeCycleSevice lifeService;
    private ChangedObject changedObject;
    private final static SiteLinkDao siteLinkDao = SpringBeanFinder.getBean(SiteLinkDao.class);

    public TunnelImplementInSequence(ChangedObject changedObject, LifeCycleSevice lifeService) {
        this.changedObject = changedObject;
        this.tunnelMap = new HashMap<>();
        this.lifeService = lifeService;
    }

    public void addTunnel(Tunnel tunnel, RouteInfo rInfo) {
        tunnelMap.put(tunnel, rInfo);
    }

    public void start(boolean toAllocate) {
        try {
            executeInSequenceAsync(toAllocate);
        } catch (Exception e) {
            String msg = String.format("prcessing tunnel error %s. ", e.getMessage());
            log.error(msg, e);
            throw e;
        }
    }

    //变成顺序可控的执行方式，执行完tunnel deImpl 后 需要继续ASE的插入工作
    private void executeInSequenceAsync(boolean toAllocate) {

        for (Tunnel tunnel : tunnelMap.keySet()) {
            processSingleTunnel(tunnel, tunnelMap.get(tunnel), toAllocate);
        }
    }

    private void processSingleTunnel(Tunnel tunnel, RouteInfo rInfo, boolean toAllocate) {
        String friendlyName = tunnel.getFriendlyName();

        log.info("Starting make tunnel to : {} {}", toAllocate ? "Allocate -> " : "Implement -> ",
                friendlyName);

        if (toAllocate) {
            String ochLinkId = rInfo.getLogicServerLinkIdList().stream()
                    .filter(sId -> OchLinkIdNamingRule.isOchLink(sId))
                    .findAny().orElse(null);
            if (ochLinkId != null) {
                Link ochLink = changedObject.getChangedOchLink(ochLinkId);
                AseInjectModeUpdator modeUpdator = new AseInjectModeUpdator(changedObject);
                modeUpdator.setMCSrc2DstPowerControlModel(ochLink, AseInjectModeUpdator.MC_POWER_CONTROL_MODEL_MANUAL);
            }
        }
        //checking rInfo xcList, does it include wssXC, if yes, update ocm required
        updateOcm(changedObject, tunnel, rInfo, toAllocate);

        CompletableFuture<Void> future = new CompletableFuture<>();
        LinkImplementState implementor = new LinkImplementState(LinkImplementState.LinkType.Tunnel,
                tunnel.getTunnelId().getValue(),
                friendlyName,
                rInfo,
                (success, error) -> {
                    if (success) {
                        log.info("Action on tunnel done successfully: {} {}",
                                toAllocate ? "Allocate -> " : "Implement -> ", friendlyName);
                        future.complete(null); // 标记成功
                    } else {
                        String msg = String.format("Action on tunnel failure: %s %s. (%s)",
                                toAllocate ? "Allocate -> " : "Implement -> ", friendlyName,
                                error.getMessage());
                        log.error(msg, error);
                        future.completeExceptionally(error);
                    }
                });
        try {
            if (toAllocate) {
                implementor.setActionType(ImplActionType.Deimplement)
                        .changeAs(changedObject, lifeService);
            } else {
                // Dummy OCH is the persisted state indicating that this SiteLink uses ASE.
                if (isAseInjectedSiteLink(tunnel)) {
                    removeSpecAttrOnCL(changedObject, rInfo, tunnel);
                }
                implementor.setActionType(ImplActionType.Implement)
                        .changeAs(changedObject, lifeService);
            }

            future.get();
        } catch (InterruptedException e) {
            log.error("Thread was interrupted", e);
            Thread.currentThread().interrupt(); // Restore interrupt status
            throw new RuntimeException("Thread was interrupted during action on NE");
        } catch (ExecutionException e) {
            log.error("Exception while waiting for NE action to complete", e.getCause());
            throw new RuntimeException(e.getCause().getMessage(), e.getCause());
        }
    }

    private boolean isAseInjectedSiteLink(Tunnel tunnel) {
        String ochLinkId = tunnel.getSupportingLink().get(0).getLinkRef().getValue();
        Link ochLink = changedObject.getChangedOchLink(ochLinkId);
        String siteLinkId = ochLink.getSupportingLink().stream()
                .map(x->x.getLinkRef().getValue())
                .filter(SiteLinkIdNamingRule::isSiteLink)
                .findAny().orElseThrow(() -> new RuntimeException("cannot find out siteLink in supporting link of ochLink " + ochLinkId));

        Link siteLink = changedObject.getChangedSiteLink(siteLinkId);
        Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();
        return siteLinkAttr.getDummyLink() != null && !siteLinkAttr.getDummyLink().isEmpty();
    }

    private void removeSpecAttrOnCL(ChangedObject changedObject, RouteInfo rInfo, Tunnel tunnel) {
        //如果是电层， rInfo.nodeList 不能有光层

        boolean isElectric = rInfo.getNodeIdList().stream().anyMatch(nodeId->{
            Node node = changedObject.getChangedPhyNode(nodeId);
            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
            return nodeAttr.getNodeType().equals(NodeType.TD);
        });
        if (! isElectric) {
            log.debug("this is optical level");
            return;
        }

        rInfo.getTpIdList().stream().forEach(tpId-> {
            String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
            Node node = changedObject.getChangedPhyNode(nodeId);

            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
            if (nodeAttr.getNodeType().equals(NodeType.TD)) {
                TerminationPoint tp = node.getTerminationPoint().stream()
                        .filter(x -> x.getTpId().getValue().equals(tpId))
                        .findAny().orElse(null);
                if (tp == null) {
                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "cannot find out tpId in related node" + tpId);
                }
                if (tp.getAugmentation(TerminationPoint1.class).getPhysical().getOtuLine() != null) {
                    Node newNode = updateLPort(node, tpId);
                    changedObject.addChangedPhyNode(newNode);
                }
            }
        });
    }

    /**
     * make tp adminDown and remove tx-laser attr
     *
     * @param node
     * @param tpId
     * @return
     */
    private Node updateLPort(Node node, String tpId) {
        //现阶段 C， L 口都是一样的，所以直接调用同样的方法，后续如果有差异化再调整
        return updateCPort(node, tpId);
    }

    /**
     * make tp adminDown and remove tx-laser attr
     *
     * @param node
     * @param tpId
     * @return
     */
    private Node updateCPort(Node node, String tpId) {
        TerminationPoint tp = node.getTerminationPoint().stream().filter(x -> x.getTpId().getValue().equals(tpId))
                .findAny().orElseThrow(() -> new RuntimeException("cannot find out tpId in related node" + tpId));

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical tpAttr =
                tp.getAugmentation(TerminationPoint1.class).getPhysical();

        List<Property> newList = tpAttr.getProperties().getProperty().stream()
                .filter(x -> !x.getName().contains("tx_laser"))
                .collect(Collectors.toList());

        TerminationPoint newTp = new TerminationPointBuilder(tp).addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                .setPhysical(new PhysicalBuilder(tpAttr)
                        .setAdminState(AdminStatus.Down)
                        .setProperties(new PropertiesBuilder().setProperty(newList).build())
                        .build())
                .build())
                .build();

        return updatedNode(node, newTp);
    }

    private Node updatedNode(Node node, TerminationPoint newTp) {
        List<TerminationPoint> newTpList = node.getTerminationPoint().stream()
                .map(x -> {
                    if (x.getTpId().getValue().equals(newTp.getTpId().getValue())) {
                        return newTp;
                    } else {
                        return x;
                    }
                }).collect(Collectors.toList());

        return new NodeBuilder(node).setTerminationPoint(newTpList).build();
    }

    private void updateOcm(ChangedObject changedObject, Tunnel tunnel, RouteInfo rInfo, boolean toAllocate) {
        if (!ocmUpdateRequired(tunnel, rInfo)) {
            return;
        }

        List<String> ochLinkIdList = tunnel.getSupportingLink().stream()
                .map(sl -> sl.getLinkRef().getValue()).collect(Collectors.toList());
        OchLinkDao ochLinkDao = SpringBeanFinder.getBean(OchLinkDao.class);

        //支撑tunnel的只有一个ochLink
        Link ochLink = ochLinkDao.getOchLinkByLinkId(ochLinkIdList.get(0));
        OcmUpdator ocmUpdator = new OcmUpdator(changedObject, ochLink);  //TODO 这个地方不用给他changedObject, 否则计算时用到所有的OCHlink, siteLink 都需要存盘。
        if (toAllocate) {
            ocmUpdator.remove(ochLink);
        } else {
            ocmUpdator.insert(ochLink);
        }

        List<Node> impactedOcmNodes = rInfoRequiredOnly(ocmUpdator, rInfo);

        //关键点：OCH 路由中没有包含ILA， 因为ILA没有XC，所以这个地方需要把ocmUpdator提前出来的ILA网元的OCM也需要加入
        // Binding third leg must only keep OCM nodes on the new affected siteLink.
        Set<String> nodeIdSet = new HashSet<>(rInfo.getNodeIdList());
        nodeIdSet.addAll(impactedOcmNodes
                .stream().map(x->x.getNodeId().getValue())
                .collect(Collectors.toList()));
        rInfo.setNodeIdList(new ArrayList<>(nodeIdSet));

        impactedOcmNodes.forEach(changedObject::addChangedPhyNode);

    }

    /**
     * 并不是每次都需要把所有涉及的光层网元都下一次OCM值，只处理rInfo XClist 相关的复用段
     * 这个方法对于原来只有两条腿，现在变成3条腿的OCH 改造有用
     *
     * 然后基于impacted 修改changedOjbect
     * @param updator
     * @param rInfo
     * @return
     */
    private List<Node> rInfoRequiredOnly(OcmUpdator updator, RouteInfo rInfo) {
        List<Node> impactedNodes = new ArrayList<>();
        Set<Link> impactedSiteLinks = new HashSet<>();

        Set<String> routeSiteLinkIds = rInfo.getLogicServerLinkIdList().stream()
                .filter(SiteLinkIdNamingRule::isSiteLink)
                .collect(Collectors.toSet());
        impactedSiteLinks.addAll(updator.getRelatedSiteLinks().stream()
                .filter(siteLink -> routeSiteLinkIds.contains(siteLink.getLinkId().getValue()))
                .collect(Collectors.toSet()));

        if (impactedSiteLinks.isEmpty()) {
            for (String nodeId : rInfo.getNodeIdList()) {
                updator.getRelatedSiteLinks().stream()
                        .filter(siteLink -> siteLink.getLinkId().getValue().contains(nodeId))
                        .findAny()
                        .ifPresent(impactedSiteLinks::add);
            }
        }

        impactedSiteLinks.stream().forEach(siteLink-> {
            RouteInfo siteLinkRouteInfo = new RouteInfo();
            Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();

            siteLinkRouteInfo.parse(siteLinkAttr.getExplictRoute().getRoute());

            for (String nodeId : siteLinkRouteInfo.getNodeIdList()) {
                Node node = updator.getOcmNodeMap().get(nodeId);
                if (node != null) {
                    impactedNodes.add(node);
                }
            }
        });

        return impactedNodes;
    }

    /**
     * base on rInfo.xcList, checking does frequency xc existed.
     * @param rInfo
     * @return
     */
    private boolean ocmUpdateRequired(Tunnel tunnel, RouteInfo rInfo) {
        for(String xcId : rInfo.getXcIdList()) {
            String nodeId = PhysicalXcIdNamingRule.getNodeId(xcId);
            Node node = changedObject.getChangedPhyNode(nodeId);
            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

            CrossConnections xc = nodeAttr.getCrossConnections().stream()
                    .filter(x -> x.getCrossConnectionId().getValue().equals(xcId))
                    .findAny().orElse(null);

            if (xc == null) {
                log.error("cannot find out xcId in related node " + xcId);
                xc = appendThisXc(tunnel, node, xcId);
            }
            if (xc.getWssChannel() != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * 现场bug的弥补， node 没有XC，但是ochLink 路由中有，这里把它补上
     * @param tunnel
     * @param node
     * @param xcId
     */
    private CrossConnections appendThisXc(Tunnel tunnel, Node node, String xcId) {
        CrossConnectionAttributes xc = findoutXcInOchRoute(tunnel, xcId);
        CrossConnections newXc = new CrossConnectionsBuilder(xc).build();

        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        List<CrossConnections> xcs = nodeAttr.getCrossConnections();
        xcs.add(newXc);

        Node newNode = new NodeBuilder(node).addAugmentation(Node1.class, new Node1Builder()
                .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder(nodeAttr)
                        .setCrossConnections(xcs)
                        .build())
                .build())
                .build();

        changedObject.addChangedPhyNode(newNode);

        return newXc;
    }

    private CrossConnectionAttributes findoutXcInOchRoute(Tunnel tunnel, String xcId) {
        String ochLinkId = tunnel.getSupportingLink().get(0).getLinkRef().getValue();
        Link ochLink = changedObject.getChangedOchLink(ochLinkId);
        Och ochLinkAttr = ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class).getOch();


        return fetchXcOnRoute(ochLinkAttr.getExplictRoute().getRoute(), xcId);
    }

    private CrossConnectionAttributes fetchXcOnRoute(List<Route> routes, String xcId) {
        List<CrossConnectionAttributes> xcsInRoute = new ArrayList<>();
        for (Route route : routes) {
            xcsInRoute.addAll(route.getPrimary().getCrossConnections());
            if (route.getSecondary() != null) {
                xcsInRoute.addAll(route.getSecondary().getCrossConnections());
            }
            if (route.getThird() != null) {
                for (Third t : route.getThird()) {
                    xcsInRoute.addAll(t.getCrossConnections());
                }
            }
        }

        CrossConnectionAttributes xc = xcsInRoute.stream().filter(x -> x.getCrossConnectionId().getValue().equals(xcId)).findAny().orElse(null);
        if (xc == null) {
            log.error("this is impossible, the xc should be in och route, because it is coming from route");
            String errMsg = String.format("OCH Route xc {} cannot find again", xcId);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, errMsg);
        }
        return xc;
    }

    private RouteInfo extensionRouteOnSiteLink(RouteInfo rInfo) {
        for (String svrLink : rInfo.getLogicServerLinkIdList()) {
            //tunnel svrLink is OCH
            Link ochLink = changedObject.getChangedOchLink(svrLink);
            changedObject.unsetOchLink(svrLink);
            for (SupportingLink sl : ochLink.getSupportingLink()) {
                String linkId = sl.getLinkRef().getValue();
                if (SiteLinkIdNamingRule.isSiteLink(linkId)) {
                    Link siteLink = changedObject.getChangedSiteLink(linkId);
                    changedObject.unsetSiteLink(linkId);
                    Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();

                    RouteInfo siteLinkRouteInfo = new RouteInfo();
                    siteLinkRouteInfo.parse(siteLinkAttr.getExplictRoute().getRoute());
                    rInfo.extend(siteLinkRouteInfo);
                }
            }
        }
        return rInfo;
    }

    private boolean isRegType(Node node, RouteInfo rInfo) {
        if (node.getNodeId().getValue().equals(rInfo.getNodeIdList().get(0)) ||
                node.getNodeId().getValue()
                        .equals(rInfo.getNodeIdList().get(rInfo.getNodeIdList().size() - 1))) {
            return false;
        } else {
            return true;
        }
    }

}

package net.flex.dci.otn.controller.allocate.link.site.removenode;

import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.ne.LinkRepo;
import net.flex.dci.otn.controller.allocate.link.common.Route;
import net.flex.dci.otn.controller.allocate.link.phy.PhyLinkUtil;
import net.flex.dci.otn.controller.allocate.link.view.ViewLink;
import net.flex.dci.otn.controller.allocate.node.phy.PhyNodeUtil;
import net.flex.dci.otn.controller.allocate.node.site.SiteNodeCorrelateResource;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjectsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObjectKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Primary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.PrimaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Secondary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.SecondaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Third;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.ThirdBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

class RemoveNodeInSiteLinkOperation {
    private final RemoveNodeInSiteLinkContext context;
    private final ChangedObject changedObject;

    RemoveNodeInSiteLinkOperation(RemoveNodeInSiteLinkContext context, ChangedObject changedObject) {
        this.context = context;
        this.changedObject = changedObject;
    }

    void execute() {
        // remove-node-in-site-link 是 insert 的逆操作：先识别 N 两侧 split OTS，再删除 split 资源并合并回 A--B。
        findSplitOtsLinks();
        removeSplitOtsLinks();
        createMergedOtsLink();
        removeInsertedNodeFromSiteNode();
        updateSiteLinkRoute();
        updateOchRoutes();
        changedObject.addRemovedPhyNode(context.nodeId);
    }

    private void findSplitOtsLinks() {
        List<Link> splitLinks = new ArrayList<>();
        if (context.siteLink.getSupportingLink() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "siteLink has no supporting-link: " + context.siteLinkId);
        }
        for (SupportingLink supportingLink : context.siteLink.getSupportingLink()) {
            Link phyLink = changedObject.getChangedPhyLink(supportingLink.getLinkRef().getValue());
            if (phyLink == null || !isOtsLink(phyLink) || !isOnNode(phyLink, context.nodeId)) {
                continue;
            }
            splitLinks.add(phyLink);
        }
        if (splitLinks.size() != 2) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "remove node must find exactly two split OTS links on siteLink, current count is " + splitLinks.size());
        }

        context.leftSplitOtsLink = splitLinks.get(0);
        context.rightSplitOtsLink = splitLinks.get(1);
    }

    private boolean isOtsLink(Link link) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1 linkAug =
                link.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class);
        return linkAug != null && linkAug.getPhysical() != null
                && LinkType.OtsLink.equals(linkAug.getPhysical().getLinkType());
    }

    private boolean isOnNode(Link link, String nodeId) {
        return nodeId.equals(link.getSource().getSourceNode().getValue())
                || nodeId.equals(link.getDestination().getDestNode().getValue());
    }

    private void removeSplitOtsLinks() {
        PhyLinkUtil phyLinkUtil = new PhyLinkUtil(changedObject);
        phyLinkUtil.removePhyLinkKeepXc(context.leftSplitOtsLink.getLinkId().getValue(), context.planeId);
        phyLinkUtil.removePhyLinkKeepXc(context.rightSplitOtsLink.getLinkId().getValue(), context.planeId);
    }

    private void createMergedOtsLink() {
        String sourceTp = externalTp(context.leftSplitOtsLink);
        String destTp = externalTp(context.rightSplitOtsLink);
        try {
            Link link = new LinkRepo().createLink(sourceTp, destTp, LinkType.OtsLink);
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical splitPhyAttr =
                    context.leftSplitOtsLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class).getPhysical();
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical newPhyAttr =
                    link.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class).getPhysical();
            Link mergedLink = new LinkBuilder(link)
                    .addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class,
                            new Link1Builder()
                                    .setPhysical(new PhysicalBuilder(newPhyAttr)
                                            .setProvider(splitPhyAttr.getProvider())
                                            .setProperties(splitPhyAttr.getProperties())
                                            .setPlaneName(splitPhyAttr.getPlaneName())
                                            .setPlaneId(splitPhyAttr.getPlaneId())
                                            // 合并后的 OTS 重新承载原 siteLink，需要恢复 phyLink -> siteLink 的反向 supporting 关系。
                                            .setSupportedLink(buildPhySupportedLinks())
                                            .setImplementState(ImplementState.Allocate)
                                            .build())
                                    .build())
                    .build();

            Site siteAttr = context.siteLink.getAugmentation(Link1.class).getSite();
            new PhyLinkUtil(changedObject).addPhyLink(mergedLink, siteAttr.getPlaneName(), siteAttr.getPlaneId());
            context.mergedOtsLink = changedObject.getChangedPhyLink(mergedLink.getLinkId().getValue());
            new ViewLink(changedObject, siteAttr.getPlaneId()).create(context.mergedOtsLink);
            updateMergedOtsInternalLinks();
        } catch (NeDesignerException e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "neDesigner error when create merged OTS link: " + e.getMessage(), e);
        }
    }

    private String externalTp(Link link) {
        if (context.nodeId.equals(link.getSource().getSourceNode().getValue())) {
            return link.getDestination().getDestTp().getValue();
        }
        return link.getSource().getSourceTp().getValue();
    }

    private List<SupportedLink> buildPhySupportedLinks() {
        List<SupportedLink> supportedLinks = new ArrayList<>();
        supportedLinks.add(new SupportedLinkBuilder()
                .setTopologyRef(new TopologyId(TopoNameConstants.Site_Topo_Key))
                .setLinkRef(new LinkId(context.siteLinkId))
                .build());
        return supportedLinks;
    }

    private void updateMergedOtsInternalLinks() {
        LinkRepo linkRepo = new LinkRepo();
        Node sourceNode = changedObject.getChangedPhyNode(context.mergedOtsLink.getSource().getSourceNode().getValue());
        Node destNode = changedObject.getChangedPhyNode(context.mergedOtsLink.getDestination().getDestNode().getValue());
        InternalLinks sourceInternalLink = linkRepo.createInternalLink(sourceNode.getNodeId().getValue(), context.mergedOtsLink);
        InternalLinks destInternalLink = linkRepo.createInternalLink(destNode.getNodeId().getValue(), context.mergedOtsLink);

        // removeSplitOtsLinks 已清理 A/N/B 的旧 internal-link 和 TP 状态；这里只给 A/Z 恢复合并后 A--B 的 internal-link。
        changedObject.addChangedPhyNode(PhyNodeUtil.addInternalLinksAndBusyTps(sourceNode,
                Collections.singletonList(sourceInternalLink),
                Collections.singleton(context.mergedOtsLink.getSource().getSourceTp().getValue())));
        changedObject.addChangedPhyNode(PhyNodeUtil.addInternalLinksAndBusyTps(destNode,
                Collections.singletonList(destInternalLink),
                Collections.singleton(context.mergedOtsLink.getDestination().getDestTp().getValue())));
    }

    private void removeInsertedNodeFromSiteNode() {
        Node siteNode = changedObject.getChangedSiteNode(context.siteNodeId);
        if (siteNode == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "cannot find siteNode for removed node: " + context.siteNodeId);
        }
        SiteNodeCorrelateResource correlateResource = new SiteNodeCorrelateResource(siteNode);
        // 插入时为 N 单独创建 rack；逆操作只删除该 N 和空 rack，不修改其它 siteLink rack 命名。
        correlateResource.updateRack_remove(null, null, context.nodeId);
        changedObject.addChangedSiteNode(correlateResource.getSiteNode());
    }

    private void updateSiteLinkRoute() {
        Site oldSiteAttr = context.siteLink.getAugmentation(Link1.class).getSite();

        Link updatedSiteLink = new LinkBuilder(context.siteLink)
                .setSupportingLink(updateSiteSupportingLinks(context.siteLink.getSupportingLink()))
                .addAugmentation(Link1.class, new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder()
                        .setSite(new SiteBuilder(oldSiteAttr)
                                .setExplictRoute(updateSiteExplicitRoute(oldSiteAttr.getExplictRoute()))
                                .build())
                        .build())
                .build();
        // Preserve the snapshot for non-route diffs; the complete route is saved separately.
        changedObject.addChangedSiteLink(updatedSiteLink);
    }

    private List<SupportingLink> updateSiteSupportingLinks(List<SupportingLink> oldSupportingLinks) {
        List<SupportingLink> newSupportingLinks = new LinkedList<>();
        if (oldSupportingLinks != null) {
            for (SupportingLink supportingLink : oldSupportingLinks) {
                String linkId = supportingLink.getLinkRef().getValue();
                if (!isSplitLinkId(linkId)) {
                    newSupportingLinks.add(supportingLink);
                }
            }
        }
        newSupportingLinks.add(buildSupportingLink(context.mergedOtsLink.getLinkId().getValue()));
        return newSupportingLinks;
    }

    private SupportingLink buildSupportingLink(String linkId) {
        return new SupportingLinkBuilder()
                .setLinkRef(new LinkId(linkId))
                .setKey(new SupportingLinkKey(new LinkId(linkId)))
                .build();
    }

    private ExplictRoute updateSiteExplicitRoute(ExplictRoute oldRoute) {
        if (oldRoute == null || oldRoute.getRoute() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "siteLink has no explict route: " + context.siteLinkId);
        }
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route> newRoutes =
                new ArrayList<>();
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route route : oldRoute.getRoute()) {
            RouteBuilder routeBuilder = new RouteBuilder(route);
            if (route.getPrimary() != null) {
                routeBuilder.setPrimary(updatePrimaryRoute(route.getPrimary()));
            }
            if (route.getSecondary() != null) {
                routeBuilder.setSecondary(updateSecondaryRoute(route.getSecondary()));
            }
            if (route.getThird() != null) {
                routeBuilder.setThird(route.getThird().stream().map(this::updateThirdRoute).collect(Collectors.toList()));
            }
            newRoutes.add(routeBuilder.build());
        }
        return new ExplictRouteBuilder(oldRoute).setRoute(newRoutes).build();
    }

    private Primary updatePrimaryRoute(Primary oldPrimary) {
        if (!containsSplitOts(oldPrimary.getExplicitRouteObjects())) {
            return oldPrimary;
        }
        return new PrimaryBuilder(oldPrimary)
                .setExplicitRouteObjects(updateExplicitRouteObjects(oldPrimary.getExplicitRouteObjects()))
                .setCrossConnections(removeNodeRouteXcs(oldPrimary.getCrossConnections()))
                .build();
    }

    private Secondary updateSecondaryRoute(Secondary oldSecondary) {
        if (!containsSplitOts(oldSecondary.getExplicitRouteObjects())) {
            return oldSecondary;
        }
        return new SecondaryBuilder(oldSecondary)
                .setExplicitRouteObjects(updateExplicitRouteObjects(oldSecondary.getExplicitRouteObjects()))
                .setCrossConnections(removeNodeRouteXcs(oldSecondary.getCrossConnections()))
                .build();
    }

    private Third updateThirdRoute(Third oldThird) {
        if (!containsSplitOts(oldThird.getExplicitRouteObjects())) {
            return oldThird;
        }
        return new ThirdBuilder(oldThird)
                .setExplicitRouteObjects(updateExplicitRouteObjects(oldThird.getExplicitRouteObjects()))
                .setCrossConnections(removeNodeRouteXcs(oldThird.getCrossConnections()))
                .build();
    }

    private boolean containsSplitOts(List<ExplicitRouteObjects> eros) {
        if (eros == null) {
            return false;
        }
        for (ExplicitRouteObjects ero : eros) {
            if (ero.getPathRouteObject() == null) {
                continue;
            }
            for (PathRouteObject routeObject : ero.getPathRouteObject()) {
                if (isSplitLinkHop(routeObject)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<ExplicitRouteObjects> updateExplicitRouteObjects(List<ExplicitRouteObjects> oldEros) {
        if (oldEros == null) {
            return null;
        }
        List<ExplicitRouteObjects> newEros = new ArrayList<>();
        for (ExplicitRouteObjects ero : oldEros) {
            newEros.add(new ExplicitRouteObjectsBuilder(ero)
                    .setPathRouteObject(mergeSplitRouteObjects(ero.getPathRouteObject()))
                    .build());
        }
        return newEros;
    }

    private List<PathRouteObject> mergeSplitRouteObjects(List<PathRouteObject> oldObjects) {
        if (oldObjects == null) {
            return null;
        }
        List<PathRouteObject> newObjects = new ArrayList<>();
        long index = 1L;
        boolean merged = false;
        for (PathRouteObject oldObject : oldObjects) {
            if (isSplitLinkHop(oldObject)) {
                if (!merged) {
                    // 旧 siteLink route 是 A--N link, N WEST TP, N EAST TP, N--B link；逆操作压缩回一个 A--B link-hop。
                    newObjects.add(Route.getLinkPathRouteObject(phyTopologyId(),
                            new LinkId(context.mergedOtsLink.getLinkId().getValue()), index++));
                    merged = true;
                }
                continue;
            }
            if (isRemovedNodeTpHop(oldObject)) {
                continue;
            }
            newObjects.add(reindexPathRouteObject(oldObject, index++));
        }
        return newObjects;
    }

    private boolean isSplitLinkHop(PathRouteObject routeObject) {
        if (routeObject.getResourceType() instanceof org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link link =
                    (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) routeObject.getResourceType();
            return link.getLinkHop() != null && link.getLinkHop().getLinkRef() != null
                    && isSplitLinkId(link.getLinkHop().getLinkRef().getValue());
        }
        return false;
    }

    private boolean isRemovedNodeTpHop(PathRouteObject routeObject) {
        if (routeObject.getResourceType() instanceof org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp) {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp tp =
                    (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp) routeObject.getResourceType();
            return tp.getTpHop() != null && tp.getTpHop().getTpRef() != null
                    && context.nodeId.equals(PhysicalTpIdNamingRule.getNodeId(tp.getTpHop().getTpRef().getValue()));
        }
        return false;
    }

    private boolean isSplitLinkId(String linkId) {
        return context.leftSplitOtsLink.getLinkId().getValue().equals(linkId)
                || context.rightSplitOtsLink.getLinkId().getValue().equals(linkId);
    }

    private PathRouteObject reindexPathRouteObject(PathRouteObject oldObject, long index) {
        return new PathRouteObjectBuilder(oldObject)
                .setIndex(index)
                .setKey(new PathRouteObjectKey(index))
                .build();
    }

    private List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> removeNodeRouteXcs(
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> oldXcs) {
        if (oldXcs == null) {
            return null;
        }
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> newXcs =
                oldXcs.stream()
                        .filter(xc -> xc.getNodeRef() == null || !context.nodeId.equals(xc.getNodeRef().getValue()))
                        .collect(Collectors.toList());
        return reindexRouteXcs(newXcs);
    }

    private List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> reindexRouteXcs(
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> oldXcs) {
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> newXcs =
                new ArrayList<>();
        long sequence = 1L;
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections xc : oldXcs) {
            // 复制的交叉保留旧 key，必须同步更新，否则 build() 会用旧 key 恢复原编号。
            newXcs.add(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsBuilder(xc)
                    .setSequence(sequence)
                    .setKey(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsKey(sequence))
                    .build());
            sequence++;
        }
        return newXcs;
    }

    private TopologyId phyTopologyId() {
        return new TopologyId(TopoNameConstants.Phy_Topo_Key);
    }

    private void updateOchRoutes() {
        if (context.ochLinks == null || context.ochLinks.isEmpty()) {
            return;
        }
        for (Link ochLink : context.ochLinks) {
            changedObject.addChangedOchLink(removeNodeXcsFromOchRoute(ochLink));
        }
    }

    private Link removeNodeXcsFromOchRoute(Link ochLink) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1 oldOchAug =
                ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class);
        if (oldOchAug == null || oldOchAug.getOch() == null || oldOchAug.getOch().getExplictRoute() == null) {
            return ochLink;
        }

        ExplictRoute oldRoute = oldOchAug.getOch().getExplictRoute();
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route> newRoutes =
                new ArrayList<>();
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route route : oldRoute.getRoute()) {
            RouteBuilder routeBuilder = new RouteBuilder(route);
            if (route.getPrimary() != null) {
                routeBuilder.setPrimary(new PrimaryBuilder(route.getPrimary())
                        .setCrossConnections(removeNodeRouteXcs(route.getPrimary().getCrossConnections()))
                        .build());
            }
            if (route.getSecondary() != null) {
                routeBuilder.setSecondary(new SecondaryBuilder(route.getSecondary())
                        .setCrossConnections(removeNodeRouteXcs(route.getSecondary().getCrossConnections()))
                        .build());
            }
            if (route.getThird() != null) {
                routeBuilder.setThird(route.getThird().stream()
                        .map(third -> new ThirdBuilder(third)
                                .setCrossConnections(removeNodeRouteXcs(third.getCrossConnections()))
                                .build())
                        .collect(Collectors.toList()));
            }
            newRoutes.add(routeBuilder.build());
        }
        ExplictRoute updatedRoute = new ExplictRouteBuilder(oldRoute).setRoute(newRoutes).build();
        return new LinkBuilder(ochLink)
                .addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class,
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1Builder()
                                .setOch(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.OchBuilder(oldOchAug.getOch())
                                        .setExplictRoute(updatedRoute)
                                        .build())
                                .build())
                .build();
    }
}

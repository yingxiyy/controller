package net.flex.dci.otn.controller.nms.nms.component.retrieve.nodes;

import static net.flex.dci.otc.common.constants.Constants.POUND;
import static net.flex.dci.otn.controller.nms.utils.Constants.OCH_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_TOPO_KEY;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.enums.FilterQuerySelector.FilterLogicalOp;
import net.flex.dci.otc.mongo.dto.FilterItem;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.AbstractNMSRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.dto.FilterDto;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import net.flex.dci.otn.controller.nms.nms.enums.NMSConvertType;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Third;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.link.LinkHop;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 2022/3/12 23:08
 */
@Slf4j
public abstract class AbstractNodeRetrieveHandler extends AbstractNMSRetrieveHandler {

    public AbstractNodeRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

    protected List<String> getPhyLinkRefNode(List<Link> phyLinks) {
        List<String> refNodeIds = new ArrayList<>();
        for (Link phyLink : phyLinks) {
            refNodeIds.addAll(getPhyLinkRefNode(phyLink));
        }
        return refNodeIds;
    }

    protected List<FilterItem> buildFilterItemsWithPlane(RetrieveTopologyDto retrieveTopologyDto,
            NMSConvertType convertType) {
        FilterDto filterDto = getFilterItems(retrieveTopologyDto);
        List<FilterItem> filterItems = filterItemHelper.getFilterItems(filterDto, convertType);

        FilterItem planeFilterItems = getPlaneFilterItem(retrieveTopologyDto, convertType);
        if (planeFilterItems != null) {
            if (!filterItems.isEmpty()) {
                planeFilterItems.setLogicalOp(FilterLogicalOp.AND);
            }
            filterItems.add(planeFilterItems);
        }

        return filterItems;
    }


    private List<String> getPhyLinkRefNode(Link phyLink) {
        List<String> phyNodeIds = new ArrayList<>();
        phyNodeIds.add(phyLink.getDestination().getDestNode().getValue());
        phyNodeIds.add(phyLink.getSource().getSourceNode().getValue());
        return phyNodeIds;
    }

    protected List<String> getRouteRefSiteId(ExplictRoute explictRoute) {
        List<Route> routes = explictRoute.getRoute();
        Set<String> refSiteNodeIds = new HashSet<>();
        for (Route route : routes) {
            if (route.getPrimary() != null) {
                Set<String> primaryRouteSite = getSiteFromRoute(
                        route.getPrimary().getExplicitRouteObjects());
                refSiteNodeIds.addAll(primaryRouteSite);
            }
            if (route.getSecondary() != null) {
                Set<String> secondaryRouteSite = getSiteFromRoute(
                        route.getSecondary().getExplicitRouteObjects());
                refSiteNodeIds.addAll(secondaryRouteSite);
            }
            if (!CollectionUtils.isEmpty(route.getThird())) {
                for (Third third : route.getThird()) {
                    Set<String> tertiaryRoutePhyNode = getSiteFromRoute(
                            third.getExplicitRouteObjects());
                    refSiteNodeIds.addAll(tertiaryRoutePhyNode);
                }
            }

        }

        return new ArrayList<>(refSiteNodeIds);
    }

    protected List<String> getRouteRefPhyNodeId(ExplictRoute explictRoute) {
        List<Route> routes = explictRoute.getRoute();
        Set<String> refPhyNodeIds = new HashSet<>();
        for (Route route : routes) {
            if (route.getPrimary() != null) {
                Set<String> primaryRoutePhyNode = getPhyNodeFromRoute(
                        route.getPrimary().getExplicitRouteObjects());
                refPhyNodeIds.addAll(primaryRoutePhyNode);
            }
            if (route.getSecondary() != null) {
                Set<String> secondaryRoutePhyNode = getPhyNodeFromRoute(
                        route.getSecondary().getExplicitRouteObjects());
                refPhyNodeIds.addAll(secondaryRoutePhyNode);
            }
            if (!CollectionUtils.isEmpty(route.getThird())) {
                for (Third third : route.getThird()) {
                    Set<String> tertiaryRoutePhyNode = getPhyNodeFromRoute(
                            third.getExplicitRouteObjects());
                    refPhyNodeIds.addAll(tertiaryRoutePhyNode);
                }
            }
        }

        return new ArrayList<>(refPhyNodeIds);
    }

    private Set<String> getSiteFromRoute(List<ExplicitRouteObjects> eros) {
        Set<String> siteIds = new HashSet<>();
        for (ExplicitRouteObjects ero : eros) {
            if (ero.getPathRouteObject() != null) {
                for (PathRouteObject pro : ero.getPathRouteObject()) {
                    Class<?> clazz = pro.getResourceType().getImplementedInterface();
                    if (clazz.isAssignableFrom(Tp.class)) {
                        Tp tpHop = (Tp) pro.getResourceType();
                        siteIds.add(tpHop.getTpHop().getSiteRef().getValue());
                    } else if (clazz.isAssignableFrom(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link.class)) {
                        LinkHop linkHop = ((org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) pro.getResourceType()).getLinkHop();
                        Set<String> refSites = getSiteFromSubRoute(linkHop);
                        siteIds.addAll(refSites);
                    }
                }
            }
        }
        return siteIds;
    }

    private Set<String> getSiteFromSubRoute(LinkHop linkHop) {
        String linkId = linkHop.getLinkRef().getValue();
        String topologyRef = linkHop.getTopologyRef().getValue();
        HashSet<String> siteSet = new HashSet<>();
        if (topologyRef.equals(OCH_TOPO_KEY)) {
            Link ochLink = netconfTopology.getOchLink(linkId);
            Och ochLinkPhysical = ochLink.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
                    .getOch();
            List<String> refSiteIds = getRouteRefSiteId(ochLinkPhysical.getExplictRoute());
            siteSet.addAll(refSiteIds);
        } else if (topologyRef.equals(SITE_TOPO_KEY)) {
            Link siteLink = netconfTopology.getSiteLink(linkId);
            Site siteLinkPhysical = siteLink.getAugmentation(Link1.class).getSite();
            List<String> refSiteIds = getRouteRefSiteId(siteLinkPhysical.getExplictRoute());
            siteSet.addAll(refSiteIds);
        }

        return siteSet;
    }

    private Set<String> getPhyNodeFromRoute(List<ExplicitRouteObjects> eros) {
        Set<String> nodeIds = new HashSet<>();
        for (ExplicitRouteObjects ero : eros) {
            if (ero.getPathRouteObject() != null) {
                for (PathRouteObject pro : ero.getPathRouteObject()) {
                    Class<?> clazz = pro.getResourceType().getImplementedInterface();
                    if (clazz.isAssignableFrom(Tp.class)) {
                        Tp tpHop = (Tp) pro.getResourceType();
                        nodeIds.add(
                                tpHop.getTpHop().getSiteRef().getValue() + POUND + tpHop.getTpHop()
                                        .getNodeRef().getValue());
                    } else if (clazz.isAssignableFrom(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link.class)) {
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link linkHop = (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) pro.getResourceType();
                        nodeIds.addAll(getPhyNodeFromSubRoute(linkHop));
                    }
                }
            }
        }
        return nodeIds;
    }

    private Set<String> getPhyNodeFromSubRoute(
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link linkHop) {
        String linkId = linkHop.getLinkHop().getLinkRef().getValue();
        String topologyRef = linkHop.getLinkHop().getTopologyRef().getValue();
        if (topologyRef.equals(OCH_TOPO_KEY)) {
            Link ochLink = netconfTopology.getOchLink(linkId);

            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1 ochLinkPhysical = ochLink.getAugmentation(
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class);
            List<String> refPhyNodeIds = getRouteRefPhyNodeId(
                    ochLinkPhysical.getOch().getExplictRoute());
            return new HashSet<>(refPhyNodeIds);
        } else if (topologyRef.equals(SITE_TOPO_KEY)) {
            Link siteLink = netconfTopology.getSiteLink(linkId);
            Link1 siteLinkPhysical = siteLink.getAugmentation(Link1.class);
            List<String> refPhyNodeIds = getRouteRefPhyNodeId(siteLinkPhysical.getSite()
                    .getExplictRoute());
            return new HashSet<>(refPhyNodeIds);
        }
        return new HashSet<>();
    }


}

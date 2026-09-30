package net.flex.dci.otn.controller.nms.nms.component.viewTopo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.nms.component.viewTopo.impl.AbstractViewTopo;
import net.flex.dci.otn.controller.nms.nms.enums.NMSViewLinkType;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetViewLinkByPlaneStartwithOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.groupby.plane.output.Topology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.ViewLinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 1/21/2024 4:42 PM
 */

@Component
@Slf4j
public class ViewLinkTopology {

    private final Map<NMSViewLinkType, AbstractViewLink> viewLinkHandlerMap;

    private final Map<ViewLinkType, AbstractViewTopo> viewTopoMap;

    private final DefaultViewLink defaultViewLink;

    private final NetconfTopology topology;

    public ViewLinkTopology(List<AbstractViewLink> viewLinks, List<AbstractViewTopo> viewTopos,
            DefaultViewLink defaultViewLink,
            NetconfTopology topology) {
        viewLinkHandlerMap = viewLinks.stream().collect(HashMap::new,
                (map, viewLink) -> map.put(viewLink.supportViewLinkType(), viewLink),
                HashMap::putAll);
        viewTopoMap = viewTopos.stream().collect(HashMap::new,
                (map, viewTopo) -> map.put(viewTopo.supportViewLinkType(), viewTopo),
                HashMap::putAll);
        this.defaultViewLink = defaultViewLink;
        this.topology = topology;
    }

    public List<Topology> getLinkTopology(ViewLinkType viewLinkType) {
        NMSViewLinkType nmsViewLinkType = NMSViewLinkType.getNMSViewLinkType(viewLinkType);
        List<Topology> topologies = viewLinkHandlerMap.getOrDefault(nmsViewLinkType,
                        defaultViewLink)
                .getRefViewLinks();
        return topologies;
    }

    public GetViewLinkByPlaneStartwithOutput getLinkTopologyByPlaneStartwithAndLinkType(
            ViewLinkType viewLinkType, String plane) {
        log.info("get link topology by plane startWith and linkType");
        List<Link> siteLinks = topology.listAllSiteLinkByPlaneNameStartwith(plane);

        if (siteLinks.isEmpty() && viewLinkType.equals(ViewLinkType.OsLink)) {
            //this is specially for reg mode and this is based on OCH link
//            return new OsLinkViewTopo(siteLinks, viewLinkType, plane).getTopo();
            return viewTopoMap.get(ViewLinkType.OsLink).getTopo(siteLinks, plane);
        } else {
//            switch (viewLinkType) {
//                case SiteLink:
//                    return new SiteLinkViewTopo(siteLinks, viewLinkType, plane).getTopo();
//                case OchLink:
//                    return new OchLinkViewTopo(siteLinks, viewLinkType, plane).getTopo();
//                case OtsLink:
//                    return new OtsLinkViewTopo(siteLinks, viewLinkType, plane).getTopo();
//            }
            return viewTopoMap.get(viewLinkType).getTopo(siteLinks, plane);
        }


    }

    public List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.output.Topology> getLinkTopologyByPlaneAndLinkType(
            ViewLinkType viewLinkType,
            String plane) {
        List<Link> siteLinks = topology.listAllSiteLinkByPlane(plane);
        List<Link> ochLinks = topology.listAllOchLinkByPlane(plane);
        NMSViewLinkType nmsViewLinkType = NMSViewLinkType.getNMSViewLinkType(viewLinkType);
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.output.Topology> topologies = viewLinkHandlerMap.getOrDefault(
                        nmsViewLinkType,
                        defaultViewLink)
                .getRefViewLinkInfoByMultiplexLink(plane, siteLinks, ochLinks);
        return topologies;
    }


    /**
     * list all view plane for the view topology
     *
     * @return
     */
    public List<String> listAllViewPLane() {
        log.info("list all the view plane ");
        //todo: get site ref all the plane
        List<String> siteLinkRefPlane = topology.listAllSiteLinkRefPlane();
        List<String> ochLinkRefPlane = topology.listAllOchLinkRefPlane();
        List<String> planeNames = Stream.concat(siteLinkRefPlane.stream(), ochLinkRefPlane.stream())
                .distinct().collect(
                        Collectors.toList());
        return planeNames;
    }
}


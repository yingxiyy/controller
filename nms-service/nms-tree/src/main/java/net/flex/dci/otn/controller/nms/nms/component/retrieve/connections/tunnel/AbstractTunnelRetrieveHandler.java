package net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.tunnel;

import static net.flex.dci.otn.controller.nms.utils.Constants.OCH_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_TOPO_KEY;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otc.mongo.dto.FilterItem;
import net.flex.dci.otc.mongo.dto.SortItem;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.AbstractNMSRetrieveHandler;
import net.flex.dci.otn.controller.nms.utils.NMSUtils;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.supported.tunnels.SupportedTunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 2022/3/10 16:58
 */
@Slf4j
public abstract class AbstractTunnelRetrieveHandler extends AbstractNMSRetrieveHandler {

    public AbstractTunnelRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

    protected List<String> getPhyNodeRefTunnelIds(List<Node> phyNode) {
        List<String> tunnelIds = new ArrayList<>();
        for (Node node : phyNode) {
            List<Link> linkIds = node.getAugmentation(Node1.class).getPhysical()
                    .getInternalLinks()
                    .stream()
                    .map(internalLink -> {
                        String linkRef = internalLink.getLinkRef();
                        Link phyLink = netconfTopology.getPhyLink(linkRef);
                        if (phyLink == null) {
                            log.warn("query phyLink is not existed LinkRef: {}", linkRef);
                        }
                        return phyLink;
                    })
                    .filter(Objects::nonNull)
                    .collect(
                            Collectors.toList());
            List<String> tempIds = getPhyLinkRefTunnelIds(linkIds);
            tunnelIds.addAll(tempIds);
        }
        return tunnelIds;
    }

    protected List<String> getTpcPhyNodeRefTunnelIds(List<Node> phyNode) {
        List<String> tunnelIds = new ArrayList<>();
        for (Node node : phyNode) {
            List<String> linkIds = node.getAugmentation(Node1.class).getPhysical()
                    .getInternalLinks()
                    .stream()
                    .map(internalLink -> netconfTopology.getPhyLink(internalLink.getLinkRef()))
                    .filter(Objects::nonNull)
                    .map(innerLink -> innerLink.getLinkId().getValue())
                    .collect(
                            Collectors.toList());
            List<String> tempIds = getPhyLinkUnderTpcRefTunnelIds(linkIds);
            tunnelIds.addAll(tempIds);
        }
        return tunnelIds;
    }

    private List<String> getPhyLinkUnderTpcRefTunnelIds(List<String> linkIds) {

        List<Link> ochLinks = netconfTopology.getOchLinksBasedOnPhyLinks(linkIds);
        List<String> supportedTunnelIds = ochLinks.stream()
                .collect(ArrayList::new, (list, ochLink) -> list.addAll(ochLink.getAugmentation(
                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class)
                        .getSupportedTunnel().stream()
                        .map(tunnel -> tunnel.getTunnelRef().getValue())
                        .collect(Collectors.toList())), ArrayList::addAll);
        return supportedTunnelIds;
    }

    protected List<String> getPhyLinkRefTunnelIds(List<Link> refPhyLinks) {
        List<String> phyLinkIds = refPhyLinks.stream().map(LinkAttributes::getLinkId).map(
                Uri::getValue).collect(
                Collectors.toList());
        log.debug("get phyLink ref tunnel ids,phy link id is:{}", phyLinkIds);
//        for (Link refPhyLink : refPhyLinks) {
//            if (refPhyLink != null) {
//                Link1 link1 = refPhyLink.getAugmentation(Link1.class);
//                if (link1.getPhysical().getSupportedLink() != null) {
//                    List<String> refTunnelIds = getSlRefTunnelIds(
//                            link1.getPhysical().getSupportedLink());
//                    tunnelIds.addAll(refTunnelIds);
//                }
//            }
//        }
        List<String> tunnelIds = getPhyLinkUnderTpcRefTunnelIds(phyLinkIds);
        return tunnelIds;
    }

    /**
     * get site link support tunnel ids
     *
     * @param siteLink
     * @return
     */
    protected List<String> getSiteLinkSupportTunnelIds(Link siteLink) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1 link1 = siteLink.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class);
        List<SupportedLink> supportedOchLinks = link1.getSite().getSupportedLink();
        if (supportedOchLinks == null || supportedOchLinks.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> supportOchLinkIds = supportedOchLinks.stream()
                .map(supportedLink -> supportedLink.getLinkRef().getValue()).collect(
                        Collectors.toSet());
        List<Link> ochLinks = netconfTopology.getOchLinksByIds(supportOchLinkIds);

        List<String> supportTunnelIds = new ArrayList<>();
        for (Link ochLink : ochLinks) {

            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1 tunnelLink1 =
                    ochLink.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class);
            List<String> supportIds = tunnelLink1.getSupportedTunnel().stream()
                    .map(supportedTunnel -> supportedTunnel.getTunnelRef().getValue()).collect(
                            Collectors.toList());
            supportTunnelIds.addAll(supportIds);
        }
        return supportTunnelIds;
    }


    protected List<String> getOchLinkSupportTunnelIds(Link ochLink) {
        log.info("get tunnel layer by och link :{}", ochLink.getLinkId().getValue());
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1 link1 =
                ochLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class);
        List<String> supportTunnelIds = link1.getSupportedTunnel().stream()
                .map(supportedTunnel -> supportedTunnel.getTunnelRef().getValue()).collect(
                        Collectors.toList());
        return supportTunnelIds;
    }


    protected List<String> getTpRefTunnelIds(TerminationPoint tp) {
        String tpId = tp.getTpId().getValue();
        log.debug("retrieve the tunnel from tp :{}", tpId);
        Physical tpPhysical = tp.getAugmentation(TerminationPoint1.class).getPhysical();
        PortType portType = tpPhysical.getPortType();
        String nodeId = NMSUtils.getTpRefPhyNodeId(tpId);
        List<String> tunnelIds = new ArrayList<>();
        if (!portType.equals(PortType.OTUClient)) {
            List<Link> refLinks = getTpRefPhyLinks(nodeId, tpId);
            List<String> phyLinkIds = refLinks.stream().map(link -> link.getLinkId().getValue())
                    .collect(
                            Collectors.toList());
            List<Link> ochLinks = netconfTopology.getOchLinksBasedOnPhyLinks(phyLinkIds);

            for (Link ochLink : ochLinks) {
                List<SupportedTunnel> supportedTunnels = ochLink.getAugmentation(
                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class)
                        .getSupportedTunnel();
                List<String> supportTunnelIds = supportedTunnels.stream()
                        .map(supportedTunnel -> supportedTunnel.getTunnelRef().getValue()).collect(
                                Collectors.toList());
                tunnelIds.addAll(supportTunnelIds);
            }
        } else {
            List<String> tpRefTunnelIds = netconfTopology.getTunnelIdsUnderRefTp(tpId);
            tunnelIds.addAll(tpRefTunnelIds);
        }
        return tunnelIds;
    }


    private List<String> getSlRefTunnelIds(List<SupportedLink> sls) {
        if (CollectionUtils.isEmpty(sls)) {
            return new ArrayList<>();
        }
        List<String> tunnelIds = new ArrayList<>();
        for (SupportedLink sl : sls) {
            String linkId = sl.getLinkRef().getValue();
            String topology = sl.getTopologyRef().getValue();
            List<String> tempIds = getRefTunnelFromSubLink(linkId, topology);
            tunnelIds.addAll(tempIds);
        }

        return tunnelIds;
    }

    private List<String> getRefTunnelFromSubLink(String linkId, String topology) {
        List<String> tunnelIds = new ArrayList<>();
        if (topology.equals(OCH_TOPO_KEY)) {
            Link ochLink = netconfTopology.getOchLink(linkId);
            tunnelIds = getOchLinkSupportTunnelIds(ochLink);
        } else if (topology.equals(SITE_TOPO_KEY)) {
            Link siteLink = netconfTopology.getSiteLink(linkId);
            tunnelIds = getSiteLinkSupportTunnelIds(siteLink);
        }
        return tunnelIds;
    }

    protected PageResult<Tunnel> retrieveTunnelPagedByTunnelIds(List<String> tunnelIds,
            Integer pageNum, Integer pageSize, List<FilterItem> filterItems,
            List<SortItem> sortItems) {
        if (tunnelIds.isEmpty()) {
            return new PageResult<>();
        }
        return netconfTopology.retrieveTunnelPagedByTunnelIds(tunnelIds, pageNum, pageSize,
                filterItems, sortItems);
    }


}

package net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.sitelink;

import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_LINK_PREFIX;
import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_TOPO_KEY;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otc.mongo.dto.FilterItem;
import net.flex.dci.otc.mongo.dto.SortItem;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.AbstractNMSRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import net.flex.dci.otn.controller.nms.nms.enums.NMSConvertType;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLink;

/**
 * @version 1.0
 * @date 2022/3/11 13:38
 */
@Slf4j
public abstract class AbstractSiteLinkRetrieveHandler extends AbstractNMSRetrieveHandler {

    public AbstractSiteLinkRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

    protected List<String> getPhyNodeRefSiteLinkIds(List<String> phyNodeIds) {
        HashSet<String> siteLinkSet = new HashSet<>();
        for (String nodeId : phyNodeIds) {
            List<Link> linkIds = netconfTopology.getPhyLinkUnderPhyNode(
                    nodeId);
            List<String> tempIds = getPhyLinkRefSiteLinkIds(linkIds);
            siteLinkSet.addAll(tempIds);
        }
        return new ArrayList<>(siteLinkSet);
    }


    protected List<String> getPhyLinkRefSiteLinkIds(List<Link> refLinks) {
        List<String> siteLinkIds = new ArrayList<>();
        for (Link refPhyLink : refLinks) {
            Link1 link1 = refPhyLink.getAugmentation(Link1.class);
            if (link1.getPhysical().getSupportedLink() != null) {
                List<String> refSiteLinkIds = getSlRefSiteLinkIds(
                        link1.getPhysical().getSupportedLink());
                siteLinkIds.addAll(refSiteLinkIds);
            }
        }
        return siteLinkIds;
    }


    private List<String> getSlRefSiteLinkIds(List<SupportedLink> supportedLink) {
        return supportedLink.stream()
                .filter(sl -> sl.getTopologyRef().getValue().equals(SITE_TOPO_KEY))
                .map(sl -> sl.getLinkRef().getValue()).collect(
                        Collectors.toList());
    }


    protected List<String> getOchRefSiteLinkIds(List<Link> ochLinks) {
        List<String> siteLinksIds = new ArrayList<>();
        for (Link link : ochLinks) {
            List<String> currentSiteLinkIds = link.getSupportingLink().stream()
                    .filter(supportingLink -> supportingLink.getLinkRef().getValue()
                            .startsWith(SITE_LINK_PREFIX))
                    .map(supportingLink -> supportingLink.getLinkRef().getValue())
                    .collect(Collectors.toList());
            siteLinksIds.addAll(currentSiteLinkIds);
        }
        return siteLinksIds;

    }


    protected PageResult<Link> retrieveAllSiteLinkByIdsPaged(List<String> siteLinkIds,
            RetrieveTopologyDto retrieveTopologyDto) {
        List<FilterItem> filterItems = buildFilterItemsByPlaneId(retrieveTopologyDto,
                NMSConvertType.SITE_LINK);
        List<SortItem> sortItems = getSortItems(retrieveTopologyDto);
        return netconfTopology.retrieveAllSiteLinkByIdsPaged(siteLinkIds,
                retrieveTopologyDto.getPageNum(), retrieveTopologyDto.getPageSize(), filterItems,
                sortItems);
    }


}

package net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.phylink;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otc.mongo.dto.FilterItem;
import net.flex.dci.otc.mongo.dto.SortItem;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.AbstractNMSRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import net.flex.dci.otn.controller.nms.nms.enums.NMSConvertType;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.InternalLinkAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRack;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/8 14:21
 */
@Slf4j
@Component
public class RackRefPhyLinkRetrieveHandler extends AbstractNMSRetrieveHandler {


    public RackRefPhyLinkRetrieveHandler(NetconfTopology netconfTopology) {
        super(netconfTopology);

    }

    @Override
    public PageResult<Link> retrieveAllLinkPaged(Integer pageNum, Integer pageSize) {
//        log.debug("retrieve all phy link by site node:{},rack :{},pageNum :{},pageSize :{}", nodeId,
//                rackId, pageNum, pageSize);
//        SupportingRack rack = netconfTopology.getRack(nodeId, rackId);
//        if (rack == null) {
//            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
//                    "cannnot find site: " + nodeId + " required rack " + rackId);
//        }
//        List<Node> nodes = rack.getSupportingNe().stream()
//                .map(supportingNe -> netconfTopology.getPhyNode(
//                        supportingNe.getNodeRef().getValue())).collect(
//                        Collectors.toList());
//        List<String> linkIds = new ArrayList<>();
//        for (Node node : nodes) {
//            List<String> phyLinkIds = node.getAugmentation(Node1.class).getPhysical()
//                    .getInternalLinks().stream()
//                    .map(InternalLinkAttributes::getLinkRef).collect(
//                            Collectors.toList());
//            linkIds.addAll(phyLinkIds);
//        }
//
//        PageResult<Link> pageResult = netconfTopology.retrieveAllPhyLinkByLinkIdsPaged(linkIds,
//                pageNum, pageSize);
//        return pageResult;
        return PageResult.<Link>builder().build();
    }

    @Override
    public PageResult<Link> retrieveAllLinkPaged(RetrieveTopologyDto retrieveTopologyDto) {
        String nodeId = retrieveTopologyDto.getNodeRef();
        String rackId = retrieveTopologyDto.getRackRef();
        SupportingRack rack = netconfTopology.getRack(nodeId, rackId);
        if (rack == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "cannnot find site: " + nodeId + " required rack " + rackId);
        }
        List<Node> nodes = rack.getSupportingNe().stream()
                .map(supportingNe -> netconfTopology.getPhyNode(
                        supportingNe.getNodeRef().getValue())).filter(Objects::nonNull).collect(
                        Collectors.toList());
        List<String> linkIds = new ArrayList<>();
        for (Node node : nodes) {
            List<String> phyLinkIds = node.getAugmentation(Node1.class).getPhysical()
                    .getInternalLinks().stream()
                    .map(InternalLinkAttributes::getLinkRef).collect(
                            Collectors.toList());
            linkIds.addAll(phyLinkIds);
        }
        List<FilterItem> filterItems = buildFilterItemsByPlaneId(retrieveTopologyDto,
                NMSConvertType.PHY_LINK);
        List<SortItem> sortItems = getSortItems(retrieveTopologyDto);
        PageResult<Link> pageResult = netconfTopology.retrieveAllPhyLinkByLinkIdsPaged(linkIds,
                retrieveTopologyDto.getPageNum(), retrieveTopologyDto.getPageSize(), filterItems,
                sortItems);
        return pageResult;
    }
}

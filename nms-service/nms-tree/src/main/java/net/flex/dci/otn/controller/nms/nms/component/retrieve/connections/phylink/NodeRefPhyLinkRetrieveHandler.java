package net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.phylink;

import java.util.List;
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
import net.flex.dci.otn.controller.nms.utils.Constants;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/8 14:20
 */
@Slf4j
@Component
public class NodeRefPhyLinkRetrieveHandler extends AbstractNMSRetrieveHandler {

//    private final String topologyRef;
//
//    private final String nodeRef;

    public NodeRefPhyLinkRetrieveHandler(NetconfTopology netconfTopology) {
        super(netconfTopology);
//        this.topologyRef = topologyRef;
//        this.nodeRef = nodeRef;
    }

    @Override
    public PageResult<Link> retrieveAllLinkPaged(Integer pageNum, Integer pageSize) {
//        log.debug(
//                "retrieve the node ref phy link ,node type is :{} ,id is:{},ref phy link ,page num :{},page size :{}",
//                topologyRef, nodeRef,
//                pageNum, pageSize);
        PageResult<Link> pageResult = null;
//        if (topologyRef.equals(Constants.SITE_TOPO_KEY)) {
//            pageResult = retrieveSiteNodeRefLink(nodeRef, pageNum, pageSize);
//        } else if (topologyRef.equals(Constants.PHY_TOPO_KEY)) {
//            pageResult = retrievePhyNodeRefLink(nodeRef, pageNum, pageSize);
//        }

        return pageResult;
    }

    @Override
    public PageResult<Link> retrieveAllLinkPaged(RetrieveTopologyDto retrieveTopologyDto) {
        String topologyRef = retrieveTopologyDto.getTopologyRef();
        String nodeRef = retrieveTopologyDto.getNodeRef();
        log.debug(
                "retrieve the node ref phy link paged ,node type is :{} ,id is:{},ref phy link ,page num :{},page size :{}",
                topologyRef, nodeRef,
                retrieveTopologyDto.getPageNum(), retrieveTopologyDto.getPageSize());
        PageResult<Link> pageResult = null;
        if (topologyRef.equals(Constants.SITE_TOPO_KEY)) {
            pageResult = retrieveSiteNodeRefLink(nodeRef, retrieveTopologyDto);
        } else if (topologyRef.equals(Constants.PHY_TOPO_KEY)) {
            pageResult = retrievePhyNodeRefLink(nodeRef, retrieveTopologyDto);
        }

        return pageResult;
    }

    private PageResult<Link> retrievePhyNodeRefLink(String nodeId,
            RetrieveTopologyDto retrieveTopologyDto) {
        Node ne = netconfTopology.getPhyNode(nodeId);
        if (ne == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can not find required site node :" + nodeId);
        }

        Node1 phyNode = ne.getAugmentation(Node1.class);
        if (phyNode == null) {
            log.error("find a phyNode, node:{}, it hasn't provide physical augment.",
                    ne.getNodeId().getValue());
            return new PageResult<>();
        }
//        List<String> linkIds = phyNode.getPhysical().getInternalLinks().stream()
//                .map(internalLinks -> internalLinks.getLinkRef()).collect(
//                        Collectors.toList());
        List<FilterItem> filterItems = buildFilterItemsByPlaneId(retrieveTopologyDto,
                NMSConvertType.PHY_LINK);
        List<SortItem> sortItems = getSortItems(retrieveTopologyDto);
        PageResult<Link> pageResult = netconfTopology.retrieveAllPhyLinkUnderByNodeIdPaged(nodeId,
                retrieveTopologyDto.getPageNum(), retrieveTopologyDto.getPageSize(), filterItems,
                sortItems);
        return pageResult;
    }

    private PageResult<Link> retrieveSiteNodeRefLink(String nodeId,
            RetrieveTopologyDto retrieveTopologyDto) {

        Node siteNode = netconfTopology.getSiteNode(nodeId);
        if (siteNode == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can not find required site Node :" + nodeId);
        }

        List<FilterItem> filterItems = buildFilterItemsByPlaneId(retrieveTopologyDto,
                NMSConvertType.PHY_LINK);
        List<SortItem> sortItems = getSortItems(retrieveTopologyDto);
        PageResult<Link> pageResult = netconfTopology.retrieveAllPhyLinkUnderByNodeIdPaged(nodeId,
                retrieveTopologyDto.getPageNum(), retrieveTopologyDto.getPageSize(), filterItems,
                sortItems);
        return pageResult;
    }

    private PageResult<Link> retrievePhyNodeRefLink(String nodeId, Integer pageNum,
            Integer pageSize) {

        Node ne = netconfTopology.getPhyNode(nodeId);
        if (ne == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can not find required site node :" + nodeId);
        }

        Node1 phyNode = ne.getAugmentation(Node1.class);
        if (phyNode == null) {
            log.error("find a phyNode, node:{}, it hasn't provide physical augment.",
                    ne.getNodeId().getValue());
            return new PageResult<>();
        }
        List<String> linkIds = phyNode.getPhysical().getInternalLinks().stream()
                .map(internalLinks -> internalLinks.getLinkRef()).collect(
                        Collectors.toList());
        PageResult<Link> pageResult = netconfTopology.retrieveAllPhyLinkByLinkIdsPaged(linkIds,
                pageNum, pageSize);
        return pageResult;
    }

    private PageResult<Link> retrieveSiteNodeRefLink(String nodeId, Integer pageNum,
            Integer pageSize) {
        Node siteNode = netconfTopology.getSiteNode(nodeId);
        if (siteNode == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can not find required site Node :" + nodeId);
        }

        PageResult<Link> pageResult = netconfTopology.retrieveAllPhyLinkUnderByNodeIdPaged(nodeId,
                pageNum, pageSize);
        return pageResult;
    }
}

package net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.tunnel;

import static net.flex.dci.otn.controller.nms.utils.Constants.PHY_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_TOPO_KEY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otc.mongo.dto.FilterItem;
import net.flex.dci.otc.mongo.dto.SortItem;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import net.flex.dci.otn.controller.nms.nms.enums.NMSConvertType;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRack;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/9 16:48
 */
@Slf4j
@Component
public class NodeRefTunnelRetrieveHandler extends AbstractTunnelRetrieveHandler {

//    private final String topologyRef;
//
//    private final String nodeId;

    public NodeRefTunnelRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
//        this.topologyRef = topologyRef;
//        this.nodeId = nodeId;
    }

    @Override
    public PageResult<Tunnel> retrieveAllTunnelPaged(Integer pageNum, Integer pageSize) {
//        log.info(
//                "retrieve all tunnel paged from topology:{},nodeId is :{},pageNum is :{},pageSize is:{}",
//                topologyRef, nodeId, pageNum, pageSize);
//        if (topologyRef.equals(PHY_TOPO_KEY)) {
//            return retrieveAllTunnelByPhyNode(nodeId, pageNum, pageSize);
//        } else if (topologyRef.equals(SITE_TOPO_KEY)) {
//            return retrieveAllTunnelBySiteNode(nodeId, pageNum, pageSize);
//        } else {
//            log.error("cannot support node retrieve for topology :{}", topologyRef);
//            throw new CommonException(CommonExceptionType.NOT_SUPPORT_ERROR,
//                    "cannot support node retrieve for topology :" + topologyRef);
//
//        }
        return null;
    }

    @Override
    public PageResult<Tunnel> retrieveAllTunnelPaged(RetrieveTopologyDto retrieveTopologyDto) {
        log.info("retrieve all topology tunnel retrieveTopology domain is:{}", retrieveTopologyDto);
        String topologyRef = retrieveTopologyDto.getTopologyRef();
        String nodeId = retrieveTopologyDto.getNodeRef();
        try {
            List<FilterItem> filterItems = buildFilterItemsByPlaneId(retrieveTopologyDto,
                    NMSConvertType.TUNNEL);
            List<SortItem> sortItems = getSortItems(retrieveTopologyDto);
            int pageNum = retrieveTopologyDto.getPageNum();
            int pageSize = retrieveTopologyDto.getPageSize();
            if (topologyRef.equals(PHY_TOPO_KEY)) {
                return retrieveAllTunnelByPhyNode(nodeId, pageNum, pageSize, filterItems,
                        sortItems);
            } else if (topologyRef.equals(SITE_TOPO_KEY)) {
                return retrieveAllTunnelBySiteNode(nodeId, pageNum, pageSize, filterItems,
                        sortItems);
            } else {
                log.error("cannot support node retrieve for topology :{}", topologyRef);
                throw new CommonException(CommonExceptionType.NOT_SUPPORT_ERROR,
                        "cannot support node retrieve for topology :" + topologyRef);

            }
        } catch (Exception ex) {
            log.error("cannot support node retrieve for topology :{} the reason is:{}", topologyRef,
                    ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.NOT_SUPPORT_ERROR,
                    "cannot support node retrieve for topology :" + topologyRef, ex);
        }
    }

    private PageResult<Tunnel> retrieveAllTunnelBySiteNode(String nodeId, Integer pageNum,
            Integer pageSize, List<FilterItem> filterItems, List<SortItem> sortItems) {
        log.debug("retrieve site node ref tunnel");
        Node siteNode = netconfTopology.getSiteNode(nodeId);
        if (siteNode == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can't find the require site node id is :" + nodeId);
        }
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1 siteNodePhysical = siteNode.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class);
        if (siteNodePhysical.getSite() == null
                || siteNodePhysical.getSite().getSupportingRack() == null) {
            return new PageResult<>();
        }

        List<String> supportedNodeIds = new ArrayList<>();
        for (SupportingRack sr : siteNodePhysical.getSite().getSupportingRack()) {
            supportedNodeIds.addAll(
                    sr.getSupportingNe().stream().map(sn -> sn.getNodeRef().getValue()).collect(
                            Collectors.toList()));
        }
        List<Node> phyNode = netconfTopology.getPhyNodesByIds(supportedNodeIds);
        List<String> tunnelIds = getPhyNodeRefTunnelIds(phyNode);
        return retrieveTunnelPagedByTunnelIds(tunnelIds, pageNum, pageSize,
                filterItems, sortItems);
    }


    private PageResult<Tunnel> retrieveAllTunnelByPhyNode(String nodeId, Integer pageNum,
            Integer pageSize, List<FilterItem> filterItems, List<SortItem> sortItems) {
        log.debug("retrieve tunnel under phy node");

        Node phyNode = netconfTopology.getPhyNode(nodeId);
        if (phyNode == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can't find required PHY NE id is :" + nodeId);
        }
        Node1 nodeProp = phyNode.getAugmentation(Node1.class);
        Physical nodePhysical = nodeProp.getPhysical();
        NodeType nodeType = nodePhysical.getNodeType();
        List<String> tunnelIds = new ArrayList<>();
        if (nodeType.equals(NodeType.OD)) {
            tunnelIds = getPhyNodeRefTunnelIds(Collections.singletonList(phyNode));
        } else {
            tunnelIds = getTpcPhyNodeRefTunnelIds(Collections.singletonList(phyNode));
        }
        return retrieveTunnelPagedByTunnelIds(tunnelIds, pageNum, pageSize,
                filterItems, sortItems);
    }


}

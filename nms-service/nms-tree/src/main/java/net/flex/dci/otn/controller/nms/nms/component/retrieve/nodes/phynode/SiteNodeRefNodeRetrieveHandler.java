package net.flex.dci.otn.controller.nms.nms.component.retrieve.nodes.phynode;

import static net.flex.dci.otn.controller.nms.utils.Constants.PHY_TOPO_KEY;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otc.mongo.dto.FilterItem;
import net.flex.dci.otc.mongo.dto.SortItem;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.nodes.AbstractNodeRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import net.flex.dci.otn.controller.nms.nms.enums.NMSConvertType;
import net.flex.dci.otn.controller.nms.utils.CommonUtils;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/13 10:03
 */
@Slf4j
@Component
public class SiteNodeRefNodeRetrieveHandler extends AbstractNodeRetrieveHandler {


    public SiteNodeRefNodeRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
//        this.nodeId = nodeId;
    }

    @Override
    public PageResult<Node> retrieveAllNodePaged(Integer pageNum, Integer pageSize) {
//        log.info("retrieve all phy node paged from site node:{},pageSize:{},pageNum:{}", nodeId,
//                pageSize, pageNum);
//        Node siteNode = netconfTopology.getSiteNode(nodeId);
//        if (siteNode == null) {
//            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
//                    "can not find the site node :" + nodeId);
//        }
//        List<String> refPhyNodeIds = siteNode.getSupportingNode().stream()
//                .filter(supportingNode -> supportingNode.getTopologyRef().getValue()
//                        .equals(PHY_TOPO_KEY))
//                .map(supportingNode -> supportingNode.getNodeRef().getValue()).collect(
//                        Collectors.toList());
//        return netconfTopology.retrieveAllPhyNodePagedByIds(refPhyNodeIds, pageNum, pageSize);
        return PageResult.<Node>builder().build();
    }

    @Override
    public PageResult<Node> retrieveAllNodePaged(RetrieveTopologyDto retrieveTopologyDto) {
        log.debug("start to retrieve all node paged ,retrieve topology domain is :{}",
                retrieveTopologyDto);
        String nodeId = retrieveTopologyDto.getNodeRef();
        Node siteNode = netconfTopology.getSiteNode(nodeId);
        if (siteNode == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can not find the site node :" + nodeId);
        }
        List<String> refPhyNodeIds = siteNode.getSupportingNode().stream()
                .filter(supportingNode -> supportingNode.getTopologyRef().getValue()
                        .equals(PHY_TOPO_KEY))
                .map(supportingNode -> supportingNode.getNodeRef().getValue()).collect(
                        Collectors.toList());
        List<FilterItem> filterItems = buildFilterItemsWithPlane(retrieveTopologyDto,
                NMSConvertType.PHY_NODE);
        List<SortItem> sortItems = getSortItems(retrieveTopologyDto);
        int pageNum = retrieveTopologyDto.getPageNum();
        int pageSize = retrieveTopologyDto.getPageSize();
        return netconfTopology.retrieveAllPhyNodePagedByIds(refPhyNodeIds, pageNum, pageSize,
                filterItems,
                sortItems);
    }

    @Override
    public List<Node> retrieveAllNode(RetrieveTopologyDto retrieveTopologyDto) {
        log.debug("start to retrieve all node  ,retrieve topology domain is :{}",
                retrieveTopologyDto);
        String nodeId = retrieveTopologyDto.getNodeRef();
        Node siteNode = netconfTopology.getSiteNode(nodeId);
        if (siteNode == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can not find the site node :" + nodeId);
        }
        List<String> refPhyNodeIds = siteNode.getSupportingNode().stream()
                .filter(supportingNode -> supportingNode.getTopologyRef().getValue()
                        .equals(PHY_TOPO_KEY))
                .map(supportingNode -> supportingNode.getNodeRef().getValue()).collect(
                        Collectors.toList());
//        List<Node> nodes = netconfTopology.retrieveAllPhyNodeByIds(refPhyNodeIds);
//        return nodes;
        List<Node> realNodes = netconfTopology.retrieveAllRealPhyNodeByIds(refPhyNodeIds);
        Set<String> notSuperviseNeIds = CommonUtils.getDifferenceSetByGuava(
                new HashSet<>(refPhyNodeIds),
                realNodes.stream().map(NodeAttributes::getNodeId).map(Uri::getValue)
                        .collect(Collectors.toSet()));
        List<Node> nodes = netconfTopology.retrieveAllPhyNodeByIds(
                new ArrayList<>(notSuperviseNeIds));
        List<Node> result = Stream.of(realNodes, nodes).flatMap(List::stream)
                .collect(Collectors.toList());
        return result;
    }

}

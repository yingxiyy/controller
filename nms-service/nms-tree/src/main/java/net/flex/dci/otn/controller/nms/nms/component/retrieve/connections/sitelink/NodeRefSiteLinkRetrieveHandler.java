package net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.sitelink;

import static net.flex.dci.otn.controller.nms.utils.Constants.PHY_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_TOPO_KEY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/11 13:42
 */
@Slf4j
@Component
public class NodeRefSiteLinkRetrieveHandler extends AbstractSiteLinkRetrieveHandler {

//    private final String topologyRef;
//
//    private final String nodeId;

    public NodeRefSiteLinkRetrieveHandler(NetconfTopology netconfTopology) {
        super(netconfTopology);
//        this.topologyRef = topologyRef;
//        this.nodeId = nodeRef;
    }


    @Override
    public PageResult<Link> retrieveAllSiteLinkPaged(RetrieveTopologyDto retrieveTopologyDto) {
        String topologyRef = retrieveTopologyDto.getTopologyRef();
        String nodeId = retrieveTopologyDto.getNodeRef();
        log.info(
                "retrieve all tunnel paged from topology:{},nodeId is :{}",
                topologyRef, nodeId);
        PageResult<Link> pageResult = new PageResult<>();
        if (topologyRef.equals(PHY_TOPO_KEY)) {
            pageResult = getPhyNodeRefSiteLinkPaged(nodeId, retrieveTopologyDto);
        } else if (topologyRef.equals(SITE_TOPO_KEY)) {
            pageResult = getSiteNodeRefSiteLinkPaged(nodeId, retrieveTopologyDto);
        } else {
            throw new CommonException(CommonExceptionType.NOT_SUPPORT_ERROR,
                    "can not support the topology :" + topologyRef + " site link retrieve");
        }
        return pageResult;
    }


    @Override
    public PageResult<Link> retrieveAllLinkPaged(RetrieveTopologyDto retrieveTopologyDto) {
        String nodeId = retrieveTopologyDto.getNodeRef();
        log.debug("retrieve phy node ref site link");
        Node node = netconfTopology.getPhyNode(nodeId);
        if (node == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can't find the phy node :" + nodeId);
        }
        List<String> refSiteLinkIds = getPhyNodeRefSiteLinkIds(Collections.singletonList(nodeId));
        return retrieveAllSiteLinkByIdsPaged(refSiteLinkIds, retrieveTopologyDto);
    }

//    private PageResult<Link> getPhyNodeRefSiteLinkPaged(String nodeId, Integer pageNum,
//            Integer pageSize) {
//        log.debug("retrieve phy node ref ");
//        Node node = netconfTopology.getPhyNode(nodeId);
//        if (node == null) {
//            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
//                    "can't find the phy node :" + nodeId);
//        }
//        List<String> refSiteLinkIds = new ArrayList<>();
//        NodeType nodeType = node.getAugmentation(Node1.class).getPhysical().getNodeType();
//        if (nodeType.equals(NodeType.OPC4)) {
//            refSiteLinkIds = getPhyNodeRefSiteLinkIds(Collections.singletonList(nodeId));
//        } else if (nodeType.equals(NodeType.TPC4)) {
//            refSiteLinkIds = getTpcPhyNodeRefSiteLinkIds(node);
//        }
//
//        return netconfTopology.retrieveAllSiteLinkByIdsPaged(refSiteLinkIds, pageNum, pageSize);
//    }

    /**
     * get tpc phy node ref site link ids
     *
     * @param node
     * @return
     */
    private List<String> getTpcPhyNodeRefSiteLinkIds(Node node) {
        List<String> linkIds = node.getAugmentation(Node1.class).getPhysical()
                .getInternalLinks()
                .stream()
                .map(internalLink -> netconfTopology.getPhyLink(internalLink.getLinkRef()))
                .filter(Objects::nonNull)
                .map(innerLink -> innerLink.getLinkId().getValue())
                .collect(
                        Collectors.toList());
        List<Link> ochLinks = netconfTopology.getOchLinksBasedOnPhyLinks(linkIds);
        List<String> refSiteLinkIds = ochLinks.stream()
                .map(ochLink -> ochLink.getSupportingLink().stream()
                        .map(supportingLink -> supportingLink.getLinkRef().getValue()).filter(
                                linkId -> SiteLinkIdNamingRule.isSiteLink(linkId))
                        .collect(Collectors.toList()))
                .collect(LinkedList::new, LinkedList::addAll, LinkedList::addAll);

        return refSiteLinkIds;
    }

    private PageResult<Link> getPhyNodeRefSiteLinkPaged(String nodeId,
            RetrieveTopologyDto retrieveTopologyDto) {
        log.debug("retrieve phy node ref ");
        Node node = netconfTopology.getPhyNode(nodeId);
        if (node == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can't find the phy node :" + nodeId);
        }
        List<String> refSiteLinkIds = new ArrayList<>();
        NodeType nodeType = node.getAugmentation(Node1.class).getPhysical().getNodeType();
        if (nodeType.equals(NodeType.OD)) {
            refSiteLinkIds = getPhyNodeRefSiteLinkIds(Collections.singletonList(nodeId));
        } else if (nodeType.equals(NodeType.TD)) {
            refSiteLinkIds = getTpcPhyNodeRefSiteLinkIds(node);
        }
//        List<String> refSiteLinkIds = getPhyNodeRefSiteLinkIds(Collections.singletonList(nodeId));
        return retrieveAllSiteLinkByIdsPaged(refSiteLinkIds, retrieveTopologyDto);
    }

    private PageResult<Link> getSiteNodeRefSiteLinkPaged(String nodeId,
            RetrieveTopologyDto retrieveTopologyDto) {
        log.debug("retrieve site node ref");
        Node node = netconfTopology.getSiteNode(nodeId);
        if (node == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can't find the site node :" + nodeId);
        }
        List<String> refPhyNodeIds = node.getSupportingNode().stream()
                .filter(supportingNode -> supportingNode.getTopologyRef().getValue()
                        .equals(PHY_TOPO_KEY))
                .map(supportingNode -> supportingNode.getNodeRef().getValue()).collect(
                        Collectors.toList());
        List<String> siteLinkIds = getPhyNodeRefSiteLinkIds(refPhyNodeIds);
        return retrieveAllSiteLinkByIdsPaged(siteLinkIds, retrieveTopologyDto);
    }

//    private PageResult<Link> getSiteNodeRefSiteLinkPaged(String nodeId, Integer pageNum,
//            Integer pageSize) {
//        log.debug("retrieve site node ref");
//        Node node = netconfTopology.getSiteNode(nodeId);
//        if (node == null) {
//            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
//                    "can't find the site node :" + nodeId);
//        }
//        List<String> refPhyNodeIds = node.getSupportingNode().stream()
//                .filter(supportingNode -> supportingNode.getTopologyRef().getValue()
//                        .equals(PHY_TOPO_KEY))
//                .map(supportingNode -> supportingNode.getNodeRef().getValue()).collect(
//                        Collectors.toList());
//        List<String> siteLinkIds = getPhyNodeRefSiteLinkIds(refPhyNodeIds);
//        return netconfTopology.retrieveAllSiteLinkByIdsPaged(siteLinkIds, pageNum, pageSize);
//    }
}

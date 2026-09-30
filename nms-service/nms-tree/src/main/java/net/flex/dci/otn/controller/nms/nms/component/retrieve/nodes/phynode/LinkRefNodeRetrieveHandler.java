package net.flex.dci.otn.controller.nms.nms.component.retrieve.nodes.phynode;

import static net.flex.dci.otn.controller.nms.utils.Constants.OCH_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.PHY_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_TOPO_KEY;

import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otc.mongo.dto.FilterItem;
import net.flex.dci.otc.mongo.dto.SortItem;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.nodes.AbstractNodeRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import net.flex.dci.otn.controller.nms.nms.enums.NMSConvertType;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/13 10:04
 */
@Slf4j
@Component
public class LinkRefNodeRetrieveHandler extends AbstractNodeRetrieveHandler {


    public LinkRefNodeRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);

    }

    @Override
    public PageResult<Node> retrieveAllNodePaged(Integer pageNum, Integer pageSize) {
//        log.info("retrieve all node from the topology:{},linkId:{},pageNum:{},pageSize:{}",
//                topologyRef, linkId, pageNum, pageSize);
//        PageResult<Node> pageResult = new PageResult<>();
//        if (topologyRef.equals(PHY_TOPO_KEY)) {
//            pageResult = retrievePhyLinkRefNode(pageNum, pageSize);
//        } else if (topologyRef.equals(SITE_TOPO_KEY)) {
//            pageResult = retrieveSiteLinkRefNode(pageNum, pageSize);
//        } else if (topologyRef.equals(OCH_TOPO_KEY)) {
//            pageResult = retrieveOchLinkRefNode(pageNum, pageSize);
//        } else {
//            throw new CommonException(CommonExceptionType.NOT_SUPPORT_ERROR,
//                    "can not support the topology type:" + topologyRef + " phy node retrieve ");
//        }
//        return pageResult;
        return PageResult.<Node>builder().build();
    }

    @Override
    public PageResult<Node> retrieveAllNodePaged(RetrieveTopologyDto retrieveTopologyDto) {
        log.debug("start to retrieve all node paged ,retrieve topology domain is :{}",
                retrieveTopologyDto);
        String topologyRef = retrieveTopologyDto.getTopologyRef();
        PageResult<Node> pageResult = new PageResult<>();
        if (topologyRef.equals(PHY_TOPO_KEY)) {
            pageResult = retrievePhyLinkRefNode(retrieveTopologyDto);
        } else if (topologyRef.equals(SITE_TOPO_KEY)) {
            pageResult = retrieveSiteLinkRefNode(retrieveTopologyDto);
        } else if (topologyRef.equals(OCH_TOPO_KEY)) {
            pageResult = retrieveOchLinkRefNode(retrieveTopologyDto);
        } else {
            throw new CommonException(CommonExceptionType.NOT_SUPPORT_ERROR,
                    "can not support the topology type:" + topologyRef + " phy node retrieve ");
        }
        return pageResult;
    }

    private PageResult<Node> retrieveOchLinkRefNode(RetrieveTopologyDto retrieveTopologyDto) {
        log.debug("retrieve och");
        String linkId = retrieveTopologyDto.getLinkRef();
        Link ochLink = netconfTopology.getOchLink(linkId);
        if (ochLink == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can't find the required och link:" + linkId);
        }
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1 ochLinkPhysical = ochLink.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class);
        List<String> nodeIds = getRouteRefPhyNodeId(ochLinkPhysical.getOch().getExplictRoute());
        List<FilterItem> filterItems = buildFilterItemsWithPlane(retrieveTopologyDto,
                NMSConvertType.PHY_NODE);
        List<SortItem> sortItems = getSortItems(retrieveTopologyDto);
        int pageNum = retrieveTopologyDto.getPageNum();
        int pageSize = retrieveTopologyDto.getPageSize();
        return netconfTopology.retrieveAllPhyNodePagedByIds(nodeIds, pageNum, pageSize, filterItems,
                sortItems);
    }

    private PageResult<Node> retrieveSiteLinkRefNode(RetrieveTopologyDto retrieveTopologyDto) {
        log.debug("retrieve from site link");
        String linkId = retrieveTopologyDto.getLinkRef();
        Link siteLink = netconfTopology.getSiteLink(linkId);
        if (siteLink == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can't find the required site link:" + linkId);
        }

        Link1 siteLinkPhysical = siteLink.getAugmentation(Link1.class);
        List<String> nodeIds = getRouteRefPhyNodeId(siteLinkPhysical.getSite().getExplictRoute());
        List<FilterItem> filterItems = buildFilterItemsWithPlane(retrieveTopologyDto,
                NMSConvertType.PHY_NODE);
        List<SortItem> sortItems = getSortItems(retrieveTopologyDto);
        int pageNum = retrieveTopologyDto.getPageNum();
        int pageSize = retrieveTopologyDto.getPageSize();
        return netconfTopology.retrieveAllPhyNodePagedByIds(nodeIds, pageNum, pageSize, filterItems,
                sortItems);
    }

    private PageResult<Node> retrievePhyLinkRefNode(RetrieveTopologyDto retrieveTopologyDto) {
        log.debug("retrieve from phy link");
        String linkId = retrieveTopologyDto.getLinkRef();
        Link phyLink = netconfTopology.getPhyLink(linkId);
        if (phyLink == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can't find the required phy link:" + linkId);
        }
        List<String> nodeIds = getPhyLinkRefNode(Collections.singletonList(phyLink));
        List<FilterItem> filterItems = buildFilterItemsWithPlane(retrieveTopologyDto,
                NMSConvertType.PHY_NODE);
        List<SortItem> sortItems = getSortItems(retrieveTopologyDto);
        int pageNum = retrieveTopologyDto.getPageNum();
        int pageSize = retrieveTopologyDto.getPageSize();
        return netconfTopology.retrieveAllPhyNodePagedByIds(nodeIds, pageNum, pageSize, filterItems,
                sortItems);
    }

//    private PageResult<Node> retrieveOchLinkRefNode(Integer pageNum, Integer pageSize) {
//        log.debug("retrieve och");
//
//        Link ochLink = netconfTopology.getOchLink(linkId);
//        if (ochLink == null) {
//            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
//                    "can't find the required och link:" + linkId);
//        }
//        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1 ochLinkPhysical = ochLink.getAugmentation(
//                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class);
//        List<String> nodeIds = getRouteRefPhyNodeId(ochLinkPhysical.getOch().getExplictRoute());
//        return netconfTopology.retrieveAllPhyNodePagedByIds(nodeIds, pageNum, pageSize);
//    }
//
//
//    private PageResult<Node> retrieveSiteLinkRefNode(Integer pageNum, Integer pageSize) {
//        log.debug("retrieve from site link");
//        Link siteLink = netconfTopology.getSiteLink(linkId);
//        if (siteLink == null) {
//            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
//                    "can't find the required site link:" + linkId);
//        }
//
//        Link1 siteLinkPhysical = siteLink.getAugmentation(Link1.class);
//        List<String> nodeIds = getRouteRefPhyNodeId(siteLinkPhysical.getSite().getExplictRoute());
//        return netconfTopology.retrieveAllPhyNodePagedByIds(nodeIds, pageNum, pageSize);
//    }

//    private PageResult<Node> retrievePhyLinkRefNode(Integer pageNum, Integer pageSize) {
//        log.debug("retrieve from phy link");
//        Link phyLink = netconfTopology.getPhyLink(linkId);
//        if (phyLink == null) {
//            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
//                    "can't find the required phy link:" + linkId);
//        }
//        List<String> nodeIds = getPhyLinkRefNode(Collections.singletonList(phyLink));
//        return netconfTopology.retrieveAllPhyNodePagedByIds(nodeIds, pageNum, pageSize);
//    }


}

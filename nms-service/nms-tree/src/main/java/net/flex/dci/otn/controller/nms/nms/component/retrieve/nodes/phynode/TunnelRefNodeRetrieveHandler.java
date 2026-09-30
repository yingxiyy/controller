package net.flex.dci.otn.controller.nms.nms.component.retrieve.nodes.phynode;

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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/13 10:05
 */
@Slf4j
@Component
public class TunnelRefNodeRetrieveHandler extends AbstractNodeRetrieveHandler {

//    private final String tunnelId;

    public TunnelRefNodeRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
//        this.tunnelId = tunnelId;
    }

    @Override
    public PageResult<Node> retrieveAllNodePaged(Integer pageNum, Integer pageSize) {
//        log.info("retrieve all ref phy node from tunnel :{},pageNum:{},pageSize:{}", tunnelId,
//                pageNum, pageSize);
//        Tunnel tunnel = netconfTopology.getTunnel(tunnelId);
//        if (null == tunnel) {
//            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
//                    "can't find the required tunnel,tunnel id :" + tunnelId);
//        }
//
//        List<String> nodeIds = getRouteRefPhyNodeId(tunnel.getExplictRoute());
//        return netconfTopology.retrieveAllPhyNodePagedByIds(nodeIds, pageNum, pageSize);
        return PageResult.<Node>builder().build();
    }

    @Override
    public PageResult<Node> retrieveAllNodePaged(RetrieveTopologyDto retrieveTopologyDto) {
        log.debug("start to retrieve all node paged ,retrieve topology domain is :{}",
                retrieveTopologyDto);
        String tunnelId = retrieveTopologyDto.getTunnelRef();
        Tunnel tunnel = netconfTopology.getTunnel(tunnelId);
        if (null == tunnel) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can't find the required tunnel,tunnel id :" + tunnelId);
        }

        List<String> nodeIds = getRouteRefPhyNodeId(tunnel.getExplictRoute());
        List<FilterItem> filterItems = buildFilterItemsWithPlane(retrieveTopologyDto,
                NMSConvertType.PHY_NODE);
        List<SortItem> sortItems = getSortItems(retrieveTopologyDto);
        int pageNum = retrieveTopologyDto.getPageNum();
        int pageSize = retrieveTopologyDto.getPageSize();
        return netconfTopology.retrieveAllPhyNodePagedByIds(nodeIds, pageNum, pageSize, filterItems,
                sortItems);
    }

    @Override
    public List<Node> retrieveAllNode(RetrieveTopologyDto retrieveTopologyDto) {
        log.debug("start to retrieve all node  ,retrieve topology domain is :{}",
                retrieveTopologyDto);
        String tunnelId = retrieveTopologyDto.getTunnelRef();
        Tunnel tunnel = netconfTopology.getTunnel(tunnelId);
        if (null == tunnel) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can't find the required tunnel,tunnel id :" + tunnelId);
        }

        List<String> refPhyNodeIds = getRouteRefPhyNodeId(tunnel.getExplictRoute());
        List<Node> nodes = netconfTopology.retrieveAllPhyNodeByIds(refPhyNodeIds);
        return nodes;
    }
}

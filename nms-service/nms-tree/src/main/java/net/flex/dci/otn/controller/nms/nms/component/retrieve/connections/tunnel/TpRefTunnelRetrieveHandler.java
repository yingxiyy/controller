package net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.tunnel;

import static net.flex.dci.otn.controller.nms.utils.Constants.PHY_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_TOPO_KEY;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otc.mongo.dto.FilterItem;
import net.flex.dci.otc.mongo.dto.SortItem;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import net.flex.dci.otn.controller.nms.nms.enums.NMSConvertType;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/9 16:55
 */
@Slf4j
@Component
public class TpRefTunnelRetrieveHandler extends AbstractTunnelRetrieveHandler {

//    private final String topologyRef;
//
//    private final String nodeId;
//
//    private final String tpId;

    public TpRefTunnelRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
//        this.topologyRef = topologyRef;
//        this.nodeId = nodeId;
//        this.tpId = tpId;
    }

    @Override
    public PageResult<Tunnel> retrieveAllTunnelPaged(Integer pageNum, Integer pageSize) {
//        log.info("start to get all tunnel based on topology :{} ,nodeId :{} ,equipment tp:{}",
//                topologyRef, nodeId, tpId);
        PageResult<Tunnel> pageResult = new PageResult<>();
//        if (topologyRef.equals(PHY_TOPO_KEY)) {
//            pageResult = retrieveAllTunnelByPhyTpPaged(nodeId, tpId, pageNum, pageSize);
//        } else if (topologyRef.equals(SITE_TOPO_KEY)) {
//            pageResult = retrieveAllTunnelBySiteTpPaged(nodeId, tpId, pageNum, pageSize);
//        } else {
//            throw new CommonException(CommonExceptionType.NOT_SUPPORT_ERROR,
//                    "the topology " + topologyRef + " retrieve tunnel is not supported");
//        }
        return pageResult;
    }

    @Override
    public PageResult<Tunnel> retrieveAllTunnelPaged(RetrieveTopologyDto retrieveTopologyDto) {
        log.info("retrieve all topology tunnel retrieveTopology domain is:{}", retrieveTopologyDto);
        PageResult<Tunnel> pageResult = new PageResult<>();
        List<FilterItem> filterItems = buildFilterItemsByPlaneId(retrieveTopologyDto,
                NMSConvertType.TUNNEL);
        String topologyRef = retrieveTopologyDto.getTopologyRef();
        String nodeId = retrieveTopologyDto.getNodeRef();
        String tpId = retrieveTopologyDto.getTpRef();
        List<SortItem> sortItems = getSortItems(retrieveTopologyDto);
        int pageNum = retrieveTopologyDto.getPageNum();
        int pageSize = retrieveTopologyDto.getPageSize();
        if (topologyRef.equals(PHY_TOPO_KEY)) {
            pageResult = retrieveAllTunnelByPhyTpPaged(topologyRef, nodeId, tpId, pageNum, pageSize,
                    filterItems,
                    sortItems);
        } else if (topologyRef.equals(SITE_TOPO_KEY)) {
            pageResult = retrieveAllTunnelBySiteTpPaged(topologyRef, nodeId, tpId, pageNum,
                    pageSize,
                    filterItems, sortItems);
        } else {
            throw new CommonException(CommonExceptionType.NOT_SUPPORT_ERROR,
                    "the topology " + topologyRef + " retrieve tunnel is not supported");
        }
        return pageResult;
    }

    private PageResult<Tunnel> retrieveAllTunnelBySiteTpPaged(String topologyRef, String nodeId,
            String tpId,
            Integer pageNum, Integer pageSize, List<FilterItem> filterItems,
            List<SortItem> sortItems) {
        log.debug("retrieve all tunnel by site tp");
        TerminationPoint tp = netconfTopology.getTerminationPoint(topologyRef, nodeId, tpId);
        if (tp == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "the tp Id:" + tpId + " is not found");
        }

        List<String> tunnelIds = getTpRefTunnelIds(tp);
        return retrieveTunnelPagedByTunnelIds(tunnelIds, pageNum, pageSize,
                filterItems, sortItems);
    }


    private PageResult<Tunnel> retrieveAllTunnelByPhyTpPaged(String topologyRef, String nodeId,
            String tpId,
            Integer pageNum, Integer pageSize, List<FilterItem> filterItems,
            List<SortItem> sortItems) {
        log.debug("retrieve all tunnel by phy tp");
        TerminationPoint tp = netconfTopology.getTerminationPoint(topologyRef, nodeId, tpId);
        if (tp == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "the tp Id:" + tpId + " is not found");
        }

        List<String> tunnelIds = getTpRefTunnelIds(tp);
        return retrieveTunnelPagedByTunnelIds(tunnelIds, pageNum, pageSize,
                filterItems, sortItems);
    }


}

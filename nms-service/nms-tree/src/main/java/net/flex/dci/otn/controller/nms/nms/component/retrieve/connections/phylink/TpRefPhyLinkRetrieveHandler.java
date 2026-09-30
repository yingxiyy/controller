package net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.phylink;

import static net.flex.dci.otn.controller.nms.utils.Constants.PHY_TOPO_KEY;

import java.util.List;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/8 14:16
 */
@Slf4j
@Component
public class TpRefPhyLinkRetrieveHandler extends AbstractNMSRetrieveHandler {

//    private final String topologyRef;
//
//    private final String nodeId;
//
//    private final String tpId;

    public TpRefPhyLinkRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
//        this.topologyRef = topologyRef;
//        this.nodeId = nodeRef;
//        this.tpId = tpRef;
    }


    @Override
    public PageResult<Link> retrieveAllLinkPaged(Integer pageNum, Integer pageSize) {
//        log.debug(
//                "retrieve all relative phy link by tp ,topology ref is :{},nodeId ref is :{},tpId is{}",
//                topologyRef, nodeId, tpId);
//        PageResult<Link> pageResult = new PageResult<>();
//        String nodeId = this.nodeId;
//        if (topologyRef.equals(SITE_TOPO_KEY)) {
//            nodeId = getToPhyNodeId(tpId);
//        }
//        pageResult = retrieveRefPhyPaged(nodeId, tpId, pageNum, pageSize);

        return PageResult.<Link>builder().build();
    }

    @Override
    public PageResult<Link> retrieveAllLinkPaged(RetrieveTopologyDto retrieveTopologyDto) {
//        return new PageResult<>();
        String nodeId = retrieveTopologyDto.getNodeRef();
        String tpId = retrieveTopologyDto.getTpRef();
        String topologyRef = retrieveTopologyDto.getTopologyRef();
        TerminationPoint tp = netconfTopology.getTerminationPoint(PHY_TOPO_KEY, nodeId, tpId);
        if (null == tp) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "cannot find topology:" + topologyRef + " node id:" + nodeId + " required TP. "
                            + tpId);
        }
        List<FilterItem> filterItems = buildFilterItemsByPlaneId(retrieveTopologyDto,
                NMSConvertType.PHY_LINK);
        List<SortItem> sortItems = getSortItems(retrieveTopologyDto);
        PageResult<Link> pageResult = netconfTopology.retrievePhyLinksUnderByTpPaged(
                tpId,
                retrieveTopologyDto.getPageNum(), retrieveTopologyDto.getPageSize(), filterItems,
                sortItems);
        return pageResult;
    }

//    private PageResult<Link> retrieveRefPhyPaged(String nodeId, String tpId, Integer pageNum,
//            Integer pageSize) {
//        TerminationPoint tp = netconfTopology.getTerminationPoint(PHY_TOPO_KEY, nodeId, tpId);
//        if (null == tp) {
//            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
//                    "cannot find topology:" + topologyRef + " node id:" + nodeId + " required TP. "
//                            + tpId);
//        }
//        PageResult<Link> pageResult = netconfTopology.retrievePhyLinksUnderByTpPaged(tpId,
//                pageNum, pageSize);
//        return pageResult;
//    }

    private String getToPhyNodeId(String tpId) {
        String[] ids = tpId.split("#");
        return ids[0] + "#" + ids[1];
    }
}

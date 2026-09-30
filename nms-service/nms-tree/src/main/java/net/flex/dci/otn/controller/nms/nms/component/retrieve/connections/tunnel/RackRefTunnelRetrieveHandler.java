package net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.tunnel;

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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRack;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/9 16:51
 */
@Slf4j
@Component
public class RackRefTunnelRetrieveHandler extends AbstractTunnelRetrieveHandler {

//    private final String nodeId;
//
//    private final String rackId;

    public RackRefTunnelRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
//        this.nodeId = nodeId;
//        this.rackId = rackId;
    }

    @Override
    public PageResult<Tunnel> retrieveAllTunnelPaged(Integer pageNum, Integer pageSize) {
//        log.info(
//                "start retrieve all tunnel under site id:{} rack:{} pageNum is :{} pageSize is :{}",
//                nodeId, rackId, pageNum, pageSize);
//        SupportingRack rack = netconfTopology.getRack(nodeId, rackId);
//        if (rack == null) {
//            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
//                    "can not found the supporting rack :" + rackId + " from site Node is :"
//                            + nodeId);
//        }
//        List<Node> nodes = getSupportingNode(rack);
//        List<String> tunnelIds = getPhyNodeRefTunnelIds(nodes);
//        return netconfTopology.retrieveTunnelPagedByTunnelIds(tunnelIds, pageNum, pageSize);
        return PageResult.<Tunnel>builder().build();
    }

    @Override
    public PageResult<Tunnel> retrieveAllTunnelPaged(RetrieveTopologyDto retrieveTopologyDto) {
        log.info("retrieve all topology tunnel retrieveTopology domain is:{}", retrieveTopologyDto);
        List<FilterItem> filterItems = buildFilterItemsByPlaneId(retrieveTopologyDto,
                NMSConvertType.TUNNEL);
        List<SortItem> sortItems = getSortItems(retrieveTopologyDto);
        String nodeId = retrieveTopologyDto.getNodeRef();
        String rackId = retrieveTopologyDto.getRackRef();
        int pageNum = retrieveTopologyDto.getPageNum();
        int pageSize = retrieveTopologyDto.getPageSize();
        SupportingRack rack = netconfTopology.getRack(nodeId, rackId);
        if (rack == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can not found the supporting rack :" + rackId + " from site Node is :"
                            + nodeId);
        }
        List<Node> nodes = getSupportingNode(rack);
        List<String> tunnelIds = getPhyNodeRefTunnelIds(nodes);
        return retrieveTunnelPagedByTunnelIds(tunnelIds, pageNum, pageSize,
                filterItems, sortItems);
    }

    /**
     * get ref supporting node
     *
     * @param rack
     * @return
     */
    private List<Node> getSupportingNode(SupportingRack rack) {
        List<String> spNeIds = rack.getSupportingNe().stream()
                .map(supportingNe -> supportingNe.getNodeRef().getValue()).collect(
                        Collectors.toList());
        return netconfTopology.getPhyNodesByIds(spNeIds);
    }
}

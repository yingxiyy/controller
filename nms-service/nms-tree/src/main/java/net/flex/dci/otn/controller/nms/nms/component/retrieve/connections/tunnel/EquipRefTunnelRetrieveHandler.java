package net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.tunnel;

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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/9 16:54
 */
@Slf4j
@Component
public class EquipRefTunnelRetrieveHandler extends AbstractTunnelRetrieveHandler {

//    private final String nodeId;
//
//    private final String equipId;

    public EquipRefTunnelRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
//        this.nodeId = nodeId;
//        this.equipId = equipId;
    }

//    @Override
//    public PageResult<Tunnel> retrieveAllTunnelPaged(Integer pageNum, Integer pageSize) {
//        log.info("retrieve under the equipment equip id is :{},pageNum is :{},pageSize is:{}",
//                equipId, pageNum, pageSize);
//        Equipments equipments = netconfTopology.getEquipment(nodeId, equipId);
//        if (equipments == null) {
//            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
//                    "can't find the required equipment id is :" + equipId);
//        }
//        List<Link> refPhyLinks = netconfTopology.getEquipmentRefPhyLinks(
//                equipments.getEquipmentId());
//        List<String> tunnelIds = getPhyLinkRefTunnelIds(refPhyLinks);
//        return netconfTopology.retrieveTunnelPagedByTunnelIds(tunnelIds, pageNum, pageSize);
//    }

    @Override
    public PageResult<Tunnel> retrieveAllTunnelPaged(RetrieveTopologyDto retrieveTopologyDto) {
        log.info("retrieve all topology tunnel retrieveTopology domain is:{}", retrieveTopologyDto);
        List<FilterItem> filterItems = buildFilterItemsByPlaneId(retrieveTopologyDto,
                NMSConvertType.TUNNEL);
        List<SortItem> sortItems = getSortItems(retrieveTopologyDto);
        int pageNum = retrieveTopologyDto.getPageNum();
        int pageSize = retrieveTopologyDto.getPageSize();
        String nodeId = retrieveTopologyDto.getNodeRef();
        String equipId = retrieveTopologyDto.getEquipRef();
        Equipments equipments = netconfTopology.getEquipment(nodeId, equipId);
        if (equipments == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can't find the required equipment id is :" + equipId);
        }
        List<Link> refPhyLinks = netconfTopology.getEquipmentRefPhyLinks(
                equipments.getEquipmentId());
        List<String> tunnelIds = getPhyLinkRefTunnelIds(refPhyLinks);
        return retrieveTunnelPagedByTunnelIds(tunnelIds, pageNum, pageSize,
                filterItems, sortItems);
    }


}

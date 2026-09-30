package net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.phylink;

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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/8 14:19
 */
@Slf4j
@Component
public class EquipRefPhyLinkRetrieveHandler extends AbstractNMSRetrieveHandler {

//    private final String nodeId;
//
//    private final String equipId;

    public EquipRefPhyLinkRetrieveHandler(NetconfTopology netconfTopology) {
        super(netconfTopology);
//        this.nodeId = nodeRef;
//        this.equipId = equipRef;
    }

    @Override
    public PageResult<Link> retrieveAllLinkPaged(Integer pageNum, Integer pageSize) {
//        log.debug("start get all PHY link based on Equipment. with  node:{}, equip: {}",
//                nodeId, equipId);
//        Equipments equip = netconfTopology.getEquipment(
//                nodeId, equipId);
//        if (null == equip) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    "cannot find required equipment.nodeId is : " + nodeId + ",equipId is:"
//                            + equipId);
//        }
//        PageResult<Link> pageResult = netconfTopology.retrievePhyLinksUnderByEquipPaged(equipId,
//                pageNum, pageSize);
        return PageResult.<Link>builder().build();
    }

    @Override
    public PageResult<Link> retrieveAllLinkPaged(RetrieveTopologyDto retrieveTopologyDto) {
        String nodeId = retrieveTopologyDto.getNodeRef();
        String equipId = retrieveTopologyDto.getEquipRef();
        log.debug("start get all PHY link based on Equipment paged. with  node:{}, equip: {}",
                nodeId, equipId);
        Equipments equip = netconfTopology.getEquipment(
                nodeId, equipId);
        if (null == equip) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "cannot find required equipment.nodeId is : " + nodeId + ",equipId is:"
                            + equipId);
        }
        List<FilterItem> filterItems = buildFilterItemsByPlaneId(retrieveTopologyDto,
                NMSConvertType.PHY_LINK);
        List<SortItem> sortItems = getSortItems(retrieveTopologyDto);
        int pageNum = retrieveTopologyDto.getPageNum();
        int pageSize = retrieveTopologyDto.getPageSize();
        return netconfTopology.retrievePhyLinksUnderByEquipPaged(equipId, pageNum, pageSize,
                filterItems, sortItems);
    }

}

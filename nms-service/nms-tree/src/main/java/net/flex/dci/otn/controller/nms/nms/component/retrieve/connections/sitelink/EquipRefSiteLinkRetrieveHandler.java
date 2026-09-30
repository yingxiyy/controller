package net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.sitelink;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/11 16:19
 */
@Slf4j
@Component
public class EquipRefSiteLinkRetrieveHandler extends AbstractSiteLinkRetrieveHandler {

//    private String nodeId;
//
//    private String equipId;

    public EquipRefSiteLinkRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
//        this.nodeId = nodeId;
//        this.equipId = equipId;
    }

    @Override
    public PageResult<Link> retrieveAllSiteLinkPaged(Integer pageNum, Integer pageSize) {
//        log.info(
//                "retrieve all site link by equipment ,nodeId :{},equipId:{},pageNum is :{},pageSize is :{}",
//                nodeId, equipId, pageNum, pageSize);
//        Equipments equipment = netconfTopology.getEquipment(nodeId, equipId);
//        if (equipment == null) {
//            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
//                    "can't found the equipment :" + equipId + "from the phyNode:" + nodeId);
//        }
//        List<Link> eqRefPhyLinks = netconfTopology.getEquipmentRefPhyLinks(equipId);
//        List<String> refSiteLinkIds = getPhyLinkRefSiteLinkIds(eqRefPhyLinks);
//        return netconfTopology.retrieveAllSiteLinkByIdsPaged(refSiteLinkIds, pageNum, pageSize);
        return PageResult.<Link>builder().build();
    }

    @Override
    public PageResult<Link> retrieveAllSiteLinkPaged(RetrieveTopologyDto retrieveTopologyDto) {
        String nodeId = retrieveTopologyDto.getNodeRef();
        String equipId = retrieveTopologyDto.getEquipRef();
        log.info(
                "retrieve all site link by equipment ,nodeId :{},equipId:{}",
                nodeId, equipId);
        Equipments equipment = netconfTopology.getEquipment(nodeId, equipId);
        if (equipment == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can't found the equipment :" + equipId + "from the phyNode:" + nodeId);
        }
        List<Link> eqRefPhyLinks = netconfTopology.getEquipmentRefPhyLinks(equipId);
        List<String> refSiteLinkIds = getPhyLinkRefSiteLinkIds(eqRefPhyLinks);
        return retrieveAllSiteLinkByIdsPaged(refSiteLinkIds, retrieveTopologyDto);
    }
}

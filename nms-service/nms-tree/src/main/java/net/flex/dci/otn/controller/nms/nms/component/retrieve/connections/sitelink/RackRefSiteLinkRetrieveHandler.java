package net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.sitelink;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRack;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/11 15:52
 */
@Slf4j
@Component
public class RackRefSiteLinkRetrieveHandler extends AbstractSiteLinkRetrieveHandler {


    public RackRefSiteLinkRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);

    }

    @Override
    public PageResult<Link> retrieveAllSiteLinkPaged(Integer pageNum, Integer pageSize) {
//        log.info(
//                "list all site link paged by rack ,site node :{},rack id :{},pageNum is :{},pageSize is :{}",
//                nodeId, rackId, pageNum, pageSize);
//        SupportingRack rack = netconfTopology.getRack(nodeId, rackId);
//        if (rack == null) {
//            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
//                    "can not find required rack :" + rackId + "site node :" + nodeId);
//        }
//        List<String> supportNeIds = rack.getSupportingNe().stream()
//                .map(supportingNe -> supportingNe.getNodeRef().getValue()).collect(
//                        Collectors.toList());
//        List<String> siteLinkIds = getPhyNodeRefSiteLinkIds(supportNeIds);
//        PageResult<Link> pageResult = netconfTopology.retrieveAllSiteLinkByIdsPaged(siteLinkIds,
//                pageNum,
//                pageSize);
//        return pageResult;
        return PageResult.<Link>builder().build();
    }

    @Override
    public PageResult<Link> retrieveAllSiteLinkPaged(RetrieveTopologyDto retrieveTopologyDto) {
        String nodeId = retrieveTopologyDto.getNodeRef();
        String rackId = retrieveTopologyDto.getRackRef();
        log.info(
                "list all site link paged by rack ,site node :{},rack id :{}",
                nodeId, rackId);
        SupportingRack rack = netconfTopology.getRack(nodeId, rackId);
        if (rack == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can not find required rack :" + rackId + "site node :" + nodeId);
        }
        List<String> supportNeIds = rack.getSupportingNe().stream()
                .map(supportingNe -> supportingNe.getNodeRef().getValue()).collect(
                        Collectors.toList());
        List<String> siteLinkIds = getPhyNodeRefSiteLinkIds(supportNeIds);
        return retrieveAllSiteLinkByIdsPaged(siteLinkIds, retrieveTopologyDto);
    }
}

package net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.sitelink;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/11 16:17
 */
@Slf4j
@Component
public class TpRefSiteLinkRetrieveHandler extends AbstractSiteLinkRetrieveHandler {

//    private String topologyRef;
//
//    private String nodeId;
//
//    private String tpId;

    public TpRefSiteLinkRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
//        this.topologyRef = topologyRef;
//        this.nodeId = nodeId;
//        this.tpId = tpId;
    }

    @Override
    public PageResult<Link> retrieveAllSiteLinkPaged(Integer pageNum, Integer pageSize) {
//        log.info("start to retrieve tp ref site links paged,topology:{},nodeId :{},tpId :{}",
//                topologyRef, nodeId, tpId);
//        TerminationPoint tp = netconfTopology.getTerminationPoint(topologyRef, nodeId, tpId);
//        if (tp == null) {
//            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
//                    "can't find the required tp id :" + tpId);
//        }
//        List<Link> phyLinks = getTpRefPhyLinks(nodeId, tpId);
//        List<String> siteLinkIds = getPhyLinkRefSiteLinkIds(phyLinks);
//        return netconfTopology.retrieveAllSiteLinkByIdsPaged(siteLinkIds, pageNum, pageSize);
        return PageResult.<Link>builder().build();
    }

    @Override
    public PageResult<Link> retrieveAllSiteLinkPaged(RetrieveTopologyDto retrieveTopologyDto) {
        String topologyRef = retrieveTopologyDto.getTopologyRef();
        String nodeId = retrieveTopologyDto.getNodeRef();
        String tpId = retrieveTopologyDto.getTpRef();
        log.info("start to retrieve tp ref site links paged,topology:{},nodeId :{},tpId :{}",
                topologyRef, nodeId, tpId);
        TerminationPoint tp = netconfTopology.getTerminationPoint(topologyRef, nodeId, tpId);
        if (tp == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can't find the required tp id :" + tpId);
        }
        List<Link> phyLinks = getTpRefPhyLinks(nodeId, tpId);
        List<String> siteLinkIds = getPhyLinkRefSiteLinkIds(phyLinks);
        return retrieveAllSiteLinkByIdsPaged(siteLinkIds, retrieveTopologyDto);
    }
}

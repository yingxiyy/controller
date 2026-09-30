package net.flex.dci.otn.controller.nms.nms.component.retrieve.nodes.sitenode;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRoute;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/13 14:47
 */
@Slf4j
@Component
public class TunnelRefSiteNodeRetrieveHandler extends AbstractSiteNodeRetrieveHandler {


    public TunnelRefSiteNodeRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

//    @Override
//    public PageResult<Node> retrieveAllNodePaged(Integer pageNum, Integer pageSize) {
//        log.info("retrieve all site node for the tunnel :{},pageNum:{},pageSize:{}", tunnelId,
//                pageNum, pageSize);
//        Tunnel tunnel = netconfTopology.getTunnel(tunnelId);
//        if (null == tunnel) {
//            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
//                    "can't find the required tunnel id:" + tunnelId);
//        }
//        List<String> siteNodeIds = getTunnelRefSiteIds(tunnel);
//        return netconfTopology.retrieveAllSiteNodePagedByIds(siteNodeIds, pageNum,
//                pageSize);
//    }

    @Override
    public PageResult<Node> retrieveAllNodePaged(RetrieveTopologyDto retrieveTopologyDto) {
        log.debug("start to retrieve all node paged ,retrieve topology domain is :{}",
                retrieveTopologyDto);
        String tunnelId = retrieveTopologyDto.getTunnelRef();
        Tunnel tunnel = netconfTopology.getTunnel(tunnelId);
        if (null == tunnel) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can't find the required tunnel id:" + tunnelId);
        }
        List<String> siteNodeIds = getTunnelRefSiteIds(tunnel);
        return retrieveRefSiteNode(siteNodeIds, retrieveTopologyDto);
    }

    private List<String> getTunnelRefSiteIds(Tunnel tunnel) {
        ExplictRoute explictRoute = tunnel.getExplictRoute();
        List<String> refSiteNodeIds = getRouteRefSiteId(explictRoute);
        return refSiteNodeIds;
    }


}

package net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.sitelink;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/11 13:44
 */
@Slf4j
@Component
public class TunnelRefSiteLinkRetrieveHandler extends AbstractSiteLinkRetrieveHandler {

//    private final String tunnelId;

    public TunnelRefSiteLinkRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
//        this.tunnelId = tunnelId;
    }


    @Override
    public PageResult<Link> retrieveAllSiteLinkPaged(Integer pageNum, Integer pageSize) {
//        log.info("start to retrieve all site link paged by tunnel :{},pageNum :{},pageSize:{}",
//                tunnelId, pageNum, pageSize);
//        Tunnel tunnel = netconfTopology.getTunnel(tunnelId);
//        if (null == tunnel) {
//            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
//                    "can not found the tunnel :" + tunnelId);
//        }
//        Set<String> supportOchLinkIds = tunnel.getSupportingLink().stream()
//                .map(supportingLink -> supportingLink.getLinkRef().getValue()).collect(
//                        Collectors.toSet());
//        List<Link> ochLinks = netconfTopology.getOchLinksByIds(supportOchLinkIds);
//        List<String> siteLinks = getOchRefSiteLinkIds(ochLinks);
//        return netconfTopology.retrieveAllSiteLinkByIdsPaged(siteLinks, pageNum, pageSize);
        return PageResult.<Link>builder().build();
    }

    @Override
    public PageResult<Link> retrieveAllSiteLinkPaged(RetrieveTopologyDto retrieveTopologyDto) {
        String tunnelId = retrieveTopologyDto.getTunnelRef();
        int pageNum = retrieveTopologyDto.getPageNum();
        int pageSize = retrieveTopologyDto.getPageSize();
        log.info("start to retrieve all site link paged by tunnel :{},pageNum :{},pageSize:{}",
                tunnelId, pageNum, pageSize);
        Tunnel tunnel = netconfTopology.getTunnel(tunnelId);
        if (null == tunnel) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can not found the tunnel :" + tunnelId);
        }
        Set<String> supportOchLinkIds = tunnel.getSupportingLink().stream()
                .map(supportingLink -> supportingLink.getLinkRef().getValue()).collect(
                        Collectors.toSet());
        List<Link> ochLinks = netconfTopology.getOchLinksByIds(supportOchLinkIds);
        List<String> siteLinks = getOchRefSiteLinkIds(ochLinks);
        return retrieveAllSiteLinkByIdsPaged(siteLinks, retrieveTopologyDto);
    }


}

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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/8 14:10
 */
@Slf4j
@Component
public class TunnelRefPhyLinkRetrieveHandler extends AbstractNMSRetrieveHandler {

//    private final String tunnelId;

    public TunnelRefPhyLinkRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
//        this.tunnelId = tunnelId;
    }

    @Override
    public PageResult<Link> retrieveAllLinkPaged(Integer pageNum, Integer pageSize) {
//        log.debug("retrieve the tunnel id :{} ,ref phy link ,page num :{},page size :{}", tunnelId,
//                pageNum, pageSize);
//        Tunnel tunnel = netconfTopology.getTunnel(new Uri(tunnelId));
//        if (tunnel == null) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    "cannot find required Tunnel link: " + tunnelId);
//        }
//        PageResult<Link> result = getTunnelRefLinkPaged(tunnel, pageNum, pageSize);
//        return result;
        return PageResult.<Link>builder().build();
    }

    @Override
    public PageResult<Link> retrieveAllLinkPaged(RetrieveTopologyDto retrieveTopologyDto) {
        String tunnelId = retrieveTopologyDto.getTunnelRef();
        Tunnel tunnel = netconfTopology.getTunnel(new Uri(tunnelId));
        if (tunnel == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "cannot find required Tunnel link: " + tunnelId);
        }
        List<String> refPhyLinksId = getTunnelRefPhyLinkIds(tunnel);
        List<FilterItem> filterItems = buildFilterItemsByPlaneId(retrieveTopologyDto,
                NMSConvertType.PHY_LINK);
        List<SortItem> sortItems = getSortItems(retrieveTopologyDto);
        return netconfTopology.retrieveAllPhyLinkByLinkIdsPaged(
                refPhyLinksId,
                retrieveTopologyDto.getPageNum(), retrieveTopologyDto.getPageSize(), filterItems,
                sortItems);
    }

    private PageResult<Link> getTunnelRefLinkPaged(Tunnel tunnel, Integer pageNum,
            Integer pageSize) {
        List<String> refPhyLinksId = getTunnelRefPhyLinkIds(tunnel);
        return netconfTopology.retrieveAllPhyLinkByLinkIdsPaged(refPhyLinksId, pageNum, pageSize);
    }


}

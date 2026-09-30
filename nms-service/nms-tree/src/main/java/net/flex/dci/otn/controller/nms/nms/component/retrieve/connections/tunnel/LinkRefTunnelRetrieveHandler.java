package net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.tunnel;

import static net.flex.dci.otn.controller.nms.utils.Constants.OCH_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.PHY_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_TOPO_KEY;

import java.util.Collections;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/9 16:58
 */
@Slf4j
@Component
public class LinkRefTunnelRetrieveHandler extends AbstractTunnelRetrieveHandler {

//    private final String topologyRef;
//
//    private final String linkId;

    public LinkRefTunnelRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
//        this.linkId = linkId;
//        this.topologyRef = topologyRef;
    }

    @Override
    public PageResult<Tunnel> retrieveAllTunnelPaged(Integer pageNum, Integer pageSize) {
//        log.info(
//                "retrieve all topology tunnel by link topology is :{} ,linkId is {} ,pageNum is {},pageSize is :{}",
//                topologyRef, linkId, pageNum, pageSize);
        PageResult<Tunnel> pageResult = new PageResult<>();
//        if (topologyRef.equals(PHY_TOPO_KEY)) {
//            pageResult = retrieveTunnelPagedByPhyLink(linkId, pageNum, pageSize);
//        } else if (topologyRef.equals(SITE_TOPO_KEY)) {
//            pageResult = retrieveTunnelPagedBySiteLink(linkId, pageNum, pageSize);
//        } else if (topologyRef.equals(OCH_TOPO_KEY)) {
//            pageResult = retrieveTunnelPagedByOCHLink(linkId, pageNum, pageSize);
//        } else {
//            throw new CommonException(CommonExceptionType.NOT_SUPPORT_ERROR,
//                    "can't support retrieve all the tunnel through the topology:"
//                            + topologyRef);
//        }

        return pageResult;
    }

    @Override
    public PageResult<Tunnel> retrieveAllTunnelPaged(RetrieveTopologyDto retrieveTopologyDto) {
        log.info("retrieve all topology tunnel retrieveTopology domain is:{}", retrieveTopologyDto);
        PageResult<Tunnel> pageResult = new PageResult<>();
        List<FilterItem> filterItems = buildFilterItemsByPlaneId(retrieveTopologyDto,
                NMSConvertType.TUNNEL);
        List<SortItem> sortItems = getSortItems(retrieveTopologyDto);
        int pageNum = retrieveTopologyDto.getPageNum();
        int pageSize = retrieveTopologyDto.getPageSize();
        String topologyRef = retrieveTopologyDto.getTopologyRef();
        String linkId = retrieveTopologyDto.getLinkRef();
        if (topologyRef.equals(PHY_TOPO_KEY)) {
            pageResult = retrieveTunnelPagedByPhyLink(linkId, pageNum, pageSize, filterItems,
                    sortItems);
        } else if (topologyRef.equals(SITE_TOPO_KEY)) {
            pageResult = retrieveTunnelPagedBySiteLink(linkId, pageNum, pageSize, filterItems,
                    sortItems);
        } else if (topologyRef.equals(OCH_TOPO_KEY)) {
            pageResult = retrieveTunnelPagedByOCHLink(linkId, pageNum, pageSize, filterItems,
                    sortItems);
        } else {
            throw new CommonException(CommonExceptionType.NOT_SUPPORT_ERROR,
                    "can't support retrieve all the tunnel through the topology:"
                            + topologyRef);
        }

        return pageResult;
    }

    private PageResult<Tunnel> retrieveTunnelPagedByOCHLink(String linkId, Integer pageNum,
            Integer pageSize, List<FilterItem> filterItems, List<SortItem> sortItems) {
        log.debug("retrieve tunnel ref och link");
        Link ochLink = netconfTopology.getOchLink(linkId);
        if (null == ochLink) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can not found the och link id :" + linkId);
        }

        List<String> supportTunnelIds = getOchLinkSupportTunnelIds(ochLink);
        PageResult<Tunnel> pageResult = retrieveTunnelPagedByTunnelIds(
                supportTunnelIds, pageNum, pageSize, filterItems, sortItems);
        return pageResult;
    }

    private PageResult<Tunnel> retrieveTunnelPagedBySiteLink(String linkId, Integer pageNum,
            Integer pageSize, List<FilterItem> filterItems, List<SortItem> sortItems) {
        log.debug("retrieve tunnel ref site link");
        Link siteLink = netconfTopology.getSiteLink(linkId);
        if (null == siteLink) {
            throw new CommonException(CommonExceptionType.NOT_SUPPORT_ERROR,
                    "can not found the site link id:" + linkId);
        }

        List<String> supportTunnelIds = getSiteLinkSupportTunnelIds(siteLink);

        return retrieveTunnelPagedByTunnelIds(supportTunnelIds, pageNum, pageSize,
                filterItems, sortItems);
    }


    private PageResult<Tunnel> retrieveTunnelPagedByPhyLink(String linkId, Integer pageNum,
            Integer pageSize, List<FilterItem> filterItems, List<SortItem> sortItems) {
        log.debug("retrieve tunnel  ref phylink");
        Link phyLink = netconfTopology.getPhyLink(linkId);
        if (null == phyLink) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can not found the phy link id:" + linkId);
        }
        List<String> tunnelIds = getPhyLinkRefTunnelIds(Collections.singletonList(phyLink));
        return retrieveTunnelPagedByTunnelIds(tunnelIds, pageNum, pageSize,
                filterItems, sortItems);
    }


}

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
import org.springframework.stereotype.Component;

/**
 * site link ref link retrieve
 *
 * @version 1.0
 * @date 2022/3/8 14:14
 */
@Slf4j
@Component
public class SiteLinkRefPhyLinkRetrieveHandler extends AbstractNMSRetrieveHandler {


    public SiteLinkRefPhyLinkRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);

    }

    @Override
    public PageResult<Link> retrieveAllLinkPaged(Integer pageNum, Integer pageSize) {
//        log.debug("list all ref site link {}  phy link paged ,pageNum is :{},pageSize is:{}",
//                linkId, pageNum, pageSize);
//        Link siteLink = netconfTopology.getSiteLink(linkId);
//        if (siteLink == null) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    "cannot find required SITE link: " + linkId);
//        }
//        PageResult<Link> pagedPhyLink = getRefPhyLinkPaged(siteLink, pageNum, pageSize);
//        return pagedPhyLink;
        return PageResult.<Link>builder().build();
    }

    @Override
    public PageResult<Link> retrieveAllLinkPaged(RetrieveTopologyDto retrieveTopologyDto) {
        String linkId = retrieveTopologyDto.getLinkRef();
        log.debug("list all ref site link {}  phy link paged ,pageNum is :{},pageSize is:{}",
                linkId, retrieveTopologyDto.getPageNum(), retrieveTopologyDto.getPageSize());
        Link siteLink = netconfTopology.getSiteLink(linkId);
        if (siteLink == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "cannot find required SITE link: " + linkId);
        }
        List<FilterItem> filterItems = buildFilterItemsByPlaneId(retrieveTopologyDto,
                NMSConvertType.PHY_LINK);
        List<SortItem> sortItems = getSortItems(retrieveTopologyDto);
        List<String> refPhyLinkIds = getSiteLinkRefPhyLinkIds(siteLink);
        PageResult<Link> pageResult = netconfTopology.retrieveAllPhyLinkByLinkIdsPaged(
                refPhyLinkIds,
                retrieveTopologyDto.getPageNum(), retrieveTopologyDto.getPageSize(), filterItems,
                sortItems);
        return pageResult;
    }

    private PageResult<Link> getRefPhyLinkPaged(Link siteLink, Integer pageNum, Integer pageSize) {
        log.debug("list all phy link paged ");
        List<String> refPhyLinkIds = getSiteLinkRefPhyLinkIds(siteLink);
        PageResult<Link> pageResult = netconfTopology.retrieveAllPhyLinkByLinkIdsPaged(
                refPhyLinkIds, pageNum, pageSize);
        return pageResult;
    }
}

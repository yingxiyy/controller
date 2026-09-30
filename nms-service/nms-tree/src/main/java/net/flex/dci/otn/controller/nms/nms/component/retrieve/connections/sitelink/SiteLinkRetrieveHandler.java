package net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.sitelink;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
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
 * @version 1.0
 * @date 2022/3/7 14:43
 */
@Slf4j
@Component
public class SiteLinkRetrieveHandler extends AbstractNMSRetrieveHandler {


    public SiteLinkRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

    @Override
    public PageResult<Link> retrieveAllSiteLinkPaged(Integer pageNum, Integer pageSize) {
        log.info("list all site link paged,pageNum is :{},pageSize is :{}", pageNum, pageSize);
        PageResult<Link> pageResult = netconfTopology.retrieveAllSiteLinkPaged(pageNum, pageSize);
        return pageResult;
    }

    @Override
    public PageResult<Link> retrieveAllSiteLinkPaged(RetrieveTopologyDto retrieveTopologyDto) {
        log.info("list all site link paged");
        List<FilterItem> filterItems = buildFilterItemsByPlaneId(retrieveTopologyDto,
                NMSConvertType.SITE_LINK);
        List<SortItem> sortItems = getSortItems(retrieveTopologyDto);
        return netconfTopology.retrieveAllSiteLinkPaged(retrieveTopologyDto.getPageNum(),
                retrieveTopologyDto.getPageSize(), filterItems, sortItems);
    }

}

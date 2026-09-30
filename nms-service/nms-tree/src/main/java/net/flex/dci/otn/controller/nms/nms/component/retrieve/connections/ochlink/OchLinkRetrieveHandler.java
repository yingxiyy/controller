package net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.ochlink;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otc.mongo.dto.FilterItem;
import net.flex.dci.otc.mongo.dto.SortItem;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import net.flex.dci.otn.controller.nms.nms.enums.NMSConvertType;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/12 20:53
 */
@Slf4j
@Component
public class OchLinkRetrieveHandler extends AbstractOchLinkRetrieveHandler {

    public OchLinkRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
    }


    @Override
    public PageResult<Link> retrieveAllOchLinkPaged(Integer pageNum, Integer pageSize) {
        log.info("retrieve all the och link paged ,pageNum :{},pageSize:{}", pageNum, pageSize);
        return netconfTopology.retrieveAllOchLinkPaged(pageNum, pageSize);
    }

    @Override
    public PageResult<Link> retrieveAllOchLinkPaged(RetrieveTopologyDto retrieveTopologyDto) {
        log.info("retrieve all the och link paged ,pageNum :{},pageSize:{}",
                retrieveTopologyDto.getPageNum(), retrieveTopologyDto.getPageSize());
        List<FilterItem> filterItems = buildFilterItemsByPlaneId(retrieveTopologyDto,
                NMSConvertType.OCH_LINK);
        List<SortItem> sortItems = getSortItems(retrieveTopologyDto);
        return netconfTopology.retrieveAllOchLinkPaged(retrieveTopologyDto.getPageNum(),
                retrieveTopologyDto.getPageSize(), filterItems, sortItems);
    }
}

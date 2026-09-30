package net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.phylink;

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
 * @date 2022/3/8 10:38
 */
@Slf4j
@Component
public class PhyLinkRetrieveHandler extends AbstractNMSRetrieveHandler {

    public PhyLinkRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

    @Override
    public PageResult<Link> retrieveAllLinkPaged(Integer pageNum, Integer pageSize) {
        log.debug("list all phy link paged ,pageNum is :{},pageSize is:{}", pageNum, pageSize);
        return netconfTopology.retrieveAllPhyLinkPaged(pageNum, pageSize);
    }

    @Override
    public PageResult<Link> retrieveAllLinkPaged(RetrieveTopologyDto retrieveTopologyDto) {
        List<FilterItem> filterItems = buildFilterItemsByPlaneId(retrieveTopologyDto,
                NMSConvertType.PHY_LINK);
        List<SortItem> sortItems = getSortItems(retrieveTopologyDto);
        return netconfTopology.retrieveAllPhyLinkPaged(retrieveTopologyDto.getPageNum(),
                retrieveTopologyDto.getPageSize(), filterItems, sortItems);
    }
}

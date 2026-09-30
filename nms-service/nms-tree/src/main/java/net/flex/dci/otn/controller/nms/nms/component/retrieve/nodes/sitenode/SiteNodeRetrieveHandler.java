package net.flex.dci.otn.controller.nms.nms.component.retrieve.nodes.sitenode;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otc.mongo.dto.FilterItem;
import net.flex.dci.otc.mongo.dto.SortItem;
import net.flex.dci.otn.controller.nms.nms.dto.FilterDto;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import net.flex.dci.otn.controller.nms.nms.enums.NMSConvertType;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/13 10:22
 */
@Slf4j
@Component
public class SiteNodeRetrieveHandler extends AbstractSiteNodeRetrieveHandler {

    public SiteNodeRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
    }


    @Override
    public PageResult<Node> retrieveAllNodePaged(Integer pageNum, Integer pageSize) {
        log.info("start to retrieve all site node paged,pageSize:{},pageNum:{}", pageSize, pageNum);
        return netconfTopology.retrieveAllSiteNodePaged(pageNum, pageSize);
    }


    @Override
    public PageResult<Node> retrieveAllNodePaged(RetrieveTopologyDto retrieveTopologyDto) {
        log.debug("start to retrieve all node paged ,retrieve topology domain is :{}",
                retrieveTopologyDto);
//        PageResult<Node> pageResult = new PageResult<>();
//        List<FilterItem> filterItems = getFilterItems(retrieveTopologyDto);
//        List<SortItem> sortItems = getSortItems(retrieveTopologyDto);
//        int pageNum = retrieveTopologyDto.getPageNum();
//        int pageSize = retrieveTopologyDto.getPageSize();
//        pageResult = netconfTopology.retrieveAllSiteNodePaged(pageNum, pageSize, filterItems,
//                sortItems);
        return retrieveRefSiteNode(retrieveTopologyDto);
    }

    @Override
    public List<Node> retrieveAllNode(RetrieveTopologyDto retrieveTopologyDto) {
        FilterDto filterDto = getFilterItems(retrieveTopologyDto);
        List<FilterItem> filterItems = filterItemHelper.getFilterItems(filterDto,
                NMSConvertType.SITE_NODE);
        List<SortItem> sortItems = getSortItems(retrieveTopologyDto);
        List<Node> nodes = netconfTopology.retrieveAllSiteNode(filterItems, sortItems);
        return nodes;
    }
}

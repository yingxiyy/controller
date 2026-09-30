package net.flex.dci.otn.controller.nms.nms.component.retrieve.nodes.sitenode;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otc.mongo.dto.FilterItem;
import net.flex.dci.otc.mongo.dto.SortItem;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.nodes.AbstractNodeRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.dto.FilterDto;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import net.flex.dci.otn.controller.nms.nms.enums.NMSConvertType;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

/**
 * @version 1.0
 * @date 2022/5/30 17:00
 */
@Slf4j
public abstract class AbstractSiteNodeRetrieveHandler extends AbstractNodeRetrieveHandler {


    public AbstractSiteNodeRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

    protected PageResult<Node> retrieveRefSiteNode(List<String> refSiteNodeIds,
            RetrieveTopologyDto retrieveTopologyDto) {
        FilterDto filterDto = getFilterItems(retrieveTopologyDto);
        List<FilterItem> filterItems = filterItemHelper.getFilterItems(filterDto,
                NMSConvertType.PHY_NODE);
        List<SortItem> sortItems = getSortItems(retrieveTopologyDto);
        int pageNum = retrieveTopologyDto.getPageNum();
        int pageSize = retrieveTopologyDto.getPageSize();
        PageResult<Node> pageResult = netconfTopology.retrieveAllSiteNodePaged(refSiteNodeIds,
                pageNum, pageSize,
                filterItems,
                sortItems);
        return pageResult;
    }

    protected PageResult<Node> retrieveRefSiteNode(
            RetrieveTopologyDto retrieveTopologyDto) {
        FilterDto filterDto = getFilterItems(retrieveTopologyDto);
        List<FilterItem> filterItems = filterItemHelper.getFilterItems(filterDto,
                NMSConvertType.SITE_NODE);
        List<SortItem> sortItems = getSortItems(retrieveTopologyDto);
        int pageNum = retrieveTopologyDto.getPageNum();
        int pageSize = retrieveTopologyDto.getPageSize();
        PageResult<Node> pageResult = netconfTopology.retrieveAllSiteNodePaged(pageNum, pageSize,
                filterItems,
                sortItems);
        return pageResult;
    }


}

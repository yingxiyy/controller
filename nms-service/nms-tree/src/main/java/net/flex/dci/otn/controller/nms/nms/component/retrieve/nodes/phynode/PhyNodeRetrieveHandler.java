package net.flex.dci.otn.controller.nms.nms.component.retrieve.nodes.phynode;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otc.mongo.dto.FilterItem;
import net.flex.dci.otc.mongo.dto.SortItem;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.nodes.AbstractNodeRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import net.flex.dci.otn.controller.nms.nms.enums.NMSConvertType;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/12 23:09
 */
@Slf4j
@Component
public class PhyNodeRetrieveHandler extends AbstractNodeRetrieveHandler {

    public PhyNodeRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

    @Override
    public List<Node> retrieveAllNode(RetrieveTopologyDto retrieveTopologyDto) {
        log.info("retrieve all phy node by condition :{}", retrieveTopologyDto.getNodeRefs());
        List<String> refNodeIds = retrieveTopologyDto.getNodeRefs();
        String queryNodeId = retrieveTopologyDto.getNodeRef();
        if (queryNodeId != null) {
            refNodeIds.add(queryNodeId);
        }
        List<Node> phyNodeByIds = netconfTopology.retrieveAllPhyNodeByIds(refNodeIds);
        return phyNodeByIds;
    }

    @Override
    public PageResult<Node> retrieveAllNodePaged(Integer pageNum, Integer pageSize) {
        log.info("retrieve all phy node paged ,pageNum:{} ,pageSize:{}", pageNum,
                pageSize);
        PageResult<Node> pageResult = new PageResult<>();
        pageResult = netconfTopology.retrieveAllPhyNodePaged(pageNum, pageSize);

        return pageResult;
    }

    @Override
    public PageResult<Node> retrieveAllNodePaged(RetrieveTopologyDto retrieveTopologyDto) {
        log.debug("start to retrieve all node paged ,retrieve topology domain is :{}",
                retrieveTopologyDto);
        PageResult<Node> pageResult = new PageResult<>();
        List<FilterItem> filterItems = buildFilterItemsWithPlane(retrieveTopologyDto,
                NMSConvertType.PHY_NODE);
        List<SortItem> sortItems = getSortItems(retrieveTopologyDto);
        int pageNum = retrieveTopologyDto.getPageNum();
        int pageSize = retrieveTopologyDto.getPageSize();
        pageResult = netconfTopology.retrieveAllPhyNodePaged(pageNum, pageSize, filterItems,
                sortItems);
        return pageResult;
    }


}

package net.flex.dci.otn.controller.nms.nms.component.retrieve.filter;

import static net.flex.dci.otn.controller.nms.utils.Constants.LINK_DESTINATION_NODE_PATH;
import static net.flex.dci.otn.controller.nms.utils.Constants.LINK_SOURCE_NODE_PATH;
import static net.flex.dci.otn.controller.nms.utils.Constants.TUNNEL_DESTINATION_TP_PATH;
import static net.flex.dci.otn.controller.nms.utils.Constants.TUNNEL_SOURCE_TP_PATH;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.enums.FilterQuerySelector.FilterLogicalOp;
import net.flex.dci.otc.common.enums.FilterQuerySelector.FilterOperation;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dto.FilterItem;
import net.flex.dci.otn.controller.nms.enums.AdditionalProperty;
import net.flex.dci.otn.controller.nms.nms.enums.NMSConvertType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2023/2/2 11:25
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PhyNodeNameFilterHelper extends AbstractFilterHelper implements TransferFilterHelper {

    private final PhyNodeDao phyNodeDao;

    @Override
    public FilterItem transferFilterItem(FilterItem filterItem,
            AdditionalProperty additionalProperty, NMSConvertType nmsConvertType) {
        log.debug("transfer filter item for the site node");
        String filterValue = filterItem.getFilterItem();
        FilterOperation filterOp = filterItem.getFilterOp();
        FilterLogicalOp logicalOp = filterItem.getLogicalOp();
        List<Node> phyNodes = phyNodeDao.listAllNodeByNameRegex(filterValue);
        if (phyNodes.isEmpty()) {
            return filterItem;
        }
        List<String> neIds = phyNodes.stream().map(node -> node.getNodeId().getValue()).collect(
                Collectors.toList());
        String newFilterRegex = buildFilters(neIds);
        FilterItem nfilterItem = null;
        switch (nmsConvertType) {
            case PHY_LINK:
                nfilterItem = buildRefPhyLinkFilter(additionalProperty, newFilterRegex, filterOp);
                break;
            case SITE_LINK:
                nfilterItem = buildRefSiteLinkFilter(additionalProperty, newFilterRegex, filterOp);
                break;
            case TUNNEL:
                nfilterItem = buildRefTunnelFilter(additionalProperty, newFilterRegex, filterOp);
                break;
        }
        assert nfilterItem != null;
        nfilterItem.setLogicalOp(logicalOp);
        return nfilterItem;
    }

    @Override
    protected FilterItem buildRefTunnelFilter(AdditionalProperty additionalProperty, String filter,
            FilterOperation filterOp) {
        String filterKey = null;
        if (additionalProperty == AdditionalProperty.DEST_NODE_NAME) {
            filterKey = TUNNEL_DESTINATION_TP_PATH;
        } else if (additionalProperty == AdditionalProperty.SOURCE_NODE_NAME) {
            filterKey = TUNNEL_SOURCE_TP_PATH;
        }
        return FilterItem.builder().filter(filterKey).filterItem(filter).filterOp(filterOp)
                .build();
    }

    @Override
    protected FilterItem buildRefSiteLinkFilter(AdditionalProperty additionalProperty,
            String filter, FilterOperation filterOperation) {
        String filterKey = null;
        if (additionalProperty == AdditionalProperty.DEST_NODE_NAME) {
            filterKey = LINK_DESTINATION_NODE_PATH;
        } else if (additionalProperty == AdditionalProperty.SOURCE_NODE_NAME) {
            filterKey = LINK_SOURCE_NODE_PATH;
        }
        return FilterItem.builder().filter(filterKey).filterItem(filter).filterOp(filterOperation)
                .build();
    }

    @Override
    protected FilterItem buildRefPhyLinkFilter(AdditionalProperty additionalProperty, String filter,
            FilterOperation filterOperation) {
        String filterKey = null;
        if (additionalProperty == AdditionalProperty.DEST_NODE_NAME) {
            filterKey = LINK_DESTINATION_NODE_PATH;
        } else if (additionalProperty == AdditionalProperty.SOURCE_NODE_NAME) {
            filterKey = LINK_SOURCE_NODE_PATH;
        }
        return FilterItem.builder().filter(filterKey).filterItem(filter).filterOp(filterOperation)
                .build();
    }
}

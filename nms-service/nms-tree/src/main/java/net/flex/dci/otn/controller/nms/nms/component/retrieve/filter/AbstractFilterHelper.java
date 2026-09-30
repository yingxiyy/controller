package net.flex.dci.otn.controller.nms.nms.component.retrieve.filter;

import static net.flex.dci.otc.common.constants.Constants.SLASH;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.enums.FilterQuerySelector.FilterOperation;
import net.flex.dci.otc.mongo.dto.FilterItem;
import net.flex.dci.otn.controller.nms.enums.AdditionalProperty;

/**
 * @version 1.0
 * @date 2023/2/3 14:17
 */

@Slf4j
public abstract class AbstractFilterHelper {

    protected String buildFilters(List<String> refSiteNodeIds) {
        StringBuilder sb = new StringBuilder();
        for (String siteNodeId : refSiteNodeIds) {
            sb.append(siteNodeId);
            sb.append(SLASH);
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }


    protected abstract FilterItem buildRefTunnelFilter(AdditionalProperty additionalProperty,
            String filter, FilterOperation filterOp);


    protected abstract FilterItem buildRefSiteLinkFilter(AdditionalProperty additionalProperty,
            String filter, FilterOperation filterOperation);


    protected abstract FilterItem buildRefPhyLinkFilter(AdditionalProperty additionalProperty,
            String filter, FilterOperation filterOperation);
}

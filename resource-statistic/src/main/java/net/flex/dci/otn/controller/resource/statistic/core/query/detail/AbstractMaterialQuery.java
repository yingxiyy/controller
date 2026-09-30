package net.flex.dci.otn.controller.resource.statistic.core.query.detail;

import static net.flex.dci.otn.controller.resource.statistic.utils.Constants.NE_TYPE;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.enums.FilterQuerySelector;
import net.flex.dci.otc.mongo.dto.FilterItem;
import net.flex.dci.otn.controller.resource.statistic.core.query.AbstractInventoryQuery;
import net.flex.dci.otn.controller.resource.statistic.enums.DeviceType;
import org.springframework.util.CollectionUtils;

/**
 * 2026/7/22
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
public abstract class AbstractMaterialQuery<T> extends AbstractInventoryQuery<T> {


    protected List<FilterItem> buildFilterItemByDeviceType(DeviceType deviceType) {
        log.info("build filter item by device type:{}", deviceType);
        List<FilterItem> filters = new ArrayList<>();
        if (deviceType == DeviceType.ALL) {
            return filters;
        }
        filters.add(FilterItem.builder()
                .filter(NE_TYPE)
                .filterItem(deviceType.name())
                .filterOp(FilterQuerySelector.FilterOperation.EQ)
                .build());
        return filters;
    }


    protected List<String> resolveNeIds(List<String> subnet, List<String> siteIds,
            List<String> neIds) {
        List<String> finalNeIds = new ArrayList<>();
        if (!CollectionUtils.isEmpty(neIds)) {
            finalNeIds = neIds;
        } else if (!CollectionUtils.isEmpty(subnet)
                && !CollectionUtils.isEmpty(siteIds)) {
            finalNeIds = extractNeIds(subnet, siteIds);
        } else if (!CollectionUtils.isEmpty(subnet)) {
            finalNeIds = extractNeIds(subnet, null);
        } else if (!CollectionUtils.isEmpty(siteIds)) {
            finalNeIds = extractNeIds(null, siteIds);
        }
        return finalNeIds;
    }

    List<String> resolveTunnelIds(List<String> subnet, List<String> siteLinkIds) {
        List<String> finalTunnelIds = new ArrayList<>();
        if (!CollectionUtils.isEmpty(siteLinkIds)) {
            finalTunnelIds = extractTunnelIds(null, siteLinkIds);
        } else {
            finalTunnelIds = extractTunnelIds(subnet, null);
        }
        return finalTunnelIds;
    }

    private List<String> extractTunnelIds(List<String> subnet, List<String> siteLinkIds) {
        log.debug("extract tunnel id by subnet {} and siteLink {}", subnet, siteLinkIds);
        List<String> tunnelIds = new ArrayList<>();
        if (!CollectionUtils.isEmpty(siteLinkIds)) {
            tunnelIds = getTunnelIdBySiteLink(siteLinkIds);
        } else if (!CollectionUtils.isEmpty(subnet)) {
            tunnelIds = tunnelDao.retrieveAllTunnelIdsBySubnetIds(subnet);
        }

        return tunnelIds;
    }

    private List<String> getTunnelIdBySiteLink(List<String> siteLinkIds) {
        log.debug("get tunnelIds by siteLink size:{}", siteLinkIds.size());
        List<String> ochLinkIds = ochLinkDao.retrieveAllOchLinkBySupportingLinkIds(siteLinkIds);
        List<String> tunnelIds = tunnelDao.retrieveAllTunnelIdsByOchLinkIds(ochLinkIds);
        return tunnelIds;
    }


    private List<String> extractNeIds(List<String> subnet, List<String> siteIds) {
        return phyNodeDao.listPhyNodeIdsBySiteIdsAndPlaneIds(siteIds, subnet);
    }
}

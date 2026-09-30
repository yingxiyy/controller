package net.flex.dci.otn.controller.resource.statistic.service.impl;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otc.mongo.dao.EquipmentsDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SubNetTreeNodeDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.dto.FilterItem;
import net.flex.dci.otc.mongo.mdoel.lldp.ServiceLLDPInfo;
import net.flex.dci.otn.controller.resource.statistic.core.resource.InventoryResourceExtractor;
import net.flex.dci.otn.controller.resource.statistic.dto.inventory.LLDPQuery;
import net.flex.dci.otn.controller.resource.statistic.rest.Page;
import net.flex.dci.otn.controller.resource.statistic.rest.lldp.LLDPInfo;
import net.flex.dci.otn.controller.resource.statistic.service.LLDPService;
import net.flex.dci.otn.controller.resource.statistic.utils.FilterItemUtils;
import org.springframework.stereotype.Service;

/**
 * @version 1.0
 * @date 12/24/2025 11:06 AM
 */
@Service
@Slf4j
public class LLDPServiceImpl extends AbstractInventoryService implements LLDPService {

    private final TunnelDao tunnelDao;

    public LLDPServiceImpl(PhyNodeDao phyNodeDao, SubNetTreeNodeDao subNetTreeNodeDao,
            EquipmentsDao equipmentsDao,
            InventoryResourceExtractor inventoryResourceExtractor, TunnelDao tunnelDao) {
        super(phyNodeDao, subNetTreeNodeDao, equipmentsDao, inventoryResourceExtractor);
        this.tunnelDao = tunnelDao;
    }

    @Override
    public Page<LLDPInfo> fetchLLDPInfoPaged(LLDPQuery lldpQuery) {
        log.info("fetchLLDBInfoPaged(lldbQuery={})", lldpQuery);
        int page = lldpQuery.getPage() <= 0 ? 1 : lldpQuery.getPage();
        int limit = lldpQuery.getLimit() <= 0 ? 20 : lldpQuery.getLimit();
        String tunnelId = lldpQuery.getTunnelId();
//        List<String> siteLinkIds = lldpQuery.getSiteLinkIds();
        List<String> tunnelIds = lldpQuery.getTunnelIds();
        List<String> subnetwork = lldpQuery.getSubnet();
        Map<String, String> sort = lldpQuery.getSort();
//        if (!siteLinkIds.isEmpty()) {
//            List<String> refTunnelIds = getSiteLinkRefTunnels(siteLinkIds);
//        }
//        List<FilterCondition> filters = lldpQuery.getFilters();
//        List<String> refTpIds = inventoryResourceExtractor.extractConnectionRelativeTpIds(
//                siteLinkId, tunnelId);
//        List<String> refNeIds = inventoryResourceExtractor.extractRelativeNeIds(siteId, subnetwork);
//        if (StringUtils.hasText(neId)) {
//            refNeIds.add(neId);
//        }
        List<FilterItem> filters = FilterItemUtils.getFilterItems(
                lldpQuery.getFilters());
        PageResult<ServiceLLDPInfo> pageResult = tunnelDao.fetchAllLLDPInfoPaged(tunnelIds,
                subnetwork,
                page, limit,
                sort, filters);
//        Page<LLDPInfo> result = ConvertorUtils.convert2LLdPInfoPaged(pageResult);
        return null;
    }

    private List<String> getSiteLinkRefTunnels(List<String> siteLinkIds) {
        return null;
    }


}

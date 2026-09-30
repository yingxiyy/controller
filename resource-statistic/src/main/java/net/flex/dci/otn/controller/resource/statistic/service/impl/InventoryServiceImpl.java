package net.flex.dci.otn.controller.resource.statistic.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otc.mongo.dao.EquipmentsDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SubNetTreeNodeDao;
import net.flex.dci.otc.mongo.dto.FilterItem;
import net.flex.dci.otc.mongo.mdoel.card.Card;
import net.flex.dci.otc.mongo.mdoel.card.Transceiver;
import net.flex.dci.otn.controller.resource.statistic.core.manager.InventoryManagerImpl;
import net.flex.dci.otn.controller.resource.statistic.core.manager.NeInventoryManager;
import net.flex.dci.otn.controller.resource.statistic.core.resource.InventoryResourceExtractor;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.InventoryExportData;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.UnifiedExportRequest;
import net.flex.dci.otn.controller.resource.statistic.dto.inventory.CardQuery;
import net.flex.dci.otn.controller.resource.statistic.dto.inventory.NeInventoryQuery;
import net.flex.dci.otn.controller.resource.statistic.dto.inventory.TransceiverQuery;
import net.flex.dci.otn.controller.resource.statistic.rest.NeDevice;
import net.flex.dci.otn.controller.resource.statistic.rest.Page;
import net.flex.dci.otn.controller.resource.statistic.rest.equipment.CardInfo;
import net.flex.dci.otn.controller.resource.statistic.rest.equipment.TransceiverInfo;
import net.flex.dci.otn.controller.resource.statistic.service.InventoryService;
import net.flex.dci.otn.controller.resource.statistic.utils.ConvertorUtils;
import net.flex.dci.otn.controller.resource.statistic.utils.FilterItemUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 2025/10/25
 *
 * @author musa
 * @version 1.0
 **/
@Service
@Slf4j
public class InventoryServiceImpl extends AbstractInventoryService implements InventoryService {

    private final NeInventoryManager neInventoryManager;

    private final InventoryManagerImpl inventoryManager;

    public InventoryServiceImpl(PhyNodeDao phyNodeDao, SubNetTreeNodeDao subNetTreeNodeDao,
            EquipmentsDao equipmentsDao,
            InventoryResourceExtractor inventoryResourceExtractor,
            NeInventoryManager neInventoryManager, InventoryManagerImpl inventoryManager) {
        super(phyNodeDao, subNetTreeNodeDao, equipmentsDao, inventoryResourceExtractor);
        this.neInventoryManager = neInventoryManager;
        this.inventoryManager = inventoryManager;
    }


    @Override
    public NeDevice getNeInventoryDetail(String neId) {
        log.debug("get the network element:{} inventory", neId);
        NeDevice neDevice = neInventoryManager.getInventoryDetail(neId);
        return neDevice;
    }

    @Override
    public PageResult<NeDevice> getNeInventoryPaged(NeInventoryQuery neInventoryQuery) {

        return null;
    }


    @Override
    public InventoryExportData<?> getUnifiedExportData(UnifiedExportRequest unifiedExportRequest) {
        log.info("get inventory unified export data request:{}", unifiedExportRequest);
        InventoryExportData<?> inventoryExportData = inventoryManager.exportUnifiedReqData(
                unifiedExportRequest);
        return inventoryExportData;
    }

    @Override
    public Page<CardInfo> fetchEquipmentInfoPaged(CardQuery cardQuery) {
        log.info("fetch equipment info paged,the query:{}", cardQuery);
        int page = Math.max(cardQuery.getPage(), 0);
        int limit = cardQuery.getLimit() <= 0 ? 20 : cardQuery.getLimit();
        String neId = cardQuery.getNeId();
        String siteLinkId = cardQuery.getSiteLinkId();
        String siteId = cardQuery.getSiteId();
        List<String> planeId = cardQuery.getSubnet();
        String tunnelId = cardQuery.getTunnelId();
        Map<String, String> sort = cardQuery.getSort();
        List<FilterItem> filters = FilterItemUtils.getFilterItems(cardQuery.getFilters());
        List<String> cardIds = inventoryResourceExtractor.extractConnectionRelativeCardIds(
                siteLinkId,
                tunnelId);
        List<String> neIds = new ArrayList<>();
        if (StringUtils.hasText(siteId)) {
            List<String> siteRefNeIds = inventoryResourceExtractor.extractRelativeNeIds(siteId,
                    planeId);
            neIds.addAll(siteRefNeIds);
        }
        if (StringUtils.hasText(neId)) {
            neIds.add(neId);
        }
        PageResult<Card> cardPageResult = equipmentsDao.fetchCardInfoPaged(neIds, cardIds, planeId,
                page,
                limit, sort,
                filters);
        Page<CardInfo> cardInfoPage = ConvertorUtils.convert2CardInfoPaged(cardPageResult);
        return cardInfoPage;
    }

    @Override
    public Page<TransceiverInfo> fetchTransceiverInfoPaged(TransceiverQuery query) {
        log.info("fetch transceiver info paged,the query:{}", query);
        int page = Math.max(query.getPage(), 0);
        int limit = query.getLimit() <= 0 ? 20 : query.getLimit();
        String neId = query.getNeId();
        String siteId = query.getSiteId();
        String siteLinkId = query.getSiteLinkId();
        List<String> planeId = query.getSubnet();
        String tunnelId = query.getTunnelId();
        Map<String, String> sort = query.getSort();
        List<FilterItem> filters = FilterItemUtils.getFilterItems(query.getFilters());
        List<String> transceiverIds = inventoryResourceExtractor.extractConnectionRelativeTransceiverIds(
                siteLinkId,
                tunnelId);
        List<String> neIds = new ArrayList<>();

        if (StringUtils.hasText(siteId)) {
            List<String> siteRefNeIds = inventoryResourceExtractor.extractRelativeNeIds(siteId,
                    planeId);
            neIds.addAll(siteRefNeIds);
        }
        if (StringUtils.hasText(neId)) {
            neIds.add(neId);
        }
        PageResult<Transceiver> transceiverPageResult = equipmentsDao.fetchTransceiverInfoPaged(
                neIds, transceiverIds, planeId, page,
                limit, sort,
                filters);
        Page<TransceiverInfo> transceiverInfoPage = ConvertorUtils.convert2TransceiverInfoPaged(
                transceiverPageResult);
        return transceiverInfoPage;
    }


}

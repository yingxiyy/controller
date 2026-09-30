package net.flex.dci.otn.controller.resource.statistic.core.inventory;

import static net.flex.dci.otn.controller.resource.statistic.utils.CommonUtils.exportCsvFileName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.EquipmentsDao;
import net.flex.dci.otc.mongo.dto.FilterItem;
import net.flex.dci.otc.mongo.mdoel.card.Card;
import net.flex.dci.otn.controller.resource.statistic.core.enums.InventoryType;
import net.flex.dci.otn.controller.resource.statistic.core.resource.InventoryResourceExtractor;
import net.flex.dci.otn.controller.resource.statistic.dto.FilterCondition;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.InventoryExportData;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.UnifiedExportRequest;
import net.flex.dci.otn.controller.resource.statistic.export.CardCsv;
import net.flex.dci.otn.controller.resource.statistic.utils.FilterItemUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * 板卡清单导出实现
 *
 * @version 1.0
 * @date 11/5/2025 4:23 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CardInventory extends AbstractInventory<CardCsv> {

    private final EquipmentsDao equipmentsDao;

    private final InventoryResourceExtractor inventoryResourceExtractor;

    @Override
    public InventoryType inventoryScope() {
        return InventoryType.CARD;
    }

    @Override
    public InventoryExportData<CardCsv> exportData(UnifiedExportRequest unifiedExportRequest) {
        log.info("export card inventory data, request: {}", unifiedExportRequest);
        List<CardCsv> cardCsvs = getExportRequestData(unifiedExportRequest);
        String filename = exportCsvFileName(unifiedExportRequest);
        return InventoryExportData.<CardCsv>builder()
                .exportDatas(cardCsvs)
                .filename(filename)
                .sheetName(inventoryScope().name())
                .clazz(getClazz())
                .build();
    }

    private List<CardCsv> getExportRequestData(UnifiedExportRequest request) {
        log.debug("export card data scope: {}", request.getScope());

        List<String> neIds = CollectionUtils.isEmpty(request.getNeIds()) ? new ArrayList<>()
                : request.getNeIds();
        List<String> siteIds = CollectionUtils.isEmpty(request.getSiteIds()) ? new ArrayList<>()
                : request.getSiteIds();
        List<String> siteLinkIds =
                CollectionUtils.isEmpty(request.getSiteLinkIds()) ? new ArrayList<>()
                        : request.getSiteLinkIds();
        List<String> tunnelIds = CollectionUtils.isEmpty(request.getTunnelIds()) ? new ArrayList<>()
                : request.getTunnelIds();
        List<String> subnetIds = CollectionUtils.isEmpty(request.getSubnet()) ? new ArrayList<>()
                : request.getSubnet();

        // 提取关联的网元ID
//        List<String> refNeIds = inventoryResourceExtractor.extractRelativeNeIds(siteIds, subnetIds);
//        List<String> cardIds = inventoryResourceExtractor.extractConnectionRelativeCardIds(
//                siteLinkIds, tunnelIds);
        List<FilterCondition> filters = request.getFilters();
        List<String> cardIds = new ArrayList<>();
        List<String> allNeIds = new ArrayList<>();
        allNeIds.addAll(neIds);
//        allNeIds.addAll(refNeIds);
        allNeIds = allNeIds.stream().distinct().collect(Collectors.toList());

        if (CollectionUtils.isEmpty(filters)) {
            log.info("no filter condition specified, exporting all cards");
            return exportAllCards();
        }

        return fetchCards(allNeIds, cardIds, subnetIds, filters);
    }

    private List<CardCsv> exportAllCards() {
        return fetchCards(Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), new ArrayList<>());
    }

    private List<CardCsv> fetchCards(List<String> neIds, List<String> cardIds,
            List<String> subnetIds, List<FilterCondition> filters) {
        List<FilterItem> filterItems = FilterItemUtils.getFilterItems(
                filters);
        List<Card> cardResult = equipmentsDao.fetchCardInfo(
                neIds, cardIds, subnetIds, null, filterItems);

        if (cardResult == null || CollectionUtils.isEmpty(cardResult)) {
            return new ArrayList<>();
        }

        return cardResult.stream()
                .map(this::convertToCardCsv)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private CardCsv convertToCardCsv(Card card) {
        if (card == null) {
            return null;
        }
        return CardCsv.builder()
                .neId(card.getNeId())
                .neName(card.getNeName())
                .neVendorName(card.getNeVendor())
                .siteId(card.getSiteId())
                .siteName(card.getSiteName())
                .subnet(card.getSubnet())
                .neIp(card.getNeIp())
                .equipmentId(card.getCardId())
                .equipmentName(card.getCardName())
                .vendorName(card.getVendorName())
                .PN(card.getPN())
                .SN(card.getSN())
                .hwVersion(card.getHwVersion())
                .swVersion(card.getSwVersion())
                .fwVersion(card.getFwVersion())
                .mfgDate(card.getMfgDate())
                .assetManagementCode(card.getAsset_management_code())
                .neType(card.getNeType())
                .neSubType(card.getNeSubType())
                .equipmentType(card.getCardType())
                .description(card.getDescription())
                .build();
    }

    @Override
    public InventoryExportData<CardCsv> exportAll() {
        List<CardCsv> cardCsvs = exportAllCards();
        return InventoryExportData.<CardCsv>builder()
                .exportDatas(cardCsvs)
                .filename("CARD_ALL.csv")
                .sheetName(inventoryScope().name())
                .clazz(getClazz())
                .build();
    }

    @Override
    protected Class<CardCsv> getClazz() {
        return CardCsv.class;
    }
}

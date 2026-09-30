package net.flex.dci.otn.controller.resource.statistic.core.query.detail;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.EquipmentsDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dto.FilterItem;
import net.flex.dci.otc.mongo.mdoel.card.Card;
import net.flex.dci.otn.controller.resource.statistic.core.enums.InventoryType;
import net.flex.dci.otn.controller.resource.statistic.dto.FilterCondition;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.InventoryExportData;
import net.flex.dci.otn.controller.resource.statistic.dto.query.BaseQueryDTO;
import net.flex.dci.otn.controller.resource.statistic.dto.query.MaterialQueryDTO;
import net.flex.dci.otn.controller.resource.statistic.dto.query.UnifiedResourceQueryParam;
import net.flex.dci.otn.controller.resource.statistic.enums.DeviceType;
import net.flex.dci.otn.controller.resource.statistic.export.CardCsv;
import net.flex.dci.otn.controller.resource.statistic.utils.ConvertorUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 2026/7/13
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class CardQuery extends AbstractMaterialQuery<CardCsv> {


    private final EquipmentsDao equipmentsDao;

    private final PhyNodeDao phyNodeDao;

    @Override
    public InventoryType inventoryScope() {
        return InventoryType.CARD;
    }

    @Override
    public InventoryExportData<CardCsv> queryData(UnifiedResourceQueryParam query) {
        return null;
    }

    @Override
    public InventoryExportData<?> queryData(BaseQueryDTO queryDTO,
            List<String> subnet) {
        log.debug("query card info by the queryDTO：{} and subnet is:{}", queryDTO, subnet);
        MaterialQueryDTO materialQueryDTO = (MaterialQueryDTO) queryDTO;

        List<String> siteIds = materialQueryDTO.getSite();
        List<String> neIds = materialQueryDTO.getDeviceId();
        DeviceType deviceType = materialQueryDTO.getDeviceType();
        List<CardCsv> cardCsvs = new ArrayList<>();
        String filename = generateFileName(subnet, siteIds, neIds, deviceType);
        return InventoryExportData.<CardCsv>builder()
                .exportDatas(cardCsvs)
                .filename(filename)
                .sheetName(inventoryScope().name())
                .clazz(getClazz())
                .build();
    }

    @Override
    public void queryStream(BaseQueryDTO baseQueryDTO, List<String> subnet,
            Consumer<List<?>> rowConsumer) {
        log.debug("query card stream by queryDTO:{} subnet:{}", baseQueryDTO, subnet);
        MaterialQueryDTO dto = (MaterialQueryDTO) baseQueryDTO;
        List<String> finalNeIds = resolveNeIds(subnet, dto.getSite(), dto.getDeviceId());
        List<FilterItem> filters = buildFilterItemByDeviceType(dto.getDeviceType());
        List<CardCsv> buffer = new ArrayList<>(500);
        try (Stream<Card> stream = equipmentsDao.fetchCardInfoStream(
                finalNeIds, new ArrayList<>(), subnet, null, filters)) {
            stream.map(ConvertorUtils::convertToCardCsv)
                    .filter(Objects::nonNull)
                    .forEach(csv -> {
                        buffer.add(csv);
                        if (buffer.size() >= 500) {
                            rowConsumer.accept(buffer);
                            buffer.clear();
                        }
                    });
            if (!buffer.isEmpty()) {
                rowConsumer.accept(buffer);
            }
        }
    }


    private String generateFileName(List<String> subnet, List<String> siteIds, List<String> neIds,
            DeviceType deviceType) {
        return null;
    }


    private List<CardCsv> getQueryData(UnifiedResourceQueryParam query) {
        log.debug("export card data scope:{}", query);

        List<String> neIds = CollectionUtils.isEmpty(query.getNeIds()) ? new ArrayList<>()
                : query.getNeIds();
        List<String> siteIds = CollectionUtils.isEmpty(query.getSiteIds()) ? new ArrayList<>()
                : query.getSiteIds();
        List<String> siteLinkIds =
                CollectionUtils.isEmpty(query.getSiteLinkIds()) ? new ArrayList<>()
                        : query.getSiteLinkIds();
        List<String> tunnelIds = CollectionUtils.isEmpty(query.getTunnelIds()) ? new ArrayList<>()
                : query.getTunnelIds();
        List<String> subnetIds = CollectionUtils.isEmpty(query.getSubnet()) ? new ArrayList<>()
                : query.getSubnet();

        // 提取关联的网元ID
//        List<String> refNeIds = inventoryResourceExtractor.extractRelativeNeIds(siteIds, subnetIds);
//        List<String> cardIds = inventoryResourceExtractor.extractConnectionRelativeCardIds(
//                siteLinkIds, tunnelIds);
        List<FilterCondition> filters = getQueryFilterItem(query);
        List<String> cardIds = new ArrayList<>();
        List<String> allNeIds = new ArrayList<>();
        allNeIds.addAll(neIds);
//        allNeIds.addAll(refNeIds);
        allNeIds = allNeIds.stream().distinct().collect(Collectors.toList());

        if (CollectionUtils.isEmpty(filters)) {
            log.info("no filter condition specified, exporting all cards");
            return queryAllCards();
        }

        return fetchCards(allNeIds, cardIds, subnetIds, filters);
    }

    private List<FilterCondition> getQueryFilterItem(UnifiedResourceQueryParam query) {
        List<FilterCondition> filterConditions = new ArrayList<>();
        String neName = query.getNeName();
        String vendor = query.getVendorName();
        if (StringUtils.hasText(neName)) {
//            filterConditions.add(FilterCondition.builder().field().value(neName).operation(
//                    FilterOperation.CONTAIN).build());
        }
        if (StringUtils.hasText(vendor)) {

        }

        return filterConditions;
    }

    private List<CardCsv> queryAllCards() {
        return new ArrayList<>();
    }

    private List<CardCsv> fetchCards(List<String> allNeIds, List<String> cardIds,
            List<String> subnetIds, List<FilterCondition> filters) {
        return new ArrayList<>();
    }

    @Override
    protected Class<CardCsv> getClazz() {
        return CardCsv.class;
    }
}

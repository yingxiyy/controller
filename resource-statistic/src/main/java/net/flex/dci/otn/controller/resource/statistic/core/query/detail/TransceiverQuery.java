package net.flex.dci.otn.controller.resource.statistic.core.query.detail;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dto.FilterItem;
import net.flex.dci.otc.mongo.mdoel.card.Transceiver;
import net.flex.dci.otn.controller.resource.statistic.core.enums.InventoryType;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.InventoryExportData;
import net.flex.dci.otn.controller.resource.statistic.dto.query.BaseQueryDTO;
import net.flex.dci.otn.controller.resource.statistic.dto.query.MaterialQueryDTO;
import net.flex.dci.otn.controller.resource.statistic.dto.query.UnifiedResourceQueryParam;
import net.flex.dci.otn.controller.resource.statistic.export.TransceiverCsv;
import net.flex.dci.otn.controller.resource.statistic.utils.ConvertorUtils;
import org.springframework.stereotype.Component;

/**
 * 2026/7/13
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class TransceiverQuery extends AbstractMaterialQuery<TransceiverCsv> {

    @Override
    public InventoryType inventoryScope() {
        return InventoryType.TRANSCEIVER;
    }

    @Override
    public InventoryExportData<TransceiverCsv> queryData(
            UnifiedResourceQueryParam unifiedResourceQueryParam) {
        return null;
    }

    @Override
    public void queryStream(BaseQueryDTO baseQueryDTO, List<String> subnet,
            Consumer<List<?>> rowConsumer) {
        log.debug("query transceiver stream by queryDTO:{} subnet:{}", baseQueryDTO, subnet);
        MaterialQueryDTO dto = (MaterialQueryDTO) baseQueryDTO;
        List<String> finalNeIds = resolveNeIds(subnet, dto.getSite(), dto.getDeviceId());
        List<FilterItem> filters = buildFilterItemByDeviceType(dto.getDeviceType());
        List<TransceiverCsv> buffer = new ArrayList<>(500);
        try (Stream<Transceiver> stream = equipmentsDao.fetchTransceiverInfoStream(
                finalNeIds, new ArrayList<>(), subnet, null, filters)) {
            stream.map(ConvertorUtils::convertToTransceiverCsv)
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

    @Override
    protected Class<TransceiverCsv> getClazz() {
        return TransceiverCsv.class;
    }
}

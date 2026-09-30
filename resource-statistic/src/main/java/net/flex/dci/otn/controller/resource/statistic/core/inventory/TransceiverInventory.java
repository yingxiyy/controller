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
import net.flex.dci.otc.mongo.mdoel.card.Transceiver;
import net.flex.dci.otn.controller.resource.statistic.core.enums.InventoryType;
import net.flex.dci.otn.controller.resource.statistic.core.resource.InventoryResourceExtractor;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.InventoryExportData;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.UnifiedExportRequest;
import net.flex.dci.otn.controller.resource.statistic.export.TransceiverCsv;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * 光模块清单导出实现
 *
 * @author musa
 * @version 1.0
 * @date 2026/4/12
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TransceiverInventory extends AbstractInventory<TransceiverCsv> {

    private final EquipmentsDao equipmentsDao;

    private final InventoryResourceExtractor inventoryResourceExtractor;

    @Override
    public InventoryType inventoryScope() {
        return InventoryType.TRANSCEIVER;
    }

    @Override
    public InventoryExportData<TransceiverCsv> exportData(
            UnifiedExportRequest unifiedExportRequest) {
        log.info("export transceiver inventory data, request: {}", unifiedExportRequest);
        List<TransceiverCsv> transceiverCsvs = getExportRequestData(unifiedExportRequest);
        String filename = exportCsvFileName(unifiedExportRequest);
        return InventoryExportData.<TransceiverCsv>builder()
                .exportDatas(transceiverCsvs)
                .filename(filename)
                .sheetName(inventoryScope().name())
                .clazz(getClazz())
                .build();
    }

    private List<TransceiverCsv> getExportRequestData(UnifiedExportRequest request) {
        log.debug("export transceiver data scope: {}", request.getScope());

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

        // 提取关联的网元ID和光模块ID
//        List<String> refNeIds = inventoryResourceExtractor.extractRelativeNeIds(siteIds, subnetIds);
//        List<String> transceiverIds = inventoryResourceExtractor.extractConnectionRelativeTransceiverIds(siteLinkIds, tunnelIds);

        // 合并所有网元ID
        List<String> allNeIds = new ArrayList<>();
        allNeIds.addAll(neIds);
        List<String> transceiverIds = new ArrayList<>();
//        allNeIds.addAll(refNeIds);
        allNeIds = allNeIds.stream().distinct().collect(Collectors.toList());

        if (CollectionUtils.isEmpty(allNeIds) && CollectionUtils.isEmpty(transceiverIds)) {
            log.info("no filter condition specified, exporting all transceivers");
            return exportAllTransceivers();
        }

        return fetchTransceivers(allNeIds, transceiverIds, subnetIds);
    }

    private List<TransceiverCsv> exportAllTransceivers() {
        return fetchTransceivers(Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList());
    }

    private List<TransceiverCsv> fetchTransceivers(List<String> neIds,
            List<String> transceiverIds, List<String> subnetIds) {
        List<Transceiver> transceiverResult = equipmentsDao.fetchTransceiverInfo(
                neIds, transceiverIds, subnetIds, null, null);

        if (transceiverResult == null || CollectionUtils.isEmpty(
                transceiverResult)) {
            return new ArrayList<>();
        }

        return transceiverResult.stream()
                .map(this::convertToTransceiverCsv)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private TransceiverCsv convertToTransceiverCsv(Transceiver transceiver) {
        if (transceiver == null) {
            return null;
        }
        return TransceiverCsv.builder()
                .neId(transceiver.getNeId())
                .neName(transceiver.getNeName())
                .neVendorName(transceiver.getNeVendor())
                .neType(transceiver.getNeType())
                .neSubType(transceiver.getNeSubType())
                .subnet(transceiver.getSubnet())
                .siteId(transceiver.getSiteId())
                .siteName(transceiver.getSiteName())
                .neIp(transceiver.getNeIp())
                .transceiverId(transceiver.getTransceiverId())
                .transceiverName(transceiver.getTransceiverName())
                .shelf(transceiver.getShelf())
                .slot(transceiver.getSlot())
                .serialNo(transceiver.getSerialNo())
                .vendorName(transceiver.getVendorName())
                .hwVersion(transceiver.getHwVersion())
                .swVersion(transceiver.getSwVersion())
                .fwVersion(transceiver.getFwVersion())
                .partNo(transceiver.getPartNo())
                .description(transceiver.getDescription())
                .connectorType(transceiver.getConnectorType())
                .formFactor(transceiver.getFormFactor())
                .ethernetPmd(transceiver.getEthernetPmd())
                .mfgDate(transceiver.getMfgDate())
                .build();
    }

    @Override
    public InventoryExportData<TransceiverCsv> exportAll() {
        List<TransceiverCsv> transceiverCsvs = exportAllTransceivers();
        return InventoryExportData.<TransceiverCsv>builder()
                .exportDatas(transceiverCsvs)
                .filename("TRANSCEIVER_ALL.csv")
                .sheetName(inventoryScope().name())
                .clazz(getClazz())
                .build();
    }

    @Override
    protected Class<TransceiverCsv> getClazz() {
        return TransceiverCsv.class;
    }
}

package net.flex.dci.otn.controller.resource.statistic.service;

import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.InventoryExportData;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.UnifiedExportRequest;
import net.flex.dci.otn.controller.resource.statistic.dto.inventory.CardQuery;
import net.flex.dci.otn.controller.resource.statistic.dto.inventory.NeInventoryQuery;
import net.flex.dci.otn.controller.resource.statistic.dto.inventory.TransceiverQuery;
import net.flex.dci.otn.controller.resource.statistic.rest.NeDevice;
import net.flex.dci.otn.controller.resource.statistic.rest.Page;
import net.flex.dci.otn.controller.resource.statistic.rest.equipment.CardInfo;
import net.flex.dci.otn.controller.resource.statistic.rest.equipment.TransceiverInfo;

/**
 * 2025/10/25
 *
 * @author musa
 * @version 1.0
 **/
public interface InventoryService {

    NeDevice getNeInventoryDetail(String neId);

    PageResult<NeDevice> getNeInventoryPaged(NeInventoryQuery neInventoryQuery);


    InventoryExportData<?> getUnifiedExportData(UnifiedExportRequest unifiedExportRequest);

    Page<CardInfo> fetchEquipmentInfoPaged(CardQuery equipmentQuery);

    Page<TransceiverInfo> fetchTransceiverInfoPaged(TransceiverQuery query);
}

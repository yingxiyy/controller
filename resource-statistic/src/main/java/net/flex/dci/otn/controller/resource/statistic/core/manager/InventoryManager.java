package net.flex.dci.otn.controller.resource.statistic.core.manager;

import net.flex.dci.otn.controller.resource.statistic.dto.csv.InventoryExportData;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.UnifiedExportRequest;

/**
 *
 * @version 1.0
 * @date 11/5/2025 4:14 PM
 */
public interface InventoryManager {

    InventoryExportData exportUnifiedReqData(UnifiedExportRequest unifiedExportRequest);
}

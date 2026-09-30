package net.flex.dci.otn.controller.resource.statistic.core.inventory;


import net.flex.dci.otn.controller.resource.statistic.core.enums.InventoryType;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.InventoryExportData;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.UnifiedExportRequest;

/**
 * @version 1.0
 * @date 11/5/2025 4:23 PM
 */
public interface Inventory<T> {

    InventoryType inventoryScope();

    InventoryExportData<T> exportData(UnifiedExportRequest unifiedExportRequest);

    InventoryExportData<T> exportAll();
}

package net.flex.dci.otn.controller.resource.statistic.core.manager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.resource.statistic.core.enums.InventoryType;
import net.flex.dci.otn.controller.resource.statistic.core.inventory.Inventory;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.InventoryExportData;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.UnifiedExportRequest;
import org.springframework.stereotype.Component;

/**
 *
 * @version 1.0
 * @date 11/5/2025 4:14 PM
 */
@Component
@Slf4j
public class InventoryManagerImpl implements InventoryManager {


    Map<InventoryType, Inventory> inventoryMap = new HashMap<>();

    public InventoryManagerImpl(List<Inventory> inventories) {
        this.inventoryMap = inventories.stream()
                .peek(inv -> log.info("Registered inventory exporter for type: {}",
                        inv.inventoryScope()))
                .collect(Collectors.toMap(
                        Inventory::inventoryScope,
                        Function.identity()
                ));
    }

    @Override
    public InventoryExportData exportUnifiedReqData(UnifiedExportRequest unifiedExportRequest) {
        log.debug("export unified reqest data type:{} scope:{}",
                unifiedExportRequest.getUnifiedType(), unifiedExportRequest.getScope());
        InventoryType inventoryType = unifiedExportRequest.getUnifiedType();
        InventoryExportData inventoryExportData = inventoryMap.get(inventoryType)
                .exportData(unifiedExportRequest);
        return inventoryExportData;
    }
}

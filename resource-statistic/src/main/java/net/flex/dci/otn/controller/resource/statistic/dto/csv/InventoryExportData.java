package net.flex.dci.otn.controller.resource.statistic.dto.csv;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 11/5/2025 4:11 PM
 */
@Data
@Builder
public class InventoryExportData<T> implements Serializable {

    private final String filename;

    private final String sheetName;

    private final List<T> exportDatas;

    private final Class<T> clazz;
}

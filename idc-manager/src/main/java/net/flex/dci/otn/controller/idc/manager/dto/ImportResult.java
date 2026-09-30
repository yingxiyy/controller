package net.flex.dci.otn.controller.idc.manager.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/9/16
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class ImportResult implements Serializable {

    private int totalRows;
    private int successCount;
    private int duplicateCount;
    private List<RowError> errors;
}

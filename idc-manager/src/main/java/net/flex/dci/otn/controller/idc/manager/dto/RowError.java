package net.flex.dci.otn.controller.idc.manager.dto;

import java.io.Serializable;
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
public class RowError implements Serializable {

    private int rowNumber;
    private String message;

    private String messageKey;
}

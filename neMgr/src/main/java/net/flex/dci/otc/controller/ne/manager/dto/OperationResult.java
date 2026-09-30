package net.flex.dci.otc.controller.ne.manager.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 *
 * 2025/11/15
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class OperationResult implements Serializable {

    private String neId;

    private Long timestamp;

    private boolean success;

    private String message;
}

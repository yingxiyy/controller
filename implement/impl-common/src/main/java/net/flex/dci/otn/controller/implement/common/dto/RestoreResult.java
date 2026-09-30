package net.flex.dci.otn.controller.implement.common.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otn.controller.implement.common.enums.SetResultCode;

/**
 *
 * @version 1.0
 * @date 10/24/2025 1:34 PM
 */
@Data
@Builder
public class RestoreResult implements Serializable {

    private SetResultCode code;

    private String message;
}

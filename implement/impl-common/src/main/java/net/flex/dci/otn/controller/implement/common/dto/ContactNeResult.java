package net.flex.dci.otn.controller.implement.common.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otn.controller.implement.common.enums.SetResultCode;

/**
 *
 * 2025/12/3
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class ContactNeResult implements Serializable {

    private SetResultCode code;

    private String message;

}

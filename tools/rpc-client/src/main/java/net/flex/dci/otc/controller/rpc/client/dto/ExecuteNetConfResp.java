package net.flex.dci.otc.controller.rpc.client.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otc.controller.rpc.client.enums.ExecuteStatus;

/**
 *
 * 2025/12/20
 *
 * @author musa
 * @version 1.0
 **/

@Data
@Builder
public class ExecuteNetConfResp implements Serializable {

    private String resp;

    private String message;

    private ExecuteStatus status;
}

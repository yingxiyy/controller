package net.flex.dci.otc.controller.rpc.client.dto;

import java.io.Serializable;
import lombok.Data;

/**
 *
 * @version 1.0
 * @date 12/11/2025 1:10 PM
 */
@Data
public class HealthStatusResp implements Serializable {

    private String status;
}

package net.flex.dci.otn.controller.system.config.common.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2025/12/29
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TelemetryServerDto implements Serializable {

    private String address;

    private Integer port;

}

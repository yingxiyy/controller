package net.flex.dci.otn.controller.system.config.common.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * 2025/12/29
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class TelemetryServerOutput implements Serializable {

    private TelemetryServerDto ipv4Server;

    private TelemetryServerDto ipv6Server;

    private boolean enable;

    private Long updatedAt;

    private String updatedBy;
}

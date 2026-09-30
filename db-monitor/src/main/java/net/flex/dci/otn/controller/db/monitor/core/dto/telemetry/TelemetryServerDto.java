package net.flex.dci.otn.controller.db.monitor.core.dto.telemetry;

import java.io.Serializable;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/11/16 14:59
 */
@Data
public class TelemetryServerDto implements Serializable {

    private TelemetryServerData data;
}

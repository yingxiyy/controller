package net.flex.dci.otn.controller.system.config.component;

import net.flex.dci.otc.mongo.mdoel.telemetry.TelemetryConfig;
import net.flex.dci.otn.controller.system.config.common.dto.TelemetryServerConfig;

/**
 * 2025/12/29
 *
 * @author musa
 * @version 1.0
 **/
public interface TelemetryServerManager {

    TelemetryConfig configTelemetryServer(TelemetryServerConfig telemetryServerConfig,String author);
}

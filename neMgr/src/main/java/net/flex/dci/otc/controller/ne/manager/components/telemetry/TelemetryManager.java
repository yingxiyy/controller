package net.flex.dci.otc.controller.ne.manager.components.telemetry;


import net.flex.dci.otc.controller.ne.manager.components.telemetry.TelemetryManagerImpl.TelemetryInfo;
import net.flex.dci.otc.controller.ne.manager.model.SensorGroup;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

/**
 * @version 1.0
 * @date 6/11/2025 3:19 PM
 */
public interface TelemetryManager {

    TelemetryInfo getTelemetryConfigurationByNe(Node ne);

    SensorGroup getNorthboundTelemetryConfigurationByNe(Node ne);

}

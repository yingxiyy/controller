package net.flex.dci.otc.controller.ne.manager.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.components.ntp.NtpAssignment;
import net.flex.dci.otc.controller.ne.manager.components.telemetry.NorthboundTelemetry;
import net.flex.dci.otc.controller.ne.manager.service.NeConfigurationManager;
import net.flex.dci.otc.serialization.util.SerializeUtil;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigureNorthboundTelemetryOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigureNorthboundTelemetryOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ReassignNtpServerOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ReassignNtpServerOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.springframework.stereotype.Component;

/**
 * 2025/12/29
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class NeConfigurationManagerImpl implements NeConfigurationManager {


    private final NtpAssignment ntpAssignment;

    private final NorthboundTelemetry northboundTelemetry;

    @Override
    public String reassignNtpServer() {
        log.info("reassign ntp server configuration to the ne");
        ntpAssignment.reassignNtpServer();
        ReassignNtpServerOutput output = new ReassignNtpServerOutputBuilder().setReturnCode(
                RpcResultType.Success).build();
        return SerializeUtil.serializeRpcOutput2Json(output);
    }

    @Override
    public String configureNorthboundTelemetry() {
        log.info("start to configure the northbound telemetry");
        northboundTelemetry.configureTelemetry();
        ConfigureNorthboundTelemetryOutput output = new ConfigureNorthboundTelemetryOutputBuilder().setReturnCode(
                RpcResultType.Success).build();
        return SerializeUtil.serializeRpcOutput2Json(output);
    }


}

package net.flex.dci.otc.controller.ne.manager.components.telemetry;

import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.DEFAULT_TIMEZONE;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.Northbound.TELEMETRY_SENSOR_GROUP_PREFIX;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils.ipIsInNet;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.components.balancer.TelemetryBalancer;
import net.flex.dci.otc.controller.ne.manager.dto.NeInfo;
import net.flex.dci.otc.controller.ne.manager.enums.IpAddressType;
import net.flex.dci.otc.controller.ne.manager.model.SensorGroup;
import net.flex.dci.otc.controller.ne.manager.properties.TelemetryManagerConfigProperties;
import net.flex.dci.otc.controller.ne.manager.properties.TelemetrySensorGroupProperties;
import net.flex.dci.otc.controller.ne.manager.utils.NeInfoUtil;
import net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils;
import net.flex.dci.otc.mongo.dao.NtpConfigDao;
import net.flex.dci.otc.mongo.mdoel.ntp.NtpConfig;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.telemetry.manager.TelemetryServer;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @version 1.0
 * @date 6/11/2025 3:19 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TelemetryManagerImpl implements TelemetryManager {


    private final TelemetrySensorGroupProperties sensorGroupProperties;

    private final TelemetryManagerConfigProperties managerConfigProperties;

    private final TelemetryBalancer telemetryBalancer;

    private final NtpConfigDao ntpConfigDao;


    @Override
    public TelemetryInfo getTelemetryConfigurationByNe(Node ne) {
        log.info("start to assign telemetry configuration properties to ne:{}",
                ne.getNodeId().getValue());
        TelemetryInfo telemetryInfo = getAssignTelemetryConfig(ne);
        return telemetryInfo;
    }

    @Override
    public SensorGroup getNorthboundTelemetryConfigurationByNe(Node ne) {
        NeInfo neInfo = NeInfoUtil.getNeInfo(ne);
        String neYangVersion = neInfo.getYangVersion();
        if (!StringUtils.hasText(neYangVersion)) {
            log.error("the ne :{} yang version data in db is invalid", ne.getNodeId().getValue());
            return null;
        }
        NodeType neType = neInfo.getNodeType();
        String vendor = neInfo.getVendor();
        SensorGroup sensorGroup = sensorGroupProperties.getTelemetrySensorGroup(vendor,
                neYangVersion, neType);
        String sensorGroupName = TELEMETRY_SENSOR_GROUP_PREFIX + sensorGroup.getSensorGroupName();
        sensorGroup.setSensorGroupName(sensorGroupName);
        return sensorGroup;
    }


    private TelemetryInfo getAssignTelemetryConfig(Node ne) {
        NeInfo neInfo = NeInfoUtil.getNeInfo(ne);
        String neYangVersion = neInfo.getYangVersion();
        if (!StringUtils.hasText(neYangVersion)) {
            log.error("the assign telemetry ne :{} data in db is invalid",
                    ne.getNodeId().getValue());
            return null;
        }
        NodeType neType = neInfo.getNodeType();
        String neIp = neInfo.getIp();
        String vendor = neInfo.getVendor();
        boolean isMixed = managerConfigProperties.isMixed();
        log.debug("current telemetry server is mixed:{}", isMixed);
        boolean isNeIpPrivate =
                isMixed ? ipIsInNet(neIp, managerConfigProperties.privateIpArea()) : true;
        //get neIp type is ipv4 or ipv6
        IpAddressType ipAddressType = NeManagerUtils.getIpAddressType(neIp);
//        String ntpAddress = isMixed ? isNeIpPrivate ? managerConfigProperties.privateNtpAddress()
//                : managerConfigProperties.publicNtpAddress()
//                : managerConfigProperties.privateNtpAddress();
        NtpInfo ntp = getNtpAddressByNeIp(ipAddressType);
        SensorGroup sensorGroup = sensorGroupProperties.getTelemetrySensorGroup(vendor,
                neYangVersion,
                neType);
        String ipArea = managerConfigProperties.privateIpArea();
//                isMixed ? (isNeIpPrivate ? managerConfigProperties.privateIpArea() : managerConfigProperties.) : null;
        TelemetryServer telemetryServer = null;
        if (isMixed) {
            telemetryServer = telemetryBalancer.pickupLeastWithIpArea(neYangVersion,
                    ipArea, isNeIpPrivate, ipAddressType);
        } else {
            telemetryServer = telemetryBalancer.pickupLeast(neYangVersion, ipAddressType);
        }
        if (telemetryServer == null) {
            log.warn("there have not available telemetryServer to set");
            return null;
        }
        return TelemetryInfo.builder().telemetryServer(telemetryServer).ntpIp(ntp.getNtpIp())
                .timezone(ntp.getTimezone())
                .sensorGroup(sensorGroup).build();

    }

    private NtpInfo getNtpAddressByNeIp(IpAddressType ipAddressType) {
        log.debug("get ntp address by neIp type:{}", ipAddressType);
//        IpAddressType ipAddressType = NeManagerUtils.getIpAddressType(ip);
        NtpConfig ntpConfig = ntpConfigDao.getNtpConfig();
        String ntpAddress =
                ipAddressType == IpAddressType.IPV4 ? ntpConfig.getIpv4NtpServer().getAddress()
                        : ntpConfig.getIpv6NtpServer().getAddress();
        String timezone = StringUtils.hasText(ntpConfig.getTimezone()) ? ntpConfig.getTimezone()
                : DEFAULT_TIMEZONE;
        return NtpInfo.builder().ntpIp(ntpAddress).timezone(timezone).build();
    }

    @Data
    @Builder
    public static class NtpInfo implements Serializable {

        private String ntpIp;

        private String timezone;
    }


    @Data
    @Builder
    public static class TelemetryInfo implements Serializable {

        private TelemetryServer telemetryServer;
        private String ntpIp;
        private SensorGroup sensorGroup;
        private String timezone;
    }
}

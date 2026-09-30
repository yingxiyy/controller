package net.flex.dci.otc.controller.ne.manager.utils;

import static net.flex.dci.otc.common.constants.Constants.POUND;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.DefaultSystemConfig.DEFAULT_HOST;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.NeNtpVersion.VENDOR_HUAWEI;

import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.IpUtils;
import net.flex.dci.otc.common.util.IpUtils.IpVersion;
import net.flex.dci.otc.controller.ne.manager.enums.IpAddressType;
import net.flex.dci.otc.controller.ne.manager.enums.NtpVersion;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.Report1524TelemetryDataInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.Report1524TelemetryDataOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.AdapterBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ConfigObjectId;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ConfigObjectType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.RemoveResourceInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.RemoveResourceInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.Report1524TelemetryDataInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.Report1524TelemetryDataOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.ne.result.FailObj;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.System;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.SystemBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.system.NtpBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.system.RadiusBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.system.SyslogBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.system.Telemetry;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.system.TelemetryBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.system.TelemetryKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.telemetry.manager.TelemetryServer;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.telemetry.manager.TelemetryServerBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.telemetry.manager.TelemetryServerKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @version 1.0
 * @date 2022/3/29 12:37
 */
@Slf4j
public class NeManagerUtils {

    public static <T> Set<T> getIntersectionSetByGuava(Set<T> before, Set<T> after) {
        Set<T> diff = Sets.intersection(before, after);
        return diff;
    }

    public static <T> Set<T> getDifferenceSetByGuava(Set<T> before, Set<T> after) {
        Set<T> diff = Sets.difference(before, after);
        return diff;
    }

    public static AlignmentStatusType calculateAlignStatus(AlignmentStatusType oldAlignStatus,
            AlignmentStatusType newAlignStatus) {
        int updateCode = AlignStatusCode.getAlignStatusCode(oldAlignStatus)
                | AlignStatusCode.getAlignStatusCode(newAlignStatus);
        return AlignStatusCode.getAlignmentStatusType(updateCode);
    }


    public static AlignmentStatusType calculateAlignStatus(
            Collection<AlignmentStatusType> statusTypes) {
        AtomicReference<AlignmentStatusType> alignmentStatusType = new AtomicReference<>(
                AlignmentStatusType.Aligned);
        statusTypes.forEach(status -> {
            alignmentStatusType.set(calculateAlignStatus(alignmentStatusType.get(), status));
        });

        return alignmentStatusType.get();
    }


    public static org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.Report1524TelemetryDataOutput convert2TelemetryEmlOutPut(
            Report1524TelemetryDataOutput output) {
        Report1524TelemetryDataOutputBuilder report1524TelemetryDataOutputBuilder = new Report1524TelemetryDataOutputBuilder();
        report1524TelemetryDataOutputBuilder.setReturnCode(output.getReturnCode());
        report1524TelemetryDataOutputBuilder.setReturnMessage(output.getReturnMessage());
        return report1524TelemetryDataOutputBuilder.build();
    }

    public static org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.Report1524TelemetryDataInput convert2TelemetryEmlInput(
            Report1524TelemetryDataInput input) {
        Report1524TelemetryDataInputBuilder reportInputBuilder = new Report1524TelemetryDataInputBuilder();
        reportInputBuilder.setNodeId(input.getNodeRef());
        reportInputBuilder.setType(input.getType());
        return reportInputBuilder.build();

    }


    public static DateAndTime getCurrentTime() {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        String str = sdf.format(new Date());
        String str1 = str.substring(0, str.length() - 2);
        String str2 = str.substring(str.length() - 2);
        String sb = str1
                + ":"
                + str2;
        return DateAndTime.getDefaultInstance(sb);

    }

    public static System getDefaultSystem() {
        log.debug("get default system configuration");
        SystemBuilder systemBuilder = new SystemBuilder();
        NtpBuilder ntpBuilder = new NtpBuilder();
        ntpBuilder.setIp(
                DEFAULT_HOST);
        SyslogBuilder syslogBuilder = new SyslogBuilder();
        syslogBuilder.setIp(
                DEFAULT_HOST);
        RadiusBuilder radiusBuilder = new RadiusBuilder();
        radiusBuilder.setIp(
                DEFAULT_HOST);
        List<Property> properties = new ArrayList<>();
        properties.add(
                new PropertyBuilder().setName(NeManagerConstants.DefaultSystemConfig.TIME_ZONE)
                        .setName(NeManagerConstants.DefaultSystemConfig.DEFAULT_TIMEZONE).build());
        PropertiesBuilder propertiesBuilder = new PropertiesBuilder();
        propertiesBuilder.setProperty(properties);
        systemBuilder.setProperties(propertiesBuilder.build());
        systemBuilder.setNtp(Collections.singletonList(ntpBuilder.build()));
        systemBuilder.setRadius(Collections.singletonList(radiusBuilder.build()));
        systemBuilder.setSyslog(Collections.singletonList(syslogBuilder.build()));
        return systemBuilder.build();
    }

    public static InputStream loadFileInputStream(String fileName) throws IOException {
        log.info("load file  inputStream:{}", fileName);
        InputStream inputStream = null;
        String rootDir = java.lang.System.getProperty("user.dir");
        File file = new File(rootDir, "config/" + fileName);
        if (file.exists()) {
            inputStream = Files.newInputStream(file.toPath());
        } else {
            Resource resource = new ClassPathResource(fileName);
            inputStream = resource.getInputStream();
        }
        return inputStream;
    }

    public static Boolean ipIsInNet(String ip, String ipArea) {
        if (DEFAULT_HOST.equals(ip)) {
            return true;
        }
        if (StringUtils.isBlank(ipArea)) {
            return true;
        }
        String[] ipArray = ipArea.split(",");
        for (String s : ipArray) {
            if (!s.contains("/")) {
                if (s.equals(ip)) {
                    return true;
                }
                continue;
            }

            String[] ips = ip.split("\\.");
            //ip地址的十进制值
            int ipAddress = (Integer.parseInt(ips[0]) << 24)
                    | (Integer.parseInt(ips[1]) << 16)
                    | (Integer.parseInt(ips[2]) << 8) | Integer.parseInt(ips[3]);
            //掩码（0-32）
            int type = Integer.parseInt(s.replaceAll(".*/", ""));
            //匹配的位数为32 - type位（16进制的1）
            int mask = 0xFFFFFFFF << (32 - type);
            String cidrIp = s.replaceAll("/.*", "");
            //网段ip十进制
            String[] cidrIps = cidrIp.split("\\.");
            int cidrIpAddr = (Integer.parseInt(cidrIps[0]) << 24)
                    | (Integer.parseInt(cidrIps[1]) << 16)
                    | (Integer.parseInt(cidrIps[2]) << 8)
                    | Integer.parseInt(cidrIps[3]);

            if ((ipAddress & mask) == (cidrIpAddr & mask)) {
                return true;
            }
            continue;
        }
        return false;
    }

    public static String getFailObjInfo(FailObj failObj) {
        String retStr = "";
        for (Object obj : failObj.getObject()) {
            ConfigObjectId confObj = (ConfigObjectId) obj;
            ConfigObjectType confObjType = confObj.getObjectType();
            String confId = confObj.getObjectId();
            String tmp = confObjType.name() + "," + confId + ";";
            retStr = retStr + tmp;
        }
        return retStr;
    }

    public static Adapter convert2Adapter(InstanceDetails instanceDetails) {
        AdapterBuilder adapterBuilder = new AdapterBuilder();
        adapterBuilder.setIp(instanceDetails.getMyIp());
        adapterBuilder.setName(Uri.getDefaultInstance(instanceDetails.getId()));
        adapterBuilder.setAdapterVersion(instanceDetails.getSwVersion());
        adapterBuilder.setApiVersion(instanceDetails.getNbiVersion());
        adapterBuilder.setSupportedNeApiVersion(instanceDetails.getSbiVersion());
        adapterBuilder.setPort(new PortNumber(instanceDetails.getPort()));
        adapterBuilder.setLoginPasswd(instanceDetails.getPasswd());
        adapterBuilder.setLoginName(instanceDetails.getUser());

        return adapterBuilder.build();
    }

    public static TelemetryServer convert2TelemetryServer(InstanceDetails instanceDetails) {

        log.debug("convert to telemetry server,instance is :{}", instanceDetails);
        String telemetryServerName = FriendlyNameGenerator.generateTeleMapperName(instanceDetails);
        TelemetryServerBuilder builder = new TelemetryServerBuilder();
        builder.setName(new Uri(telemetryServerName));
        builder.setPort(new PortNumber(instanceDetails.getPort()));
        builder.setKey(new TelemetryServerKey(new Uri(telemetryServerName)));
        builder.setIp(instanceDetails.getHostIp());
        builder.setApiVersion(instanceDetails.getNbiVersion());
        builder.setTelemetryVersion(instanceDetails.getSwVersion());
        builder.setSupportedNeVersion(instanceDetails.getSbiVersion());
        return builder.build();

    }

    public static NtpVersion resolveNtpVersion(Node ne, String ntpIp) {
        log.debug("resolve ntp version for ne:{}", ne.getNodeId().getValue());
        String vendorName = ne.getAugmentation(Node1.class).getPhysical().getVendorName();
        if (!vendorName.equalsIgnoreCase(VENDOR_HUAWEI)) {
            return null;
        }
        if (ntpIp.contains(":")) {
            return NtpVersion.NTP_V4;
        } else {
            return NtpVersion.NTP_V3;
        }
    }

    public static IpAddressType getIpAddressType(String ip) {
        return IpUtils.getIpVersion(ip) == IpVersion.IPv4 ? IpAddressType.IPV4 : IpAddressType.IPV6;
    }

    public static Map<IpAddressType, List<Node>> dividedNeByIpType(List<Node> implementPhyNodes) {
        log.debug("divided the phy node map by ip address type");
        Map<IpAddressType, List<Node>> ipAddressTypeListMap = new HashMap<>();
        for (Node node : implementPhyNodes) {
            String ip = NeInfoUtil.getIp(node);
            IpAddressType ipAddressType = getIpAddressType(ip);
            ipAddressTypeListMap.computeIfAbsent(ipAddressType, i -> new ArrayList<>()).add(node);
        }
        return ipAddressTypeListMap;
    }

    public static String buildNorthboundTelemetryKey(Telemetry northboundTelemetry) {
        String northBoundGroupName = northboundTelemetry.getSensorGroupName();
        String northboundTelemetryIp = northboundTelemetry.getIp();
        String northboundTelemetryPort = northboundTelemetry.getPort().getValue().toString();
        return northBoundGroupName + POUND
                + northboundTelemetryIp + POUND + northboundTelemetryPort;
    }

    public static <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> partitions = new ArrayList<>();
        int size = list.size();
        for (int i = 0; i < size; i += batchSize) {
            partitions.add(list.subList(i, Math.min(size, i + batchSize)));
        }
        return partitions;
    }


    public static RemoveResourceInput buildRemoveTelemetryResourceInput(String groupIdName,
            String subscribeIp,
            int port, String neId) {
        PhysicalBuilder physicalBuilder = new PhysicalBuilder();
        physicalBuilder.setSystem(new SystemBuilder()
                .setTelemetry(Collections.singletonList(
                        new TelemetryBuilder().setSensorGroupName(groupIdName).setIp(subscribeIp)
                                .setKey(new TelemetryKey(subscribeIp, groupIdName))
                                .setPort(new PortNumber(port)).build()))
                .build());
        return new RemoveResourceInputBuilder()
                .setNodeId(NodeId.getDefaultInstance(neId))
                .setPhysical(physicalBuilder.build())
                .build();
    }

}

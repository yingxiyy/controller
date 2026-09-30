package net.flex.dci.otc.controller.ne.manager.components.balancer;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.enums.IpAddressType;
import net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils;
import net.flex.dci.otc.mongo.dao.TelemetryDao;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.telemetry.manager.TelemetryServer;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.telemetry.server.attribute.Ne;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * 2025/6/10
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class TelemetryBalancer {

    private final TelemetryDao telemetryDao;

    /**
     * pickup least load telemetry Server
     *
     * @param yangVersion
     * @return
     */
    public TelemetryServer pickupLeast(String yangVersion, IpAddressType ipAddressType) {
        log.debug("pickup least telemetryServer witch yangVersion:{}", yangVersion);
        List<TelemetryServer> telemetryServices = telemetryDao.listAllServerBySupportNeYangVersion(
                Collections.singleton(yangVersion));
        List<TelemetryServer> candidateTelemetryServers = filterCandidateTelemetry(
                telemetryServices, ipAddressType);
        return pickupLeastLoad(candidateTelemetryServers);
    }

    private List<TelemetryServer> filterCandidateTelemetry(List<TelemetryServer> telemetryServices,
            IpAddressType ipAddressType) {
        log.debug("filter candidate telemetry server size:{} and filter ipAddressType:{}",
                telemetryServices.size(), ipAddressType);
        List<TelemetryServer> candidateTelemetryServers = telemetryServices.stream()
                .filter(telemetryServer -> NeManagerUtils.getIpAddressType(telemetryServer.getIp())
                        .equals(ipAddressType))
                .collect(Collectors.toList());
        return candidateTelemetryServers;
    }

    /**
     * pickup least load telemetry server with ipArea
     *
     * @param yangVersion
     * @param ipArea
     * @return
     */
    public TelemetryServer pickupLeastWithIpArea(String yangVersion, String ipArea,
            Boolean isNeInPrivate, IpAddressType ipAddressType) {
        log.debug("pickup least witch ipArea :{} and yangVersion:{} isNeInPrivate {}", ipArea,
                yangVersion, isNeInPrivate);
        List<TelemetryServer> telemetryServers = telemetryDao.listAllServerBySupportNeYangVersion(
                Collections.singleton(yangVersion));
        log.debug("current version telemetry server :{}", telemetryServers);
        List<TelemetryServer> inIpAreaTelemetryServers = telemetryServers.stream()
                .filter(telemetryServer -> isNeInPrivate == NeManagerUtils.ipIsInNet(
                        telemetryServer.getIp(), ipArea)).collect(
                        Collectors.toList());
        List<TelemetryServer> candidateServers = filterCandidateTelemetry(inIpAreaTelemetryServers,
                ipAddressType);
        return pickupLeastLoad(candidateServers);
    }

    /**
     * load balance for the telemetry server
     *
     * @param supportedServer
     * @return
     */
    private TelemetryServer pickupLeastLoad(List<TelemetryServer> supportedServer) {
        TelemetryServer selected = null;
        int minConnections = Integer.MAX_VALUE;
        if (!CollectionUtils.isEmpty(supportedServer)) {
            for (TelemetryServer server : supportedServer) {
                int connections = server.getNe() == null ? 0 : server.getNe().size();
                log.debug("Telemetry server {} with load {}", server.getName(), connections);
                if (connections < minConnections) {
                    selected = server;
                    minConnections = connections;
                }
            }
        }
        return selected;
    }

    public List<String> getConnectNeIds() {
        log.debug("get current assign telemetry configuration ne ids");
        List<TelemetryServer> telemetryServers = telemetryDao.listTelemetryServers();
        List<String> connectedNeIds = telemetryServers.stream()
                .flatMap(telemetryServer -> {
                    java.util.Optional<List<Ne>> neListOptional = java.util.Optional.ofNullable(
                            telemetryServer.getNe());
                    return neListOptional.map(List::stream)
                            .orElseGet(java.util.stream.Stream::empty);
                })
                .map(ne -> java.util.Optional.ofNullable(ne.getNodeId()).map(Uri::getValue)
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return connectedNeIds;
    }

    public TelemetryServer getTelemetryServerByNeId(String neId) {
        log.debug("get the connected telemetry server by neId:{}", neId);
        return telemetryDao.getTelemetryServerByPhyNodeId(neId);
    }
}

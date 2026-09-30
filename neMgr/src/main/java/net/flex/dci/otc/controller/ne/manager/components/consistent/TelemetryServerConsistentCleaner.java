package net.flex.dci.otc.controller.ne.manager.components.consistent;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.enums.MapperType;
import net.flex.dci.otc.controller.ne.manager.utils.FriendlyNameGenerator;
import net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils;
import net.flex.dci.otc.mongo.dao.TelemetryDao;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import net.flex.dci.otc.zkclient4boot.refactor.utils.DciInstancesUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.telemetry.manager.TelemetryServer;
import org.springframework.stereotype.Component;

/**
 * 2025/6/11
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class TelemetryServerConsistentCleaner implements ConsistentCleaner {

    private final TelemetryDao telemetryDao;

    @Override
    public void checkAndCleanInconsistent() {
        log.debug("clean inconsistent telemetry server from db to zookeeper");
        List<InstanceDetails> telemetryServerMetrics = MapperType.TELEMETRY_COLLECTOR.getModuleNames()
                .stream()
                .flatMap(moduleName -> DciInstancesUtils.getStateInstancesByModuleName(moduleName)
                        .stream())
                .collect(Collectors.toList());
        List<TelemetryServer> telemetryServers = telemetryDao.listTelemetryServers();
        checkAndCleanInconsistent(telemetryServers, telemetryServerMetrics);
    }

    /**
     * check and clean inconsistent telemetry collector
     *
     * @param telemetryServers
     * @param telemetryServerMetrics
     */
    private void checkAndCleanInconsistent(List<TelemetryServer> telemetryServers,
            List<InstanceDetails> telemetryServerMetrics) {
        log.debug("check inconsistent telemetry server and clean");
        Map<String, InstanceDetails> telemetryServerMetricsMap = telemetryServerMetrics.stream()
                .collect(Collectors.toMap(
                        FriendlyNameGenerator::generateTeleMapperName,
                        instance -> instance));

//        Map<String, TelemetryServer> telemetryServerMap = telemetryServers.stream().collect(
//                Collectors.toMap(telemetryServer -> telemetryServer.getName().getValue(),
//                        telemetryServer -> telemetryServer));
        Map<String, List<TelemetryServer>> telemetryServerMap = telemetryServers.stream()
                .collect(Collectors.groupingBy(
                        telemetryServer -> telemetryServer.getName().getValue()
                ));
        telemetryServerMap.forEach((name, servers) -> {
            if (servers.size() > 1) {
                log.error("TelemetryServer name {} have {} duplicated key",
                        name, servers.size());
                telemetryDao.deleteTelemetryServer(name);
            }
        });
        Set<String> metricsName = telemetryServerMetricsMap.keySet();
        Set<String> telemetryServersName = telemetryServerMap.keySet();
        log.debug("in registration center metrics name:{}", metricsName);
        log.debug("in db telemetry server name:{}", telemetryServersName);

        Set<String> inDbTelemetryServerNames = NeManagerUtils.getDifferenceSetByGuava(
                telemetryServersName, metricsName);
        Set<String> inRegistrationCenters = NeManagerUtils.getDifferenceSetByGuava(metricsName,
                telemetryServersName);
        log.debug("only in db telemetry server names:{}", inDbTelemetryServerNames);
        log.debug("only in registration center metrics name:{}", inRegistrationCenters);
        List<TelemetryServer> inRegistrationTelemetryServers = inRegistrationCenters.stream()
                .map(instance -> {
                    InstanceDetails instanceDetail = telemetryServerMetricsMap.get(instance);
                    return NeManagerUtils.convert2TelemetryServer(instanceDetail);
                }).collect(
                        Collectors.toList());
        cleanInconsistent(inDbTelemetryServerNames, inRegistrationTelemetryServers);
    }

    private void cleanInconsistent(Set<String> indbTelemetryIds,
            List<TelemetryServer> inMetricTelemetryServers) {
        log.debug("remove telemetry:{} in db and create adapters:{}", indbTelemetryIds,
                inMetricTelemetryServers);
        indbTelemetryIds.forEach(telemetryDao::deleteTelemetryServer);
        inMetricTelemetryServers.forEach(telemetryDao::createTelemetryServer);
    }
}

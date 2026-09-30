package net.flex.dci.otc.controller.ne.manager.components.telemetry;

import static net.flex.dci.otc.common.constants.Constants.POUND;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.Northbound.TELEMETRY_SENSOR_GROUP_PREFIX;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils.buildNorthboundTelemetryKey;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils.buildRemoveTelemetryResourceInput;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils.dividedNeByIpType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.controller.ne.manager.components.balancer.TelemetryBalancer;
import net.flex.dci.otc.controller.ne.manager.core.service.NeResourceService;
import net.flex.dci.otc.controller.ne.manager.dto.NeInfo;
import net.flex.dci.otc.controller.ne.manager.enums.IpAddressType;
import net.flex.dci.otc.controller.ne.manager.model.SensorGroup;
import net.flex.dci.otc.controller.ne.manager.service.NeManager;
import net.flex.dci.otc.controller.ne.manager.utils.AsynchronousExecutor;
import net.flex.dci.otc.controller.ne.manager.utils.NeInfoUtil;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.TelemetryServerConfigDao;
import net.flex.dci.otc.mongo.mdoel.telemetry.TelemetryConfig;
import net.flex.dci.otc.mongo.mdoel.telemetry.TelemetryServer;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.RemoveResourceInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.RemoveResourceOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.NeSystemInfo;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.SystemBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.system.Telemetry;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.system.TelemetryBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.system.TelemetryKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * 2026/1/6
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class NorthboundTelemetryImpl implements NorthboundTelemetry {

    private final NeManager neManager;

    private final NeResourceService neResourceService;

    private final TelemetryBalancer telemetryBalancer;

    private final TelemetryManager telemetryManager;

    private final TelemetryServerConfigDao telemetryServerConfigDao;

    private static final Semaphore TELEMETRY_SEMAPHORE = new Semaphore(20);


    private final PhyNodeDao phyNodeDao;


    @Override
    public void configureTelemetry() {
        log.info("configure northbound telemetry");
        long startTime = System.currentTimeMillis();
        try {
            List<String> neIds = telemetryBalancer.getConnectNeIds();
            if (CollectionUtils.isEmpty(neIds)) {
                log.warn(
                        "no physical node need to process the operation configure northbound telemetry");
                return;
            }
            log.info("Found {} node need to configure northbound telemetry", neIds.size());
            TelemetryConfig telemetryConfig = telemetryServerConfigDao.getTelemetryConfig();

            boolean northboundEnable = telemetryConfig.isEnable();
            if (northboundEnable) {
                boolean hasValidServer = (telemetryConfig.getIpv4Server() != null
                        && telemetryConfig.getIpv4Server().isValid())
                        || (telemetryConfig.getIpv6Server() != null
                        && telemetryConfig.getIpv6Server().isValid());
                if (!hasValidServer) {
                    log.warn(
                            "Current northbound telemetry server is enable but not set valid server,discard it and do nothing");
                    return;
                }
                executeAssignNorthBoundTelemetry(neIds, telemetryConfig);
            } else {
                executeUnsubscribeNorthBoundTelemetry(neIds, telemetryConfig);
            }
            long duration = System.currentTimeMillis() - startTime;
            log.info("Finish assign northbound telemetry cost:{} ms", duration);
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to assign northbound telemetry cost:{} ms reason is:{}", duration,
                    ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Northbound telemetry assign failed:" + ex.getMessage(), ex);
        }

    }

    /**
     * unsubscribe the north bound telemetry assign configuration
     *
     * @param neIds
     * @param telemetryConfig
     */
    private void executeUnsubscribeNorthBoundTelemetry(List<String> neIds,
            TelemetryConfig telemetryConfig) {
        log.debug("disable the northbound telemetry to the phyNode:{}", neIds);
        if (CollectionUtils.isEmpty(neIds)) {
            log.info("disable the northbound telemetry to the phyNode is empty,do nothing");
            return;
        }
        int batchQuerySize = 100;
        int totalBatches = (int) Math.ceil((double) neIds.size() / batchQuerySize);
        for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
            int fromIndex = batchIndex * batchQuerySize;
            int toIndex = Math.min(fromIndex + batchQuerySize, neIds.size());
            List<String> queryList = neIds.subList(fromIndex, toIndex);
            Set<String> batchIds = new HashSet<>(queryList);
            log.debug("Process batch [{}/{}],current ne count:{}", batchIndex + 1, totalBatches,
                    batchIds.size());
            try {
                List<Node> phyNodes = phyNodeDao.listOperPhyNodeByIds(new ArrayList<>(batchIds));
                if (phyNodes.isEmpty()) {
                    log.info("there no phy node need to unsubscribe do nothing ");
                    continue;
                }
                log.debug("Process batch [{}] unsubscribe northbound telemetry to the phy node:{}",
                        batchIndex + 1,
                        phyNodes.size());
                unsubscribeNorthBoundTelemetry(phyNodes);
            } catch (Exception ex) {
                log.error("failed to unsubscribe northbound telemetry the reason is:{}",
                        ex.getMessage(),
                        ex);
            }
        }

    }

    /**
     * unsubscribe northbound telemetry
     *
     * @param phyNodes
     */
    private void unsubscribeNorthBoundTelemetry(List<Node> phyNodes) {
        List<String> phyNodeIds = phyNodes.stream().map(NodeAttributes::getNodeId)
                .map(Uri::getValue).collect(
                        Collectors.toList());
        log.debug("unsubscribe northbound telemetry to phyNode:{}", phyNodeIds);
        Map<String, List<String>> northboundTelemetryNeMap = getNorthBoundTelemetryNeMap(phyNodes);
        _unsubscribeNorthBoundTelemetry(northboundTelemetryNeMap);
    }

    /**
     * get nodes northBoundTelemetry map
     *
     * @param phyNodes
     * @return
     */
    private Map<String, List<String>> getNorthBoundTelemetryNeMap(List<Node> phyNodes) {
        if (CollectionUtils.isEmpty(phyNodes)) {
            return Collections.emptyMap();
        }
        return phyNodes.stream()
                .map(this::extractNodeIdAndNorthboundTelemetry)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        NodeTelemetryInfo::getTelemetryKey,
                        Collectors.mapping(NodeTelemetryInfo::getNodeId, Collectors.toList())
                ));
    }

    private NodeTelemetryInfo extractNodeIdAndNorthboundTelemetry(Node node) {
        log.debug("extract node northbound telemetry:{}", node.getNodeId());
        String nodeId = node.getNodeId().getValue();
        Telemetry northboundTelemetry = getNorthboundTelemetry(node);
        if (northboundTelemetry == null) {
            return null;
        }
        String key = buildNorthboundTelemetryKey(northboundTelemetry);
        return new NodeTelemetryInfo(key, nodeId);
    }

    private Telemetry getNorthboundTelemetry(Node node) {
        return Optional.ofNullable(node.getAugmentation(Node1.class))
                .map(Node1::getPhysical)
                .map(NeSystemInfo::getSystem)
                .map(org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.System::getTelemetry)
                .filter(telemetries -> !CollectionUtils.isEmpty(telemetries))
                .flatMap(telemetries -> telemetries.stream()
                        .filter(telemetry -> telemetry.getSensorGroupName() != null)
                        .filter(telemetry -> telemetry.getSensorGroupName()
                                .startsWith(TELEMETRY_SENSOR_GROUP_PREFIX))
                        .findAny())
                .orElse(null);
    }

    /**
     * unsubscribe the north bound telemetry
     *
     * @param northboundTelemetryNeMap
     */
    private void _unsubscribeNorthBoundTelemetry(
            Map<String, List<String>> northboundTelemetryNeMap) {
        if (CollectionUtils.isEmpty(northboundTelemetryNeMap)) {
            log.info("unsubscribe north bound telemetry reference is null,do nothing");
            return;
        }
        log.info("start to unsubscribe north bound telemetry，total :{}",
                northboundTelemetryNeMap.size());
        int totalUnsubscribed = 0;
        int totalFailed = 0;
        for (Map.Entry<String, List<String>> entry : northboundTelemetryNeMap.entrySet()) {
            String key = entry.getKey();
            List<String> neIds = entry.getValue();
            String[] parts = key.split(POUND);
            String groupIdName = parts[0];
            String subscribeIp = parts[1];
            int port = Integer.parseInt(parts[2]);
            log.info("unsubscribe {} ({}:{})，total {} 个NE", groupIdName, subscribeIp, port,
                    neIds.size());
            BatchResult result = processBatchUnsubscribe(groupIdName, subscribeIp, port,
                    neIds);
            totalUnsubscribed += result.getSuccessCount();
            totalFailed += result.getFailedCount();

            if (result.getFailedCount() > 0) {
                log.warn("subscribeGroup {} have {} ne failed,the failedNeId is: {}",
                        groupIdName, result.getFailedCount(), result.getFailedNeIds());
            }

        }
        log.info("Northbound telemetry unsubscription completed, successful: {}, failed: {}",
                totalUnsubscribed, totalFailed);
    }

    private BatchResult processBatchUnsubscribe(String groupIdName, String subscribeIp, int port,
            List<String> neIds) {
        log.debug("start processBatch unsubscribe");
        if (CollectionUtils.isEmpty(neIds)) {
            return new BatchResult(0, 0, Collections.emptyList());
        }
        int batchSize = 50;
        int totalBatches = (int) Math.ceil((double) neIds.size() / batchSize);

        log.debug("Processing unsubscribe {} to {} ne,totalBatch :{}", groupIdName, neIds.size(),
                totalBatches);
        int successCount = 0;
        int failedCount = 0;
        List<String> failedNeIds = new ArrayList<>();
        for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
            int startIndex = batchIndex * batchSize;
            int endIndex = Math.min(startIndex + batchSize, neIds.size());
            List<String> batchNeIds = neIds.subList(startIndex, endIndex);
            BatchResult result = executeUnsubscribeBatch(groupIdName, subscribeIp, port,
                    batchNeIds);
            successCount += result.successCount;
            failedCount += result.failedCount;
            failedNeIds.addAll(result.getFailedNeIds());
            if (batchIndex < totalBatches - 1) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return new BatchResult(successCount, failedCount, failedNeIds);
    }

    private BatchResult executeUnsubscribeBatch(String groupIdName, String subscribeIp, int port,
            List<String> neIds) {
        int successCount = 0;
        List<String> failedNeIds = new ArrayList<>();

        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        log.info("unsubscribe the ne:{} northbound telemetry:{} ip:{} port:{}", neIds, groupIdName,
                subscribeIp, port);
        for (String neId : neIds) {
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    TELEMETRY_SEMAPHORE.acquire();
                    RemoveResourceInput removeResourceInput = buildRemoveTelemetryResourceInput(
                            groupIdName,
                            subscribeIp, port, neId);
                    RemoveResourceOutput output = neResourceService.removeResource(
                            removeResourceInput);
                    if (output.getSuccessObj() != null
                            && output.getSuccessObj().getObject() != null) {
                        return true;
                    } else {
                        return false;
                    }
                } catch (Exception e) {
                    log.error("unsubscribe NE {} northbound telemetry failed", neId, e);
                    return false;
                } finally {
                    TELEMETRY_SEMAPHORE.release();
                }
            }, AsynchronousExecutor.getTelemetryExecutor());
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .join();

        for (int i = 0; i < futures.size(); i++) {
            try {
                Boolean success = futures.get(i).get();
                String neId = neIds.get(i);

                if (Boolean.TRUE.equals(success)) {
                    successCount++;
                    log.debug("NE {} unsubscribe successfully", neId);
                } else {
                    failedNeIds.add(neId);
                    log.warn("NE {} unsubscribe failed", neId);
                }
            } catch (Exception e) {
                failedNeIds.add(neIds.get(i));
                log.error("unsubscribe failed", e);
            }
        }

        return new BatchResult(successCount, failedNeIds.size(), failedNeIds);
    }


    /**
     * enable the north bound telemetry assign configuration
     *
     * @param neIds
     * @param telemetryConfig
     */
    private void executeAssignNorthBoundTelemetry(List<String> neIds,
            TelemetryConfig telemetryConfig) {
        log.debug("assign northbound telemetry to phyNode:{}", neIds);
        if (CollectionUtils.isEmpty(neIds)) {
            log.info("assign northbound telemetry to the phyNode is empty,do nothing");
            return;
        }
        int batchQuerySize = 100;
        int totalBatches = (int) Math.ceil((double) neIds.size() / batchQuerySize);

        for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
            int fromIndex = batchIndex * batchQuerySize;
            int toIndex = Math.min(fromIndex + batchQuerySize, neIds.size());
            List<String> queryList = neIds.subList(fromIndex, toIndex);
            Set<String> batchIds = new HashSet<>(queryList);
            log.debug("Process batch [{}/{}]，current ne count: {}", batchIndex + 1, totalBatches,
                    batchIds.size());
            try {
                List<Node> phyNodes = phyNodeDao.listOperPhyNodeByIds(new ArrayList<>(batchIds));
                if (phyNodes.isEmpty()) {
                    log.info("there no phy node need to configure northbound ");
                    continue;
                }
                log.debug("Process batch [{}] need assign northbound telemetry to the phy node:{}",
                        batchIndex + 1,
                        phyNodes.size());
                Map<IpAddressType, List<Node>> ipAddressTypeNodeMap = dividedNeByIpType(
                        phyNodes);
                assignNorthboundTelemetry(ipAddressTypeNodeMap, telemetryConfig);
            } catch (Exception ex) {
                log.error("failed to assignNorthboundTelemetry the reason is:{}", ex.getMessage(),
                        ex);
            }
        }
    }

    private void assignNorthboundTelemetry(Map<IpAddressType, List<Node>> ipAddressTypeNodeMap,
            TelemetryConfig telemetryConfig) {
        log.info("configure northbound telemetry by ip address (IPv4 and IPv6)");
        int ipv4Count = ipAddressTypeNodeMap.getOrDefault(IpAddressType.IPV4,
                Collections.emptyList()).size();
        int ipv6Count = ipAddressTypeNodeMap.getOrDefault(IpAddressType.IPV6,
                Collections.emptyList()).size();
        log.info("IPv4 phyNode: {}, IPv6 phyNode: {}", ipv4Count, ipv6Count);
        for (Map.Entry<IpAddressType, List<Node>> entry : ipAddressTypeNodeMap.entrySet()) {
            IpAddressType ipAddressType = entry.getKey();
            List<Node> nodes = entry.getValue();
            if (CollectionUtils.isEmpty(nodes)) {
                log.debug("No nodes found for IP type: {}", ipAddressType);
                continue;
            }
            log.info("reassign the {} ipType phy node:{}", ipAddressType, nodes.size());
            switch (ipAddressType) {
                case IPV4:
                    processAssignNorthboundTelemetry(nodes, telemetryConfig.getIpv4Server());
                    break;
                case IPV6:
                    processAssignNorthboundTelemetry(nodes, telemetryConfig.getIpv6Server());
                    break;
            }
        }
    }

    private void processAssignNorthboundTelemetry(List<Node> nodes,
            TelemetryServer telemetryServer) {
        log.info("Process assign northbound telemetry server:{} port:{} to the phyNode count:{}",
                telemetryServer.getServerAddress(), telemetryServer.getPort(), nodes.size());
        int batchSize = 50;
        int totalBatches = (int) Math.ceil((double) nodes.size() / batchSize);
        int totalCount = nodes.size();
        for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
            int startIndex = batchIndex * batchSize;
            int toIndex = Math.min(startIndex + batchSize, totalCount);
            List<Node> configNodes = nodes.subList(startIndex, toIndex);
            log.debug("Processing batch {}/{}: Node {}-{}/{}", batchIndex + 1, totalBatches,
                    startIndex + 1, toIndex,
                    nodes.size());
            processBatchConfigTelemetry(configNodes, telemetryServer);

        }
    }

    private void processBatchConfigTelemetry(List<Node> configNodes,
            TelemetryServer telemetryServer) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Node node : configNodes) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    TELEMETRY_SEMAPHORE.acquire();
                    assignNorthBoundTelemetry2Node(node, telemetryServer);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    TELEMETRY_SEMAPHORE.release();
                }
            }, AsynchronousExecutor.getTelemetryExecutor());
            futures.add(future);
        }
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(60, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("Processing batch time out");
        } catch (Exception e) {
            log.error("Batch failed", e);
        }
    }

    private void assignNorthBoundTelemetry2Node(Node node, TelemetryServer telemetryServer) {
        String nodeId = node.getNodeId().getValue();
        try {
            log.info("assign northbound telemetry to node the neId:{},northbound telemetry:{}",
                    nodeId, telemetryServer);
            SensorGroup sensorGroup = telemetryManager.getNorthboundTelemetryConfigurationByNe(
                    node);
            NeInfo neInfo = NeInfoUtil.getNeInfo(node);
            Telemetry telemetry = new TelemetryBuilder()
                    .setIp(telemetryServer.getServerAddress())
                    .setKey(new TelemetryKey(telemetryServer.getServerAddress(),
                            sensorGroup.getSensorGroupName()))
                    .setSensorGroupName(sensorGroup.getSensorGroupName())
                    .setSensorGroupPath(sensorGroup.getSensorGroupPath())
                    .setHeartbeatInterval(sensorGroup.getHeartbeatInterval())
                    .setSampleInterval(sensorGroup.getSampleInterval())
                    .setSuppressRedundant(sensorGroup.getSuppressRedundant())
                    .setPort(new PortNumber(telemetryServer.getPort()))
                    .setLocalSourceAddress(neInfo.getIp()) //set telemetry report ip
                    .build();
            SystemBuilder systemBuilder = new SystemBuilder();
            systemBuilder.setTelemetry(Collections.singletonList(telemetry));
            PhysicalBuilder physicalBuilder = new PhysicalBuilder();
            physicalBuilder.setSystem(systemBuilder.build());
            ConfigNeOutput configOutput = neManager.configNe(nodeId, physicalBuilder.build(),
                    new ArrayList<>());
            if (configOutput.getFailObj() != null
                    && configOutput.getFailObj().getObject() != null
                    && !configOutput.getFailObj().getObject().isEmpty()) {
                log.error("Northbound telemetry configuration failed for NE: {}, Error: {}",
                        nodeId, configOutput.getFailObj());
            } else {
                log.info("Northbound telemetry configuration successful for NE: {}", nodeId);
            }

        } catch (Exception ex) {
            log.error("Exception assign northbound telemetry to NE {}: {}", nodeId, ex.getMessage(),
                    ex);
        }

    }

    @Data
    @AllArgsConstructor
    private static class NodeTelemetryInfo {

        private String telemetryKey;
        private String nodeId;
    }


    /**
     * 批量处理结果类
     */
    @Data
    @AllArgsConstructor
    private static class BatchResult {

        private int successCount;
        private int failedCount;
        private List<String> failedNeIds;
    }


}

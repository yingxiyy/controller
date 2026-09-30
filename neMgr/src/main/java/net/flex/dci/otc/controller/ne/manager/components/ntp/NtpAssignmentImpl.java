package net.flex.dci.otc.controller.ne.manager.components.ntp;

import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.TIMEZONE;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils.dividedNeByIpType;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils.resolveNtpVersion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.controller.ne.manager.components.balancer.AdapterBalancer;
import net.flex.dci.otc.controller.ne.manager.dto.ManageNeSynchroInfo;
import net.flex.dci.otc.controller.ne.manager.enums.IpAddressType;
import net.flex.dci.otc.controller.ne.manager.enums.NtpVersion;
import net.flex.dci.otc.controller.ne.manager.service.NeManager;
import net.flex.dci.otc.controller.ne.manager.utils.AsynchronousExecutor;
import net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils;
import net.flex.dci.otc.mongo.dao.NtpConfigDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.mdoel.ntp.NtpConfig;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.SystemBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.system.Ntp;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.system.NtpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * 2026/1/5
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class NtpAssignmentImpl implements NtpAssignment {

    private final AdapterBalancer adapterBalancer;

    private final PhyNodeDao phyNodeDao;

    private final NtpConfigDao ntpConfigDao;

    private final NeManager neManager;

    private static final Semaphore NTP_SEMAPHORE = new Semaphore(10);


    @Override
    public void reassignNtpServer() {
        log.info("start to reassign ntp server");
        long startTime = System.currentTimeMillis();
        try {
            Set<String> nodeIds = getPhysicalNeIds();
            if (CollectionUtils.isEmpty(nodeIds)) {
                log.warn("no physical node need to process the operation reassign ntp server");
                return;
            }
            log.info("Found {} nodes to process reassign ntp", nodeIds.size());
            NtpConfig ntpConfig = ntpConfigDao.getNtpConfig();
            if (!ntpConfig.getIpv4NtpServer().isValid() && !ntpConfig.getIpv6NtpServer()
                    .isValid()) {
                log.warn("current reassign ntp server ip address is not set,discard it");
                return;
            }
            executeReassignment(nodeIds, ntpConfig);
            long duration = System.currentTimeMillis() - startTime;
            log.info("finish reassign ntp duration :{} ms", duration);
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("ntp reassign failed  {}ms, {}", duration, ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "ntp reassign failed:" + ex.getMessage(), ex);
        }

    }

    private void executeReassignment(Set<String> nodeIds, NtpConfig ntpConfig) {
        log.debug("execute reassign ntp to the phyNodeIds:{}", nodeIds);
        if (CollectionUtils.isEmpty(nodeIds)) {
            log.info("there no ne need to reassign ntp,do nothing");
            return;
        }
        int queryBatchSize = 100;
        List<String> idList = new ArrayList<>(nodeIds);
        int totalBatches = (int) Math.ceil((double) idList.size() / queryBatchSize);
        int totalProcessed = 0;
        for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
            int fromIndex = batchIndex * queryBatchSize;
            int toIndex = Math.min(fromIndex + queryBatchSize, idList.size());
            List<String> batchIds = idList.subList(fromIndex, toIndex);
            Set<String> batchIdSet = new HashSet<>(batchIds);
            log.debug("Process batch [{}/{}]，current ne count: {}", batchIndex + 1, totalBatches,
                    batchIdSet.size());
            try {
                List<Node> refConfigPhyNodes = phyNodeDao.listConfigPhyNodeByIds(
                        new ArrayList<>(batchIdSet));
                List<Node> implementPhyNodes = refConfigPhyNodes.stream()
                        .filter(node -> node.getAugmentation(
                                Node1.class).getPhysical().getImplementState()
                                == ImplementState.Implement).collect(
                                Collectors.toList());
                if (implementPhyNodes.isEmpty()) {
                    log.info("there no phy node need to reassign ntp");
                    continue;
                }
                log.debug("Process batch [{}] need reassign ntp to the phy node:{}", batchIndex + 1,
                        implementPhyNodes.size());

                Map<IpAddressType, List<Node>> ipAddressTypeNodeMap = dividedNeByIpType(
                        implementPhyNodes);
                reassignNtpServerByIpType(ipAddressTypeNodeMap, ntpConfig);
            } catch (Exception e) {
                log.error(
                        "Batch processing failure [Batch {}/{}]; Sample NE IDs: {}; Root cause: {}",
                        batchIndex + 1, totalBatches,
                        batchIds.stream().limit(5).collect(Collectors.toList()),
                        e.getMessage(), e);
            }
        }
        log.info(
                "Completion of NTP reassignment: {} network elements processed from an initial set of {}.",
                totalProcessed, nodeIds.size());
    }

    private void reassignNtpServerByIpType(Map<IpAddressType, List<Node>> ipAddressTypeNodeMap,
            NtpConfig ntpConfig) {
        log.info("reassign ntp by ip address (IPv4 and IPv6)");
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
                    processReassignNtp(nodes, ntpConfig.getIpv4NtpServer().getAddress(),
                            ntpConfig.getTimezone());
                    break;
                case IPV6:
                    processReassignNtp(nodes, ntpConfig.getIpv6NtpServer().getAddress(),
                            ntpConfig.getTimezone());
                    break;
            }

        }
    }

    private void processReassignNtp(List<Node> nodes, String address, String timezone) {
        log.info(
                "process reassign ntp server to the address:{} and timezone is:{}，phyNode count:{}",
                address, timezone, nodes.size());
        int batchSize = 30;
        int totalBatches = (int) Math.ceil((double) nodes.size() / batchSize);
        int totalNodes = nodes.size();
        for (int i = 0; i < totalBatches; i++) {
            int start = i * batchSize;
            int end = Math.min(start + batchSize, nodes.size());
            List<Node> batch = nodes.subList(start, end);
            log.debug("Processing batch {}/{}: Node {}-{}/{}", i + 1, totalBatches, start + 1, end,
                    nodes.size());
            processBatch(batch, address, timezone);
            log.debug("Processing batch {}/{} finished", i + 1, totalBatches);
            if (i < totalBatches - 1) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        log.info("ntp reassign to phy node:{} finished", totalNodes);
    }

    private void processBatch(List<Node> batch, String address, String timezone) {
        CountDownLatch latch = new CountDownLatch(batch.size());
        for (Node node : batch) {

            AsynchronousExecutor.submit(() -> {
                try {
                    NTP_SEMAPHORE.acquire();
                    assignNtp2Node(node, address, timezone);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    NTP_SEMAPHORE.release();
                    latch.countDown();
                }
            }, AsynchronousExecutor.getNtpExecutor());
        }
        try {
            boolean completed = latch.await(60, TimeUnit.SECONDS);
            if (!completed) {
                long remaining = latch.getCount();
                log.warn("Processing batch time out，still have {} phy node not completely",
                        remaining);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void assignNtp2Node(Node node, String address, String timezone) {
        String nodeId = node.getNodeId().getValue();
        try {
            log.info("assign ntp server:{} timezone:{} to the ne:{}", address, timezone, nodeId);
            Ntp ntp = buildNtpConfig(node, address);
            SystemBuilder systemBuilder = new SystemBuilder();
            systemBuilder.setNtp(Collections.singletonList(ntp));
            PhysicalBuilder physicalBuilder = new PhysicalBuilder();
            List<Property> listPro = new ArrayList<>();
            //add timezone
            Property pro = new PropertyBuilder().setName(TIMEZONE)
                    .setValue(timezone)
                    .build();
            listPro.add(pro);
            Properties properties = new PropertiesBuilder().setProperty(listPro).build();
            systemBuilder.setProperties(properties);
            physicalBuilder.setSystem(systemBuilder.build());
            ConfigNeOutput configOutput = neManager.configNe(
                    node.getNodeId().getValue(), physicalBuilder.build(), new ArrayList<>());
            if (configOutput.getFailObj() != null
                    && configOutput.getFailObj().getObject() != null
                    && !configOutput.getFailObj().getObject().isEmpty()) {
                log.error("NTP configuration failed for NE: {}, Error: {}",
                        nodeId, configOutput.getFailObj());
            } else {
                log.info("NTP configuration successful for NE: {}", nodeId);
            }
        } catch (Exception e) {
            log.error("Exception assigning NTP to NE {}: {}", nodeId, e.getMessage(), e);
        }
    }

    private Ntp buildNtpConfig(Node node, String address) {
        NtpVersion ntpVersion = resolveNtpVersion(node, address);
        NtpBuilder ntpBuilder = new NtpBuilder();
        ntpBuilder.setIp(address);
        if (ntpVersion != null) {
            ntpBuilder.setVersion(ntpVersion.getValue());
        }
        return ntpBuilder.build();
    }


    private Set<String> getPhysicalNeIds() {
        log.debug("get need assign ntp ");
        List<String> phyNodeIds = phyNodeDao.listAllExistIpInConfigNodeIds();
        if (phyNodeIds.isEmpty()) {
            return new HashSet<>();
        }
        List<ManageNeSynchroInfo> managedNes = adapterBalancer.getManagedNeIds();
        List<String> managedNeIds = managedNes.stream().map(ManageNeSynchroInfo::getNeId).collect(
                Collectors.toList());
        Set<String> realManagedNeIds = NeManagerUtils.getIntersectionSetByGuava(
                new HashSet<>(managedNeIds), new HashSet<>(phyNodeIds));
        return realManagedNeIds;
    }


}

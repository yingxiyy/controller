package net.flex.dci.otn.controller.resource.statistic.core.manager;

import static net.flex.dci.otn.controller.resource.statistic.core.utils.ResourceStatisticConstants.BATCH_SIZE;

import com.google.common.collect.Lists;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.OchLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.TunnelIdNamingRule;
import net.flex.dci.otc.mongo.dao.TerminationPointDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import net.flex.dci.otn.controller.resource.statistic.rest.lldp.LLDPInfo;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import net.flex.dci.otn.topology.cache.model.PhyNodeCache;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.lldp.neighbor.Neighbor;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.lldp.attributes.Lldp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * 2026/7/18
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class LLdpInventoryManager extends AbstractElementInventoryManager<LLDPInfo> {

    private final TunnelDao tunnelDao;

    private final DciTopologyCacheManager topologyCacheManager;

    private final TerminationPointDao terminationPointDao;


    @Override
    public LLDPInfo getInventoryDetail(String id) {
        return null;
    }

    @Override
    public List<LLDPInfo> getInventoryDetails(List<String> ids) {
        return Collections.emptyList();
    }

    @Override
    public List<LLDPInfo> getInventoryDetails(List<String> ids, List<String> subnetIds) {
        log.debug("get inventory details the tunnel:{} and subnetIds:{}", ids, subnetIds);
        List<String> fetchTunnelIds = new ArrayList<>();
        if (!CollectionUtils.isEmpty(ids)) {
            fetchTunnelIds.addAll(ids);
        } else if (!CollectionUtils.isEmpty(subnetIds)) {
            List<String> tunnelIds = fetchTunnelIdsBySubnetIds(subnetIds);
            if (tunnelIds != null && !tunnelIds.isEmpty()) {
                fetchTunnelIds.addAll(tunnelIds);
            }
        } else {
            log.warn("Querying ALL LLDP details without any filter.");
            List<String> implementTunnelIds = fetchAllImplementTunnelIds();
            return fetchAllTunnelLLdpInfos(implementTunnelIds);
        }

        if (fetchTunnelIds.isEmpty()) {
            log.debug("No valid tunnel IDs found, returning empty list.");
            return Collections.emptyList();
        }

        return fetchAllTunnelLLdpInfos(fetchTunnelIds);
    }

    private List<String> fetchAllImplementTunnelIds() {
        List<String> tunnelIds = tunnelDao.getAllImplementTunnelIds();
        return tunnelIds;
    }

    private List<LLDPInfo> fetchAllTunnelLLdpInfos(List<String> tunnelIds) {
        log.debug("fetch all tunnel lldp infos ids size:{}", tunnelIds.size());
        if (CollectionUtils.isEmpty(tunnelIds)) {
            return Collections.emptyList();
        }
        List<LLDPInfo> lldpInfos = new ArrayList<>();
        List<List<String>> partitions = Lists.partition(tunnelIds, BATCH_SIZE);
        log.info("tunnel export total:{}, divided into {} partition, each partition is {}",
                tunnelIds.size(), partitions.size(),
                BATCH_SIZE);
        for (int i = 0; i < partitions.size(); i++) {
            List<String> ids = partitions.get(i);
            log.info("start to process {} tunnel LLDP,size:{}", i + 1, ids.size());
            List<LLDPInfo> details = processSingleBatchSize(ids);
            lldpInfos.addAll(details);
        }
        return lldpInfos;
    }

    private List<LLDPInfo> processSingleBatchSize(List<String> tunnelIds) {
        log.debug("process single batch id:{}", tunnelIds);
        List<LinkStateDto> tunnelStates = tunnelDao.getAllTunnelsStateByTunnelIds(tunnelIds);
        Map<String, TerminationPoint> terminationPointMap = batchGetTerminationPoint(tunnelStates);
        List<LLDPInfo> lldpInfos = constructLLdpInfo(tunnelStates, terminationPointMap);
        return lldpInfos;
    }

    private List<LLDPInfo> constructLLdpInfo(List<LinkStateDto> tunnelStates,
            Map<String, TerminationPoint> terminationPointMap) {
        List<LLDPInfo> lldpInfos = new ArrayList<>();
        for (LinkStateDto tunnelState : tunnelStates) {
            String tunnelId = tunnelState.getId();
            String tunnelName = tunnelState.getFriendlyName();
            String subnet = tunnelState.getSubnetName();
            String srcCTpId = TunnelIdNamingRule.getATp(tunnelId);
            String destCTpId = TunnelIdNamingRule.getZtp(tunnelId);
            String ochLinkId = tunnelState.getSupportingLink().get(0);
            String srcLTpId = OchLinkIdNamingRule.getTpAId(ochLinkId);
            String destLTpId = OchLinkIdNamingRule.getTpZId(ochLinkId);
            TerminationPoint sourceLTp = terminationPointMap.get(srcLTpId);
            TerminationPoint destLTp = terminationPointMap.get(destLTpId);
            TerminationPoint sourceCTp = terminationPointMap.get(srcCTpId);
            TerminationPoint destCTp = terminationPointMap.get(destCTpId);
            Lldp srcLLDP = sourceCTp == null ? null : getTpLLdp(sourceCTp);
            Lldp destLLDP = destCTp == null ? null : getTpLLdp(destCTp);
            LLDPInfo lldpInfo = buildLlDpInfo(subnet, tunnelName, tunnelId, sourceCTp, destCTp,
                    sourceLTp,
                    destLTp,
                    srcLLDP,
                    destLLDP);
            lldpInfos.add(lldpInfo);
        }
        return lldpInfos;
    }

    private LLDPInfo buildLlDpInfo(String subnet, String tunnelName, String tunnelId,
            TerminationPoint sourceCTp,
            TerminationPoint destCTp,
            TerminationPoint sourceLTp,
            TerminationPoint destLTp,
            Lldp srcLLDP, Lldp destLLDP) {
        String sourceCTpId = sourceCTp.getTpId().getValue();
        String destCtpId = destCTp.getTpId().getValue();
        String sourceNeId = PhysicalTpIdNamingRule.getNodeId(sourceCTpId);
        String destNeId = PhysicalTpIdNamingRule.getNodeId(destCtpId);

        String sourceCtpName = getTerminationPointName(sourceCTp);
        String destCtpName = getTerminationPointName(destCTp);

        String sourceLtpName = getTerminationPointName(sourceLTp);
        String destLtpName = getTerminationPointName(destLTp);

        PhyNodeCache sourceNodeCache = topologyCacheManager.getValue(sourceNeId,
                PhyNodeCache.class);
        PhyNodeCache destNodeCache = topologyCacheManager.getValue(destNeId,
                PhyNodeCache.class);
        String sourceNe = sourceNodeCache.getFriendlyName();
        String destNe = destNodeCache.getFriendlyName();

        LLDPInfo.LLDPInfoBuilder<?, ?> lldpInfoBd = LLDPInfo.builder()
                .tunnelName(tunnelName)
                .subnet(subnet)
                .tunnelId(tunnelId)
                .sourceNe(sourceNe)
                .sourceSite(sourceNodeCache.getSiteName())
                .destinationSite(destNodeCache.getSiteName())
                .destinationNe(destNe)
                .sourceClientPort(sourceCtpName)
                .destinationClientPort(destCtpName)
                .sourceLinePort(sourceLtpName)
                .destinationLinePort(destLtpName);
        if (srcLLDP != null && !CollectionUtils.isEmpty(srcLLDP.getNeighbor())) {
            Neighbor srcNeighbor = srcLLDP.getNeighbor().get(0);
            lldpInfoBd.aTransmissionRemoteChassis(srcNeighbor.getChassisId())
                    .aTransmissionRemotePort(srcNeighbor.getPortId())
                    .aTransmissionManagerAddress(srcNeighbor.getManagementAddress())
                    .aTransmissionSystemName(srcNeighbor.getSystemName())
                    .sourceNeighborEstablishTime(srcNeighbor.getLastUpdate())
            ;
        }
        if (destLLDP != null && !CollectionUtils.isEmpty(destLLDP.getNeighbor())) {
            Neighbor destNeighbor = destLLDP.getNeighbor().get(0);
            lldpInfoBd.zTransmissionRemoteChassis(destNeighbor.getChassisId())
                    .zTransmissionRemotePort(destNeighbor.getPortId())
                    .zTransmissionManagerAddress(destNeighbor.getManagementAddress())
                    .zTransmissionSystemName(destNeighbor.getSystemName())
                    .destinationNeighborEstablishTime(destNeighbor.getLastUpdate());
        }
        return lldpInfoBd.build();
    }

    private String getTerminationPointName(TerminationPoint terminationPoint) {
        Physical tpPhysical = terminationPoint.getAugmentation(TerminationPoint1.class)
                .getPhysical();
        return tpPhysical.getFriendlyName();
    }


    private Map<String, TerminationPoint> batchGetTerminationPoint(
            List<LinkStateDto> serviceInfos) {
        log.debug("batch get termination Point infos:{}", serviceInfos.size());
        List<TunnelInfo> tunnelInfos = buildTunnelInfo(serviceInfos);
        Set<String> tpIds = tunnelInfos.stream().flatMap(tunnelInfo -> {
                    String srcCport = tunnelInfo.getSourceClientTpId();
                    String destCport = tunnelInfo.getDestinationClientTpId();
                    String destLport = tunnelInfo.getDestinationLineTpId();
                    String srcLport = tunnelInfo.getSourceLineTpId();
                    return Arrays.asList(srcLport, srcCport, destLport, destCport).stream();
                })
                .collect(Collectors.toSet());
        Set<String> neIds = tpIds.stream().map(PhysicalTpIdNamingRule::getNodeId)
                .collect(
                        Collectors.toSet());
        Map<String, Map<String, TerminationPoint>> neTpMapOp = terminationPointDao.batchGetOpNeTpMap(
                neIds, tpIds);
        Map<String, TerminationPoint> resultMap = new HashMap<>();
        Set<String> foundTpIds = new HashSet<>();
        for (Map.Entry<String, Map<String, TerminationPoint>> neEntry : neTpMapOp.entrySet()) {
            for (Map.Entry<String, TerminationPoint> tpEntry : neEntry.getValue().entrySet()) {
                resultMap.put(tpEntry.getKey(), tpEntry.getValue());
                foundTpIds.add(tpEntry.getKey());
            }
        }

        Set<String> missingTpIds = new HashSet<>(tpIds);
        missingTpIds.removeAll(foundTpIds);
        if (!missingTpIds.isEmpty()) {
            log.debug("Found {} tps missing in operational,fallback to config missing Ids:{}",
                    missingTpIds.size(), missingTpIds);
            Set<String> missingNeIds = missingTpIds.stream().map(PhysicalTpIdNamingRule::getNodeId)
                    .collect(
                            Collectors.toSet());
            Map<String, Map<String, TerminationPoint>> neTpMapConfig =
                    terminationPointDao.batchGetConfigNeTpMap(missingNeIds, missingTpIds);
            for (Map.Entry<String, Map<String, TerminationPoint>> neEntry : neTpMapConfig.entrySet()) {
                for (Map.Entry<String, TerminationPoint> tpEntry : neEntry.getValue().entrySet()) {
                    resultMap.putIfAbsent(tpEntry.getKey(), tpEntry.getValue());
                }
            }

        }

        return resultMap;
    }

    private List<TunnelInfo> buildTunnelInfo(List<LinkStateDto> serviceInfos) {
        List<TunnelInfo> tunnelDetails = serviceInfos.stream().map(
                        linkStateDto -> {
                            String tunnelId = linkStateDto.getId();
                            String srcClientPort = TunnelIdNamingRule.getZtp(tunnelId);
                            String destClientPort = TunnelIdNamingRule.getATp(tunnelId);
                            String ochLinkId = linkStateDto.getSupportingLink().get(0);
                            String srcLinePort = OchLinkIdNamingRule.getTpAId(ochLinkId);
                            String destLinePort = OchLinkIdNamingRule.getTpZId(ochLinkId);
                            return new TunnelInfo(srcClientPort, destClientPort, srcLinePort, destLinePort,
                                    ochLinkId);
                        }
                )
                .collect(Collectors.toList());
        return tunnelDetails;
    }


    private List<String> fetchTunnelIdsBySubnetIds(List<String> subnetIds) {
        log.debug("fetch implement tunnelIds by subnetIds:{}", subnetIds);
        List<String> tunnelIds = tunnelDao.retrieveAllImplementTunnelIdsBySubnetIds(subnetIds);
        return tunnelIds;
    }

    private Lldp getTpLLdp(TerminationPoint tp) {
        Physical tpPhysical = tp.getAugmentation(
                TerminationPoint1.class).getPhysical();
        return tpPhysical.getLldp();
    }

    public List<LLDPInfo> retrieveAllTunnelLLdpInfos(List<LinkStateDto> tunnelState) {
        log.debug("retrieve all tunnel lldp infos the size :{}", tunnelState.size());
        Map<String, TerminationPoint> terminationPointMap = batchGetTerminationPoint(tunnelState);
        List<LLDPInfo> lldpInfos = constructLLdpInfo(tunnelState, terminationPointMap);
        return lldpInfos;
    }


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    private static class TunnelInfo implements Serializable {

        private String sourceClientTpId;
        private String destinationClientTpId;
        private String sourceLineTpId;
        private String destinationLineTpId;
        private String ochLinkId;
    }
}

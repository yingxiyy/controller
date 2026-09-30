package net.flex.dci.otn.controller.resource.statistic.core.inventory;

import static net.flex.dci.otn.controller.resource.statistic.utils.CommonUtils.exportCsvFileName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.TerminationPointDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otn.controller.resource.statistic.core.enums.InventoryType;
import net.flex.dci.otn.controller.resource.statistic.core.manager.LLdpInventoryManager;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.InventoryExportData;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.UnifiedExportRequest;
import net.flex.dci.otn.controller.resource.statistic.export.LLDPCsv;
import net.flex.dci.otn.controller.resource.statistic.rest.lldp.LLDPInfo;
import net.flex.dci.otn.controller.resource.statistic.utils.ResourceConvertor;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 11/5/2025 4:23 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LldpInventory extends AbstractInventory<LLDPCsv> {

    private final TunnelDao tunnelDao;

    private final OchLinkDao ochLinkDao;

    private final TerminationPointDao terminationPointDao;

    private final DciTopologyCacheManager topologyCacheManager;

    private final LLdpInventoryManager lLdpInventoryManager;


    @Override
    public InventoryType inventoryScope() {
        return InventoryType.LLDP;
    }

    @Override
    public InventoryExportData<LLDPCsv> exportData(UnifiedExportRequest unifiedExportRequest) {
        log.debug("export data for the unified export request:{}", unifiedExportRequest);
//        List<String> neIds = unifiedExportRequest.getNeIds();
//        List<String> siteIds = unifiedExportRequest.getSiteIds();
//        List<String> subnetIds = unifiedExportRequest.getSubnet();
//        List<String> tunnelIds = unifiedExportRequest.getTunnelIds();
//        List<FilterCondition> filters = unifiedExportRequest.getFilters();
//        List<FilterItem> filterItems = FilterItemUtils.getFilterItems(
//                filters);
//        List<String> tpIds = new ArrayList<>();
//        List<Lldp> lldps = phyNodeDao.fetchAllLldpInfo(neIds, tpIds, subnetIds, new HashMap<>(),
//                new ArrayList<>());
//        List<ServiceInfoDto> serviceInfos = tunnelDao.fetchAllLLDPInfos(tunnelIds,
//                subnetIds, filterItems);
//        Map<String, TerminationPoint> terminationPointMap = batchGetTerminationPoint(serviceInfos);
//        List<LLDPInfo> lldpInfos = constructLLdpInfo(serviceInfos, terminationPointMap);
//        List<LLDPCsv> lldpcsvs = ConvertorCsvUtils.convert2LLdPCsvDatas(lldpInfos);
//        String filename = exportCsvFileName(unifiedExportRequest);
//        InventoryExportData<LLDPCsv> exportData = InventoryExportData.<LLDPCsv>builder()
//                .clazz(getClazz()).filename(filename).sheetName(inventoryScope().name())
//                .exportDatas(lldpcsvs).build();
//        return exportData;
        List<LLDPCsv> tunnelCsvs = getQueryData(unifiedExportRequest);
        String filename = exportCsvFileName(unifiedExportRequest);
        return InventoryExportData.<LLDPCsv>builder()
                .exportDatas(tunnelCsvs)
                .clazz(getClazz())
                .filename(filename)
                .sheetName(inventoryScope().name())
                .build();
    }

    private List<LLDPCsv> getQueryData(UnifiedExportRequest unifiedExportRequest) {
        log.debug("get lldp query data,the request :{}", unifiedExportRequest);
        List<String> subnetIds = unifiedExportRequest.getSubnet();
        List<String> siteLinkIds = unifiedExportRequest.getSiteLinkIds();
        List<String> refTunnelIds = getRefTunnelIdsBySiteLinkIds(siteLinkIds);
        List<String> tunnelIds = new ArrayList<>();
        tunnelIds.addAll(refTunnelIds);
        return fetchTunnelLLdpInfos(tunnelIds, subnetIds);
    }

    private List<LLDPCsv> fetchTunnelLLdpInfos(List<String> tunnelIds, List<String> subnetIds) {
        log.debug("fetch tunnel lldp the tunnel Id:{} ,subnetIds:{}", tunnelIds, subnetIds);
        List<LLDPInfo> lldpInfos = lLdpInventoryManager.getInventoryDetails(tunnelIds, subnetIds);
        List<LLDPCsv> lldpCsvs = ResourceConvertor.convert2LLdpCsv(lldpInfos);
        return lldpCsvs;
    }

    private List<String> getRefTunnelIdsBySiteLinkIds(List<String> siteLinkIds) {
        log.debug("get ref tunnel ids by site linkIds:{}", siteLinkIds);
        if (CollectionUtils.isEmpty(siteLinkIds)) {
            return Collections.emptyList();
        }
        List<String> ochLinkIds = ochLinkDao.retrieveAllOchLinkBySupportingLinkIds(siteLinkIds);
        List<String> refTunnelIds = tunnelDao.retrieveAllTunnelIdsByOchLinkIds(ochLinkIds);
        return refTunnelIds;
    }

//    private List<LLDPInfo> constructLLdpInfo(List<ServiceInfoDto> serviceInfos,
//            Map<String, TerminationPoint> terminationPointMap) {
//        List<LLDPInfo> lldpInfos = new ArrayList<>();
//        for (ServiceInfoDto serviceInfoDto : serviceInfos) {
//            String tunnelId = serviceInfoDto.getTunnelId();
//            String tunnelName = serviceInfoDto.getTunnelName();
//            String subnet = serviceInfoDto.getSubnet();
//            String srcTpId = serviceInfoDto.getSourceTpId();
//            String destTpId = serviceInfoDto.getDestinationTpId();
//            TerminationPoint sourceTp = terminationPointMap.get(srcTpId);
//            TerminationPoint destTp = terminationPointMap.get(destTpId);
//            Lldp srcLLDP = sourceTp == null ? null : getTpLLdp(sourceTp);
//            Lldp destLLDP = destTp == null ? null : getTpLLdp(destTp);
//            LLDPInfo lldpInfo = buildLlDpInfo(subnet, tunnelName, tunnelId, srcTpId, destTpId,
//                    srcLLDP,
//                    destLLDP);
//            lldpInfos.add(lldpInfo);
//        }
//        return lldpInfos;
//    }

//    private LLDPInfo buildLlDpInfo(String subnet, String tunnelName, String tunnelId,
//            String srcTpId,
//            String destTp, Lldp srcLLDP, Lldp destLLDP) {
//        TerminationPointCache sourceTpCache = topologyCacheManager.getValue(srcTpId,
//                TerminationPointCache.class);
//        TerminationPointCache destTpCache = topologyCacheManager.getValue(destTp,
//                TerminationPointCache.class);
//        String sourceNe = sourceTpCache.getFriendlyName().split("#")[0];
//        String destNe = destTpCache.getFriendlyName().split("#")[0];
//
//        LLDPInfoBuilder<?, ?> lldpInfoBd = LLDPInfo.builder()
//                .tunnelName(tunnelName)
//                .subnet(subnet)
//                .tunnelId(tunnelId)
//                .sourceNe(sourceNe)
//                .sourcePort(sourceTpCache.getSimpleName())
//                .destinationNe(destNe)
//                .destinationPort(destTpCache.getSimpleName());
//        if (srcLLDP != null && !CollectionUtils.isEmpty(srcLLDP.getNeighbor())) {
//            Neighbor srcNeighbor = srcLLDP.getNeighbor().get(0);
//            lldpInfoBd.aTransmissionRemoteChassis(srcNeighbor.getChassisId())
//                    .aTransmissionRemotePort(srcNeighbor.getPortId())
//                    .aTransmissionManagerAddress(srcNeighbor.getManagementAddress())
//                    .aTransmissionSystemName(srcNeighbor.getSystemName());
//        }
//        if (destLLDP != null && !CollectionUtils.isEmpty(destLLDP.getNeighbor())) {
//            Neighbor destNeighbor = destLLDP.getNeighbor().get(0);
//            lldpInfoBd.zTransmissionRemoteChassis(destNeighbor.getChassisId())
//                    .zTransmissionRemotePort(destNeighbor.getPortId())
//                    .zTransmissionManagerAddress(destNeighbor.getManagementAddress())
//                    .zTransmissionSystemName(destNeighbor.getSystemName());
//        }
//        return lldpInfoBd.build();
//    }


    @Override
    public InventoryExportData<LLDPCsv> exportAll() {
        return null;
    }


    @Override
    protected Class<LLDPCsv> getClazz() {
        return LLDPCsv.class;
    }
}

package net.flex.dci.otn.controller.resource.statistic.core.inventory;

import static net.flex.dci.otn.controller.resource.statistic.utils.CommonUtils.exportCsvFileName;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otn.controller.resource.statistic.core.enums.InventoryType;
import net.flex.dci.otn.controller.resource.statistic.core.manager.TunnelInventoryManager;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.InventoryExportData;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.UnifiedExportRequest;
import net.flex.dci.otn.controller.resource.statistic.export.TunnelCsv;
import net.flex.dci.otn.controller.resource.statistic.rest.TunnelDetail;
import net.flex.dci.otn.controller.resource.statistic.utils.ResourceConvertor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 11/5/2025 4:24 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TunnelInventory extends AbstractInventory<TunnelCsv> {

    private final TunnelDao tunnelDao;

    private final PhyLinkDao phyLinkDao;

    private final OchLinkDao ochLinkDao;

    private final SiteLinkDao siteLinkDao;

    private final TunnelInventoryManager inventoryManager;


    @Override
    public InventoryType inventoryScope() {
        return InventoryType.TUNNEL;
    }

    @Override
    public InventoryExportData<TunnelCsv> exportData(UnifiedExportRequest unifiedExportRequest) {
        log.debug("export phy link inventory detail,the request is :{}", unifiedExportRequest);
        List<TunnelCsv> tunnelCsvs = getExportRequestData(unifiedExportRequest);
        String filename = exportCsvFileName(unifiedExportRequest);
        return InventoryExportData.<TunnelCsv>builder()
                .exportDatas(tunnelCsvs)
                .clazz(getClazz())
                .filename(filename)
                .sheetName(inventoryScope().name())
                .build();
    }

    private List<TunnelCsv> getExportRequestData(UnifiedExportRequest unifiedExportRequest) {
        log.debug("export tunnel data scope is :{}", unifiedExportRequest.getScope());
        List<String> subnetIds =
                CollectionUtils.isEmpty(unifiedExportRequest.getSubnet()) ? new ArrayList<>()
                        : unifiedExportRequest.getSubnet();
        List<String> neIds =
                CollectionUtils.isEmpty(unifiedExportRequest.getNeIds()) ? new ArrayList<>()
                        : unifiedExportRequest.getNeIds();
        List<String> siteIds =
                CollectionUtils.isEmpty(unifiedExportRequest.getSiteIds()) ? new ArrayList<>()
                        : unifiedExportRequest.getSiteIds();
        List<String> siteLinkIds =
                CollectionUtils.isEmpty(unifiedExportRequest.getSiteLinkIds()) ? new ArrayList<>()
                        : unifiedExportRequest.getSiteLinkIds();
        List<String> phyLinkIds =
                CollectionUtils.isEmpty(unifiedExportRequest.getPhyLinkIds()) ? new ArrayList<>()
                        : unifiedExportRequest.getPhyLinkIds();
        List<String> tunnelIds =
                CollectionUtils.isEmpty(unifiedExportRequest.getTunnelIds()) ? new ArrayList<>()
                        : unifiedExportRequest.getTunnelIds();
        if (CollectionUtils.isEmpty(neIds) && CollectionUtils.isEmpty(siteIds)
                && CollectionUtils.isEmpty(siteLinkIds) && CollectionUtils.isEmpty(tunnelIds)
                && CollectionUtils.isEmpty(subnetIds) && CollectionUtils.isEmpty(phyLinkIds)) {
            log.info("no filter condition specified, exporting all tunnel infos");
            return exportAllTunnel();
        }
        List<String> fetchTunnelIds = new ArrayList<>();
        fetchTunnelIds.addAll(tunnelIds);
        if (!neIds.isEmpty()) {
            List<String> neRefTunnelIds = getNeRefTunnelIds(neIds);
            fetchTunnelIds.addAll(neRefTunnelIds);
        }
        if (!subnetIds.isEmpty()) {
            List<String> subnetRefTunnelIds = getSubnetRefTunnelIds(subnetIds);
            fetchTunnelIds.addAll(subnetRefTunnelIds);
        }
        if (!phyLinkIds.isEmpty()) {
            List<String> phyLinkRefTunnelIds = getPhyLinkRefTunnelIds(phyLinkIds);
            fetchTunnelIds.addAll(phyLinkRefTunnelIds);
        }
        if (!siteLinkIds.isEmpty()) {
            List<String> siteLinkRefTunnelIds = getSiteLinkRefTunnelIds(siteLinkIds);
            fetchTunnelIds.addAll(siteLinkRefTunnelIds);
        }
        if (!siteIds.isEmpty()) {
            List<String> siteRefTunnelIds = getSiteRefTunnelIds(siteIds);
            fetchTunnelIds.addAll(siteRefTunnelIds);
        }
        fetchTunnelIds = fetchTunnelIds.stream().distinct().collect(Collectors.toList());
        return fetchTunnelsByIds(fetchTunnelIds);
    }

    private List<String> getSiteRefTunnelIds(List<String> siteIds) {
        log.debug("get site ref tunnelIds:{}", siteIds);
        List<String> refTunnelIds = tunnelDao.retrieveAllTunnelIdsBySiteIds(siteIds);
        return refTunnelIds;
    }

    private List<String> getSiteLinkRefTunnelIds(List<String> siteLinkIds) {
        log.debug("get site link ref tunnelIds:{}", siteLinkIds);
        List<String> refOchLinkIds = ochLinkDao.retrieveAllOchLinkBySupportingLinkIds(siteLinkIds);
        List<String> refTunnelIds = tunnelDao.retrieveAllTunnelIdsByOchLinkIds(refOchLinkIds);
        return refTunnelIds;
    }

    private List<String> getPhyLinkRefTunnelIds(List<String> phyLinkIds) {
        log.debug("get phyLink ref tunnel ids:{}", phyLinkIds);
        List<String> refSiteLinkIds = siteLinkDao.retrieveAllSiteLinkIdsBySupportingLinkIds(
                phyLinkIds);
        List<String> supportingLinkIds = Stream.concat(phyLinkIds.stream(), refSiteLinkIds.stream())
                .collect(
                        Collectors.toList());
        List<String> refOchLinkIds = ochLinkDao.retrieveAllOchLinkBySupportingLinkIds(
                supportingLinkIds);
        return tunnelDao.retrieveAllTunnelIdsByOchLinkIds(refOchLinkIds);
    }

    private List<String> getSubnetRefTunnelIds(List<String> subnetIds) {
        log.debug("get subnet tunnel ids by subnetIds:{}", subnetIds);
        List<String> tunnelIds = tunnelDao.retrieveAllTunnelIdsBySubnetIds(subnetIds);
        return tunnelIds;
    }

    private List<String> getNeRefTunnelIds(List<String> neIds) {
        log.debug("get neIds ref tunnelIds:{}", neIds);
        List<String> phyLinkIds = phyLinkDao.retrieveAllPhyLinkIdsByPhyNodeIds(neIds);
        List<String> refSiteLinkIds = siteLinkDao.retrieveAllSiteLinkIdsBySupportingLinkIds(
                phyLinkIds);
        List<String> supportingLinkIds = Stream.concat(phyLinkIds.stream(), refSiteLinkIds.stream())
                .collect(
                        Collectors.toList());
        List<String> refOchLinkIds = ochLinkDao.retrieveAllOchLinkBySupportingLinkIds(
                supportingLinkIds);
        return tunnelDao.retrieveAllTunnelIdsByOchLinkIds(refOchLinkIds);
    }

    private List<TunnelCsv> exportAllTunnel() {
        List<String> tunnelIds = tunnelDao.getAllTunnelIds();
        return fetchTunnelsByIds(tunnelIds);
    }

    private List<TunnelCsv> fetchTunnelsByIds(List<String> tunnelIds) {
        if (CollectionUtils.isEmpty(tunnelIds)) {
            return new ArrayList<>();
        }

        log.info("fetching {} tunnel details", tunnelIds.size());

        List<TunnelDetail> tunnelDetails = inventoryManager.getInventoryDetails(
                tunnelIds);

        List<TunnelCsv> tunnelCsvs = ResourceConvertor.convert2TunnelCsv(tunnelDetails);
        return tunnelCsvs;
    }

    @Override
    public InventoryExportData<TunnelCsv> exportAll() {
        return null;
    }


    @Override
    protected Class<TunnelCsv> getClazz() {
        return TunnelCsv.class;
    }
}

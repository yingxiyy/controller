package net.flex.dci.otn.controller.resource.statistic.core.inventory;

import static net.flex.dci.otn.controller.resource.statistic.utils.CommonUtils.exportCsvFileName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otn.controller.resource.statistic.core.enums.InventoryType;
import net.flex.dci.otn.controller.resource.statistic.core.manager.PhyLinkInventoryManager;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.InventoryExportData;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.UnifiedExportRequest;
import net.flex.dci.otn.controller.resource.statistic.export.PhyLinkCsv;
import net.flex.dci.otn.controller.resource.statistic.rest.PhyLinkDetail;
import net.flex.dci.otn.controller.resource.statistic.utils.ResourceConvertor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * 2026/4/12
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class PhyLinkInventory extends AbstractInventory<PhyLinkCsv> {

    private final PhyLinkDao phyLinkDao;

    private final PhyLinkInventoryManager phyLinkInventoryManager;

    private final SiteLinkDao siteLinkDao;

    private final TunnelDao tunnelDao;

    private final OchLinkDao ochLinkDao;

    @Override
    protected Class<PhyLinkCsv> getClazz() {
        return PhyLinkCsv.class;
    }

    @Override
    public InventoryType inventoryScope() {
        return InventoryType.PHY_LINK;
    }

    @Override
    public InventoryExportData<PhyLinkCsv> exportData(UnifiedExportRequest unifiedExportRequest) {
        log.debug("export phy link inventory detail,the request is :{}", unifiedExportRequest);
        List<PhyLinkCsv> phyLinkCsvs = getExportRequestData(unifiedExportRequest);
        String filename = exportCsvFileName(unifiedExportRequest);
        return InventoryExportData.<PhyLinkCsv>builder()
                .exportDatas(phyLinkCsvs)
                .clazz(getClazz())
                .filename(filename)
                .sheetName(inventoryScope().name())
                .build();
    }

    private List<PhyLinkCsv> getExportRequestData(UnifiedExportRequest unifiedExportRequest) {
        log.debug("export phy link data scope is :{}", unifiedExportRequest.getScope());
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
            log.info("no filter condition specified, exporting all phy link infos");
            return exportAllPhyLinks();
        }
        List<String> fetchPhyLinkIds = new ArrayList<>();
        fetchPhyLinkIds.addAll(phyLinkIds);
        if (!CollectionUtils.isEmpty(subnetIds)) {
            List<String> subnetRefPhyLinkIds = getPhyLinkIdBySubnetIds(subnetIds);
            fetchPhyLinkIds.addAll(subnetRefPhyLinkIds);
        }
        if (!CollectionUtils.isEmpty(neIds)) {
            List<String> neRefPhyLinkIds = getPhyLinkIdByNeIds(neIds);
            fetchPhyLinkIds.addAll(neRefPhyLinkIds);
        }
        if (!CollectionUtils.isEmpty(siteIds)) {
            List<String> siteRefPhyLinkIds = getPhyLinkIdBySiteIds(siteIds);
            fetchPhyLinkIds.addAll(siteRefPhyLinkIds);
        }
        if (!CollectionUtils.isEmpty(siteLinkIds)) {
            List<String> siteLinkRefPhyLinkIds = getPhyLinkIdBySiteLinkIds(siteLinkIds);
            fetchPhyLinkIds.addAll(siteLinkRefPhyLinkIds);
        }

        if (!CollectionUtils.isEmpty(tunnelIds)) {
            List<String> tunnelRefPhyLinkIds = getPhyLinIdByTunnelIds(tunnelIds);
            fetchPhyLinkIds.addAll(tunnelRefPhyLinkIds);
        }

        return fetchPhyLinksByIds(fetchPhyLinkIds);
    }

    /**
     * get physical link id by tunnel ids
     *
     * @param tunnelIds
     * @return
     */
    private List<String> getPhyLinIdByTunnelIds(List<String> tunnelIds) {
        log.debug("get the phyLinkId by tunnelIds:{}", tunnelIds);
        if (CollectionUtils.isEmpty(tunnelIds)) {
            return Collections.emptyList();
        }
        List<String> refOchLinkIds = tunnelDao.retrieveAllOchLinkIdsByTunnelIds(tunnelIds);
        List<String> refSupportingLinkIds = ochLinkDao.retrieveAllSupportingLinkByOchLinkIds(
                refOchLinkIds);
        List<String> phyLinkIds = refSupportingLinkIds.stream()
                .filter(linkId -> !SiteLinkIdNamingRule.isSiteLink(linkId)).collect(
                        Collectors.toList());
        List<String> siteLinkIds = refSupportingLinkIds.stream()
                .filter(SiteLinkIdNamingRule::isSiteLink).collect(
                        Collectors.toList());
        List<String> siteLinkRefPhyLinkIds = siteLinkDao.retrieveAllPhyLInkIdsBySiteLinkIds(
                siteLinkIds);
        return Stream.concat(phyLinkIds.stream(), siteLinkRefPhyLinkIds.stream())
                .collect(Collectors.toList());
    }

    /**
     * get physical link ids by site link ids
     *
     * @param siteLinkIds
     * @return
     */
    private List<String> getPhyLinkIdBySiteLinkIds(List<String> siteLinkIds) {
        log.debug("get phyLinkId by siteLinIds:{}", siteLinkIds);
        if (CollectionUtils.isEmpty(siteLinkIds)) {
            return Collections.emptyList();
        }
        List<String> phyLinkIds = siteLinkDao.retrieveAllPhyLInkIdsBySiteLinkIds(siteLinkIds);
        return phyLinkIds;
    }

    private List<String> getPhyLinkIdBySiteIds(List<String> siteIds) {
        log.debug("get phyLinkId by siteIds:{}", siteIds);
        if (CollectionUtils.isEmpty(siteIds)) {
            return Collections.emptyList();
        }
        List<String> phyLinkIds = phyLinkDao.retrieveAllPhyLinkIdBySiteIds(siteIds);
        return phyLinkIds;
    }

    private List<String> getPhyLinkIdByNeIds(List<String> neIds) {
        log.debug("get phyLinkId by neIds :{}", neIds);
        if (CollectionUtils.isEmpty(neIds)) {
            return Collections.emptyList();
        }
        List<String> phyLinkIds = phyLinkDao.retrieveAllPhyLinkIdsByPhyNodeIds(neIds);
        return phyLinkIds;
    }

    private List<String> getPhyLinkIdBySubnetIds(List<String> subnetIds) {
        log.debug("get phyLinkId by subnetId:{}", subnetIds);
        if (CollectionUtils.isEmpty(subnetIds)) {
            return Collections.emptyList();
        }
        List<String> phyLinkIds = phyLinkDao.retrieveAllPhyLinkIdBySubnetIds(subnetIds);
        return phyLinkIds;
    }

    private List<PhyLinkCsv> exportAllPhyLinks() {
        log.info("export all phy links");
        List<String> phyLinkIds = phyLinkDao.retrieveAllPhyLinkIds();
        return fetchPhyLinksByIds(phyLinkIds);
    }

    private List<PhyLinkCsv> fetchPhyLinksByIds(List<String> phyLinkIds) {
        if (CollectionUtils.isEmpty(phyLinkIds)) {
            return new ArrayList<>();
        }

        log.info("fetching {} phyLink details", phyLinkIds.size());

        List<PhyLinkDetail> phyLinkDetails = phyLinkInventoryManager.getInventoryDetails(
                phyLinkIds);

        List<PhyLinkCsv> phyLinkCsvs = ResourceConvertor.convert2PhyLinkCsv(phyLinkDetails);
        return phyLinkCsvs;
    }

    @Override
    public InventoryExportData<PhyLinkCsv> exportAll() {
        return null;
    }
}

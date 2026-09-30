package net.flex.dci.otn.controller.resource.statistic.core.inventory;

import static net.flex.dci.otn.controller.resource.statistic.utils.CommonUtils.exportCsvFileName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otn.controller.resource.statistic.core.enums.InventoryType;
import net.flex.dci.otn.controller.resource.statistic.core.manager.SiteLinkInventoryManager;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.InventoryExportData;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.UnifiedExportRequest;
import net.flex.dci.otn.controller.resource.statistic.export.SiteLinkCsv;
import net.flex.dci.otn.controller.resource.statistic.rest.SiteLinkDetail;
import net.flex.dci.otn.controller.resource.statistic.utils.ResourceConvertor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * 2026/4/11
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class SiteLinkInventory extends AbstractInventory<SiteLinkCsv> {

    private final SiteLinkInventoryManager siteLinkInventoryManager;

    private final SiteLinkDao siteLinkDao;

    private final TunnelDao tunnelDao;

    private final OchLinkDao ochLinkDao;

    private final PhyLinkDao phyLinkDao;

    @Override
    protected Class<SiteLinkCsv> getClazz() {
        return SiteLinkCsv.class;
    }

    @Override
    public InventoryType inventoryScope() {
        return InventoryType.SITE_LINK;
    }

    @Override
    public InventoryExportData<SiteLinkCsv> exportData(UnifiedExportRequest unifiedExportRequest) {
        log.info("export data the unified export request :{}", unifiedExportRequest);
        List<SiteLinkCsv> siteLinkCsvs = getExportRequestData(unifiedExportRequest);
        String filename = exportCsvFileName(unifiedExportRequest);
        return InventoryExportData.<SiteLinkCsv>builder().exportDatas(siteLinkCsvs)
                .filename(filename).sheetName(inventoryScope().name()).clazz(getClazz()).build();
    }

    private List<SiteLinkCsv> getExportRequestData(UnifiedExportRequest unifiedExportRequest) {
        log.debug("export ne device csv info the export scope is:{} ",
                unifiedExportRequest.getScope());
        List<String> subnetIds =
                CollectionUtils.isEmpty(unifiedExportRequest.getSubnet()) ? new ArrayList<>()
                        : unifiedExportRequest.getSubnet();
        List<String> neIds =
                CollectionUtils.isEmpty(unifiedExportRequest.getNeIds()) ? new ArrayList<>()
                        : unifiedExportRequest.getNeIds();
        List<String> phyLinkIds =
                CollectionUtils.isEmpty(unifiedExportRequest.getPhyLinkIds()) ? new ArrayList<>()
                        : unifiedExportRequest.getPhyLinkIds();
        List<String> siteIds =
                CollectionUtils.isEmpty(unifiedExportRequest.getSiteIds()) ? new ArrayList<>()
                        : unifiedExportRequest.getSiteIds();
        List<String> siteLinkIds =
                CollectionUtils.isEmpty(unifiedExportRequest.getSiteLinkIds()) ? new ArrayList<>()
                        : unifiedExportRequest.getSiteLinkIds();
        List<String> tunnelIds =
                CollectionUtils.isEmpty(unifiedExportRequest.getTunnelIds()) ? new ArrayList<>()
                        : unifiedExportRequest.getTunnelIds();

        if (CollectionUtils.isEmpty(neIds) && CollectionUtils.isEmpty(siteIds)
                && CollectionUtils.isEmpty(siteLinkIds) && CollectionUtils.isEmpty(tunnelIds)
                && CollectionUtils.isEmpty(subnetIds) && CollectionUtils.isEmpty(phyLinkIds)) {
            log.info("no filter condition specified, exporting all siteLinks");
            return exportAllSiteLinkInfos();
        }
        List<String> exportSiteLinkIds = new ArrayList<>();
        exportSiteLinkIds.addAll(siteLinkIds);

        List<String> subnetRefSiteLinkIds = getSubnetSiteLinkIds(subnetIds);
        List<String> neRefSiteLinkIds = getNeRefSiteLinkIds(neIds);
        List<String> phyLinkRefSiteLinkIds = getPhyLinkRefSiteLinkIds(phyLinkIds);
        List<String> siteRefSiteLinkIds = getSiteRefSiteLinkIds(siteIds);
        List<String> tunnelRefSiteLinkIds = getTunnelRefSiteLinkIds(tunnelIds);
        exportSiteLinkIds.addAll(neRefSiteLinkIds);
        exportSiteLinkIds.addAll(siteRefSiteLinkIds);
        exportSiteLinkIds.addAll(tunnelRefSiteLinkIds);
        exportSiteLinkIds.addAll(phyLinkRefSiteLinkIds);
        exportSiteLinkIds.addAll(subnetRefSiteLinkIds);
        exportSiteLinkIds = exportSiteLinkIds.stream().distinct().collect(Collectors.toList());
        log.info("merged neIds count: {} (direct: {}, site : {}, tunnel: {})",
                exportSiteLinkIds.size(), siteLinkIds.size(), siteIds.size(),
                tunnelRefSiteLinkIds.size());
        return fetchSiteLinksByIds(exportSiteLinkIds);
    }

    private List<String> getSubnetSiteLinkIds(List<String> subnetIds) {
        log.debug("get subnet ref site link id subnetIds:{}", subnetIds);
        List<String> siteLinkIds = siteLinkDao.retrieveAllSiteLinkIdsBySubnetIds(subnetIds);
        return siteLinkIds;
    }

    private List<String> getPhyLinkRefSiteLinkIds(List<String> phyLinkIds) {
        log.debug("get phy link ref site linkIds:{}", phyLinkIds);
        List<String> osLinkIds = phyLinkIds.stream()
                .filter(PhysicalLinkIdNamingRule::isOsLink).collect(
                        Collectors.toList());
        List<String> wssLinkIds = phyLinkIds.stream()
                .filter(PhysicalLinkIdNamingRule::isWssLink).collect(
                        Collectors.toList());
        List<String> siteLinkPhyIds = phyLinkIds.stream()
                .filter(linkId -> !PhysicalLinkIdNamingRule.isWssLink(linkId)
                        && !PhysicalLinkIdNamingRule.isOsLink(linkId)).collect(
                        Collectors.toList());
        List<String> siteLinkIds = siteLinkDao.retrieveAllSiteLinkIdsBySupportingLinkIds(
                siteLinkPhyIds);
        List<String> wssLinkRefSiteLinkIds = phyLinkDao.retrieveAllSupportingSiteLinkIdsByWssLinkIds(
                wssLinkIds);
        List<String> ochLinkIds = ochLinkDao.retrieveAllOchLinkBySupportingLinkIds(osLinkIds);
        List<String> supportingLinkIds = ochLinkDao.retrieveAllSupportingLinkByOchLinkIds(
                ochLinkIds);
        List<String> refSiteLinkIds = new ArrayList<>();
        refSiteLinkIds.addAll(wssLinkRefSiteLinkIds);
        refSiteLinkIds.addAll(
                supportingLinkIds.stream().filter(SiteLinkIdNamingRule::isSiteLink).collect(
                        Collectors.toList()));
        refSiteLinkIds.addAll(siteLinkIds);

        return refSiteLinkIds;
    }

    private List<String> getTunnelRefSiteLinkIds(List<String> tunnelIds) {
        log.debug("get tunnel ref site linkIds tunnelIds:{}", tunnelIds);
        if (CollectionUtils.isEmpty(tunnelIds)) {
            return Collections.emptyList();
        }
        List<String> ochLinkIds = tunnelDao.retrieveAllOchLinkIdsByTunnelIds(tunnelIds);
        List<String> ochLinkSupportingLinkIds = ochLinkDao.retrieveAllSupportingLinkByOchLinkIds(
                ochLinkIds);
        List<String> siteLinkIds = ochLinkSupportingLinkIds.stream()
                .filter(SiteLinkIdNamingRule::isSiteLink).collect(
                        Collectors.toList());
        return siteLinkIds;
    }

    private List<String> getSiteRefSiteLinkIds(List<String> siteIds) {
        log.debug("get site ref site link ids by siteIds:{}", siteIds);
        if (CollectionUtils.isEmpty(siteIds)) {
            return Collections.emptyList();
        }
        List<String> siteLinkIds = siteLinkDao.retrieveAllSiteLinkIdsBySiteIds(siteIds);
        return siteLinkIds;
    }

    private List<String> getNeRefSiteLinkIds(List<String> neIds) {
        log.debug("get ne ref site link ids:{}", neIds);
        List<String> phyLinkIds = phyLinkDao.retrieveAllPhyLinkIdsByPhyNodeIds(neIds);
        List<String> refSiteLinkIds = siteLinkDao.retrieveAllSiteLinkIdsBySupportingLinkIds(
                phyLinkIds);
        List<String> ochLinkIds = ochLinkDao.retrieveAllOchLinkBySupportingLinkIds(phyLinkIds);
        List<String> refOchLinkSupportingLinkIds = ochLinkDao.retrieveAllSupportingLinkByOchLinkIds(
                ochLinkIds);
        List<String> refOchLinkSupportingSiteLinkIds = refOchLinkSupportingLinkIds.stream().filter(
                SiteLinkIdNamingRule::isSiteLink).collect(
                Collectors.toList());
        List<String> siteLinkIds = Stream.concat(refSiteLinkIds.stream(),
                refOchLinkSupportingSiteLinkIds.stream()).collect(
                Collectors.toList());
        return siteLinkIds;
    }

    private List<SiteLinkCsv> fetchSiteLinksByIds(List<String> exportSiteLinkIds) {
        if (CollectionUtils.isEmpty(exportSiteLinkIds)) {
            return new ArrayList<>();
        }

        log.info("fetching {} site link details", exportSiteLinkIds.size());
        List<SiteLinkDetail> siteLinkDetails = siteLinkInventoryManager.getInventoryDetails(
                exportSiteLinkIds);

        List<SiteLinkCsv> siteLinkCsvs = ResourceConvertor.convert2SiteLinkCsv(siteLinkDetails);
        return siteLinkCsvs;
    }


    private List<SiteLinkCsv> exportAllSiteLinkInfos() {
        log.info("export all site link infos");
        List<String> allSiteLinkIds = siteLinkDao.retrieveAllSiteLinkIds();
        return fetchSiteLinksByIds(allSiteLinkIds);
    }

    @Override
    public InventoryExportData<SiteLinkCsv> exportAll() {
        return null;
    }
}

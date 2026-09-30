package net.flex.dci.otn.controller.resource.statistic.core.inventory;

import static net.flex.dci.otn.controller.resource.statistic.utils.CommonUtils.exportCsvFileName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otn.controller.resource.statistic.core.enums.InventoryType;
import net.flex.dci.otn.controller.resource.statistic.core.manager.NeInventoryManager;
import net.flex.dci.otn.controller.resource.statistic.dto.FilterCondition;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.InventoryExportData;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.UnifiedExportRequest;
import net.flex.dci.otn.controller.resource.statistic.export.NeDeviceCsv;
import net.flex.dci.otn.controller.resource.statistic.rest.NeDevice;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 11/5/2025 4:23 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NeInventory extends AbstractInventory<NeDeviceCsv> {

    private final NeInventoryManager neInventoryManager;

    private final TunnelDao tunnelDao;

    private final SiteLinkDao siteLinkDao;

    private final OchLinkDao ochLinkDao;

    private final SiteNodeDao siteNodeDao;

    private final PhyLinkDao phyLinkDao;


    private final PhyNodeDao phyNodeDao;


    @Override
    public InventoryType inventoryScope() {
        return InventoryType.NE;
    }

    @Override
    public InventoryExportData<NeDeviceCsv> exportData(UnifiedExportRequest unifiedExportRequest) {
        log.debug("export ne inventory data,scope:{}", unifiedExportRequest.getScope());
        List<NeDeviceCsv> neDeviceCsvs = getExportRequestData(unifiedExportRequest);
        String filename = exportCsvFileName(unifiedExportRequest);
        return InventoryExportData.<NeDeviceCsv>builder().exportDatas(neDeviceCsvs)
                .filename(filename).sheetName(inventoryScope().name()).clazz(getClazz()).build();
    }

    @Override
    public InventoryExportData<NeDeviceCsv> exportAll() {
        return null;
    }

    private List<NeDeviceCsv> getExportRequestData(UnifiedExportRequest unifiedExportRequest) {
        log.debug("export ne device csv info the export scope is:{} ",
                unifiedExportRequest.getScope());
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
        List<String> tunnelIds =
                CollectionUtils.isEmpty(unifiedExportRequest.getTunnelIds()) ? new ArrayList<>()
                        : unifiedExportRequest.getTunnelIds();
        List<FilterCondition> filters = unifiedExportRequest.getFilters();

        if (CollectionUtils.isEmpty(neIds) && CollectionUtils.isEmpty(siteIds)
                && CollectionUtils.isEmpty(siteLinkIds) && CollectionUtils.isEmpty(tunnelIds)
                && CollectionUtils.isEmpty(subnetIds)) {
            log.info("no filter condition specified, exporting all ne devices");
            return exportAllNeDevices(filters);
        }
        List<String> siteRefNeIds = getSiteRefNeIds(siteIds);
        List<String> siteLinkRefNeId = getSiteLinkRefTotalNeIds(siteLinkIds);
        List<String> tunnelRefNeIds = getTunnelRefNeIds(tunnelIds);
        List<String> subnetRefNeIds = getSubnetRefNeIds(subnetIds);

        List<String> refNeIds = new ArrayList<>();
        refNeIds.addAll(neIds);
        refNeIds.addAll(siteRefNeIds);
        refNeIds.addAll(siteLinkRefNeId);
        refNeIds.addAll(tunnelRefNeIds);
        refNeIds.addAll(subnetRefNeIds);

        refNeIds = refNeIds.stream().distinct().collect(Collectors.toList());

        log.info("merged neIds count: {} (direct: {}, siteLink: {}, tunnel: {})",
                refNeIds.size(), neIds.size(), siteLinkRefNeId.size(), tunnelRefNeIds.size());

        return fetchNeDevicesByIds(refNeIds, filters);
    }

    private List<String> getSubnetRefNeIds(List<String> subnetIds) {
        log.info("export all current ne devices,the subnet id is:{}", subnetIds);
        List<String> neIds = phyNodeDao.retrieveAllPhyNodeIdsBySubnetIds(subnetIds);
        return neIds;
    }

    private List<NeDeviceCsv> exportAllNeDevices(List<FilterCondition> filters) {
        log.info("export all current ne devices ");
        List<String> neIds = phyNodeDao.retrieveAllPhyNodeIds();

        return fetchNeDevicesByIds(neIds, filters);
    }

    private List<String> getSiteRefNeIds(List<String> siteIds) {
        log.debug("get site ref neIds siteIds:{} ", siteIds);
        if (CollectionUtils.isEmpty(siteIds)) {
            return new ArrayList<>();
        }
        List<String> neIds = siteNodeDao.retrieveAllPhyNodeIdsBySiteIds(siteIds);
        return neIds;
    }

    private List<String> getSiteLinkRefTotalNeIds(List<String> siteLinkIds) {
        log.debug("get site link ref total neIds:{}", siteLinkIds);
        if (CollectionUtils.isEmpty(siteLinkIds)) {
            return new ArrayList<>();
        }
        List<String> siteLinkNeIds = getSiteLinkRefNeIds(siteLinkIds);
        List<String> businessNeIds = getBusinessRefNeIds(siteLinkIds);
        return Stream.concat(
                        siteLinkNeIds.stream(),
                        businessNeIds.stream()
                )
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> getBusinessRefNeIds(List<String> siteLinkIds) {
        List<Link> ochLinks = ochLinkDao.getAllBusinessOchLinksUnderSiteLinkIds(siteLinkIds);
        List<String> supportingPhyLinkIds = ochLinks.stream().map(LinkAttributes::getSupportingLink)
                .flatMap(Collection::stream)
                .map(SupportingLink::getLinkRef)
                .map(Uri::getValue)
                .filter(linkId -> !SiteLinkIdNamingRule.isSiteLink(linkId))
                .collect(Collectors.toList());
        return getRefNeIdByPhyLink(supportingPhyLinkIds);
    }

    /**
     * 根据 SiteLink ID 列表获取关联的网元 ID
     */
    private List<String> getSiteLinkRefNeIds(List<String> siteLinkIds) {
        if (CollectionUtils.isEmpty(siteLinkIds)) {
            return new ArrayList<>();
        }
        List<String> refPhyLinkIds = siteLinkDao.retrieveAllPhyLInkIdsBySiteLinkIds(siteLinkIds);
        return getRefNeIdByPhyLink(refPhyLinkIds);
    }

    private List<String> getRefNeIdByPhyLink(List<String> refPhyLinkIds) {
        Set<String> refNodeIds = new HashSet<>();
        for (String phyLinkId : refPhyLinkIds) {
            List<String> phyNodeIds = PhysicalLinkIdNamingRule.extractPhyNodeIds(
                    phyLinkId);
            refNodeIds.addAll(phyNodeIds);
        }
        return new ArrayList<>(refNodeIds);
    }


    private List<String> getTunnelRefNeIds(List<String> tunnelIds) {
        if (CollectionUtils.isEmpty(tunnelIds)) {
            return new ArrayList<>();
        }
        List<String> supportOchLinkIds = tunnelDao.retrieveAllOchLinkIdsByTunnelIds(tunnelIds);
        List<String> supportingLinkIds = ochLinkDao.retrieveAllSupportingLinkByOchLinkIds(
                supportOchLinkIds);
        List<String> phyLinkIds = supportingLinkIds.stream()
                .filter(linkId -> !SiteLinkIdNamingRule.isSiteLink(linkId))
                .collect(Collectors.toList());

        return getRefNeIdByPhyLink(phyLinkIds);
    }


    private List<NeDeviceCsv> fetchNeDevicesByIds(List<String> neIds,
            List<FilterCondition> filters) {
        if (CollectionUtils.isEmpty(neIds)) {
            return new ArrayList<>();
        }

        log.info("fetching {} ne devices details", neIds.size());

        List<NeDevice> neDevices = neInventoryManager.getInventoryDetails(neIds);

        return neDevices.stream()
                .map(this::convertToNeDeviceCsv)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    /**
     * 将 NeDevice 转换为 NeDeviceCsv
     */
    private NeDeviceCsv convertToNeDeviceCsv(NeDevice neDevice) {
        if (neDevice == null) {
            return null;
        }

        try {

            NeDeviceCsv.NeDeviceCsvBuilder builder = NeDeviceCsv.builder()
                    .neId(neDevice.getNeId())
                    .neName(neDevice.getName())
                    .siteId(neDevice.getSiteId())
                    .siteName(neDevice.getSiteName())
                    .ipAddress(neDevice.getIpAddress())
                    .port(neDevice.getPort())
                    .neAccount(neDevice.getUserAccount())
                    .nePassword(neDevice.getPassword())
                    .vendor(neDevice.getVendor())
                    .vendorType(neDevice.getVendorType())
                    .deviceType(neDevice.getDeviceType())
                    .deviceSubType(neDevice.getNeSubType())
                    .swVersion(neDevice.getSwVersion())
                    .northApiVersion(neDevice.getNorthApiVersion())
                    .subnet(neDevice.getNetwork())
                    .createTime(neDevice.getCreateTime());

            builder.relativeServices(neDevice.getServiceConnect());

            builder.relativeSiteLink(neDevice.getRefSiteLink());

            return builder.build();
        } catch (Exception e) {
            log.error("failed to convert ne device to csv: {}", neDevice.getNeId(), e);
            return null;
        }
    }

    @Override
    protected Class<NeDeviceCsv> getClazz() {
        return NeDeviceCsv.class;
    }


    @Data
    @Builder
    private static class NeLinkInfo {

        private Map<String, List<String>> neRefTunnelMap;

        private Map<String, String> neRefSiteLinkMap;
    }
}

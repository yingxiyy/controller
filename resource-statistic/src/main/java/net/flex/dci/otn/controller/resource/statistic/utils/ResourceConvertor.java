package net.flex.dci.otn.controller.resource.statistic.utils;

import static net.flex.dci.otn.controller.resource.statistic.core.utils.ResourceStatisticUtil.formatTimestampWithZone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.resource.statistic.export.LLDPCsv;
import net.flex.dci.otn.controller.resource.statistic.export.PhyLinkCsv;
import net.flex.dci.otn.controller.resource.statistic.export.SiteLinkCsv;
import net.flex.dci.otn.controller.resource.statistic.export.TunnelCsv;
import net.flex.dci.otn.controller.resource.statistic.rest.PhyLinkDetail;
import net.flex.dci.otn.controller.resource.statistic.rest.SiteLinkDetail;
import net.flex.dci.otn.controller.resource.statistic.rest.TunnelDetail;
import net.flex.dci.otn.controller.resource.statistic.rest.lldp.LLDPInfo;

/**
 * SiteLink 转换工具类
 *
 * @author musa
 * @version 1.0
 * @date 2026/4/11
 **/
@Slf4j
public class ResourceConvertor {

    /**
     * 将 SiteLinkDetail 列表转换为 SiteLinkCsv 列表
     *
     * @param siteLinkDetails SiteLink 详情列表
     * @return SiteLinkCsv 列表
     */
    public static List<SiteLinkCsv> convert2SiteLinkCsv(List<SiteLinkDetail> siteLinkDetails) {
        if (siteLinkDetails == null || siteLinkDetails.isEmpty()) {
            log.debug("siteLinkDetails is empty, return empty list");
            return new ArrayList<>();
        }

        log.debug("converting {} siteLinkDetails to SiteLinkCsv", siteLinkDetails.size());

        return siteLinkDetails.stream()
                .map(ResourceConvertor::convertSingle)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 将单个 SiteLinkDetail 转换为 SiteLinkCsv
     *
     * @param detail SiteLink 详情
     * @return SiteLinkCsv
     */

    private static SiteLinkCsv convertSingle(SiteLinkDetail detail) {
        if (detail == null) {
            return null;
        }

        try {
            return SiteLinkCsv.builder()
                    // SiteLink 基本信息
                    .siteLinkId(detail.getSiteLinkId())
                    .siteLinkName(detail.getSiteLinkName())
                    .subnet(detail.getSubnet())

                    // A端（源端）信息
                    .sourceSiteId(detail.getSourceSiteId())
                    .sourceSiteName(detail.getSourceSiteName())
                    .sourceNeId(detail.getSourceNeId())
                    .sourceNeName(detail.getSourceNeName())
                    .sourceTpId(detail.getSourceTpId())
                    .sourceTpName(detail.getSourceTpName())

                    // Z端（目的端）信息
                    .destinationSiteId(detail.getDestSiteId())
                    .destinationSiteName(detail.getDestSiteName())
                    .destinationNeId(detail.getDestNeId())
                    .destinationNeName(detail.getDestNeName())
                    .destinationTpId(detail.getDestTpId())
                    .destinationTpName(detail.getDestTpName())

                    .msModel(detail.getMsModel())
                    .protectionType(detail.getProtectionType())
                    .bandwidth(detail.getBandwidth())
                    .demandSource(detail.getDemandSource())
                    .creationTime(detail.getCreationTime())
                    .activationTime(detail.getActivationTime())
                    .implementState(detail.getImplementState())

                    .build();
        } catch (Exception e) {
            log.error("failed to convert SiteLinkDetail to SiteLinkCsv: {}", detail.getSiteLinkId(),
                    e);
            return null;
        }
    }

    public static List<PhyLinkCsv> convert2PhyLinkCsv(List<PhyLinkDetail> phyLinkDetails) {
        if (phyLinkDetails == null || phyLinkDetails.isEmpty()) {
            log.debug("phyLinkDetails is empty, return empty list");
            return Collections.emptyList();
        }

        log.debug("converting {} phyLinkDetails to PhyLinkCsv", phyLinkDetails.size());

        return phyLinkDetails.stream()
                .map(ResourceConvertor::convertPhyLinkSingle)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 将单个 PhyLinkDetail 转换为 PhyLinkCsv
     *
     * @param detail PhyLink 详情
     * @return PhyLinkCsv
     */
    private static PhyLinkCsv convertPhyLinkSingle(PhyLinkDetail detail) {
        if (detail == null) {
            return null;
        }

        try {
            return PhyLinkCsv.builder()
                    .phyLinkId(detail.getPhyLinkId())
                    .phyLinkName(detail.getPhyLinkName())
                    .sourceSite(detail.getSourceSite())
                    .sourceNe(detail.getSourceNe())
                    .sourceTp(detail.getSourceTp())
                    .destinationSite(detail.getDestinationSite())
                    .destinationNe(detail.getDestinationNe())
                    .destinationTp(detail.getDestinationTp())
                    .subnet(detail.getSubnet())
                    .direction(detail.getDirection().name())
//                    .sourcePortFlow(detail.getSourcePortFlow().name())
//                    .destinationPortFlow(detail.getDestinationPortFlow().name())
                    .build();
        } catch (Exception e) {
            log.error("failed to convert PhyLinkDetail to PhyLinkCsv: {}", detail.getPhyLinkId(),
                    e);
            return null;
        }
    }

    public static List<TunnelCsv> convert2TunnelCsv(List<TunnelDetail> tunnelDetails) {
        if (tunnelDetails == null) {
            return null;
        }
        return tunnelDetails.stream()
                .map(detail -> TunnelCsv.builder()
                        .tunnelId(detail.getTunnelId())
                        .tunnelName(detail.getTunnelName())
                        .serviceType(detail.getServiceType())
                        .protectionType(detail.getProtectionType())
                        .subnet(detail.getSubnet())
                        .centreFrequency(detail.getCentreFrequency())
                        .sourceSiteName(detail.getSourceSiteName())
                        .sourceNeName(detail.getSourceNeName())
                        .sourceLineTpName(detail.getSourceLineTpName())
                        .sourceClientTpName(detail.getSourceClientTpName())
                        .destSiteName(detail.getDestSiteName())
                        .destNeName(detail.getDestNeName())
                        .destClientTpName(detail.getDestClientTpName())
                        .destLineTpName(detail.getSourceLineTpName())
                        .creationTime(detail.getCreationTime())
                        .activationTime(detail.getActivationTime())
                        .lineSideRate(detail.getLineSideRate())
                        .clientSideRate(detail.getClientSideRate())
                        .protectionLevel(detail.getProtectionLevel())
                        .protectionType(detail.getProtectionType())
                        .build())
                .collect(Collectors.toList());
    }

    public static List<LLDPCsv> convert2LLdpCsv(List<LLDPInfo> lldpInfos) {
        if (lldpInfos.isEmpty()) {
            return Collections.emptyList();
        }
        return lldpInfos.stream().map(ResourceConvertor::convert2LLdpCsvData)
                .collect(Collectors.toList());
    }

    private static LLDPCsv convert2LLdpCsvData(LLDPInfo lldpInfo) {

        return LLDPCsv.builder()
                .tunnelId(lldpInfo.getTunnelId())
                .tunnelName(lldpInfo.getTunnelName())
                .subnet(lldpInfo.getSubnet())
                .sourceNe(lldpInfo.getSourceNe())
                .sourceSite(lldpInfo.getSourceSite())
                .destinationNe(lldpInfo.getDestinationNe())
                .sourceClientPort(lldpInfo.getSourceClientPort())
                .sourceLinePort(lldpInfo.getSourceLinePort())
                .destinationSite(lldpInfo.getDestinationSite())
                .destinationClientPort(lldpInfo.getDestinationClientPort())
                .destinationLinePort(lldpInfo.getDestinationLinePort())
                .sourceNeighborEstablishTime(
                        formatTimestampWithZone(lldpInfo.getSourceNeighborEstablishTime()))
                .sourceTransmissionManagerAddress(lldpInfo.getATransmissionManagerAddress())
                .sourceTransmissionSystemName(lldpInfo.getATransmissionSystemName())
                .sourceTransmissionRemotePort(lldpInfo.getATransmissionRemotePort())
                .sourceTransmissionRemoteChassis(lldpInfo.getATransmissionRemoteChassis())
                .destinationTransmissionManagerAddress(lldpInfo.getZTransmissionManagerAddress())
                .destinationTransmissionSystemName(lldpInfo.getZTransmissionSystemName())
                .destinationTransmissionRemoteChassis(lldpInfo.getZTransmissionRemoteChassis())
                .destinationTransmissionRemotePort(lldpInfo.getZTransmissionRemotePort())
                .destinationNeighborEstablishTime(
                        formatTimestampWithZone(lldpInfo.getDestinationNeighborEstablishTime()))
                .build();
    }
}

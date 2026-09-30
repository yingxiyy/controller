package net.flex.dci.otn.controller.nms.nms.component.dimension;

import static net.flex.dci.otc.common.util.Constant.GLOBAL_ROOT_NODE_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otc.mongo.dao.SubNetTreeNodeDao;
import net.flex.dci.otc.mongo.dto.NeSubTypeInfo;
import net.flex.dci.otc.mongo.dto.SiteLinkSubnetInfoDto;
import net.flex.dci.otc.mongo.enums.NeSubType;
import net.flex.dci.otc.mongo.mdoel.subnet.SubNetTreeNode;
import net.flex.dci.otn.controller.nms.nms.dto.dimension.BoardCardDetails;
import net.flex.dci.otn.controller.nms.nms.dto.dimension.NeDetails;
import net.flex.dci.otn.controller.nms.nms.dto.dimension.SiteDetails;
import net.flex.dci.otn.controller.nms.nms.dto.dimension.SiteLinkDetails;
import net.flex.dci.otn.controller.nms.nms.dto.dimension.TerminalDetails;
import net.flex.dci.otn.controller.nms.nms.dto.dimension.TerminationPointDetails;
import net.flex.dci.otn.controller.nms.nms.dto.dimension.WssLinkDetails;
import net.flex.dci.otn.controller.nms.utils.SiteRoleBitCalcUtil;
import net.flex.dci.otn.topology.cache.manager.TopologyCacheManager;
import net.flex.dci.otn.topology.cache.model.EquipmentCache;
import net.flex.dci.otn.topology.cache.model.LinkNodeInfo;
import net.flex.dci.otn.topology.cache.model.PhyLinkCache;
import net.flex.dci.otn.topology.cache.model.PhyNodeCache;
import net.flex.dci.otn.topology.cache.model.SiteLinkCache;
import net.flex.dci.otn.topology.cache.model.TerminationPointCache;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteReachabilityInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSubnetDimensionalViewInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.card.info.Port;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.card.info.PortBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.dimensional.view.topology.ExternalLinks;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.dimensional.view.topology.ExternalLinksBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.dimensional.view.topology.InternalLinks;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.dimensional.view.topology.InternalLinksBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.dimensional.view.topology.Sites;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.dimensional.view.topology.SitesBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.dimensional.view.topology.external.links.ExternalLink;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.dimensional.view.topology.external.links.ExternalLinkBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.dimensional.view.topology.external.links.ExternalLinkKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.dimensional.view.topology.internal.links.InternalLink;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.dimensional.view.topology.internal.links.InternalLinkBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.dimensional.view.topology.internal.links.InternalLinkKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.dimensional.view.topology.sites.Site;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.dimensional.view.topology.sites.SiteBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.reachability.output.Reachability;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.subnet.dimensional.view.output.Topology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.subnet.dimensional.view.output.TopologyBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.ne.info.Card;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.ne.info.CardBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.site.info.Ne;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.site.info.NeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.SiteType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 2026/3/26
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class DimensionViewImpl implements DimensionView {

    private final SubNetTreeNodeDao subNetTreeNodeDao;

    private final SiteLinkDao siteLinkDao;

    private final SiteNodeDao siteNodeDao;

    private final PhyLinkDao phyLinkDao;

    private final PhyNodeDao phyNodeDao;

//    private final OchLinkDao ochLinkDao;

    private final SiteReachability siteReachability;

    private final TopologyCacheManager topologyCacheManager;


    @Override
    public Topology getSubnetDimensionView(
            GetSubnetDimensionalViewInput input) {
        log.info("get subnet dimension view,subnet :{}",
                input);
        String subnetId = input.getSubnetId();
        if (!StringUtils.hasText(subnetId)) {
            log.info("subnet is not set,query global view");
            subnetId = GLOBAL_ROOT_NODE_ID;
        }
        String finalSubnetId = subnetId;
        SubNetTreeNode subNetTreeNode = subNetTreeNodeDao.findBySubNetId(subnetId)
                .orElseThrow(() -> new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "current subnet node  " + finalSubnetId + " is not found"));
        log.info("current subnet is:{}", subNetTreeNode.getName());
        List<String> siteLinkIds = siteLinkDao.retrieveAllSiteLinkIdBySubnetId(subnetId);
        List<String> inDimensionSiteLinkIds = filterDimensionSiteLinkId(siteLinkIds);
        List<String> wssLinkIds = phyLinkDao.retrieveAllWssLinkIdBySupportingSiteLinks(
                inDimensionSiteLinkIds);
        List<SiteLinkDetails> siteLinkDetails = getSiteLinkDetails(inDimensionSiteLinkIds);
        List<WssLinkDetails> wssLinkDetails = getWssLinkDetails(wssLinkIds);
        List<NeSubTypeInfo> neSubTypeInfos = phyNodeDao.listAllNeSubTypeBySubnet(
                subnetId);
        Map<String, List<NeSubTypeInfo>> siteNeSubTypeMap = neSubTypeInfos
                .stream()
                .collect(Collectors.groupingBy(neSubTypeInfo -> PhysicalNodeIdNamingRule.getSiteId(
                        neSubTypeInfo.getNeId())));
        Topology topology = buildDimensionTopology(siteLinkDetails, wssLinkDetails,
                siteNeSubTypeMap);
        return topology;
    }


    @Override
    public Reachability getSiteReachability(GetSiteReachabilityInput input) {
        log.info("get site reachability ");
        String subnetId = input.getSubnetId();
        NodeId sourceSiteId = input.getSourceSiteId();
        NodeId destSiteId = input.getDestSiteId();

        validateSite(sourceSiteId, destSiteId);
        if (!StringUtils.hasText(subnetId)) {
            log.info("subnet is not set,query global view");
            subnetId = GLOBAL_ROOT_NODE_ID;
        }
        String finalSubnetId = subnetId;
        SubNetTreeNode subNetTreeNode = subNetTreeNodeDao.findBySubNetId(subnetId)
                .orElseThrow(() -> new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "current subnet node  " + finalSubnetId + " is not found"));
        log.info("get site source site:{} and destSite:{} subnet is:{}", sourceSiteId, destSiteId,
                subNetTreeNode.getName());
        Reachability reachability = siteReachability.getReachability(subnetId,
                sourceSiteId, destSiteId);
        return reachability;
    }

    private void validateSite(NodeId sourceSiteId, NodeId destSiteId) {
        if (sourceSiteId == null) {
            log.error("get site reachability base site should not be null");
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Get site reachability site id should not be null");
        }
        String srcSiteId = sourceSiteId.getValue();
        String desSiteId = destSiteId == null ? null : destSiteId.getValue();
        boolean existSrcSite = siteNodeDao.existSiteBySiteId(srcSiteId);
        if (!existSrcSite) {
            log.error("get site reachability source site is not existed");
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "get site reachability source site is not existed");
        }

        if (desSiteId != null) {
            boolean existDestSite = siteNodeDao.existSiteBySiteId(desSiteId);
            if (!existDestSite) {
                log.error("get site reachability destination site is not existed");
                throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                        "get site reachability destination site is not existed");

            }
        }

    }

    private List<String> filterDimensionSiteLinkId(List<String> siteLinkIds) {
        Set<String> siteIds = new HashSet<>();
        for (String siteLinkId : siteLinkIds) {
            String sourceSite = SiteLinkIdNamingRule.getSiteA(siteLinkId);
            String destSite = SiteLinkIdNamingRule.getSiteZ(siteLinkId);
            siteIds.add(sourceSite);
            siteIds.add(destSite);
        }
        List<String> dimensionLinkIds = siteNodeDao.retrieveAllDimensionLinkBySiteIds(siteIds);
        return siteLinkIds.stream().filter(dimensionLinkIds::contains).collect(
                Collectors.toList());
    }

    private Topology buildDimensionTopology(List<SiteLinkDetails> siteLinkDetails,
            List<WssLinkDetails> wssLinkDetails,
            Map<String, List<NeSubTypeInfo>> siteNeSubTypeMap) {
        log.info("start to build dimension topology");
        Set<String> twoDimensionSites = identifyTwoDimensionSites(siteLinkDetails, wssLinkDetails,
                siteNeSubTypeMap);
        log.info("identified {} two-dimension sites: {}", twoDimensionSites.size(),
                twoDimensionSites);

        Sites sites = buildSites(siteLinkDetails, wssLinkDetails, twoDimensionSites);

        ExternalLinks externalLinks = buildMergedExternalLinks(siteLinkDetails, twoDimensionSites);
        InternalLinks internalLinks = buildInternalLinks(wssLinkDetails);

        TopologyBuilder topologyBuilder = new TopologyBuilder();
        topologyBuilder.setSites(sites);
        topologyBuilder.setExternalLinks(externalLinks);
        topologyBuilder.setInternalLinks(internalLinks);
        return topologyBuilder.build();
    }

    /**
     * 识别 2 维度站点 条件：
     * 1. 站点内只有 2 个网元
     * 2. 这 2 个网元都不是 OTM 类型
     * 3. 站点内只有 1 条 internal link
     * 4. 站点有 2 条外部连接（一进一出）
     */
    private Set<String> identifyTwoDimensionSites(List<SiteLinkDetails> siteLinkDetails,
            List<WssLinkDetails> wssLinkDetails,
            Map<String, List<NeSubTypeInfo>> siteNeSubTypeMap) {
        Set<String> twoDimensionSites = new HashSet<>();
        Set<String> allSiteIds = getSiteIds(siteLinkDetails, wssLinkDetails);

        for (String siteId : allSiteIds) {
            if (isTwoDimensionSite(siteId, siteLinkDetails, wssLinkDetails, siteNeSubTypeMap)) {
                twoDimensionSites.add(siteId);
            }
        }

        return twoDimensionSites;
    }

    /**
     * 判断是否为 2 维度站点
     *
     * 支持两种类型：
     * 1. 光中继（ILA/DGE）：2个网元 + 1条WssLink + 2条SiteLink
     * 2. 电中继（REG）：2个网元 + 0条WssLink + 2条SiteLink
     */
    private boolean isTwoDimensionSite(String siteId, List<SiteLinkDetails> siteLinkDetails,
            List<WssLinkDetails> wssLinkDetails,
            Map<String, List<NeSubTypeInfo>> siteNeSubTypeMap) {

        List<NeSubTypeInfo> neInfos = siteNeSubTypeMap.get(siteId);
        if (neInfos == null || neInfos.size() != 2) {
            return false;
        }

        for (NeSubTypeInfo neInfo : neInfos) {
            if (neInfo.getNeSubType().equals(NeSubType.OPC_OTM)) {
                return false;
            }
        }

        long externalLinkCount = siteLinkDetails.stream()
                .filter(link -> siteId.equals(link.getSourceSiteId()) || siteId.equals(
                        link.getDestSiteId()))
                .count();
        if (externalLinkCount != 2) {
            return false;
        }

        long internalLinkCount = wssLinkDetails.stream()
                .filter(link -> siteId.equals(link.getSourceSiteId()) || siteId.equals(
                        link.getDestSiteId()))
                .count();

        return internalLinkCount <= 1;
    }

    /**
     * 获取所有站点 ID
     */
    private Set<String> getSiteIds(List<SiteLinkDetails> siteLinkDetails,
            List<WssLinkDetails> wssLinkDetails) {
        Set<String> siteIds = new HashSet<>();
        siteLinkDetails.forEach(link -> {
            siteIds.add(link.getSourceSiteId());
            siteIds.add(link.getDestSiteId());
        });
        wssLinkDetails.forEach(link -> {
            siteIds.add(link.getSourceSiteId());
            siteIds.add(link.getDestSiteId());
        });
        return siteIds;
    }

    private Sites buildSites(List<SiteLinkDetails> siteLinkDetails,
            List<WssLinkDetails> wssLinkDetails, Set<String> twoDimensionSites) {
        log.debug("build sites for sites: siteLink size:{} wssLink size:{}", siteLinkDetails.size(),
                wssLinkDetails.size());
        SitesBuilder sitesBuilder = new SitesBuilder();

        Set<String> neIds = new HashSet<>();
        Set<String> tpIds = new HashSet<>();
        for (SiteLinkDetails link : siteLinkDetails) {
            neIds.add(link.getSourceNeId());
            neIds.add(link.getDestNeId());
            tpIds.add(link.getSourceTpId());
            tpIds.add(link.getDestTpId());
        }
        for (WssLinkDetails link : wssLinkDetails) {
            neIds.add(link.getSourceNeId());
            neIds.add(link.getDestNeId());
            tpIds.add(link.getSourceTpId());
            tpIds.add(link.getDestTpId());
        }
        Set<String> equipmentIds = tpIds.stream()
                .map(PhysicalTpIdNamingRule::getEquipId)
                .collect(Collectors.toSet());

        Map<String, PhyNodeCache> neCacheMap = topologyCacheManager.batchGetValues(
                new ArrayList<>(neIds), PhyNodeCache.class);
        Map<String, EquipmentCache> equipmentCacheMap = topologyCacheManager.batchGetValues(
                new ArrayList<>(equipmentIds), EquipmentCache.class);
        Map<String, TerminationPointCache> tpCacheMap = topologyCacheManager.batchGetValues(
                new ArrayList<>(tpIds), TerminationPointCache.class);

        List<SiteDetails> siteDetails = buildSiteDetails(siteLinkDetails, wssLinkDetails,
                neCacheMap, equipmentCacheMap, tpCacheMap);

        List<SiteDetails> filteredSiteDetails = siteDetails.stream()
                .filter(site -> !twoDimensionSites.contains(site.getSiteId()))
                .collect(Collectors.toList());

        log.info("filtered {} two-dimension sites from sites list",
                siteDetails.size() - filteredSiteDetails.size());

        List<Site> siteList = convert2SiteList(filteredSiteDetails);
        sitesBuilder.setSite(siteList);
        return sitesBuilder.build();
    }

    private List<SiteDetails> buildSiteDetails(List<SiteLinkDetails> siteLinkDetails,
            List<WssLinkDetails> wssLinkDetails,
            Map<String, PhyNodeCache> neCacheMap,
            Map<String, EquipmentCache> equipmentCacheMap,
            Map<String, TerminationPointCache> tpCacheMap) {
        log.debug("build site details from siteLink size:{} wssLink size:{}",
                siteLinkDetails.size(), wssLinkDetails.size());
        List<TerminalDetails> siteLinkTerminalDetails = siteLinkDetails.stream()
                .flatMap(detail -> {
                    TerminalDetails sourceTerminal = TerminalDetails.builder()
                            .siteId(detail.getSourceSiteId()).siteName(detail.getSourceSiteName())
                            .neName(detail.getSourceNeName())
                            .neId(detail.getSourceNeId())
                            .equipmentId(PhysicalTpIdNamingRule.getEquipId(detail.getSourceTpId()))
                            .portId(detail.getSourceTpId())
                            .portName(detail.getSourceTpName())
                            .build();
                    TerminalDetails destTerminal = TerminalDetails.builder()
                            .siteId(detail.getDestSiteId())
                            .siteName(detail.getDestSiteName())
                            .neId(detail.getDestNeId())
                            .neName(detail.getDestNeName())
                            .equipmentId(PhysicalTpIdNamingRule.getEquipId(detail.getDestTpId()))
                            .portId(detail.getDestTpId())
                            .portName(detail.getDestTpName())
                            .build();
                    List<TerminalDetails> terminalDetailsList = Arrays.asList(sourceTerminal,
                            destTerminal);
                    return terminalDetailsList.stream();
                })
                .collect(Collectors.toList());
        List<TerminalDetails> wssLinkTerminalDetails = wssLinkDetails.stream()
                .flatMap(detail -> {
                    TerminalDetails sourceTerminal = TerminalDetails.builder()
                            .siteId(detail.getSourceSiteId())
                            .neName(detail.getSourceNeName())
                            .neId(detail.getSourceNeId())
                            .portId(detail.getSourceTpId())
                            .equipmentId(PhysicalTpIdNamingRule.getEquipId(detail.getSourceTpId()))
                            .portName(detail.getSourceTpName())
                            .build();
                    TerminalDetails destTerminal = TerminalDetails.builder()
                            .siteId(detail.getDestSiteId())
                            .neId(detail.getDestNeId())
                            .neName(detail.getDestNeName())
                            .portId(detail.getDestTpId())
                            .equipmentId(PhysicalTpIdNamingRule.getEquipId(detail.getDestTpId()))
                            .portName(detail.getDestTpName())
                            .build();
                    List<TerminalDetails> terminalDetailsList = Arrays.asList(sourceTerminal,
                            destTerminal);
                    return terminalDetailsList.stream();
                }).collect(
                        Collectors.toList());
        List<TerminalDetails> terminalDetailsList = new ArrayList<>();
        terminalDetailsList.addAll(siteLinkTerminalDetails);
        terminalDetailsList.addAll(wssLinkTerminalDetails);

        Map<String, List<TerminalDetails>> terminalDetailsMap = terminalDetailsList.stream()
                .collect(Collectors.groupingBy(
                        TerminalDetails::getSiteId));
        List<SiteDetails> siteDetails = buildSiteDetails(terminalDetailsMap, neCacheMap,
                equipmentCacheMap, tpCacheMap);
        return siteDetails;
    }

    /**
     * build site details
     *
     * @param terminalDetailsMap
     * @return
     */
    private List<SiteDetails> buildSiteDetails(
            Map<String, List<TerminalDetails>> terminalDetailsMap,
            Map<String, PhyNodeCache> neCacheMap,
            Map<String, EquipmentCache> equipmentCacheMap,
            Map<String, TerminationPointCache> tpCacheMap) {
        log.debug("build site details from terminalDetailsMap size:{}", terminalDetailsMap.size());

        List<SiteDetails> siteDetailsList = new ArrayList<>();

        for (Map.Entry<String, List<TerminalDetails>> entry : terminalDetailsMap.entrySet()) {
            String siteId = entry.getKey();
            List<TerminalDetails> terminalDetails = entry.getValue();
            String siteName = terminalDetails.isEmpty() ? "" : terminalDetails.get(0).getSiteName();
            List<NeDetails> neDetailsList = buildNeDetailsForSite(terminalDetails, neCacheMap,
                    equipmentCacheMap, tpCacheMap);

            SiteDetails siteDetails = SiteDetails.builder()
                    .siteId(siteId)
                    .siteName(siteName)
                    .nes(neDetailsList)
                    .build();

            siteDetailsList.add(siteDetails);
        }

        return siteDetailsList;
    }


    private List<NeDetails> buildNeDetailsForSite(List<TerminalDetails> terminalDetails,
            Map<String, PhyNodeCache> neCacheMap, Map<String, EquipmentCache> equipmentCacheMap,
            Map<String, TerminationPointCache> tpCacheMap) {
        log.debug("build ne details the terminal size:{}", terminalDetails.size());
        List<NeDetails> neDetails = new ArrayList<>();
        Map<String, List<TerminalDetails>> neTerminalMap = terminalDetails.stream().collect(
                Collectors.groupingBy(TerminalDetails::getNeId));
        for (Map.Entry<String, List<TerminalDetails>> entry : neTerminalMap.entrySet()) {
            String neId = entry.getKey();
            List<TerminalDetails> terminalDetailsList = entry.getValue();
            PhyNodeCache phyNodeCache = neCacheMap.get(neId);
            String neName = phyNodeCache.getFriendlyName();
            String neSubType = phyNodeCache.getNeSubType();
            List<BoardCardDetails> cardDetails = buildBoardCardDetailsForNe(terminalDetailsList,
                    equipmentCacheMap, tpCacheMap);

            NeDetails neDetail = NeDetails.builder()
                    .neId(neId)
                    .neName(neName)
                    .neSubType(neSubType)
                    .boardCardDetails(cardDetails)
                    .build();
            neDetails.add(neDetail);
        }

        return neDetails;
    }

    private List<BoardCardDetails> buildBoardCardDetailsForNe(
            List<TerminalDetails> terminalDetailsList,
            Map<String, EquipmentCache> equipmentCacheMap,
            Map<String, TerminationPointCache> tpCacheMap) {
        log.debug("build board card detail the boardCard is:{}", terminalDetailsList.size());
        List<BoardCardDetails> boardCardDetails = new ArrayList<>();
        Map<String, List<TerminalDetails>> cardTerminalMap = terminalDetailsList.stream().collect(
                Collectors.groupingBy(TerminalDetails::getEquipmentId));
        for (Map.Entry<String, List<TerminalDetails>> entry : cardTerminalMap.entrySet()) {
            String cardId = entry.getKey();
            EquipmentCache equipmentCache = equipmentCacheMap.get(cardId);
            List<TerminalDetails> cardTerminalDetails = entry.getValue();
            List<TerminationPointDetails> tpDetails = buildTpDetailsForCard(cardTerminalDetails,
                    tpCacheMap);
            BoardCardDetails boardCardDetail = BoardCardDetails.builder()
                    .cardId(cardId)
                    .cardName(equipmentCache.getFriendlyName())
                    .ports(tpDetails)
                    .build();
            boardCardDetails.add(boardCardDetail);
        }
        return boardCardDetails;
    }

    private List<TerminationPointDetails> buildTpDetailsForCard(
            List<TerminalDetails> terminalDetailsList,
            Map<String, TerminationPointCache> tpCacheMap) {
        List<TerminationPointDetails> terminationPointDetails = terminalDetailsList.stream()
                .map(terminal -> {
                    String cardId = terminal.getEquipmentId();
                    TerminationPointCache terminationPointCache = tpCacheMap.get(
                            terminal.getPortId());
                    return TerminationPointDetails.builder()
                            .cardId(cardId)
                            .tpId(terminationPointCache.getId())
                            .tpName(terminationPointCache.getSimpleName())
                            .portType(PortType.valueOf(terminationPointCache.getPortType()))
                            .build();
                })
                .collect(Collectors.toList());
        return terminationPointDetails;
    }


    private List<Site> convert2SiteList(List<SiteDetails> siteDetails) {
        log.debug("convert to site list size:{}", siteDetails.size());
        List<Site> siteList = siteDetails.stream().map(detail -> {
            String siteId = detail.getSiteId();
            String siteName = detail.getSiteName();
            List<NeDetails> neDetails = detail.getNes();
            List<Ne> nes = convertDetails2Nes(neDetails);
            SiteBuilder siteBuilder = new SiteBuilder();
            SiteType siteType = calculateSiteType(nes);
            siteBuilder.setSiteId(NodeId.getDefaultInstance(siteId));
            siteBuilder.setSiteName(siteName);
            siteBuilder.setSiteType(siteType);
            siteBuilder.setNe(nes);
            return siteBuilder.build();
        }).collect(Collectors.toList());
        return siteList;
    }

    private SiteType calculateSiteType(List<Ne> nes) {
        log.debug("calculate site type nes size:{}", nes.size());
        SiteType siteType = SiteRoleBitCalcUtil.calcByNeSubType(
                nes.stream().map(neInfo -> NeSubType.fromCode(neInfo.getNeSubtype()))
                        .collect(Collectors.toList()));
        return siteType;
    }

    private List<Ne> convertDetails2Nes(List<NeDetails> neDetails) {
        List<Ne> nes = neDetails.stream().map(this::convertDetail2Ne).collect(Collectors.toList());
        return nes;
    }

    private Ne convertDetail2Ne(NeDetails neDetails) {
        NeBuilder neBuilder = new NeBuilder();
        neBuilder.setNeId(NodeId.getDefaultInstance(neDetails.getNeId()));
        neBuilder.setNeName(neDetails.getNeName());
        neBuilder.setNeSubtype(neDetails.getNeSubType());
        List<Card> cards = convert2Cards(neDetails.getBoardCardDetails());
        neBuilder.setCard(cards);
        return neBuilder.build();
    }

    private List<Card> convert2Cards(List<BoardCardDetails> boardCardDetails) {
        List<Card> cards = boardCardDetails.stream().map(cardDetail -> {
            CardBuilder cardBuilder = new CardBuilder();
            cardBuilder.setCardId(cardDetail.getCardId());
            cardBuilder.setCardName(cardDetail.getCardName());
            cardBuilder.setNeId(cardBuilder.getNeId());
            List<Port> ports = convert2Ports(cardDetail.getPorts());
            cardBuilder.setPort(ports);
            return cardBuilder.build();
        }).collect(Collectors.toList());
        return cards;

    }

    private List<Port> convert2Ports(List<TerminationPointDetails> ports) {
        List<Port> portList = ports.stream().map(
                terminationPointDetails -> {
                    PortBuilder portBuilder = new PortBuilder();
                    portBuilder.setPortId(terminationPointDetails.getTpId());
                    portBuilder.setPortName(terminationPointDetails.getTpName());
                    portBuilder.setPortType(terminationPointDetails.getPortType());
                    return portBuilder.build();
                }
        ).collect(Collectors.toList());
        return portList;
    }


    private ExternalLinks buildExternalLinks(List<SiteLinkDetails> siteLinkDetails) {
        log.debug("build external links, size: {}", siteLinkDetails.size());
        ExternalLinksBuilder externalLinksBuilder = new ExternalLinksBuilder();
        List<ExternalLink> externalLinkList = new ArrayList<>();

        for (SiteLinkDetails details : siteLinkDetails) {
            ExternalLinkBuilder linkBuilder = new ExternalLinkBuilder();
            linkBuilder.setLinkId(LinkId.getDefaultInstance(details.getSiteLinkId()));
            linkBuilder.setKey(new ExternalLinkKey(
                    LinkId.getDefaultInstance(details.getSiteLinkId())));
            linkBuilder.setLinkName(details.getFriendlyName());

            linkBuilder.setSourceSiteId(new NodeId(details.getSourceSiteId()));
            linkBuilder.setSourceSiteName(details.getSourceSiteName());

            linkBuilder.setDestinationSiteId(new NodeId(details.getDestSiteId()));
            linkBuilder.setDestinationSiteName(details.getDestSiteName());

            linkBuilder.setSourceNeId(new NodeId(details.getSourceNeId()));
            linkBuilder.setSourceNeName(details.getSourceNeName());

            linkBuilder.setDestinationNeId(new NodeId(details.getDestNeId()));
            linkBuilder.setDestinationNeName(details.getDestNeName());

            linkBuilder.setSubnetId(details.getSubnetId());
            linkBuilder.setSubnetName(details.getSubnetName());

            linkBuilder.setSourcePortId(TpId.getDefaultInstance(details.getSourceTpId()));
            linkBuilder.setSourcePortName(details.getSourceTpName());
            linkBuilder.setDestinationPortId(TpId.getDefaultInstance(details.getDestTpId()));
            linkBuilder.setDestinationPortName(details.getDestTpName());

            externalLinkList.add(linkBuilder.build());
        }

        externalLinksBuilder.setExternalLink(externalLinkList);
        return externalLinksBuilder.build();
    }

    /**
     * 构建合并后的外部连接 将经过 2 维度站点的连接合并为一条逻辑路径
     */
    private ExternalLinks buildMergedExternalLinks(List<SiteLinkDetails> siteLinkDetails,
            Set<String> twoDimensionSites) {
        log.debug("build merged external links, original size: {}, two-dimension sites: {}",
                siteLinkDetails.size(), twoDimensionSites.size());

        if (twoDimensionSites.isEmpty()) {
            return buildExternalLinks(siteLinkDetails);
        }

        Map<String, List<SiteLinkDetails>> siteToLinksMap = new HashMap<>();
        for (SiteLinkDetails link : siteLinkDetails) {
            siteToLinksMap.computeIfAbsent(link.getSourceSiteId(), k -> new ArrayList<>())
                    .add(link);
            siteToLinksMap.computeIfAbsent(link.getDestSiteId(), k -> new ArrayList<>()).add(link);
        }

        List<ExternalLink> mergedLinks = new ArrayList<>();
        Set<String> processedLinks = new HashSet<>();

        for (SiteLinkDetails link : siteLinkDetails) {
            if (processedLinks.contains(link.getSiteLinkId())) {
                continue;
            }

            String sourceSite = link.getSourceSiteId();
            String destSite = link.getDestSiteId();

            if (twoDimensionSites.contains(sourceSite)) {
                continue;
            }

            if (twoDimensionSites.contains(destSite)) {
                List<SiteLinkDetails> path = new ArrayList<>();
                path.add(link);
                processedLinks.add(link.getSiteLinkId());

                String currentSite = destSite;
                while (twoDimensionSites.contains(currentSite)) {
                    SiteLinkDetails nextLink = findNextLink(currentSite, siteToLinksMap,
                            processedLinks);
                    if (nextLink == null) {
                        break;
                    }
                    path.add(nextLink);
                    processedLinks.add(nextLink.getSiteLinkId());

                    currentSite = nextLink.getDestSiteId().equals(currentSite)
                            ? nextLink.getSourceSiteId()
                            : nextLink.getDestSiteId();
                }

                ExternalLink mergedLink = createMergedLink(path);
                if (mergedLink != null) {
                    mergedLinks.add(mergedLink);
                }
            } else {
                ExternalLink externalLink = convertToExternalLink(link);
                mergedLinks.add(externalLink);
                processedLinks.add(link.getSiteLinkId());
            }
        }

        log.info("merged {} original links into {} logical links",
                siteLinkDetails.size(), mergedLinks.size());

        ExternalLinksBuilder externalLinksBuilder = new ExternalLinksBuilder();
        externalLinksBuilder.setExternalLink(mergedLinks);
        return externalLinksBuilder.build();
    }

    /**
     * 找到从指定站点出发的下一条未处理连接
     */
    private SiteLinkDetails findNextLink(String siteId,
            Map<String, List<SiteLinkDetails>> siteToLinksMap, Set<String> processedLinks) {
        List<SiteLinkDetails> links = siteToLinksMap.getOrDefault(siteId, Collections.emptyList());
        for (SiteLinkDetails link : links) {
            if (!processedLinks.contains(link.getSiteLinkId())) {
                return link;
            }
        }
        return null;
    }

    /**
     * 创建合并后的连接
     */
    private ExternalLink createMergedLink(List<SiteLinkDetails> path) {
        if (path.isEmpty()) {
            return null;
        }

        SiteLinkDetails firstLink = path.get(0);
        SiteLinkDetails lastLink = path.get(path.size() - 1);

        ExternalLinkBuilder linkBuilder = new ExternalLinkBuilder();

        String mergedLinkId = firstLink.getSiteLinkId() + "_to_" + lastLink.getSiteLinkId();
        linkBuilder.setLinkId(LinkId.getDefaultInstance(mergedLinkId));
        linkBuilder.setKey(new ExternalLinkKey(LinkId.getDefaultInstance(mergedLinkId)));
        linkBuilder.setLinkName(firstLink.getFriendlyName() + " -> " + lastLink.getFriendlyName());

        linkBuilder.setSourceSiteId(new NodeId(firstLink.getSourceSiteId()));
        linkBuilder.setSourceSiteName(firstLink.getSourceSiteName());
        linkBuilder.setDestinationSiteId(new NodeId(lastLink.getDestSiteId()));
        linkBuilder.setDestinationSiteName(lastLink.getDestSiteName());

        linkBuilder.setSourceNeId(new NodeId(firstLink.getSourceNeId()));
        linkBuilder.setSourceNeName(firstLink.getSourceNeName());
        linkBuilder.setDestinationNeId(new NodeId(lastLink.getDestNeId()));
        linkBuilder.setDestinationNeName(lastLink.getDestNeName());

        linkBuilder.setSubnetId(firstLink.getSubnetId());
        linkBuilder.setSubnetName(firstLink.getSubnetName());

        linkBuilder.setSourcePortId(TpId.getDefaultInstance(firstLink.getSourceTpId()));
        linkBuilder.setSourcePortName(firstLink.getSourceTpName());
        linkBuilder.setDestinationPortId(TpId.getDefaultInstance(lastLink.getDestTpId()));
        linkBuilder.setDestinationPortName(lastLink.getDestTpName());

        return linkBuilder.build();
    }

    /**
     * 将 SiteLinkDetails 转换为 ExternalLink
     */
    private ExternalLink convertToExternalLink(SiteLinkDetails details) {
        ExternalLinkBuilder linkBuilder = new ExternalLinkBuilder();
        linkBuilder.setLinkId(LinkId.getDefaultInstance(details.getSiteLinkId()));
        linkBuilder.setKey(new ExternalLinkKey(LinkId.getDefaultInstance(details.getSiteLinkId())));
        linkBuilder.setLinkName(details.getFriendlyName());

        linkBuilder.setSourceSiteId(new NodeId(details.getSourceSiteId()));
        linkBuilder.setSourceSiteName(details.getSourceSiteName());
        linkBuilder.setDestinationSiteId(new NodeId(details.getDestSiteId()));
        linkBuilder.setDestinationSiteName(details.getDestSiteName());

        linkBuilder.setSourceNeId(new NodeId(details.getSourceNeId()));
        linkBuilder.setSourceNeName(details.getSourceNeName());
        linkBuilder.setDestinationNeId(new NodeId(details.getDestNeId()));
        linkBuilder.setDestinationNeName(details.getDestNeName());

        linkBuilder.setSubnetId(details.getSubnetId());
        linkBuilder.setSubnetName(details.getSubnetName());

        linkBuilder.setSourcePortId(TpId.getDefaultInstance(details.getSourceTpId()));
        linkBuilder.setSourcePortName(details.getSourceTpName());
        linkBuilder.setDestinationPortId(TpId.getDefaultInstance(details.getDestTpId()));
        linkBuilder.setDestinationPortName(details.getDestTpName());

        return linkBuilder.build();
    }

    private InternalLinks buildInternalLinks(List<WssLinkDetails> wssLinkDetails) {
        log.debug("build internal links, size: {}", wssLinkDetails.size());
        InternalLinksBuilder internalLinksBuilder = new InternalLinksBuilder();
        List<InternalLink> internalLinkList = new ArrayList<>();

        for (WssLinkDetails details : wssLinkDetails) {
            InternalLinkBuilder linkBuilder = new InternalLinkBuilder();
            linkBuilder.setLinkId(LinkId.getDefaultInstance(details.getWssLinkId()));
            linkBuilder.setKey(new InternalLinkKey(
                    LinkId.getDefaultInstance(details.getWssLinkId())));
            linkBuilder.setLinkName(details.getWssName());

            linkBuilder.setSiteId(new NodeId(details.getSourceSiteId()));

            linkBuilder.setSourceNeId(new NodeId(details.getSourceNeId()));

            linkBuilder.setDestinationNeId(new NodeId(details.getDestNeId()));
            linkBuilder.setSourceNeName(details.getSourceNeName());
            linkBuilder.setDestinationNeName(details.getDestNeName());
            linkBuilder.setSourcePortId(TpId.getDefaultInstance(details.getSourceTpId()));
            linkBuilder.setSourcePortName(details.getSourceTpName());
            linkBuilder.setDestinationPortId(TpId.getDefaultInstance(details.getDestTpId()));
            linkBuilder.setDestinationPortName(details.getDestTpName());

            internalLinkList.add(linkBuilder.build());
        }

        internalLinksBuilder.setInternalLink(internalLinkList);
        return internalLinksBuilder.build();
    }


    private List<WssLinkDetails> getWssLinkDetails(List<String> wssLinkIds) {
        log.debug("get wss link details:{}", wssLinkIds);
        if (CollectionUtils.isEmpty(wssLinkIds)) {
            return Collections.emptyList();
        }
//        List<PhyLinkCache> wssLinkCaches = wssLinkIds.stream()
//                .map(wssLinkId -> topologyCacheManager.getValue(wssLinkId,
//                        PhyLinkCache.class)).collect(Collectors.toList());
        Map<String, PhyLinkCache> wssLinkCachesMap = topologyCacheManager.batchGetValues(wssLinkIds,
                PhyLinkCache.class);
        List<PhyLinkCache> wssLinkCaches = new ArrayList<>(wssLinkCachesMap.values());
        List<WssLinkDetails> wssLinkDetails = wssLinkCaches.stream()
                .map(phyLinkCache -> {
                    String phyLinkId = phyLinkCache.getId();
                    String friendlyName = phyLinkCache.getFriendlyName();
                    LinkNodeInfo sourceNodeInfo = phyLinkCache.getSource();
                    LinkNodeInfo destNodeInfo = phyLinkCache.getDestination();

                    WssLinkDetails linkDetails = WssLinkDetails.builder()
                            .wssLinkId(phyLinkId)
                            .wssName(friendlyName)
                            .sourceSiteId(sourceNodeInfo.getSiteId())
                            .sourceNeId(sourceNodeInfo.getNodeId())
                            .sourceNeName(sourceNodeInfo.getNodeName())
                            .sourceTpId(sourceNodeInfo.getPortId())
                            .sourceTpName(sourceNodeInfo.getPortName())
                            .destSiteId(destNodeInfo.getSiteId())
                            .destNeId(destNodeInfo.getNodeId())
                            .destNeName(destNodeInfo.getNodeName())
                            .destTpId(destNodeInfo.getPortId())
                            .destTpName(destNodeInfo.getPortName())
                            .build();
                    return linkDetails;

                }).collect(Collectors.toList());
        return wssLinkDetails;
    }

    private List<SiteLinkDetails> getSiteLinkDetails(List<String> siteLinkIds) {
        log.debug("get site link details :{}", siteLinkIds);
        if (CollectionUtils.isEmpty(siteLinkIds)) {
            return Collections.emptyList();
        }
        List<SiteLinkSubnetInfoDto> siteLinkSubnetInfoDtos = siteLinkDao.retrieveAllSiteLinkSubnetInfo(
                siteLinkIds);
        Map<String, SiteLinkSubnetInfoDto> siteLinkSubnetInfoDtoMap = siteLinkSubnetInfoDtos.stream()
                .collect(Collectors.toMap(SiteLinkSubnetInfoDto::getSiteLinkId, dto -> dto));
//        List<SiteLinkCache> siteLinkCaches = siteLinkIds.stream()
//                .map(siteLinkId -> topologyCacheManager.getValue(siteLinkId,
//                        SiteLinkCache.class)).collect(Collectors.toList());
        Map<String, SiteLinkCache> siteLinkCacheMap = topologyCacheManager.batchGetValues(
                siteLinkIds,
                SiteLinkCache.class);
        List<SiteLinkCache> siteLinkCaches = new ArrayList<>(siteLinkCacheMap.values());
        List<SiteLinkDetails> siteLinkDetails = siteLinkCaches.stream()
                .map(siteLinkCache -> {
                    String siteLinkId = siteLinkCache.getId();
                    String siteLinkName = siteLinkCache.getFriendlyName();
                    SiteLinkSubnetInfoDto siteLinkSubnetInfoDto = siteLinkSubnetInfoDtoMap.get(
                            siteLinkId);
                    LinkNodeInfo sourceInfo = siteLinkCache.getSource();
                    LinkNodeInfo destInfo = siteLinkCache.getDestination();

                    SiteLinkDetails linkDetails = SiteLinkDetails.builder()
                            .siteLinkId(siteLinkId)
                            .friendlyName(siteLinkName)
                            .subnetId(siteLinkSubnetInfoDto.getSubnetId())
                            .subnetName(siteLinkSubnetInfoDto.getSubnetName())
                            .sourceNeId(sourceInfo.getNodeId())
                            .sourceSiteId(sourceInfo.getSiteId())
                            .sourceSiteName(sourceInfo.getSiteName())
                            .destSiteId(destInfo.getSiteId())
                            .destSiteName(destInfo.getSiteName())
                            .sourceNeName(sourceInfo.getNodeName())
                            .destNeId(destInfo.getNodeId())
                            .destNeName(destInfo.getNodeName())
                            .sourceTpId(sourceInfo.getPortId())
                            .sourceTpName(sourceInfo.getPortName())
                            .destTpId(destInfo.getPortId())
                            .destTpName(destInfo.getPortName())
                            .build();
                    return linkDetails;
                })
                .collect(Collectors.toList());
        return siteLinkDetails;

    }
}

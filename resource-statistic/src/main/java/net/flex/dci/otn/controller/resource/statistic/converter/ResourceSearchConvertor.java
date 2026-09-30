package net.flex.dci.otn.controller.resource.statistic.converter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otc.mongo.dao.SubNetTreeNodeDao;
import net.flex.dci.otc.mongo.dto.NodeInfoDto;
import net.flex.dci.otc.mongo.dto.SiteInfo;
import net.flex.dci.otc.mongo.mdoel.subnet.SubNetTreeNode;
import net.flex.dci.otn.controller.resource.statistic.dto.FilterCondition;
import net.flex.dci.otn.controller.resource.statistic.dto.PreviewList;
import net.flex.dci.otn.controller.resource.statistic.dto.ResourceSearchParameter;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.UnifiedExportRequest;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import net.flex.dci.otn.topology.cache.model.BasicCache;
import net.flex.dci.otn.topology.cache.model.PhyLinkCache;
import net.flex.dci.otn.topology.cache.model.SiteLinkCache;
import net.flex.dci.otn.topology.cache.model.TunnelCache;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * 2026/6/29
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class ResourceSearchConvertor {

    private final PhyNodeDao phyNodeDao;

    private final SubNetTreeNodeDao subNetTreeNodeDao;

    private final SiteNodeDao siteNodeDao;

    private final DciTopologyCacheManager dciTopologyCacheManager;

    private static final int PREVIEW_SIZE = 5;

    public ResourceSearchParameter convertSearchParameter(UnifiedExportRequest exportRequest) {
        log.debug("convert search parameter the exportRequest:{}", exportRequest);
        List<String> subnetIds = exportRequest.getSubnet();
        List<String> siteNodeIds = exportRequest.getSiteIds();
        List<String> phyNodeIds = exportRequest.getNeIds();
        List<String> phyLinkIds = exportRequest.getPhyLinkIds();
        List<String> siteLinkIds = exportRequest.getSiteLinkIds();
        List<String> tunnelIds = exportRequest.getTunnelIds();

        List<String> previewPhyNodeIds = truncateList(phyNodeIds);
        List<String> previewSiteIds = truncateList(siteNodeIds);
        List<String> previewPhyLinkIds = truncateList(phyLinkIds);
        List<String> previewSiteLinkIds = truncateList(siteLinkIds);
        List<String> previewTunnelIds = truncateList(tunnelIds);

        List<String> subNet = getSearchSubnetDetail(subnetIds);
        List<String> phyNode = getSearchPhyNode(previewPhyNodeIds);
        List<String> siteNode = getSearchSiteNode(previewSiteIds);
        List<String> phyLink = getSearchPhyLink(previewPhyLinkIds);
        List<String> siteLink = getSearchSiteLink(previewSiteLinkIds);
        List<String> tunnel = getSearchTunnel(previewTunnelIds);

        List<FilterCondition> filters = exportRequest.getFilters();
        List<FilterCondition> previewFilters = truncateList(filters);

        ResourceSearchParameter resourceSearchParameter = ResourceSearchParameter.builder()
                .subnet(subNet)
                .ne(buildPreview(phyNodeIds, phyNode))
                .site(buildPreview(siteNodeIds, siteNode))
                .phyLink(buildPreview(phyLinkIds, phyLink))
                .site(buildPreview(siteLinkIds, siteLink))
                .siteLink(buildPreview(siteLinkIds, siteLink))
                .tunnel(buildPreview(tunnelIds, tunnel))
                .filters(buildPreview(filters, previewFilters))
                .build();
        return resourceSearchParameter;
    }

    private List<String> getSearchTunnel(List<String> tunnelIds) {
        log.debug("get search tunnel by tunnel ids:{}", tunnelIds);
        if (CollectionUtils.isEmpty(tunnelIds)) {
            return Collections.emptyList();
        }
        Map<String, TunnelCache> tunnelCacheMap = dciTopologyCacheManager.batchGetValues(tunnelIds,
                TunnelCache.class);
        List<String> tunnelNames = tunnelCacheMap.values().stream().map(BasicCache::getFriendlyName)
                .collect(Collectors.toList());
        return tunnelNames;
    }

    private List<String> getSearchSiteLink(List<String> siteLinkIds) {
        log.debug("get search site link by siteLink ids:{}", siteLinkIds);
        if (CollectionUtils.isEmpty(siteLinkIds)) {
            return Collections.emptyList();
        }
        Map<String, SiteLinkCache> siteLinkCacheMap = dciTopologyCacheManager.batchGetValues(
                siteLinkIds,
                SiteLinkCache.class);
        List<String> siteLinkNames = siteLinkCacheMap.values().stream()
                .map(BasicCache::getFriendlyName).collect(
                        Collectors.toList());
        return siteLinkNames;
    }

    private List<String> getSearchPhyLink(List<String> phyLinkIds) {
        log.debug("get search phy link by phyLinks ids:{}", phyLinkIds);
        if (CollectionUtils.isEmpty(phyLinkIds)) {
            return Collections.emptyList();
        }
        Map<String, PhyLinkCache> phyLinkCacheMaps = dciTopologyCacheManager.batchGetValues(
                phyLinkIds,
                PhyLinkCache.class);
        List<String> phyLinkNames = phyLinkCacheMaps.values().stream()
                .map(BasicCache::getFriendlyName).collect(
                        Collectors.toList());
        return phyLinkNames;
    }

    private List<String> getSearchSiteNode(List<String> siteNodeIds) {
        log.debug("get search site node by site ids:{}", siteNodeIds);
        if (CollectionUtils.isEmpty(siteNodeIds)) {
            return Collections.emptyList();
        }
        List<SiteInfo> siteInfos = siteNodeDao.getSiteFriendlyNameByIds(siteNodeIds);
        return siteInfos.stream().map(SiteInfo::getSiteName).collect(Collectors.toList());
    }

    private List<String> getSearchPhyNode(List<String> phyNodeIds) {
        if (CollectionUtils.isEmpty(phyNodeIds)) {
            return Collections.emptyList();
        }
        List<NodeInfoDto> phyNodeInfos = phyNodeDao.listConfigNodeINfoByIds(phyNodeIds);
        return phyNodeInfos.stream().map(NodeInfoDto::getFriendlyName).collect(Collectors.toList());
    }

    private List<String> getSearchSubnetDetail(List<String> subnetIds) {
        log.debug("get Subnet detail :{}", subnetIds);
        if (CollectionUtils.isEmpty(subnetIds)) {
            return Collections.emptyList();
        }
        List<SubNetTreeNode> subNetTreeNodes = subNetTreeNodeDao.getSubNetBySubnetIds(subnetIds);
        List<String> subNetTreeNodeName = subNetTreeNodes.stream().map(SubNetTreeNode::getName)
                .collect(
                        Collectors.toList());
        return subNetTreeNodeName;
    }


    private <T> List<T> truncateList(List<T> list) {
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream()
                .limit(PREVIEW_SIZE)
                .collect(Collectors.toList());
    }

    private <T> PreviewList<T> buildPreview(List<?> fullIdList, List<T> previewItems) {
        return PreviewList.<T>builder()
                .total(CollectionUtils.isEmpty(fullIdList) ? 0 : fullIdList.size())
                .items(previewItems)
                .build();
    }
}

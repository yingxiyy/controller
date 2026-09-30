package net.flex.dci.otn.controller.nms.utils;

import java.util.List;
import java.util.stream.Collectors;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otn.controller.nms.nms.component.dimension.dto.ExternalEdge;
import net.flex.dci.otn.controller.nms.nms.component.dimension.dto.InternalEdge;

/**
 * 2026/5/26
 *
 * @author musa
 * @version 1.0
 **/
public class ConnectionUtils {

    public static InternalEdge buildWssInternalEdge(String wssLinkId) {
        String sourceTpId = PhysicalLinkIdNamingRule.getTpAId(wssLinkId);
        String destTpId = PhysicalLinkIdNamingRule.getTpZId(wssLinkId);
        String sourceNeId = PhysicalLinkIdNamingRule.getNodeAId(wssLinkId);
        String destNeId = PhysicalLinkIdNamingRule.getNodeZId(wssLinkId);
        String siteId = PhysicalNodeIdNamingRule.getSiteId(sourceNeId);
        return InternalEdge.builder()
                .internalLinkId(wssLinkId)
                .siteId(siteId)
                .sourceNeId(sourceNeId)
                .destNeId(destNeId)
                .sourceTpId(sourceTpId)
                .destTpId(destTpId)
                .build();
    }

    public static List<InternalEdge> buildWssInternalEdges(List<String> wssLinkIds) {
        return wssLinkIds.stream().map(ConnectionUtils::buildWssInternalEdge)
                .collect(Collectors.toList());
    }

    public static ExternalEdge buildExternalEdge(String siteLinkId) {
        String sourceSiteId = SiteLinkIdNamingRule.getSiteA(siteLinkId);
        String destSiteId = SiteLinkIdNamingRule.getSiteZ(siteLinkId);
        String sourceNeId = SiteLinkIdNamingRule.getNodeA(siteLinkId);
        String destNeId = SiteLinkIdNamingRule.getNodeZ(siteLinkId);
        String sourceTpId = SiteLinkIdNamingRule.getTpAId(siteLinkId);
        String destTpId = SiteLinkIdNamingRule.getTpZId(siteLinkId);
        return ExternalEdge.builder().siteLinkId(siteLinkId).destSiteId(destSiteId)
                .sourceSiteId(sourceSiteId)
                .sourceNeId(sourceNeId)
                .destNeId(destNeId)
                .sourceTpId(sourceTpId)
                .destTpId(destTpId)
                .build();
    }

    public static List<ExternalEdge> buildExternalEdges(List<String> siteLinkIds) {
        return siteLinkIds.stream().map(ConnectionUtils::buildExternalEdge)
                .collect(Collectors.toList());
    }
}

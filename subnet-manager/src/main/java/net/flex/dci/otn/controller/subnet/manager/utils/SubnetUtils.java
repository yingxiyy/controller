package net.flex.dci.otn.controller.subnet.manager.utils;

import com.alibaba.fastjson.JSON;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.AuthConstant;
import net.flex.dci.otc.common.util.namingrule.OchLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import net.flex.dci.otn.controller.subnet.manager.dto.task.SubNetTaskOperationDetail;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Node1;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.ViewLinkType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.node.attributes.View;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.springframework.util.CollectionUtils;

/**
 * 2026/2/9
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
public class SubnetUtils {

    public static String getOperator(HttpServletRequest request) {
        String author = request.getHeader(AuthConstant.USER_TOKEN_HEADER);
        return author;
    }

    public static List<String> getPhyLinkIdsFromLinkSupportingLinks(List<Link> links) {
        if (CollectionUtils.isEmpty(links)) {
            return Collections.emptyList();
        }
        return links.stream()
                .flatMap(siteLink -> siteLink.getSupportingLink().stream())
                .map(SupportingLink::getLinkRef)
                .map(Uri::getValue)
                .filter(PhysicalLinkIdNamingRule::isPhysicalLinkId).distinct()
                .collect(Collectors.toList());
    }

    public static List<String> getLinkIds(List<Link> links) {
        return links.stream().map(LinkAttributes::getLinkId).map(
                Uri::getValue).collect(Collectors.toList());
    }

    public static String getSiteLinkRefSubnet(Link siteLink) {
        Site siteLinkPhysical = siteLink.getAugmentation(
                Link1.class).getSite();
        return siteLinkPhysical.getPlaneId();
    }

    public static String getViewNodeSubnetId(Node viewNode) {
        View view = viewNode.getAugmentation(Node1.class).getView();
        return view.getSubnetId();
    }


    public static String getViewLinkSubnetId(Link viewLink) {
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.link.attributes.View view = viewLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Link1.class)
                .getView();
        return view.getSubnetId();
    }

    public static String buildRequestDetail(String requestBody, String responseBody) {
        SubNetTaskOperationDetail subNetTaskOperationDetail = SubNetTaskOperationDetail.builder()
                .request(JSON.parse(requestBody))
                .response(responseBody)
                .build();
        return JSON.toJSONString(subNetTaskOperationDetail);
    }

    public static List<String> getPhyLinkIdsFromLinkStateSupportingLinks(
            List<LinkStateDto> totalLinks) {
        if (CollectionUtils.isEmpty(totalLinks)) {
            return Collections.emptyList();
        }
        List<String> phyLinkIds = totalLinks.stream()
                .flatMap(siteLink -> siteLink.getSupportingLink().stream())
                .filter(PhysicalLinkIdNamingRule::isPhysicalLinkId)
                .collect(Collectors.toList());
        return phyLinkIds;
    }

    @FunctionalInterface
    public interface SiteIdExtractionStrategy {

        String extractSiteId(String linkId, boolean isSource);
    }

    private static final Map<ViewLinkType, SiteIdExtractionStrategy> EXTRACTION_STRATEGY_MAP;

    static {
        EXTRACTION_STRATEGY_MAP = new HashMap<>();
        SiteIdExtractionStrategy osOtsExtractionStrategy = (linkId, isSource) -> isSource
                ? PhysicalLinkIdNamingRule.getSiteAId(linkId)
                : PhysicalLinkIdNamingRule.getSiteZId(linkId);
        EXTRACTION_STRATEGY_MAP.put(ViewLinkType.OsLink, osOtsExtractionStrategy);
        EXTRACTION_STRATEGY_MAP.put(ViewLinkType.OtsLink, osOtsExtractionStrategy);
        EXTRACTION_STRATEGY_MAP.put(ViewLinkType.SiteLink,
                (linkId, isSource) -> isSource ? SiteLinkIdNamingRule.getSiteA(linkId)
                        : SiteLinkIdNamingRule.getSiteZ(linkId));
        EXTRACTION_STRATEGY_MAP.put(ViewLinkType.OchLink,
                (linkId, isSource) -> isSource ? PhysicalTpIdNamingRule.getSiteId(
                        OchLinkIdNamingRule.getTpAId(linkId))
                        : PhysicalTpIdNamingRule.getSiteId(OchLinkIdNamingRule.getTpZId(linkId)));
    }


    public static String getLinkRefSiteByViewLinkType(String linkId, ViewLinkType viewLinkType,
            boolean isSource) {
        // 1. 空值防御：提前校验关键参数
        if (linkId == null || viewLinkType == null) {
            log.warn(
                    "extract site id failed：linkId or viewLinkType is null！linkId={}, viewLinkType={}",
                    linkId, viewLinkType);
            return null;
        }

        SiteIdExtractionStrategy strategy = EXTRACTION_STRATEGY_MAP.get(viewLinkType);
        if (strategy == null) {
            log.warn("can not find the ViewLinkType siteId extracted strategy！type={}, linkId={}",
                    viewLinkType, linkId);
            return null;
        }

        try {
            String siteId = strategy.extractSiteId(linkId, isSource);
            if (siteId == null) {
                log.debug("extract site id from linkId is null linkId={}, type={}, isSource={}",
                        linkId, viewLinkType, isSource);
            }
            return siteId;
        } catch (Exception e) {
            log.error("failed to extract site id from linkId,linkId={}, type={}, isSource={}",
                    linkId,
                    viewLinkType, isSource, e);
            return null;
        }
    }

}

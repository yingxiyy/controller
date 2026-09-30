package net.flex.dci.otc.controller.status.core.calculator.operation;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.core.cache.ConnectionCacheManager;
import net.flex.dci.otc.controller.status.core.cache.NodeCacheManager;
import net.flex.dci.otc.controller.status.dto.ProtectedLinkDto;
import net.flex.dci.otc.controller.status.dto.operation.SiteLinksOperState;
import net.flex.dci.otc.controller.status.dto.operation.SiteLinksOperState.SiteLinkOperState;
import net.flex.dci.otc.controller.status.util.LinkHelper;
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/8 16:41
 */
@Component
@Slf4j
public class SiteLinkOperationStateCalculator extends
        AbstractLinkOperationStateCalculator<SiteLinksOperState, LinkStateDto> {


    public SiteLinkOperationStateCalculator(LinkHelper linkHelper,
            ConnectionCacheManager connectionCacheManager,
            NodeCacheManager nodeCacheManager) {
        super(linkHelper, connectionCacheManager, nodeCacheManager);
    }

    @Override
    public SiteLinksOperState calculate(List<LinkStateDto> links) {
        log.debug("calculate the site links operstate");
        List<SiteLinkOperState> siteLinkOperStates = links.stream()
                .map(this::calculateSiteLinkOperState).collect(
                        Collectors.toList());
        log.debug("end to calculate the site links operstate");
        return SiteLinksOperState.builder().siteLinkOperStates(siteLinkOperStates).build();
    }

    @Override
    public SiteLinksOperState calculate(List<LinkStateDto> links, OperStatus operStatus) {
        return null;
    }

    private SiteLinkOperState calculateSiteLinkOperState(LinkStateDto link) {
        String linkId = link.getId();

        log.debug("start to calculate the site link oper state,site link id is:{}", linkId);
        boolean isVirtualSiteLink = isVirtualLink(linkId);
        if (isVirtualSiteLink) {
            return SiteLinkOperState.builder().siteLinkId(linkId).operStatus(OperStatus.Up).build();
        }
//        Boolean isProtectedMode = linkHelper.isSiteLinkInProtectedMode(link);
        OperStatus operStatus = OperStatus.Unknown;
//        if (!isProtectedMode) {
        operStatus = calculateUnprotectedOperState(link);
//        } else {
//            operStatus = calculateProtectedOperState(link);
//        }

        return SiteLinkOperState.builder().siteLinkId(linkId).operStatus(operStatus).build();
    }

    /**
     * calculate protected oper state model
     *
     * @param siteLink
     * @return
     */
    private OperStatus calculateProtectedOperState(Link siteLink) {
        ProtectedLinkDto protectedSiteLinkDto = linkHelper.getProtectedSiteLinkDetailInfo(
                siteLink);
        OperStatus operStatus = calculateProtectedLinkOperState(protectedSiteLinkDto);
        return operStatus;
    }


    /**
     * calculate unprotected oper state model
     *
     * @param link
     * @return
     */
    private OperStatus calculateUnprotectedOperState(LinkStateDto link) {
        log.debug("calculate unprotected operState link is:{}", link);
        List<String> phyLinkIds = link.getSupportingLink();
        return calculateRefLinkOperStatus(phyLinkIds);
    }


}

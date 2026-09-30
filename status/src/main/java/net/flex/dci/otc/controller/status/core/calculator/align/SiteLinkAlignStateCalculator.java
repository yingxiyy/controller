package net.flex.dci.otc.controller.status.core.calculator.align;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.core.cache.ConnectionCacheManager;
import net.flex.dci.otc.controller.status.core.cache.NodeCacheManager;
import net.flex.dci.otc.controller.status.dto.align.SiteLinksAlignState;
import net.flex.dci.otc.controller.status.dto.align.SiteLinksAlignState.SiteLinkAlignState;
import net.flex.dci.otc.controller.status.util.StatusUtil;
import net.flex.dci.otc.mongo.dao.EquipmentsDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.TerminationPointDao;
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/7 15:26
 */
@Component
@Slf4j
public class SiteLinkAlignStateCalculator extends
        AbstractAlignStateCalculator<SiteLinksAlignState, LinkStateDto> {


    private final PhyLinkDao phyLinkDao;

    private final ConnectionCacheManager connectionCacheManager;

    protected SiteLinkAlignStateCalculator(TerminationPointDao terminationPointDao,
            EquipmentsDao equipmentsDao,
            NodeCacheManager nodeCacheManager,
            PhyNodeDao phyNodeDao1, PhyLinkDao phyLinkDao,
            ConnectionCacheManager connectionCacheManager) {
        super(terminationPointDao, equipmentsDao, nodeCacheManager, phyNodeDao1);
        this.phyLinkDao = phyLinkDao;
        this.connectionCacheManager = connectionCacheManager;
    }


    @Override
    public SiteLinksAlignState calculateAlignState(List<LinkStateDto> links) {
        log.debug("start to calculate align state for site links :{}", links);
        List<SiteLinkAlignState> siteLinkAlignStates = links.stream()
                .map(this::calculateLinkAlignState).collect(
                        Collectors.toList());
        return SiteLinksAlignState.builder().siteLinkAlignStates(siteLinkAlignStates).build();
    }

    private SiteLinkAlignState calculateLinkAlignState(LinkStateDto link) {
        String siteLinkId = link.getId();
        log.debug("start to calculate the site link align status ,id:{}", siteLinkId);
//        Boolean isProtectedMode = LinkHelper.isSiteLinkInProtectedMode(link);
        AlignmentStatusType alignmentStatusType = calculateSiteLinkAlignState(link);
        return SiteLinkAlignState.builder().siteLinkId(siteLinkId).alignState(alignmentStatusType)
                .build();
    }

    /**
     * calculate unProtected site link align state
     *
     * @param link
     * @return
     */
    private AlignmentStatusType calculateSiteLinkAlignState(LinkStateDto link) {
        log.debug("calculate the unprotected site link align status,link is :{}", link);
        boolean isVirtualSiteLink = isVirtualLink(link.getId());
        if (isVirtualSiteLink) {
            return AlignmentStatusType.Aligned;
        }
        List<String> supportingPhyLinkIds = link.getSupportingLink();
        List<LinkStateDto> phyLinks = connectionCacheManager.getPhyLinksByIds(supportingPhyLinkIds);
        List<AlignmentStatusType> alignmentStatusTypes = phyLinks.stream()
                .map(LinkStateDto::getAlignment).collect(Collectors.toList());
        return StatusUtil.calculateAlignStatus(alignmentStatusTypes);
    }

//    private AlignmentStatusType calculateSiteLinkAlignState(Link link, boolean isProtectedMode) {
//        log.debug("start to calculate the site link align state");
//        AlignmentStatusType alignmentStatusType = AlignmentStatusType.Unknown;
//        if (isProtectedMode) {
//            alignmentStatusType = calculateProtectedSiteLinkAlignState(link);
//        } else {
//            alignmentStatusType = calculateUnprotectedSiteLinkAlignState(link);
//        }
//        return alignmentStatusType;
//    }
//
//    /**
//     * calculate  Protected site link align state
//     *
//     * @param link
//     * @return
//     */
//    private AlignmentStatusType calculateProtectedSiteLinkAlignState(Link link) {
//        return AlignmentStatusType.Unknown;
//    }
//
//
//    /**
//     * calculate unProtected site link align state
//     *
//     * @param link
//     * @return
//     */
//    private AlignmentStatusType calculateUnprotectedSiteLinkAlignState(Link link) {
//        log.debug("calculate the unprotected site link align status,link is :{}", link);
//        List<String> supportingPhyLinkIds = link.getSupportingLink().stream()
//                .map(supportingLink -> supportingLink.getLinkRef().getValue()).collect(
//                        Collectors.toList());
//        List<Link> phyLinks = phyLinkDao.getAllPhyLinksByIds(supportingPhyLinkIds);
//        List<AlignmentStatusType> alignmentStatusTypes = phyLinks.stream()
//                .map(phyLink -> phyLink.getAugmentation(
//                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class)
//                        .getPhysical().getAlignmentStatus()).collect(Collectors.toList());
//        return StatusUtil.calculateAlignStatus(alignmentStatusTypes);
//    }


}

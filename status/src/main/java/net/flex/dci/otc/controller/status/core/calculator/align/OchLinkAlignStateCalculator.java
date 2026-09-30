package net.flex.dci.otc.controller.status.core.calculator.align;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.core.cache.ConnectionCacheManager;
import net.flex.dci.otc.controller.status.core.cache.NodeCacheManager;
import net.flex.dci.otc.controller.status.dto.align.OchLinksAlignState;
import net.flex.dci.otc.controller.status.dto.align.OchLinksAlignState.OchLinkAlignState;
import net.flex.dci.otc.controller.status.util.StatusUtil;
import net.flex.dci.otc.mongo.dao.EquipmentsDao;
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
public class OchLinkAlignStateCalculator extends
        AbstractAlignStateCalculator<OchLinksAlignState, LinkStateDto> {

    private final ConnectionCacheManager connectionCacheManager;

    protected OchLinkAlignStateCalculator(TerminationPointDao terminationPointDao,
            EquipmentsDao equipmentsDao,
            NodeCacheManager nodeCacheManager,
            PhyNodeDao phyNodeDao1, ConnectionCacheManager connectionCacheManager) {
        super(terminationPointDao, equipmentsDao, nodeCacheManager, phyNodeDao1);
        this.connectionCacheManager = connectionCacheManager;
    }


    @Override
    public OchLinksAlignState calculateAlignState(List<LinkStateDto> links) {
        log.debug("start to calculate the align state for links:{}", links);

        Set<String> allSiteLinkIds = new HashSet<>();
        Set<String> allPhyLinkIds = new HashSet<>();
        for (LinkStateDto ochLink : links) {
            for (String supportLink : ochLink.getSupportingLink()) {
                if (StatusUtil.isSiteLinkId(supportLink)) {
                    allSiteLinkIds.add(supportLink);
                } else {
                    allPhyLinkIds.add(supportLink);
                }
            }
        }

        Map<String, LinkStateDto> siteLinkMap = batchGetSiteLinks(allSiteLinkIds);
        Map<String, LinkStateDto> phyLinkMap = batchGetPhyLinks(allPhyLinkIds);

        List<OchLinkAlignState> ochLinkAlignStateList = links.stream()
                .map(ochLink -> calculateOchLinkAlignState(ochLink, siteLinkMap, phyLinkMap))
                .collect(Collectors.toList());
        return OchLinksAlignState.builder().alignStates(ochLinkAlignStateList).build();
    }

    private OchLinkAlignState calculateOchLinkAlignState(LinkStateDto ochLink,
            Map<String, LinkStateDto> siteLinkMap,
            Map<String, LinkStateDto> phyLinkMap) {
        String ochLinkId = ochLink.getId();
        log.debug("start to calculate the och link alarm state,och link id is :{}", ochLinkId);
        List<String> supportLinks = ochLink.getSupportingLink();
        Map<Boolean, List<String>> partitionedRefIds = supportLinks.stream()
                .collect(Collectors.partitioningBy(StatusUtil::isSiteLinkId));
        List<String> refSiteLinkIds = partitionedRefIds.get(true);
        List<String> refPhyLinkIds = partitionedRefIds.get(false);
        AlignmentStatusType phyLinkAlignmentStatus = calculatePhyLinkAlignState(refPhyLinkIds,
                phyLinkMap);
        AlignmentStatusType siteLinkAlignmentStatus = calculateSiteLinkAlignState(refSiteLinkIds,
                siteLinkMap);
        AlignmentStatusType alignmentStatusType = StatusUtil.calculateAlignStatus(
                phyLinkAlignmentStatus, siteLinkAlignmentStatus);
        return OchLinkAlignState.builder().ochLinkId(ochLinkId)
                .alignmentStatusType(alignmentStatusType).build();
    }

    private AlignmentStatusType calculateSiteLinkAlignState(List<String> siteLinkIds,
            Map<String, LinkStateDto> siteLinkMap) {
        List<AlignmentStatusType> alignStates = siteLinkIds.stream()
                .map(siteLinkMap::get)
                .filter(Objects::nonNull)
                .map(LinkStateDto::getAlignment)
                .collect(Collectors.toList());
        return StatusUtil.calculateAlignStatus(alignStates);
    }


    private AlignmentStatusType calculatePhyLinkAlignState(List<String> phyLinkIds,
            Map<String, LinkStateDto> phyLinkMap) {
        List<AlignmentStatusType> alignmentStates = phyLinkIds.stream()
                .map(phyLinkMap::get)
                .map(link -> {
                    if (link == null || isVirtualLink(link.getId())) {
                        return AlignmentStatusType.Aligned;
                    }
                    return link.getAlignment();
                })
                .collect(Collectors.toList());
        return StatusUtil.calculateAlignStatus(alignmentStates);
    }

    private Map<String, LinkStateDto> batchGetSiteLinks(Set<String> siteLinkIds) {
        return connectionCacheManager.batchGetSiteLinksByIds(new ArrayList<>(siteLinkIds));
    }

    private Map<String, LinkStateDto> batchGetPhyLinks(Set<String> phyLinkIds) {
        return connectionCacheManager.batchGetPhyLinksByIds(new ArrayList<>(phyLinkIds));
    }


}

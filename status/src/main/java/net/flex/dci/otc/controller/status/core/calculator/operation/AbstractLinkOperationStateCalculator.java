package net.flex.dci.otc.controller.status.core.calculator.operation;

import static net.flex.dci.otc.controller.status.util.Constants.VIRTUAL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.controller.status.core.cache.ConnectionCacheManager;
import net.flex.dci.otc.controller.status.core.cache.NodeCacheManager;
import net.flex.dci.otc.controller.status.dto.ProtectedLinkDto;
import net.flex.dci.otc.controller.status.enums.ProtectionActivePathRole;
import net.flex.dci.otc.controller.status.util.LinkHelper;
import net.flex.dci.otc.controller.status.util.StatusUtil;
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 6/19/2025 3:39 PM
 */
@Slf4j
public abstract class AbstractLinkOperationStateCalculator<T, V> implements
        IOperationStateCalculator<T, V> {

    protected final LinkHelper linkHelper;


    protected final ConnectionCacheManager connectionCacheManager;

    protected final NodeCacheManager nodeCacheManager;


    public AbstractLinkOperationStateCalculator(LinkHelper linkHelper,
            ConnectionCacheManager connectionCacheManager, NodeCacheManager nodeCacheManager) {
        this.linkHelper = linkHelper;
        this.connectionCacheManager = connectionCacheManager;
        this.nodeCacheManager = nodeCacheManager;
    }

    protected OperStatus calculateMixTypeProtectedOperState(ProtectedLinkDto protectedLinkDto) {
        log.debug("calculate mixType protected  operState ");
        Set<ProtectionActivePathRole> protectionActivePathRoles = protectedLinkDto.getActivePathRoles();
        OperStatus operStatus = OperStatus.Unknown;
        for (ProtectionActivePathRole protectionActivePathRole : protectionActivePathRoles) {
            if (protectionActivePathRole == ProtectionActivePathRole.Primary) {
                OperStatus primaryOperStatus = calculateRefLinkOperStatus(
                        protectedLinkDto.getPrimaryLinkIds());
                operStatus = StatusUtil.calculateOperState(operStatus, primaryOperStatus);
            } else if (protectionActivePathRole == ProtectionActivePathRole.Secondary) {
                OperStatus secondaryOperStatus = calculateRefLinkOperStatus(
                        protectedLinkDto.getSecondaryLinkIds());
                operStatus = StatusUtil.calculateOperState(secondaryOperStatus, operStatus);
            } else if (protectionActivePathRole == ProtectionActivePathRole.Tertiary) {
                OperStatus tertiaryOperStatus = calculateRefLinkOperStatus(
                        protectedLinkDto.getTertiaryLinkIds());
                operStatus = StatusUtil.calculateOperState(tertiaryOperStatus, operStatus);
            }
        }

        return operStatus;
    }

    protected OperStatus calculateRefLinkOperStatus(List<String> linkIds) {
        log.debug("calculate ref link operStatue:{}", linkIds);
        Map<Boolean, List<String>> partitionedRefIds = linkIds.stream()
                .collect(Collectors.partitioningBy(StatusUtil::isSiteLinkId));
        List<String> refSiteLinkIds = partitionedRefIds.get(true);
        List<String> refPhyLinkIds = partitionedRefIds.get(false);
        Map<String, LinkStateDto> siteLinkMap = connectionCacheManager.batchGetSiteLinksByIds(
                refSiteLinkIds);
        Map<String, LinkStateDto> phyLinkMap = connectionCacheManager.batchGetPhyLinksByIds(
                refPhyLinkIds);
        OperStatus refPhyLinkOperStatus = calculatePhyLinkOperState(refPhyLinkIds, phyLinkMap);
        OperStatus refSiteLinkOperStatus = calculateSiteLinkOperStatus(refSiteLinkIds, siteLinkMap);
        OperStatus currentOperStatus = StatusUtil.calculateOperState(refSiteLinkOperStatus,
                refPhyLinkOperStatus);
        return currentOperStatus;
    }

    protected OperStatus calculateProtectedLinkOperState(ProtectedLinkDto protectedSiteLinkDto) {
        log.debug("start to calculate ");
        if (CollectionUtils.isEmpty(protectedSiteLinkDto.getActivePathRoles())) {
            log.debug("current protected link is not active,operation status unknown");
            return OperStatus.Unknown;
        }
        Set<ProtectionActivePathRole> protectionActivePathRoles = protectedSiteLinkDto.getActivePathRoles();
        int size = protectionActivePathRoles.size();
        OperStatus operStatus = OperStatus.Unknown;
        List<String> linkIds = new ArrayList<>();
        if (1 == size) {
            ProtectionActivePathRole role = protectionActivePathRoles.iterator().next();
            switch (role) {
                case Primary:
                    linkIds = protectedSiteLinkDto.getPrimaryLinkIds();
                    break;
                case Secondary:
                    linkIds = protectedSiteLinkDto.getSecondaryLinkIds();
                    break;
                case Tertiary:
                    linkIds = protectedSiteLinkDto.getTertiaryLinkIds();
                    break;
            }
            operStatus = calculateRefLinkOperStatus(linkIds);
        } else {
            operStatus = calculateMixTypeProtectedOperState(protectedSiteLinkDto);
        }

        return operStatus;
    }

    private OperStatus calculateSiteLinkOperStatus(List<String> siteLinkIds,
            Map<String, LinkStateDto> siteLinkMap) {
        List<OperStatus> operStatuses = siteLinkIds.stream()
                .map(siteLinkMap::get)
                .filter(Objects::nonNull)
                .map(LinkStateDto::getOperStatus)
                .collect(Collectors.toList());
        return StatusUtil.calculateOperState(operStatuses);
    }

    private OperStatus calculatePhyLinkOperState(List<String> phyLinkIds,
            Map<String, LinkStateDto> phyLinkMap) {
        List<OperStatus> operStatuses = phyLinkIds.stream()
                .map(phyLinkMap::get)
                .filter(Objects::nonNull)
                .map(LinkStateDto::getOperStatus)
                .collect(Collectors.toList());
        return StatusUtil.calculateOperState(operStatuses);
    }

    protected boolean isVirtualLink(String linkId) {
        boolean isSiteLink = SiteLinkIdNamingRule.isSiteLink(linkId);
        String srcNeId = isSiteLink ? SiteLinkIdNamingRule.getNodeA(linkId)
                : PhysicalLinkIdNamingRule.getNodeAId(linkId);
        String destNeId = isSiteLink ? SiteLinkIdNamingRule.getNodeZ(linkId)
                : PhysicalLinkIdNamingRule.getNodeZId(linkId);
        Map<String, Node> refNodesMap = nodeCacheManager.getConfigNodes(
                Arrays.asList(srcNeId, destNeId));
        Optional<Node> virtualNode = refNodesMap.values().stream()
                .filter(node -> node.getAugmentation(Node1.class).getPhysical().getVendorType()
                        .equals(VIRTUAL)).findAny();
        return virtualNode.isPresent();
    }

}

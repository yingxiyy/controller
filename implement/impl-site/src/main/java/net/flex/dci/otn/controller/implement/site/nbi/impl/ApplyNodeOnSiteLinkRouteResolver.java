package net.flex.dci.otn.controller.implement.site.nbi.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical;

final class ApplyNodeOnSiteLinkRouteResolver {

    private ApplyNodeOnSiteLinkRouteResolver() {
    }

    static Result resolve(Link siteLink, String insertedNodeId, Function<String, Link> phyLinkLoader) {
        List<Link> splitOtsLinks = getSplitOtsLinks(siteLink, insertedNodeId, phyLinkLoader);
        if (splitOtsLinks.size() != 2) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "inserted node should have two allocate OTS links, node-id="
                            + insertedNodeId + ", actual=" + splitOtsLinks.size());
        }
        Set<String> insertedNodeTpIds = splitOtsLinks.stream()
                .map(link -> getInsertedNodeTpId(link, insertedNodeId))
                .collect(Collectors.toSet());
        if (insertedNodeTpIds.size() != 2) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "inserted node should have two distinct route TPs, node-id="
                            + insertedNodeId + ", actual=" + insertedNodeTpIds.size());
        }

        List<String> azNodes = splitOtsLinks.stream()
                .flatMap(link -> Stream.of(
                        PhysicalLinkIdNamingRule.getNodeAId(link.getLinkId().getValue()),
                        PhysicalLinkIdNamingRule.getNodeZId(link.getLinkId().getValue())))
                .filter(nodeId -> !nodeId.equals(insertedNodeId))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        if (azNodes.size() != 2) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "cannot infer A/Z nodes for inserted node: " + insertedNodeId);
        }

        return new Result(insertedNodeId, azNodes, splitOtsLinks, insertedNodeTpIds);
    }

    static boolean hasOtherAllocateOts(Link siteLink, Set<String> currentSplitLinkIds,
            Function<String, Link> phyLinkLoader) {
        if (siteLink.getSupportingLink() == null) {
            return false;
        }
        return siteLink.getSupportingLink().stream()
                .map(SupportingLink::getLinkRef)
                .map(LinkId::getValue)
                .filter(PhysicalLinkIdNamingRule::isOtsLink)
                .filter(linkId -> !currentSplitLinkIds.contains(linkId))
                .map(phyLinkLoader)
                .anyMatch(link -> link != null && isAllocatePhyLink(link));
    }

    private static List<Link> getSplitOtsLinks(Link siteLink, String insertedNodeId,
            Function<String, Link> phyLinkLoader) {
        if (siteLink.getSupportingLink() == null) {
            return new ArrayList<>();
        }
        return siteLink.getSupportingLink().stream()
                .map(SupportingLink::getLinkRef)
                .map(LinkId::getValue)
                .filter(PhysicalLinkIdNamingRule::isOtsLink)
                .map(phyLinkLoader)
                .filter(link -> link != null && isAllocatePhyLink(link))
                .filter(link -> linkContainsNode(link, insertedNodeId))
                .sorted(Comparator.comparing(link -> link.getLinkId().getValue()))
                .collect(Collectors.toList());
    }

    private static boolean isAllocatePhyLink(Link link) {
        Physical physical = link.getAugmentation(Link1.class).getPhysical();
        return physical != null && ImplementState.Allocate.equals(physical.getImplementState());
    }

    private static boolean linkContainsNode(Link link, String nodeId) {
        String linkId = link.getLinkId().getValue();
        return nodeId.equals(PhysicalLinkIdNamingRule.getNodeAId(linkId))
                || nodeId.equals(PhysicalLinkIdNamingRule.getNodeZId(linkId));
    }

    private static String getInsertedNodeTpId(Link link, String insertedNodeId) {
        if (insertedNodeId.equals(link.getSource().getSourceNode().getValue())) {
            return link.getSource().getSourceTp().getValue();
        }
        return link.getDestination().getDestTp().getValue();
    }

    static class Result {
        final String insertedNodeId;
        final List<String> azNodeIds;
        final List<Link> splitOtsLinks;
        final Set<String> insertedNodeTpIds;

        Result(String insertedNodeId, List<String> azNodeIds, List<Link> splitOtsLinks,
                Set<String> insertedNodeTpIds) {
            this.insertedNodeId = insertedNodeId;
            this.azNodeIds = azNodeIds;
            this.splitOtsLinks = splitOtsLinks;
            this.insertedNodeTpIds = insertedNodeTpIds;
        }
    }
}

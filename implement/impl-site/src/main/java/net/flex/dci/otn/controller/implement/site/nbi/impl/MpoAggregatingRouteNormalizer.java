package net.flex.dci.otn.controller.implement.site.nbi.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;

/**
 * Expands virtual MPO route resources against the current SiteLink scope.
 *
 * <p>RouteInfo only knows the virtual "-MPO" route marker. ByteDance SiteLink
 * implementation must use the concrete supporting links generated for this
 * SiteLink; if those links are missing, fail instead of guessing from the
 * whole node and opening unrelated MPO ports.</p>
 */
class MpoAggregatingRouteNormalizer implements RouteInfo.MpoExpansionResolver {

    private static final String MPO_PATTERN = ".*-MPO\\d+$";

    private final ChangedObject changedObject;
    private final Set<String> supportedMpoLinkIds;
    private final Set<String> supportedMpoTpIds;

    MpoAggregatingRouteNormalizer(ChangedObject changedObject, Link siteLink) {
        this.changedObject = changedObject;
        this.supportedMpoLinkIds = collectSupportedMpoLinkIds(siteLink);
        this.supportedMpoTpIds = collectSupportedMpoTpIds(supportedMpoLinkIds);
    }

    static boolean supports(NeYangModel model) {
        // Chassis20 is the persisted Bone2.0 model; legacy ByteDance uses the same
        // virtual MPO route contract and must retain the existing normalization.
        return NeYangModel.ByteDance.equals(model) || NeYangModel.Chassis20.equals(model);
    }

    @Override
    public List<String> expandTp(String tpId) {
        List<String> supportedTps = findSupportedMpoTps(tpId);
        if (!supportedTps.isEmpty()) {
            return supportedTps;
        }
        throw cannotExpand("TP", tpId);
    }

    @Override
    public List<String> expandLink(String linkId) {
        Set<String> linkIds = findSupportedMpoLinksWithSameVirtualEndpoints(linkId);
        if (!linkIds.isEmpty()) {
            return new ArrayList<>(linkIds);
        }
        throw cannotExpand("link", linkId);
    }

    @Override
    public List<String> expandXc(String xcId) {
        List<String> existingXcs = findExistingMpoXcs(xcId);
        if (!existingXcs.isEmpty()) {
            return existingXcs;
        }
        throw cannotExpand("XC", xcId);
    }

    private Set<String> collectSupportedMpoLinkIds(Link siteLink) {
        Set<String> linkIds = new LinkedHashSet<>();
        if (siteLink == null) {
            return linkIds;
        }
        if (siteLink.getSupportingLink() != null) {
            for (SupportingLink supportingLink : siteLink.getSupportingLink()) {
                addSupportedMpoLink(linkIds, supportingLink.getLinkRef().getValue());
            }
        }

        Link1 siteAugmentation = siteLink.getAugmentation(Link1.class);
        if (siteAugmentation == null || siteAugmentation.getSite() == null
                || siteAugmentation.getSite().getSupportedLink() == null) {
            return linkIds;
        }
        siteAugmentation.getSite().getSupportedLink()
                .forEach(supportedLink -> addSupportedMpoLink(linkIds,
                        supportedLink.getLinkRef().getValue()));
        return linkIds;
    }

    private void addSupportedMpoLink(Set<String> linkIds, String linkId) {
        if (linkId != null && isExpandedMpo(linkId)) {
            linkIds.add(linkId);
        }
    }

    private Set<String> collectSupportedMpoTpIds(Set<String> linkIds) {
        Set<String> tpIds = new LinkedHashSet<>();
        for (String linkId : linkIds) {
            addExpandedMpoTp(tpIds, PhysicalLinkIdNamingRule.getTpAId(linkId));
            addExpandedMpoTp(tpIds, PhysicalLinkIdNamingRule.getTpZId(linkId));
        }
        return tpIds;
    }

    private void addExpandedMpoTp(Set<String> tpIds, String tpId) {
        if (isExpandedMpo(tpId)) {
            tpIds.add(tpId);
        }
    }

    private List<String> findSupportedMpoTps(String virtualTpId) {
        if (supportedMpoTpIds.isEmpty()) {
            return Collections.emptyList();
        }
        String virtualPrefix = virtualMpoPrefix(virtualTpId);
        List<String> tpIds = new ArrayList<>();
        for (String tpId : supportedMpoTpIds) {
            if (virtualPrefix.equals(virtualMpoPrefix(tpId))) {
                tpIds.add(tpId);
            }
        }
        return tpIds;
    }

    private Set<String> findSupportedMpoLinksWithSameVirtualEndpoints(String linkId) {
        Set<String> linkIds = new LinkedHashSet<>();
        String aPrefix = virtualMpoPrefix(PhysicalLinkIdNamingRule.getTpAId(linkId));
        String zPrefix = virtualMpoPrefix(PhysicalLinkIdNamingRule.getTpZId(linkId));
        for (String supportedLinkId : supportedMpoLinkIds) {
            if (sameVirtualEndpoints(PhysicalLinkIdNamingRule.getTpAId(supportedLinkId),
                    PhysicalLinkIdNamingRule.getTpZId(supportedLinkId), aPrefix, zPrefix)) {
                linkIds.add(supportedLinkId);
            }
        }
        return linkIds;
    }

    private List<String> findExistingMpoXcs(String xcId) {
        Set<String> requestedMpoPrefixes = virtualMpoPrefixesFromId(xcId);
        if (requestedMpoPrefixes.isEmpty()) {
            return Collections.emptyList();
        }
        Node node = changedObject.getChangedPhyNode(PhysicalXcIdNamingRule.getNodeId(xcId));
        if (node == null || node.getAugmentation(Node1.class) == null
                || node.getAugmentation(Node1.class).getPhysical() == null
                || node.getAugmentation(Node1.class).getPhysical().getCrossConnections() == null) {
            return Collections.emptyList();
        }

        List<String> xcIds = new ArrayList<>();
        for (CrossConnections xc : node.getAugmentation(Node1.class).getPhysical()
                .getCrossConnections()) {
            if (virtualMpoPrefixesFromXc(xc).containsAll(requestedMpoPrefixes)) {
                xcIds.add(xc.getCrossConnectionId().getValue());
            }
        }
        return xcIds;
    }

    private Set<String> virtualMpoPrefixesFromXc(CrossConnections xc) {
        Set<String> mpoPrefixes = new LinkedHashSet<>();
        if (xc.getSourceTp() != null) {
            xc.getSourceTp().forEach(sourceTp -> collectMpoPrefix(mpoPrefixes,
                    sourceTp.getTpRef().getValue()));
        }
        if (xc.getDestinationTp() != null) {
            xc.getDestinationTp().forEach(destinationTp -> collectMpoPrefix(mpoPrefixes,
                    destinationTp.getTpRef().getValue()));
        }
        return mpoPrefixes;
    }

    private Set<String> virtualMpoPrefixesFromId(String id) {
        Set<String> mpoPrefixes = new LinkedHashSet<>();
        String[] parts = id.split("-Site-");
        for (int index = 1; index < parts.length; index++) {
            collectMpoPrefix(mpoPrefixes, "Site-" + parts[index]);
        }
        return mpoPrefixes;
    }

    private void collectMpoPrefix(Set<String> mpoPrefixes, String tpId) {
        if (tpId != null && (isExpandedMpo(tpId) || tpId.endsWith("-MPO"))) {
            mpoPrefixes.add(virtualMpoPrefix(tpId));
        }
    }

    private boolean sameVirtualEndpoints(String firstTp, String secondTp, String aPrefix,
            String zPrefix) {
        String firstPrefix = virtualMpoPrefix(firstTp);
        String secondPrefix = virtualMpoPrefix(secondTp);
        return (aPrefix.equals(firstPrefix) && zPrefix.equals(secondPrefix))
                || (aPrefix.equals(secondPrefix) && zPrefix.equals(firstPrefix));
    }

    private boolean isExpandedMpo(String id) {
        return id != null && id.matches(MPO_PATTERN);
    }

    private String virtualMpoPrefix(String tpId) {
        return tpId == null ? "" : tpId.replaceFirst("MPO\\d+$", "MPO");
    }

    private CommonException cannotExpand(String resourceType, String resourceId) {
        return new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                String.format("cannot expand virtual MPO %s in current SiteLink scope: %s",
                        resourceType, resourceId));
    }
}

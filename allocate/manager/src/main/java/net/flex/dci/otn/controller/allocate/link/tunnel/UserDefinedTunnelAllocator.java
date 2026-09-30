package net.flex.dci.otn.controller.allocate.link.tunnel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelNewOchOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.AllocateTunnels2Input;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.creation.attributes.VendorOccupationRate;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.creation.attributes.vendor.occupation.rate.SelectedTunnelInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info._2.SiteLinkRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info._2.SiteLinkRouteBuilder;
import org.springframework.stereotype.Service;

@Service
public class UserDefinedTunnelAllocator extends TunnelAllocator2 {

    /**
     * User-defined OCH reuse policy. If reused-och-link-id is provided, only those OCH links
     * are allowed. If only card-ids are provided, reusable OCH links must already terminate on
     * those cards; otherwise allocation falls through to new-OCH creation with selected cards.
     */
    @Override
    protected Map<String, List<Link>> getReusedOchMap(AllocateTunnels2Input input, ParamCreate param,
                                                      String opMode) {
        Map<String, List<Link>> reusedOchMap = super.getReusedOchMap(input, param, opMode);
        Map<String, Set<String>> selectedReusedOchIds = getSelectedReusedOchIds(input);
        if (selectedReusedOchIds.isEmpty()) {
            return filterReusedOchBySelectedCards(input, reusedOchMap);
        }

        Map<String, List<Link>> filtered = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : selectedReusedOchIds.entrySet()) {
            String key = entry.getKey();
            Set<String> selectedIds = entry.getValue();
            List<Link> candidates = reusedOchMap.getOrDefault(key, Collections.emptyList());
            List<Link> matched = candidates.stream()
                    .filter(link -> selectedIds.contains(link.getLinkId().getValue()))
                    .collect(Collectors.toList());
            Set<String> matchedIds = matched.stream().map(link -> link.getLinkId().getValue())
                    .collect(Collectors.toSet());
            Set<String> missingIds = new HashSet<>(selectedIds);
            missingIds.removeAll(matchedIds);
            if (!missingIds.isEmpty()) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "Selected reused och link is not available: " + missingIds);
            }
            validateSelectedCardsOnReusedOch(input, key, matched);
            filtered.put(key, matched);
        }
        return filtered;
    }

    /**
     * User-defined new-OCH policy. Selected central-frequency values replace automatic
     * frequency selection for new OCH creation, and selected card IDs are passed to the
     * designer through the getSelectedCardIdsBySite hook.
     */
    @Override
    protected TunnelNewOchOutput allocateTunnelsNewOch(Integer number, SiteLinkRoute siteLinkRoute,
                                                       Map<String, Node> totalInMemoryNode, String vendorName,
                                                       String productType, ParamCreate param,
                                                       AllocateTunnels2Input input, String opMode,
                                                       Set<String> usedNodeSet, WDM_Band wdmBand) {
        List<SelectedTunnelInfo> selectedTunnelInfos = getSelectedTunnelInfos(input, vendorName,
                productType);
        SiteLinkRoute route = siteLinkRoute;
        List<Long> selectedFrequenciesConfigured = selectedTunnelInfos.stream()
                .filter(info -> info.getReusedOchLinkId() == null)
                .filter(info -> info.getCentralFrequency() != null)
                .map(info -> info.getCentralFrequency().longValue())
                .collect(Collectors.toList());
        List<Long> selectedFrequencies = selectedFrequenciesConfigured.stream()
                .filter(frequency -> siteLinkRoute.getCentralFrequencies().contains(frequency))
                .collect(Collectors.toList());
        if (!selectedFrequenciesConfigured.isEmpty()) {
            if (selectedFrequencies.size() < param.getOchNumber(number)) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "Selected central-frequency number is less than required och number.");
            }
            route = new SiteLinkRouteBuilder(siteLinkRoute)
                    .setCentralFrequencies(selectedFrequencies)
                    .build();
        }

        return super.allocateTunnelsNewOch(number, route, totalInMemoryNode, vendorName,
                productType, param, input, opMode, usedNodeSet, wdmBand);
    }

    /**
     * Converts selected-tunnel-info.card-ids into a site-scoped constraint map. The card-id
     * format is Site#Ne#LINECARD-shelf-slot; the site prefix determines which A/Z/REG site is
     * constrained. Sites not present in this map keep the original automatic allocation logic.
     */
    @Override
    protected Map<String, List<String>> getSelectedCardIdsBySite(AllocateTunnels2Input input,
                                                                 String vendorName,
                                                                 String productType) {
        return getSelectedCardIdsBySite(getSelectedTunnelInfos(input, vendorName, productType));
    }

    /**
     * Extracts explicit reused OCH choices by vendor/product so normal OCH reuse cannot pick a
     * different link when the REST caller already selected one.
     */
    private Map<String, Set<String>> getSelectedReusedOchIds(AllocateTunnels2Input input) {
        Map<String, Set<String>> selectedReusedOchIds = new HashMap<>();
        for (VendorOccupationRate vendor : input.getVendorOccupationRate()) {
            String key = TunnelUtil.createVendorProductKey(vendor.getVendorName(),
                    vendor.getProductType());
            for (SelectedTunnelInfo info : getSelectedTunnelInfos(vendor)) {
                if (info.getReusedOchLinkId() == null) {
                    continue;
                }
                selectedReusedOchIds.computeIfAbsent(key, ignored -> new HashSet<>())
                        .add(info.getReusedOchLinkId());
            }
        }
        return selectedReusedOchIds;
    }

    /**
     * Card-only user-defined requests may still reuse an OCH, but only if the OCH endpoints are
     * exactly on the selected cards. Non-matching reusable OCHs are hidden so new-OCH allocation
     * can honor the requested card positions.
     */
    private Map<String, List<Link>> filterReusedOchBySelectedCards(AllocateTunnels2Input input,
                                                                   Map<String, List<Link>> reusedOchMap) {
        Map<String, List<Link>> filtered = new HashMap<>();
        for (VendorOccupationRate vendor : input.getVendorOccupationRate()) {
            String key = TunnelUtil.createVendorProductKey(vendor.getVendorName(),
                    vendor.getProductType());
            Set<String> selectedCardIds = getSelectedTunnelInfos(vendor).stream()
                    .filter(info -> info.getCardIds() != null)
                    .flatMap(info -> info.getCardIds().stream())
                    .collect(Collectors.toSet());
            if (selectedCardIds.isEmpty()) {
                filtered.put(key, reusedOchMap.getOrDefault(key, Collections.emptyList()));
                continue;
            }
            List<Link> matched = reusedOchMap.getOrDefault(key, Collections.emptyList()).stream()
                    .filter(link -> matchSelectedCards(link, selectedCardIds))
                    .collect(Collectors.toList());
            filtered.put(key, matched);
        }
        return filtered;
    }

    private List<SelectedTunnelInfo> getSelectedTunnelInfos(AllocateTunnels2Input input,
                                                            String vendorName,
                                                            String productType) {
        for (VendorOccupationRate vendor : input.getVendorOccupationRate()) {
            if (vendorName.equals(vendor.getVendorName())
                    && productType.equals(vendor.getProductType())) {
                return getSelectedTunnelInfos(vendor);
            }
        }
        return Collections.emptyList();
    }

    private List<SelectedTunnelInfo> getSelectedTunnelInfos(VendorOccupationRate vendor) {
        return vendor.getSelectedTunnelInfo() == null ? Collections.emptyList()
                : vendor.getSelectedTunnelInfo();
    }

    private Map<String, List<String>> getSelectedCardIdsBySite(List<SelectedTunnelInfo> selectedTunnelInfos) {
        Map<String, List<String>> selectedCardIdsBySite = new HashMap<>();
        for (SelectedTunnelInfo info : selectedTunnelInfos) {
            if (info.getReusedOchLinkId() != null || info.getCardIds() == null) {
                continue;
            }
            for (String cardId : info.getCardIds()) {
                String siteId = getSiteId(cardId);
                selectedCardIdsBySite.computeIfAbsent(siteId, ignored -> new ArrayList<>())
                        .add(cardId);
            }
        }
        return selectedCardIdsBySite;
    }

    /**
     * Validates the stronger case where both reused-och-link-id and card-ids are provided.
     * A reusable OCH cannot move L ports, so selected cards must match the OCH endpoint cards.
     */
    private void validateSelectedCardsOnReusedOch(AllocateTunnels2Input input, String key,
                                                  List<Link> reusedOchLinks) {
        List<String> selectedCardIds = input.getVendorOccupationRate().stream()
                .filter(vendor -> key.equals(TunnelUtil.createVendorProductKey(
                        vendor.getVendorName(), vendor.getProductType())))
                .flatMap(vendor -> getSelectedTunnelInfos(vendor).stream())
                .filter(info -> info.getReusedOchLinkId() != null)
                .filter(info -> info.getCardIds() != null)
                .flatMap(info -> info.getCardIds().stream())
                .collect(Collectors.toList());
        if (selectedCardIds.isEmpty()) {
            return;
        }

        Set<String> reusedOchCardIds = reusedOchLinks.stream()
                .flatMap(link -> java.util.Arrays.asList(
                        PhysicalTpIdNamingRule.getEquipId(link.getSource().getSourceTp().getValue()),
                        PhysicalTpIdNamingRule.getEquipId(link.getDestination().getDestTp().getValue()))
                        .stream())
                .collect(Collectors.toSet());
        List<String> unmatched = selectedCardIds.stream()
                .filter(cardId -> !reusedOchCardIds.contains(normalizeSelectedCardId(cardId)))
                .collect(Collectors.toList());
        if (!unmatched.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Selected card-id does not match selected reused OCH link: " + unmatched);
        }
    }

    private boolean matchSelectedCards(Link link, Set<String> selectedCardIds) {
        Set<String> reusedOchCardIds = java.util.Arrays.asList(
                PhysicalTpIdNamingRule.getEquipId(link.getSource().getSourceTp().getValue()),
                PhysicalTpIdNamingRule.getEquipId(link.getDestination().getDestTp().getValue()))
                .stream()
                .collect(Collectors.toSet());
        return selectedCardIds.stream()
                .map(this::normalizeSelectedCardId)
                .allMatch(reusedOchCardIds::contains);
    }

    private String getSiteId(String cardId) {
        String normalizedCardId = normalizeSelectedCardId(cardId);
        int index = normalizedCardId.indexOf("#");
        if (index <= 0) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Invalid card-id: " + cardId);
        }
        return normalizedCardId.substring(0, index);
    }

    private String normalizeSelectedCardId(String selectedCardId) {
        int index = selectedCardId.indexOf(":");
        if (index > 0) {
            String prefix = selectedCardId.substring(0, index);
            if ("OT".equalsIgnoreCase(prefix) || "OP".equalsIgnoreCase(prefix)) {
                return selectedCardId.substring(index + 1);
            }
        }
        return selectedCardId;
    }
}

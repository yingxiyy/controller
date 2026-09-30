package net.flex.dci.otn.controller.allocate.designer.site;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.flex.dci.otn.controller.allocate.ne.ExternalLinkTo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RoutingType;

/** Resource-layout extension point for vendor scenarios that cannot be expressed by siteModel JSON. */
abstract class SiteResourceLayout {

    abstract List<String> resolveMainCardClasses(List<String> originalCardClasses);

    abstract List<String> getProtectionPeerCardClasses();

    abstract boolean requiresDedicatedProtectionNode();

    abstract boolean reuseProtectionPeerNodeForThird();

    abstract Integer getMainNodeSlot(String cardType, int occurrence);

    abstract Integer getProtectionNodeSlot(RoutingType role);

    abstract String getOlpLinePort(RoutingType role);

    boolean supportsTilaClassMode() {
        return false;
    }

    String resolveTilaClassMode(String cardType, boolean reversed, RoutingType protectionPeerRole) {
        return null;
    }

    String resolveEndpointTilaClassMode(String cardType, boolean reversed) {
        if (!"TILA".equals(cardType)) {
            return null;
        }
        // Bone2.0 OTM endpoints use LINE_WEST internally and reserve LINE_EAST for the fiber span.
        return "OTA_WEST";
    }

    boolean useFmux32Com1ProtectionXc(String cardType) {
        return false;
    }

    boolean hasCardSelfLinks(String cardType) {
        return false;
    }

    boolean usesEastZEndTilaOutput() {
        return false;
    }

    boolean usesDualFmux64Topology() {
        return false;
    }

    boolean routesDualFmux64ThroughOlp() {
        return false;
    }

    String getFmuxMainLinePort(boolean protectedLink) {
        return ByteDance2CardChainResolver.getFmuxMainLinePort(protectedLink);
    }

    Map<String, List<ExternalLinkTo>> selectZEndExternalLinks(
            Map<String, List<ExternalLinkTo>> fromToMap, String destinationCardType) {
        if (!usesEastZEndTilaOutput() || !"TILA".equals(destinationCardType)) {
            return fromToMap;
        }

        Map<String, List<ExternalLinkTo>> selected = new LinkedHashMap<>();
        fromToMap.forEach((sourcePort, targets) -> selected.put(sourcePort, targets.stream()
                .map(target -> {
                    if (!"TILA".equals(target.getCardType()) || !"LINE_WEST".equals(target.getPort())) {
                        return target;
                    }
                    ExternalLinkTo eastTarget = new ExternalLinkTo();
                    eastTarget.setNeType(target.getNeType());
                    eastTarget.setCardType(target.getCardType());
                    eastTarget.setPort("LINE_EAST");
                    return eastTarget;
                })
                .collect(Collectors.toList())));
        return selected;
    }

    Map<String, List<ExternalLinkTo>> selectCardSelfLinks(
            Map<String, List<ExternalLinkTo>> fromToMap, String cardType) {
        return new LinkedHashMap<>();
    }

    Map<String, List<ExternalLinkTo>> selectFmuxTilaLinks(
            Map<String, List<ExternalLinkTo>> fromToMap, String sourceCardType,
            String destinationCardType, String selectedFmuxPort) {
        return keepSingleFmuxTilaPort(fromToMap, sourceCardType, destinationCardType,
                selectedFmuxPort, "FMUX_32", "TILA", "LINE_WEST", "LINE_WEST");
    }

    Map<String, List<ExternalLinkTo>> selectOlpTilaLinks(
            Map<String, List<ExternalLinkTo>> fromToMap, String sourceCardType,
            String destinationCardType) {
        return fromToMap;
    }

    Map<String, List<ExternalLinkTo>> keepSingleFmuxTilaPort(
            Map<String, List<ExternalLinkTo>> fromToMap, String sourceCardType,
            String destinationCardType, String selectedFmuxPort, String fmuxCardType,
            String tilaCardType, String fmuxToTilaPort, String tilaToFmuxPort) {
        if (!(fmuxCardType.equals(sourceCardType) && tilaCardType.equals(destinationCardType))
                && !(tilaCardType.equals(sourceCardType) && fmuxCardType.equals(destinationCardType))) {
            return fromToMap;
        }

        Map<String, List<ExternalLinkTo>> selected = new LinkedHashMap<>();
        if (fmuxCardType.equals(sourceCardType)) {
            if (fromToMap.containsKey(selectedFmuxPort)) {
                // FMUX protection legs enter the local TILA WEST side; EAST stays free for the fiber span.
                List<ExternalLinkTo> targets = fromToMap.get(selectedFmuxPort).stream()
                        .filter(target -> fmuxToTilaPort.equals(target.getPort()))
                        .collect(Collectors.toList());
                if (!targets.isEmpty()) {
                    selected.put(selectedFmuxPort, targets);
                }
            }
            return selected;
        }

        // Reversed Z-end route also starts at TILA WEST; EAST remains the external OTM output.
        if (fromToMap.containsKey(tilaToFmuxPort)) {
            List<ExternalLinkTo> targets = fromToMap.get(tilaToFmuxPort).stream()
                    .filter(target -> selectedFmuxPort.equals(target.getPort()))
                    .collect(Collectors.toList());
            if (!targets.isEmpty()) {
                selected.put(tilaToFmuxPort, targets);
            }
        }
        return selected;
    }
}

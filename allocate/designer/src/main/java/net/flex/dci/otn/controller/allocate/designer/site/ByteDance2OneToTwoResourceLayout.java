package net.flex.dci.otn.controller.allocate.designer.site;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.flex.dci.otn.controller.allocate.ne.ExternalLinkTo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RoutingType;

/**
 * Hard-coded Bone2.0 1+2 OTM layout from "BONE 2.0 Network scenario-card.pptx",
 * slides 14 and 15. The vendor chassis layout cannot be represented by the generic
 * siteModel JSON: NE1 contains OLP3-3/FMUX/main TILA, while slave and third TILA
 * share a second NE. Keep all future slot or port changes for this scenario here.
 */
final class ByteDance2OneToTwoResourceLayout extends SiteResourceLayout {

    static final String PANEL = "PANEL";
    static final String MUXPANEL = "MUXPANEL";
    static final String FMUX = "CMUX";
    static final String OLP = "OP";
    static final String TILA = "ILA";
    private static final String OLP_CARD_TYPE = "OLP3_3";
    private static final String TILA_CARD_TYPE = "TILA";
    private static final String TILA_INTERNAL_PORT = "LINE_WEST";
    private static final String FMUX_CARD_TYPE = "FMUX_32";
    private static final int FLEX64_BANDWIDTH = 64;

    private final int bandwidth;

    ByteDance2OneToTwoResourceLayout(int bandwidth) {
        this.bandwidth = bandwidth;
    }

    @Override
    List<String> resolveMainCardClasses(List<String> originalCardClasses) {
        List<String> result = new ArrayList<>(Arrays.asList(PANEL, MUXPANEL, FMUX));
        if (bandwidth == 64) {
            result.add(FMUX);
        }
        result.add(OLP);
        result.add(TILA);
        return result;
    }

    @Override
    List<String> getProtectionPeerCardClasses() {
        return Arrays.asList(TILA);
    }

    @Override
    boolean requiresDedicatedProtectionNode() {
        return true;
    }

    @Override
    boolean reuseProtectionPeerNodeForThird() {
        return true;
    }

    @Override
    Integer getMainNodeSlot(String cardType, int occurrence) {
        if ("OLP3_3".equals(cardType)) {
            return 1;
        }
        if ("FMUX_32".equals(cardType)) {
            return occurrence == 0 ? 3 : 7;
        }
        if ("TILA".equals(cardType)) {
            return 5;
        }
        return null;
    }

    @Override
    Integer getProtectionNodeSlot(RoutingType role) {
        if (role == RoutingType.Slave) {
            return 1;
        }
        if (role == RoutingType.Third) {
            return 3;
        }
        return null;
    }

    @Override
    String getOlpLinePort(RoutingType role) {
        if (role == RoutingType.Main) {
            return "1A";
        }
        if (role == RoutingType.Slave) {
            return "1B";
        }
        if (role == RoutingType.Third) {
            return "1C";
        }
        return null;
    }

    @Override
    Map<String, List<ExternalLinkTo>> selectOlpTilaLinks(
            Map<String, List<ExternalLinkTo>> fromToMap, String sourceCardType,
            String destinationCardType) {
        Map<String, List<ExternalLinkTo>> selected = new LinkedHashMap<>();
        if (OLP_CARD_TYPE.equals(sourceCardType) && TILA_CARD_TYPE.equals(destinationCardType)) {
            fromToMap.forEach((sourcePort, targets) -> {
                List<ExternalLinkTo> westTargets = targets.stream()
                        .filter(target -> TILA_CARD_TYPE.equals(target.getCardType()))
                        .filter(target -> TILA_INTERNAL_PORT.equals(target.getPort()))
                        .collect(Collectors.toList());
                if (!westTargets.isEmpty()) {
                    selected.put(sourcePort, westTargets);
                }
            });
            return selected;
        }
        if (TILA_CARD_TYPE.equals(sourceCardType) && OLP_CARD_TYPE.equals(destinationCardType)) {
            List<ExternalLinkTo> olpTargets = fromToMap
                    .getOrDefault(TILA_INTERNAL_PORT, java.util.Collections.emptyList()).stream()
                    .filter(target -> OLP_CARD_TYPE.equals(target.getCardType()))
                    .collect(Collectors.toList());
            if (!olpTargets.isEmpty()) {
                selected.put(TILA_INTERNAL_PORT, olpTargets);
            }
            return selected;
        }
        return fromToMap;
    }

    @Override
    boolean supportsTilaClassMode() {
        return true;
    }

    @Override
    boolean usesEastZEndTilaOutput() {
        return true;
    }

    @Override
    boolean usesDualFmux64Topology() {
        return bandwidth == FLEX64_BANDWIDTH;
    }

    @Override
    boolean routesDualFmux64ThroughOlp() {
        return usesDualFmux64Topology();
    }

    @Override
    boolean useFmux32Com1ProtectionXc(String cardType) {
        return usesDualFmux64Topology() && FMUX_CARD_TYPE.equals(cardType);
    }

    @Override
    boolean hasCardSelfLinks(String cardType) {
        return usesDualFmux64Topology() && FMUX_CARD_TYPE.equals(cardType);
    }

    @Override
    Map<String, List<ExternalLinkTo>> selectCardSelfLinks(
            Map<String, List<ExternalLinkTo>> fromToMap, String cardType) {
        Map<String, List<ExternalLinkTo>> selected = new LinkedHashMap<>();
        if (!hasCardSelfLinks(cardType) || !fromToMap.containsKey("SIG")) {
            return selected;
        }
        List<ExternalLinkTo> targets = fromToMap.get("SIG").stream()
                .filter(target -> FMUX_CARD_TYPE.equals(target.getCardType()))
                .filter(target -> "COM2".equals(target.getPort()))
                .collect(Collectors.toList());
        if (!targets.isEmpty()) {
            selected.put("SIG", targets);
        }
        return selected;
    }

    @Override
    String resolveTilaClassMode(String cardType, boolean reversed, RoutingType protectionPeerRole) {
        return resolveEndpointTilaClassMode(cardType, reversed);
    }
}

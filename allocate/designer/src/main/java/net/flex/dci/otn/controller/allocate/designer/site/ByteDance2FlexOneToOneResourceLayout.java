package net.flex.dci.otn.controller.allocate.designer.site;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.flex.dci.otn.controller.allocate.ne.ExternalLinkTo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RoutingType;

/** Bone2.0 Flex C 32-wave OMSP layout: both TILA cards live on the same NE. */
final class ByteDance2FlexOneToOneResourceLayout extends SiteResourceLayout {

    private static final String FMUX_CARD_TYPE = "FMUX_32";
    private static final String TILA_CARD_TYPE = "TILA";
    private static final String FMUX_TO_TILA_PORT = "LINE_WEST";
    private static final String TILA_TO_FMUX_PORT = "LINE_WEST";

    @Override
    List<String> resolveMainCardClasses(List<String> originalCardClasses) {
        return Arrays.asList("PANEL", "MUXPANEL", "CMUX", "ILA");
    }

    @Override
    List<String> getProtectionPeerCardClasses() {
        return Collections.emptyList();
    }

    @Override
    boolean requiresDedicatedProtectionNode() {
        return false;
    }

    @Override
    boolean reuseProtectionPeerNodeForThird() {
        return false;
    }

    @Override
    Integer getMainNodeSlot(String cardType, int occurrence) {
        if ("TILA".equals(cardType)) {
            return 1;
        }
        if ("FMUX_32".equals(cardType)) {
            return occurrence == 0 ? 3 : 7;
        }
        return null;
    }

    @Override
    Integer getProtectionNodeSlot(RoutingType role) {
        return null;
    }

    @Override
    String getOlpLinePort(RoutingType role) {
        return null;
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
    String resolveTilaClassMode(String cardType, boolean reversed, RoutingType protectionPeerRole) {
        return resolveEndpointTilaClassMode(cardType, reversed);
    }

    @Override
    boolean useFmux32Com1ProtectionXc(String cardType) {
        return FMUX_CARD_TYPE.equals(cardType);
    }

    @Override
    boolean hasCardSelfLinks(String cardType) {
        return FMUX_CARD_TYPE.equals(cardType);
    }

    @Override
    Map<String, List<ExternalLinkTo>> selectCardSelfLinks(
            Map<String, List<ExternalLinkTo>> fromToMap, String cardType) {
        Map<String, List<ExternalLinkTo>> selected = new LinkedHashMap<>();
        if (!FMUX_CARD_TYPE.equals(cardType) || !fromToMap.containsKey("SIG")) {
            return selected;
        }
        // Flex32 routes model the common FMUX SIG-COM2 segment as a physical OMS link.
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
    Map<String, List<ExternalLinkTo>> selectFmuxTilaLinks(
            Map<String, List<ExternalLinkTo>> fromToMap, String sourceCardType,
            String destinationCardType, String selectedFmuxPort) {
        return keepSingleFmuxTilaPort(fromToMap, sourceCardType, destinationCardType, selectedFmuxPort,
                FMUX_CARD_TYPE, TILA_CARD_TYPE, FMUX_TO_TILA_PORT, TILA_TO_FMUX_PORT);
    }
}

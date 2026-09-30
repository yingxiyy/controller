package net.flex.dci.otn.controller.allocate.designer.site;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.flex.dci.otn.controller.allocate.ne.ExternalLinkTo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RoutingType;

/**
 * Bone2.0 Flex C 64T endpoint layout.
 *
 * <p>The MUX panel fans out to two FMUX_32 cards. Slot 3 is the main FMUX
 * connected to TILA, while slot 7 contributes the second 32-channel group
 * through the main FMUX COM1 port.</p>
 */
final class ByteDance2Flex64ResourceLayout extends SiteResourceLayout {

    private static final String FMUX_CARD_TYPE = "FMUX_32";
    private final boolean protectedLayout;

    ByteDance2Flex64ResourceLayout() {
        this(false);
    }

    ByteDance2Flex64ResourceLayout(boolean protectedLayout) {
        this.protectedLayout = protectedLayout;
    }

    @Override
    List<String> resolveMainCardClasses(List<String> originalCardClasses) {
        return Arrays.asList("PANEL", "MUXPANEL", "CMUX", "CMUX", "ILA");
    }

    @Override
    List<String> getProtectionPeerCardClasses() {
        return java.util.Collections.emptyList();
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
    boolean usesDualFmux64Topology() {
        return true;
    }

    @Override
    String getFmuxMainLinePort(boolean protectedLink) {
        return "SIGA";
    }

    @Override
    boolean useFmux32Com1ProtectionXc(String cardType) {
        return protectedLayout && FMUX_CARD_TYPE.equals(cardType);
    }

    @Override
    boolean hasCardSelfLinks(String cardType) {
        return FMUX_CARD_TYPE.equals(cardType);
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

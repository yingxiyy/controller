package net.flex.dci.otn.controller.allocate.designer.site;

import java.util.Arrays;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RoutingType;

/** Bone2.0 fixed-grid 1+2 layout from scenario-card slides 20 and 21. */
final class ByteDance2FixedOneToTwoResourceLayout extends SiteResourceLayout {

    @Override
    List<String> resolveMainCardClasses(List<String> originalCardClasses) {
        return Arrays.asList("PANEL", "MUX", "OP", "ILA");
    }

    @Override
    List<String> getProtectionPeerCardClasses() {
        // Dedicated protection NE still follows the standard I-node shell: PANEL + ILA.
        return Arrays.asList("PANEL", "ILA");
    }

    @Override
    boolean requiresDedicatedProtectionNode() {
        return true;
    }

    @Override
    boolean reuseProtectionPeerNodeForThird() {
        return false;
    }

    @Override
    Integer getMainNodeSlot(String cardType, int occurrence) {
        if ("OLP3_3".equals(cardType)) {
            return 1;
        }
        if ("TILA".equals(cardType)) {
            return 3;
        }
        return null;
    }

    @Override
    Integer getProtectionNodeSlot(RoutingType role) {
        if (role == RoutingType.Slave || role == RoutingType.Third) {
            return 1;
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
}

package net.flex.dci.otn.controller.allocate.designer.site;

import java.util.Collections;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RoutingType;

/** Preserves the existing site-model driven allocation behavior. */
class DefaultSiteResourceLayout extends SiteResourceLayout {

    private final boolean bone2TilaClassMode;

    DefaultSiteResourceLayout() {
        this(false);
    }

    DefaultSiteResourceLayout(boolean bone2TilaClassMode) {
        this.bone2TilaClassMode = bone2TilaClassMode;
    }

    @Override
    List<String> resolveMainCardClasses(List<String> originalCardClasses) {
        return originalCardClasses;
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
        return bone2TilaClassMode;
    }

    @Override
    String resolveTilaClassMode(String cardType, boolean reversed, RoutingType protectionPeerRole) {
        if (!bone2TilaClassMode || !"TILA".equals(cardType)) {
            return null;
        }
        // Bone2.0 transit TILA is a real ILA, not an OMS start/end OTA.
        return "ILA";
    }
}

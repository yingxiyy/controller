package net.flex.dci.otn.controller.allocate.designer.site;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RoutingType;

/**
 * Bone2.0 endpoint behavior for scenarios that otherwise use the site-model driven layout.
 */
class ByteDance2DefaultResourceLayout extends DefaultSiteResourceLayout {

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

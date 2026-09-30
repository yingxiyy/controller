package net.flex.dci.otn.controller.nms.nms.component.connection;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsPath;

/**
 * @version 1.0
 * @date 9/15/2025 3:07 PM
 */
public interface ServiceBearer {

    ApsPath getProtectedTunnelSiteLinkLocation(String tunnelId, String siteLinkId, Link ochLink);
}

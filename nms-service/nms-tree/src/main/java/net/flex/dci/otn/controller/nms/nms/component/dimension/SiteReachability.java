package net.flex.dci.otn.controller.nms.nms.component.dimension;

import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.reachability.output.Reachability;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;

/**
 * 2026/4/9
 *
 * @author musa
 * @version 1.0
 **/
public interface SiteReachability {


    Reachability getReachability(String subnetId, NodeId sourceSite, NodeId destSite);
}

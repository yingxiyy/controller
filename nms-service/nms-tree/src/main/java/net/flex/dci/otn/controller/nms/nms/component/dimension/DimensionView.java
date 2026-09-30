package net.flex.dci.otn.controller.nms.nms.component.dimension;

import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteReachabilityInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSubnetDimensionalViewInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.reachability.output.Reachability;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.subnet.dimensional.view.output.Topology;

/**
 * 2026/3/26
 *
 * @author musa
 * @version 1.0
 **/
public interface DimensionView {

    Topology getSubnetDimensionView(GetSubnetDimensionalViewInput getSubnetDimensionalViewInput);

    Reachability getSiteReachability(GetSiteReachabilityInput input);
}

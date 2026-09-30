package net.flex.dci.otn.controller.allocate.link.site;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;

import java.util.List;
import java.util.stream.Collectors;

public class SiteLinkRoute {
    private Link link;

    public SiteLinkRoute(Link link) {
        this.link = link;
    }

    public List<CrossConnectionAttributes> getAllXCs() {
        Site linkAttr = link.getAugmentation(Link1.class).getSite();
        List<CrossConnectionAttributes> allXCs = linkAttr.getExplictRoute().getRoute().stream().flatMap(route -> {
            List<CrossConnections> xcs = route.getPrimary().getCrossConnections();
            if (route.getSecondary() != null) {
                xcs.addAll(route.getSecondary().getCrossConnections());
            }
            if (route.getThird() != null) {
                xcs.addAll(route.getThird().stream().flatMap(x -> x.getCrossConnections().stream()).collect(Collectors.toList()));
            }
            return xcs.stream();
        }).collect(Collectors.toList());
        return allXCs;
    }
}

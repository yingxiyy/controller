package net.flex.dci.otn.controller.allocate.designer.model.tunnel;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;

import java.util.List;

@Builder
@Data
public class TunnelBindOutputData {
    @NonNull
    private List<Node> nodes;
    @NonNull
    private List<Link> links;
    @NonNull
    private List<Link> routeLinks;
    @NonNull
    private List<CrossConnections> xcs;
    @NonNull
    List<Link> siteLinks;
    @NonNull
    Link ochLink;
    @NonNull
    List<String> removedResourceIds;
    @NonNull
    String thirdSourceTp;
    @NonNull
    String thirdDestTp;
}

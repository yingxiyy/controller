package net.flex.dci.otn.controller.allocate.link.tunnel;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.allocate.common.namingrule.PhyLinkFriendlyName;
import net.flex.dci.otn.controller.allocate.designer.model.Route;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import net.flex.dci.otn.controller.allocate.link.common.LinkPhyResource;
import net.flex.dci.otn.controller.allocate.link.phy.PhyLinkUtil;
import net.flex.dci.otn.controller.allocate.node.phy.PhyNodeUtil;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class TunnelPhyResource extends LinkPhyResource {
  public TunnelPhyResource(ChangedObject changedObject) {
    super(changedObject);
  }

  //
  //based on ochLink route copy related XC, TP, Transceiver, and new OT card if necessary, internalLink
  //
  public void copyPhyResource(Tunnel tunnel, RouteInfo info) {
    List<CrossConnections> xcList = getAllRouteXCs(tunnel);
    copyXC(xcList, info.getMain());
    copyXC(xcList, info.getSlave());

    Set<String> tpList = new HashSet<>();
    for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.base.attributes.SourceTp tp : tunnel.getSourceTp()) {
      tpList.add(tp.getTpRef().getValue());
    }
    for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.base.attributes.DestinationTp tp : tunnel.getDestinationTp()) {
      tpList.add(tp.getTpRef().getValue());
    }
    mergeTp(tpList, info.getMain());
    mergeTp(tpList, info.getSlave());
    mergeTransceiver(tpList, info.getMain());
    mergeTransceiver(tpList, info.getSlave());
  }

  private List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> getAllRouteXCs(Tunnel tunnel) {
    List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> xcList = new ArrayList<>();
    for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route linkRoute : tunnel.getExplictRoute().getRoute()) {
      for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections xc : linkRoute.getPrimary().getCrossConnections()) {
        xcList.add(xc);
      }
      if (linkRoute.getSecondary() != null) {
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections xc : linkRoute.getSecondary().getCrossConnections()) {
          xcList.add(xc);
        }
      }
    }
    return xcList;
  }

}

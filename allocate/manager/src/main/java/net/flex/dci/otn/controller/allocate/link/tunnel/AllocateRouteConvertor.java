package net.flex.dci.otn.controller.allocate.link.tunnel;

import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.TunnelRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result.vendor.RouteInfos;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.Links;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.Nodes;

import java.util.ArrayList;
import java.util.List;

public class AllocateRouteConvertor {
  private RouteInfos allocateRoute;
  public AllocateRouteConvertor(RouteInfos allocateRoute) {
    this.allocateRoute = allocateRoute;
  }

  public RouteInfo convert() {
    return RouteInfo.builder()
        .main(convertRoute(allocateRoute.getMain()))
        .slave(convertRoute(allocateRoute.getSlave()))
        .build();
  }

  private net.flex.dci.otn.controller.allocate.designer.model.Route convertRoute(TunnelRoute allocateRoute) {
    if (allocateRoute == null) {
      return null;
    }
    else {
      if (allocateRoute.getNodes() == null) {
        throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "routeInfo hasn't provide node list");
      }
      if (allocateRoute.getCrossConnections() == null) {
        throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "routeInfo hasn't provide cross connection list");
      }
      return net.flex.dci.otn.controller.allocate.designer.model.Route.builder()
          .nodes(convertNode(allocateRoute.getNodes()))
          .links(convertLink(allocateRoute.getLinks()))
          .xcs(allocateRoute.getCrossConnections())
          .build();
    }
  }

  private List<Node> convertNode(List<Nodes> allocateNodes) throws CommonException {
    List<Node> nodeList = new ArrayList<>();
    for (Nodes node : allocateNodes) {
      nodeList.add(convertNode(node));
    }
    return nodeList;
  }

  private Node convertNode(Nodes node) {
    return new NodeBuilder()
        .setNodeId(node.getNodeId())
        .setKey(new NodeKey(node.getNodeId()))
        .setTerminationPoint(convertTp(node.getTerminationPoint()))
        .setSupportingNode(node.getSupportingNode())
        .addAugmentation(Node1.class, new Node1Builder()
            .setPhysical(new PhysicalBuilder(node.getPhysical()).build())
            .build())
        .build();
  }

  private List<TerminationPoint> convertTp(List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.phy.ne.full.info.TerminationPoint> allocateTpList) {
    List<TerminationPoint> tpList = new ArrayList<>();
    for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.phy.ne.full.info.TerminationPoint allocateTp : allocateTpList) {
      tpList.add(new TerminationPointBuilder()
          .setTpId(allocateTp.getTpId())
          .setKey(new TerminationPointKey(allocateTp.getTpId()))
          .addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1.class,
              new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder()
                  .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(allocateTp.getPhysical()).build())
                  .build())
          .build()
      );
    }
    return tpList;
  }

  private List<Link> convertLink(List<Links> allocateLinks) {
    List<Link> linkList = new ArrayList<>();
    if (allocateLinks == null)
      return linkList;

    for (Links link : allocateLinks) {
      linkList.add(convertLink(link));
    }
    return linkList;
  }

  public Link convertLink(Links allocateLink) {
    return new LinkBuilder()
        .setLinkId(allocateLink.getLinkId())
        .setKey(new LinkKey(allocateLink.getLinkId()))
        .setSource(allocateLink.getSource())
        .setDestination(allocateLink.getDestination())
        .setSupportingLink(allocateLink.getSupportingLink())
        .addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class,
            new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1Builder()
                .setPhysical(allocateLink.getPhysical())
                .build())
        .build();
  }

}

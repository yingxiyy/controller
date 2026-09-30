package net.flex.dci.otn.controller.nms.nms.component.route.retriever;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.ResourceType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.link.LinkHopBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.tp.tp.hop.PhyNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.tp.tp.hop.PhyTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.tp.tp.hop.SiteNodeBuilder;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class RetrieverUtil {
  private static RetrieverUtil inst = null;
  private NetconfTopology netconfTopology;

  public static RetrieverUtil getInstance() {
    if (inst == null) {
      inst = new RetrieverUtil();
    }
    return inst;
  }

  public RetrieverUtil setNetConfTopo(NetconfTopology netconfTopology) {
    this.netconfTopology = netconfTopology;
    return this;
  }

  public ResourceType getTerminationPointResourceType(String tpId) {
    if (!PhysicalTpIdNamingRule.isTpId(tpId)) {
      throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
              "the route info is invalided");
    }

    String siteId = PhysicalTpIdNamingRule.getSiteId(tpId);
    String neId = PhysicalTpIdNamingRule.getNodeId(tpId);
    String equipId = PhysicalTpIdNamingRule.getEquipId(tpId);

    Node phyNode = netconfTopology.getNeNode(neId);
    Node siteNode = netconfTopology.getSiteNode(siteId);

    Equipments refEquipments = findEquip(phyNode, equipId);
    TerminationPoint terminationPoint = getRefTerminationPoint(phyNode.getTerminationPoint(), tpId);

    SiteNodeBuilder siteNodeBuilder = new SiteNodeBuilder();
    siteNodeBuilder.fieldsFrom(siteNode);
    siteNodeBuilder.fieldsFrom(siteNode.getAugmentation(Node1.class));

    PhyTpBuilder phyTpBuilder = new PhyTpBuilder();
    phyTpBuilder.fieldsFrom(terminationPoint);
    phyTpBuilder.fieldsFrom(terminationPoint.getAugmentation(TerminationPoint1.class));
    log.debug("tp port type is :{}", phyTpBuilder.getPhysical().getPortType());

    Node drawNode = drawNode(phyNode, refEquipments, terminationPoint);
    PhyNodeBuilder phyNodeBuilder = new PhyNodeBuilder();
    phyNodeBuilder.fieldsFrom(drawNode);
    phyNodeBuilder.fieldsFrom(drawNode.getAugmentation(
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class));

    ResourceType resourceType = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.TpBuilder()
            .setTpHop(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.tp.TpHopBuilder()
                    .setSiteNode(siteNodeBuilder.build())
                    .setPhyNode(phyNodeBuilder.build())
                    .setPhyTp(phyTpBuilder.build())
                    .build())
            .build();
    return resourceType;
  }

  private Node drawNode(Node phyNode, Equipments equipments, TerminationPoint terminationPoint) {
    Physical physical = phyNode.getAugmentation(
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class)
            .getPhysical();

    List<Equipments> eqList = new LinkedList<>();
    eqList.add(equipments);

    Node1Builder node1Builder = new Node1Builder();
    node1Builder.setPhysical(
            new PhysicalBuilder(physical)
                    .setCrossConnections(null)
                    .setInternalLinks(null)
                    .setEquipments(eqList)
                    .setOCMGripGroups(null)
                    .setSystem(null)
                    .build());
    return new NodeBuilder().setNodeId(phyNode.getNodeId()).addAugmentation(
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class,
            node1Builder.build()).setTerminationPoint(
            Collections.singletonList(terminationPoint)).build();
  }

  private Equipments findEquip(Node phyNode, String equipId) {
    if (phyNode != null && phyNode.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class) != null) {
      Physical phyNodeAttr = phyNode.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class)
            .getPhysical();
      if (phyNodeAttr.getEquipments() != null)  {
        Optional<Equipments> eqOp = phyNodeAttr.getEquipments().stream().filter(eq -> eq.getEquipmentId().equals(equipId)).findFirst();
        if (eqOp.isPresent()) {
          return eqOp.get();
        }
      }
    }
    throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "cannot find required equipId " + equipId);
  }


  private TerminationPoint getRefTerminationPoint(List<TerminationPoint> terminationPointList, String tpId) {
    Optional<TerminationPoint> tpOp = terminationPointList.stream().filter(tp -> tp.getTpId().getValue().equals(tpId)).findFirst();
    if (tpOp.isPresent()) {
      return tpOp.get();
    }

    throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "cannot find required tpId " + tpId);
  }


  public ResourceType getLinkResourceType(Link phyLink) {
    LinkHopBuilder lb = new LinkHopBuilder();
    Link1 linkPhysical = phyLink.getAugmentation(Link1.class);

    lb.fieldsFrom(phyLink);
    lb.fieldsFrom(linkPhysical);

    ResourceType linkResource = new LinkBuilder().setLinkHop(lb.build()).build();
    return linkResource;
  }
}

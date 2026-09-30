package net.flex.dci.otn.controller.discovery.common.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1Builder;

@Slf4j
public class SiteNodeMachine {
  private ChangedObject changedObject;
  private String nodeId;

  public SiteNodeMachine(ChangedObject changedObject, String nodeId) {
    this.changedObject = changedObject;
    this.nodeId = nodeId;
  }

  public void markImpl() {
    log.trace("make to impl {}", nodeId);

    Node node = changedObject.getChangedSiteNode(nodeId);

    ImplementState nodeImplStatus = node.getAugmentation(Node1.class).getSite().getImplementState();
//    if (nodeImplStatus.equals(ImplementState.Implement)) {
//      return;
//    }
    Node newNode = new NodeBuilder(node).addAugmentation(Node1.class,
            new Node1Builder().setSite(new SiteBuilder(node.getAugmentation(Node1.class).getSite())
                            .setImplementState(ImplementState.Implement).setAdminState(AdminStatus.Up).build())
                    .build()).build();

    changedObject.addChangedSiteNode(newNode);
  }
}

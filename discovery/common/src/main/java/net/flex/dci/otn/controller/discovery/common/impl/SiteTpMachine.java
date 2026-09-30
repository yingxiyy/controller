package net.flex.dci.otn.controller.discovery.common.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.tp.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.tp.attributes.SiteBuilder;

import java.util.Iterator;

@Slf4j
public class SiteTpMachine {
  private ChangedObject changedObject;
  private String tpId;

  public SiteTpMachine(ChangedObject changedObject, TpId tpId) {
    this.changedObject = changedObject;
    this.tpId = tpId.getValue();
  }

  public void markImpl() {
    log.trace("make to impl {}", tpId);

    //TP of siteNode, the name is same as phyTP's
    String nodeId = PhysicalTpIdNamingRule.getSiteId(tpId);
    Node node = changedObject.getChangedSiteNode(nodeId);

    int pos = 0;
    TerminationPoint newTp = null;
    Iterator<TerminationPoint> iter = node.getTerminationPoint().iterator();
    while (iter.hasNext()) {
      TerminationPoint neTp = iter.next();
      if (neTp.getTpId().getValue().equals(tpId)) {
        Site tpSiteAttr = neTp.getAugmentation(TerminationPoint1.class).getSite();
        ImplementState implStatus = tpSiteAttr.getImplementState();
//        if (implStatus.equals(ImplementState.Implement)) {
//          //the TP has wroking in impl status
//          return;
//        }
        newTp = new TerminationPointBuilder(neTp).addAugmentation(TerminationPoint1.class,
                        new TerminationPoint1Builder().setSite(new SiteBuilder(tpSiteAttr)
                                        .setImplementState(ImplementState.Implement)
                                        .setAdminState(AdminStatus.Up)
                                        .build())
                                .build())
                .build();

        iter.remove();
        break;
      }
      pos++;
    }

    if (newTp != null) {
      node.getTerminationPoint().add(pos, newTp);
      changedObject.addChangedSiteNode(node);

      new SiteNodeMachine(changedObject, nodeId).markImpl();
    }
  }
}

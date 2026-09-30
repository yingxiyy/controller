package net.flex.dci.otn.controller.implement.tunnel.impl.frequency;

import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder;

import java.util.List;

public class PhysicalLink {
  public static void replaceLink(ChangedObject changedObject, List<String> oldLinkIdList, Frequency frequency) {
    for (String oldLinkId : oldLinkIdList) {
      Link oldLink = changedObject.getChangedPhyLink(oldLinkId);
      Physical phyLinkAttr = oldLink.getAugmentation(Link1.class).getPhysical();
      String newLinkId = frequency.replaceMuxChannelTpId(oldLinkId);
      String sTp = oldLink.getSource().getSourceTp().getValue();
      String dTp = oldLink.getDestination().getDestTp().getValue();
      Link newLink = null;
      if (frequency.hasMuxChannel(sTp)) {
        newLink = new LinkBuilder(oldLink)
            .setLinkId(new LinkId(newLinkId))
            .setKey(new LinkKey(new LinkId(newLinkId)))
            .setSource(new SourceBuilder(oldLink.getSource()).setSourceTp(new TpId(frequency.replaceMuxChannelTpId(sTp))).build())
            .addAugmentation(Link1.class, new Link1Builder()
                .setPhysical(new PhysicalBuilder(phyLinkAttr)
                    .setFriendlyName(frequency.replaceMuxChannelTpId(phyLinkAttr.getFriendlyName()))
                    .setFriendlyNameDisplay(frequency.replaceMuxChannelTpId(phyLinkAttr.getFriendlyNameDisplay()))
                    .build())
                .build())
            .build();
      } else {
        newLink = new LinkBuilder(oldLink)
            .setLinkId(new LinkId(newLinkId))
            .setKey(new LinkKey(new LinkId(newLinkId)))
            .setDestination(new DestinationBuilder(oldLink.getDestination()).setDestTp(new TpId(frequency.replaceMuxChannelTpId(dTp))).build())
            .addAugmentation(Link1.class, new Link1Builder()
                .setPhysical(new PhysicalBuilder(phyLinkAttr)
                    .setFriendlyName(frequency.replaceMuxChannelTpId(phyLinkAttr.getFriendlyName()))
                    .setFriendlyNameDisplay(frequency.replaceMuxChannelTpId(phyLinkAttr.getFriendlyNameDisplay()))
                    .build())
                .build())
            .build();
      }
      changedObject.addChangedPhyLink(newLink);
      changedObject.addRemovedPhyLink(oldLinkId);
    }
  }
}

package net.flex.dci.otn.controller.implement.common.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder;

@Slf4j
public class PhysicalLink {
    private ChangedObject changedObject;
    public PhysicalLink(ChangedObject changedObject) {
        this.changedObject = changedObject;
    }

    public static ImplementState getImplementState(Link link) {
        Physical phyLinkAttr = link.getAugmentation(Link1.class).getPhysical();
        return phyLinkAttr.getImplementState();
    }

    public static AdminStatus getAdminState(Link link) {
        Physical phyLinkAttr = link.getAugmentation(Link1.class).getPhysical();
        return phyLinkAttr.getAdminState();
    }

    private Link updateImplementState(Link link, ImplementState targetState) {
        Physical phyLinkAttr = link.getAugmentation(Link1.class).getPhysical();
        if (phyLinkAttr.getImplementState().equals(targetState) ||
                (targetState.equals(ImplementState.Doimplementing) && phyLinkAttr.getImplementState().equals(ImplementState.Implement)) ||
                (targetState.equals(ImplementState.Deimplementing) && phyLinkAttr.getImplementState().equals(ImplementState.Allocate))) {
            return null;
        }

        log.debug("update Link ImplementState {} {}", link.getLinkId().getValue(), targetState.name());
        LinkBuilder linkBuilder = new LinkBuilder(link)
                .addAugmentation(Link1.class,
                        new Link1Builder()
                                .setPhysical(new PhysicalBuilder(phyLinkAttr)
                                        .setImplementState(targetState)
                                        .setAdminState(targetState.equals(ImplementState.Implement) ? AdminStatus.Up : phyLinkAttr.getAdminState())
                                        .build())
                                .build());

        return linkBuilder.build();
    }

    public void updateImplementState(String linkId, ImplementState targetState) {
        if (linkId.endsWith("-MPO")) {
            //this is logical MPO port, should split to normal link"
            for (int i = 1; i<=8; i++) {
                String newLinkId = linkId.replaceAll("-MPO", "-MPO" + i);
                Link link = changedObject.getChangedPhyLink(newLinkId);

                Link newLink = updateImplementState(link, targetState);
                if (newLink != null) {
                    changedObject.addChangedPhyLink(newLink);
                }
            }
        } else {
            Link link = changedObject.getChangedPhyLink(linkId);

            Link newLink = updateImplementState(link, targetState);
            if (newLink != null) {
                changedObject.addChangedPhyLink(newLink);
            }
        }
    }
}

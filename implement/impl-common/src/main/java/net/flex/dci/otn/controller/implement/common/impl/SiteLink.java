package net.flex.dci.otn.controller.implement.common.impl;

import lombok.extern.slf4j.Slf4j;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;

@Slf4j
public class SiteLink {


    public static ImplementState getImplementState(Link link) {
        Site siteLinkAttr = link.getAugmentation(Link1.class).getSite();
        return siteLinkAttr.getImplementState();
    }

    public static AdminStatus getAdminState(Link link) {
        Site siteLinkAttr = link.getAugmentation(Link1.class).getSite();
        return siteLinkAttr.getAdminState();
    }
    public static Link updateImplementState(Link link, ImplementState targetState) {
        Site siteLinkAttr = link.getAugmentation(Link1.class).getSite();
        if (siteLinkAttr.getImplementState().equals(targetState) ||
                (targetState.equals(ImplementState.Doimplementing) && siteLinkAttr.getImplementState().equals(ImplementState.Implement)) ||
                (targetState.equals(ImplementState.Deimplementing) && siteLinkAttr.getImplementState().equals(ImplementState.Allocate))) {
            return null;
        }

        log.debug("update Link ImplementState {} {}", link.getLinkId().getValue(), targetState.name());
        LinkBuilder linkBuilder = new LinkBuilder(link)
                .addAugmentation(Link1.class,
                        new Link1Builder()
                                .setSite(new SiteBuilder(siteLinkAttr)
                                        .setImplementState(targetState)
                                        .setAdminState(targetState.equals(ImplementState.Implement) ? AdminStatus.Up : siteLinkAttr.getAdminState())
                                        .build())
                                .build());

        return linkBuilder.build();
    }
}

package net.flex.dci.otn.controller.implement.common.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.FrequencyScope;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.OchBuilder;

import java.util.List;

@Slf4j
public class OchLink {
    private Link link;

    public OchLink(Link link) {
        this.link = link;
    }

    public Link updateImplementState(ImplementState targetState) {
        Och ochLinkAttr = link.getAugmentation(Link1.class).getOch();
        if (ochLinkAttr.getImplementState().equals(targetState) ||
                (targetState.equals(ImplementState.Doimplementing) && ochLinkAttr.getImplementState().equals(ImplementState.Implement)) ||
                (targetState.equals(ImplementState.Deimplementing) && ochLinkAttr.getImplementState().equals(ImplementState.Allocate))) {
            return null;
        }
        log.debug("update Link ImplementState {} {}", link.getLinkId().getValue(), targetState.name());
        LinkBuilder linkBuilder = new LinkBuilder(link)
                .addAugmentation(Link1.class,
                        new Link1Builder()
                                .setOch(new OchBuilder(ochLinkAttr)
                                        .setImplementState(targetState)
                                        .setAdminState(targetState.equals(ImplementState.Implement) ? AdminStatus.Up : ochLinkAttr.getAdminState())
                                        .build())
                                .build());

        link = linkBuilder.build();

        return link;
    }
}


/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.ne;

import static net.flex.dci.otn.controller.allocate.designer.namingrule.NEIdGenerator.getCurrentTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.namingrule.NEIdGenerator;
import net.flex.dci.otn.controller.allocate.designer.namingrule.NameGenerator;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinksBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.topology.type.SiteTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.springframework.stereotype.Service;

@Service
public class LinkRepo {

    public Link createLink(String fromTpId, String toTpId, LinkType linkType, List<String> supportedLinkIds, LinkDirection linkDirection) throws NeDesignerException {
        Source source = new SourceBuilder()
                .setSourceTp(new TpId(fromTpId))
                .setSourceNode(new NodeId(NEIdGenerator.getNodeIdByTpId(fromTpId)))
                .build();
        Destination destination = new DestinationBuilder()
                .setDestTp(new TpId(toTpId))
                .setDestNode(new NodeId(NEIdGenerator.getNodeIdByTpId(toTpId)))
                .build();

        List<SupportedLink> supportedLinks = new ArrayList<SupportedLink>();

        for (String supportedLinkId : supportedLinkIds) {
            LinkId linkId = new LinkId(supportedLinkId);
            SupportedLink supportLink = new SupportedLinkBuilder()
                    .setKey(new SupportedLinkKey(linkId,
                            new TopologyId(SiteTopology.QNAME.getLocalName())))
                    .setLinkRef(linkId)
                    .setTopologyRef(new TopologyId(SiteTopology.QNAME.getLocalName()))
                    .build();
            supportedLinks.add(supportLink);
        }

        //Link
        String friendlyName = NameGenerator.createLinkFriendlyName(linkType);
        Link1 link1 = new Link1Builder()
                .setPhysical(
                        new PhysicalBuilder()
                                .setAdminState(AdminStatus.Unknown)
                                .setAlarmState(AlarmSeverity.Unknown)
                                .setAlignmentStatus(AlignmentStatusType.Unknown)
                                .setCreationTime(getCurrentTime())
                                .setFriendlyName(friendlyName)
                                .setFriendlyNameDisplay(friendlyName)
                                .setImplementState(ImplementState.Allocate)
                                .setLinkType(linkType)
                                .setOperationalState(OperStatus.Unknown)
                                .setSupportedLink(supportedLinks)
                                .setDirection(linkDirection)
                                .setProperties(NameGenerator.getFakeProperty())
                                .build())
                .build();

        LinkId linkId = new LinkId(NEIdGenerator.createLinkId(fromTpId, toTpId, linkType));
        return new LinkBuilder().setLinkId(linkId)
                .setKey(new LinkKey(linkId))
                .setSource(source)
                .setDestination(destination)
                .addAugmentation(Link1.class, link1)
                .build();
    }

    public Link createLink(String fromTpId, String toTpId, LinkType linkType, List<String> supportedLinkIds) throws NeDesignerException {
        return createLink(fromTpId, toTpId, linkType, supportedLinkIds, LinkDirection.Bidirection);
    }

    public Link createLink(String fromTpId, String toTpId, LinkType linkType) throws NeDesignerException {
        return createLink(fromTpId, toTpId, linkType, Collections.EMPTY_LIST);
    }


    public InternalLinks createInternalLink(String nodeId, Link link) {
        LinkDirection direction = link.getAugmentation(Link1.class).getPhysical().getDirection();
        String srcTp;
        String destTp;

        if (direction.equals(LinkDirection.Unidirection)) {
            srcTp = link.getSource().getSourceTp().getValue();
            destTp = link.getDestination().getDestTp().getValue();
        } else {
            if (link.getSource().getSourceNode().getValue().equals(nodeId)) {
                srcTp = link.getSource().getSourceTp().getValue();
                destTp = link.getDestination().getDestTp().getValue();
            } else {
                srcTp = link.getDestination().getDestTp().getValue();
                destTp = link.getSource().getSourceTp().getValue();
            }
        }
        return new InternalLinksBuilder()
                .setDstTp(destTp)
//                .setKey(PhysicalLinkIdNamingRule.createInternalLinkKey(link.getLinkId().getValue()))
                .setKey(PhysicalLinkIdNamingRule.createInternalLinkKey(getInternalLinkFriendlyName(nodeId, srcTp, destTp)))
//                .setKey(new InternalLinksKey(link.getAugmentation(Link1.class).getPhysical().getFriendlyName()))
                .setLinkName(link.getAugmentation(Link1.class).getPhysical().getFriendlyName())
                .setLinkType(link.getAugmentation(Link1.class).getPhysical().getLinkType())
                .setLinkRef(link.getLinkId().getValue())
                .setImplementState(ImplementState.Allocate)
                .setSrcTp(srcTp)
                .setProperties(NameGenerator.getFakeProperty())
                .setDirection(direction)
                .build();
    }

    private String getInternalLinkFriendlyName(String nodeId, String srcTp, String destTp) {
        String srcNodeId = PhysicalTpIdNamingRule.getNodeId(srcTp);
        String dstNodeId = PhysicalTpIdNamingRule.getNodeId(destTp);
        if (srcNodeId.equals(dstNodeId)) {
            return PhysicalTpIdNamingRule.getPurePortNameByTpId(srcTp) + "#" + PhysicalTpIdNamingRule.getPurePortNameByTpId(destTp);
        } else if (nodeId.equals(srcNodeId)) {
            return PhysicalTpIdNamingRule.getPurePortNameByTpId(srcTp) + "#EXT:" + PhysicalTpIdNamingRule.getPurePortNameByTpId(destTp);
        } else {
            return "EXT:" + PhysicalTpIdNamingRule.getPurePortNameByTpId(srcTp) + "#" + PhysicalTpIdNamingRule.getPurePortNameByTpId(destTp);
        }
    }
}

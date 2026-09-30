/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.common.namingrule;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
@Component
public class PhyLinkFriendlyName {

    //siteSrc名称-siteDst名称-时间戳

    private String createFriendlyName(String srcNodeName, String dstNodeName, String srcTpName, String dstTpName) {
        String friendlyName = "";
        if (srcNodeName.equals(dstNodeName)) {
            friendlyName = srcNodeName;
            friendlyName = srcNodeName + "#" + srcTpName + "--" + dstTpName;
        } else {
            friendlyName = srcNodeName + "#" + srcTpName + "--" + dstNodeName + "#" +dstTpName;;
        }
        return friendlyName;
    }

    public Link updateFriendlyName(Link link, Node srcNode, Node dstNode) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical phyLinkAttr = link.getAugmentation(Link1.class).getPhysical();
//    if (phyLinkAttr.getLinkType().equals(LinkType.CableLink))
//      continue;

        try {
            LinkBuilder lb = new LinkBuilder(link);
            PhysicalBuilder pb = new PhysicalBuilder(phyLinkAttr);
            String srcNodeFriendlyName = srcNode.getAugmentation(Node1.class).getPhysical().getFriendlyName();
            String destNodeFriendlyName = dstNode.getAugmentation(Node1.class).getPhysical().getFriendlyName();

            pb.setFriendlyName(createFriendlyName(srcNodeFriendlyName, destNodeFriendlyName,
              getPortName(srcNode, link.getSource().getSourceTp()),
              getPortName(dstNode, link.getDestination().getDestTp())));
            pb.setFriendlyNameDisplay(pb.getFriendlyName());

            Link newLink = lb.addAugmentation(
              Link1.class,
              new Link1Builder().setPhysical(pb.build()).build()
            ).build();
            return newLink;
        } catch (NullPointerException e) {
            log.error("find null point on " + phyLinkAttr.getFriendlyName() + ", " + link.getLinkId(), e);
            throw e;
        }
    }

    private String getPortName(Node node, TpId tpId) {
        for (TerminationPoint tp : node.getTerminationPoint()) {
            if (tp.getTpId().getValue().equals(tpId.getValue())) {
                Physical tpAttr = tp.getAugmentation(TerminationPoint1.class).getPhysical();
                return tpAttr.getFriendlyName();
            }
        }
        return tpId.getValue();
    }
}

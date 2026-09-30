/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.site;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.allocate.link.common.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
public class ScanLink {

    ChangedObject changedObject;
    private PhyLinkDao phyLinkDao;

    public ScanLink(ChangedObject changedObject) {
        this.changedObject = changedObject;
        phyLinkDao = SpringBeanFinder.getBean(PhyLinkDao.class);
    }
    public void remove(Link removedSiteLink) {
        List<SupportingLink> otsLinkList = removedSiteLink.getSupportingLink().stream()
                .filter(x -> PhysicalLinkIdNamingRule.isOtsLink(x.getLinkRef().getValue()))
                .collect(Collectors.toList());
        List<String> otsLinkIdList = otsLinkList.stream().map(x -> x.getLinkRef().getValue()).collect(Collectors.toList());
        List<String> oaEqIdList = new ArrayList<>();
        for (String otsLinkId : otsLinkIdList) {
            String aTp = PhysicalLinkIdNamingRule.getTpAId(otsLinkId);
            String zTp = PhysicalLinkIdNamingRule.getTpZId(otsLinkId);
            oaEqIdList.add(PhysicalTpIdNamingRule.getEquipId(aTp));
            oaEqIdList.add(PhysicalTpIdNamingRule.getEquipId(zTp));
        }

        List<Link> scanLinkList = phyLinkDao.getScanLinks();
        for (Link link : scanLinkList) {
            String aTp = PhysicalLinkIdNamingRule.getTpAId(link.getLinkId().getValue());
            String zTp = PhysicalLinkIdNamingRule.getTpZId(link.getLinkId().getValue());
            if (oaEqIdList.contains(aTp) || oaEqIdList.contains(zTp)) {
                changedObject.addRemovedPhyLink(link.getLinkId().getValue());
//                makeTpFree(PhysicalTpIdNamingRule.getNodeId(aTp), aTp);
            }
        }
    }

    private void makeTpFree(String nodeId, String tpId) {
        Node node = changedObject.getChangedPhyNode(nodeId);
        List<TerminationPoint> newTpList = new ArrayList<>();

        Iterator<TerminationPoint> iter = node.getTerminationPoint().iterator();
        while (iter.hasNext()) {
            TerminationPoint tp = iter.next();
            if (tp.getTpId().getValue().equals(tpId)) {
                iter.remove();
                Physical tpAttr = tp.getAugmentation(TerminationPoint1.class).getPhysical();
                TerminationPoint newTp = new TerminationPointBuilder(tp)
                        .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                                .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(tpAttr)
                                        .setConnectionStatus(ConnectionStatus.Idle)
                                        .build())
                                .build())
                        .build();
                newTpList.add(newTp);
            } else {
                newTpList.add(tp);
            }
        }
        Node newNode = new NodeBuilder(node).setTerminationPoint(newTpList).build();
        changedObject.addChangedPhyNode(newNode);
    }


}

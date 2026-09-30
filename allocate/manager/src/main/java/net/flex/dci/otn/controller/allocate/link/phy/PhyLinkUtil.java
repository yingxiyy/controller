/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.phy;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.ViewLinkDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.allocate.common.namingrule.PhyLinkFriendlyName;
import net.flex.dci.otn.controller.allocate.designer.ne.LinkRepo;
import net.flex.dci.otn.controller.allocate.node.phy.PhyNodeRemover;
import net.flex.dci.otn.controller.allocate.node.phy.PhyNodeUtil;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author YYX
 * @date 12/7/2021
 */
@Slf4j
public class PhyLinkUtil {
    private ChangedObject changedObject;

    private Node srcNode;
    private Node dstNode;

    public Node getSrcNode() {
        return srcNode;
    }

    public Node getDstNode() {
        return dstNode;
    }

    public PhyLinkUtil(ChangedObject changedObject) {
        this.changedObject = changedObject;
    }

    /**
     * remove phylink, and related viewLink
     * make the TP as free of the phyLink
     * and remove the node related internalLink
     * @param linkId
     * @param planeId
     * @return
     */
    public PhyLinkUtil removePhyLink(String linkId, String planeId) {
        return removePhyLink(linkId, planeId, false);
    }

    public PhyLinkUtil removePhyLinkKeepXc(String linkId, String planeId) {
        return removePhyLink(linkId, planeId, true);
    }

    private PhyLinkUtil removePhyLink(String linkId, String planeId, boolean keepXc) {
        log.debug("remove phy link {}", linkId);

        Link removedPhyLink = changedObject.getChangedPhyLink(linkId);
        if (removedPhyLink == null) {
            log.error("phy link {} not exist in database, has removed", linkId);
            return this;
        }

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical phyLinkAttr =
                removedPhyLink.getAugmentation(Link1.class).getPhysical();

        if (phyLinkAttr.getLinkType().equals(LinkType.OtsLink) || phyLinkAttr.getLinkType().equals(LinkType.OsLink)) {
            removeRelatedViewLinks(removedPhyLink);
        }
        changedObject.addRemovedPhyLink(linkId);
        String srcTpId = PhysicalLinkIdNamingRule.getTpAId(linkId);
        String dstTpId = PhysicalLinkIdNamingRule.getTpZId(linkId);
        String srcNodeId = PhysicalTpIdNamingRule.getNodeId(srcTpId);
        String dstNodeId = PhysicalTpIdNamingRule.getNodeId(dstTpId);

        srcNode = changedObject.getChangedPhyNode(srcNodeId);
        if (srcNode != null) {
            srcNode = keepXc ? PhyNodeUtil.cleanTpStateOnly(srcNode, srcTpId)
                    : PhyNodeUtil.cleanTpAttr(srcNode, srcTpId);
            changedObject.addChangedPhyNode(srcNode);
        }

        dstNode = changedObject.getChangedPhyNode(dstNodeId);
        if (dstNode != null) {
            dstNode = keepXc ? PhyNodeUtil.cleanTpStateOnly(dstNode, dstTpId)
                    : PhyNodeUtil.cleanTpAttr(dstNode, dstTpId);
            changedObject.addChangedPhyNode(dstNode);
        }

        removeOldInternalLinks(linkId);
        return this;
    }

    /**
     * A physical link can be mapped into ViewLinks of multiple sibling subnets.
     * 删除的时候直接通过viewLink 看它和phyLink的关系，然后全部相关的删除.
     */
    private void removeRelatedViewLinks(Link removedPhyLink) {
        String linkId = removedPhyLink.getLinkId().getValue();
        ViewLinkDao viewLinkDao = SpringBeanFinder.getBean(ViewLinkDao.class);

        List<Link> viewLinks = viewLinkDao.getViewLinkBySupportingLinks(Collections.singletonList(linkId));

        viewLinks.forEach(viewLink -> changedObject.addRemovedViewLink(viewLink));

        log.debug("remove view links based on phyLink {}", viewLinks.stream().map(x->x.getLinkId().getValue()));
    }


    private void removeOldInternalLinks(String phyLinkId) {
        String aNodeId = PhysicalLinkIdNamingRule.getNodeAId(phyLinkId);
        String zNodeId = PhysicalLinkIdNamingRule.getNodeZId(phyLinkId);
        PhyNodeRemover phyNodeRemover = new PhyNodeRemover(changedObject);

        // PhyLinkUtil.cleanTpAttr 会按 TP 清理 internal-link；这里再按 linkRef 精确清理一次，
        // 保证旧 A--B internal-link 不会和后续 A--N/N--B internal-link 共存。
        phyNodeRemover.removeInternalLink(aNodeId, phyLinkId);
        phyNodeRemover.removeInternalLink(zNodeId, phyLinkId);
    }

    public PhyLinkUtil addPhyLink(Link link, String planeName, String planeId) {

        String srcNodeId = link.getSource().getSourceNode().getValue();
        String dstNodeId = link.getDestination().getDestNode().getValue();


        srcNode = changedObject.getChangedPhyNode(srcNodeId);
        dstNode = changedObject.getChangedPhyNode(dstNodeId);

        Link newLink = new LinkBuilder(link)
                .addAugmentation(Link1.class, new Link1Builder()
                        .setPhysical(new PhysicalBuilder(link.getAugmentation(Link1.class).getPhysical())
                                .setPlaneName(planeName)
                                .setPlaneId(planeId)
                                .build())
                        .build())
                .build();

        PhyLinkFriendlyName phyLinkFriendlyNameGenerator = SpringBeanFinder.getBean(PhyLinkFriendlyName.class);
        newLink = phyLinkFriendlyNameGenerator.updateFriendlyName(newLink, srcNode, dstNode);

        changedObject.addChangedPhyLink(newLink);

        return this;
    }


    //call when create wssLink, should
    //   insert internalLink in relatedNode
    //   related TP set busy
    public void addPhyLinkAddtional(ChangedObject changedObject, Link wssLink) {
        String srcNodeId = wssLink.getSource().getSourceNode().getValue();
        String dstNodeId = wssLink.getDestination().getDestNode().getValue();

        updateInternalLink(srcNodeId, wssLink);
        updateInternalLink(dstNodeId, wssLink);
    }

    private void updateInternalLink(String nodeId, Link wssLink) {
        Node node = changedObject.getChangedPhyNode(nodeId);
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

        String linkId = wssLink.getLinkId().getValue();
        InternalLinks il = nodeAttr.getInternalLinks().stream()
                .filter(x -> x.getLinkRef().equals(linkId))
                .findAny().orElse(null);


        String aTp = PhysicalLinkIdNamingRule.getTpAId(linkId);
        String zTp = PhysicalLinkIdNamingRule.getTpZId(linkId);

        if (il == null) {
            log.debug("no wssLink existed in node, insert as internalLink");
            LinkRepo repo = new LinkRepo();
            il = repo.createInternalLink(nodeId, wssLink);

            List<InternalLinks> newIlList = new ArrayList<>(nodeAttr.getInternalLinks());
            newIlList.add(il);

            Node newNode = new NodeBuilder(node).addAugmentation(Node1.class, new Node1Builder().setPhysical(
                                    new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder(nodeAttr)
                                            .setInternalLinks(newIlList).build())
                            .build())
                    .build();
            changedObject.addChangedPhyNode(newNode);
        }

        updatePhyTpConnectionStatus(nodeId, aTp, ConnectionStatus.Busy);
        updatePhyTpConnectionStatus(nodeId, zTp, ConnectionStatus.Busy);
    }

    private void updatePhyTpConnectionStatus(String nodeId, String tpId, ConnectionStatus connectionStatus) {
        Node node = changedObject.getChangedPhyNode(nodeId);
        node = PhyNodeUtil.updateTpConnectionStatus(node, tpId, connectionStatus);
        changedObject.addChangedPhyNode(node);
    }

}

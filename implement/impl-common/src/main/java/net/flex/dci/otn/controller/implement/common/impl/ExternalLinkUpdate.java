package net.flex.dci.otn.controller.implement.common.impl;

import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinksBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 外部连接原始数据时左右两边的ID，不方便，替换成IP
 */
public class ExternalLinkUpdate {
    ChangedObject changedObject;
    RouteInfo rInfo;


    public ExternalLinkUpdate(ChangedObject changedObject, RouteInfo rInfo) {
        this.rInfo = rInfo;
        this.changedObject = changedObject;
    }


    public void start() {
        rInfo.getPhyLinkIdList().forEach(linkId->{
            String srcTpId = PhysicalLinkIdNamingRule.getTpAId(linkId);
            String dstTpId = PhysicalLinkIdNamingRule.getTpZId(linkId);

            String srcNodeId = PhysicalLinkIdNamingRule.getNodeAId(linkId);
            String dstNodeId = PhysicalLinkIdNamingRule.getNodeAId(linkId);
            if (dstNodeId.equals(srcNodeId)) {
                return;
            }

            Node srcNode = changedObject.getChangedPhyNode(srcNodeId);
            Node dstNode = changedObject.getChangedPhyNode(dstNodeId);

            String srcIp = getIp(srcNode);
            String dstIp = getIp(dstNode);

            updateExternalLink(srcNode, linkId, srcTpId, dstTpId, dstIp);
            updateExternalLink(dstNode, linkId, srcTpId, dstTpId, srcIp);
        });
    }

    private void updateExternalLink(Node node, String linkId, String srcTpId, String dstTpId, String dstIp) {
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        Optional<InternalLinks> ilOp = nodeAttr.getInternalLinks().stream()
                .filter(il -> (il.getSrcTp().equals(srcTpId) && il.getDstTp().equals(dstTpId)) ||
                        (il.getSrcTp().equals(dstTpId) && il.getDstTp().equals(srcTpId)))
                .findAny();
        if (!ilOp.isPresent()) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "Impossible, cannot find out related internalLink with phyLink. " + linkId);
        }
        InternalLinks il = ilOp.get();
        InternalLinks newIL = new InternalLinksBuilder(il).setLinkName(il.getLinkName().replace("EXT", dstIp)).build();

        List<InternalLinks> newIlLink = new ArrayList<>(nodeAttr.getInternalLinks());
        newIlLink.add(newIL);
        Node newNode = new NodeBuilder(node).addAugmentation(Node1.class, new Node1Builder()
                .setPhysical(new PhysicalBuilder(nodeAttr)
                        .setInternalLinks(newIlLink)
                        .build())
                        .build())
                .build();
        changedObject.addChangedPhyNode(newNode);
    }

    private String getIp(Node node) {
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        if (nodeAttr.getIp() == null || nodeAttr.getIp().isEmpty()) {
            return "EXT";
        } else {
            return nodeAttr.getIp();
        }
    }
}

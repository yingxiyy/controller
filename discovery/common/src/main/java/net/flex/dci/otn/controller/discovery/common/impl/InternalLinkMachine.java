package net.flex.dci.otn.controller.discovery.common.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinksBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
public class InternalLinkMachine {
    private Node cfgNode;
    private Node opNode;

    public InternalLinkMachine(Node cfgNode, Node opNode) {
        this.cfgNode = cfgNode;
        this.opNode = opNode;
    }

    public Node start() {
        log.trace("make internalLink to impl {}", cfgNode.getNodeId().getValue());

        List<InternalLinks> cfgItlLinks = cfgNode.getAugmentation(Node1.class).getPhysical().getInternalLinks();
        List<InternalLinks> opItlLinks = opNode.getAugmentation(Node1.class).getPhysical().getInternalLinks();
        List<InternalLinks> newInternalLinkList = new ArrayList<>();

        Iterator<InternalLinks> iter = cfgItlLinks.iterator();
        while (iter.hasNext()) {
            InternalLinks cfgLink = iter.next();
            String cfgSrcNodeId = PhysicalTpIdNamingRule.getNodeId(cfgLink.getSrcTp());
            String cfgDstNodeId = PhysicalTpIdNamingRule.getNodeId(cfgLink.getDstTp());

            InternalLinks newInternalLink = null;
            for (InternalLinks opLink : opItlLinks) {
                if (cfgLink.getSrcTp().equals(opLink.getSrcTp())) {
                    if (cfgLink.getDstTp().equals(opLink.getDstTp())) {
                        newInternalLink = makeImpl(cfgLink, opLink);
                        break;
                    } else {
                        if (!cfgSrcNodeId.equals(cfgDstNodeId)) {
                            //this is external link on NE, cannot distinguish the peer node
                            String cfgDstPortId = PhysicalTpIdNamingRule.getPurePortNameByTpId(cfgLink.getDstTp());
                            String opDstPortId = PhysicalTpIdNamingRule.getPurePortNameByTpId(opLink.getDstTp());

                            if (cfgDstPortId.equals(opDstPortId)) {
                                newInternalLink = makeImpl(cfgLink, opLink);
                            } else {
                                log.error("cannot matching internalLink {} src is same {} and dst on {}, {}",
                                        cfgLink.getLinkName(),
                                        cfgLink.getSrcTp(), cfgDstPortId, opDstPortId);
                            }
                            break;
                        }
                    }
                } else if (cfgLink.getSrcTp().equals(opLink.getDstTp())) {
                    if (cfgLink.getDstTp().equals(opLink.getSrcTp())) {
                        newInternalLink = makeImpl(cfgLink, opLink);
                        break;
                    } else {
                        if (!cfgSrcNodeId.equals(cfgDstNodeId)) {
                            //this is external link on NE, cannot distinguish the peer node
                            String cfgDstPortId = PhysicalTpIdNamingRule.getPurePortNameByTpId(cfgLink.getDstTp());
                            String opSrcPortId = PhysicalTpIdNamingRule.getPurePortNameByTpId(opLink.getSrcTp());

                            if (cfgDstPortId.equals(opSrcPortId)) {
                                newInternalLink = makeImpl(cfgLink, opLink);
                            } else {
                                log.error("cannot matching internalLink {} src is dst {} and peer is {}, {}",
                                        cfgLink.getLinkName(),
                                        cfgLink.getSrcTp(), cfgDstPortId, opSrcPortId);
                            }
                            break;
                        }
                    }
                }
            }
            if (newInternalLink != null) {
                iter.remove(); //the old cfg Internal link will be replaced with new.
                newInternalLinkList.add(newInternalLink);
            } else {
                log.debug("hasn't found matching internalLink on real NE {}", cfgLink.getLinkName());
            }
        }
        newInternalLinkList.addAll(cfgItlLinks);
        return newCfgNode(cfgNode, newInternalLinkList);
    }
    
    private InternalLinks makeImpl(InternalLinks cfgLink, InternalLinks opLink) {
        return new InternalLinksBuilder(cfgLink)
                .setLinkName(opLink.getLinkName())
                .setKey(opLink.getKey())
                .setAdminState(AdminStatus.Up)
                .setImplementState(ImplementState.Implement)
                .build();
    }
    
    private Node newCfgNode(Node cfgNode, List<InternalLinks> newInternalLinkList) {
        return new NodeBuilder(cfgNode)
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder(cfgNode.getAugmentation(Node1.class).getPhysical())
                                .setInternalLinks(newInternalLinkList)
                                .build())
                        .build())
                .build();
    }

}

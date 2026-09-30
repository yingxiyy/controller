/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.tunnel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dto.OchFreeSlotDao;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.config.NEInfoConfig;
import net.flex.dci.otn.controller.allocate.ne.Card;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OduGranularity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.GetFreeResourceInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.GetFreeResourceOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.GetFreeResourceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.get.free.resource.output.Nodes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.get.free.resource.output.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.get.free.resource.output.NodesKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.get.free.resource.output.nodes.ReuseOchLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.get.free.resource.output.nodes.ReuseOchLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.get.free.resource.output.nodes.ReuseOchLinkKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FreeResourceQuery {

    @Autowired
    private PhyNodeDao phyNodeDao;

    @Autowired
    private OchLinkDao ochLinkDao;

    @Autowired
    private NEInfoConfig neInfoConfig;

    public GetFreeResourceOutput doIt(GetFreeResourceInput input) {
        validate(input);

        int cardWidth = getCardWidth(input);
        log.info(
                "query free resource, siteNodeId:{}, vendorName:{}, productType:{}, nodeType:{}, cardType:{}, planeId:{}, cardWidth:{}",
                input.getSiteNodeId(), input.getVendorName(), input.getProductType(),
                input.getNodeType(), input.getCardType(), input.getPlaneId(), cardWidth);

        // First query candidate TD nodes with any empty LINECARD slot from DB, then apply the
        // card-width check in memory because width comes from the loaded card JSON definition.
        List<Node> nodesWithFreeSlots = phyNodeDao.queryFreeNode(input.getSiteNodeId(),
                input.getNodeType().name(), input.getVendorName(), input.getProductType(),
                input.getCardType(), input.getPlaneId()).stream()
                .filter(node -> hasEnoughContinuousEmptySlots(node, cardWidth))
                .collect(Collectors.toList());

        Map<String, Node> outputNodes = new HashMap<>();
        nodesWithFreeSlots.forEach(node -> outputNodes.put(node.getNodeId().getValue(), node));

        // Reused OCH availability is independent from empty slots. A TD node with no empty slot
        // still needs to be returned if an existing OCH link has available ODU slots.
        Map<String, List<ReuseOchLink>> reuseOchLinksByNode = new HashMap<>();
//        List<Node> sameSiteNodes = phyNodeDao.listConfigPhyNodeBySiteNodeId(input.getSiteNodeId());
//        for (Node node : sameSiteNodes) {
//            if (!matchNode(input, node)) {
//                continue;
//            }
//            List<ReuseOchLink> reuseOchLinks = getReuseOchLinks(input, node);
//            if (!reuseOchLinks.isEmpty()) {
//                outputNodes.put(node.getNodeId().getValue(), node);
//                reuseOchLinksByNode.put(node.getNodeId().getValue(), reuseOchLinks);
//            }
//        }

        List<Nodes> nodes = outputNodes.values().stream()
                .map(node -> toOutputNode(node,
                        reuseOchLinksByNode.getOrDefault(node.getNodeId().getValue(),
                        Collections.emptyList())))
                .sorted(Comparator.comparing(node -> node.getNodeId().getValue()))
                .collect(Collectors.toList());

        log.info("query free resource done, free-slot node count:{}, output node count:{}",
                nodesWithFreeSlots.size(), nodes.size());
        return new GetFreeResourceOutputBuilder().setNodes(nodes).build();
    }

    private void validate(GetFreeResourceInput input) {
        if (input.getNodeType() != NodeType.TD) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "get-free-resource only supports TD node-type");
        }
        if (input.getSiteNodeId() == null || input.getVendorName() == null
                || input.getProductType() == null || input.getCardType() == null
                || input.getPlaneId() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "site-node-id, vendor-name, product-type, card-type, plane-id are mandatory");
        }
    }

    private int getCardWidth(GetFreeResourceInput input) {
        try {
            Card card = neInfoConfig.getNeInfo(input.getVendorName(), input.getProductType(),
                    input.getNodeType().name()).getCardByCardType(input.getCardType());
            log.debug("matched card definition, cardType:{}, width:{}", input.getCardType(),
                    card.getWidth());
            return card.getWidth();
        } catch (NeDesignerException e) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, e.getMessage(), e);
        }
    }

    private boolean matchNode(GetFreeResourceInput input, Node node) {
        Node1 node1 = node.getAugmentation(Node1.class);
        if (node1 == null || node1.getPhysical() == null) {
            return false;
        }
        return Objects.equals(input.getVendorName(), node1.getPhysical().getVendorName())
                && Objects.equals(input.getProductType(), node1.getPhysical().getVendorType())
                && input.getNodeType().equals(node1.getPhysical().getNodeType())
                && (input.getPlaneId() == null
                || Objects.equals(input.getPlaneId(), node1.getPhysical().getPlaneId()));
    }

    private boolean hasEnoughContinuousEmptySlots(Node node, int width) {
        List<Integer> emptySlots = getEmptyLineCardSlots(node);
        if (emptySlots.isEmpty()) {
            log.debug("node {} has no empty LINECARD slots", node.getNodeId().getValue());
            return false;
        }
        Set<Integer> emptySlotSet = new HashSet<>(emptySlots);
        for (Integer slot : emptySlots) {
            boolean enough = true;
            for (int offset = 0; offset < width; offset++) {
                if (!emptySlotSet.contains(slot + offset)) {
                    enough = false;
                    break;
                }
            }
            if (enough) {
                log.debug("node {} has enough continuous empty slots from slot {}, width:{}",
                        node.getNodeId().getValue(), slot, width);
                return true;
            }
        }
        log.debug("node {} empty LINECARD slots {} cannot satisfy card width {}",
                node.getNodeId().getValue(), emptySlots, width);
        return false;
    }

    private List<Integer> getEmptyLineCardSlots(Node node) {
        Node1 node1 = node.getAugmentation(Node1.class);
        if (node1 == null || node1.getPhysical() == null
                || node1.getPhysical().getEquipments() == null) {
            return Collections.emptyList();
        }
        return node1.getPhysical().getEquipments().stream()
                .filter(equipment -> Boolean.TRUE.equals(equipment.isEmpty()))
                .filter(equipment -> equipment.getEquipmentId() != null
                        && equipment.getEquipmentId().contains("LINECARD"))
                .map(Equipments::getSlot)
                .filter(Objects::nonNull)
                .map(Integer::parseInt)
                .sorted()
                .collect(Collectors.toList());
    }

    private List<ReuseOchLink> getReuseOchLinks(GetFreeResourceInput input, Node node) {
        // The DAO filters OCH links by this TD endpoint, card type, ODU granularity and
        // vendor/product, matching the user-defined reused-och-link selection requirements.
        List<OchFreeSlotDao> freeSlots = ochLinkDao.quereFreeLinkEndWithNode(
                node.getNodeId().getValue(), input.getCardType(),
                toDbOdu(input.getSlotGranularity()), toDbOdu(input.getOduType()),
                input.getVendorName(), input.getProductType());
        if (freeSlots == null || freeSlots.isEmpty()) {
            log.debug("node {} has no reusable OCH links", node.getNodeId().getValue());
            return Collections.emptyList();
        }
        log.debug("node {} reusable OCH link count:{}", node.getNodeId().getValue(),
                freeSlots.size());
        return freeSlots.stream()
                .map(slot -> new ReuseOchLinkBuilder()
                        .setOchLinkId(slot.getId())
                        .setKey(new ReuseOchLinkKey(slot.getId()))
                        .setAvailable((byte) countAvailableSlots(slot.getAvailableSlots()))
                        .build())
                .collect(Collectors.toList());
    }

    private String toDbOdu(OduGranularity oduGranularity) {
        return oduGranularity.name().toLowerCase();
    }

    private int countAvailableSlots(String availableSlots) {
        if (availableSlots == null || availableSlots.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String segment : availableSlots.split(",")) {
            String trimmed = segment.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.contains("-")) {
                String[] range = trimmed.split("-");
                count += Integer.parseInt(range[1]) - Integer.parseInt(range[0]) + 1;
            } else {
                count++;
            }
        }
        return count;
    }

    private Nodes toOutputNode(Node node, List<ReuseOchLink> reuseOchLinks) {
        return new NodesBuilder()
                .setNodeId(node.getNodeId())
                .setKey(new NodesKey(node.getNodeId()))
                .setPhysical(node.getAugmentation(Node1.class).getPhysical())
                .setTerminationPoint(toOutputTps(node.getTerminationPoint()))
                .setReuseOchLink(reuseOchLinks)
                .build();
    }

    private List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.phy.ne.full.info.TerminationPoint> toOutputTps(
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint> tps) {
        if (tps == null) {
            return null;
        }
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.phy.ne.full.info.TerminationPoint> output = new ArrayList<>();
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint tp : tps) {
            output.add(toOutputTp(tp));
        }
        return output;
    }

    private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.phy.ne.full.info.TerminationPoint toOutputTp(
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint tp) {
        return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.phy.ne.full.info.TerminationPointBuilder(tp)
                .setPhysical(tp.getAugmentation(TerminationPoint1.class).getPhysical())
                .build();
    }
}

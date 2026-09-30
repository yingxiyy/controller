/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.site;

import static net.flex.dci.otn.controller.allocate.designer.model.NeInfo.CARD_TYPE_VENDOR_SEPERATOR;
import static net.flex.dci.otn.controller.allocate.designer.model.NeInfo.OP_CARD_TYPE;
import static net.flex.dci.otn.controller.allocate.designer.model.NeInfo.PANEL_CARD_TYPE;
import static net.flex.dci.otn.controller.allocate.designer.model.NeInfo.OP_PORT_SIG;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.NoAvailableSlotsException;
import net.flex.dci.otn.controller.allocate.designer.NodeUtils;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfo;
import net.flex.dci.otn.controller.allocate.designer.ne.EquipmentRepo;
import net.flex.dci.otn.controller.allocate.designer.model.RamanSupport;
import net.flex.dci.otn.controller.allocate.designer.ne.NeNodeRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.TpRepo;
import net.flex.dci.otn.controller.allocate.designer.site.model.LinkOutput;
import net.flex.dci.otn.controller.allocate.ne.Card;
import net.flex.dci.otn.controller.allocate.ne.CrossConnection;
import net.flex.dci.otn.controller.allocate.ne.CrossConnectionPoint;
import net.flex.dci.otn.controller.allocate.ne.ExternalLinkTo;
import net.flex.dci.otn.controller.allocate.ne.OcmGridGroup;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.FrequencyType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To2;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionUnprotected;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.attributes.OCMGripGroups;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.attributes.OCMGripGroupsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.grip.group.Channels;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.grip.group.ChannelsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RoutingType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SiteNodeService {

    @Autowired
    private EquipmentRepo equipmentRepo;
    @Autowired
    private TpRepo tpRepo;
    @Autowired
    private XCService xcService;
    @Autowired
    private LinkService linkService;

    @Autowired
    private NeNodeRepo neNodeRepo;
    @Autowired
    private NodeUtils nodeUtils;

    private NodeXcLink createNeXcLink(@NonNull Integer grid, NodeTp nodeTp, NeInfo neInfo, Class<? extends ProtectionType> protectionType, @NonNull Boolean reversed) throws NeDesignerException {
        /*Input*/
        @NonNull List<CardTps> cardTpsList = nodeTp.getCardTps();

        @NonNull String nodeId = nodeTp.getNodeId();
        List<CardTps> slaveCardTpsList = nodeTp.getSlaveCardTps();
        List<CardTps> thirdCardTpsList = nodeTp.getThirdCardTps();
        boolean isOTSP = cardTpsList.get(cardTpsList.size() - 1).card.getCardType().equals(OP_CARD_TYPE);//fisrt card is panel

        if (reversed) {
            Collections.reverse(cardTpsList);
            Collections.reverse(slaveCardTpsList);
            Collections.reverse(thirdCardTpsList);
        }

        /*Output*/
        List<CrossConnections> xcs;
        List<Link> links = new ArrayList<>();
        List<InternalLinks> internalLinks = new ArrayList<>();
        List<CrossConnections> slaveXcs;
        List<CrossConnections> thirdXcs;
        List<Link> slaveLinks = new ArrayList<>();
        List<Link> thirdLinks = new ArrayList<>();
        Set<String> busyIds = new HashSet<>();

        //create xcs
        Boolean isProtected = protectionType.equals(ProtectionUnprotected.class) ? false : true;
        xcs = createNodeXcs(grid, isProtected, cardTpsList, nodeId, nodeTp.getResourceLayout());
        slaveXcs = createNodeXcs(grid, isProtected, slaveCardTpsList, nodeId, nodeTp.getResourceLayout());
        thirdXcs = createNodeXcs(grid, isProtected, thirdCardTpsList, nodeId, nodeTp.getResourceLayout());

        //create link for main
        CardTps protectedCardTps = null;
        int size = cardTpsList.size();
        boolean usesDualFmux64MainTopology = nodeTp.getProtectionPeerRole() == null
                && nodeTp.getResourceLayout().usesDualFmux64Topology();
        if (usesDualFmux64MainTopology) {
            protectedCardTps = createDualFmux64Links(nodeTp.getResourceLayout(), neInfo,
                    nodeId, cardTpsList, reversed,
                    nodeTp.getResourceLayout().getFmuxMainLinePort(isProtected),
                    links, internalLinks, busyIds);
        }
        for (int i = 0; i < size; i++) {
            if (usesDualFmux64MainTopology) {
                continue;
            }
            CardTps cardTps = cardTpsList.get(i);
            Card card = cardTps.getCard();
            if (!cardTps.getSlavePortNameTpMap().isEmpty()) {
                protectedCardTps = cardTps;
            }

            //create panel link
            if (card.getCardType().equals(PANEL_CARD_TYPE)) {
                Map<String, Map<String, List<ExternalLinkTo>>> toCardMap = neInfo.getExternalLinkInfo().get(card.getCardType());

                for (int j = 0; j < size; j++) {
                    CardTps toCardTps = cardTpsList.get(j);
                    String toCardType = toCardTps.card.getCardType();
                    Map<String, List<ExternalLinkTo>> fromToMap = toCardMap.get(toCardType);
                    if (fromToMap != null) {//连接panel的mux卡找到了
                        try {
                            LinkOutput linkOutPut = linkService.createCLLinks(nodeId, fromToMap, cardTps.getPortNameTpMap(), toCardTps.getPortNameTpMap(), busyIds);

                            links.addAll(linkOutPut.getLinks());
                            internalLinks.addAll(linkOutPut.getInternalLinks());

                            break;
                        } catch (Exception e) {
                            String msg = String.format("Failed to create cable link from %s to %s ", card.getCardType(), toCardType);
                            log.error(msg, e);
                            throw new NeDesignerException(msg, e);
                        }
                    }
                }

                continue;

            }

            //create oms link
            if (i == size - 1) {
                continue;//最后一张板卡不需要创建link
            }
            CardTps nextCardTps = cardTpsList.get(i + 1);
            if (nextCardTps.getCard().getCardType().equals(PANEL_CARD_TYPE)) {
                continue;//panel不需要oms link
            }
            try {
                createCardSelfLinks(nodeTp.getResourceLayout(), neInfo, nodeId, cardTps,
                        links, internalLinks, busyIds);
                Map<String, List<ExternalLinkTo>> fromToMap = neInfo.getExternalLinkInfoFromTo(card.getCardType(), nextCardTps.getCard().getCardType());
                fromToMap = selectFmuxLine(nodeTp.getResourceLayout(), fromToMap, card.getCardType(),
                        nextCardTps.getCard().getCardType(),
                        nodeTp.getResourceLayout().getFmuxMainLinePort(isProtected));
                fromToMap = nodeTp.getResourceLayout().selectOlpTilaLinks(fromToMap,
                        card.getCardType(), nextCardTps.getCard().getCardType());
                fromToMap = RamanSupport.selectStationLinks(fromToMap,
                        card.getCardType(), nextCardTps.getCard().getCardType());
//                if (!isOTSP && i == 0 && nextCardTps.getCard().getCardType().equals(OP_CARD_TYPE)) {
//                    fromToMap = getOtspOpToOaMap_getOmstpOatoOpMap(fromToMap);
//                    if (fromToMap.isEmpty()) {
//                        log.error("Failed to get link relation from OP to OA for OTSP.");
//                        throw new NeDesignerException("Failed to get link relation from OP to OA for OTSP.");
//                    }
//                }
//                if (isOTSP && i == 0 && nextCardTps.getCard().getCardType().equals(OA_CARD_TYPE)) {
//                    fromToMap = getOtspOpToOaMap_getOmstpOatoOpMap(fromToMap);
//                    if (fromToMap.isEmpty()) {
//                        log.error("Failed to get link relation from OP to OA for OTSP.");
//                        throw new NeDesignerException("Failed to get link relation from OP to OA for OTSP.");
//                    }
//                }

                LinkOutput linkOutPut = linkService.createOmsLinks(nodeId, fromToMap, cardTps.getPortNameTpMap(), nextCardTps.getPortNameTpMap(), busyIds);
                links.addAll(linkOutPut.getLinks());
                internalLinks.addAll(linkOutPut.getInternalLinks());
            } catch (Exception e) {
                String msg = String.format("Failed to create link from %s to %s ", cardTps.getCard().getCardType(), nextCardTps.getCard().getCardType());
                log.error(msg, e);
                throw new NeDesignerException(msg, e);
            }

        }

        //create link for slave
        createNodeSlaveLinks(neInfo, reversed, nodeId, slaveCardTpsList, internalLinks, slaveLinks, busyIds, protectedCardTps, nodeTp.getResourceLayout());
        //create link for third
        createNodeSlaveLinks(neInfo, reversed, nodeId, thirdCardTpsList, internalLinks, thirdLinks, busyIds, protectedCardTps, nodeTp.getResourceLayout());

        xcs = filterOpXcs(xcs, links);
        return NodeXcLink.builder()
                .links(links)
                .xcs(xcs)
                .internalLinks(internalLinks)
                .slaveLinks(slaveLinks)
                .slaveXs(slaveXcs)
                .thirdLinks(thirdLinks)
                .thirdXs(thirdXcs)
                .busyIds(busyIds)
                .build();

    }

    private CardTps createDualFmux64Links(SiteResourceLayout resourceLayout, NeInfo neInfo,
            String nodeId, List<CardTps> cardTpsList, boolean reversed, String mainFmuxLinePort, List<Link> links,
            List<InternalLinks> internalLinks, Set<String> busyIds) throws NeDesignerException {
        CardTps panel = getRequiredCard(cardTpsList, PANEL_CARD_TYPE, 0);
        CardTps muxPanel = getRequiredCardByPrefix(cardTpsList, "MUXPANEL", 0);
        CardTps mainFmux = getRequiredCardAtSlot(cardTpsList, "FMUX_32", "3");
        CardTps extensionFmux = getRequiredCardAtSlot(cardTpsList, "FMUX_32", "7");
        CardTps tila = getRequiredCard(cardTpsList, "TILA", 0);
        Optional<CardTps> olp = resourceLayout.routesDualFmux64ThroughOlp()
                ? Optional.of(getRequiredCard(cardTpsList, "OLP3_3", 0)) : Optional.empty();
        CardTps protectedCard;

        if (reversed) {
            protectedCard = connectDualFmux64Line(neInfo, nodeId, mainFmux, tila, olp,
                    mainFmuxLinePort, links, internalLinks, busyIds);
            createCardSelfLinks(resourceLayout, neInfo, nodeId, mainFmux,
                    links, internalLinks, busyIds);
            createSelectedLinks(neInfo, nodeId, extensionFmux, mainFmux, "SIG", "COM1", 1,
                    links, internalLinks, busyIds, false);
            createSelectedLinks(neInfo, nodeId, muxPanel, extensionFmux,
                    "MPO?,5,8,1", "MPO?,1,4,1", 4, links, internalLinks, busyIds, false);
            createSelectedLinks(neInfo, nodeId, muxPanel, mainFmux,
                    "MPO?,1,4,1", "MPO?,1,4,1", 4, links, internalLinks, busyIds, false);
            createSelectedLinks(neInfo, nodeId, panel, muxPanel, null, null, 1,
                    links, internalLinks, busyIds, true);
        } else {
            createSelectedLinks(neInfo, nodeId, panel, muxPanel, null, null, 1,
                    links, internalLinks, busyIds, true);
            createSelectedLinks(neInfo, nodeId, muxPanel, extensionFmux,
                    "MPO?,5,8,1", "MPO?,1,4,1", 4, links, internalLinks, busyIds, false);
            createSelectedLinks(neInfo, nodeId, extensionFmux, mainFmux, "SIG", "COM1", 1,
                    links, internalLinks, busyIds, false);
            createCardSelfLinks(resourceLayout, neInfo, nodeId, mainFmux,
                    links, internalLinks, busyIds);
            protectedCard = connectDualFmux64Line(neInfo, nodeId, mainFmux, tila, olp,
                    mainFmuxLinePort, links, internalLinks, busyIds);
            createSelectedLinks(neInfo, nodeId, muxPanel, mainFmux,
                    "MPO?,1,4,1", "MPO?,1,4,1", 4, links, internalLinks, busyIds, false);
        }
        return protectedCard;
    }

    private CardTps connectDualFmux64Line(NeInfo neInfo, String nodeId, CardTps mainFmux,
            CardTps tila, Optional<CardTps> olp, String mainFmuxLinePort, List<Link> links,
            List<InternalLinks> internalLinks, Set<String> busyIds) throws NeDesignerException {
        if (!olp.isPresent()) {
            createSelectedLinks(neInfo, nodeId, mainFmux, tila, mainFmuxLinePort, "LINE_WEST", 1,
                    links, internalLinks, busyIds, false);
            return mainFmux;
        }

        CardTps protectionCard = olp.get();
        createSelectedLinks(neInfo, nodeId, protectionCard, mainFmux,
                "?SIG,1,3,1", mainFmuxLinePort, 1, links, internalLinks, busyIds, false);
        createSelectedLinks(neInfo, nodeId, tila, protectionCard,
                "LINE_WEST", "1A", 1, links, internalLinks, busyIds, false);
        return protectionCard;
    }

    private CardTps getRequiredCard(List<CardTps> cards, String cardType, int occurrence)
            throws NeDesignerException {
        List<CardTps> matches = cards.stream()
                .filter(card -> cardType.equals(card.getCard().getCardType()))
                .collect(Collectors.toList());
        if (occurrence >= matches.size()) {
            throw new NeDesignerException("Cannot find required " + cardType
                    + " occurrence " + occurrence + " for Bone2.0 Flex64");
        }
        return matches.get(occurrence);
    }

    private CardTps getRequiredCardByPrefix(List<CardTps> cards, String cardTypePrefix, int occurrence)
            throws NeDesignerException {
        List<CardTps> matches = cards.stream()
                .filter(card -> card.getCard().getCardType().startsWith(cardTypePrefix))
                .collect(Collectors.toList());
        if (occurrence >= matches.size()) {
            throw new NeDesignerException("Cannot find required " + cardTypePrefix
                    + " occurrence " + occurrence + " for Bone2.0 Flex64");
        }
        return matches.get(occurrence);
    }

    private CardTps getRequiredCardAtSlot(List<CardTps> cards, String cardType, String slot)
            throws NeDesignerException {
        return cards.stream()
                .filter(card -> cardType.equals(card.getCard().getCardType()))
                .filter(card -> isCardAtSlot(card, slot))
                .findFirst()
                .orElseThrow(() -> new NeDesignerException("Cannot find required " + cardType
                        + " at slot " + slot + " for Bone2.0 Flex64"));
    }

    private static boolean isCardAtSlot(CardTps card, String slot) {
        return card.getPortNameTpMap().values().stream().findFirst()
                .map(PhysicalTpIdNamingRule::getEquipId)
                .map(PhysicalEqpIdNamingRule::getSlotFromEquipId)
                .filter(slot::equals)
                .isPresent();
    }

    private void createSelectedLinks(NeInfo neInfo, String nodeId, CardTps sourceCard,
            CardTps destinationCard, String sourcePort, String destinationPort, int expectedLinks,
            List<Link> links, List<InternalLinks> internalLinks, Set<String> busyIds, boolean cable)
            throws NeDesignerException {
        Map<String, List<ExternalLinkTo>> fromToMap = neInfo.getExternalLinkInfoFromTo(
                sourceCard.getCard().getCardType(), destinationCard.getCard().getCardType());
        if (sourcePort != null) {
            fromToMap = selectPortRelation(fromToMap, destinationCard.getCard().getCardType(),
                    sourcePort, destinationPort);
        }
        LinkOutput output = cable
                ? linkService.createCLLinks(nodeId, fromToMap, sourceCard.getPortNameTpMap(),
                        destinationCard.getPortNameTpMap(), busyIds)
                : linkService.createOmsLinks(nodeId, fromToMap, sourceCard.getPortNameTpMap(),
                        destinationCard.getPortNameTpMap(), busyIds);
        if (output.getLinks().size() != expectedLinks) {
            throw new NeDesignerException("Expected " + expectedLinks + " links from "
                    + sourceCard.getCard().getCardType() + " " + sourcePort + " to "
                    + destinationCard.getCard().getCardType() + " " + destinationPort
                    + ", but created " + output.getLinks().size());
        }
        links.addAll(output.getLinks());
        internalLinks.addAll(output.getInternalLinks());
    }

    static Map<String, List<ExternalLinkTo>> selectPortRelation(
            Map<String, List<ExternalLinkTo>> fromToMap, String destinationCardType,
            String sourcePort, String destinationPort) throws NeDesignerException {
        Map<String, List<ExternalLinkTo>> selected = new java.util.LinkedHashMap<>();
        List<ExternalLinkTo> targets = Optional.ofNullable(fromToMap.get(sourcePort))
                .orElse(Collections.emptyList()).stream()
                .filter(target -> destinationCardType.equals(target.getCardType()))
                .filter(target -> destinationPort.equals(target.getPort()))
                .collect(Collectors.toList());
        if (targets.isEmpty()) {
            throw new NeDesignerException("Cannot find Bone2.0 Flex64 port relation "
                    + sourcePort + " -> " + destinationCardType + " " + destinationPort);
        }
        selected.put(sourcePort, targets);
        return selected;
    }

    private Map<String, List<ExternalLinkTo>> selectFmuxLine(
            SiteResourceLayout resourceLayout, Map<String, List<ExternalLinkTo>> fromToMap, String sourceCardType,
            String destinationCardType, String selectedFmuxPort) {
        return resourceLayout.selectFmuxTilaLinks(fromToMap, sourceCardType, destinationCardType,
                selectedFmuxPort);
    }

    private void createNodeSlaveLinks(NeInfo neInfo, Boolean reversed, String nodeId, List<CardTps> slaveCardTpsList, List<InternalLinks> internalLinks, List<Link> slaveLinks, Set<String> busyIds,
                                      CardTps protectedCardTps, SiteResourceLayout resourceLayout) throws NeDesignerException {
        int slaveSize = slaveCardTpsList.size();
        for (int i = 0; i < slaveSize; i++) {
            if (protectedCardTps == null) {
                String msg = "Failed to create slave link ,because failed to get protected card TP.";
                log.error(msg);
                throw new NeDesignerException(msg);
            }
            CardTps slaveCardTps = slaveCardTpsList.get(i);
            Card card = slaveCardTps.getCard();

            //create link from/to protected card
            if (i == 0) {
                CardTps srcCardTps;
                CardTps destCardTps;

                Map<String, String> srcPortMap;
                Map<String, String> destPortMap;
                if (reversed) {
                    srcCardTps = slaveCardTps;
                    srcPortMap = slaveCardTps.getPortNameTpMap();
                    destCardTps = protectedCardTps;
                    destPortMap = protectedCardTps.getSlavePortNameTpMap();
                } else {
                    srcCardTps = protectedCardTps;
                    srcPortMap = protectedCardTps.getSlavePortNameTpMap();
                    destCardTps = slaveCardTps;
                    destPortMap = slaveCardTps.getPortNameTpMap();
                }

                try {
                    Map<String, List<ExternalLinkTo>> fromToMap = neInfo.getExternalLinkInfoFromTo(srcCardTps.card.getCardType(), destCardTps.card.getCardType());
                    // FMUX SIGB is the OMSP slave output; preserve A/Z TILA orientation.
                    fromToMap = selectFmuxLine(resourceLayout, fromToMap, srcCardTps.card.getCardType(),
                            destCardTps.card.getCardType(), "SIGB");
                    LinkOutput linkOutPut = linkService.createOmsLinks(nodeId, fromToMap, srcPortMap, destPortMap, busyIds);
                    slaveLinks.addAll(linkOutPut.getLinks());
                    internalLinks.addAll(linkOutPut.getInternalLinks());

                } catch (Exception e) {
                    String msg = String.format("Failed to create link from %s to %s ", srcCardTps.card.getCardType(), destCardTps.card.getCardType());
                    log.error(msg, e);
                    throw new NeDesignerException(msg, e);
                }
            }

            //create link for slave card
            if (i == slaveSize - 1) {
                continue;
            }

            CardTps nextCardTps = slaveCardTpsList.get(i + 1);
            try {
                Map<String, List<ExternalLinkTo>> fromToMap = neInfo.getExternalLinkInfoFromTo(card.getCardType(), nextCardTps.getCard().getCardType());
                LinkOutput linkOutPut = linkService.createOmsLinks(nodeId, fromToMap, slaveCardTps.getPortNameTpMap(), nextCardTps.getPortNameTpMap(), busyIds);
                slaveLinks.addAll(linkOutPut.getLinks());
                internalLinks.addAll(linkOutPut.getInternalLinks());
            } catch (Exception e) {
                String msg = String.format("Failed to create link from %s to %s ", slaveCardTps.getCard().getCardType(), nextCardTps.getCard().getCardType());
                log.error(msg, e);
                throw new NeDesignerException(msg, e);
            }
        }
    }

    private void createCardSelfLinks(SiteResourceLayout resourceLayout, NeInfo neInfo, String nodeId,
            CardTps cardTps, List<Link> links, List<InternalLinks> internalLinks, Set<String> busyIds)
            throws NeDesignerException {
        if (!resourceLayout.hasCardSelfLinks(cardTps.card.getCardType())) {
            return;
        }
        Map<String, List<ExternalLinkTo>> fromToMap = resourceLayout.selectCardSelfLinks(
                neInfo.getExternalLinkInfoFromTo(cardTps.card.getCardType(), cardTps.card.getCardType()),
                cardTps.card.getCardType());
        if (fromToMap.isEmpty()) {
            return;
        }
        // Bone2.0 Flex32 needs the FMUX SIG-COM2 common segment in siteLink route/supporting-link.
        LinkOutput linkOutPut = linkService.createOmsLinks(nodeId, fromToMap,
                cardTps.getPortNameTpMap(), cardTps.getPortNameTpMap(), busyIds);
        links.addAll(linkOutPut.getLinks());
        internalLinks.addAll(linkOutPut.getInternalLinks());
    }

    /**
     * OLP3_3 has 3 group cards, remove unused ones.
     *
     * @param xcs
     * @param links
     * @return
     */
    private List<CrossConnections> filterOpXcs(List<CrossConnections> xcs, List<Link> links) {
        Set<String> linkTpIs = links.stream()
                .flatMap(link -> Arrays.asList(link.getSource().getSourceTp().getValue(), link.getDestination().getDestTp().getValue()).stream())
                .collect(Collectors.toSet());
        return xcs.stream().filter(xc -> xc.getAps() == null || isApsXcUsedByLink(xc, linkTpIs))
                .collect(Collectors.toList());

    }

    private boolean isApsXcUsedByLink(CrossConnections xc, Set<String> linkTpIds) {
        // Bone2.0 Flex32 FMUX APS is COM2 -> SIGA/SIGB; destination ports prove the XC is used.
        boolean sourceUsed = xc.getSourceTp().stream()
                .anyMatch(tp -> linkTpIds.contains(tp.getTpRef().getValue()));
        boolean destinationUsed = xc.getDestinationTp().stream()
                .anyMatch(tp -> linkTpIds.contains(tp.getTpRef().getValue()));
        return sourceUsed || destinationUsed;
    }

    private Map<String, List<ExternalLinkTo>> getOtspOpToOaMap_getOmstpOatoOpMap(Map<String, List<ExternalLinkTo>> fromToMap) {
        Map<String, List<ExternalLinkTo>> output = new HashMap<>();
        for (Entry<String, List<ExternalLinkTo>> entry : fromToMap.entrySet()) {
            if (OP_PORT_SIG.stream().anyMatch(sigPort -> entry.getKey().endsWith(sigPort))) {
                output.put(entry.getKey(), entry.getValue());
                break;
            }
        }
        return output;
    }

    private List<CrossConnections> createNodeXcs(@NonNull Integer grid, Boolean isProtected,
            @NonNull List<CardTps> cardTpsList, @NonNull String nodeId,
            SiteResourceLayout resourceLayout) throws NeDesignerException {
        List<CrossConnections> xcs = new ArrayList<>();
        for (CardTps cardTps : cardTpsList) {
            boolean extensionFmux = resourceLayout.usesDualFmux64Topology()
                    && "FMUX_32".equals(cardTps.card.getCardType())
                    && !isCardAtSlot(cardTps, "3");
            Map<String, String> totalPortTp = cardTps.getPortNameTpMap();

            //create xc
            if (!cardTps.getSlavePortNameTpMap().isEmpty()) {
                totalPortTp = Stream.concat(cardTps.getPortNameTpMap().entrySet().stream(), cardTps.getSlavePortNameTpMap().entrySet().stream()).collect(
                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }
            if (cardTps.getThirdPortNameTpMap() != null && !cardTps.getThirdPortNameTpMap().isEmpty()) {
                // Bone2.0 OLP3-3 APS is defined as SIG -> A/B/C; main route owns the APS XC.
                totalPortTp = Stream.concat(totalPortTp.entrySet().stream(), cardTps.getThirdPortNameTpMap().entrySet().stream())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }
            if (resourceLayout.useFmux32Com1ProtectionXc(cardTps.card.getCardType())
                    && !extensionFmux) {
                xcs.addAll(createFmux32ProtectionXcs(nodeId, cardTps.card, totalPortTp, isProtected));
                continue;
            }
            xcs.addAll(xcService.createCardXcs(grid, nodeId, cardTps.card, totalPortTp,
                    extensionFmux ? false : isProtected));
        }
        return xcs;
    }

    private List<CrossConnections> createFmux32ProtectionXcs(String nodeId, Card card,
            Map<String, String> totalPortTp, Boolean isProtected) throws NeDesignerException {
        List<CrossConnections> xcs = new ArrayList<>();
        for (CrossConnection crossConnection : card.getCrossConnections()) {
            if (!crossConnection.getInitiated()) {
                continue;
            }
            CrossConnection fmux32Xc = adaptFmux32ProtectionXc(crossConnection);
            xcs.addAll(xcService.createXCs(nodeId, totalPortTp, fmux32Xc, isProtected));
        }
        return xcs;
    }

    CrossConnection adaptFmux32ProtectionXc(CrossConnection crossConnection) {
        if (crossConnection.getAps() == null) {
            return crossConnection;
        }
        // Bone2.0 Flex32 FMUX APS uses both COM ports as the fixed common side.
        return copyCrossConnectionWithPorts(crossConnection, "COM1,COM2", "SIGA,SIGB");
    }

    private CrossConnection copyCrossConnectionWithPorts(CrossConnection original,
            String sourcePort, String destinationPort) {
        CrossConnection copy = new CrossConnection();
        copy.setType(original.getType());
        copy.setBiDirection(original.getBiDirection());
        copy.setInitiated(original.getInitiated());
        copy.setAdditional(original.getAdditional());
        copy.setIsFixed(original.getIsFixed());
        copy.setExistedOnNe(original.getExistedOnNe());
        copy.setMultiple(original.getMultiple());
        copy.setDescription(original.getDescription());
        copy.setCentralFrequency(original.getCentralFrequency());
        copy.setSupportedSignal(original.getSupportedSignal());
        copy.setWssChannel(original.getWssChannel());
        copy.setAmplifier(original.getAmplifier());
        copy.setAps(original.getAps());
        copy.setServiceType(original.getServiceType());
        copy.setOpModes(original.getOpModes());
        copy.setFrom(copyPointWithPort(original.getFrom(), sourcePort));
        copy.setTo(copyPointWithPort(original.getTo(), destinationPort));
        return copy;
    }

    private CrossConnectionPoint copyPointWithPort(CrossConnectionPoint original, String port) {
        CrossConnectionPoint copy = new CrossConnectionPoint();
        copy.setPort(port);
        copy.setPortFriendlyName(original == null ? null : original.getPortFriendlyName());
        copy.setLayer(original == null ? null : original.getLayer());
        copy.setCardType(original == null ? null : original.getCardType());
        copy.setSupportedSignal(original == null ? null : original.getSupportedSignal());
        copy.setTransceiverSupportedSignal(original == null ? null
                : original.getTransceiverSupportedSignal());
        return copy;
    }


    /**
     * Always return first one from the possible list
     *
     * @param card
     * @param usedSlots
     * @param specifiedSlot
     * @return
     * @throws NeDesignerException
     */
    private Integer getCardAvailableSlots(Card card, Set<Integer> usedSlots, Integer specifiedSlot) throws NoAvailableSlotsException {
        List<Integer> possibleSlots = card.getPossibleSlot();
        if (specifiedSlot != null) {
            if (usedSlots.contains(specifiedSlot)) {
                throw new NoAvailableSlotsException(String.format("Invalid specifiedSlot:%s for card:%s, because this slot has been used.", specifiedSlot, card.getVendorType()));
            }
            if (!possibleSlots.contains(specifiedSlot)) {
                throw new NoAvailableSlotsException(
                        String.format("Invalid specifiedSlot:%s for card:%s, because this slot is not in possible slots:%s.", specifiedSlot, card.getVendorType(), possibleSlots));
            }
            return specifiedSlot;
        }

        for (Integer slot : possibleSlots) {
            if (!usedSlots.contains(slot)) {
                return slot;
            }
        }
        throw new NoAvailableSlotsException(String.format("No available slot for card: %s", card.getCardType()));
    }

    public NodeTp createNeEquipAndTp(Integer grid, Class<? extends ProtectionType> protectionType,
                                     WDM_Band wdmBand, String nodeId, List<String> cardClassList,
                                     List<String> extraSlaveCardClass, NeInfo neInfo,
                                     Set<String> cardTypeVendors, List<Equipments> equips,
                                     List<TerminationPoint> nodeTpList,
                                     SiteResourceLayout resourceLayout,
                                     RoutingType protectionPeerRole)
            throws NoAvailableSlotsException, NeDesignerException {
        return createNeEquipAndTp(grid, protectionType, wdmBand, nodeId, cardClassList, extraSlaveCardClass, neInfo,
                cardTypeVendors, false, false, false, equips, nodeTpList, resourceLayout, protectionPeerRole);
    }

    private NodeTp createNeEquipAndTp(Integer grid, Class<? extends ProtectionType> protectionType, WDM_Band wdmBand, String nodeId,
                                     List<String> cardClassList, List<String> extraSlaveCardClass, NeInfo neInfo,
                                     Set<String> cardTypeVendors, boolean ramanOnLeft, boolean ramanOnRight, boolean reversed,
                                     List<Equipments> equips, List<TerminationPoint> nodeTpList,
                                     SiteResourceLayout resourceLayout, RoutingType protectionPeerRole)
            throws NoAvailableSlotsException, NeDesignerException {
        Set<Integer> lineCardUsedSlots = nodeUtils.getLineCardUsedSlots(equips);
//        List<TerminationPoint> nodeTpList = new ArrayList<>();

        List<CardTps> cardTpsList = new ArrayList<>();
        List<CardTps> slaveCardTpsList = new ArrayList<>();
        List<CardTps> thirdCardTpsList = new ArrayList<>();
        Map<String, Card> specifiedCardsByCardClass = neInfo.getCardsByCardVendors(cardTypeVendors);
        Map<String, Integer> cardOccurrences = new HashMap<>();

        for (String cardClass : cardClassList) {
            //如果UI指定，就获取指定的card，否则默认取json配置的第一个card
            Card card = specifiedCardsByCardClass.containsKey(cardClass) ? specifiedCardsByCardClass.get(cardClass)
                    : neInfo.getDefaultCardByCardClass(cardClass, wdmBand, grid);

            if (cardClass.equals(PANEL_CARD_TYPE) && equips != null && equips.stream()
                    .filter(e -> e.getEquipTypeConfiged().equals(PANEL_CARD_TYPE))
                    .findAny()
                    .isPresent()) {
                //not create equip,tp, node reuse scenario(e.g. 96波)
                Map<String, String> cardPortNameTpIdMapPanel = new HashMap<>();
                Optional<TerminationPoint> idlePanelMuxTpOptional = nodeTpList.stream()
                        .filter(tp -> tp.getAugmentation(TerminationPoint1.class)
                                .getPhysical()
                                .getPortType()
                                .equals(PortType.PanelMUX) && !tp.getAugmentation(TerminationPoint1.class).getPhysical()
                                .getConnectionStatus().equals(
                                        ConnectionStatus.Busy)).findAny();
                if (!idlePanelMuxTpOptional.isPresent()) {
                    throw new NeDesignerException(String.format("No available TP for %s, for reused node: %s", PortType.PanelMUX, nodeId));
                }
                String idlePanelMuxTpId = idlePanelMuxTpOptional.get().getTpId().getValue();
                cardPortNameTpIdMapPanel.put(PhysicalTpIdNamingRule.getPortNameByTpId(idlePanelMuxTpId), idlePanelMuxTpId);
                cardTpsList.add(CardTps.builder()
                        .card(card)
                        .portNameTpMap(cardPortNameTpIdMapPanel)
                        .slavePortNameTpMap(Collections.EMPTY_MAP)
                        .thirdPortNameTpMap(Collections.EMPTY_MAP)
                        .build());//这里添加cardTpsList，是为了后续创建cable link
                continue;
            }

            Integer specifiedSlot = getSpecifiedSlot(card.getVendorType(), cardTypeVendors);
            int occurrence = cardOccurrences.getOrDefault(card.getCardType(), 0);
            Integer layoutSlot = protectionPeerRole == null
                    ? resourceLayout.getMainNodeSlot(card.getCardType(), occurrence)
                    // Protection slot override is for the peer line card; PANEL keeps its model-defined slot.
                    : PANEL_CARD_TYPE.equals(card.getCardType()) ? null : resourceLayout.getProtectionNodeSlot(protectionPeerRole);
            if (layoutSlot != null) {
                specifiedSlot = layoutSlot;
            }
            createCardEquipAndTp(neInfo, nodeId, lineCardUsedSlots, equips, nodeTpList, cardTpsList, card,
                    specifiedSlot, resourceLayout, reversed, protectionPeerRole);
            if (ProtectionBidir1To2.class.equals(protectionType)
                    && "OLP3_3".equals(card.getCardType())) {
                splitOlpOneToTwoPorts(cardTpsList.get(cardTpsList.size() - 1));
            }
            cardOccurrences.put(card.getCardType(), occurrence + 1);
        }

        if (ramanOnLeft || ramanOnRight) {
            addRamanCards(neInfo, nodeId, lineCardUsedSlots, equips, nodeTpList, cardTpsList,
                    ramanOnLeft, ramanOnRight, reversed);
        }

        for (String cardClass : extraSlaveCardClass) {
            Card card = specifiedCardsByCardClass.containsKey(cardClass) ? specifiedCardsByCardClass.get(cardClass)
                    : neInfo.getDefaultCardByCardClass(cardClass, wdmBand, grid);
            Integer specifiedSlot = getSpecifiedSlot(card.getVendorType(), cardTypeVendors);
            createCardEquipAndTp(neInfo, nodeId, lineCardUsedSlots, equips, nodeTpList, slaveCardTpsList, card,
                    specifiedSlot, resourceLayout, reversed, protectionPeerRole);
        }

        if (protectionType.equals(ProtectionBidir1To2.class)) {
            for (String cardClass : extraSlaveCardClass) {
                Card card = specifiedCardsByCardClass.containsKey(cardClass) ? specifiedCardsByCardClass.get(cardClass)
                        : neInfo.getDefaultCardByCardClass(cardClass, wdmBand, grid);
                Integer specifiedSlot = getSpecifiedSlot(card.getVendorType(), cardTypeVendors);
                createCardEquipAndTp(neInfo, nodeId, lineCardUsedSlots, equips, nodeTpList, thirdCardTpsList, card,
                        specifiedSlot, resourceLayout, reversed, protectionPeerRole);
            }
        }

        return NodeTp.builder()
                .equipments(equips)
                .nodeTpList(nodeTpList)
                .cardTps(cardTpsList)
                .slaveCardTps(slaveCardTpsList)
                .thirdCardTps(thirdCardTpsList)
                .nodeId(nodeId)
                .resourceLayout(resourceLayout)
                .protectionPeerRole(protectionPeerRole)
                .build();
    }

    private void splitOlpOneToTwoPorts(CardTps olpCardTps) {
        Map<String, String> protectionPorts = olpCardTps.getSlavePortNameTpMap();
        // Bone2.0 slide 14/15: OLP group 1 maps A=main, B=slave, C=third.
        Map<String, String> mainPorts = olpCardTps.getPortNameTpMap().entrySet().stream()
                .filter(entry -> "1SIG".equals(entry.getKey()) || "1A".equals(entry.getKey()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        Map<String, String> slavePorts = protectionPorts.entrySet().stream()
                .filter(entry -> "1B".equals(entry.getKey()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        Map<String, String> thirdPorts = protectionPorts.entrySet().stream()
                .filter(entry -> "1C".equals(entry.getKey()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        olpCardTps.setPortNameTpMap(mainPorts);
        olpCardTps.setSlavePortNameTpMap(slavePorts);
        olpCardTps.setThirdPortNameTpMap(thirdPorts);
    }

    private void addRamanCards(NeInfo neInfo, String nodeId, Set<Integer> lineCardUsedSlots,
                               List<Equipments> equipments, List<TerminationPoint> nodeTpList,
                               List<CardTps> cardTpsList, boolean ramanOnLeft, boolean ramanOnRight,
                               boolean reversed) throws NoAvailableSlotsException, NeDesignerException {
        Card ramanCard = neInfo.getCardByCardVendor(RamanSupport.CARD_VENDOR_TYPE);
        CardTps leftRaman = null;
        CardTps rightRaman = null;

        if (ramanOnLeft) {
            leftRaman = createCardEquipAndTp(neInfo, nodeId, lineCardUsedSlots, equipments, nodeTpList, ramanCard,
                    RamanSupport.getSlot(ramanOnLeft, ramanOnRight, true));
        }
        if (ramanOnRight) {
            rightRaman = createCardEquipAndTp(neInfo, nodeId, lineCardUsedSlots, equipments, nodeTpList, ramanCard,
                    RamanSupport.getSlot(ramanOnLeft, ramanOnRight, false));
        }

        CardTps preReverseLeft = reversed ? rightRaman : leftRaman;
        CardTps preReverseRight = reversed ? leftRaman : rightRaman;
        if (preReverseLeft != null) {
            int index = !cardTpsList.isEmpty() && PANEL_CARD_TYPE.equals(cardTpsList.get(0).getCard().getCardType()) ? 1 : 0;
            cardTpsList.add(index, preReverseLeft);
        }
        if (preReverseRight != null) {
            cardTpsList.add(preReverseRight);
        }
    }

    /**
     * return slot if user specified or null
     *
     * @param vendorType
     * @param cardTypeVendors
     * @return
     */
    private Integer getSpecifiedSlot(String vendorType, Set<String> cardTypeVendors) throws NeDesignerException {
        String cardTypeVendorFound = null;
        for (String cardTypeVendor : cardTypeVendors) {
            if (cardTypeVendor.contains(vendorType)) {
                cardTypeVendorFound = cardTypeVendor;
                break;
            }
        }
        if (cardTypeVendorFound == null) {
            return null;
        }
        if (!cardTypeVendorFound.contains(CARD_TYPE_VENDOR_SEPERATOR)) {
            return null;
        }
        Integer slot;
        try {
            slot = Integer.parseInt(cardTypeVendorFound.substring(cardTypeVendorFound.lastIndexOf(CARD_TYPE_VENDOR_SEPERATOR) + 1));
        } catch (Exception e) {
            log.error("Invalid cardVendor,bcause failed to get slot by :{}", cardTypeVendorFound, e);
            throw new NeDesignerException("Invalid cardVendor,bcause failed to get slot by :" + cardTypeVendorFound, e);
        }
        return slot;
    }

    private void createCardEquipAndTp(NeInfo neInfo, String nodeId, Set<Integer> lineCardUsedSlots, List<Equipments> equipments, List<TerminationPoint> nodeTpList, List<CardTps> cardTpsList,
                                      Card card, Integer specifiedSlot) throws NoAvailableSlotsException, NeDesignerException {
        cardTpsList.add(createCardEquipAndTp(neInfo, nodeId, lineCardUsedSlots, equipments, nodeTpList, card, specifiedSlot));
    }

    private void createCardEquipAndTp(NeInfo neInfo, String nodeId, Set<Integer> lineCardUsedSlots,
                                      List<Equipments> equipments, List<TerminationPoint> nodeTpList,
                                      List<CardTps> cardTpsList, Card card, Integer specifiedSlot,
                                      SiteResourceLayout resourceLayout, boolean reversed,
                                      RoutingType protectionPeerRole)
            throws NoAvailableSlotsException, NeDesignerException {
        cardTpsList.add(createCardEquipAndTp(neInfo, nodeId, lineCardUsedSlots, equipments, nodeTpList, card,
                specifiedSlot, resourceLayout, reversed, protectionPeerRole));
    }

    private CardTps createCardEquipAndTp(NeInfo neInfo, String nodeId, Set<Integer> lineCardUsedSlots, List<Equipments> equipments,
                                         List<TerminationPoint> nodeTpList, Card card, Integer specifiedSlot)
            throws NoAvailableSlotsException, NeDesignerException {
        return createCardEquipAndTp(neInfo, nodeId, lineCardUsedSlots, equipments, nodeTpList, card, specifiedSlot,
                null, false, null);
    }

    private CardTps createCardEquipAndTp(NeInfo neInfo, String nodeId, Set<Integer> lineCardUsedSlots,
                                         List<Equipments> equipments, List<TerminationPoint> nodeTpList,
                                         Card card, Integer specifiedSlot, SiteResourceLayout resourceLayout,
                                         boolean reversed, RoutingType protectionPeerRole)
            throws NoAvailableSlotsException, NeDesignerException {
        Integer slot = getCardAvailableSlots(card, lineCardUsedSlots, specifiedSlot);

        //Create Card
        Equipments equipment = equipmentRepo.createCardEquipment(nodeId, card, slot, neInfo);
        equipment = applyLayoutEquipmentProperties(equipment, card, resourceLayout, reversed, protectionPeerRole);
        Set<Integer> usedSlots = equipmentRepo.getLineCardUsedSlots(equipment);
        lineCardUsedSlots.addAll(usedSlots);

        //replace empty card with line card
        boolean isReplaced = false;
        for (int i = equipments.size() - 1; i >= 0; i--) {
            Equipments item = equipments.get(i);
            if (item.getEquipType() == null || item.getEquipType().equals(EquipType.Other)) {
                continue;
            }
            Integer itemSlot = Integer.parseInt(item.getSlot());

            if (item.getEquipType().equals(EquipType.EMPTY)) {
                if (usedSlots.contains(itemSlot)) {
                    if (itemSlot.equals(Integer.parseInt(equipment.getSlot()))) {
                        isReplaced = true;
                        equipments.set(i, equipment);
                    } else {
                        equipments.remove(i);
                    }
                }
            }
        }
        if (!isReplaced) {
//            int index = equipmentRepo.getTrafficCardIndex(neInfo.getEmptyCard());
//            equipments.add(index, equipment);
//            log.debug("Add equipment:{} at index:{}", equipment.getEquipmentId(), index);
            equipments.add(equipment);
        }

        //Create tps for this card
        Map<String, TerminationPoint> cardPortNameTpMap = tpRepo.createCardTp(equipment, card, neInfo);
        Map<String, TerminationPoint> slaveCardPortNameTpMap = tpRepo.createSlaveCardTp(equipment, card, neInfo);
        nodeTpList.addAll(cardPortNameTpMap.values());
        nodeTpList.addAll(slaveCardPortNameTpMap.values());
        Map<String, String> cardPortNameTpIdMap = cardPortNameTpMap.entrySet()
                .stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().getTpId().getValue()));
        Map<String, String> slaveCardPortNameTpIdMap = slaveCardPortNameTpMap.entrySet()
                .stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().getTpId().getValue()));
        //Create transceivers if needed
        equipments.addAll(equipmentRepo.createCardTransceivers(nodeId, card, slot, neInfo));
        return CardTps.builder().card(card).portNameTpMap(cardPortNameTpIdMap).slavePortNameTpMap(slaveCardPortNameTpIdMap).build();
    }

    private Equipments applyLayoutEquipmentProperties(Equipments equipment, Card card,
                                                      SiteResourceLayout resourceLayout, boolean reversed,
                                                      RoutingType protectionPeerRole) {
        if (resourceLayout == null) {
            return equipment;
        }
        String classMode = resourceLayout.resolveTilaClassMode(card.getCardType(), reversed, protectionPeerRole);
        if (classMode == null) {
            return equipment;
        }
        // Bone2.0 TILA class-mode is a first-class equipment leaf, not a generic property.
        return new EquipmentsBuilder(equipment)
                .setClassMode(classMode)
                .build();
    }


    public SiteNodeInfo createSiteNode(@NonNull Integer grid, WDM_Band wdmBand, Node node, List<String> cardTypes, List<String> slaveCardTypes, Set<String> cardTypeVendors,
                                       boolean ramanOnLeft, boolean ramanOnRight, NeInfo neInfo,
                                       Class<? extends ProtectionType> protectionType,
                                       OcmGridGroup ocmGripGroupDefinition,
                                       @NonNull Boolean reversed, SiteResourceLayout resourceLayout,
                                       RoutingType protectionPeerRole) throws NeDesignerException {
        String nodeId = node.getNodeId().getValue();
        NodeTp nodeTp;
        List<Equipments> equipList = node.getAugmentation(Node1.class).getPhysical().getEquipments();
        List<TerminationPoint> tps = node.getTerminationPoint();
        if (tps == null) {
            tps = new ArrayList<>();
        }

        try {
            nodeTp = createNeEquipAndTp(grid, protectionType, wdmBand, nodeId, cardTypes,
                    slaveCardTypes, neInfo, cardTypeVendors, ramanOnLeft, ramanOnRight, reversed,
                    equipList, tps, resourceLayout, protectionPeerRole);
        } catch (NoAvailableSlotsException | NeDesignerException e) {
            String msg = "Failed to create equipment and tp;" + e.getMessage();
            log.error(msg, e);
            throw new NeDesignerException(msg, e);
        }

        NodeXcLink nodeXcLink = createNeXcLink(grid, nodeTp, neInfo, protectionType, reversed);

        Boolean isStuffed = nodeUtils.isStuffed(equipList);

        List<OCMGripGroups> ocmGripGroupsList = createOcmGripGroups(equipList, ocmGripGroupDefinition);

        List<InternalLinks> internalLinksUpdate = new ArrayList<>(node.getAugmentation(Node1.class).getPhysical().getInternalLinks());
        internalLinksUpdate.addAll(nodeXcLink.getInternalLinks());

        List<CrossConnections> xcsUpdated = new ArrayList<>(node.getAugmentation(Node1.class).getPhysical().getCrossConnections());
        xcsUpdated.addAll(nodeXcLink.getNodeXcs());

        Node newNode = neNodeRepo
                .refreshNode(node, equipList, nodeTp.getNodeTpList(), internalLinksUpdate, xcsUpdated, nodeXcLink.getBusyIds(), isStuffed, ocmGripGroupsList);

//        newNode=nodeUtils.sortTp(newNode);

        return SiteNodeInfo.builder()
                .xcs(nodeXcLink.getXcs())
                .links(nodeXcLink.getLinks())
                .slaveLinks(nodeXcLink.getSlaveLinks())
                .slaveXcs(nodeXcLink.getSlaveXs())
                .thirdLinks(nodeXcLink.getThirdLinks())
                .thirdXcs(nodeXcLink.getThirdXs())
                .rightPeer(nodeTp.getRightPeer())
                .slaveRightPeer(nodeTp.getSlaveRightPeer())
                .thirdRightPeer(nodeTp.getThirdRightPeer())
                .leftPeer(nodeTp.getLeftPeer())
                .slaveLeftPeer(nodeTp.getSlaveLeftPeer())
                .thirdLeftPeer(nodeTp.getThirdLeftPeer())
                .node(newNode)
                .build();
    }

    private List<OCMGripGroups> createOcmGripGroups(List<Equipments> equipments, OcmGridGroup ocMGripGroupDefinition) {
        if (ocMGripGroupDefinition == null) {
            return null;
        }
        String centralFrequency = ocMGripGroupDefinition.getCentralFrequency();
        Integer length = ocMGripGroupDefinition.getLength();
        List<Channels> channels = new ArrayList<>();

        //for 固定频率
        Integer index = ocMGripGroupDefinition.getIndex();
        if (centralFrequency != null && length != null) {
            String[] centralFrequencyConfig = centralFrequency.split(",");
            long start = Long.parseLong(centralFrequencyConfig[0]);
            long end = Long.parseLong(centralFrequencyConfig[1]);
            long step = Long.parseLong(centralFrequencyConfig[2]);

            for (int i = 1; i <= length; i++) {
                long channelCentralFrequency = start - step * (i - 1);
                long lowerFrequency = channelCentralFrequency - step / 2;
                long upperFrequency = channelCentralFrequency + step / 2;

                Channels channel = new ChannelsBuilder()
                        .setIndex(i)
                        .setLowerFrequency(new FrequencyType(BigInteger.valueOf(lowerFrequency)))
                        .setUpperFrequency(new FrequencyType(BigInteger.valueOf(upperFrequency)))
                        .build();
                channels.add(channel);
            }
        }

        List<String> ocmEquipTypes = ocMGripGroupDefinition.getEquipTypes();
        List<OCMGripGroups> ocmGripGroupsList = new ArrayList<>();
        for (Equipments equipment : equipments) {
            if (ocmEquipTypes.contains(equipment.getEquipType().name())) {
                String ocmSlot = PhysicalEqpIdNamingRule.getOcmSlot(equipment.getEquipmentId());
                ocmGripGroupsList.add(new OCMGripGroupsBuilder().setIndex(index).setChannels(channels).setSlot(ocmSlot).build());
            }
        }

        return ocmGripGroupsList;
    }


}

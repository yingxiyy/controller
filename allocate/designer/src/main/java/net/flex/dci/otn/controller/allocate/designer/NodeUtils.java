
/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer;

import static java.util.stream.Collectors.counting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.allocate.designer.config.NEInfoConfig;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfo;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfoUtil;
import net.flex.dci.otn.controller.allocate.designer.namingrule.NameGenerator;
import net.flex.dci.otn.controller.allocate.designer.ne.EquipmentRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.NeNodeRepo;
import net.flex.dci.otn.controller.allocate.designer.site.model.NodeConstructInfo;
import net.flex.dci.otn.controller.allocate.designer.tunnel.TunnelUtils;
import net.flex.dci.otn.controller.allocate.ne.Card;
import net.flex.dci.otn.controller.allocate.ne.CrossConnection;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.OCMGripGroup;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.attributes.OCMGripGroups;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NodeUtils {

    public static final String OT_XC_KEYWORD = "odu";
    public static final String INDEX = "index";
    @Autowired
    private EquipmentRepo equipmentRepo;
    @Autowired
    private NEInfoConfig neInfoConfig;
    @Autowired
    private TunnelUtils tunnelUtils;


    private static final Integer MAXIMUM_WIDTH_SLOT = 4;

    /**
     * 因为同一张板卡上的L,C端口的TP，都在创建板卡时，就按照指定规则已经创建了。 所以，此时C端口的TP，可以根据L端口得到。
     *
     * @param cPortName e.g. "C2"
     * @param lPortTp e.g. "Site-1621320693433#Ne-1621320704693#LINECARD-1-1#PORT-1-1-L1"
     * @return e.g. "Site-1621320693433#Ne-1621320704693#LINECARD-1-1#PORT-1-1-C2"
     */
    public String getCPortTp(String cPortName, String lPortTp) {
        String preFix = lPortTp.substring(0, lPortTp.lastIndexOf("-") + 1);
        return preFix + cPortName;
    }


    public Integer pickedAvailableSlot(Node node, Card card) throws NeDesignerException {
        List<Integer> possibleSlots = card.getPossibleSlot();
        Integer slot = pickedAvailableSlot(node, possibleSlots, card.getWidth());
        if (slot != null) {
            return slot;
        }

        throw new NeDesignerException(String.format("There is no slot available, for card:%s with possible slots:%s, for node %s.", card.getVendorType(), possibleSlots, node
                .getNodeId()
                .getValue()));
    }

    public Integer pickedAvailableSlot(Node node, List<Integer> possibleSlots, int width) throws NeDesignerException {
        Set<Integer> usedSlots = getLineCardUsedSlots(node);
        for (Integer slot : possibleSlots) {
            boolean isAvailable = true;
            for (int i = 0; i < width; i++) {
                // Multi-slot cards occupy a continuous range starting from the selected slot.
                // Check each occupied slot instead of only checking the first slot repeatedly.
                if (usedSlots.contains(slot + i)) {
                    isAvailable = false;
                    break;
                }
            }
            if (isAvailable) {
                return slot;
            }
        }
        return null;
    }

    public List<String> getCardTypeList(Node node) {
        return getCardEquipmentsStream(node).map(i -> i.getEquipTypeConfiged()).collect(Collectors.toList());
    }

    public List<Equipments> getTransceivers(Node node) {
        return node.getAugmentation(Node1.class)
                .getPhysical()
                .getEquipments()
                .stream()
                .filter(i -> i.getEquipType().equals(EquipType.TRANSCEIVER))
                .collect(Collectors.toList());
    }

    /**
     * e.g. TRANSCEIVER-1-1-LINEBOSC and TRANSCEIVER-1-1-LINEAOSC, so need List
     **/
    public Map<String, List<Equipments>> getTransceiversGroupBySlots(Node node) {
        return node.getAugmentation(Node1.class).getPhysical().getEquipments().stream().filter(i -> i.getEquipType().equals(EquipType.TRANSCEIVER))
                .collect(Collectors.groupingBy(Equipments::getSlot));
    }

    private Stream<Equipments> getCardEquipmentsStream(Node node) {
        return node.getAugmentation(Node1.class).getPhysical().getEquipments().stream()
                .filter(i -> i.getEquipType() != null && (!i.getEquipType().equals(EquipType.Other) || !i.getEquipType().equals(EquipType.EMPTY)));
    }

    public List<Equipments> getCardEquipments(Node node) {
        return node.getAugmentation(Node1.class).getPhysical().getEquipments().stream()
                .filter(i -> i.getEquipType() != null && (!i.getEquipType().equals(EquipType.Other) && !i.getEquipType()
                        .equals(EquipType.EMPTY) && !i.getEquipType().equals(EquipType.TRANSCEIVER)))
                .collect(Collectors.toList());
    }

    public Boolean hasMuxEquipments(Node node) {
        return node.getAugmentation(Node1.class).getPhysical().getEquipments().stream()
                .filter(i -> i.getEquipType() != null && (i.getEquipType().equals(EquipType.MUXPANEL) || i.getEquipType().equals(EquipType.MUX))).findAny().isPresent();
    }

    public List<Equipments> getLineCardEquipments(Node node) {
        return node.getAugmentation(Node1.class).getPhysical().getEquipments().stream()
                .filter(i -> i.getEquipType() != null && (!i.getEquipType().equals(EquipType.Other) && !i.getEquipType()
                        .equals(EquipType.EMPTY) && !i.getEquipType().equals(EquipType.TRANSCEIVER)
                        && !i.getEquipType().equals(EquipType.MUX)))
                .collect(Collectors.toList());
    }

    public List<Equipments> getEquipments(Node node) throws NeDesignerException {
        try {
            return node.getAugmentation(Node1.class).getPhysical().getEquipments();
        } catch (NullPointerException e) {
            log.error("Failed to get equipments for node: {}", node, e);
            throw new NeDesignerException("Failed to get equipments for node: " + node.getNodeId().getValue(), e);
        }
    }

    public Map<String, Equipments> getEquipmentMap(Node node) {
        try {
            return node.getAugmentation(Node1.class)
                    .getPhysical()
                    .getEquipments()
                    .stream()
                    .collect(Collectors.toMap(Equipments::getEquipmentId, Function.identity()));
        } catch (NullPointerException e) {
            log.error("Failed to get equipments for node: {}", node, e);
            return null;
        }
    }


    public Set<Integer> getLineCardUsedSlots(Node node) {
        return getCardEquipmentsStream(node)
                .flatMap(item -> equipmentRepo.getLineCardUsedSlots(item).stream()).
                collect(Collectors.toSet());
    }

//    public Set<Integer> getEquipUsedSlot(Equipments item, NeInfo neInfo) throws NeDesignerException {
//        Integer width = null;
//        Integer height = null;
//        try {
//            Card card = neInfo.getCard(EquipmentRepoUtils.getCardType(item));
//            width = card.getWidth();
//            height = card.getHeight();
//            return getEquipUsedSlotForEmpty(Integer.parseInt(item.getSlot()), width, height);
//        } catch (Exception e) {
//            String msg = String.format("Failed to get height, width for %s, by the json file:%s.", item, neInfo.getJsonFileName());
//            log.error(msg, e);
//            throw new NeDesignerException(msg, e);
//        }
//    }

/*    public Set<Integer> getEquipUsedSlotForEmpty(Integer equipSlot, Integer width, Integer height) {

        Set<Integer> usedSlot = new HashSet<>();
        Set<Integer> widthSlots = new HashSet<>();

        for (int i = width; i > 0; i--) {
            widthSlots.add(equipSlot++);
        }
        usedSlot.addAll(widthSlots);

        for (int h = 1; h < height; h++) {
            for (Integer widthSlot : widthSlots) {
                usedSlot.add(widthSlot + MAXIMUM_WIDTH_SLOT);
            }
        }
        return usedSlot;
    }*/

/*
    public List<Equipments> getNonEmptyEquipments(Node node) {
        return node.getAugmentation(Node1.class).getPhysical().getEquipments().stream().filter(i -> !i.getEquipType().equals(EquipType.EMPTY)).collect(Collectors.toList());
    }
*/


    public Map<String, List<String>> getIdleEquipTpMap(Node node) {
        return node.getTerminationPoint().stream()
                .filter(tp -> tp.getAugmentation(TerminationPoint1.class).getPhysical().getConnectionStatus().equals(ConnectionStatus.Idle))
                .collect(Collectors.groupingBy(item -> item.getAugmentation(TerminationPoint1.class).getPhysical().getEquipmentRef(),
                        Collectors.mapping(tp -> tp.getTpId().getValue(), Collectors.toCollection(ArrayList::new))));
    }

    /*
    public Map<String, List<Equipments>> getAvailableEquipment(Node node) {
        return getCardEquipmentsStream(node).collect(Collectors.groupingBy(Equipments::getEquipTypeConfiged, Collectors.mapping(e -> e, Collectors.toCollection(LinkedList::new))));
    }*/

    /*  public EnumMap<CardType, List<Map<String, TerminationPoint>>> getCardTps(Node node) {
          Map<String, List<TerminationPoint>> equipTpMap = getEquipTpMap(node);

          EnumMap<CardType, List<Map<String, TerminationPoint>>> result = new EnumMap<>(CardType.class);
          for (Equipments equipment : getCardEquipmentsStream(node).collect(Collectors.toList())) {
              CardType cardType = CardType.fromValue(equipment.getEquipTypeConfiged());
              List<Map<String, TerminationPoint>> tps = result.get(cardType);
              if (tps == null) {
                  tps = new LinkedList<>();
              }
              List<TerminationPoint> equipTps = equipTpMap.get(equipment.getEquipmentId());
              if (equipTps == null || equipTps.isEmpty()) {
                  continue;
              }

              tps.add(getPortNameTpMap(equipTps));
          }

          return result;
      }
  */
    public Map<String, String> getPortNameTpIdMap(List<String> equipTps) {
        return equipTps.stream().collect(Collectors.toMap(tpId -> NameGenerator.getPortNameByTpId(tpId), tp -> tp, (first, second) -> first));
    }

    /**
     * Provide structured data can be used by ConstructRepo easily.
     *
     * @param node
     * @return
     */
    public NodeConstructInfo getNodeConstructInfo(Node node) {

        /*output*/
        List<Equipments> nonEmptyEquipments = new ArrayList<>();
        Set<Integer> lineCardUsedSlots = new HashSet<>();
        Map<String, List<Map<String, String>>> idleCardTps = new HashMap<>();
        Map<Integer, Equipments> availableEmptyEquipmentMap = new HashMap<>();
        List<TerminationPoint> tpList = node.getTerminationPoint();

        /*input*/
        List<Equipments> equipments = node.getAugmentation(Node1.class).getPhysical().getEquipments();
        Map<String, List<String>> idleEquipTpIdsMap = getIdleEquipTpMap(node);

        if (equipments != null) {
            for (Equipments equipment : equipments) {
                if (equipment.getEquipType().equals(EquipType.EMPTY)) {
                    availableEmptyEquipmentMap.put(Integer.parseInt(equipment.getSlot()), equipment);
                } else {
                    nonEmptyEquipments.add(equipment);
                    if ((!equipment.getEquipType().equals(EquipType.Other)) && (!equipment.getEquipType().equals(EquipType.TRANSCEIVER))) {
                        lineCardUsedSlots.addAll(equipmentRepo.getLineCardUsedSlots(equipment));
                        String cardType = equipment.getEquipTypeConfiged();
                        List<Map<String, String>> tps = idleCardTps.get(cardType);
                        if (tps == null) {
                            tps = new ArrayList<>();
                        }
                        List<String> equipTps = idleEquipTpIdsMap.get(equipment.getEquipmentId());
                        if (equipTps != null && !equipTps.isEmpty()) {
                            tps.add(getPortNameTpIdMap(equipTps));
                            idleCardTps.put(cardType, tps);
                        }
                    }
                }
            }
        }
        return NodeConstructInfo.builder()
                .nonEmptyEquipments(nonEmptyEquipments)
                .lineCardUsedSlots(lineCardUsedSlots)
                .idleCardTps(idleCardTps)
                .availableEmptyEquipmentMap(availableEmptyEquipmentMap)
                .tpList(tpList)
                .build();
    }


    public List<InternalLinks> getInternalLinks(Node node) {

        List<InternalLinks> internalLinks = node.getAugmentation(Node1.class).getPhysical().getInternalLinks();
        if (internalLinks == null) {
            return new ArrayList<>();
        }
        return internalLinks;

    }

    public List<CrossConnections> getXcs(Node node) {
        List<CrossConnections> xcs = node.getAugmentation(Node1.class).getPhysical().getCrossConnections();
        if (xcs == null) {
            return new ArrayList<>();
        }
        return xcs;
    }

    /**
     * This method is for OT card only. 获取L口的OSlink，以L口的tp为key
     *
     * @param node
     * @return
     */
    public Map<String, InternalLinks> getOtOsLinkGroupByLPort(Node node) {
        List<InternalLinks> internalLinks = getInternalLinks(node);
        return internalLinks.stream().filter(item -> item.getLinkType().equals(LinkType.OsLink))
                .collect(Collectors.toMap(item -> item.getSrcTp().contains("MUX") ? item.getDstTp() : item.getSrcTp(), Function.identity()));
    }

    /**
     * This method is for OT card only. 因为OT卡的交叉总是从C口到L口，并且source和dest都只有一个。 如果将来发现OT卡交叉特性有变，再重构此方法。
     * <p>
     * 此方法只包括C口到L口的交叉， 比如L口-m1d1，不包括在类
     *
     * @param node
     * @return
     */
    public Map<String, List<CrossConnections>> getOtXcsGroupByLPortTp(Node node) {
        List<CrossConnections> xcs = getXcs(node);
        return xcs.stream()
                .filter(item -> item.getCrossConnectionId().getValue().contains(OT_XC_KEYWORD))
                .collect(Collectors.groupingBy(xc -> xc.getDestinationTp().get(0).getTpRef().getValue()));
    }

    public List<CrossConnections> getOtXcsByLPortTp(Node node, String lPortTp) {
        List<CrossConnections> xcs = getXcs(node);
        return xcs.stream().filter(item -> item.getCrossConnectionId().getValue().contains(lPortTp)).collect(Collectors.toList());
    }

    /**
     * This method is for OT card only. 因为OT卡的交叉总是从C口到L口，并且source和dest都只有一个。 如果将来发现OT卡交叉特性有变，再重构此方法。
     *
     * @param node
     * @return
     */
    public Map<String, Long> getOtXcCountGroupByLPortTp(Node node) {
        List<CrossConnections> xcs = getXcs(node);
        return getOtXcCountGroupByLPortTp(xcs);
    }

    /**
     * This method is for OT card only. 因为OT卡的交叉总是从C口到L口，并且source和dest都只有一个。 如果将来发现OT卡交叉特性有变，再重构此方法。
     *
     * @param xcs
     * @return
     */
    public Map<String, Long> getOtXcCountGroupByLPortTp(List<CrossConnections> xcs) {
        return xcs.stream().filter(item -> item.getCrossConnectionId().getValue().contains(OT_XC_KEYWORD))
                .collect(Collectors.groupingBy(xc -> xc.getDestinationTp().get(0).getTpRef().getValue(), counting()));
    }


    public Card getEmptyCard(Node node) throws NeDesignerException {
        return getNeInfo(node).getEmptyCard();
    }

    public List<TerminationPoint> getTps(Node node) {
        return node.getTerminationPoint();
    }


    public Boolean isStuffed(List<Equipments> equipments) {
        return !equipments.stream().filter(item -> item.getEquipType().equals(EquipType.EMPTY)).findAny().isPresent();
    }

    public Boolean isStuffed(Node node) {
        // The node-level stuffed flag is derived from equipments when nodes are created or refreshed.
        // Some imported/config nodes can have a stale stuffed flag, so allocation checks should follow
        // the actual EMPTY equipment list to avoid skipping reusable slots.
        return isStuffed(node.getAugmentation(Node1.class).getPhysical().getEquipments());
    }


    public Map<String, OCMGripGroups> getSlotOcmGroupsMap(Node node) throws NeDesignerException {
        try {

            List<OCMGripGroups> ocmGripGroups = node.getAugmentation(Node1.class).getPhysical().getOCMGripGroups();
            if (ocmGripGroups == null) {
                return Collections.EMPTY_MAP;
            }
            return ocmGripGroups.stream().collect(Collectors.toMap(OCMGripGroup::getSlot, Function.identity()));
        } catch (NullPointerException e) {
            log.error("Failed to get OCMGripGroups for node: {}", node, e);
            throw new NeDesignerException("Failed to get OCMGripGroups for node: " + node.getNodeId().getValue(), e);
        }
    }


    public List<OCMGripGroups> getOcmGroups(Node node) throws NeDesignerException {
        try {
            List<OCMGripGroups> result = node.getAugmentation(Node1.class).getPhysical().getOCMGripGroups();
            if (result == null) {
                result = new ArrayList<>();
            }
            return result;
        } catch (NullPointerException e) {
            log.error("Failed to get OCMGripGroups for node: {}", node, e);
            throw new NeDesignerException("Failed to get OCMGripGroups for node: " + node.getNodeId().getValue(), e);
        }
    }

    public boolean hasCmuxMux(Node node) throws NeDesignerException {
        return getEquipments(node).stream()
                .filter(item -> item.getEquipType() != null && (item.getEquipType().equals(EquipType.CMUX64) || item.getEquipType().equals(EquipType.MUX)))
                .findAny()
                .isPresent();
    }

    /**
     * 满足以下条件才是stuff：
     * <p>
     * 1. 没有空槽位了
     * <p>
     * 2. 如果有OT卡，L口没有间隙可用
     * <p>
     * 也就是说，不能再在此node上面创建tunnel了
     *
     * @param xcs
     * @param equipments
     * @return
     */
    public Boolean checkIsStuffed(List<CrossConnections> xcs, List<Equipments> equipments, List<TerminationPoint> tps, String vendorName, String vendorType) {
        if (equipments.stream().filter(Equipments::isEmpty).findAny().isPresent()) {
            return false;
        }
        Map<String, Equipments> otEquips = equipments.stream()
                .filter(item -> item.getEquipType().equals(EquipType.OT))
                .collect(Collectors.toMap(Equipments::getEquipmentId, Function.identity()));
        if (otEquips == null || otEquips.isEmpty()) {
            //没有OT卡，也没有empty卡,也就是说OPC卡插满了
            return true;
        }

        for (TerminationPoint tp : tps) {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical physical = tp.getAugmentation(TerminationPoint1.class)
                    .getPhysical();

            if (!physical.getPortType().equals(PortType.OTULine)) {//过滤L口
                continue;
            }

            //L口空闲
            if (physical.getOtuLine() == null) {
                return false;
            }
            String equipId = physical.getEquipmentRef();
            Equipments equip = otEquips.get(equipId);
            if (equip == null) {
                log.error("Invalid node with wrong OT equip:{}, set stuff as true.", equipId);
                return true;
            }

            //下面对于这种非法node，stuff都设置为true，就是避免再被重用
            Card otCardInfo = null;
            try {
                otCardInfo = neInfoConfig.getNeInfo(vendorName, vendorType, NodeType.TD.name()).getCardByCardVendor(equipmentRepo.getCardVendorType(equip));
            } catch (NeDesignerException e) {
                log.error("Invalid node, because failed to get json definition for OT equip:{}, set stuff as true.", equipId);
                return true;
            }

            String lPortName = PhysicalTpIdNamingRule.getPortNameByTpId(tp.getTpId().getValue());
            CrossConnection xcInfo = null;
            try {
                xcInfo = tunnelUtils.getOtXcInfo(otCardInfo, lPortName, physical.getOtuLine().getSignalRate(), physical.getOtuClient()
                        .getSignalRate(), equip.getServiceType());
            } catch (NeDesignerException e) {
                log.error("Invalid node, because failed to get XC json definition for OT equip:{}, set stuff as true.", equipId);
                return true;
            }

            List<String> lPortXcLayers = null;
            try {
                lPortXcLayers = NeInfoUtil.getXcLayers(xcInfo.getTo().getLayer());//这个L口可以创建多少条交叉
            } catch (NeDesignerException e) {
                log.error("Invalid node, because get invalid XC json definition for OT equip:{}, set stuff as true.", equipId);
                return true;
            }
            Map<String, Long> xcCountGroupByLportTp = getOtXcCountGroupByLPortTp(xcs);
            Long tpXcs = xcCountGroupByLportTp.get(tp);
            if (tpXcs == null || tpXcs < lPortXcLayers.size()) {
                return false;
            }
        }
        return true;
    }

    public String getMuxEquipIdByOsLink(InternalLinks osLink) {
        String muxTp = osLink.getSrcTp().contains("MUX") ? osLink.getSrcTp() : osLink.getDstTp();
        return PhysicalTpIdNamingRule.getEquipId(muxTp);
    }

    public long getEmptySlotCount(Node node) throws NeDesignerException {
        try {
            return getEquipments(node).stream().filter(item -> item.isEmpty()).count();
        } catch (Exception e) {
            String msg = String.format("Invalid node: %s", node.getNodeId().getValue());
            log.error(msg, e);
            throw new NeDesignerException(msg);
        }
    }

    public Set<String> getEquipIdsByCardType(Node node, String cardType) {
        return node.getAugmentation(Node1.class).getPhysical().getEquipments().stream().filter(item -> item.getEquipTypeConfiged().equals(cardType))
                .map(Equipments::getEquipmentId)
                .collect(Collectors.toSet());
    }

    public Set<String> getEquipIdsByCardVendor(Node node, String cardVendor, SERVICETYPE serviceType) {
        return node.getAugmentation(Node1.class).getPhysical().getEquipments().stream()
                .filter(item -> item.getEquipTypeVendorSpecific() != null && item.getEquipTypeVendorSpecific().equals(cardVendor)
                        && (item.getServiceType() == null || item.getServiceType() == serviceType))
                .map(Equipments::getEquipmentId)
                .collect(Collectors.toSet());
    }

    public Set<Integer> getEmptySlot(Node node) throws NeDesignerException {
        try {
            return getEquipments(node).stream().filter(item -> item.isEmpty()).map(item -> Integer.parseInt(item.getSlot())).collect(Collectors.toSet());
        } catch (Exception e) {
            String msg = String.format("Invalid node: %s", node.getNodeId().getValue());
            log.error(msg, e);
            throw new NeDesignerException(msg);
        }
    }

    public boolean isSameEquip(TerminationPoint aNodeTp, TerminationPoint zNodeTp) {
        return getEquipIdByTp(aNodeTp).equals(getEquipIdByTp(zNodeTp));
    }

    private String getEquipIdByTp(TerminationPoint tp) {
        return tp.getAugmentation(TerminationPoint1.class).getPhysical().getEquipmentRef();
    }

    public Card getCardInfoByTpId(Node node, String tpId) throws NeDesignerException {
        Optional<TerminationPoint> tpOptional = node.getTerminationPoint().stream().filter(tp -> tp.getTpId().getValue().equals(tpId)).findAny();
        if (!tpOptional.isPresent()) {
            String msg = String.format("Failed to find tp %s, in node:%s", tpId, node.getNodeId().getValue());
            log.error(msg);
            throw new NeDesignerException(msg);
        }
        String equipId = getEquipIdByTp(tpOptional.get());
        return getCardInfoByEquipId(node, equipId);
    }

    public Card getCardInfoByEquipId(Node node, String equipId) throws NeDesignerException {
        Optional<Equipments> equipmentsOptional = getEquipments(node).stream().filter(equipment -> equipment.getEquipmentId().equals(equipId)).findAny();
        if (!equipmentsOptional.isPresent()) {
            String msg = String.format("Failed to find equipment: %s, in node:%s", equipId, node.getNodeId().getValue());
            log.error(msg);
            throw new NeDesignerException(msg);
        }
        Equipments equipment = equipmentsOptional.get();
//        String cardType = equipment.getEquipTypeConfiged();
        String cardVendorType = equipment.getEquipTypeVendorSpecific();//need to refactor to cardVendorType later for DX

        NeInfo neInfo = getNeInfo(node);
        return neInfo.getCardByCardVendor(cardVendorType);
    }

    private NeInfo getNeInfo(Node node) throws NeDesignerException {
        Physical physical = node.getAugmentation(Node1.class).getPhysical();
        String vendorName = physical.getVendorName();
        String vendorType = physical.getVendorType();
        return neInfoConfig.getNeInfo(vendorName, vendorType, physical.getNodeType().name());

    }

    public String getTpIdByTpFriendlyName(Node node, String tpFriendlyName) {
        Optional<TerminationPoint> tpOptinal = node.getTerminationPoint().stream()
                .filter(tp -> tp.getAugmentation(TerminationPoint1.class).getPhysical().getFriendlyName().equals(tpFriendlyName)).findAny();
        if (!tpOptinal.isPresent()) {
            String msg = String.format("Failed to get tp by tp friendly name:%s in node:%s", tpFriendlyName, node.getNodeId().getValue());
            log.error(msg);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, msg);
        }
        return tpOptinal.get().getTpId().getValue();
    }

    public Equipments getLineCardEquipBySlot(Node node, Integer slot) {
        Optional<Equipments> optional = getLineCardEquipments(node).stream().filter(equip -> equip.getSlot().equals(slot.toString())).findAny();

        return optional.isPresent() ? optional.get() : null;
    }

    public boolean isLPortTp(TerminationPoint tp) {
        return tp.getAugmentation(TerminationPoint1.class).getPhysical().getPortType().equals(PortType.OTULine);
    }

    public NeInfo getNeInfoByNode(Node node, NodeType nodeType) throws NeDesignerException {

        String vendorName = node.getAugmentation(Node1.class).getPhysical().getVendorName();
        String vendorType = node.getAugmentation(Node1.class).getPhysical().getVendorType();
        return neInfoConfig.getNeInfo(vendorName, vendorType, nodeType.name());
    }

    private String getPropertyByName(Node node, String propertyName) {
        Properties properties = node.getAugmentation(Node1.class).getPhysical().getProperties();
        if (properties != null) {
            List<Property> properTyList = properties.getProperty();
            for (Property property : properTyList) {
                if (property.getName().equals(propertyName)) {
                    return property.getValue();
                }
            }
        }
        return null;
    }

    public PortType getTpPortType(TerminationPoint tp) {
        return tp.getAugmentation(TerminationPoint1.class).getPhysical().getPortType();
    }

//    public Pair<Integer, Integer> getAvailableSlotPair(Node node, Card card1, Card card2) throws NeDesignerException {
//        Set<Integer> usedSlots = getLineCardUsedSlots(node);
//        List<Integer> possibleSlots = card1.getPossibleSlot();
//        List<Integer> possibleSlots2 = card2.getPossibleSlot();
//
//        for (Integer slot : possibleSlots) {
//            Set<Integer> newCard1Slots = equipmentRepo.getEquipUsedSlots(slot, card1.getWidth(), card1.getHeight());
//            if (usedSlots.stream().anyMatch(newCard1Slots::contains)) {
//                continue;
//            }
//
//            //check card2
//            for (Integer card2Slot : possibleSlots2) {
//                Set<Integer> newCard2Slots = equipmentRepo.getEquipUsedSlots(card2Slot, card2.getWidth(), card2.getHeight());
//                if (usedSlots.stream().anyMatch(newCard2Slots::contains)) {
//                    continue;
//                }
//                if (newCard1Slots.stream().anyMatch(newCard2Slots::contains)) {
//                    continue;
//                }
//                return Pair.of(slot, card2Slot);
//            }
//        }
//
//        throw new NeDesignerException(
//                String.format("There is no slot available, for card:%s with possible slots:%s, and  card:%s with possible slots:%s,for node %s.", card1.getVendorType(), card1
//                                .getPossibleSlot(),
//                        card2.getVendorType(), card2.getPossibleSlot(),
//                        node.getNodeId().getValue()));
//
//    }

    public String getIp(Node node) {
        String ip = node.getAugmentation(Node1.class).getPhysical().getIp();
        if (ip == null || ip.isEmpty()) {
            return null;
        }
        return ip;
    }

    public Set<Integer> getLineCardUsedSlots(List<Equipments> equipmentsList) {
        if (equipmentsList == null || equipmentsList.isEmpty()) {
            return new HashSet<>();
        }
        return equipmentsList.stream().flatMap(item -> equipmentRepo.getLineCardUsedSlots(item).stream()).
                collect(Collectors.toSet());
    }

    public String getMpoPortName(Node siteLinkNode, String mdTpId) throws NeDesignerException {
        Optional<Equipments> muxpanelOptional = siteLinkNode.getAugmentation(Node1.class).getPhysical().getEquipments().stream()
                .filter(equipments -> equipments.getEquipType().equals(EquipType.MUXPANEL)).findAny();
        if (!muxpanelOptional.isPresent()) {
            Optional<Equipments> muxOptional = siteLinkNode.getAugmentation(Node1.class).getPhysical().getEquipments().stream()
                    .filter(equipments -> equipments.getEquipType().equals(EquipType.MUX)).findAny();
            if (muxOptional.isPresent()) {
                return null;
            }
            throw new NeDesignerException("Failed to getMpoPortName, because failed to find muxpanel in sitelink node:" + siteLinkNode.getNodeId().getValue());
        }
        String muxPanelCardType = muxpanelOptional.get().getEquipTypeConfiged();
        NeInfo neInfo = getNeInfoByNode(siteLinkNode, NodeType.OD);
        Card muxPanelCardInfo = neInfo.getCardByCardType(muxPanelCardType);
        String mdPortName = PhysicalTpIdNamingRule.getPortNameByTpId(mdTpId);
        return NeInfoUtil.getMpoPortName(muxPanelCardInfo, mdPortName);
    }

    public Node sortTp(Node node) {
        Map<String, TerminationPoint> tpMap = node.getTerminationPoint().stream()
                .collect(Collectors.toMap(tp -> PhysicalTpIdNamingRule.getShortTpByTpId(tp.getTpId().getValue()), Function.identity()));
        List<String> sortedTps = tpMap.keySet().stream().sorted(new PortNameComparator()).collect(Collectors.toList());
        List<TerminationPoint> newTps = new ArrayList<>();
        for (int i = 0; i < sortedTps.size(); i++) {
            String tpKey = sortedTps.get(i);
            TerminationPoint oldTp = tpMap.get(tpKey);
            Properties oldProperties = oldTp.getAugmentation(TerminationPoint1.class).getPhysical().getProperties();
            Property indexProperty = new PropertyBuilder().setName(INDEX)
                    .setValue(String.valueOf(i))
                    .build();
            List<Property> newPropertyList = new ArrayList<>();
            if (oldProperties != null && oldProperties.getProperty() != null) {
                newPropertyList.addAll(oldProperties.getProperty());
            }

            newPropertyList.add(indexProperty);
            Properties newProperties = new PropertiesBuilder().setProperty(newPropertyList).build();

            TerminationPoint newTp = new TerminationPointBuilder(oldTp)
                    .addAugmentation(TerminationPoint1.class,
                            new TerminationPoint1Builder(oldTp.getAugmentation(TerminationPoint1.class))
                                    .setPhysical(new PhysicalBuilder(oldTp.getAugmentation(TerminationPoint1.class).getPhysical()).
                                            setProperties(newProperties)
                                            .build()).build()).build();
            newTps.add(newTp);
        }
        return new NodeBuilder(node)
                .setTerminationPoint(newTps)
                .build();
    }

    public Set<String> getConnectedSiteLinkNodeIds(Node otNode) {
        Set<String> result = new HashSet<>();
        String nodeId = otNode.getNodeId().getValue();
        List<InternalLinks> internalLinks = otNode.getAugmentation(Node1.class).getPhysical().getInternalLinks();
        if (internalLinks != null) {
            List<InternalLinks> sitelinkConnect = internalLinks.stream()
                    .filter(internalLink -> !internalLink.getSrcTp().contains(nodeId) || !internalLink.getDstTp().contains(nodeId)).collect(Collectors.toList());
            for (InternalLinks inLink : sitelinkConnect) {
                if (inLink.getSrcTp().contains(nodeId)) {
                    result.add(PhysicalTpIdNamingRule.getNodeId(inLink.getDstTp()));
                    continue;
                }
                result.add(PhysicalTpIdNamingRule.getNodeId(inLink.getSrcTp()));
            }
        }
        return result;
    }

    public Set<String> getWssUsedExpTpIds(Node node) {
        return getInternalLinks(node).stream().filter(internalLinks -> internalLinks.getLinkType().equals(LinkType.WssLink)).map(internalLinks -> internalLinks.getSrcTp()).collect(Collectors.toSet());
    }
    public Set<String> getWssLinkIds(Node node) {
        return getInternalLinks(node).stream().filter(internalLinks -> internalLinks.getLinkType().equals(LinkType.WssLink)).map(internalLinks -> internalLinks.getLinkRef()).collect(Collectors.toSet());
    }

    public Node getNodeCopy(Node node) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder physicalBuilder = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder(node.getAugmentation(Node1.class).getPhysical())
                .setInternalLinks(new ArrayList<>(node.getAugmentation(Node1.class).getPhysical().getInternalLinks()));

        Node1 phyNode = new Node1Builder().setPhysical(physicalBuilder.build()).build();
        List<TerminationPoint> tps = new ArrayList<>(node.getTerminationPoint());
        return new NodeBuilder()
                .setNodeId(node.getNodeId())
                .setKey(new NodeKey(node.getNodeId()))
                .addAugmentation(Node1.class, phyNode)
                .setTerminationPoint(tps)
                .build();
    }
    public WDM_Band getWdmBand(Node node) throws NeDesignerException {
        Optional<Equipments> muxEqupOption = getEquipments(node).stream().filter(e -> e.getEquipType().equals(EquipType.MUXPANEL)).findAny();
        if(!muxEqupOption.isPresent()){
            log.error("Failed to get MUXPANEL equipments for node: {}", node);
            throw new NeDesignerException("Failed to get MUXPANEL equipments for node: " + node.getNodeId().getValue());
        }

        if(muxEqupOption.get().getProperties()!=null&&muxEqupOption.get().getProperties().getProperty()!=null){
            Optional<Property> wdmOption = muxEqupOption.get().getProperties().getProperty().stream().filter(p -> p.getName().equals("WdmBand")).findAny();
            if(wdmOption.isPresent()){
              return WDM_Band.fromString( wdmOption.get().getValue());
            }
        }
        log.error("Failed to get wdmband  for node: {}", node);
        throw new NeDesignerException("Failed to get wdmband for node: " + node.getNodeId().getValue());

    }
}

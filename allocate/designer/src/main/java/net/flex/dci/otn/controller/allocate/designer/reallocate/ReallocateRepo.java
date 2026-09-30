package net.flex.dci.otn.controller.allocate.designer.reallocate;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.NodeUtils;
import net.flex.dci.otn.controller.allocate.designer.model.Route;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import net.flex.dci.otn.controller.allocate.designer.model.site.ReallocateDataModel;
import net.flex.dci.otn.controller.allocate.designer.ne.EquipmentRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.NeNodeRepo;
import net.flex.dci.otn.controller.allocate.ne.Card;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.attributes.OCMGripGroups;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.attributes.OCMGripGroupsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReallocateRepo {

    @Autowired
    private ReallocateXcRepo reallocateXcRepo;
    @Autowired
    private ReallocateLinkRepo reallocateLinkRepo;
    @Autowired
    private ReallocateEquipRepo reallocateEquipRepo;
    @Autowired
    private ReallocateTpRepo reallocateTpRepo;
    @Autowired
    private ReInternalLinkRepo reInternalLinkRepo;
    @Autowired
    private ReusedNodeRepo reusedNodeRepo;


    @Autowired
    private EquipmentRepo equipmentRepo;
    @Autowired
    private NeNodeRepo neNodeRepo;

    @Autowired
    private NodeUtils nodeUtils;


    private Route reallocateRoute(List<String> nodeIds, Map<String, Map<String, String>> reallocateNodeEquipMap, Map<String, Node> reusedNodesSnapshotMap,
            HashMap<String, CrossConnections> newTotalXcs, HashMap<String, Link> newTotalLinks,
            LinkedHashMap<String, CrossConnections> newRouteXcs, LinkedHashMap<String, Link> newRouteLinks, Map<String, Node> computeNodes)
            throws NeDesignerException {

        Set<String> oldNodeSet = reallocateNodeEquipMap.keySet();

        Set<String> outputNodeIds = new HashSet<>();
        Set<String> unchangedNodeIds = new HashSet<>();//没变的node,这里的没变是指没有移走equip
        Map<String, List<Equipments>> reusedNodeEquipMap = new HashMap<>();
        Map<String, List<TerminationPoint>> reusedNodeTpMap = new HashMap<>();
        Map<String, List<OCMGripGroups>> reusedNodeOcmGroupMap = new HashMap<>();

        /** 处理老方案中的node: start **/
        for (String nodeId : nodeIds) {
            Node node = computeNodes.get(nodeId);

            if (node == null) {
                String msg = String.format("Failed to find node: %s in computeNodes map", nodeId);
                log.error(msg);
                throw new NeDesignerException(msg);
            }

            //这个node没有改变
            if (!oldNodeSet.contains(nodeId)) {
                unchangedNodeIds.add(nodeId);
                continue;
            }

            Map<String, String> oldEquipIdMap = reallocateNodeEquipMap.get(nodeId);
            Set<String> oldEquipIds = oldEquipIdMap.keySet();

            Set<Integer> removedSlots = new HashSet<>();//记录删除的slots，是为了后续： 如果node为清空，则需增加empty card
            Set<Integer> newUsedSlots = new HashSet<>();//如果reallocate发生在node内部（即只换了下slot），则记录新的的used slots，是为了后续： 如果node为清空，则需增加empty card

            /**Reallocate equipment(remove/update equipment) and TPs: Start **/
            Map<String, Equipments> reallocateEquipInsideNode = new HashMap<>(); //<oldEquipId,newEquip>
            Map<String, Equipments> reallocateEquipOutsideNode = new HashMap<>(); //<oldEquipId,newEquip>
            Boolean isNodeEmpty = true;
            List<Equipments> oldEquips = nodeUtils.getEquipments(node);
            Map<String, OCMGripGroups> oldOcmGroupMap = nodeUtils.getSlotOcmGroupsMap(node);//slot is the key
            Map<String, List<Equipments>> oldTransceiverMap = nodeUtils.getTransceiversGroupBySlots(node);

            List<Equipments> oldEmptyEquips = new ArrayList<>();
            List<Equipments> newEquips = new ArrayList<>();
            for (Equipments oldEquip : oldEquips) {

                if (oldEquip.getEquipType().equals(EquipType.TRANSCEIVER)) {
                    //后面处理
                    continue;
                }

                if (oldEquip.getEquipType().equals(EquipType.EMPTY)) {
                    //Empty card在后面更新node时再处理，如果这个node在reallocate以后仍然插有卡，不为空
                    oldEmptyEquips.add(oldEquip);
                    continue;
                }

                if (oldEquip.getEquipType().equals(EquipType.Other)) {
                    newEquips.add(oldEquip);
                    continue;
                }

                String oldEquipId = oldEquip.getEquipmentId();
                String newEquipId = oldEquipIdMap.get(oldEquipId);

                //此equip未被抽走，保持不变
                if (newEquipId == null) {
                    isNodeEmpty = false;
                    newEquips.add(oldEquip);
                    continue;
                }

                String newNodeId = PhysicalNodeIdNamingRule.getNodeId(newEquipId);
                Equipments newEquip = reallocateEquipRepo.reallocateEquip(oldEquip, newEquipId, newNodeId);
                removedSlots.addAll(equipmentRepo.getLineCardUsedSlots(oldEquip));
                List<Equipments> newTransceivers = new ArrayList<>();
                List<Equipments> oldTransceivers = oldTransceiverMap.get(oldEquip.getSlot());
                if (oldTransceivers != null && !oldTransceivers.isEmpty()) {//证明以前的那个equip是带有transceiver的，因此transceiver也要reallocate
                    for (Equipments oldTransceiver : oldTransceivers) {
                        newTransceivers.add(reallocateEquipRepo.reallocateTransceiver(oldTransceiver, newEquip.getSlot(), newNodeId));
                    }
                }

                //更新ocmGroup
                OCMGripGroups newOcmGroup = null;
                String oldOcmSlot = PhysicalEqpIdNamingRule.getOcmSlot(oldEquip.getEquipmentId());
                OCMGripGroups oldOcmGroup = oldOcmGroupMap.get(oldOcmSlot);
                oldOcmGroupMap.remove(oldOcmSlot);
                if (oldOcmGroup != null) {
                    String newOcmSlot = PhysicalEqpIdNamingRule.getOcmSlot(newEquip.getEquipmentId());
                    newOcmGroup = new OCMGripGroupsBuilder().setSlot(newOcmSlot).setChannels(oldOcmGroup.getChannels()).setIndex(oldOcmGroup.getIndex()).build();
                }

                if (newNodeId.equals(nodeId)) {//同一个node，只是换了slot
                    isNodeEmpty = false;
                    newEquips.add(newEquip);
                    if (!newTransceivers.isEmpty()) {
                        newEquips.addAll(newTransceivers);
                    }

                    Set<Integer> lineCardUsedSlots = equipmentRepo.getLineCardUsedSlots(newEquip);
                    newUsedSlots.addAll(lineCardUsedSlots);

                    reallocateEquipInsideNode.put(oldEquipId, newEquip);

                    if (newOcmGroup != null) {
                        oldOcmGroupMap.put(newEquip.getSlot(), newOcmGroup);
                    }
                } else {
                    //new equip在其他node了
                    reallocateEquipOutsideNode.put(oldEquipId, newEquip);

                    List<Equipments> reusedNodeEquips = reusedNodeEquipMap.get(newNodeId);
                    if (reusedNodeEquips == null) {
                        reusedNodeEquips = new ArrayList<>();
                    }
                    reusedNodeEquips.add(newEquip);
                    if (!newTransceivers.isEmpty()) {
                        reusedNodeEquips.addAll(newTransceivers);
                    }
                    reusedNodeEquipMap.put(newNodeId, reusedNodeEquips);

                    if (newOcmGroup != null) {
                        List<OCMGripGroups> reusedNodeOcmGroups = reusedNodeOcmGroupMap.get(newNodeId);
                        if (reusedNodeOcmGroups == null) {
                            reusedNodeOcmGroups = new ArrayList<>();
                        }
                        reusedNodeOcmGroups.add(newOcmGroup);
                        reusedNodeOcmGroupMap.put(newNodeId, reusedNodeOcmGroups);
                    }
                }
            }

            //reallocate TP
            List<TerminationPoint> oldTps = nodeUtils.getTps(node);
            List<TerminationPoint> newTps = new ArrayList<>();

            for (TerminationPoint oldTp : oldTps) {
                String oldTpId = oldTp.getTpId().getValue();
                Boolean needRemoved = false;
                for (String oldEquipId : oldEquipIds) {
                    if (oldTpId.contains(oldEquipId)) {
                        Equipments newEquip = reallocateEquipInsideNode.get(oldEquipId);
                        if (newEquip != null) {
                            TerminationPoint newTp = reallocateTpRepo.reallocateTp(oldTp, newEquip);
                            newTps.add(newTp);

                        } else {//新的TP在其他node
                            newEquip = reallocateEquipOutsideNode.get(oldEquipId);
                            TerminationPoint newTp = reallocateTpRepo.reallocateTp(oldTp, newEquip);

                            String newNodeId = PhysicalNodeIdNamingRule.getNodeId(newEquip.getEquipmentId());
                            List<TerminationPoint> reusedNodeTps = reusedNodeTpMap.get(newNodeId);
                            if (reusedNodeTps == null) {
                                reusedNodeTps = new ArrayList<>();
                            }
                            reusedNodeTps.add(newTp);
                            reusedNodeTpMap.put(newNodeId, reusedNodeTps);

                        }
                        needRemoved = true;
                        break;
                    }
                }
                if (!needRemoved) {
                    newTps.add(oldTp);//此TP未被改变}
                }
            }
            /**Reallocate equipment(remove/update equipment) and TPs: End **/

            /**给空出来的slot创建空白卡,更新tp，xc，internalLink。 Start **/
            Boolean isStuffed = true;
            //旧的empty card所在槽位未被占用，则添加
            for (Equipments emptyCard : oldEmptyEquips) {
                if (!newUsedSlots.contains(Integer.parseInt(emptyCard.getSlot()))) {
                    isStuffed = false;
                    newEquips.add(emptyCard);
                }
            }
            /**对于被remove了的板卡有两种场景需要处理：
             1. create empty card
             2. remove transceiver
             **/
            Card emptyCard = nodeUtils.getEmptyCard(node);
            for (Integer slot : removedSlots) {
                if (!newUsedSlots.contains(slot)) {
                    isStuffed = false;
                    newEquips.add(equipmentRepo.createEmptyEquipment(nodeId, slot, emptyCard));
                }
            }

            //remove transceiver
            for (Entry<String, List<Equipments>> entry : oldTransceiverMap.entrySet()) {
                if (!removedSlots.contains(Integer.parseInt(entry.getKey()))) {
                    newEquips.addAll(entry.getValue());
                }
            }

            //remove/update xc
            List<CrossConnections> oldNodeXcs = nodeUtils.getXcs(node);
            List<CrossConnections> newNodeXcs = reallocateXcRepo.reallocateItemsInNode(nodeId, oldNodeXcs, newTotalXcs);

            //remove/update internal Link
            List<InternalLinks> oldInternalLinks = nodeUtils.getInternalLinks(node);
            List<InternalLinks> newInternalLinks = reInternalLinkRepo.reallocate(nodeId, oldInternalLinks, newTotalLinks);

            Node newNode = neNodeRepo.refreshNode(node, newEquips, newTps, newInternalLinks, newNodeXcs, isStuffed, new ArrayList<>(oldOcmGroupMap.values()));
            computeNodes.put(nodeId, newNode);
            if (!isNodeEmpty) {
                outputNodeIds.add(nodeId);
            }
            /**给空出来的slot创建空白卡,更新tp，xc，internalLink。 End **/

        }
        /** 处理老方案中的node: end **/

        /**处理被利旧的那些node: start **/
        List<Link> newLinksList = new ArrayList<Link>(newTotalLinks.values());
        List<CrossConnections> newXcsList = new ArrayList<CrossConnections>(newTotalXcs.values());
        Map<String, List<CrossConnections>> nodeXcsMap = newXcsList.stream().collect(Collectors.groupingBy(item -> item.getNodeRef().getValue()));

        for (String nodeId : reusedNodeEquipMap.keySet()) {
            List<TerminationPoint> newTps = reusedNodeTpMap.get(nodeId);
            List<Equipments> newEquips = reusedNodeEquipMap.get(nodeId);
            List<CrossConnections> newReusedNodeXcs = nodeXcsMap.get(nodeId);
            List<InternalLinks> newInternalLinks = reInternalLinkRepo.getInternalLinksByNode(nodeId, newLinksList);

            List<OCMGripGroups> ocmGripGroupsList = reusedNodeOcmGroupMap.get(nodeId);
            Node node = reusedNodesSnapshotMap.get(nodeId);
            if (node == null) {
                node = computeNodes.get(nodeId);
                if (node == null) {
                    String msg = String.format("Failed to find reused node: %s ", nodeId);
                    log.error(msg);
                    throw new NeDesignerException(msg);
                }
            }

            Node updatedNode = reusedNodeRepo.updateNode(node, newEquips, newTps, newReusedNodeXcs, newInternalLinks, ocmGripGroupsList);
            computeNodes.put(nodeId, updatedNode);
            outputNodeIds.add(nodeId);
        }
        /**处理被利旧的那些node: end **/

        /**处理没有变的那些node: start:  这些node，唯一可能被影响的只有internalLink * **/
        for (String nodeId : unchangedNodeIds) {
            Node node = computeNodes.get(nodeId);
            List<InternalLinks> oldInternalLinks = nodeUtils.getInternalLinks(node);
            List<InternalLinks> newInternalLinks = reInternalLinkRepo.reallocate(node.getNodeId().getValue(), oldInternalLinks, newTotalLinks);

            Node updatedNode = neNodeRepo.refreshNode(node, newInternalLinks);
            computeNodes.put(nodeId, updatedNode);
            outputNodeIds.add(nodeId);

        }
        /**处理没有变的那些node: end **/

        /**处理当前route（main/slave）的xc,link**/
        List<Link> newLRouteLinkList = new ArrayList<Link>(newRouteLinks.values());
        List<CrossConnections> newRouteXcList = new ArrayList<CrossConnections>(newRouteXcs.values());

        List<Node> outputNodesList = new ArrayList<>();
        for (String outputNodeId : outputNodeIds) {
            outputNodesList.add(computeNodes.get(outputNodeId));
        }

        //Output
        return Route.builder().links(newLRouteLinkList).xcs(newRouteXcList).nodes(outputNodesList).build();

    }

    public RouteInfo reallocateSite(RouteInfo routeInfo, ReallocateDataModel reallocateDataModel) throws NeDesignerException {
        if (reallocateDataModel == null) {
            return routeInfo;//板卡没有被移动，和compute出来时一样
        }
        Route newMain;
        Route newSlave;
        if (!reallocateDataModel.getReallocateEquipMap().isEmpty()) {

            //Prepare input for reallocateRoute
            Map<String, String> reallocateEquipMap = reallocateDataModel.getReallocateEquipMap();
            Map<String, Map<String, String>> reallocateNodeEquipMap = reallocateDataModel.getReallocateNodeEquipMap();
            Map<String, Node> reusedNodesSnapshotMap = reallocateDataModel.getReusedNodesSnapshot().stream().collect(Collectors.toMap(item -> item.getNodeId().getValue(), Function.identity()));

            //Main
            Route mainRoute = routeInfo.getMain();
            LinkedHashMap<String, CrossConnections> newMainXcs = reallocateXcRepo.reallocate(mainRoute.getXcs(), reallocateEquipMap);
            LinkedHashMap<String, Link> newMainLinks = reallocateLinkRepo.reallocate(mainRoute.getLinks(), reallocateEquipMap); //<oldId,newItem>
            ArrayList<String> mainNodeIds = mainRoute.getNodes().stream().map(item -> item.getNodeId().getValue()).collect(Collectors.toCollection(ArrayList::new));

            HashMap<String, CrossConnections> newTotalXcs = new HashMap<>();
            newTotalXcs.putAll(newMainXcs);
            HashMap<String, Link> newTotalLinks = new HashMap<>();
            newTotalLinks.putAll(newMainLinks);
            LinkedHashMap<String, CrossConnections> newSlaveXcs = null;
            LinkedHashMap<String, Link> newSlaveLinks = null;
            Route slaveRoute = routeInfo.getSlave();

            Map<String, Node> computeNodes = mainRoute.getNodes().stream().collect(Collectors.toMap(item -> item.getNodeId().getValue(), Function.identity()));
            if (slaveRoute != null) {
                newSlaveXcs = reallocateXcRepo.reallocate(slaveRoute.getXcs(), reallocateEquipMap);
                newSlaveLinks = reallocateLinkRepo.reallocate(slaveRoute.getLinks(), reallocateEquipMap);
                newTotalLinks.putAll(newSlaveLinks);
                newTotalXcs.putAll(newSlaveXcs);
                computeNodes.putAll(slaveRoute.getNodes().stream().collect(Collectors.toMap(item -> item.getNodeId().getValue(), Function.identity())));
            }

            //因为main node需要处理slave的xc被移动的情况，所以需要total
            newMain = reallocateRoute(mainNodeIds, reallocateNodeEquipMap, reusedNodesSnapshotMap, newTotalXcs, newTotalLinks, newMainXcs, newMainLinks, computeNodes);
            newSlave = null;
            if (slaveRoute != null) {
                //更新main导致的computeNodes变化
                List<Node> newMainNodes = newMain.getNodes();
                for (Node node : newMainNodes) {
                    computeNodes.put(node.getNodeId().getValue(), node);
                }
                ArrayList<String> slaveNodeIds = slaveRoute.getNodes().stream().map(item -> item.getNodeId().getValue()).collect(Collectors.toCollection(ArrayList::new));
                newSlave = reallocateRoute(slaveNodeIds, reallocateNodeEquipMap, reusedNodesSnapshotMap, newTotalXcs, newTotalLinks, newSlaveXcs, newSlaveLinks, computeNodes);

                //更新slaveRoute中导致变化的main的node
                Map<String, Node> newSlaveNodesMap = newSlave.getNodes().stream().collect(Collectors.toMap(item -> item.getNodeId().getValue(), Function.identity()));
                int newMainNodesLength = newMainNodes.size();
                if (!newSlaveNodesMap.isEmpty()) {
                    for (int i = 0; i < newMainNodesLength; i++) {
                        String nodeId = newMainNodes.get(i).getNodeId().getValue();
                        Node updatedNode = newSlaveNodesMap.get(nodeId);
                        if (updatedNode != null) {
                            newMainNodes.set(i, updatedNode);
                        }
                    }
                }

            }
        } else {
            newMain = routeInfo.getMain();
            newSlave = routeInfo.getSlave();
        }

        //handle vendor type change for card, e.g. OA25 to OA35
        Map<String, String> reallocateEquipVendorMap = reallocateDataModel.getReallocateEquipVendorMap();
        if (!reallocateEquipVendorMap.isEmpty()) {
            newMain = handleVendorTypeChange(reallocateEquipVendorMap, newMain);
            if (newSlave != null && newSlave.getNodes() != null && !newSlave.getNodes().isEmpty()) {
                newSlave = handleVendorTypeChange(reallocateEquipVendorMap, newSlave);
            }
        }

        return RouteInfo.builder().main(newMain).slave(newSlave).build();
    }

    private Route handleVendorTypeChange(Map<String, String> reallocateEquipVendorMap, Route route) throws NeDesignerException {
        List<Node> nodes = route.getNodes();
        List<Node> newNodes = new ArrayList<>();
        for (Node node : nodes) {
            String nodeId = node.getNodeId().getValue();
            Set<String> nodeRelatedEquips = reallocateEquipVendorMap.keySet().stream().filter(equipId -> equipId.contains(nodeId)).collect(Collectors.toSet());
            if (nodeRelatedEquips.isEmpty()) {
                newNodes.add(node);
                continue;
            }

            List<Equipments> equipList = nodeUtils.getEquipments(node);
            List<Equipments> newEquipList = new ArrayList<>();
            for (int i = 0; i < equipList.size(); i++) {
                Equipments equipments = equipList.get(i);
                String equipId = equipments.getEquipmentId();
                if (!nodeRelatedEquips.contains(equipId)) {
                    newEquipList.add(equipments);
                    continue;
                }
                String newVendorType = reallocateEquipVendorMap.get(equipId);
                Equipments newEquip = reallocateEquipRepo.changeEquipVendorSpecific(equipments, newVendorType);
                newEquipList.add(newEquip);
            }

            Node newNode = neNodeRepo.refreshNodeEquips(node, newEquipList);
            newNodes.add(newNode);
        }
        route.setNodes(newNodes);

        return route;
    }


}

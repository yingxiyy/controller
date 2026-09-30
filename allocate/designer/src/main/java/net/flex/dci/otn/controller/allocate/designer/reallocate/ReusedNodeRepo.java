package net.flex.dci.otn.controller.allocate.designer.reallocate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.NodeUtils;
import net.flex.dci.otn.controller.allocate.designer.ne.EquipmentRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.NeNodeRepo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.attributes.OCMGripGroups;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReusedNodeRepo {

    @Autowired
    private NeNodeRepo neNodeRepo;
    @Autowired
    private NodeUtils nodeUtils;
    @Autowired
    private EquipmentRepo equipmentRepo;


    /**
     * 需要考虑当前重用的node，包含tpc卡，所以检查stuffed时要考虑这种情况
     *
     * @param node
     * @param addedEquips
     * @param addedTps
     * @param addedXcs
     * @param addedInternalLinks
     * @param ocmGripGroupsList
     * @return
     * @throws NeDesignerException
     */
    public Node updateNode(Node node, List<Equipments> addedEquips, List<TerminationPoint> addedTps, List<CrossConnections> addedXcs, List<InternalLinks> addedInternalLinks,
            List<OCMGripGroups> ocmGripGroupsList)
            throws NeDesignerException {

        Set<Integer> newUsedSlots = addedEquips.stream().flatMap(item -> equipmentRepo.getLineCardUsedSlots(item).stream()).collect(Collectors.toSet());

        //处理empty卡和isStuffed
        List<Equipments> equips = new ArrayList<>();
        List<Equipments> oldEquips = nodeUtils.getEquipments(node);
        Boolean isStuffed = true;
        List<Equipments> otEquips = new ArrayList<>();
        for (Equipments oldEquip : oldEquips) {
            if (oldEquip.getEquipType().equals(EquipType.EMPTY)) {
                if (!newUsedSlots.contains(Integer.parseInt(oldEquip.getSlot()))) {
                    isStuffed = false;//仍然有empty卡，所以没满
                    equips.add(oldEquip);
                }
                continue;
            }
            equips.add(oldEquip);

            //为后续判断stuff情况作准备
            if (oldEquip.getEquipType().equals(EquipType.OT)) {
                otEquips.add(oldEquip);
            }
        }
        equips.addAll(addedEquips);

        List<InternalLinks> internalLinks = new ArrayList<>();
        internalLinks.addAll(nodeUtils.getInternalLinks(node));
        if (addedInternalLinks != null && !addedInternalLinks.isEmpty()) {
            internalLinks.addAll(addedInternalLinks);
        }

        List<CrossConnections> xcs = new ArrayList<>();
        List<CrossConnections> oldXcs = nodeUtils.getXcs(node);
        xcs.addAll(oldXcs);
        if (addedXcs != null && !addedXcs.isEmpty()) {
            xcs.addAll(addedXcs);
        }

        List<TerminationPoint> oldTps = nodeUtils.getTps(node);
        List<TerminationPoint> tps = new ArrayList<>();
        tps.addAll(oldTps);
        tps.addAll(addedTps);
        List<OCMGripGroups> newOcmGroups = nodeUtils.getOcmGroups(node);
        if (newOcmGroups == null || newOcmGroups.isEmpty()) {
            newOcmGroups = ocmGripGroupsList;
        } else if (ocmGripGroupsList != null) {
            newOcmGroups.addAll(ocmGripGroupsList);
        }

        return neNodeRepo.refreshNode(node, equips, tps, internalLinks, xcs, isStuffed, newOcmGroups);
    }
}

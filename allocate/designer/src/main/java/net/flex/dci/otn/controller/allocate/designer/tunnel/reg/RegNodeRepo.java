/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.tunnel.reg;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfo;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfoUtil;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.RegInput;
import net.flex.dci.otn.controller.allocate.designer.ne.EquipmentRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.NeNodeRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.TpRepo;
import net.flex.dci.otn.controller.allocate.designer.tunnel.OtTpService;
import net.flex.dci.otn.controller.allocate.ne.Card;
import net.flex.dci.otn.controller.allocate.ne.Port;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RegNodeRepo {

    @Autowired
    private EquipmentRepo equipmentRepo;
    @Autowired
    private TpRepo tpRepo;
    @Autowired
    private OtTpService otTpService;
    @Autowired
    private NeNodeRepo neNodeRepo;

    public TerminationPoint getOrCreateCardEquipAndTp(String nodeId, String tpFriendlyName, Map<String, Node> totalNodesMap, RegInput input, Set<String> busyTpSet) throws NeDesignerException {

        //check if tp existed already, then update as busy
        Node node = totalNodesMap.get(nodeId);
        List<TerminationPoint> tpList = node.getTerminationPoint();
        TerminationPoint targetTp = null;
        if (tpList != null) {
            for (int i = 0; i < tpList.size(); i++) {
                TerminationPoint tp = tpList.get(i);
                if (!tp.getAugmentation(TerminationPoint1.class).getPhysical().getFriendlyName().equals(tpFriendlyName)) {
                    continue;
                }

                targetTp = tp;
                String tpId = tp.getTpId().getValue();

                //check if TP is available(not busy)
                if (busyTpSet.contains(tpId)) {
                    return targetTp;//此TP已经处理过了
                }

                if (tp.getAugmentation(TerminationPoint1.class).getPhysical().getConnectionStatus().equals(ConnectionStatus.Busy)) {
                    String msg = String.format("Failed to get TP, because Tp :%s in node:%s is busy already", tpFriendlyName, nodeId);
                    log.error(msg);
                    throw new NeDesignerException(msg);
                }

                tpList.set(i, getUpdatedBusyTp(tp, input));
                break;
            }
        }
        //not found target TP, then try to create equip
        Equipments newEquipment = null;
        if (targetTp == null) {
            //create equip
            String cardVendorType = getCardTypeByTpFriendlyName(tpFriendlyName);
            Integer newEquipSlot = getEquipSlotByTpFriendlyName(tpFriendlyName);

            NeInfo tpcNeInfo = input.getTpcNeInfo();
            Card cardInfo = tpcNeInfo.getCardByCardVendor(cardVendorType);
            if (!cardInfo.getPossibleSlot().contains(newEquipSlot)) {
                String msg = String.format("Failed to create equip from tpFriendlyName:%s for node:%s with tpFriendlyName:%s, because slot:%d is invalid, valid slots should be:%s",
                        tpFriendlyName, nodeId, tpFriendlyName, newEquipSlot, cardInfo.getPossibleSlot());
                log.error(msg);
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
            }

            newEquipment = equipmentRepo.createEquipment(nodeId, newEquipSlot, cardVendorType, tpcNeInfo);
            //create TP
            List<Port> ports = cardInfo.getPorts();
            ports.sort(Comparator.comparing(Port::getName));
            for (Port port : ports) {
                List<String> portNames = NeInfoUtil.getNameList(port.getName());
                List<String> portIndexs = NeInfoUtil.getNameList(port.getIndex());
                int size = portNames.size();
                for (int i = 0; i < size; i++) {
                    String portName = portNames.get(i);
                    String portIndex = portIndexs.get(i);
                    TerminationPoint newTp = tpRepo.createTp(newEquipment, cardVendorType, portName, port,i, Integer.parseInt(portIndex));
                    if (tpFriendlyName.equals(newTp.getAugmentation(TerminationPoint1.class).getPhysical().getFriendlyName())) {
                        newTp = getUpdatedBusyTp(newTp, input);
                        targetTp = newTp;
                    }
                    tpList.add(newTp);
                }
            }
            if (targetTp == null) {
                String msg = String.format("Failed to create tp by tp friendly name:%s, and NE Json file:%s", tpFriendlyName, tpcNeInfo.getJsonFileName());
                log.error(msg);
                throw new NeDesignerException(msg);
            }
        }

        node = neNodeRepo.refreshNodeBy_Tps_newEquip_checkStuffed(node, tpList, newEquipment);
        totalNodesMap.put(nodeId, node);
        busyTpSet.add(targetTp.getTpId().getValue());
        return targetTp;
    }


    /**
     * Here the TP should be L PORT TP or TP of the OP card.
     *
     * @param tp
     * @param input
     * @return
     */
    private TerminationPoint getUpdatedBusyTp(TerminationPoint tp, RegInput input) {
        TerminationPoint newBusyTp;
        Physical tpPhysical = tp.getAugmentation(TerminationPoint1.class).getPhysical();
        if (tpPhysical.getPortType().equals(PortType.OTULine)) {
            newBusyTp = otTpService.createBusyLPortTp(tp, input.getLineSignalRate(), input.getCentFreq(), input.getOutputPower(), input.getServicetype(), null);
        } else {
            newBusyTp = tpRepo.getBusyTp(tp);
        }
        return newBusyTp;
    }

    private Integer getEquipSlotByTpFriendlyName(String tpFriendlyName) {
        return Integer.parseInt(tpFriendlyName.split("-")[2]);
    }

    private String getCardTypeByTpFriendlyName(String tpFriendlyName) {
        return tpFriendlyName.substring(0, tpFriendlyName.indexOf("-"));
    }
}

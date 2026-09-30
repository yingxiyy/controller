/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.tunnel.reg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.NodeUtils;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfoUtil;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.RegInput;
import net.flex.dci.otn.controller.allocate.designer.ne.NeNodeRepo;
import net.flex.dci.otn.controller.allocate.designer.tunnel.OtTpService;
import net.flex.dci.otn.controller.allocate.designer.tunnel.OtTransceiverService;
import net.flex.dci.otn.controller.allocate.designer.tunnel.OtXcService;
import net.flex.dci.otn.controller.allocate.designer.tunnel.TunnelUtils;
import net.flex.dci.otn.controller.allocate.ne.Card;
import net.flex.dci.otn.controller.allocate.ne.CrossConnection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SERVICETYPE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SignalProtocolType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.TpcRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.TpcRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RegTpcRepo {

    @Autowired
    private NodeUtils nodeUtils;
    @Autowired
    private TunnelUtils tunnelUtils;
    @Autowired
    private OtXcService otXcService;
    @Autowired
    private OtTpService otTpService;
    @Autowired
    private OtTransceiverService otTransceiverService;
    @Autowired
    private NeNodeRepo neNodeRepo;

    public List<TpcRoute> createTpcRoutes(String startLPortTpId, String endLPortTpId, RegInput regInput, Map<String, Node> totalNodesMap)
            throws NeDesignerException {
        String tpcStartNodeId = PhysicalTpIdNamingRule.getNodeId(startLPortTpId);
        Node startOtNode = totalNodesMap.get(tpcStartNodeId);

        String tpcEndNodeId = PhysicalTpIdNamingRule.getNodeId(endLPortTpId);
        Node endOtNode = totalNodesMap.get(tpcEndNodeId);

        //fetch existed busy C Port
        List<CrossConnections> existedStartXcs = startOtNode.getAugmentation(Node1.class).getPhysical().getCrossConnections().stream()
                .filter(xc -> xc.getCrossConnectionId().getValue().contains(startLPortTpId) && xc.getDirection().equals(LinkDirection.Bidirection)).collect(
                        Collectors.toList());
        Set<String> busyCPortTpsStart = existedStartXcs.stream().map(xc -> xc.getSourceTp().get(0).getTpRef().getValue()).collect(Collectors.toSet());
        List<CrossConnections> existedEndXcs = endOtNode.getAugmentation(Node1.class).getPhysical().getCrossConnections().stream()
                .filter(xc -> xc.getCrossConnectionId().getValue().contains(endLPortTpId) && xc.getDirection().equals(LinkDirection.Bidirection)).collect(
                        Collectors.toList());
        Set<String> busyCPortTpsEnd = existedEndXcs.stream().map(xc -> xc.getSourceTp().get(0).getTpRef().getValue()).collect(Collectors.toSet());

        List<TpcRoute> tpcRoutes = new ArrayList<>();

        //create xc and make C port busy
        for (int i = 0; i < regInput.getTunnelNumber(); i++) {

            CrossConnections tpcXcStart;
            if (existedStartXcs.size() > i) {
                tpcXcStart = existedStartXcs.get(i);//note: 当存在XC时，则默认不需要创建transceiver了
            } else {
                tpcXcStart = createTpcXcAndUpdateNode(tpcStartNodeId, startLPortTpId, busyCPortTpsStart, regInput, totalNodesMap);
            }

            CrossConnections tpcXcEnd;
            if (existedEndXcs.size() > i) {
                tpcXcEnd = existedEndXcs.get(i);
            } else {
                tpcXcEnd = createTpcXcAndUpdateNode(tpcEndNodeId, endLPortTpId, busyCPortTpsEnd, regInput, totalNodesMap);
            }

            TpcRoute tpcRoute = new TpcRouteBuilder()
                    .setSourceTp(tpcXcStart.getSourceTp().get(0).getTpRef().getValue())
                    .setDestTp(tpcXcEnd.getSourceTp().get(0).getTpRef().getValue())
                    .setCrossConnections(Arrays.asList(tpcXcStart, tpcXcEnd)).build();
            tpcRoutes.add(tpcRoute);
        }
        return tpcRoutes;

    }

    private CrossConnections createTpcXcAndUpdateNode(String nodeId, String lPortTpId, Set<String> busyCPortTps, RegInput regInput, Map<String, Node> totalNodesMap) throws NeDesignerException {
        Node node = totalNodesMap.get(nodeId);
        CrossConnections newTpcXc = createTpcXc(regInput.getLineSignalRate(), regInput.getTunnelSignalRate(),node, lPortTpId, busyCPortTps,regInput.getServicetype());
        String cPortTpId =
                newTpcXc.getSourceTp().get(0).getTpRef().getValue().contains(lPortTpId) ? newTpcXc.getDestinationTp().get(0).getTpRef().getValue()
                        : newTpcXc.getSourceTp().get(0).getTpRef().getValue();

        List<TerminationPoint> tps = node.getTerminationPoint();
        Card otCardInfo = null;
        for (int i = 0; i < tps.size(); i++) {
            TerminationPoint tp = tps.get(i);
            if (tp.getTpId().getValue().equals(cPortTpId)) {
                //update C port TP
                tp = otTpService.createBusyCPortTp(tp, regInput.getTunnelSignalRate(), regInput.getClientMedium());
                tps.set(i, tp);

                otCardInfo = nodeUtils.getCardInfoByEquipId(node, tp.getAugmentation(TerminationPoint1.class).getPhysical().getEquipmentRef());
                break;
            }
        }

        //创建transceiver
        if (otCardInfo == null) {
            throw new NeDesignerException("Internal code error, not got otCardInfo when set C port TP:" + cPortTpId);
        }
        List<Equipments> transceivers = otTransceiverService.createTransceiver(node, cPortTpId, lPortTpId, otCardInfo, regInput.getClientMedium());
        List<Equipments> equipList = nodeUtils.getEquipments(node);
        equipList.addAll(transceivers);

        //fresh node
        List<CrossConnections> xcs = nodeUtils.getXcs(node);
        xcs.add(newTpcXc);
        node = neNodeRepo.refreshNodeByXcsTpsEquips(node, xcs, tps, equipList);
        totalNodesMap.put(nodeId, node);

        return newTpcXc;
    }

    private CrossConnections createTpcXc(Class<? extends SignalProtocolType> lineSignalRate, Class<? extends SignalProtocolType> tunnelSignalRate, Node otNode, String lPortTpId, Set<String> busyCPortTps, SERVICETYPE servicetype) throws NeDesignerException {

        Card otCardInfo = nodeUtils.getCardInfoByTpId(otNode, lPortTpId);
        String nodeId = otNode.getNodeId().getValue();
        String lPortName = PhysicalTpIdNamingRule.getPortNameByTpId(lPortTpId);
        CrossConnection xcInfo = tunnelUtils.getOtXcInfo(otCardInfo, lPortName, lineSignalRate,tunnelSignalRate,servicetype);

        List<String> cportNames = NeInfoUtil.getNameList(xcInfo.getFrom().getPort());

        for (String cPortName : cportNames) {
            String availableCPortTp = nodeUtils.getCPortTp(cPortName, lPortTpId);
            if (busyCPortTps.contains(availableCPortTp)) {
                continue;
            }
            busyCPortTps.add(availableCPortTp);
            return otXcService.createXC(nodeId, availableCPortTp, lPortTpId, xcInfo);


        }
        log.error("Failed to create tpc xc for tp:{}, because no available C port can picked, with busyCPortTps:{}. Check the NE json config.", lPortTpId, busyCPortTps);
        throw new NeDesignerException("Failed to create tpc XC for L port:" + lPortTpId + " ,Check the NE json config");
    }

}

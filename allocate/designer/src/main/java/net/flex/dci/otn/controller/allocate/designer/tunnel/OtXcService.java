/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.tunnel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.NodeUtils;
import net.flex.dci.otn.controller.allocate.designer.config.NEInfoConfig;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfoUtil;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelInput;
import net.flex.dci.otn.controller.allocate.designer.ne.XCRepo;
import net.flex.dci.otn.controller.allocate.designer.site.XCService;
import net.flex.dci.otn.controller.allocate.designer.tunnel.model.PickedOtResource;
import net.flex.dci.otn.controller.allocate.ne.Card;
import net.flex.dci.otn.controller.allocate.ne.CrossConnection;
import net.flex.dci.otn.controller.allocate.ne.SupportedSignal;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SERVICETYPE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SignalProtocolType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OtXcService {

    @Autowired
    private XCRepo xcRepo;

    @Autowired
    private XCService xcService;

    @Autowired
    private TunnelUtils tunnelUtils;

    @Autowired
    private NEInfoConfig neInfoConfig;
    @Autowired
    private NodeUtils nodeUtils;


    /**
     * For OT card XC, always use C port as source.
     *
     * @param pickedOtResource
     * @param tunnelInput
     * @return
     */
    public CrossConnections createXC(PickedOtResource pickedOtResource, TunnelInput tunnelInput) throws NeDesignerException {
        Card otCardInfo = neInfoConfig.getNeInfo(tunnelInput.getVendorName(), tunnelInput.getVendorType(), NodeType.TD.name())
                .getCardByCardVendor(tunnelInput.getCardType());//note: 这里input实际上提供的是card vendor
        @NonNull String ctp = pickedOtResource.getPickedOtTps().getCtp();
        @NonNull String ltp = pickedOtResource.getPickedOtTps().getLtp();
        String lPortName = PhysicalTpIdNamingRule.getPortNameByTpId(ltp);

        CrossConnection xcInfo = tunnelUtils.getOtXcInfo(otCardInfo, lPortName, tunnelInput.getLineSignalRate(), tunnelInput.getTunnelSignalRate(),
                null);//set servicetype as null just for compile,because this class not used anymore
        return createXC(pickedOtResource.getNode().getNodeId().getValue(), ctp, ltp, xcInfo);
    }

    public CrossConnections createXC(String nodeId, String ctp, String ltp, CrossConnection xcInfo) throws NeDesignerException {
        String cPortName = PhysicalTpIdNamingRule.getPortNameByTpId(ctp);
        String lPortName = PhysicalTpIdNamingRule.getPortNameByTpId(ltp);
        String clientOduSlot = NeInfoUtil.getCPortXcLayers_OT(xcInfo);
        String lineOduSlot = NeInfoUtil.getLPortXcLayersByCPortName_OT(xcInfo, cPortName);//L口的layer是由C端口确定的

        List<SourceTp> sTPs = new ArrayList<>();
        sTPs.add(new SourceTpBuilder().setTpRef(new TpId(ctp)).setSlot(clientOduSlot).build());
        List<DestinationTp> dTPs = new ArrayList<>();
        dTPs.add(new DestinationTpBuilder().setTpRef(new TpId(ltp))
                .setSlot(lineOduSlot).build());

        String description = xcRepo.getXCDescription(xcInfo, ctp, cPortName, lPortName, null);

        return xcRepo.createOTXC(nodeId, xcInfo, sTPs, dTPs, description);
    }


    public List<CrossConnections> createOtXCsForNewOch(String lPortTp, Node node, Integer number, Card otCardInfo, Class<? extends SignalProtocolType> lineSignalRate,
            Class<? extends SignalProtocolType> tunnelSignalRate, SERVICETYPE servicetype) throws NeDesignerException {
        String nodeId = node.getNodeId().getValue();
        String lPortName = PhysicalTpIdNamingRule.getPortNameByTpId(lPortTp);
        CrossConnection xcInfo = tunnelUtils.getOtXcInfo(otCardInfo, lPortName, lineSignalRate, tunnelSignalRate, servicetype);

        List<String> cportNames = NeInfoUtil.getNameList(xcInfo.getFrom().getPort());
        if (number > cportNames.size()) {
            log.error("Required to create {} XC, but  can only create {} for card config:{}", number, cportNames.size(), otCardInfo);
            String msg = String.format("Required to create %d XC, but json can only create %d.", number, cportNames.size());
            throw new NeDesignerException(msg);
        }

        List<CrossConnections> xcs = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            String cPortName = cportNames.get(i);
            String availableCPortTp = nodeUtils.getCPortTp(cPortName, lPortTp);
            CrossConnections newXc = createXC(nodeId, availableCPortTp, lPortTp, xcInfo);
            xcs.add(newXc);

        }
        return xcs;

    }

    public List<CrossConnections> createOp6XCs(String op6SigPortTp, Node node, Card op6CardInfo) throws NeDesignerException {
        String nodeId = node.getNodeId().getValue();
        String sigPortName = PhysicalTpIdNamingRule.getPortNameByTpId(op6SigPortTp);
        String tpPrefix = op6SigPortTp.substring(0, op6SigPortTp.lastIndexOf("-"));

        //获取需要创建交叉的port
        Map<String, String> portNameTpIdMap = new HashMap<>();
        portNameTpIdMap.put(sigPortName, op6SigPortTp);

        for (CrossConnection crossConnection : op6CardInfo.getCrossConnections()) {
            if (!crossConnection.getInitiated()) {
                continue;
            }
            if (!crossConnection.getFrom().getPort().equals(sigPortName)) {
                continue;
            }
            List<String> toNames = NeInfoUtil.getNameList(crossConnection.getTo().getPort());
            for (String toName : toNames) {
                String toTp = op6SigPortTp.replace(sigPortName, toName);
                portNameTpIdMap.put(toName, toTp);
            }
        }

        return xcService.createCardXcs(nodeId, op6CardInfo, portNameTpIdMap, true);
    }

    public CrossConnections createOchXc(String nodeId, String lPortTpId, Card otCardInfo) throws NeDesignerException {
        String lPortName = PhysicalTpIdNamingRule.getPortNameByTpId(lPortTpId);
        CrossConnection xcInfo = NeInfoUtil.getOtXcInfo(otCardInfo, lPortName, SupportedSignal.OCH, SupportedSignal.OCH, null);//och xc not need serviceType
        return createXC(nodeId, lPortTpId, lPortTpId, xcInfo);
    }

    public CrossConnections createRegXc(String lPortTp, String nodeId, Card otCardInfo, SERVICETYPE regServiceType) throws NeDesignerException {
        CrossConnection xcInfo = NeInfoUtil.getRegXcInfo(otCardInfo, regServiceType);
        List<SourceTp> sTPs = new ArrayList<>();
        sTPs.add(new SourceTpBuilder().setTpRef(new TpId(lPortTp)).setSlot(xcInfo.getFrom().getLayer()).build());
        List<DestinationTp> dTPs = new ArrayList<>();
        dTPs.add(new DestinationTpBuilder().setTpRef(new TpId(lPortTp))
                .setSlot(xcInfo.getTo().getLayer()).build());

        String lPortName = PhysicalTpIdNamingRule.getPortNameByTpId(lPortTp);

        String description = xcRepo.getXCDescription(xcInfo, lPortTp, lPortName, lPortName, null);

        return xcRepo.createOTXC(nodeId, xcInfo, sTPs, dTPs, description);
    }
}

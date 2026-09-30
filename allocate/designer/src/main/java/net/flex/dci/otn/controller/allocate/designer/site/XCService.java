/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.site;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfoUtil;
import net.flex.dci.otn.controller.allocate.designer.ne.XCRepo;
import net.flex.dci.otn.controller.allocate.ne.Card;
import net.flex.dci.otn.controller.allocate.ne.CrossConnection;
import net.flex.dci.otn.controller.allocate.ne.CrossConnectionPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.amplifier.attributes.Amplifier;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.amplifier.attributes.AmplifierBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class XCService {

    public static final BigDecimal TARGET_GAIN_FIX_OA = new BigDecimal(13);
    public static final BigDecimal TARGET_GAIN_FLEX_OA = new BigDecimal(17);
    @Autowired
    private XCRepo xcRepo;

    public List<CrossConnections> createCardXcs(Integer grid, String nodeId, Card card, @NonNull Map<String, String> portNameTpIdMap, Boolean isProtected) throws NeDesignerException {
        List<CrossConnections> xcs = new ArrayList<>();

        for (CrossConnection crossConnection : card.getCrossConnections()) {
            if (!crossConnection.getInitiated()) {
                continue;
            }
            //Create new XCs
            try {
                List<CrossConnections> newXcs = createXCs(nodeId, portNameTpIdMap, crossConnection, isProtected);
                if (card.getCardType().equals("OA_C") && grid != null) {
                    newXcs = updateTargetGainFix(newXcs, grid);
                }
                xcs.addAll(newXcs);
            } catch (Exception e) {
                String msg = "Failed to create XC for cardType:" + card.getCardType();
                log.error(msg, e);
                throw new NeDesignerException(msg, e);
            }
        }
        return xcs;
    }

    public List<CrossConnections> createCardXcs(String nodeId, Card card, @NonNull Map<String, String> portNameTpIdMap, Boolean isProtected) throws NeDesignerException {

        return createCardXcs(null, nodeId, card, portNameTpIdMap, isProtected);
    }

    private List<CrossConnections> updateTargetGainFix(List<CrossConnections> xcs, Integer grid) {
        BigDecimal target_gain = grid == 0 ? TARGET_GAIN_FLEX_OA : TARGET_GAIN_FIX_OA;
        List<CrossConnections> result = new ArrayList<>();
        for (CrossConnections xc : xcs) {
            if (xc.getAmplifier() == null) {
                result.add(xc);
                continue;
            }
            Amplifier updatedAmplifier = new AmplifierBuilder(xc.getAmplifier()).setTargetGain(target_gain).build();
            result.add(new CrossConnectionsBuilder(xc).setAmplifier(updatedAmplifier).build());
        }
        return result;
    }

    public List<CrossConnections> createXCs(String nodeId, Map<String, String> portNameTpIdMap, CrossConnection crossConnection, Boolean isProtected) throws NeDesignerException {
        if (!crossConnection.getInitiated()) {
            return null;
        }

        List<CrossConnections> xcs = new ArrayList<>();

        CrossConnectionPoint xcFrom = crossConnection.getFrom();
        CrossConnectionPoint xcTo = crossConnection.getTo();

        List<String> fromNames = NeInfoUtil.getNameList(xcFrom.getPort());
        List<String> toNames = NeInfoUtil.getNameList(xcTo.getPort());

        Integer multiple = crossConnection.getMultiple();
        if (multiple == 1) {
            List<String> srcTpIds = fromNames.stream().map(portName -> portNameTpIdMap.get(portName)).filter(Objects::nonNull).collect(Collectors.toList());
            List<String> destTpIds = toNames.stream().map(portName -> portNameTpIdMap.get(portName)).filter(Objects::nonNull).collect(Collectors.toList());

            if (srcTpIds.isEmpty() || destTpIds.isEmpty()) {
                log.debug("Not create XC because no available TP for this XC:{}.", crossConnection);
                return xcs;
            }
            CrossConnections xc = createXC(nodeId, crossConnection, srcTpIds, destTpIds, isProtected, null);
            xcs.add(xc);

        } else {
            String fromName = fromNames.size() < multiple ? fromNames.get(0) : null;
            String toName = toNames.size() < multiple ? toNames.get(0) : null;
            ArrayList<String> cenFrequencyList = xcRepo.getCenFrequencyList(crossConnection);
            for (int i = 0; i < multiple; i++) {
                String fromPort = fromName == null ? fromNames.get(i) : fromName;
                String srcTpId = portNameTpIdMap.get(fromPort);

                String toPort = toName == null ? toNames.get(i) : toName;
                String destTpId = portNameTpIdMap.get(toPort);

                if (srcTpId == null || destTpId == null) {
                    log.debug("Not create XC because no available TP for this XC:{}.", crossConnection);
                    return xcs;
                }
                String cenFrequency = null;
                if (cenFrequencyList != null) {
                    cenFrequency = cenFrequencyList.get(i);
                }
                String description = xcRepo.getXCDescription(crossConnection, srcTpId, fromPort, toPort, cenFrequency);
                CrossConnections xc = createXC(nodeId, crossConnection, Arrays.asList(srcTpId), Arrays.asList(destTpId), isProtected, description);
                xcs.add(xc);
            }
        }
        return xcs;
    }

    /**
     * Always create XC for MAIN, because for SLAVE, always update the existed XC
     *
     * @param nodeId
     * @param crossConnection
     * @param srcTpIds
     * @param destTpIds
     * @param description
     * @return
     */
    public CrossConnections createXC(String nodeId, CrossConnection crossConnection, List<String> srcTpIds, List<String> destTpIds, Boolean isProtected, String description)
            throws NeDesignerException {

        List<String> tpIdList = new ArrayList<>();

        List<SourceTp> sTPs = new ArrayList<>();
        for (String srcTp : srcTpIds) {
            tpIdList.add(srcTp);
            sTPs.add(new SourceTpBuilder().setTpRef(TpId.getDefaultInstance(srcTp)).build());
        }

        List<DestinationTp> dTPs = new ArrayList<>();
        for (String destTp : destTpIds) {
            tpIdList.add(destTp);
            dTPs.add(new DestinationTpBuilder().setTpRef(TpId.getDefaultInstance(destTp)).build());
        }
        if (description == null) {
            return xcRepo.createXC(nodeId, crossConnection, sTPs, dTPs, isProtected);
        }
        return xcRepo.createXC(nodeId, crossConnection, sTPs, dTPs, isProtected, description);

    }


}

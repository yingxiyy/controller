/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.tunnel;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.mongo.enums.NeSubType;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.NodeUtils;
import net.flex.dci.otn.controller.allocate.designer.config.NEInfoConfig;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelInput;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelNewOchInput;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelReuseOchInput;
import net.flex.dci.otn.controller.allocate.designer.ne.EquipmentRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.LinkRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.TpRepo;
import net.flex.dci.otn.controller.allocate.designer.tunnel.model.PickedOtResource;
import net.flex.dci.otn.controller.allocate.ne.Card;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ETHERNETCOMPLIANCECODE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SERVICETYPE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OtNodeService {

    @Autowired
    private OtTransceiverService otTransceiverService;

    @Autowired
    private LinkRepo linkRepo;

    @Autowired
    private OtTpService otTpService;

    @Autowired
    private NodeUtils nodeUtils;

    @Autowired
    private TunnelUtils tunnelUtils;

    @Autowired
    private NEInfoConfig neInfoConfig;

    @Autowired
    private EquipmentRepo equipmentRepo;

    @Autowired
    private TpRepo tpRepo;

    /**
     * 1. add XC
     *
     * 2. create transceiver
     *
     * 3. create internalLink, if new OsLink created
     *
     * 4. update TP busy/SignalRate/CentralFrequency, if new OsLink created
     *
     * deprecated
     *
     * @param pickedOtResource
     * @param otCardInfo
     * @param newOlsLink
     * @param newXc
     * @param input
     * @param cenFrequency
     * @param isStuffed
     * @return
     * @throws NeDesignerException
     */
    public Node updatedOtNode(PickedOtResource pickedOtResource, Card otCardInfo, Link newOlsLink, CrossConnections newXc, TunnelInput input,
            BigInteger cenFrequency, Boolean isStuffed) throws NeDesignerException {

        @NonNull Node node = pickedOtResource.getNode();
        String vendorName = node.getAugmentation(Node1.class).getPhysical().getVendorName();
        String vendorType = node.getAugmentation(Node1.class).getPhysical().getVendorType();

        //add OT XC
        List<CrossConnections> xcs = new ArrayList<>();
        xcs.addAll(node.getAugmentation(Node1.class).getPhysical().getCrossConnections());
        xcs.add(newXc);

        //create OT transceiver
        @NonNull String ctp = pickedOtResource.getPickedOtTps().getCtp();
        @NonNull String ltp = pickedOtResource.getPickedOtTps().getLtp();
        List<Equipments> transceivers = otTransceiverService.createTransceiver(node, ctp, ltp, otCardInfo, input.getClientMedium());
        List<Equipments> equipments = new ArrayList<>();
        equipments.addAll(node.getAugmentation(Node1.class).getPhysical().getEquipments());
        equipments.addAll(transceivers);

        Node newOtNode;
        List<InternalLinks> otInternalLinks = new ArrayList<>();
        otInternalLinks.addAll(node.getAugmentation(Node1.class).getPhysical().getInternalLinks());

        //create OT internalLink
        if (newOlsLink != null) {
            InternalLinks otInternalLink = linkRepo.createInternalLink(node.getNodeId().getValue(), newOlsLink);
            otInternalLinks.add(otInternalLink);
        }

        //set OT TPs busy/SignalRate/CentralFrequency
        List<TerminationPoint> tps = new ArrayList<>();
        tps.addAll(node.getTerminationPoint());
        for (int i = 0; i < tps.size(); i++) {
            TerminationPoint item = tps.get(i);
            if (ctp.equals(item.getTpId().getValue())) {
                TerminationPoint busyCtp = otTpService.createBusyCPortTp(item, input.getTunnelSignalRate(), input.getClientMedium());
                tps.set(i, busyCtp);

                if (newOlsLink != null) {
                    continue;
                } else {
                    break;
                }
            }

            //只有当新的OSLin创建了，才需要去更新L端口的TP
            if (newOlsLink != null && ltp.equals(item.getTpId().getValue())) {
                TerminationPoint busyLtp = otTpService.createBusyLPortTp(item, input.getLineSignalRate(), cenFrequency, input.getOutputPower(), input.getServicetype(), null);//deprecated
                tps.set(i, busyLtp);
            }
        }

        //create new OT node
        if (isStuffed == null) {
            //只有当isStuffed为空时，才需要去计算是否stuff了
            isStuffed = nodeUtils.checkIsStuffed(xcs, equipments, tps, vendorName, vendorType);
        }
        Physical physical = new PhysicalBuilder(node.getAugmentation(Node1.class).getPhysical())
                .setInternalLinks(otInternalLinks)
                .setCrossConnections(xcs)
                .setEquipments(equipments)
                .setStuffed(isStuffed)
                .build();
        Node1 phyNode = new Node1Builder().setPhysical(physical).build();

        newOtNode = new NodeBuilder(node)
                .setTerminationPoint(tps)
                .addAugmentation(Node1.class, phyNode).build();
        return newOtNode;
    }


    public Node updatedOtNodeReuseOch(Node node, List<CrossConnections> newXcs, List<Equipments> newTransceivers, TunnelReuseOchInput input, Class<? extends ETHERNETCOMPLIANCECODE> clientMedium) {
        String siteId = PhysicalNodeIdNamingRule.getSiteId(node.getNodeId().getValue());
        //add OT XC
        List<CrossConnections> xcs = new ArrayList<>();
        xcs.addAll(node.getAugmentation(Node1.class).getPhysical().getCrossConnections());
        xcs.addAll(newXcs);

        //add OT transceiver
        List<Equipments> equipments = new ArrayList<>();
        equipments.addAll(node.getAugmentation(Node1.class).getPhysical().getEquipments());
        equipments.addAll(newTransceivers);

        //set OT TPs busy/SignalRate/CentralFrequency
        Set<String> ctps = newXcs.stream().map(xc -> xc.getSourceTp().get(0).getTpRef().getValue()).collect(Collectors.toSet());
        List<TerminationPoint> tps = new ArrayList<>();
        tps.addAll(node.getTerminationPoint());
        for (int i = 0; i < tps.size(); i++) {
            TerminationPoint item = tps.get(i);
            if (ctps.contains(item.getTpId().getValue())) {
                TerminationPoint busyCtp = otTpService.createBusyCPortTp(item, input.getTunnelSignalRate(), clientMedium);
                tps.set(i, busyCtp);
            }
        }

        Physical physical = new PhysicalBuilder(node.getAugmentation(Node1.class).getPhysical())
                .setCrossConnections(xcs)
                .setEquipments(equipments)
                .build();
        Node1 phyNode = new Node1Builder().setPhysical(physical).build();

        return new NodeBuilder(node)
                .setTerminationPoint(tps)
                .addAugmentation(Node1.class, phyNode).build();
    }

    public Node updatedOtNodeNewOch(Node node, List<Link> newOsLinks, List<CrossConnections> newOtXcs, List<CrossConnections> newOp6Xc, List<Equipments> newTransceivers, TunnelNewOchInput input,
            Long cenFrequency, Class<? extends ETHERNETCOMPLIANCECODE> clientMedium) {

        //add OT XC
        List<CrossConnections> xcs = new ArrayList<>();
        xcs.addAll(node.getAugmentation(Node1.class).getPhysical().getCrossConnections());
        xcs.addAll(newOtXcs);
        xcs.addAll(newOp6Xc);

        //add OT transceiver
        List<Equipments> equipments = new ArrayList<>();
        equipments.addAll(node.getAugmentation(Node1.class).getPhysical().getEquipments());
        equipments.addAll(newTransceivers);

        Node newOtNode;
        List<InternalLinks> otInternalLinks = new ArrayList<>();
        otInternalLinks.addAll(node.getAugmentation(Node1.class).getPhysical().getInternalLinks());

        //create OT internalLink
        Set<String> busyTpIds = new HashSet<>();
        if (newOsLinks != null) {
            for (Link newOlsLink : newOsLinks) {
                InternalLinks otInternalLink = linkRepo.createInternalLink(node.getNodeId().getValue(), newOlsLink);
                otInternalLinks.add(otInternalLink);
                busyTpIds.add(newOlsLink.getSource().getSourceTp().getValue());
                busyTpIds.add(newOlsLink.getDestination().getDestTp().getValue());
            }
        }

        //set OT TPs busy/SignalRate/CentralFrequency
        Set<String> ctps = newOtXcs.isEmpty() ? Collections.EMPTY_SET : newOtXcs.stream().map(xc -> xc.getSourceTp().get(0).getTpRef().getValue()).collect(Collectors.toSet());
        String ltp = newOtXcs.isEmpty() ? null : newOtXcs.get(0).getDestinationTp().get(0).getTpRef().getValue();
        List<TerminationPoint> tps = new ArrayList<>();
        tps.addAll(node.getTerminationPoint());
        for (int i = 0; i < tps.size(); i++) {
            TerminationPoint item = tps.get(i);
            String tpId = item.getTpId().getValue();
            if (ctps.contains(tpId)) {
                TerminationPoint busyCtp = otTpService.createBusyCPortTp(item, input.getTunnelSignalRate(), clientMedium);
                tps.set(i, busyCtp);
                continue;
            }

            if (ltp != null && ltp.equals(tpId)) {
                TerminationPoint busyLtp = otTpService.createBusyLPortTp(item, input.getLineSignalRate(), BigInteger.valueOf(cenFrequency), input.getOutputPower(), input.getServiceType(),
                        input.getOpMode());
                tps.set(i, busyLtp);
                continue;
            }

            if (busyTpIds.contains(tpId)) {
                TerminationPoint busyTp;
                if (item.getAugmentation(TerminationPoint1.class).getPhysical().getPortType().equals(PortType.OTULine)) {
                    busyTp = otTpService.createBusyLPortTp(item, input.getLineSignalRate(), BigInteger.valueOf(cenFrequency), input.getOutputPower(), input.getServiceType(), input.getOpMode());
                } else {
                    busyTp = tpRepo.getBusyTp(item);
                }
                tps.set(i, busyTp);
                continue;
            }
        }

        Physical physical = new PhysicalBuilder(node.getAugmentation(Node1.class).getPhysical())
                .setInternalLinks(otInternalLinks)
                .setCrossConnections(xcs)
                .setEquipments(equipments)
                .build();
        Node1 phyNode = new Node1Builder().setPhysical(physical).build();

        newOtNode = new NodeBuilder(node)
                .setTerminationPoint(tps)
                .addAugmentation(Node1.class, phyNode).build();
        return newOtNode;
    }


    public Node updatedOtNodeNewOchReg(Node node, List<Link> newOsLinks, List<CrossConnections> newRegXcs, TunnelNewOchInput input, Long cenFrequency, SERVICETYPE regServiceType) {

        List<CrossConnections> xcs = new ArrayList<>();
        xcs.addAll(node.getAugmentation(Node1.class).getPhysical().getCrossConnections());
        xcs.addAll(newRegXcs);

        List<InternalLinks> otInternalLinks = new ArrayList<>();
        otInternalLinks.addAll(node.getAugmentation(Node1.class).getPhysical().getInternalLinks());

        //create OT internalLink
        Set<String> busyTpIds = new HashSet<>();
        if (newOsLinks != null) {
            for (Link newOlsLink : newOsLinks) {
                InternalLinks otInternalLink = linkRepo.createInternalLink(node.getNodeId().getValue(), newOlsLink);
                otInternalLinks.add(otInternalLink);
                busyTpIds.add(newOlsLink.getSource().getSourceTp().getValue());
                busyTpIds.add(newOlsLink.getDestination().getDestTp().getValue());
            }
        }

        //create busy reg L port tp
        List<TerminationPoint> tps = new ArrayList<>();
        tps.addAll(node.getTerminationPoint());
        for (int i = 0; i < tps.size(); i++) {
            TerminationPoint item = tps.get(i);
            String tpId = item.getTpId().getValue();

            if (busyTpIds.contains(tpId)) {
                TerminationPoint busyTp = otTpService.createBusyLPortTp(item, input.getLineSignalRate(), BigInteger.valueOf(cenFrequency), input.getOutputPower(), regServiceType, input.getOpMode());
                tps.set(i, busyTp);
                continue;
            }
        }

        Physical physical = new PhysicalBuilder(node.getAugmentation(Node1.class).getPhysical())
                .setInternalLinks(otInternalLinks)
                .setCrossConnections(xcs)
                .setCustomedType(NeSubType.EPC_REG.getSubTypeName())
                .build();
        Node1 phyNode = new Node1Builder().setPhysical(physical).build();
        return new NodeBuilder(node)
                .setTerminationPoint(tps)
                .addAugmentation(Node1.class, phyNode).build();
    }
}

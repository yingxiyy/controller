/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.tunnel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otn.controller.allocate.common.namingrule.TunnelFriendlyName;
import net.flex.dci.otn.controller.allocate.common.util.Constant;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.NodeUtils;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelReuseOchInput;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelReuseOchOutput;
import net.flex.dci.otn.controller.allocate.ne.Card;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OduGranularity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.och.Available;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.TpcRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.TpcRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TunnelReuseOchAllocate {

    @Autowired
    private OchLinkDao ochLinkDao;
    @Autowired
    private PhyNodeDao phyNodeDao;
    @Autowired
    private NodeUtils nodeUtils;
    @Autowired
    private OtReusedStrategy otReusedStrategy;
    @Autowired
    private OtNodeService otNodeService;
    @Autowired
    private OtTransceiverService otTransceiverService;


    public TunnelReuseOchOutput allocate(TunnelReuseOchInput input, Card otCardInfo) throws NeDesignerException {
        String ochLinkId = input.getOchLinkId();
        @NonNull Integer number = input.getNumber();
        Link ochLink = ochLinkDao.getOchLinkByLinkId(ochLinkId);
        Och ochLinkAttr = ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class).getOch();
        @NonNull OduGranularity tunnelOdu = input.getTunnelOdu();

        String msgPrefix = String.format("Failed to create required %d tunnels on OCH link:%s, because ", number, ochLinkId);

        //validate OCH: check if och available still meet the requirement right now, because och link may have changes after computing tunnel
        if (ochLinkAttr.getAvailable() == null) {
            String msg = String.format("%s och link has changed, available is null right now.", msgPrefix);
            log.error(msg);
            throw new NeDesignerException(msg);
        }

        Optional<Available> availableOptional = ochLinkAttr.getAvailable().stream().filter(item -> item.getSupportedOduj().equals(tunnelOdu)).findAny();
        if (!availableOptional.isPresent()) {
            String msg = String.format("%s och link has changed, available for %s is null right now.", msgPrefix, tunnelOdu.name());
            log.error(msg);
            throw new NeDesignerException(msg);
        }
        String[] availableOdus = availableOptional.get().getAvailableOdujSlot().split(Constant.OCH_AVAILABLE_ODU_SEPARATOR);
        Integer ochCanCreateTunnelNumber = availableOdus.length;
        if (ochCanCreateTunnelNumber < number) {
            String msg = String.format("%s och link has changed, available for %s is %d right now,less than to create number:%d", msgPrefix, tunnelOdu.name(), ochCanCreateTunnelNumber, number);
            log.error(msg);
            throw new NeDesignerException(msg);
        }
//        Integer ochBandwidth = Integer.parseInt(ochLinkAttr.getBandwidth());//一个L口总共可以创建的业务数量
        Integer ochBandwidth = getOchBandWidth(ochLinkAttr.getOdukType());

        //Validate source TPC node
        List<Node> tpcNodeSnapshot = new ArrayList<>();
        String srcNodeId = ochLink.getSource().getSourceNode().getValue();
        String srcTpId = ochLink.getSource().getSourceTp().getValue();//e.g. "Site-1664524279625#Ne-1664527353054#LINECARD-1-1#PORT-1-1-L1"
        Node srcNode;
        if (input.getTotalInMemoryNode().containsKey(srcNodeId)) {
            srcNode = input.getTotalInMemoryNode().get(srcNodeId);
        } else {
            srcNode = phyNodeDao.getConfigPhyNodeById(srcNodeId);
            tpcNodeSnapshot.add(srcNode);
        }
        List<CrossConnections> srcLportXcs = nodeUtils.getOtXcsByLPortTp(srcNode, srcTpId);

        Integer srcExistedTunnelNumber = srcLportXcs.size();
        Integer srcCanCreateTunnelNumber = ochBandwidth - srcExistedTunnelNumber;
        if (srcCanCreateTunnelNumber < number) {
            String msg = String.format("%s source TPC node:%s has changed, available to create is %d right now.", msgPrefix, srcNodeId, srcCanCreateTunnelNumber);
            log.error(msg);
            throw new NeDesignerException(msg);
        }

        //validate dest TPC node
        String dstNodeId = ochLink.getDestination().getDestNode().getValue();
        String dstTpId = ochLink.getDestination().getDestTp().getValue();
        Node dstNode;
        if (input.getTotalInMemoryNode().containsKey(dstNodeId)) {
            dstNode = input.getTotalInMemoryNode().get(dstNodeId);
        } else {
            dstNode = phyNodeDao.getConfigPhyNodeById(dstNodeId);
            tpcNodeSnapshot.add(dstNode);
        }
        List<CrossConnections> dstLportXcs = nodeUtils.getOtXcsByLPortTp(dstNode, dstTpId);
        Integer dstExistedTunnelNumber = dstLportXcs.size();
        Integer dstCanCreateTunnelNumber = ochBandwidth - dstExistedTunnelNumber;
        if (dstCanCreateTunnelNumber < number) {
            String msg = String.format("%s dest TPC node has changed, available to create is %d right now.", msgPrefix, dstNodeId, dstCanCreateTunnelNumber);
            log.error(msg);
            throw new NeDesignerException(msg);
        }

        //final validate
        if (srcCanCreateTunnelNumber != dstCanCreateTunnelNumber) {
            String msg = String.format("%s och link has changed, source can create %d tunnel, but dest can create %d tunnel.", msgPrefix, srcCanCreateTunnelNumber, dstCanCreateTunnelNumber);
            log.error(msg);
            throw new NeDesignerException(msg);
        }

        //allocate  source
        Map<String,String> portIdFriendlyNameMapSrc=srcNode.getTerminationPoint().stream().collect(Collectors.toMap(
                tp->tp.getTpId().getValue(),tp->tp.getAugmentation(TerminationPoint1.class).getPhysical().getFriendlyName()
        ));
        List<CrossConnections> newSrcXcs = otReusedStrategy.reUsedOch(srcNodeId, srcTpId, srcLportXcs, otCardInfo, input.getLineSignalRate(), input.getTunnelSignalRate(), number,input.getServicetype());
        List<Equipments> newSrcTransceivers = otTransceiverService.createTransceiversReuseOch(srcNodeId, newSrcXcs, otCardInfo, input.getClientMediumA(),portIdFriendlyNameMapSrc);
        Node updateSrcNode = otNodeService.updatedOtNodeReuseOch(srcNode, newSrcXcs, newSrcTransceivers, input,input.getClientMediumA());

        //allocate dest
        Map<String,String> portIdFriendlyNameMapDst=dstNode.getTerminationPoint().stream().collect(Collectors.toMap(
                tp->tp.getTpId().getValue(),tp->tp.getAugmentation(TerminationPoint1.class).getPhysical().getFriendlyName()
        ));
        List<CrossConnections> dscXcs = otReusedStrategy.reUsedOch(dstNodeId, dstTpId, dstLportXcs, otCardInfo, input.getLineSignalRate(), input.getTunnelSignalRate(), number,input.getServicetype());
        Map<String, CrossConnections> dscXcsBySlotMap = dscXcs.stream().collect(Collectors.toMap(item -> getLPortSlotFromXc(item), Function.identity()));
        List<Equipments> newDstTransceivers = otTransceiverService.createTransceiversReuseOch(dstNodeId, dscXcs, otCardInfo, input.getClientMediumZ(),portIdFriendlyNameMapDst);
        Node updateDstNode = otNodeService.updatedOtNodeReuseOch(dstNode, dscXcs, newDstTransceivers, input,input.getClientMediumZ());

        //build tpcRoutes
        List<TpcRoute> tpcRoutes = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            CrossConnections newSrcXc = newSrcXcs.get(i);
            String srcSlot = getLPortSlotFromXc(newSrcXc);
            CrossConnections newDestXc = dscXcsBySlotMap.get(srcSlot);
            if (newDestXc == null) {//同一条OCH上面的业务，tpc应该都是成对的,同一条业务的slot是相同的，如果找不到，可能是node变了
                String msg = String.format("%s maybe TPC node has changed, because failed to find new dest xc by peer source xc slot:%s.", msgPrefix, srcSlot);
                log.error(msg);
                throw new NeDesignerException(msg);
            }
            String srcCtp = newSrcXc.getSourceTp().get(0).getTpRef().getValue();//e.g. "Site-1649603142454#Ne-1649842342351#LINECARD-1-1#PORT-1-1-C1"
            String destCtp = newDestXc.getSourceTp().get(0).getTpRef().getValue();//e.g. "Site-1649603142454#Ne-1649842342351#LINECARD-1-1#PORT-1-1-C1"
            TpcRoute tpcRoute = new TpcRouteBuilder()
                    .setSourceTp(srcCtp)
                    .setDestTp(destCtp)
                    .setCrossConnections(Arrays.asList(newSrcXc, newDestXc)).build();
            tpcRoutes.add(tpcRoute);
        }
        Map<String, Node> inMemoryNode = new HashMap<>();
        inMemoryNode.put(srcNodeId, updateSrcNode);
        inMemoryNode.put(dstNodeId, updateDstNode);

        return TunnelReuseOchOutput.builder()
                .ochLinkSnapshot(ochLink)
                .tpcRoutes(tpcRoutes)
                .tpcNodeSnapshot(tpcNodeSnapshot)
                .inMemoryTpcNode(inMemoryNode)
                .build();
    }

    private Integer getOchBandWidth(OduGranularity odukType) {
        switch (odukType) {
            case Odu4x2:
                return 2;
            case Odu4x3:
                return 3;
            case Odu4x4:
                return 4;
            case Odu4:
                return 1;
            case Odu4x6:
                return 6;
            case Odu4x8:
                return 8;
            default:
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "doesn't know how to covert odukType:" + odukType);
        }
    }

    private String getLPortSlotFromXc(CrossConnections xc) {
        return xc.getDestinationTp().get(0).getSlot();
    }
}

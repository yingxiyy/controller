/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.tunnel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.enums.NeSubType;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.config.NEInfoConfig;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.RegInput;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.RegSegment;
import net.flex.dci.otn.controller.allocate.designer.ne.NeNodeRepo;
import net.flex.dci.otn.controller.allocate.designer.tunnel.reg.RegRepo;
import net.flex.dci.otn.controller.allocate.link.common.BomGenerator;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.bom.rev200710.BomInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.AllocateRegsInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.AllocateRegsOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.AllocateRegsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.allocate.regs.output.BomInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.allocate.regs.output.TunnelAllocateResult2;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.allocate.regs.output.TunnelAllocateResult2Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.reg.segment.info.RegSegments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.Nodes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.ReusedNodesSnapshot;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.Vendor;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.VendorBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.vendor.NewOchTunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.vendor.NewOchTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.vendor._new.och.tunnel.TunnelRouteInfos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RegAllocator {

    @Autowired
    private PhyNodeDao phyNodeDao;
    @Autowired
    private NEInfoConfig neInfoConfig;
    @Autowired
    private NeNodeRepo neNodeRepo;
    @Autowired
    private RegRepo regRepo;
    @Autowired
    private BomGenerator bomGenerator;

    public AllocateRegsOutput doIt(AllocateRegsInput input) throws CommonException {
        try {
            //parse parm
            ParamReg param = new ParamReg(input);
            param.parser(input);
            param.tpcNeInfo = neInfoConfig.getNeInfo(param.vendorName, param.productType, NodeType.TPC4.name());

            //allocate
            ImmutableTriple<NewOchTunnel, Map<String, Node>, List<Node>> newOchTunnelInfo = allocateNewOchReg(param);
            NewOchTunnel newOchTunnel = newOchTunnelInfo.getLeft();
            Map<String, Node> totalNodeMap = newOchTunnelInfo.getMiddle();
            List<Node> reusedNodeSnapshot = newOchTunnelInfo.getRight();

            //build BOM
            BomInfo bomInfo;
            try {
                bomInfo = bomGenerator.constructBomInfo(totalNodeMap, reusedNodeSnapshot);
            } catch (NeDesignerException e) {
                log.error("Failed to construct output.", e);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "Failed to generate BOM." + e.getCause().getMessage(), e);
            }

            //construct output
            Vendor vendor = new VendorBuilder().setVendorName(param.vendorName).setProductType(param.productType)
                    .setNewOchTunnel(newOchTunnel)
                    .build();
            List<ReusedNodesSnapshot> reusedNodesSnapshot = reusedNodeSnapshot.stream().map(node -> RouteYangDataConverter.getReusedNodesSnapshot(node))
                    .collect(Collectors.toList());
            List<Nodes> nodes = totalNodeMap.values().stream().map(node -> RouteYangDataConverter.getTunnelNodes(node)).collect(Collectors.toList());

            TunnelAllocateResult2 tunnelAllocateResult = new TunnelAllocateResult2Builder()
                    .setReusedNodesSnapshot(reusedNodesSnapshot)
                    .setVendor(Arrays.asList(vendor))
                    .setNodes(nodes).build();

            return new AllocateRegsOutputBuilder().setTunnelAllocateResult2(tunnelAllocateResult)
                    .setBomInfo(new BomInfoBuilder(bomInfo).build())
                    .setReturnCode(RpcResultType.Success).build();

        } catch (Exception e) {
            log.error("Failed to allocate REG tunnels for input:{}", input, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage());
        }


    }

    private ImmutableTriple<NewOchTunnel, Map<String, Node>, List<Node>> allocateNewOchReg(ParamReg param) throws NeDesignerException {

        Map<String, Node> tempNodeMap = new HashMap<>();//note: key is ip OR siteId
        List<Node> reusedNodeSnapShot = new ArrayList<>();//note:store the snapshot for existed node, when got from db
        //prepare input from primary
        List<RegSegment> primarySeg = new ArrayList<>();
        for (RegSegments segment : param.primarySegments) {
            Node srcNode = getNode(segment.getSource().getIp(), segment.getSource().getSite(), param, tempNodeMap, reusedNodeSnapShot);
            Node destNode = getNode(segment.getDestination().getIp(), segment.getDestination().getSite(), param, tempNodeMap, reusedNodeSnapShot);

            primarySeg.add(new RegSegment(srcNode.getNodeId().getValue(), segment.getSource().getTpFriendlyName(), destNode.getNodeId().getValue(), segment.getDestination().getTpFriendlyName()));
        }

        //prepare input from secondary
        List<RegSegment> secondarySeg = null;
        if (param.secondarySegments != null && !param.secondarySegments.isEmpty()) {
            secondarySeg = new ArrayList<>();
            for (RegSegments segment : param.secondarySegments) {
                Node srcNode = getNode(segment.getSource().getIp(), segment.getSource().getSite(), param, tempNodeMap, reusedNodeSnapShot);
                Node destNode = getNode(segment.getDestination().getIp(), segment.getDestination().getSite(), param, tempNodeMap, reusedNodeSnapShot);

                secondarySeg.add(
                        new RegSegment(srcNode.getNodeId().getValue(), segment.getSource().getTpFriendlyName(), destNode.getNodeId().getValue(), segment.getDestination().getTpFriendlyName()));
            }
        }

        RegInput regInput = RegInput.builder()
                .tpcNeInfo(param.tpcNeInfo)
                .clientMedium(param.getClientMedium())
                .lineSignalRate(param.getLinePortSignalRate())
                .tunnelSignalRate(param.getTunnelSignalRate())
                .plane(param.getPlaneName())
                .riskGroupName(param.getRiskGroupName())
                .primarySegments(primarySeg)
                .secondarySegments(secondarySeg)
                .vendorName(param.vendorName)
                .vendorType(param.productType)
                .srcSite(param.getSrcSite().getNodeId().getValue())
                .destSite(param.getDesSite().getNodeId().getValue())
                .tunnelNumber(param.bundleNumber)
                .totalNodes(tempNodeMap.values())
                .centFreq(param.centFreq)
                .build();

        //For reg, every time only allocate one OCH
        Pair<TunnelRouteInfos, Map<String, Node>> allocateResult = regRepo.allocate(regInput);

        TunnelRouteInfos tunnelRouteInfo = allocateResult.getLeft();
        Map<String, Node> totalNodeMap = allocateResult.getRight();//key is the nodeId

        NewOchTunnel newOchTunnel = new NewOchTunnelBuilder().setTunnelRouteInfos(Arrays.asList(tunnelRouteInfo)).build();
        return ImmutableTriple.of(newOchTunnel, totalNodeMap, reusedNodeSnapShot);
    }

    private Node getNode(String ip, String siteId, ParamReg param, Map<String, Node> totalInNodes, List<Node> reusedNodeSnapShot) throws NeDesignerException {
        Node node;
        if (ip != null && !ip.isEmpty()) {
            node = totalInNodes.get(ip);
            if (node != null) {
                return node;
            }
            //fetch node from DB by ip
            node = phyNodeDao.getConfigPhyNodeByIp(ip);
            if (node != null) {
                reusedNodeSnapShot.add(node);//node exist in DB already
                totalInNodes.put(ip, node);
                return node;
            }
        }

        if (siteId == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "Not found mandatory SiteId in segment");
        }
        node = totalInNodes.get(siteId);
        if (node == null) {
            log.info("Failed to find phy node in DB by ip :{},try to create new node with siteId:{}", ip, siteId);
            node = neNodeRepo.createUnmonitoredNode(siteId, NodeType.TPC4, param.tpcNeInfo, param.getPlaneName(),param.getPlaneId(), param.getRiskGroupName(), ip, NeSubType.EPC_REG);
            totalInNodes.put(siteId, node);
        }

        return node;
    }
}

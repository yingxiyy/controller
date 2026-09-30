/*
 *
 *  * Copyright (c) 2021-2020 Network Flex Any Comp. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package net.flex.dci.otn.controller.allocate.link.site;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otn.controller.allocate.common.RouteYangDataConverter;
import net.flex.dci.otn.controller.allocate.designer.NeDesigner;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.NodeUtils;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import net.flex.dci.otn.controller.allocate.designer.model.site.ReallocateDataModel;
import net.flex.dci.otn.controller.allocate.designer.reallocate.ReallocateEquipRepo;
import net.flex.dci.otn.controller.allocate.link.common.BomGenerator;
import net.flex.dci.otn.controller.allocate.common.YangDataConverter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.bom.rev200710.BomInfo;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.bom.rev200710.ne.bom.group.info.site.NeBomInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.AllocateLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.AllocateLinkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.AllocateLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.allocate.link.output.BomInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.allocate.link.output.LinkComputeResult;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.allocate.link.output.LinkComputeResultBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.allocate.link.output.ReallocateUiNeInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.allocate.link.output.ReallocateUiNeInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.allocate.link.output.ReusedNodesSnapshot;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.allocate.link.output.ReusedNodesSnapshotBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.link.compute.result.Main;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.link.compute.result.Slave;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.link.compute.result.Third;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.reallocate.info.ReallocateEquipment;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.reallocate.ui.ne.info.FinalNodes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.reallocate.ui.ne.info.FinalNodesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.reallocate.ui.ne.info.InitNodes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.reallocate.ui.ne.info.InitNodesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.reused.nodes.snapshot.ConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.reused.nodes.snapshot.OpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ui.ne.info.Nodes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ui.ne.info.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ui.ne.info.nodes.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ui.ne.info.nodes.EquipmentsBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class SiteLinkAllocator {

    private static final String VALIDATE_ERROR_1 = "利旧设备：%s不存在%s";
    private static final String VALIDATE_ERROR_2 = "设备(%s)的槽位(%s)已经被占用了%s";
    private static final String VALIDATE_ERROR_3 = "CMUX盘必须和Main的OA盘一起移动%s";
    private static final String VALIDATE_ERROR_4 = "CMUX盘必须和MUX Panel盘一起移动%s";
    private static final String VALIDATE_ERROR_5 = " 一个设备上只能有一个MUX/CMUX,改利旧设备不可用%s";
    private static final String VALIDATE_ERROR_END = ", 请重新选择利旧方案，然后生成方案";
    private static final String VALIDATE_ERROR_6 = "设备%s结构不正常%s";
    public static final String INIT_MAIN_OA_SLOT = "1";

    @Autowired
    private PhyNodeDao phyNodeDao;
    @Autowired
    private NeDesigner neDesigner;
    @Autowired
    private BomGenerator bomGenerator;
    @Autowired
    private NodeUtils nodeUtils;
    @Autowired
    private ReallocateEquipRepo reallocateEquipRepo;

    public AllocateLinkOutput doIt(AllocateLinkInput input) throws CommonException {
        log.debug("Allocate siteLink start...");
        ReallocateDataModel reallocateDataModel;
        try {
            reallocateDataModel = getReallocateData(input);
        } catch (ValidateException e) {
            log.error("Invalid reallocate input:", input, e);

            //移动不符合规则，丢出错信息给UI，指导客户下一步操作
            log.error("Not valid reallocate action.{}", e.getMessage());
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, e.getMessage());

        } catch (Exception e) {
            log.error("Failed to get ReallocateDataModel", e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "failed to get ReallocateDataModel.", e);
        }

        //reallocate and generate BOM
        RouteInfo routeInfoInput = RouteYangDataConverter.getRouteInfo(input.getMain(), input.getSlave(), input.getThird());
        RouteInfo routeInfoOutput;
        try {
            routeInfoOutput = neDesigner.reallocateSite(routeInfoInput, reallocateDataModel);
        } catch (NeDesignerException e) {
            log.error("Failed to reallocate by ne designer.", e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "neDesigner error" + e.getCause().getMessage(), e);
        }

        //construct output
        try {
            LinkComputeResult linkComputeResult = constructLinkComputeResult(routeInfoOutput);

            List<Nodes> reusedNodesConfig = YangDataConverter.convertToUINodeList(reallocateDataModel.getReusedNodesSnapshot());
            List<Nodes> reusedNodesOp = YangDataConverter.convertToUINodeList(reallocateDataModel.getReusedNodesSnapshotOp());
            ReusedNodesSnapshot reusedNodesSnapshot = new ReusedNodesSnapshotBuilder()
                    .setConfig(new ConfigBuilder().setNodes(reusedNodesConfig).build())
                    .setOp(new OpBuilder().setNodes(reusedNodesOp).build())
                    .build();

            BomInfo bomInfo;
            try {
                bomInfo = constructBomInfo(routeInfoOutput, input.getVendorName(), input.getProductType(), reusedNodesOp, reusedNodesConfig);
            } catch (NeDesignerException e) {
                log.error("Failed to construct output.", e);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "Failed to generate BOM." + e.getCause().getMessage(), e);
            }
            ReallocateUiNeInfo reallocateUiNeInfo = constructReallocateUiNeInfo(input, linkComputeResult);

            return new AllocateLinkOutputBuilder()
                    .setLinkComputeResult(linkComputeResult)
                    .setBomInfo(new BomInfoBuilder(bomInfo).build())
                    .setReallocateUiNeInfo(reallocateUiNeInfo)
                    .setReusedNodesSnapshot(reusedNodesSnapshot)
                    .setReturnCode(RpcResultType.Success).build();

        } catch (CommonException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to construct output.", e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "construct output error" + e.toString(), e);
        }

    }

    private ReallocateDataModel getReallocateData(AllocateLinkInput input) throws NeDesignerException {

        //获取初始方案中的所有nodeId，这个在混site移动场景会使用
        Map<String, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.link.route.Nodes> nodeIdsInComputeResult = input.getMain().getNodes().stream()
                .collect(Collectors.toMap(item -> item.getNodeId().getValue(), Function.identity()));
        if (input.getSlave() != null && input.getSlave().getNodes() != null) {
            nodeIdsInComputeResult.putAll(input.getSlave().getNodes().stream().collect(Collectors.toMap(item -> item.getNodeId().getValue(), Function.identity())));
        }
        if (input.getThird() != null && input.getThird().getNodes() != null) {
            nodeIdsInComputeResult.putAll(input.getThird().getNodes().stream().collect(Collectors.toMap(item -> item.getNodeId().getValue(), Function.identity())));
        }

        //数据库获取利旧node，作为镜像
        List<ReallocateEquipment> reallocateEquipments = input.getReallocateEquipment();

        List<String> reusedNodeIds = reallocateEquipments.stream().map(ReallocateEquipment::getNewNodeId).distinct().collect(Collectors.toList());
        List<Node> reusedNodesSnapshot = phyNodeDao.listConfigPhyNodeByIds(reusedNodeIds);
        Map<String, Node> reusedNodesSnapshotMap = reusedNodesSnapshot.stream().collect(Collectors.toMap(n -> n.getNodeId().getValue(), Function.identity()));//key is nodeId

        //group by old nodeId
        Map<String, List<ReallocateEquipment>> reallocateNodeEquipMap = reallocateEquipments.stream().filter(item -> item.getNewNodeId() != null)
                .collect(Collectors.groupingBy(item -> PhysicalNodeIdNamingRule.getNodeId(item.getOldEquipId())));

        Map<String, Map<String, String>> reallocateNodeEquipMapModel = new HashMap<>();
        Map<String, String> reallocateEquipMap = new HashMap<>();

        Map<String, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.link.route.Nodes> oldNodes = getNodesByIds(input, reallocateNodeEquipMap.keySet());

        for (Map.Entry<String, List<ReallocateEquipment>> entry : reallocateNodeEquipMap.entrySet()) {

            String oldNodeId = entry.getKey();
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.link.route.Nodes oldNode = oldNodes.get(oldNodeId);
            if (oldNode == null) {
                String msg = String.format("Failed to find node: %s from compute result.", oldNodeId);
                log.error(msg);
                throw new NeDesignerException(msg);
            }
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments> oldEquips = oldNode.getPhysical().getEquipments();
            List<ReallocateEquipment> items = entry.getValue();
            Set<String> oldEquipIdsMoved = items.stream().map(ReallocateEquipment::getOldEquipId).collect(Collectors.toSet());
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments> oldEquipsMoved =
                    oldEquips.stream().filter(e -> oldEquipIdsMoved.contains(e)).collect(Collectors.toList());

            validate(reusedNodesSnapshotMap, items, oldEquipsMoved, nodeIdsInComputeResult);

            //准备数据
            Map<String, String> oldNewEquipMap = items.stream()
                    .collect(Collectors.toMap(ReallocateEquipment::getOldEquipId, item -> reallocateEquipRepo.reallocateEquipId(item.getOldEquipId(), item.getNewNodeId(), item.getNewSlot())));
            reallocateNodeEquipMapModel.put(oldNodeId, oldNewEquipMap);
            reallocateEquipMap.putAll(oldNewEquipMap);

        }

        //准备数据，为site（重用compute结果里面的node）的场景
        //group by reused nodeId in compute result
        Map<String, List<ReallocateEquipment>> reallocateMapInsideCompute = reallocateEquipments.stream().filter(item -> nodeIdsInComputeResult.containsKey(item.getNewNodeId()))
                .collect(Collectors.groupingBy(ReallocateEquipment::getNewNodeId));

        //准备reallocate vendor数据
        Map<String, String> reallocateEquipVendorMap = new HashMap<>();
        for (ReallocateEquipment reallocateEquipment : reallocateEquipments) {
            String newCardVendorType = reallocateEquipment.getNewCardVendorType();
            if (newCardVendorType == null || newCardVendorType.isEmpty()) {
                continue;
            }
            String oldEquipId = reallocateEquipment.getOldEquipId();
            String newEquipId = reallocateEquipMap.get(oldEquipId);
            if (newEquipId == null) {
                newEquipId = oldEquipId;
            }
            reallocateEquipVendorMap.put(newEquipId, newCardVendorType);
        }

        List<Node> reusedNodesSnapshotOp = phyNodeDao.listOperPhyNodeByIds(reusedNodeIds);
        return ReallocateDataModel.builder()
                .reusedNodesSnapshot(reusedNodesSnapshot)
                .reusedNodesSnapshotOp(reusedNodesSnapshotOp)
                .reallocateNodeEquipMap(reallocateNodeEquipMapModel)
                .reallocateEquipMap(reallocateEquipMap)
                .reallocateMapInsideCompute(reallocateMapInsideCompute)
                .reallocateEquipVendorMap(reallocateEquipVendorMap)
                .build();
    }

    private void validate(Map<String, Node> reusedNodesSnapshotMap, List<ReallocateEquipment> items,
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments> oldEquipsMoved,
            Map<String, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.link.route.Nodes> nodeIdsInComputeResult) {
        //检查移动是否符合移动规则
        boolean hasMainOa = false;
        boolean hasMux = false;
        boolean hasCMux = false;
        boolean hasMuxPanel = false;
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments equip : oldEquipsMoved) {
            if (equip.getEquipType().equals(EquipType.CMUX64)) {
                hasCMux = true;
                continue;
            }
            if (equip.getEquipType().equals(EquipType.MUXPANEL)) {
                hasMuxPanel = true;
                continue;
            }
            if (equip.getEquipType().equals(EquipType.MUX)) {
                hasMux = true;
                continue;
            }
            if (equip.getEquipType().equals(EquipType.OA) && equip.getSlot().equals(INIT_MAIN_OA_SLOT)) {//compute link得到的初始值，main的OA卡，总是1槽位
                hasMainOa = true;
                continue;
            }
        }
        String msg;
        if (hasMainOa != hasCMux) {
            throw new ValidateException(VALIDATE_ERROR_3);
        }
        if (hasCMux != hasMuxPanel) {
            throw new ValidateException(VALIDATE_ERROR_4);
        }

        //检查利旧的node是否可用
        for (ReallocateEquipment reallocateEquipment : items) {
            String newNodeId = reallocateEquipment.getNewNodeId();
            if (newNodeId == null || newNodeId.isEmpty()) {
                continue;
            }
            Node reusedNode = reusedNodesSnapshotMap.get(newNodeId);
            //node是否存在
            if (reusedNode == null) {
                if (nodeIdsInComputeResult.containsKey(newNodeId)) {
                    //对于混site的情况，因为只是测试场景，所以就暂时不做node是否可以重用的validate了
                    continue;
                }
                log.error("Failed to find node: {} in db.", newNodeId);
                msg = String.format(VALIDATE_ERROR_1, newNodeId, VALIDATE_ERROR_END);
                throw new ValidateException(msg);
            }
            canNodeReused(reusedNode, reallocateEquipment.getNewSlot(), hasCMux, hasMux);
        }
    }

    private void canNodeReused(Node node, String newSlot, boolean hasCMux, boolean hasMux) {
        String msg;

        //node是否满，槽位是否可用
        if (nodeUtils.isStuffed(node) || nodeUtils.getLineCardUsedSlots(node).contains(Integer.parseInt(newSlot))) {
            log.error("Slot: {} is not available for node {}", newSlot, node.getNodeId().getValue());
            msg = String.format(VALIDATE_ERROR_2, node.getNodeId().getValue(), newSlot, VALIDATE_ERROR_END);
            throw new ValidateException(msg);
        }

        // 一个网元上只能有一个MUX/CMUX
        if (hasCMux || hasMux) {
            try {
                if (nodeUtils.hasCmuxMux(node)) {
                    msg = String.format(VALIDATE_ERROR_5, VALIDATE_ERROR_END);
                    log.error(msg);
                    throw new ValidateException(msg);
                }
            } catch (NeDesignerException e) {
                log.error("Invalid node :{}", node.getNodeId().getValue());
                msg = String.format(VALIDATE_ERROR_6, VALIDATE_ERROR_END);
                throw new ValidateException(msg);
            }
        }
    }

    private Map<String, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.link.route.Nodes> getNodesByIds(AllocateLinkInput input, Set<String> nodeIds) {
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.link.route.Nodes> nodes = new ArrayList<>(input.getMain().getNodes());
        if (input.getSlave() != null && input.getSlave().getNodes() != null && !input.getSlave().getNodes().isEmpty()) {
            nodes.addAll(input.getSlave().getNodes());
        }
        if (input.getThird() != null && input.getThird().getNodes() != null && !input.getThird().getNodes().isEmpty()) {
            nodes.addAll(input.getThird().getNodes());
        }

        return nodes.stream().filter(item -> nodeIds.contains(item.getNodeId().getValue())).collect(Collectors.toMap(n -> n.getNodeId().getValue(), Function.identity()));
    }


    private BomInfo constructBomInfo(RouteInfo routeInfoOutput, String vendorName, String productType, List<Nodes> reusedNodesSnapshotOp,
            List<Nodes> reusedNodesConfig) throws NeDesignerException {
        //获取重用node的镜像，首先比较op，如果op有，用op，如果op没得，config有，用config
        List<Nodes> reusedNodesSnapshot = new ArrayList<>(Stream.of(reusedNodesSnapshotOp, reusedNodesConfig).flatMap(List::stream)
                .collect(Collectors.toMap(Nodes::getNodeId, d -> d, (Nodes x, Nodes y) -> x == null ? y : x)).values());

        List<Node> bomNodeList = new ArrayList<>(routeInfoOutput.getMain().getNodes());
        if (routeInfoOutput.getSlave() != null && routeInfoOutput.getSlave().getNodes() != null) {
            bomNodeList.addAll(routeInfoOutput.getSlave().getNodes());
        }
        if (routeInfoOutput.getThird() != null && routeInfoOutput.getThird().getNodes() != null) {
            bomNodeList.addAll(routeInfoOutput.getThird().getNodes());
        }
        Map<String, Node> bomNodes = bomNodeList.stream().collect(Collectors.toMap(node -> node.getNodeId().getValue(), Function.identity(), (n1, n2) -> n2));
        Map<String, Map<String, NeBomInfo>> bomMap = bomGenerator.generateBomMap(bomNodes, vendorName, productType, reusedNodesSnapshot);
        return bomGenerator.constructBomInfo(bomMap);

    }

    private ReallocateUiNeInfo constructReallocateUiNeInfo(AllocateLinkInput input, LinkComputeResult linkComputeResult) {
        //InitNode
        InitNodes initNodes = new InitNodesBuilder()
                .setMain(getMainUiNeInfo(input.getMain()))
                .setSlave(getSlaveUiNeInfo(input.getSlave())).build();
        //FinalNodes
        FinalNodes finalNodes = new FinalNodesBuilder()
                .setMain(getMainUiNeInfo(linkComputeResult.getMain()))
                .setSlave(getSlaveUiNeInfo(linkComputeResult.getSlave())).build();

        return new ReallocateUiNeInfoBuilder()
                .setReallocateEquipment(input.getReallocateEquipment())
                .setFinalNodes(finalNodes)
                .setInitNodes(initNodes).build();
    }

    static LinkComputeResult constructLinkComputeResult(RouteInfo routeInfoOutput) {
        //main
        Main main = RouteYangDataConverter.getMain(routeInfoOutput.getMain());

        //slave
        Slave slave = RouteYangDataConverter.getSlave(routeInfoOutput.getSlave());

        //third
        Third third = RouteYangDataConverter.getThird(routeInfoOutput.getThird());

        return new LinkComputeResultBuilder().setMain(main).setSlave(slave).setThird(third).build();
    }

    private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ui.ne.info.group.Slave getSlaveUiNeInfo(Slave slave) {
        if (slave == null) {
            return null;
        }
        return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ui.ne.info.group.SlaveBuilder()
                .setNodes(convertToUINodeList(slave.getNodes()))
                .build();
    }

    private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ui.ne.info.group.Main getMainUiNeInfo(Main main) {
        return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ui.ne.info.group.MainBuilder()
                .setNodes(convertToUINodeList(main.getNodes()))
                .build();
    }

    private List<Nodes> convertToUINodeList(List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.link.route.Nodes> nodesList) {
        if (nodesList == null || nodesList.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        List<Nodes> output = new ArrayList<>();
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.link.route.Nodes node : nodesList) {
            List<Equipments> equipments = node.getPhysical().getEquipments().stream().map(item -> new EquipmentsBuilder(item).build()).collect(Collectors.toList());
            output.add(new NodesBuilder().setNodeId(node.getNodeId().getValue()).setEquipments(equipments).build());
        }
        return output;
    }

    class ValidateException extends RuntimeException {

        public ValidateException(String errorMessage) {
            super(errorMessage);
        }
    }
}

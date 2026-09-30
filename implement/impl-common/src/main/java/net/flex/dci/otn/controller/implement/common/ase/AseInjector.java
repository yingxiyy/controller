package net.flex.dci.otn.controller.implement.common.ase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.optical.tool.RamanTransparentRoute;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otn.controller.implement.common.ase.ne.ConfigNeSequence;
import net.flex.dci.otn.controller.implement.common.ase.ne.ReadyResource;
import net.flex.dci.otn.controller.implement.common.ase.ne.SummaryAseSiteLinkState;
import net.flex.dci.otn.controller.implement.common.impl.ImplActionType;
import net.flex.dci.otn.controller.implement.common.impl.Step;
import net.flex.dci.otn.controller.implement.common.impl.StepToe;
import net.flex.dci.otn.controller.implement.common.lifecycle.LifeCycleSevice;
import net.flex.dci.otn.controller.implement.common.recorder.StepRecord;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.attributes.OCMGripGroups;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.attributes.OCMGripGroupsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;

/**
 * 主要用于复用段下假波的第一次下发，和deImpl 复用段的时候所有假波数据的删除 一个个下发太慢了，一个OCH下发1分钟，96个一个半小时了。 所以改算法 一次性下发，
 * 任务中心里面记录的不是一条条假波数据而是涉及的网元，和下发的数据
 */
@Slf4j
public class AseInjector {

    private String siteLinkId;
    private LifeCycleSevice lifeService;

    private ChangedObject changedObject;
    private final static MultipleTransaction mongoTransaction = SpringBeanFinder.getBean(
            MultipleTransaction.class);
    ;

    public AseInjector(String siteLinkId, LifeCycleSevice lifeService) {
        this.siteLinkId = siteLinkId;
        this.lifeService = lifeService;

        changedObject = new ChangedObject();
    }

    public void inject() throws Exception {
        Link siteLink = changedObject.getChangedSiteLink(siteLinkId);
        Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();

        log.info("start to inject ASE: {} {}", siteLink.getLinkId(),
                siteLinkAttr.getFriendlyName());
        DummyOchAllocatorOverSiteLink injector = new DummyOchAllocatorOverSiteLink(siteLink,
                changedObject);
        List<Link> dummyOchList = injector.allocateAllDummyOchs();  //allocate och 的时候已经把网元的XC 创建，并放入changedObject了

        RouteInfo rInfo = new RouteInfo();
        rInfo.parse(siteLink.getAugmentation(Link1.class).getSite().getExplictRoute().getRoute());

        List<Node> nonILANodeList = new ArrayList<>();
        List<Node> ilaNodeList = new ArrayList<>();
        groupNodeWithILA(rInfo, nonILANodeList, ilaNodeList);

        Map<String, TerminationPoint> tpNeedWrite2Nes = new HashMap<>();
//        配置OTS-1-1-LINE的CHANNEL模型的功率控制模式为APC, 新版本又不需要下发APC 门限， 和授权的功率自动控制范围 (范围这个是media-channel的）
        // 假波端口控制共用透明路由；普通复用段沿用原列表，不改实际设备配置路由。
        RamanTransparentRoute transparentRoute = new RamanTransparentRoute(siteLink, changedObject);
        tpNeedWrite2Nes = updateOtsLinePortParams(nonILANodeList,
                transparentRoute.hasTransparentEndpoints() ? transparentRoute.getEndpointTpIds() : rInfo.getTpIdList());

        Map<String, List<OCMGripGroups>> ocmData = new AseOcmGenerator().generate(siteLink,
                dummyOchList);
        List<Node> write2NeList = fetchWrite2Ne(nonILANodeList, ilaNodeList, tpNeedWrite2Nes,
                ocmData);

        buildWrite2DB(tpNeedWrite2Nes, ocmData);

        log.info("start async write ASE info to device");
        CompletableFuture<Void> future = new CompletableFuture<>();
        actionOnNes(write2NeList, ImplActionType.Implement, (success, error) -> {
            String friendlyName = siteLinkAttr.getFriendlyName();
            if (success) {
                log.info("Inject siteLink successfully: {}", friendlyName);
                future.complete(null); // 标记成功
            } else {
                String msg = String.format("Inject siteLink failure: %s", friendlyName);
                log.error(msg, error);
                future.completeExceptionally(error);
            }
        }); //after everything done, should save data to DB

        try {
            future.get(); // 阻塞等待所有处理完成

            mongoTransaction.save(changedObject);
            log.info("ASE inject has done.");
        } catch (InterruptedException | ExecutionException e) {
            throw e;
        }
    }

    /**
     * 生成需要存入数据库的数据
     *
     * @param tpNeedWrite2Nes //tpNeedWrite2Nes is not include ILA related
     * @param ocmData //这个网元中的交叉已经在create dummy och 的时候保存，无需再次处理
     */
    private void buildWrite2DB(Map<String, TerminationPoint> tpNeedWrite2Nes,
            Map<String, List<OCMGripGroups>> ocmData) {
        for (String nodeId : ocmData.keySet()) {
            Node dbNode = changedObject.getChangedPhyNode(nodeId);
            Physical dbNodeAttr = dbNode.getAugmentation(Node1.class).getPhysical();

            List<TerminationPoint> newTpList;
            TerminationPoint newTp = tpNeedWrite2Nes.get(nodeId);
            if (newTp != null) {
                newTpList = dbNode.getTerminationPoint().stream().map(tp -> {
                    if (tp.getTpId().getValue().equals(newTp.getTpId().getValue())) {
                        return newTp;
                    } else {
                        return tp;
                    }
                }).collect(Collectors.toList());
            } else {
                newTpList = dbNode.getTerminationPoint();
            }

            Node newNode = new NodeBuilder(dbNode)
                    .setTerminationPoint(newTpList)
                    .addAugmentation(Node1.class, new Node1Builder()
                            .setPhysical(
                                    new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder(
                                            dbNodeAttr)
                                            .setOCMGripGroups(ocmData.get(nodeId))
                                            .build())
                            .build())
                    .build();

            changedObject.addChangedPhyNode(newNode);
        }
    }

    public void remove() {
        Link siteLink = changedObject.getChangedSiteLink(siteLinkId);
        Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();
        lifeService = getSubLifeService(siteLinkAttr.getFriendlyName());

        String msg = null;
        if (siteLinkAttr.getDummyLink() != null) {
            log.info("start to remove injected ASE info from siteLink: {} {}", siteLink.getLinkId(),
                    siteLinkAttr.getFriendlyName());

            try {
                DummyOchAllocatorOverSiteLink injector = new DummyOchAllocatorOverSiteLink(siteLink,
                        changedObject);
                List<Link> needRemovedOchList = injector.getNeedRemovedOch();

                RouteInfo rInfo = new RouteInfo();
                rInfo.parse(siteLinkAttr.getExplictRoute().getRoute());

                List<Node> write2NeList = cleanXcAndOcmData(rInfo, needRemovedOchList);
                if (write2NeList == null) {
                    log.debug("no ASE related info need to clean up");
                    return;
                }
                lifeService.sendMsg();

                removeAseInfos(write2NeList, lifeService);

                mongoTransaction.save(changedObject);
                log.info("All ASE OCH removing has done.");
            } catch (Exception e) {
                msg = (e.getMessage() != null) ? e.getMessage() : e.toString();
                log.error(msg, e);
                throw e;
            } finally {
                lifeService.logEndLinkImpl(msg);
            }
        }
    }

    private void removeAseInfos(List<Node> write2NeList, LifeCycleSevice lifeService) {
        log.debug("Start remove ASE info....");
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        actionOnNes(write2NeList, ImplActionType.Deimplement, (success, throwable) -> {
            if (throwable != null) {
                log.error("write2Ne failed. ", throwable);
                future.completeExceptionally(throwable);
            } else {
                future.complete(success);
            }
        });

        try {
            future.get();
        } catch (InterruptedException e) {
            log.error("Thread was interrupted", e);
            Thread.currentThread().interrupt(); // Restore interrupt status
            throw new RuntimeException("Thread was interrupted during ASE removal");
        } catch (ExecutionException e) {
            log.error("Exception while waiting for NE action to complete", e);
            throw new RuntimeException(String.format("Failed to remove ASE info.", e.getCause()));
        }
    }

    private LifeCycleSevice getSubLifeService(String friendlyName) {
        LifeCycleSevice removeAseLifeService = new LifeCycleSevice();
        removeAseLifeService.buildLifeService(siteLinkId,
                TaskInfoMessage.ResourceType.siteLink,
                friendlyName,
                TaskInfoMessage.ActionType.removeAse,
                lifeService.getWhoDoesThis());

        removeAseLifeService.setGroupId(lifeService.getGroupId());
        return removeAseLifeService;
    }

    //删除网元上所有ASE相关的交叉，可以不通过dummyOchLink, 直接通过xc的description
    private List<Node> cleanXcAndOcmData(RouteInfo rInfo, List<Link> dummyOchList) {
        if (dummyOchList.isEmpty()) {
            return null;
        }

        List<Node> needWrite2Nes = rInfo.getNodeIdList().stream().map(nodeId -> {
            log.info("clean xc and ocm data nodeId:{}", nodeId);
            Node node = changedObject.getChangedPhyNode(nodeId);
            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
            List<CrossConnections> aseXcList = extractDummyXc(nodeAttr.getCrossConnections(),
                    ImplActionType.Deimplement);

            List<OCMGripGroups> emptyOcmGroup;
            if (nodeAttr.getOCMGripGroups() == null) {
                emptyOcmGroup = null;
            } else {
                emptyOcmGroup = nodeAttr.getOCMGripGroups().stream().map(ocmData -> {
                    return new OCMGripGroupsBuilder(ocmData)
                            .setChannels(new ArrayList<>())
                            .build();
                }).collect(Collectors.toList());
            }
            Node newNode = new NodeBuilder()
                    .setNodeId(node.getNodeId())
                    .setKey(new NodeKey(node.getNodeId()))
                    .addAugmentation(Node1.class, new Node1Builder().setPhysical(
                                    new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder()
                                            .setCrossConnections(aseXcList)
                                            .setOCMGripGroups(emptyOcmGroup)
                                            .setFriendlyName(nodeAttr.getFriendlyName())
                                            .setIp(nodeAttr.getIp())
                                            .build())
                            .build())
                    .build();

            removeXcAndOcmInDB(nodeId, emptyOcmGroup);
            log.info("current node is :{},name:{} ip:{}", newNode, nodeAttr.getFriendlyName(),
                    nodeAttr.getIp());
            return newNode;
        }).collect(Collectors.toList());

        return needWrite2Nes;
    }

    private void removeXcAndOcmInDB(String nodeId, List<OCMGripGroups> emptyOcmGroup) {
        Node node = changedObject.getChangedPhyNode(nodeId);
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        List<CrossConnections> nonAseXcList =
                nodeAttr.getCrossConnections().stream()
                        .filter(x -> !x.getCrossConnectionId().getValue()
                                .startsWith(PhysicalXcIdNamingRule.ASE_XC_PREFIX))
                        .collect(Collectors.toList());

        Node newNode = new NodeBuilder(node)
                .addAugmentation(Node1.class, new Node1Builder().setPhysical(
                                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder(
                                        nodeAttr)
                                        .setCrossConnections(nonAseXcList)
                                        .setOCMGripGroups(emptyOcmGroup)
                                        .build())
                        .build())
                .build();
        changedObject.addChangedPhyNode(newNode);
    }

    private void actionOnNes(List<Node> write2NeList, ImplActionType actionType,
            BiConsumer<Boolean, Throwable> onFinish) {
        StepToe toe = new StepToe();
        for (Node node : write2NeList) {
            log.info("current node:{}", node.getNodeId());
            ReadyResource readyResource = collectReadyResource(node, actionType);
            toe.addToe(new ConfigNeSequence(node, actionType, lifeService).prepare(readyResource));
        }
        Step<StepRecord> step = new Step(toe);
        step.start(new SummaryAseSiteLinkState(changedObject, write2NeList, siteLinkId, actionType,
                onFinish));
    }

    private ReadyResource collectReadyResource(Node node, ImplActionType actionType) {
        //默认全部资源都不ready
        return new ReadyResource();

//        //find out ready XC
//        ImplementState targetState = actionType.equals(ImplActionType.Implement) ? ImplementState.Implement : ImplementState.Allocate;
//        List<CrossConnections> xcList = node.getAugmentation(Node1.class).getPhysical().getCrossConnections();
//
//        ReadyResource readyResource = new ReadyResource();
//
//        //ILA 网元没有XC需要下发
//        readyResource.getXcList().addAll(xcList.stream().filter(xc -> xc.getImplementState().equals(targetState))
//            .collect(Collectors.toList()));
//
//        return readyResource;
    }

    private List<Node> fetchWrite2Ne(List<Node> nonILANodeList, List<Node> ilaNodeList,
            Map<String, TerminationPoint> newTpMap, Map<String, List<OCMGripGroups>> ocmData) {
        log.info("start prepare data write to device");
        //nonILA node的下发信息都类似， ILA node 没有交叉，只有ocm
        List<Node> needWrite2Nes = nonILANodeList.stream().map(node -> {
                    String nodeId = node.getNodeId().getValue();
                    Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

                    List<TerminationPoint> tpList = new ArrayList<>();
                    TerminationPoint newTp = newTpMap.get(node.getNodeId().getValue());
                    if (newTp == null) {
                        log.info("when ASE inject, no updated TP. {}({})", nodeAttr.getFriendlyName(),
                                nodeAttr.getIp() == null ? "" : nodeAttr.getIp());
                    } else {
                        tpList.add(newTp);
                        log.info("when ASE inject, updated TP. {}({}) {}", nodeAttr.getFriendlyName(),
                                nodeAttr.getIp() == null ? "" : nodeAttr.getIp(),
                                newTp.getTpId().getValue());
                    }
                    return new NodeBuilder().setNodeId(node.getNodeId())
                            .setKey(new NodeKey(node.getNodeId()))
                            .setTerminationPoint(tpList)
                            .addAugmentation(Node1.class, new Node1Builder()
                                    .setPhysical(
                                            new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder()
                                                    .setCrossConnections(extractDummyXc(
                                                            node.getAugmentation(Node1.class).getPhysical()
                                                                    .getCrossConnections(),
                                                            ImplActionType.Implement))
                                                    .setOCMGripGroups(ocmData.get(nodeId))
                                                    .setFriendlyName(nodeAttr.getFriendlyName())
                                                    .setIp(nodeAttr.getIp())
                                                    .build())
                                    .build())
                            .build();
                })
                .filter(node -> {
                    if (node == null) {
                        log.debug("[ILA节点处理] filter 1个无效null节点");
                        return false;
                    }
                    return true;
                }).collect(Collectors.toList());

        needWrite2Nes.addAll(ilaNodeList.stream().map(node -> {
            String nodeId = node.getNodeId().getValue();
            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

            return new NodeBuilder().setNodeId(node.getNodeId())
                    .setKey(new NodeKey(node.getNodeId()))
                    .setTerminationPoint(new ArrayList<>())
                    .addAugmentation(Node1.class, new Node1Builder()
                            .setPhysical(
                                    new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder()
                                            .setCrossConnections(new ArrayList<>())
                                            .setOCMGripGroups(ocmData.get(nodeId))
                                            .setFriendlyName(nodeAttr.getFriendlyName())
                                            .setIp(nodeAttr.getIp())
                                            .build())
                            .build())
                    .build();
        }).collect(Collectors.toList()));

        needWrite2Nes = needWrite2Nes.stream().filter(Objects::nonNull)
                .collect(Collectors.toList());
        return needWrite2Nes;
    }

    //才进入inject 的时候已经检查过，复用段上面没有已经Impl 了的业务。所以这个地方直接找ASE开头的假波
    private List<CrossConnections> extractDummyXc(List<CrossConnections> crossConnections,
            ImplActionType actionType) {
        if (actionType.equals(ImplActionType.Implement)) {
            return crossConnections.stream()
                    .filter(x -> x.getCrossConnectionId().getValue().startsWith(PhysicalXcIdNamingRule.ASE_XC_PREFIX))
                    .map(x -> new CrossConnectionsBuilder(x)
                            .setImplementState(ImplementState.Implement)
                            .setAdminState(AdminStatus.Up)
                            .build())
                    .collect(Collectors.toList());
        } else {
            return crossConnections.stream()
                    .filter(x -> x.getCrossConnectionId().getValue().startsWith(PhysicalXcIdNamingRule.ASE_XC_PREFIX))
                    .map(x -> new CrossConnectionsBuilder(x)
                            .setImplementState(ImplementState.Allocate)
                            .setAdminState(AdminStatus.Down)
                            .build())
                    .collect(Collectors.toList());
        }
    }

    /**
     * 获取 TP需要写到网元上的新属性，
     *
     * @param nonILANodeList
     * @param tpIdList
     * @return <nodeId, newTp>
     */
    private Map<String, TerminationPoint> updateOtsLinePortParams(List<Node> nonILANodeList,
            List<String> tpIdList) {
        log.info("start to update ase-related-ots port param");

        Map<String, TerminationPoint> updatedTps = new HashMap<>();
        for (String tpId : tpIdList) {
            for (Node node : nonILANodeList) {
                if (tpId.contains(node.getNodeId().getValue())) {
                    TerminationPoint lineTp = node.getTerminationPoint().stream()
                            .filter(tp -> tp.getTpId().getValue().equals(tpId) && (
                                    tp.getAugmentation(TerminationPoint1.class).getPhysical()
                                            .getPortType().equals(PortType.OALine) ||
                                            tp.getAugmentation(TerminationPoint1.class)
                                                    .getPhysical().getPortType()
                                                    .equals(PortType.ILALINEA) ||
                                            tp.getAugmentation(TerminationPoint1.class)
                                                    .getPhysical().getPortType()
                                                    .equals(PortType.ILALINEB)))
                            .findAny().orElseThrow(() -> new CommonException(
                                    CommonExceptionType.INTERNAL_SERVICE_ERROR,
                                    "cannot find required TP in node " + tpId));

                    TerminationPoint newLineTp = changeTpAttribute(lineTp);
                    updatedTps.put(node.getNodeId().getValue(), newLineTp);
                    break;
                }
            }
        }
        return updatedTps;
    }

    private TerminationPoint changeTpAttribute(TerminationPoint lineTp) {
        TerminationPoint newLineTp = new TerminationPointBuilder(lineTp)
                .addAugmentation(TerminationPoint1.class,
                        new TerminationPoint1Builder().setPhysical(
                                        new PhysicalBuilder(lineTp.getAugmentation(TerminationPoint1.class)
                                                .getPhysical())
                                                .setFriendlyName(
                                                        lineTp.getAugmentation(TerminationPoint1.class)
                                                                .getPhysical().getFriendlyName())
                                                .setProperties(PropertyTool.addProperty(
                                                        lineTp.getAugmentation(TerminationPoint1.class)
                                                                .getPhysical().getProperties(),
                                                        "channel-optical-power-adjustment.control-mode",
                                                        "MANUAL"))
                                                .build())
                                .build())
                .build();

        return newLineTp;
    }


    private void groupNodeWithILA(RouteInfo rInfo, List<Node> nonILANodeList,
            List<Node> ilaNodeList) {
        for (String eqId : rInfo.getEqIdList()) {
            String nodeId = PhysicalEqpIdNamingRule.getNodeId(eqId);
            Node node = changedObject.getChangedPhyNode(nodeId);
            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

            Optional<Equipments> eqOp = nodeAttr.getEquipments().stream()
                    .filter(eq -> eq.getEquipmentId().equals(eqId))
                    .findAny();
            if (!eqOp.isPresent()) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "cannot find the eq, which existed in route info " + eqId);
            }

            Equipments eq = eqOp.get();
            if (eq.getEquipType().equals(EquipType.IRA) ||
                    eq.getEquipType().equals(EquipType.DGE) ||
                    eq.getEquipType().equals(EquipType.OA)) {

                nonILANodeList.add(node);
            } else if (eq.getEquipType().equals(EquipType.ILA)) {
                ilaNodeList.add(node);
            }
        }
    }


}

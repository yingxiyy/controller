package net.flex.dci.otn.controller.implement.common.impl;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otn.controller.implement.common.config.ImplConfig;
import net.flex.dci.otn.controller.implement.common.lifecycle.LifeCycleSevice;
import net.flex.dci.otn.controller.implement.common.recorder.StepRecord;
import net.flex.dci.otn.controller.implement.common.utils.NoIpConfigWriteLogger;
import net.flex.dci.otn.controller.implement.common.utils.NoIpConfigWriteLogger.NoIpDeviceError;
import net.flex.dci.otn.controller.implement.common.utils.SimulatorNeError;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.RemoveResourceOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.ne.result.FailObj;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.object.ids.Object;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.amplifier.attributes.AmplifierBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.attributes.OCMGripGroups;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.otu.line.attributes.ModelSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.physical.OtuLine;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.physical.OtuLineBuilder;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class ConfigNeSequence implements Callable<StepRecord> {

    public final static String STATUS_PENDING = "pending";
    public final static String STATUS_SUCCESS = "success";
    public final static String STATUS_FAILURE = "failure";
    private final static String FORMAT_BASE_INFO = "baseInfo@%s@%s";
    private final static String FORMAT_IL = "link@%s@%s";
    private final static String FORMAT_EQ = "equipment@%s@%s";
    private final static String FORMAT_XC = "xc@%s@%s";
    private final static String FORMAT_TP = "tp@%s@%s";
    private final static String FORMAT_OCM = "ocmInfo@%s@%s";
    private final static String FORMAT_OTHER = "ne@other";
    private final static String STEP_BASE_INFO = "baseInfo";
    private final static String STEP_IL = "internalLink";
    private final static String STEP_EQ = "equipment";
    private final static String STEP_FIX_XC = "fixXC";
    private final static String STEP_FLEX_XC = "flexXC";
    private final static String STEP_TP = "tp";
    private final static String STEP_OCM = "ocmInfo";

    private Node node;
    private Physical updateNodeAttr;
    private NodeType nodeType;

    private ImplActionType actionType;
    private NeManagerRpc neMgr;
    private MultipleTransaction mongoTransaction;
    private PhysicalNode physicalNode;

    private StepRecord stepRecord;
    private LifeCycleSevice lifeService;
    private boolean saveAfterACtion;

    //    private static final PhyNodeDao phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);
    private ChangedObject changedObject;

    private ImplConfig implConfig = SpringBeanFinder.getBean(ImplConfig.class);
    private Gson gson = new Gson();

    public ConfigNeSequence(ChangedObject changedObject, Node node, ImplActionType actionType, LifeCycleSevice lifeService) {
        this.node = node;
        this.actionType = actionType;
        this.lifeService = lifeService;

        neMgr = SpringBeanFinder.getBean(NeManagerRpc.class);
        mongoTransaction = SpringBeanFinder.getBean(MultipleTransaction.class);
        physicalNode = new PhysicalNode(actionType);
        nodeType = node.getAugmentation(Node1.class).getPhysical().getNodeType();

        updateNodeAttr = node.getAugmentation(Node1.class).getPhysical();

        stepRecord = new StepRecord(node.getNodeId().getValue(), updateNodeAttr.getFriendlyName(),
                updateNodeAttr.getIp() == null ? "" : updateNodeAttr.getIp());

        this.changedObject = changedObject;
    }

    public Callable<StepRecord> prepare(Map<String, PhysicalNode.ReadyResource> readyResourceMap) {
        //configNeSequence 顺序执行，只会有一步错误，错误信息保存与result
        List<String> actionSequencies = new ArrayList<>();

        boolean hasFlexXc = hasFlexXc();
        switch (actionType) {
            case Implement:
//                if (updateNodeAttr.getImplementState().equals(ImplementState.Allocate))
                actionSequencies.add(STEP_BASE_INFO);

                if (updateNodeAttr.getEquipments() != null && !updateNodeAttr.getEquipments().isEmpty()) {
                    actionSequencies.add(STEP_EQ);
                }

                if (node.getTerminationPoint() != null && !node.getTerminationPoint().isEmpty()) {
                    actionSequencies.add(STEP_TP);
                }

                if (updateNodeAttr.getInternalLinks() != null && !updateNodeAttr.getInternalLinks().isEmpty()) {
                    actionSequencies.add(STEP_IL);
                }

                if (updateNodeAttr.getCrossConnections() != null && !updateNodeAttr.getCrossConnections().isEmpty()) {
                    actionSequencies.add(STEP_FIX_XC);
                    if (hasFlexXc) {
                        actionSequencies.add(STEP_FLEX_XC);
                    }
                }
                if (nodeType.equals(NodeType.OD)) {
                    if (lifeService.getTaskInfoMessage() != null && lifeService.getTaskInfoMessage().getResourceType() != null && lifeService.getTaskInfoMessage().getResourceType().equals(TaskInfoMessage.ResourceType.ochlink)) {
                        actionSequencies.add(STEP_OCM);
                    }
                }

                updatePropertyAfterSequence(actionSequencies, readyResourceMap, STATUS_PENDING);
                break;
            case Deimplement:
                if (hasFlexXc) {
                    actionSequencies.add(STEP_FLEX_XC);
                }
                if (nodeType.equals(NodeType.OD)) {
                    actionSequencies.add(STEP_OCM);
                } else {
                    actionSequencies.add(STEP_FIX_XC);
                }

                actionSequencies.add(STEP_TP);
                actionSequencies.add(STEP_IL);
                if (nodeType.equals(NodeType.OD)) {
                    actionSequencies.add(STEP_FIX_XC);
                }

                updatePropertyAfterSequence(actionSequencies, readyResourceMap, STATUS_PENDING);
                break;
        }

        lifeService.logStatusChanged(stepRecord);
        return this;
    }

    @Override
    /**
     * the sequence is:
     *    // basicInfo
     *    // internalLink
     *    // createXC
     *    // eq adminUp
     *    // tp adminUp
     *
     *    总是以成功方式放回，外面的futures.asList 检查result判断释放有错误
     */
    public StepRecord call() throws Exception {
        String dispName = updateNodeAttr.getIp() == null ? updateNodeAttr.getFriendlyName() : updateNodeAttr.getIp();
        Thread.currentThread().setName(dispName);

        log.debug("start write2NE {} ({})", node.getNodeId(), dispName);

        boolean hasFlexXc = hasFlexXc();
        try {
            saveAfterACtion = true;
            switch (actionType) {
                case Implement:
                {
//                        if (updateNodeAttr.getImplementState().equals(ImplementState.Allocate)) {
                    //basicInfo 只在第一次下发是写入
                    writeBasicInfo();
//                        }
                }
                makeEqImplState();

                makeTpUp();
                makeTransceiverUp();
                setEqAttr();

                createInternalLink();
                makeFixXcUp();
                if (hasFlexXc) {
                    createFlexXC();
                }
                if (nodeType.equals(NodeType.OD)) {
                    writeOcmInfo();
                }
                if (nodeType.equals(NodeType.TD)) {
                    setTpAdditional();  //一些TPC网元的TP属性需要再交叉创建后才可以下发，所以这里再次下发
                }
                break;
                case Deimplement:
//                    removeOtherAttributes();
                    if (hasFlexXc) {
                        removeFlexXC();
                    }
                    if (nodeType.equals(NodeType.OD)) {
                        writeOcmInfo();
                    } else {
                        makeFixXcDown();
                    }

                    makeTransceiverDown();
                    makeTpDown();
                    removeInternalLink();

                    if (nodeType.equals(NodeType.OD)) {
                        makeFixXcDown();
                    }
                    //                groupDown_TP_FixXC();
                    //                setTpAndTransceiverAttr();
                    //                makeTpAndTransceiverDown();
                    break;
            }
            updateNode(ImplementState.Implement);
        } catch (Exception e) {
            //!!重要这个地方不能抛异常，否则外层就丢失的stepRecord!!
            updateNode(ImplementState.PartialImplement);
//            Throwable root = ExceptionUtils.getRootCause(e);
//            String msg = (root != null ? root.getMessage() : e.getMessage());
            log.error("config NE error happen\n", ExceptionUtils.getRootCause(e));
//            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, msg);
        }

        return stepRecord;
    }

    private void updateNode(ImplementState implementState) {
        String nodeId = node.getNodeId().getValue();
        node = changedObject.getChangedPhyNode(nodeId);
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        Node newNode = new NodeBuilder(node)
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder(nodeAttr)
                                .setImplementState(implementState)
                                .build())
                        .build())
                .build();
        // The node may have been cached before the DB was directly marked ...ing.
        // Clear the stale original snapshot so store2DB rewrites the terminal node,
        // matching the 2606 full-rewrite behavior for NE action results.
        changedObject.unsetPhyNode(nodeId);
        changedObject.addChangedPhyNode(newNode);
    }

    //only keep adminDown attribute
    private void removeOtherAttributes() {
        log.debug("remove other attr");
        //1.tp
        List<TerminationPoint> newTPList = new ArrayList<>();
        for (TerminationPoint tp : node.getTerminationPoint()) {
            TerminationPoint newTp = new TerminationPointBuilder()
                    .setTpId(tp.getTpId())
                    .setKey(tp.getKey())
                    .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                            .setPhysical(
                                    new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder()
                                            .setAdminState(AdminStatus.Down)
                                            .setImplementState(ImplementState.Allocate)
                                            .setPortType(tp.getAugmentation(TerminationPoint1.class).getPhysical().getPortType())
                                            .setFriendlyName(
                                                    tp.getAugmentation(TerminationPoint1.class)
                                                            .getPhysical().getFriendlyName())
                                            .build())
                            .build())
                    .build();

            newTPList.add(newTp);
        }
        //2. updateNodeAttr
        List<Equipments> newEqList = new ArrayList<>();
        for (Equipments eq : updateNodeAttr.getEquipments()) {
            Equipments newEq = new EquipmentsBuilder()
                    .setEquipmentId(eq.getEquipmentId())
                    .setKey(eq.getKey())
                    .setAdminState(AdminStatus.Down)
                    .setImplementState(ImplementState.Allocate)
                    .setFriendlyName(eq.getFriendlyName())
                    .build();

            newEqList.add(newEq);
        }

        NodeBuilder newNode = new NodeBuilder(node)
                .setTerminationPoint(newTPList)
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder(updateNodeAttr)
                                .setEquipments(newEqList)
                                .build())
                        .build());

        node = newNode.build();
    }

    private void updatePropertyAfterSequence(List<String> actionSequencies,
                                             Map<String, PhysicalNode.ReadyResource> readyResourceMap, String status) {
        for (String seq : actionSequencies) {
            updateProperty(readyResourceMap, seq, status);
        }
    }

    private void updateProperty(Map<String, PhysicalNode.ReadyResource> readyResourceMap,
                                String seq, String status) {
        switch (seq) {
            case STEP_BASE_INFO:
                String neName = String.format(FORMAT_BASE_INFO, node.getNodeId().getValue(),
                        node.getAugmentation(Node1.class).getPhysical().getFriendlyName());
                stepRecord.updateProperty(neName, status);
                break;
            case STEP_OCM:
                String ocmName = String.format(FORMAT_OCM, node.getNodeId().getValue(),
                        node.getAugmentation(Node1.class).getPhysical().getFriendlyName());
                stepRecord.updateProperty(ocmName, status);
                break;
            case STEP_IL:
                for (InternalLinks il : updateNodeAttr.getInternalLinks()) {
                    String name = String.format(FORMAT_IL, il.getLinkRef(), il.getLinkName());
                    stepRecord.updateProperty(name, status);
                }
                if (readyResourceMap.get(node.getNodeId().getValue()) != null) {
                    for (InternalLinks il : readyResourceMap.get(node.getNodeId().getValue())
                            .getIlList()) {
                        //没有在上面的循环中找到，说明这个InternalLink已经和目标状态一致了
                        String name = String.format(FORMAT_IL, il.getLinkRef(), il.getLinkName());
                        stepRecord.updateProperty(name, STATUS_SUCCESS);
                    }
                }
                break;
            case STEP_EQ:
                for (Equipments eq : updateNodeAttr.getEquipments()) {
                    String name = String.format(FORMAT_EQ, eq.getEquipmentId(),
                            eq.getFriendlyName());
                    stepRecord.updateProperty(name, status);
                }
                if (readyResourceMap.get(node.getNodeId().getValue()) != null) {
                    for (Equipments eq : readyResourceMap.get(node.getNodeId().getValue())
                            .getEqList()) {
                        //没有在上面的循环中找到，说明这个InternalLink已经和目标状态一致了
                        String name = String.format(FORMAT_EQ, eq.getEquipmentId(),
                                eq.getFriendlyName());
                        stepRecord.updateProperty(name, STATUS_SUCCESS);
                    }
                }
                break;
            case STEP_FIX_XC:
            case STEP_FLEX_XC:
                for (CrossConnections xc : updateNodeAttr.getCrossConnections()) {
                    String name = String.format(FORMAT_XC, xc.getCrossConnectionId().getValue(),
                            xc.getDescription());
                    stepRecord.updateProperty(name, status);
                }
                if (readyResourceMap.get(node.getNodeId().getValue()) != null) {
                    for (CrossConnections xc : readyResourceMap.get(node.getNodeId().getValue())
                            .getXcList()) {
                        //没有在上面的循环中找到，说明这个XC已经和目标状态一致了
                        String name = String.format(FORMAT_XC, xc.getCrossConnectionId().getValue(),
                                xc.getDescription());
                        stepRecord.updateProperty(name, STATUS_SUCCESS);
                    }
                }
                break;
            case STEP_TP:
                for (TerminationPoint tp : node.getTerminationPoint()) {
                    String name = String.format(FORMAT_TP, tp.getTpId().getValue(),
                            tp.getAugmentation(TerminationPoint1.class).getPhysical()
                                    .getFriendlyName());
                    stepRecord.updateProperty(name, status);
                }
                if (readyResourceMap.get(node.getNodeId().getValue()) != null) {
                    for (TerminationPoint tp : readyResourceMap.get(node.getNodeId().getValue())
                            .getTpList()) {
                        String name = String.format(FORMAT_TP, tp.getTpId().getValue(),
                                tp.getAugmentation(TerminationPoint1.class).getPhysical()
                                        .getFriendlyName());
                        stepRecord.updateProperty(name, STATUS_SUCCESS);
                    }
                }
                break;
        }
    }

    private boolean hasFlexXc() {
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

        Iterator<CrossConnections> iter = nodeAttr.getCrossConnections().iterator();
        while (iter.hasNext()) {
            CrossConnections xc = iter.next();
            if (!xc.isFixed()) {
                //in DCN network, only frequency XC has flex
                return true;
            }
        }
        return false;
    }


    /**
     * make tp and related transceiver down
     */
    @Deprecated
    private void makeTpAndTransceiverDown() {
        log.debug("makeTpDown {}",
                updateNodeAttr.getIp() == null ? "" : updateNodeAttr.getIp());
        writeTpAttributes(AdminStatus.Down);
        lifeService.logStatusChanged(stepRecord);
    }


    /**
     * only change impl state
     */
    @Deprecated
    private void writeTpAttributes(AdminStatus adminStatus) {
        List<Equipments> transList = new ArrayList<>();

        for (Equipments eq : updateNodeAttr.getEquipments()) {
            if (eq.getEquipType().equals(EquipType.TRANSCEIVER)) {
                transList.add(new EquipmentsBuilder()
                        .setEquipmentId(eq.getEquipmentId())
                        .setKey(eq.getKey())
                        .setImplementState(
                                adminStatus.equals(AdminStatus.Down) ? ImplementState.Allocate
                                        : ImplementState.Implement)
                        .setAdminState(adminStatus)
                        .build());
            }
        }
        List<TerminationPoint> tpList = new ArrayList<>();
        for (TerminationPoint tp : node.getTerminationPoint()) {
            tpList.add(new TerminationPointBuilder(tp)
                    .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                            .setPhysical(
                                    new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder()
                                            .setAdminState(adminStatus)
                                            .setImplementState(adminStatus.equals(AdminStatus.Down)
                                                    ? ImplementState.Allocate
                                                    : ImplementState.Implement)
                                            .build())
                            .build())
                    .build());
        }
        Node changedNode = new NodeBuilder(node)
                .setTerminationPoint(tpList)
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder()
                                .setIp(updateNodeAttr.getIp())
                                .setEquipments(transList)
                                .build())
                        .build())
                .build();

        try {
            StepResult result = write2Ne(changedNode);
            if (actionType.equals(ImplActionType.Implement)) {
                updateAfterTp(result, changedNode, ImplementState.Implement);
                updateAfterEq(result, transList, ImplementState.Implement);
            } else if (actionType.equals(ImplActionType.Deimplement)) {
                updateAfterTp(result, changedNode, ImplementState.Allocate);
                updateAfterEq(result, transList, ImplementState.Allocate);
            }
        } catch (Exception e) {
//            updatePropertyAfterSequence(getNextSequence(STEP_TP), STATUS_FAILURE);  //keep pending is correct
            throw e;
        }
    }


    /**
     * make tp
     */
    private void makeTpDown() {
        log.debug("makeTpDown {}",
                updateNodeAttr.getIp() == null ? "" : updateNodeAttr.getIp());

        List<TerminationPoint> terminationPointList;
        if (actionType.equals(ImplActionType.Deimplement) &&
                node.getAugmentation(Node1.class).getPhysical().getNodeType().equals(NodeType.TD)) {
            //deimpl 的时候TD设备的TP点参数不需要下发
            terminationPointList = tpDownRequiredForTD();

        } else {
            terminationPointList = node.getTerminationPoint();
        }

        writeTpAttributes(terminationPointList);
        lifeService.logStatusChanged(stepRecord);
    }

    /**
     * make tp and related transceiver up
     */
    private void makeTpUp() {
        log.debug("makeTpUp {}",
                updateNodeAttr.getIp() == null ? "" : updateNodeAttr.getIp());

        List<TerminationPoint> terminationPointList;
        if (actionType.equals(ImplActionType.Implement) &&
                node.getAugmentation(Node1.class).getPhysical().getNodeType().equals(NodeType.TD)) {
            //impl 的时候TD设备的TP点有些参数需要提前下发
            terminationPointList = tpUpRequiredForTD();
            writeTpAttributes(terminationPointList);
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            //对于电中继， 需要对L口配置loopback， 这个时候需要把端口变为maintenance
//            specRequireForRegPortUp();  useless in byteDance mode, it has one special mode=REG400G....
        } else {
            //最终确定由adapter 统一屏蔽
            terminationPointList = node.getTerminationPoint();
            writeTpAttributes(terminationPointList);
        }

        lifeService.logStatusChanged(stepRecord);
    }

    private void specRequireForRegPortUp() {

        List<TerminationPoint> regSpecTpList = node.getTerminationPoint()
                .stream().filter(tp -> {
                    if (PropertyTool.existProperty(tp.getAugmentation(TerminationPoint1.class).getPhysical().getProperties(), "regen-failure-propagated")) {
                        return true;
                    } else {
                        return false;
                    }
                }).collect(Collectors.toList());

        writeTpAttributes(makeTpUp_directly(regSpecTpList));

        writeTpAttributes(setLoopback(regSpecTpList));

        writeTpAttributes(makeTpUp_directly(regSpecTpList));
    }

    private List<TerminationPoint> makeTpUp_directly(List<TerminationPoint> needUpdate) {
        List<TerminationPoint> terminationPointList = needUpdate.stream()
                .map(tp -> {
                    TerminationPoint newTp = new TerminationPointBuilder(tp)
                            .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                                    .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder()
                                            .setFriendlyName(tp.getAugmentation(TerminationPoint1.class).getPhysical().getFriendlyName())
                                            .setAdminState(AdminStatus.Up)
                                            .setImplementState(ImplementState.Implement)
                                            .build())
                                    .build())
                            .build();
                    return newTp;
                }).filter(Objects::nonNull).collect(Collectors.toList());

        return terminationPointList;
    }

    private List<TerminationPoint> setLoopback(List<TerminationPoint> needUpdate) {
        List<TerminationPoint> terminationPointList = needUpdate.stream()
                .map(tp -> {
                    Properties prop = PropertyTool.addProperty(null, "loopback-mode", "FACILITY");
                    TerminationPoint newTp = new TerminationPointBuilder(tp)
                            .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                                    .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder()
                                            .setFriendlyName(tp.getAugmentation(TerminationPoint1.class).getPhysical().getFriendlyName())
                                            .setAdminState(AdminStatus.Maintenance)
                                            .setImplementState(ImplementState.Implement)
                                            .setProperties(prop)
                                            .build())
                                    .build())
                            .build();
                    return newTp;
                }).filter(Objects::nonNull).collect(Collectors.toList());

        return terminationPointList;
    }

    /**
     * make tp and related transceiver down
     */
    private void makeTransceiverDown() {
        log.debug("makeTransceiverDown {}",
                updateNodeAttr.getIp() == null ? "" : updateNodeAttr.getIp());
        makeTransceiverImplStateChange();
        lifeService.logStatusChanged(stepRecord);
    }

    /**
     * make tp and related transceiver up
     */
    private void makeTransceiverUp() {
        log.debug("makeTransceiverUp {}",
                updateNodeAttr.getIp() == null ? "" : updateNodeAttr.getIp());
        makeTransceiverImplStateChange();
        lifeService.logStatusChanged(stepRecord);
    }

//    private void setTpAndTransceiverAttr() {
//        log.debug("setTpAttr {}", updateNodeAttr.getIp() == null ? "" : updateNodeAttr.getIp());
//        changeTpAndTransceiverAttr();
//        lifeService.logStatusChanged(stepRecord);
//    }

    /**
     * only change impl state
     */
    private void makeTransceiverImplStateChange() {
        List<Equipments> transList = new ArrayList<>();
        for (Equipments eq : updateNodeAttr.getEquipments()) {
            if (eq.getEquipType().equals(EquipType.TRANSCEIVER)) {
                transList.add(eq);
            }
        }
        makeEqImplState(transList);
    }

//    private void changeTpAndTransceiverAttr() {
//        List<Equipments> transList = new ArrayList<>();
//
//        for (Equipments eq : updateNodeAttr.getEquipments()) {
//            if (eq.getEquipType().equals(EquipType.TRANSCEIVER)) {
//                transList.add(new EquipmentsBuilder(eq)
//                        .setImplementState(null)
//                        .setAdminState(null)
//                        .build());
//            }
//        }
//        List<TerminationPoint> tpList = new ArrayList<>();
//        for (TerminationPoint tp : node.getTerminationPoint()) {
//            tpList.add(new TerminationPointBuilder(tp)
//                    .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
//                            .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(tp.getAugmentation(TerminationPoint1.class).getPhysical())
//                                    .setImplementState(null)
//                                    .setAdminState(null)
//                                    .build())
//                            .build())
//                    .build());
//        }
//        Node changedNode = new NodeBuilder(node)
//                .setTerminationPoint(node.getTerminationPoint())
//                .addAugmentation(Node1.class, new Node1Builder()
//                        .setPhysical(new PhysicalBuilder()
//                                .setEquipments(transList)
//                                .build())
//                        .build())
//                .build();
//
//        try {
//            StepResult result = write2Ne(changedNode);
//            updateAfterTp(result, changedNode, isImplementAction ? ImplementState.Implement : ImplementState.Allocate);
//            updateAfterEq(result, changedNode, isImplementAction ? ImplementState.Implement : ImplementState.Allocate);
//        } catch (Exception e) {
////            updatePropertyAfterSequence(getNextSequence(STEP_TP), STATUS_FAILURE);  //keep pending is correct
//            throw e;
//        }
//    }

    /**
     * only change impl state
     */
    private void writeTpAttributes(List<TerminationPoint> terminationPointList) {
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        log.debug("update spec TP on {}, {} ",
                updateNodeAttr.getIp() == null ? "" : updateNodeAttr.getIp(),
                terminationPointList.stream()
                        .map(tp -> tp.getAugmentation(TerminationPoint1.class).getPhysical()
                                .getFriendlyName())
                        .collect(Collectors.toList()));

        Node changedNode = new NodeBuilder(node)
                .setTerminationPoint(terminationPointList)
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder(nodeAttr)
                                .setSystem(null)
                                .setDcn(null)
                                .setInternalLinks(new ArrayList<>())
                                .setCrossConnections(new ArrayList<>())
                                .setEquipments(new ArrayList<>())
                                .setOCMGripGroups(new ArrayList<>())
                                .setProperties(null)
                                .build())
                        .build())
                .build();

        try {
            StepResult result = write2Ne(changedNode);
            if (actionType.equals(ImplActionType.Deimplement)) {
                updateAfterTp(result, changedNode, ImplementState.Allocate);
            } else {
                updateAfterTp(result, changedNode, ImplementState.Implement);
            }

        } catch (Exception e) {
//            updatePropertyAfterSequence(getNextSequence(STEP_TP), STATUS_FAILURE);  //keep pending is correct
            throw e;
        }
    }

    //现在adminUp 的情况下把TP属性清理
    private List<TerminationPoint> tpDownRequiredForTD() {
        List<TerminationPoint> terminationPointList = node.getTerminationPoint().stream()
                .map(tp -> {
                    if (tp.getAugmentation(TerminationPoint1.class).getPhysical().getPortType().equals(PortType.OTULine)) {
                        TerminationPoint newTp = new TerminationPointBuilder(tp)
                                .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                                        .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder()
                                                .setFriendlyName(tp.getAugmentation(TerminationPoint1.class).getPhysical().getFriendlyName())
                                                .setAdminState(AdminStatus.Down)
                                                .setImplementState(ImplementState.Allocate)
                                                .build()
                                        ).build()
                                ).build();
                        return newTp;
                    } else if (tp.getAugmentation(TerminationPoint1.class).getPhysical().getPortType().equals(PortType.OTUClient)) {
                        TerminationPoint newTp = new TerminationPointBuilder(tp)
                                .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                                        .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder()
                                                .setFriendlyName(tp.getAugmentation(TerminationPoint1.class).getPhysical().getFriendlyName())
                                                .setAdminState(AdminStatus.Down)
                                                .setImplementState(ImplementState.Allocate)
                                                .build()
                                        ).build()
                                ).build();
                        return newTp;
                    }

                    return tp;
                }).filter(Objects::nonNull).collect(Collectors.toList());

        return terminationPointList;
    }

    private List<TerminationPoint> tpUpRequiredForTD() {
        List<TerminationPoint> terminationPointList = node.getTerminationPoint().stream()
                .map(tp -> {
                    if (tp.getAugmentation(TerminationPoint1.class).getPhysical().getPortType().equals(PortType.OTULine)) {
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical tpAttr = tp.getAugmentation(TerminationPoint1.class).getPhysical();
                        OtuLine otuLine = tpAttr.getOtuLine();
                        log.debug("otuLine info: {}", otuLine);

                        TerminationPoint newTp = new TerminationPointBuilder(tp)
                                .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                                        .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder()
                                                .setAdminState(tpAttr.getAdminState())
                                                .setImplementState(tpAttr.getImplementState())
                                                .setFriendlyName(tpAttr.getFriendlyName())
                                                .setOtuLine(otuLine == null ? null : new OtuLineBuilder()
                                                        .setModelSpec(new ModelSpecBuilder(otuLine.getModelSpec()).build())
                                                        .setCentralFrequency(otuLine.getCentralFrequency())
                                                        .build())
                                                .build()
                                        ).build()
                                ).build();
                        return newTp;
                    } else if (tp.getAugmentation(TerminationPoint1.class).getPhysical().getPortType().equals(PortType.OTUClient)) {
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical tpAttr = tp.getAugmentation(TerminationPoint1.class).getPhysical();

                        TerminationPoint newTp = new TerminationPointBuilder(tp)
                                .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                                        .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder()
                                                .setAdminState(tpAttr.getAdminState())
                                                .setImplementState(tpAttr.getImplementState())
                                                .setFriendlyName(tpAttr.getFriendlyName())
                                                .setOtuClient(tpAttr.getOtuClient())
                                                .build()
                                        ).build()
                                ).build();
                        return newTp;
                    }

                    return tp;
                }).filter(Objects::nonNull).collect(Collectors.toList());

        return terminationPointList;
    }

    private void setTpAdditional() {
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        log.debug("make spec TP attributes {}, {} ",
                updateNodeAttr.getIp() == null ? "" : updateNodeAttr.getIp(),
                node.getTerminationPoint().stream()
                        .map(tp -> tp.getAugmentation(TerminationPoint1.class).getPhysical()
                                .getFriendlyName())
                        .collect(Collectors.toList()));


        List<TerminationPoint> terminationPointList = node.getTerminationPoint().stream()
                .map(tp -> {
                    if (tp.getAugmentation(TerminationPoint1.class).getPhysical().getPortType().equals(PortType.OTULine)) {
                        OtuLine outLine = tp.getAugmentation(TerminationPoint1.class).getPhysical().getOtuLine();
                        TerminationPoint newTp = new TerminationPointBuilder(tp)
                                .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                                        .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(tp.getAugmentation(TerminationPoint1.class).getPhysical())
                                                .setOtuLine(outLine == null ? null : new OtuLineBuilder(outLine)
                                                        .setModelSpec(null)
                                                        .build())
                                                .build()
                                        ).build()
                                ).build();
                        return newTp;
                    } else if (tp.getAugmentation(TerminationPoint1.class).getPhysical().getPortType().equals(PortType.OTUClient)) {
                        TerminationPoint newTp = new TerminationPointBuilder(tp)
                                .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                                        .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(tp.getAugmentation(TerminationPoint1.class).getPhysical())
                                                .setOtuClient(null)
                                                .setOtuLine(null)
                                                .build()
                                        ).build()
                                ).build();
                        return newTp;
                    } else {
                        return tp;
                    }
                }).collect(Collectors.toList());

        Node changedNode = new NodeBuilder(node)
                .setTerminationPoint(terminationPointList)
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder(nodeAttr)
                                .setSystem(null)
                                .setDcn(null)
                                .setInternalLinks(new ArrayList<>())
                                .setCrossConnections(new ArrayList<>())
                                .setEquipments(new ArrayList<>())
                                .setOCMGripGroups(new ArrayList<>())
                                .setProperties(null)
                                .build())
                        .build())
                .build();

        try {
            StepResult result = write2Ne(changedNode);
            if (actionType.equals(ImplActionType.Deimplement)) {
                updateAfterTp(result, changedNode, ImplementState.Allocate);
            } else {
                updateAfterTp(result, changedNode, ImplementState.Implement);
            }

        } catch (Exception e) {
//            updatePropertyAfterSequence(getNextSequence(STEP_TP), STATUS_FAILURE);  //keep pending is correct
            throw e;
        }
    }

    private void updateAfterTp(StepResult result, Node node, ImplementState implementState) {
        Node dbCfgNode = changedObject.getChangedPhyNode(node.getNodeId().getValue());

        boolean errFound = false;
        Set<String> actionObjIds = node.getTerminationPoint().stream()
                .map(tp -> tp.getTpId().getValue())
                .collect(Collectors.toSet());
        boolean hasNodeLevelError = hasNodeLevelError(result, actionObjIds);
        if (hasNodeLevelError) {
            //has some error hasn't find related action obj, put on NE
            TerminationPoint firstOne = node.getTerminationPoint().get(0);
            String name = String.format(FORMAT_TP, firstOne.getTpId().getValue(),
                    firstOne.getAugmentation(TerminationPoint1.class).getPhysical()
                            .getFriendlyName());
            stepRecord.updatePropertyWithError(name, STATUS_FAILURE,
                    result.getError().get(0).getException().getMessage());
            if (implConfig.isTunnelForceDeimplement()) {
                log.error(gson.toJson(stepRecord));
                return; //return as success;
            }
            throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                    result.getError().get(0).getException().getMessage());
        }

        for (TerminationPoint tp : node.getTerminationPoint()) {
            String name = String.format(FORMAT_TP, tp.getTpId().getValue(),
                    tp.getAugmentation(TerminationPoint1.class).getPhysical().getFriendlyName());
            Iterator<StepResult.ErrorInfo> iter = result.getError().iterator();

            boolean found = false;
            while (iter.hasNext()) {
                StepResult.ErrorInfo error = iter.next();
                String errorTpId = error.getObjId();
                if (errorTpId.equals(tp.getTpId().getValue())) {
                    stepRecord.updatePropertyWithError(name, STATUS_FAILURE,
                            error.getException().getMessage());
//                    iter.remove();
                    found = true;
                    errFound = true;
                    break;
                }
            }
            if (!found) {//成功后修改config树的TP的implState
                dbCfgNode = physicalNode.updateTpImplState(dbCfgNode, tp.getTpId().getValue(),
                        tp.getAugmentation(TerminationPoint1.class).getPhysical().getAdminState(),
                        tp.getAugmentation(TerminationPoint1.class).getPhysical().getImplementState());

                stepRecord.updateProperty(name, STATUS_SUCCESS);
            }
        }
        if (saveAfterACtion) {
            synchronized (changedObject) {
                changedObject.addChangedPhyNode(dbCfgNode);
            }
        }

        if (errFound) {
            if (implConfig.isTunnelForceDeimplement()) {
                log.error(gson.toJson(stepRecord));
                return; //return as success;
            }
            throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                    result.getError().get(0).getException().getMessage());
        }
    }

    private void makeFixXcUp() {
        log.debug("makeFixXcUp {}",
                updateNodeAttr.getIp() == null ? "" : updateNodeAttr.getIp());
        List<CrossConnections> fixXcList = getFixXC(node);
        updateXC2Ne(fixXcList, true);
        lifeService.logStatusChanged(stepRecord);
    }

    private void makeFixXcDown() {
        log.debug("makeFixXcDown {}",
                updateNodeAttr.getIp() == null ? "" : updateNodeAttr.getIp());
        List<CrossConnections> fixXcList = getFixXC(node);
        updateXC2Ne(fixXcList, true);
        lifeService.logStatusChanged(stepRecord);
    }

    private void updateXC2Ne(List<CrossConnections> xcList, boolean isFixed) {

        if (xcList.isEmpty()) {
            log.debug("no XC require to update");
            return;
        }
//        if (isFixed) {
//            log.debug("update fixed xc {}", xcList.stream().map(xc->xc.getDescription())
//                    .collect(Collectors.toList()));
//            StepResult result = new StepResult(node.getNodeId().getValue());
//            updateAfterXc(result, xcList, isImplementAction ? ImplementState.Implement : ImplementState.Allocate);
//            return;
//        }

        //oduXC NE cannot create again, 因为网元上的交叉没有删除干净
//        if (xcList.get(0).getSourceTp().get(0).getSlot() != null) {
//            if (xcList.get(0).getSourceTp().get(0).getSlot().contains("odu")) {
//                updateAfterXc(new StepResult(node.getNodeId().getValue()), xcList, isImplementAction ? ImplementState.Implement : ImplementState.Allocate);
//                return;
//            }
//        }

        log.debug("make spec xc on {}, {}",
                updateNodeAttr.getIp() == null ? "" : updateNodeAttr.getIp(),
                xcList.stream().map(xc -> xc.getDescription()).collect(Collectors.toList()));

        Node changedNode = new NodeBuilder(node)
                .setTerminationPoint(new ArrayList<>())
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder(updateNodeAttr)
                                .setSystem(null)
                                .setDcn(null)
                                .setEquipments(new ArrayList<>())
                                .setInternalLinks(new ArrayList<>())
                                .setOCMGripGroups(new ArrayList<>())
                                .setCrossConnections(xcList)
                                .setProperties(null)
                                .build())
                        .build())
                .build();

        try {
            StepResult result;
            if (actionType.equals(ImplActionType.Deimplement)) {
                result = remove2Ne(changedNode);
                updateAfterXc(result, xcList, ImplementState.Allocate);
            } else {
                result = write2Ne(changedNode);
                updateAfterXc(result, xcList, ImplementState.Implement);
            }
        } catch (Exception e) {
//            updatePropertyAfterSequence(getNextSequence(STEP_FIX_XC), STATUS_FAILURE);   //keep pending is correct
            throw e;
        }
    }

    private List<CrossConnections> getFixXC(Node node) {
        List<CrossConnections> fixXcList = new ArrayList<>();

        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        for (CrossConnections xc : nodeAttr.getCrossConnections()) {
            if (xc.isFixed()) {
                if (actionType.equals(ImplActionType.Implement)) {
                    //spec check on Amplifier
                    xc = checkAmplifierForImplement(xc);
                    xc = checkAmplifierGainRange(xc);
                } else {
                    //amplifier xc should be be set to down
                    xc = checkAmplifierForAllocate(xc);
                    xc = checkAmplifierGainRange(xc);
                }
                fixXcList.add(xc);
            }
        }
        return fixXcList;
    }

    private CrossConnections checkAmplifierGainRange(CrossConnections xc) {
        // RAMAN 保留原始增益范围，但仍按原流程下发交叉。
        if (xc.getDescription() != null && xc.getDescription().contains("RAMAN")) {
            return xc;
        }
        if (xc.getAmplifier() != null) {
            log.debug("check amplifier xc {}", xc.getDescription());

            if (xc.getAmplifier().getGainRange() == null) {
                log.error("find amplifier xc error gainRange is {}}, and targetGain is {}", null, xc.getAmplifier().getTargetGain());
                if (xc.getDescription().contains("BA")) {
                    return new CrossConnectionsBuilder(xc)
                            .setAmplifier(new AmplifierBuilder(xc.getAmplifier())
                                    .setGainRange(FIXEDGAINRANGE.class)
                                    .build())
                            .build();
                } else {
                    if (xc.getAmplifier().getTargetGain().doubleValue() < 18.0) {
                        return new CrossConnectionsBuilder(xc)
                                .setAmplifier(new AmplifierBuilder(xc.getAmplifier())
                                        .setGainRange(LOWGAINRANGE.class)
                                        .build())
                                .build();
                    } else {
                        return new CrossConnectionsBuilder(xc)
                                .setAmplifier(new AmplifierBuilder(xc.getAmplifier())
                                        .setGainRange(HIGHGAINRANGE.class)
                                        .build())
                                .build();
                    }
                }
            } else {
                log.debug("check amplifier xc {} , targetGain is {} and gainRange is {}",
                        xc.getDescription(),
                        xc.getAmplifier().getTargetGain().doubleValue(),
                        xc.getAmplifier().getGainRange().getSimpleName());

                if (xc.getAmplifier().getTargetGain().doubleValue() < 18.0 && xc.getAmplifier().getGainRange().equals(HIGHGAINRANGE.class)) {
                    log.error("find amplifier xc error targetGain, and updated");
                    return new CrossConnectionsBuilder(xc)
                            .setAmplifier(new AmplifierBuilder(xc.getAmplifier())
                                    .setGainRange(LOWGAINRANGE.class)
                                    .build())
                            .build();
                }

                if (xc.getAmplifier().getTargetGain().doubleValue() >= 18.0 && xc.getAmplifier().getGainRange().equals(LOWGAINRANGE.class)) {
                    log.error("find amplifier xc error targetGain, and updated");
                    return new CrossConnectionsBuilder(xc)
                            .setAmplifier(new AmplifierBuilder(xc.getAmplifier())
                                    .setGainRange(HIGHGAINRANGE.class)
                                    .build())
                            .build();
                }
            }
        }
        return xc;
    }

    private CrossConnections checkAmplifierForImplement(CrossConnections xc) {
        if (xc.getAmplifier() != null) {
            log.debug("check amplifier xc {} ForImplement, implementState {}", xc.getDescription(), xc.getImplementState());

            if (xc.getImplementState() == null || !xc.getImplementState().equals(ImplementState.Implement)) {
                log.error("find amplifier xc error, it should be implemented, update it");

                return new CrossConnectionsBuilder(xc)
                        .setImplementState(ImplementState.Implement)
                        .setAdminState(AdminStatus.Up)
                        .build();
            }
        }
        return xc;
    }

    private CrossConnections checkAmplifierForAllocate(CrossConnections xc) {
        if (xc.getAmplifier() != null) {
            log.debug("check amplifier xc {} ForAllocate, implementState {}", xc.getDescription(), xc.getImplementState());

            if (xc.getImplementState() == null || !xc.getImplementState().equals(ImplementState.Implement)) {
                log.error("find amplifier xc error, for allocate should not change, keep implemented, update it");

                return new CrossConnectionsBuilder(xc)
                        .setImplementState(ImplementState.Implement)
                        .setAdminState(AdminStatus.Up)
                        .build();
            }
        }
        return xc;
    }

    private List<CrossConnections> getFlexXC(Node node) {
        List<CrossConnections> flexXcList = new ArrayList<>();

        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        for (CrossConnections xc : nodeAttr.getCrossConnections()) {
            if (!xc.isFixed()) {
                flexXcList.add(xc);
            }
        }
        return flexXcList;
    }

    private void createFlexXC() {
        log.debug("createFlexXC {}",
                updateNodeAttr.getIp() == null ? "" : updateNodeAttr.getIp());
        List<CrossConnections> flexXCList = getFlexXC(node);
        updateXC2Ne(flexXCList, false);
        lifeService.logStatusChanged(stepRecord);
    }

    private void removeFlexXC() {
        log.debug("removeFlexXC {}",
                updateNodeAttr.getIp() == null ? "" : updateNodeAttr.getIp());
        List<CrossConnections> flexXCList = getFlexXC(node);
        updateXC2Ne(flexXCList, false);
        lifeService.logStatusChanged(stepRecord);
    }

    private void updateAfterXc(StepResult result, List<CrossConnections> changedXcList,
                               ImplementState implementState) {
        Node dbCfgNode = changedObject.getChangedPhyNode(node.getNodeId().getValue());

        boolean errFound = false;
        Set<String> actionObjIds = changedXcList.stream()
                .map(xc -> xc.getCrossConnectionId().getValue())
                .collect(Collectors.toSet());
        boolean hasNodeLevelError = hasNodeLevelError(result, actionObjIds);
        if (hasNodeLevelError) {
            //has some error hasn't find related action obj, put on NE
            CrossConnections firstOne = changedXcList.get(0);
            String name = String.format(FORMAT_XC, firstOne.getCrossConnectionId().getValue(),
                    firstOne.getDescription());
            stepRecord.updatePropertyWithError(name, STATUS_FAILURE,
                    result.getError().get(0).getException().getMessage());
            if (implConfig.isTunnelForceDeimplement()) {
                log.error(gson.toJson(stepRecord));
                return; //return as success;
            }
            throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                    result.getError().get(0).getException().getMessage());
        }

        for (CrossConnections xc : changedXcList) {
            String name = String.format(FORMAT_XC, xc.getCrossConnectionId().getValue(),
                    xc.getDescription());
            Iterator<StepResult.ErrorInfo> iter = result.getError().iterator();

            boolean found = false;
            while (iter.hasNext()) {
                StepResult.ErrorInfo error = iter.next();
                if (error.getObjId().equals(xc.getCrossConnectionId().getValue())) {
                    stepRecord.updatePropertyWithError(name, STATUS_FAILURE,
                            error.getException().getMessage());
//                    iter.remove();
                    found = true;
                    errFound = true;
                    break;
                }
            }
            if (!found) { //成功后修改config树的XC的implState
                dbCfgNode = physicalNode.updateXcImplState(dbCfgNode, xc.getCrossConnectionId().getValue(),
                        xc.getAdminState(), xc.getImplementState());

                stepRecord.updateProperty(name, STATUS_SUCCESS);
            }
        }
        if (saveAfterACtion) {
            synchronized (changedObject) {
                changedObject.addChangedPhyNode(dbCfgNode);
            }
        }

        if (errFound) {
            if (implConfig.isTunnelForceDeimplement()) {
                log.error(gson.toJson(stepRecord));
                return; //return as success;
            }
            throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                    result.getError().get(0).getException().getMessage());
        }
    }

    //有错误，但是和下发对象ID不匹配，就返回true,代表这个是网元级别错误
    private boolean hasNodeLevelError(StepResult result, Set<String> actionObjIds) {
        if (!result.hasError()) {
            return false;
        }
        for (StepResult.ErrorInfo error : result.getError()) {
            if (actionObjIds.contains(error.getObjId())) {
                // Object-level errors are already tied to the submitted action object.
                return false;
            }
        }
        return true;
    }

    private void writeOcmInfo() {
        log.debug("writeOcmInfo {}",
                updateNodeAttr.getIp() == null ? "" : updateNodeAttr.getIp());
        updateOcm2Ne();
        lifeService.logStatusChanged(stepRecord);
    }

    private void updateOcm2Ne() {
        log.debug("make spec ocm info on {}",
                updateNodeAttr.getIp() == null ? "" : updateNodeAttr.getIp());

        try {
            //ocmGroupInfo is empty, I will call remove
            updateNodeAttr.getOCMGripGroups().forEach(group->{
                StepResult result;
                Node changedNode = getNodeOnOcm(group);
                if (group.getChannels() == null || group.getChannels().isEmpty()) {
                    result = remove2Ne(changedNode);
                    updateAfterOcm(result);
                } else {
                    result = write2Ne(changedNode);
                }
                updateAfterOcm(result);
            });
        } catch (Exception e) {
//            updatePropertyAfterSequence(getNextSequence(STEP_OCM), STATUS_FAILURE);  //keep pending is correct
            throw e;
        }
    }

    private Node getNodeOnOcm(OCMGripGroups group) {
        List<OCMGripGroups> ocmGroup = new ArrayList<>();
        ocmGroup.add(group);
        Node changedNode = new NodeBuilder(node)
                .setTerminationPoint(new ArrayList<>())
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder(updateNodeAttr)
                                .setSystem(null)
                                .setDcn(null)
                                .setInternalLinks(new ArrayList<>())
                                .setEquipments(new ArrayList<>())
                                .setOCMGripGroups(ocmGroup)
                                .setCrossConnections(new ArrayList<>())
                                .setProperties(null)
                                .build())
                        .build())
                .build();

        return changedNode;
    }

    private void updateAfterOcm(StepResult result) throws CommonException {
        String ocmName = String.format(FORMAT_OCM, node.getNodeId().getValue(),
                node.getAugmentation(Node1.class).getPhysical().getFriendlyName());
        if (result.hasError()) {
            stepRecord.updatePropertyWithError(ocmName, STATUS_FAILURE,
                    result.getError().get(0).getException().getMessage());

            if (implConfig.isTunnelForceDeimplement()) {
                log.error(gson.toJson(stepRecord));
                return; //return as success;
            }
            throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                    result.getError().get(0).getException().getMessage());
        } else {
            stepRecord.updateProperty(ocmName, STATUS_SUCCESS);
        }
    }

    private void makeEqImplState() {
        log.debug("makeEqUp {}",
                updateNodeAttr.getIp() == null ? "" : updateNodeAttr.getIp());
        //find out EQ at first. and then Transceiver
        List<Equipments> cardList = new ArrayList<>();
//        List<Equipments> transList = new ArrayList<>();

        for (Equipments eq : updateNodeAttr.getEquipments()) {
            if (eq.getEquipType().equals(EquipType.TRANSCEIVER)) {
//                transList.add(eq);
            } else {
                Equipments newEq = new EquipmentsBuilder(eq).setProperties(null).build();
                cardList.add(newEq);
            }
        }
        makeEqImplState(cardList);
//        makeEqUp(transList);
    }

    private void makeEqImplState(List<Equipments> eqList) {
        log.debug("update spec EQ state on {}, {}",
                updateNodeAttr.getIp() == null ? "" : updateNodeAttr.getIp(),
                eqList.stream().map(eq -> eq.getFriendlyName()).collect(Collectors.toList()));

        Node changedNode = new NodeBuilder(node)
                .setTerminationPoint(new ArrayList<>())
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder(updateNodeAttr)
                                .setSystem(null)
                                .setDcn(null)
                                .setInternalLinks(new ArrayList<>())
                                .setEquipments(eqList)
                                .setCrossConnections(new ArrayList<>())
                                .setOCMGripGroups(new ArrayList<>())
                                .setProperties(null)
                                .build())
                        .build())
                .build();

        try {
            StepResult result = write2Ne(changedNode);
            if (actionType.equals(ImplActionType.Deimplement)) {
                updateAfterEq(result, eqList, ImplementState.Allocate);
            } else {
                updateAfterEq(result, eqList, ImplementState.Implement);
            }
        } catch (Exception e) {
//            updatePropertyAfterSequence(getNextSequence(STEP_EQ), STATUS_FAILURE);   //keep pending is correct
            throw e;
        }
        lifeService.logStatusChanged(stepRecord);
    }

    private void setEqAttr() {
        log.debug("setEqAttr {}",
                updateNodeAttr.getIp() == null ? "" : updateNodeAttr.getIp());

        List<Equipments> cardList = new ArrayList<>();

        for (Equipments eq : updateNodeAttr.getEquipments()) {
            if (!eq.getEquipType().equals(EquipType.TRANSCEIVER)) {
                cardList.add(eq);
            }
        }
        makeEqImplState(cardList);
    }


    private void updateAfterEq(StepResult result, List<Equipments> eqList,
                               ImplementState implementState) throws CommonException {
        Node dbCfgNode = changedObject.getChangedPhyNode(node.getNodeId().getValue());

        boolean errFound = false;
        Set<String> actionObjIds = eqList.stream()
                .map(Equipments::getEquipmentId)
                .collect(Collectors.toSet());
        boolean hasNodeLevelError = hasNodeLevelError(result, actionObjIds);
        if (hasNodeLevelError) {
            //has some error hasn't find related action obj, put on NE
            Equipments firstOne = eqList.get(0);
            String name = String.format(FORMAT_EQ, firstOne.getEquipmentId(),
                    firstOne.getFriendlyName());
            stepRecord.updatePropertyWithError(name, STATUS_FAILURE,
                    result.getError().get(0).getException().getMessage());
            if (implConfig.isTunnelForceDeimplement()) {
                log.error(gson.toJson(stepRecord));
                return; //return as success;
            }
            throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                    result.getError().get(0).getException().getMessage());
        }
        for (Equipments eq : eqList) {
            String name = String.format(FORMAT_EQ, eq.getEquipmentId(), eq.getFriendlyName());
            Iterator<StepResult.ErrorInfo> iter = result.getError().iterator();

            boolean found = false;
            while (iter.hasNext()) {
                StepResult.ErrorInfo error = iter.next();
                if (error.getObjId().equals(eq.getEquipmentId())) {
                    stepRecord.updatePropertyWithError(name, STATUS_FAILURE,
                            error.getException().getMessage());
//                    iter.remove();
                    found = true;
                    errFound = true;
                    break;
                }
            }
            if (!found) {//成功后修改config树的EQ的implState
                dbCfgNode = physicalNode.updateEqImplState(dbCfgNode, eq.getEquipmentId(),
                        eq.getAdminState(), eq.getImplementState());

                stepRecord.updateProperty(name, STATUS_SUCCESS);
            }
        }

        if (saveAfterACtion) {
            synchronized (changedObject) {
                changedObject.addChangedPhyNode(dbCfgNode);
            }
        }

        if (errFound) {
            if (implConfig.isTunnelForceDeimplement()) {
                log.error(gson.toJson(stepRecord));
                return; //return as success;
            }
            throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                    result.getError().get(0).getException().getMessage());
        }
    }

    private void createInternalLink() {
        log.debug("createInternalLink {}",
                updateNodeAttr.getIp() == null ? "" : updateNodeAttr.getIp());
        updateInternalLinkOnNe();
        lifeService.logStatusChanged(stepRecord);
    }

    private void removeInternalLink() {
        log.debug("removeInternalLink {}",
                updateNodeAttr.getIp() == null ? "" : updateNodeAttr.getIp());
        updateInternalLinkOnNe();
        lifeService.logStatusChanged(stepRecord);
    }

    private void updateInternalLinkOnNe() {
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

        log.debug("make spec internal link on {}, {}",
                updateNodeAttr.getIp() == null ? "" : updateNodeAttr.getIp(),
                nodeAttr.getInternalLinks().stream().map(il -> il.getLinkRef()).collect(Collectors.toList()));

        Node changedNode = new NodeBuilder(node)
                .setTerminationPoint(new ArrayList<>())
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder(nodeAttr)
                                .setSystem(null)
                                .setDcn(null)
//                                .setInternalLinks(new ArrayList<>())
                                .setCrossConnections(new ArrayList<>())
                                .setEquipments(new ArrayList<>())
                                .setOCMGripGroups(new ArrayList<>())
                                .setProperties(null)
                                .build())
                        .build())
                .build();

        try {
            StepResult result;
            if (actionType.equals(ImplActionType.Deimplement)) {
                result = remove2Ne(changedNode);
                updateAfterInternalLink(result, changedNode, ImplementState.Allocate);
            } else {
                result = write2Ne(changedNode);
                updateAfterInternalLink(result, changedNode, ImplementState.Implement);
            }
        } catch (Exception e) {
//            updatePropertyAfterSequence(getNextSequence(STEP_IL), STATUS_FAILURE);   //keep pending is correct
            throw e;
        }
    }

    private void updateAfterInternalLink(StepResult result, Node node,
                                         ImplementState implementState) throws CommonException {
        Node dbCfgNode = changedObject.getChangedPhyNode(node.getNodeId().getValue());

        boolean errFound = false;
        Set<String> actionObjIds = updateNodeAttr.getInternalLinks().stream()
                .map(InternalLinks::getLinkName)
                .collect(Collectors.toSet());
        boolean hasNodeLevelError = hasNodeLevelError(result, actionObjIds);
        if (hasNodeLevelError) {
            //has some error hasn't find related action obj, put on NE
            InternalLinks firstOne = updateNodeAttr.getInternalLinks().get(0);
            String name = String.format(FORMAT_IL, firstOne.getLinkRef(), firstOne.getLinkName());
            stepRecord.updatePropertyWithError(name, STATUS_FAILURE,
                    result.getError().get(0).getException().getMessage());
            if (implConfig.isTunnelForceDeimplement()) {
                log.error(gson.toJson(stepRecord));
                return; //return as success;
            }
            throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                    result.getError().get(0).getException().getMessage());
        }
        for (InternalLinks il : updateNodeAttr.getInternalLinks()) {
            String name = String.format(FORMAT_IL, il.getLinkRef(), il.getLinkName());
            Iterator<StepResult.ErrorInfo> iter = result.getError().iterator();

            boolean found = false;
            while (iter.hasNext()) {
                StepResult.ErrorInfo error = iter.next();
                String errorLinkId = error.getObjId();
                if (errorLinkId.equals(il.getLinkName())) {
                    stepRecord.updatePropertyWithError(name, STATUS_FAILURE,
                            error.getException().getMessage());
//                    iter.remove();
                    found = true;
                    errFound = true;
                    break;
                }
            }
            if (!found) {//成功后修改config树的IL的implState
                dbCfgNode = physicalNode.updateLinkImplState(dbCfgNode, il.getLinkRef(),
                        il.getImplementState());

                stepRecord.updateProperty(name, STATUS_SUCCESS);
            }
        }
        if (saveAfterACtion) {
            synchronized (changedObject) {
                changedObject.addChangedPhyNode(dbCfgNode);
            }
        }

        if (errFound) {
            if (implConfig.isTunnelForceDeimplement()) {
                log.error(gson.toJson(stepRecord));
                return; //return as success;
            }

            throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                    result.getError().get(0).getException().getMessage());
        }
    }

    private void writeBasicInfo() {
        log.debug("writeBasicInfo {}",
                updateNodeAttr.getIp() == null ? "" : updateNodeAttr.getIp());
        Node changedNode = new NodeBuilder(node)
                .setTerminationPoint(new ArrayList<>())
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder(updateNodeAttr)
                                .setInternalLinks(new ArrayList<>())
                                .setCrossConnections(new ArrayList<>())
                                .setEquipments(new ArrayList<>())
                                .setOCMGripGroups(new ArrayList<>())
                                .setSystem(null)
//                                .setProperties(null)
                                .build())
                        .build())
                .build();

        try {
            StepResult result = write2Ne(changedNode);
            updateAfterBasicInfo(result, changedNode.getNodeId().getValue());

            lifeService.logStatusChanged(stepRecord);
        } catch (Exception e) {
//                updatePropertyAfterSequence(getNextSequence(STEP_BASE_INFO), STATUS_FAILURE);  //keep pending is correct
            throw e;
        }
    }

    private void updateAfterBasicInfo(StepResult result, String nodeId) throws CommonException {
        if (!result.hasError()) {
            updateNodeImplState2Impl(nodeId);
        }
        String neName = String.format(FORMAT_BASE_INFO, node.getNodeId().getValue(),
                node.getAugmentation(Node1.class).getPhysical().getFriendlyName());
        if (result.hasError()) {
            stepRecord.updatePropertyWithError(neName, STATUS_FAILURE,
                    result.getError().get(0).getException().getMessage());

            if (implConfig.isTunnelForceDeimplement()) {
                log.error(gson.toJson(stepRecord));
                return; //return as success;
            }
            throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                    result.getError().get(0).getException().getMessage());
        } else {
            stepRecord.updateProperty(neName, STATUS_SUCCESS);
        }
    }

    private void updateNodeImplState2Impl(String nodeId) {
        if (saveAfterACtion) {

            synchronized (changedObject) {
                Node node = changedObject.getChangedPhyNode(nodeId);
                Node newNode = PhysicalNode.updateImplementState(node, ImplementState.Implement);
                changedObject.addChangedPhyNode(newNode);
//            phyNodeDao.updateConfigNodeImplState(nodeId, ImplementState.Implement, AdminStatus.Up);
//            phyNodeDao.updateOpNodeImplState(nodeId, ImplementState.Implement, AdminStatus.Up);
            }
        }
    }

    private StepResult write2Ne(Node newNode) throws CommonException {
        StepResult result = new StepResult(newNode.getNodeId().getValue());
        log.debug("{} write to NE: {}", actionType.name(), newNode);

        if (isEmptyNe(newNode)) {
            return result;
        }

        Physical nodeAttr = newNode.getAugmentation(Node1.class).getPhysical();
//        if (implConfig.isOpcIntercepted() && nodeAttr.getNodeType().equals(NodeType.OPC4)) {
//            //opc网元不写入网元，跳过。直接成功
//            return result;
//        }

        ConfigNeOutput output;
        try {
            if (nodeAttr.getIp() == null && implConfig.isWriteWithoutIP()) {
                //for test, skip TPC NE, 没有IP也可以直接写成功, sleep 5s 模拟操作网元等待时间
                // No-IP nodes skip adapter writes; keep the exact payload for field WSS XC overlap checks.
                List<NoIpDeviceError> noIpDeviceErrors =
                        NoIpConfigWriteLogger.logSkippedWrite(log, "common-configNe", actionType, newNode);
                result = SimulatorNeError.check(newNode.getNodeId().getValue());
                addNoIpDeviceErrors(result, noIpDeviceErrors);
                log.info("no IP, return success directly");
                return result;
            }
            output = neMgr.configNe(newNode);
        } catch (CommonException e) {
            log.error("write ne error (config)", e);
            result.addError(newNode.getNodeId().getValue(), e);
            return result;
//            throw exception;
        }

        return extractFailObj(result, output.getFailObj());
    }

    private boolean isEmptyNe(Node newNode) {
        Physical nodeAttr = newNode.getAugmentation(Node1.class).getPhysical();
        if ((newNode.getTerminationPoint() == null || newNode.getTerminationPoint().isEmpty()) &&
                (nodeAttr.getCrossConnections() == null || nodeAttr.getCrossConnections().isEmpty())
                &&
                (nodeAttr.getInternalLinks() == null || nodeAttr.getInternalLinks().isEmpty()) &&
                (nodeAttr.getEquipments() == null || nodeAttr.getEquipments().isEmpty()) &&
                (nodeAttr.getOCMGripGroups() == null || nodeAttr.getOCMGripGroups().isEmpty()) &&
                (nodeAttr.getProperties() == null || nodeAttr.getProperties().getProperty() == null
                        || nodeAttr.getProperties().getProperty().isEmpty())) {
            log.debug("nothing write to NE, return directly");
            return true;
        }
        return false;
    }

    private StepResult remove2Ne(Node newNode) throws CommonException {

        StepResult result = new StepResult(newNode.getNodeId().getValue());
        if (isEmptyNe(newNode)) {
            return result;
        }

        log.debug("remove from NE: {}", newNode);

        Physical nodeAttr = newNode.getAugmentation(Node1.class).getPhysical();
//        if (implConfig.isOpcIntercepted() && nodeAttr.getNodeType().equals(NodeType.OPC4)) {
//            //opc网元不写入网元，跳过。直接成功
//            return result;
//        }
        if (nodeAttr.getIp() == null && implConfig.isWriteWithoutIP()) {
            //for test, skip TPC NE, 没有IP的网元直接认为设置成功
            // No-IP nodes skip adapter writes; keep the exact payload for field WSS XC overlap checks.
            addNoIpDeviceErrors(result,
                    NoIpConfigWriteLogger.logSkippedWrite(log, "common-removeResource", actionType, newNode));
            log.info("no IP, return success directly");
            return result;
        }

        RemoveResourceOutput output;
        try {
            output = neMgr.removeResource(newNode);
        } catch (CommonException e) {
            log.error("write ne error (remove)", e);
            result.addError(newNode.getNodeId().getValue(), e);
            return result;
//            throw exception;
        }

        return extractFailObj(result, output.getFailObj());
    }

    private void addNoIpDeviceErrors(StepResult result, List<NoIpDeviceError> errors) {
        for (NoIpDeviceError error : errors) {
            result.addError(error.getObjectId(),
                    new CommonException(CommonExceptionType.DEVICE_ERROR, error.getMessage()));
        }
    }

    private StepResult extractFailObj(StepResult result, FailObj failObj) throws CommonException {
        if (failObj != null && failObj.getObject() != null && failObj.getObject().size() > 0) {
            for (Object obj : failObj.getObject()) {
                if (obj.getMessageInfo() != null && !obj.getMessageInfo().isEmpty()) {
                    result.addError(obj.getObjectId(),
                            new CommonException(CommonExceptionType.DEVICE_ERROR,
                                    obj.getMessageInfo()));
                }
            }

            log.debug("configNe result has error: {} \n {}", updateNodeAttr.getIp(),
                    failObj);
            log.info("result of this action is: \n{}", result);
            //cannot throw exception, because part of object is successfully, thus need update their status
//            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "config NE error");
        } else {
            log.debug("write2Ne success {}", updateNodeAttr.getIp());
        }
        return result;
    }
}

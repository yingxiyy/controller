package net.flex.dci.otn.controller.implement.common.ase.ne;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otn.controller.implement.common.config.ImplConfig;
import net.flex.dci.otn.controller.implement.common.impl.ImplActionType;
import net.flex.dci.otn.controller.implement.common.impl.StepResult;
import net.flex.dci.otn.controller.implement.common.lifecycle.LifeCycleSevice;
import net.flex.dci.otn.controller.implement.common.recorder.StepRecord;
import net.flex.dci.otn.controller.implement.common.utils.NoIpConfigWriteLogger;
import net.flex.dci.otn.controller.implement.common.utils.NoIpConfigWriteLogger.NoIpDeviceError;
import net.flex.dci.otn.controller.implement.common.utils.SimulatorNeError;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.BatchConfigNeInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.BatchConfigNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.RemoveResourceOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.ne.result.FailObj;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.object.ids.Object;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Slf4j
public class ConfigNeSequence implements Callable<StepRecord> {

    public final static String STATUS_PENDING = "pending";
    public final static String STATUS_SUCCESS = "success";
    public final static String STATUS_FAILURE = "failure";

    private final static String FORMAT_XC = "xc@%s@%s";
    private final static String STEP_FLEX_XC = "flexXC";

    public final static String FORMAT_OCM = "ocmInfo@%s@%s";
    private final static String STEP_OCM = "ocmInfo";

    private final static String FORMAT_TP = "tp@%s@%s";
    private final static String STEP_TP = "tp";

    private Node node;
    private Physical updateNodeAttr;
    private ImplActionType actionType;


    private StepRecord stepRecord;
    private LifeCycleSevice lifeService;

    private static final NeManagerRpc neMgr = SpringBeanFinder.getBean(NeManagerRpc.class);;
    private static final ImplConfig implConfig = SpringBeanFinder.getBean(ImplConfig.class);
    private Gson gson = new Gson();

    public ConfigNeSequence(Node node, ImplActionType actionType, LifeCycleSevice lifeService) {
        this.node = node;
        this.lifeService = lifeService;
        this.actionType = actionType;

        updateNodeAttr = node.getAugmentation(Node1.class).getPhysical();

        stepRecord = new StepRecord(node.getNodeId().getValue(), updateNodeAttr.getFriendlyName(),
                updateNodeAttr.getIp() == null ? "" : updateNodeAttr.getIp());
    }

    public Callable<StepRecord> prepare(ReadyResource readyResource) {
        List<String> actionSequencies = new ArrayList<>();
        if (actionType.equals(ImplActionType.Implement)) {
            actionSequencies.add(STEP_TP);
            actionSequencies.add(STEP_FLEX_XC);
            actionSequencies.add(STEP_OCM);
        } else {
            actionSequencies.add(STEP_FLEX_XC);
            actionSequencies.add(STEP_OCM);
        }
        updatePropertyAfterSequence(actionSequencies, readyResource, STATUS_PENDING);

        lifeService.logStatusChanged(stepRecord);
        return this;
    }

    @Override
    public StepRecord call() throws Exception {
	    String dispName = updateNodeAttr.getIp() == null ? updateNodeAttr.getFriendlyName() : updateNodeAttr.getIp();
        Thread.currentThread().setName(dispName);

        log.debug("start write2NE {} ({})", node.getNodeId(), dispName);

//        log.debug("process following resource \n {} ", gson.toJson(stepRecord));
        try {
            if (actionType.equals(ImplActionType.Implement)) {
                updateTp2Ne();
                updateXC2Ne();
                updateOcm2Ne();
            } else {
                updateOcm2Ne();
                removeXC2Ne();
            }
//            updateNode(ImplementState.Implement);
        } catch (Exception e) {
            //!!重要这个地方不能抛异常，否则外层就丢失的stepRecord!!
//            updateNode(ImplementState.PartialImplement);
        }
        lifeService.logStatusChanged(stepRecord);
        return stepRecord;
    }

    private void updatePropertyAfterSequence(List<String> actionSequencies, ReadyResource readyResource, String status) {
        for (String seq : actionSequencies) {
            updateProperty(readyResource, seq, status);
        }
    }

    private void updateProperty(ReadyResource readyResource, String seq, String status) {
        switch (seq) {
            case STEP_FLEX_XC:
                for (CrossConnections xc : updateNodeAttr.getCrossConnections()) {
                    String name = String.format(FORMAT_XC, xc.getCrossConnectionId().getValue(), xc.getDescription());
                    stepRecord.updateProperty(name, status);
                }

                for (CrossConnections xc : readyResource.getXcList()) {
                    //没有在上面的循环中找到，说明这个XC已经和目标状态一致了
                    String name = String.format(FORMAT_XC, xc.getCrossConnectionId().getValue(),
                            xc.getDescription());
                    stepRecord.updateProperty(name, STATUS_SUCCESS);
                }

                break;

            case STEP_OCM:
                String ocmName = String.format(FORMAT_OCM, node.getNodeId().getValue(),
                    node.getAugmentation(Node1.class).getPhysical().getFriendlyName());
                stepRecord.updateProperty(ocmName, status);
                break;
            case STEP_TP:
                for (TerminationPoint tp : node.getTerminationPoint()) {
                    String name = String.format(FORMAT_TP, tp.getTpId().getValue(),
                        tp.getAugmentation(TerminationPoint1.class).getPhysical().getFriendlyName());
                    stepRecord.updateProperty(name, status);
                }
                break;
        }
    }

    private void updateOcm2Ne() {
        log.debug("make spec ocm info on {}, {}",
            updateNodeAttr.getIp() == null ? "" : updateNodeAttr.getIp(),
            node.getAugmentation(Node1.class).getPhysical().getOCMGripGroups());

        Node changedNode = new NodeBuilder(node)
            .setTerminationPoint(new ArrayList<>())
            .addAugmentation(Node1.class, new Node1Builder()
                .setPhysical(new PhysicalBuilder()
                        .setIp(updateNodeAttr.getIp())
                    .setOCMGripGroups(updateNodeAttr.getOCMGripGroups())
                    .build())
                .build())
            .build();

        try {
            StepResult result = write2Ne(changedNode);
            updateAfterOcm(result);
        } catch (Exception e) {
            throw e;
        }
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
                    result.getError().toString());
        } else {
            stepRecord.updateProperty(ocmName, STATUS_SUCCESS);
        }
    }

    private void updateTp2Ne() {
        List<TerminationPoint> tpList = node.getTerminationPoint();
        if (tpList == null || tpList.isEmpty()) {
            log.info("no TP need update to NE");
            return;
        }

        log.debug("update tp on {}, {}",
            updateNodeAttr.getIp() == null ? "" : updateNodeAttr.getIp(),
            tpList.stream().map(tp -> tp.getAugmentation(TerminationPoint1.class).getPhysical().getFriendlyName()));


        Node changedNode = new NodeBuilder(node)
            .setTerminationPoint(tpList)
            .addAugmentation(Node1.class, new Node1Builder()
                .setPhysical(new PhysicalBuilder()
                        .setIp(updateNodeAttr.getIp())
                    .setCrossConnections(null)
                    .build())
                .build())
            .build();
        try {
            StepResult result = write2Ne(changedNode);
            updateAfterTp(result, tpList);
        } catch (Exception e) {
            throw e;
        }
    }

    private void removeXC2Ne() {
        List<CrossConnections> xcList = updateNodeAttr.getCrossConnections();
        if (xcList == null || xcList.isEmpty()) {
            log.info("no XC need remove from NE");
            return;
        }

        log.debug("remove spec xc on {} size: {} \n {}",
            updateNodeAttr.getIp() == null ? "" : updateNodeAttr.getIp(),
            xcList.size(),
            xcList.stream().map(xc -> xc.getDescription()).collect(Collectors.toList()));

        Node changedNode = new NodeBuilder(node)
            .setTerminationPoint(new ArrayList<>())
            .addAugmentation(Node1.class, new Node1Builder()
                .setPhysical(new PhysicalBuilder()
                        .setIp(updateNodeAttr.getIp())
                    .setCrossConnections(xcList)
                    .build())
                .build())
            .build();
        try {
            StepResult result = remove2Ne(changedNode);
            updateAfterXc(result, xcList);
        } catch (Exception e) {
            throw e;
        }
    }

    private void updateXC2Ne() {
        List<CrossConnections> xcList = updateNodeAttr.getCrossConnections();
        if (xcList == null || xcList.isEmpty()) {
            log.info("no XC need update to NE");
            return;
        }

        log.debug("update spec xc on {}, size: {} \n {}",
                updateNodeAttr.getIp() == null ? "" : updateNodeAttr.getIp(),
                xcList.size(),
                xcList.stream().map(xc -> xc.getDescription()).collect(Collectors.toList()));

        Node changedNode = new NodeBuilder(node)
            .setTerminationPoint(new ArrayList<>())
            .addAugmentation(Node1.class, new Node1Builder()
                .setPhysical(new PhysicalBuilder()
                        .setIp(updateNodeAttr.getIp())
                    .setCrossConnections(xcList)
                    .build())
                .build())
            .build();
        try {
            StepResult result = batchWrite2Ne(changedNode);
            updateAfterXc(result, xcList);
        } catch (Exception e) {
            throw e;
        }
    }

    private void updateAfterTp(StepResult result, List<TerminationPoint> tpList) {
        boolean errFound = false;
        Set<String> actionObjIds = tpList.stream()
                .map(tp -> tp.getTpId().getValue())
                .collect(Collectors.toSet());

        boolean hasNodeLevelError = hasNodeLevelError(result, actionObjIds);
        if (hasNodeLevelError) {
            TerminationPoint firstOne = tpList.get(0);
            String name = String.format(FORMAT_TP, firstOne.getTpId().getValue(),
                firstOne.getAugmentation(TerminationPoint1.class).getPhysical().getFriendlyName());
            stepRecord.updatePropertyWithError(name, STATUS_FAILURE,
                result.getError().get(0).getException().getMessage());
            if (implConfig.isTunnelForceDeimplement()) {
                log.error(gson.toJson(stepRecord));
                return; //return as success;
            }
            throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                    result.getError().toString());
        }

        for (TerminationPoint tp : tpList) {
            String name = String.format(FORMAT_TP, tp.getTpId().getValue(),
                tp.getAugmentation(TerminationPoint1.class).getPhysical().getFriendlyName());
            Iterator<StepResult.ErrorInfo> iter = result.getError().iterator();

            boolean found = false;
            while (iter.hasNext()) {
                StepResult.ErrorInfo error = iter.next();
                if (error.getObjId().equals(tp.getTpId().getValue())) {
                    stepRecord.updatePropertyWithError(name, STATUS_FAILURE,
                        error.getException().getMessage());
                    found = true;
                    errFound = true;
                    break;
                }
            }
            if (!found) {
                stepRecord.updateProperty(name, STATUS_SUCCESS);
            }
        }

        if (errFound) {
            if (implConfig.isTunnelForceDeimplement()) {
                log.error(gson.toJson(stepRecord));
                return; //return as success;
            }
            throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                    result.getError().toString());
        }
    }

    private void updateAfterXc(StepResult result, List<CrossConnections> changedXcList) {

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
                    result.getError().toString());
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
                    found = true;
                    errFound = true;
                }
            }
            if (!found) {
                stepRecord.updateProperty(name, STATUS_SUCCESS);
            }
        }

        if (result.hasError() && !errFound) {
            //has some error hasn't find related action obj, put on NE
            CrossConnections firstOne = changedXcList.get(0);
            String name = String.format(FORMAT_XC, firstOne.getCrossConnectionId().getValue(),
                    firstOne.getDescription());
            stepRecord.updatePropertyWithError(name, STATUS_FAILURE,
                    result.getError().get(0).getException().getMessage());

            errFound = true;
        }

        if (errFound) {
            if (implConfig.isTunnelForceDeimplement()) {
                log.error(gson.toJson(stepRecord));
                return; //return as success;
            }
            throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                    result.getError().toString());
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

    private StepResult remove2Ne(Node newNode) throws CommonException {
        StepResult result = new StepResult(newNode.getNodeId().getValue());
        if (isEmptyNe(newNode)) {
            return result;
        }

        log.debug("remove from NE: {}", newNode);

        RemoveResourceOutput output;
        try {
            if (write2NeWithoutIp(newNode)) {
                //for test, skip TPC NE, 没有IP也可以直接写成功, sleep 5s 模拟操作网元等待时间
                result = SimulatorNeError.check(newNode.getNodeId().getValue());
                // No-IP nodes skip adapter writes; keep the exact payload for field WSS XC overlap checks.
                addNoIpDeviceErrors(result,
                        NoIpConfigWriteLogger.logSkippedWrite(log, "ase-removeResource", actionType, newNode));
                log.info("no IP, return success directly");
                return result;
            }
            output = neMgr.removeResource(newNode);
        } catch (Exception e) {
            log.error("write ne error (remove)", e);
            CommonException exception = new CommonException(
                CommonExceptionType.CANNOT_FIND_COOPERATOR,
                "internal calling error when neMgr.removeResource");
            result.addError(newNode.getNodeId().getValue(), exception);
            return result;
        }

        return extractFailObj(result, output.getFailObj());
    }

    private StepResult write2Ne(Node changedNode) throws CommonException {
        StepResult result = new StepResult(node.getNodeId().getValue());
        if (isEmptyNe(changedNode)) {
            return result;
        }

        log.debug("write to NE: {}", changedNode);
        if (write2NeWithoutIp(changedNode)) {
            result = SimulatorNeError.check(node.getNodeId().getValue());
            // No-IP nodes skip adapter writes; keep the exact payload for field WSS XC overlap checks.
            addNoIpDeviceErrors(result,
                    NoIpConfigWriteLogger.logSkippedWrite(log, "ase-configNe", actionType, changedNode));
            log.info("no IP, return success directly");
            return result;
        }

        ConfigNeOutput output;
        try {
            output = neMgr.configNe(changedNode);
        } catch (CommonException e) {
            log.error("write ne error", e);
            result.addError(node.getNodeId().getValue(), e);
            return result;
        }

        return extractFailObj(result, output.getFailObj());
    }

    private StepResult batchWrite2Ne(Node changedNode) throws CommonException {
        StepResult result = new StepResult(node.getNodeId().getValue());
        if (isEmptyNe(changedNode)) {
            return result;
        }

        //filter out only support wss XC
        Node batchModeNode = extractWssXC(changedNode);
        log.debug("batch write to NE: {}", batchModeNode);
        if (write2NeWithoutIp(changedNode)) {
            result = SimulatorNeError.check(node.getNodeId().getValue());
            // No-IP nodes skip adapter writes; keep the exact WSS payload that would be sent in batch mode.
            addNoIpDeviceErrors(result,
                    NoIpConfigWriteLogger.logSkippedWrite(log, "ase-batchConfigNe", actionType, batchModeNode));
            log.info("no IP, return success directly");
            return result;
        }

        BatchConfigNeOutput output;
        try {
            output = neMgr.batchConfigNe(new BatchConfigNeInputBuilder()
                    .setNodeId(batchModeNode.getNodeId())
                    .setPhysical(batchModeNode.getAugmentation(Node1.class).getPhysical())
                    .setTerminationPoint(null)
                    .build());
        } catch (CommonException e) {
            log.error("write ne error", e);
            result.addError(node.getNodeId().getValue(), e);
            return result;
        }

        return extractFailObj(result, output.getFailObj());
    }

    private Node extractWssXC(Node changedNode) {
        return new NodeBuilder(changedNode)
            .addAugmentation(Node1.class, new Node1Builder()
                .setPhysical(new PhysicalBuilder()
                    .setIp(changedNode.getAugmentation(Node1.class).getPhysical().getIp())
                    .setCrossConnections(changedNode.getAugmentation(Node1.class).getPhysical()
                        .getCrossConnections().stream().filter(xc -> xc.getWssChannel() != null)
                        .collect(Collectors.toList()))
                    .build())
                .build())
            .build();
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
        } else {
            log.debug("write2Ne success {}", updateNodeAttr.getIp());
        }
        return result;
    }


    private boolean isEmptyNe(Node newNode) {
        Physical nodeAttr = newNode.getAugmentation(Node1.class).getPhysical();
        if ((newNode.getTerminationPoint() == null || newNode.getTerminationPoint().isEmpty()) &&
            (nodeAttr.getCrossConnections() == null || nodeAttr.getCrossConnections().isEmpty()) &&
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

    private boolean write2NeWithoutIp(Node node) {
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        if (nodeAttr.getIp() == null && implConfig.isWriteWithoutIP()) {
            return true;
        }
        return false;
    }
}

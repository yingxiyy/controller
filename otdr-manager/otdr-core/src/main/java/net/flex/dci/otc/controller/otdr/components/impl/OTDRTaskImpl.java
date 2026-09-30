package net.flex.dci.otc.controller.otdr.components.impl;

import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.controller.otdr.components.BaseComponent;
import net.flex.dci.otc.controller.otdr.components.OTDRTask;
import net.flex.dci.otc.controller.otdr.domain.OtdrScanDetail;
import net.flex.dci.otc.controller.otdr.enums.OtdrMonitorDirection;
import net.flex.dci.otc.controller.otdr.model.OTDRScanParameters;
import net.flex.dci.otc.controller.otdr.notification.OtdrScanStateNotification;
import net.flex.dci.otc.controller.otdr.scan.OTDRScanner;
import net.flex.dci.otc.controller.otdr.utils.OTDRLogger;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otn.db.jpa.service.dao.OtdrDaoService;
import net.flex.dci.otn.db.jpa.service.dao.dto.OtdrProgressDto;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.OtdrScanResultType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrParameter.MonitorDirection;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrParameter.ScanMode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/8/30 15:36
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OTDRTaskImpl extends BaseComponent implements OTDRTask {

    private final OtdrDaoService otdrDaoService;

    private final OTDRScanner otdrScanner;

    private final PhyLinkDao phyLinkDao;

    private final OtdrScanStateNotification otdrScanStateNotification;

    @Override
    public String startOtdr(StartOtdrInput startOtdrInput, TaskInfoMessage taskInfoMessage) {

        log.info("start otdr,the input is:{}", startOtdrInput);
        String nodeId = startOtdrInput.getNodeId().getValue();
        String monitorName = startOtdrInput.getName();
        String monitorPortId = startOtdrInput.getMonitorPort();
        ScanMode scanMode = startOtdrInput.getScanMode();
        MonitorDirection direction = startOtdrInput.getMonitorDirection();
        OTDRScanParameters otdrScanParameters = OTDRScanParameters.parseOTDRScanParameterByInput(
                startOtdrInput);
        //to find scan link
        Link otsLink = getScanRefOtsLink(nodeId, monitorPortId);
        try {
            OtdrProgressDto otdrProgressDto = otdrDaoService.findInProgressDto(nodeId,
                    monitorPortId);
            String nodeName = phyNodeDao.getFriendlyName(nodeId);
            String resultId = null;
            if (otdrProgressDto != null) {
                resultId = otdrProgressDto.getResultId();
                sendOtdrAlreadyInProgress(nodeId, direction, nodeName, monitorPortId, nodeName);
            } else {
                resultId = executeStartOTDRCmd(nodeId, monitorPortId, monitorName, scanMode,
                        direction,
                        otdrScanParameters,
                        otsLink, taskInfoMessage);
            }
            OTDRLogger.logOTDRScan(taskInfoMessage, otsLink, monitorPortId, scanMode, direction,
                    resultId);
            return resultId;
        } catch (Exception ex) {
            log.error("failed to start OTDR ,reason is:{}", ex.getMessage(), ex);
            OTDRLogger.logOTDRScanFailure(taskInfoMessage, ex, otsLink, monitorPortId, direction,
                    scanMode);
            throw ex;
        }
    }

    private void sendOtdrAlreadyInProgress(String nodeId, MonitorDirection direction,
            String monitorName, String monitorPortId, String nodeName) {
        log.debug("send otdr already in progress");
        OtdrScanDetail otdrScanDetail = OtdrScanDetail.builder().nodeId(nodeId).direction(
                        OtdrMonitorDirection.getDirection(direction.getIntValue())).monitorName(monitorName)
                .monitorPortId(monitorPortId).startTime(new Date()).state(
                        OtdrScanResultType.INPROGRESS).nodeName(nodeName).build();
        otdrScanStateNotification.sendOtdrScanStateNotification(otdrScanDetail);
    }


    private String executeStartOTDRCmd(String nodeId, String monitorPortId, String monitorName,
            ScanMode scanMode, MonitorDirection direction,
            OTDRScanParameters otdrScanParameters,
            Link otsLink,
            TaskInfoMessage taskInfoMessage) {
        log.info(
                "execute the start OTDR cmd,ne:{},monitorName:{},scanMode:{},direction:{} scan link id is:{} ",
                nodeId, monitorName, scanMode, direction, otsLink.getLinkId().getValue());
        log.debug("otdr scan parameters:{}", otdrScanParameters);
        Node node = phyNodeDao.getPhyNodeById(nodeId);
//        String nodeName = node.getAugmentation(Node1.class).getPhysical().getFriendlyName();
        String friendlyName = node.getAugmentation(Node1.class).getPhysical().getFriendlyName();
        NodeType nodeType = node.getAugmentation(Node1.class).getPhysical().getNodeType();
//        if (!(nodeType.equals(NodeType.OPC4) || nodeType.equals(NodeType.OD))) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    "the operation for the ne is not supported");
//        }
        Adapter adapter = adapterDao.getAdapterByNeId(nodeId);
        if (adapter == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the ne:(%s) should be registered first", friendlyName));
        }
        //is the monitor port is on the node
//        otdrValidator.validateMonitorSituation(monitorPortId, node);
        String resultId = otdrScanner.startOTDR(nodeId, monitorName, monitorPortId, scanMode,
                direction, otdrScanParameters, adapter, otsLink, taskInfoMessage);
//        String resultId = startOtdr(adapter, nodeId, nodeName, monitorName, monitorPortId, scanMode,
//                direction);
        return resultId;
    }

    private Link getScanRefOtsLink(String nodeId, String monitorPortId) {
        log.debug("get scan ref otsLink by nodeId:{} monitorPortId:{}", nodeId, monitorPortId);
        List<Link> refPhyLinks = phyLinkDao.listAllPhyLinksUnderTp(monitorPortId);
        //assem the tp only connect one link
        return refPhyLinks.get(0);
    }

//    private String startOtdr(Adapter adapter, String nodeId, String nodeName, String monitorName,
//            String monitorPortId, ScanMode scanMode, MonitorDirection monitorDirection) {
//        log.debug("send start otdr command to the ne:{}", nodeName);
//        StartOtdrInput startOtdrInput = new StartOtdrInputBuilder()
//                .setName(monitorName)
//                .setNodeId(
//                        NodeId.getDefaultInstance(nodeId))
//                .setMonitorDirection(monitorDirection)
//                .setScanMode(scanMode)
//                .setMonitorPort(monitorPortId)
//                .build();
//        StartOtdrOutput startOtdrOutput = otdrRpc.startOtdr(adapter, startOtdrInput);
//        String resultId = startOtdrOutput.getResultId();
//        log.debug("monitor the otdr task state job start");
//        recordOtdrResultInProgress(nodeId, nodeName, resultId, monitorPortId, monitorName,
//                monitorDirection);
//        AsynchronousExecutor.execute(
//                new OtdrTaskStateMonitor(nodeId, resultId, monitorName, monitorPortId, adapter));
//        return startOtdrOutput.getResultId();
//    }
//
//    private void recordOtdrResultInProgress(String nodeId, String nodeName, String resultId,
//            String monitorPortId,
//            String monitorName,
//            MonitorDirection monitorDirection) {
//        log.debug("record the ne:{} otdr task in progress", nodeName);
//        Date current = new Date();
//        String monitorPorName = terminationPointDao.getTpPhysical(
//                PhysicalTpIdNamingRule.getNodeId(monitorPortId), monitorPortId).getFriendlyName();
//        OtdrResultRecord otdrResultRecord = new OtdrResultRecord();
//        otdrResultRecord.setNeId(nodeId);
//        otdrResultRecord.setResultId(resultId);
//        otdrResultRecord.setTpId(monitorPortId);
//        otdrResultRecord.setTpName(monitorPorName);
//        otdrResultRecord.setMonitorDirection(monitorDirection.getIntValue());
//        otdrResultRecord.setScanResult(OtdrScanResultType.INPROGRESS.getIntValue());
//        otdrResultRecord.setStartTime(current);
//        BigInteger taskId = generateTaskId(current);
//        otdrResultRecord.setTaskId(taskId);
//        List<Link> phyLinks = phyLinkDao.listAllPhyLinksUnderTp(monitorPortId);
//        //assemble there have only one phy link
//        Link link = phyLinks.get(0);
//        if (link != null) {
//            if (link.getAugmentation(Link1.class) != null
//                    && link.getAugmentation(Link1.class).getPhysical() != null) {
//                Physical phy = link.getAugmentation(Link1.class).getPhysical();
//                if (phy.getProvider() != null) {
//                    BigDecimal baseDistance = phy.getProvider().getDistance();
//                    if (baseDistance != null) {
//                        otdrResultRecord.setBaseDistance(baseDistance.doubleValue());
//                    }
//                    BigDecimal azAtt =
//                            phy.getProvider().getAttenuationAz() == null ? new BigDecimal(0)
//                                    : phy.getProvider().getAttenuationAz();
//                    BigDecimal zaAtt =
//                            phy.getProvider().getAttenuationZa() == null ? new BigDecimal(0)
//                                    : phy.getProvider().getAttenuationZa();
//
//                    boolean isSource = link.getSource().getSourceTp().getValue()
//                            .equals(monitorPortId);
//                    if (isSource) {
//                        if (monitorDirection == MonitorDirection.OUT
//                                && azAtt != null) {
//                            otdrResultRecord.setBaseLoss(azAtt.doubleValue());
//                        } else if (monitorDirection == MonitorDirection.IN
//                                && azAtt != null) {
//                            otdrResultRecord.setBaseLoss(zaAtt.doubleValue());
//                        }
//                    } else {
//                        if (monitorDirection == MonitorDirection.OUT
//                                && azAtt != null) {
//                            otdrResultRecord.setBaseLoss(zaAtt.doubleValue());
//                        } else if (monitorDirection == MonitorDirection.IN
//                                && azAtt != null) {
//                            otdrResultRecord.setBaseLoss(azAtt.doubleValue());
//                        }
//                    }
//                    String monitorDirectionStr = getMonitorDirectionStr(isSource, monitorDirection);
//                    otdrResultRecord.setMonitorDirectionStr(monitorDirectionStr);
//                }
//
//            }
//        }
//        otdrDaoService.save(otdrResultRecord);
//        AsynchronousExecutor.execute(
//                () -> sendInProgressNotification(nodeId, nodeName, monitorName, monitorPortId,
//                        monitorDirection));
//    }
//
//    private String getMonitorDirectionStr(boolean isSource, MonitorDirection monitorDirection) {
//        log.debug("to get monitor direction str");
//        int sourceFlag = isSource ? 1 : 0;
//        int monitorDirectionFlag = 0;
//        if (isSource) {
//            monitorDirectionFlag = sourceFlag & monitorDirection.getIntValue();
//        } else {
//            monitorDirectionFlag = sourceFlag ^ monitorDirection.getIntValue();
//        }
//        return Objects.requireNonNull(OtdrMonitorDirection.getDirection(
//                monitorDirectionFlag)).getDirectionStr();
//    }
//
//    private void sendInProgressNotification(String nodeId, String nodeName, String monitorName,
//            String monitorPortId, MonitorDirection monitorDirection) {
//        log.debug("send otdr scan in progress notification!");
//        OtdrScanDetail otdrScanDetail = OtdrScanDetail.builder()
//                .state(OtdrScanResultType.INPROGRESS).monitorName(monitorName)
//                .nodeName(nodeName)
//                .direction(OtdrMonitorDirection.getDirection(monitorDirection.getIntValue()))
//                .monitorPortId(monitorPortId).nodeId(nodeId).build();
//        otdrNotificationService.sendOtdrScanStateNotification(otdrScanDetail);
//    }

//    private void validateMonitorPort(String monitorPortId, Node node) {
//        log.debug("validate the monitor port");
//        Set<String> terminationPointIds = node.getTerminationPoint().stream()
//                .map(terminationPoint -> terminationPoint.getTpId().getValue()).collect(
//                        Collectors.toSet());
//        if (!terminationPointIds.contains(monitorPortId)) {
//            String friendlyName = node.getAugmentation(Node1.class).getPhysical().getFriendlyName();
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    String.format("the ne %s monitor port:%s is invalided", friendlyName,
//                            monitorPortId));
//        }
//    }
}

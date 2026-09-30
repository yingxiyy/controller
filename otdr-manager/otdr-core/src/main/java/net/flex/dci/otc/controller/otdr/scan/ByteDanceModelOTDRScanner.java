package net.flex.dci.otc.controller.otdr.scan;

import static net.flex.dci.otc.controller.otdr.utils.OtdrUtils.getNodeName;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.controller.otdr.domain.OtdrScanDetail;
import net.flex.dci.otc.controller.otdr.enums.OTDRScanWavelength;
import net.flex.dci.otc.controller.otdr.enums.OtdrMonitorDirection;
import net.flex.dci.otc.controller.otdr.model.OTDRScanParameters;
import net.flex.dci.otc.controller.otdr.monitor.OtdrTaskStateMonitor;
import net.flex.dci.otc.controller.otdr.notification.OtdrScanStateNotification;
import net.flex.dci.otc.controller.otdr.utils.AsynchronousExecutor;
import net.flex.dci.otc.controller.rpc.client.rpcs.OTDRRpc;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.TerminationPointDao;
import net.flex.dci.otn.db.jpa.service.dao.OtdrDaoService;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.OtdrScanResultType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrOutput.Status;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrParameter.MonitorDirection;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrParameter.ScanMode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 7/18/2025 11:00 AM
 */
@Component
@Slf4j
public class ByteDanceModelOTDRScanner extends AbstractOTDRScanner {


    public ByteDanceModelOTDRScanner(OTDRRpc otdrRpc, TerminationPointDao terminationPointDao,
            PhyLinkDao phyLinkDao, OtdrDaoService otdrDaoService,
            OtdrScanStateNotification otdrNotificationService) {
        super(otdrRpc, terminationPointDao, phyLinkDao, otdrDaoService, otdrNotificationService);
    }

    @Override
    public NeYangModel supportYangModel() {
        return NeYangModel.ByteDance;
    }

    @Override
    public String startOTDR(Node ne, String monitorName, String monitorPortId, ScanMode scanMode,
            MonitorDirection monitorDirection, OTDRScanParameters otdrScanParameters,
            Adapter adapter, Link otsLink,
            TaskInfoMessage taskInfoMessage) {
        log.info(
                "bytedance otdr scan job start,the current business port is :{},ref node id is:{},direction is:{},scanMode is:{} through adapter :{}",
                monitorPortId, ne.getNodeId().getValue(), monitorDirection, scanMode,
                adapter.getName());
        String nodeId = ne.getNodeId().getValue();
        String nodeName = getNodeName(ne);
        StartOtdrInputBuilder startOtdrInputBuilder = new StartOtdrInputBuilder()
                .setName(monitorName)
                .setNodeId(
                        NodeId.getDefaultInstance(nodeId))
                .setMonitorDirection(monitorDirection)
                .setScanMode(scanMode)
                .setMonitorPort(monitorPortId);
        if (scanMode == ScanMode.MANUAL) {
            startOtdrInputBuilder.setDistanceRange(otdrScanParameters.getDistanceRange())
                    .setEndOfFiberThreshold(otdrScanParameters.getEndOfFiberThreshold())
                    .setPulseWidth(otdrScanParameters.getPulseWidth())
                    .setDistanceRange(otdrScanParameters.getDistanceRange())
                    .setReflectionThreshold(otdrScanParameters.getReflectionThreshold())
                    .setScanWavelength(
                            OTDRScanWavelength.valueOf(otdrScanParameters.getScanWaveLength())
                                    .getScanWavelength())
                    .setRefractiveIndex(otdrScanParameters.getRefractiveIndex())
                    .setScanTime(otdrScanParameters.getScanTime())
                    .setSamplingResolution(otdrScanParameters.getSamplingResolution())
                    .setSpliceLossThreshold(otdrScanParameters.getSpliceLossThreshold());
        }
        StartOtdrInput startOtdrInput = startOtdrInputBuilder.build();
        StartOtdrOutput startOtdrOutput = otdrRpc.startOtdr(adapter, startOtdrInput);
        Status scanStatus = startOtdrOutput.getStatus();
        if (scanStatus.equals(Status.FAILURE)) {
            String failedMessage = startOtdrOutput.getErrorMessage();
            log.error("failed to start OTDR the reason is:{}", failedMessage);
            OtdrScanDetail otdrScanDetail = OtdrScanDetail.builder().monitorPortId(monitorPortId)
                    .nodeName(nodeName)
                    .nodeId(nodeId).state(OtdrScanResultType.FAIL).monitorName(monitorName)
                    .direction(OtdrMonitorDirection.getDirection(monitorDirection.getIntValue()))
                    .errorMessage(failedMessage).build();
//            OTDRLogger.logOTDRScanFailure(taskInfoMessage, failedMessage,);
            otdrNotificationService.sendOtdrScanStateNotification(otdrScanDetail);
            throw new CommonException(CommonExceptionType.OTDR_SCAN_FAILURE,
                    String.format("OTDR scan failure the reason is: %s", failedMessage));

        }

        String resultId = startOtdrOutput.getResultId();
        log.debug("monitor the otdr task state job start,OTDR result is:{}", resultId);
        boolean isSource = recordOtdrResultInProgress(nodeId, nodeName, resultId, monitorPortId, monitorName,
                monitorDirection, scanMode, otdrScanParameters);
        AsynchronousExecutor.execute(
                new OtdrTaskStateMonitor(nodeId, resultId, monitorName, monitorPortId,
                        monitorDirection, scanMode, isSource, adapter,
                        otsLink, taskInfoMessage));
        return startOtdrOutput.getResultId();
    }


}

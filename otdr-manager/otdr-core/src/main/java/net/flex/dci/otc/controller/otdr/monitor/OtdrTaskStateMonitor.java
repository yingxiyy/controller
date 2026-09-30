package net.flex.dci.otc.controller.otdr.monitor;

import static net.flex.dci.otc.controller.otdr.utils.OtdrConstants.NAMESPACE;
import static net.flex.dci.otc.controller.otdr.utils.OtdrConstants.operation;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.controller.otdr.domain.OtdrScanDetail;
import net.flex.dci.otc.controller.otdr.enums.OtdrMonitorDirection;
import net.flex.dci.otc.controller.otdr.notification.OtdrScanStateNotification;
import net.flex.dci.otc.controller.otdr.utils.AsynchronousExecutor;
import net.flex.dci.otc.controller.otdr.utils.OTDRLogger;
import net.flex.dci.otc.controller.otdr.utils.OtdrUtils;
import net.flex.dci.otc.controller.rpc.client.rpcs.OTDRRpc;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.serialization.JsonUtil;
import net.flex.dci.otn.db.jpa.entity.OtdrResultRecord;
import net.flex.dci.otn.db.jpa.service.dao.OtdrDaoService;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrResultInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrResultInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrResultOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.OtdrScanResultType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrParameter.MonitorDirection;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrParameter.ScanMode;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.otdr.monitor.attributes.Results;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.otdr.monitor.attributes.results.Result;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.otdr.monitors.grouping.OtdrMonitors;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.otdr.monitors.grouping.otdr.monitors.OtdrMonitor;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

/**
 * @version 1.0
 * @date 2022/8/30 14:47
 */
@Slf4j
public class OtdrTaskStateMonitor implements Callable<Integer> {


    private final String nodeId;

    private final String resultId;

    private final String monitorPort;

    private final OTDRRpc otdrRpc;

    private final Adapter adapter;

    private final OtdrDaoService otdrDaoService;

    private final String monitorName;

    private final JsonUtil jsonUtil;

    private final OtdrScanStateNotification otdrScanStateNotification;

    private final Link otsLink;

    private final String neName;

    private final TaskInfoMessage taskInfoMessage;

    private final MonitorDirection direction;

    private final ScanMode scanMode;

    private final boolean isSource;

    public OtdrTaskStateMonitor(String nodeId, String resultId, String monitorName,
            String monitorPort, MonitorDirection direction, ScanMode scanMode, boolean isSource,
            Adapter adapter,
            Link otsLink,
            TaskInfoMessage taskInfoMessage) {
        this.nodeId = nodeId;
        this.resultId = resultId;
        this.monitorPort = monitorPort;
        this.otsLink = otsLink;
        this.taskInfoMessage = taskInfoMessage;
        this.adapter = adapter;
        this.otdrDaoService = SpringBeanFinder.getBean(OtdrDaoService.class);
        this.otdrRpc = SpringBeanFinder.getBean(OTDRRpc.class);
        this.jsonUtil = SpringBeanFinder.getBean(JsonUtil.class);
        this.otdrScanStateNotification = SpringBeanFinder.getBean(OtdrScanStateNotification.class);
        PhyNodeDao phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);
        this.neName = phyNodeDao.getFriendlyName(nodeId);
        this.monitorName = monitorName;
        this.direction = direction;
        this.scanMode = scanMode;
        this.isSource = isSource;
    }

    @Override
    public Integer call() throws Exception {
        try {
            Thread.sleep(5000);
            log.debug("start to monitor the otdr job via polling");
            int pollingCount = 0;
            OtdrScanResultType otdrScanResultType = null;
            while (!Thread.currentThread().isInterrupted()) {
                pollingCount++;
                try {
                    OtdrScanResultType scanResult = getOtdrTaskStable(adapter, nodeId, neName,
                            monitorPort,
                            monitorName,
                            direction,
                            resultId);
                    if (scanResult == OtdrScanResultType.COMPLETE) {
                        log.info("OTDR job completed after {} polls", pollingCount);
                        otdrScanResultType = OtdrScanResultType.COMPLETE;
                        break;
                    } else if (scanResult == OtdrScanResultType.FAIL) {
                        log.warn("OTDR job failed after {} polls", pollingCount);
                        otdrScanResultType = OtdrScanResultType.FAIL;
                        break;
                    }

                    Thread.sleep(30000);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw exception;
                } catch (Exception ex) {
                    log.error("Error during polling attempt {},will retry after delay",
                            pollingCount,
                            ex);
                    Thread.sleep(10000);
                }
            }
            log.debug("scan OTDR job completely! scanResult:{}", otdrScanResultType);
            recordData(adapter, monitorPort, monitorName, nodeId, neName, direction, resultId,
                    otdrScanResultType,
                    otsLink,
                    taskInfoMessage);
            return 2;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("OTDR monitoring task was interrupted", ex);
            return -1;
        } catch (Exception ex) {
            log.error("Unexpected error in OTDR monitoring task", ex);
            return -1;
        }
    }

    private void recordData(Adapter adapter, String monitorPort, String monitorName,
            String nodeId,
            String neName,
            MonitorDirection direction,
            String resultId, OtdrScanResultType scanResultType,
            Link otsLink, TaskInfoMessage taskInfoMessage) {
        long startTime = System.currentTimeMillis();

        log.info("Starting OTDR data recording - startTime:{} Node: {}, ResultId: {}",
                startTime, nodeId, resultId);
        try {
            GetOtdrResultInput getOtdrResultInput = new GetOtdrResultInputBuilder()
                    .setResultId(resultId)
                    .setName(monitorName)
                    .setNodeId(NodeId.getDefaultInstance(nodeId))
                    .build();
            GetOtdrResultOutput otdrResultOutput = otdrRpc.getOtdrResult(
                    adapter, getOtdrResultInput);
            //load to db
            recordTheOtdrData(otdrResultOutput, nodeId, neName, direction, otsLink, scanResultType,
                    taskInfoMessage,
                    monitorName);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("OTDR data recording failed - Duration: {}ms, Error: {}",
                    duration, e.getMessage(), e);
            handleRecordDataException(e, taskInfoMessage, resultId, monitorPort, nodeId, neName,
                    otsLink,
                    direction,
                    monitorName);
            throw e;
        }
    }

    private void handleRecordDataException(Exception e, TaskInfoMessage taskInfoMessage,
            String resultId, String monitorPort, String nodeId, String neName, Link otsLink,
            MonitorDirection direction,
            String monitorName) {

        OTDRLogger.logOTDRScanFailure(taskInfoMessage, e, otsLink, monitorPort, direction,
                scanMode);
        OtdrResultRecord otdrResultRecord = otdrDaoService.getOtdrRecordInProgressByResultIdAndTpId(
                resultId,
                monitorPort);
        otdrResultRecord.setScanResult(OtdrScanResultType.FAIL.getIntValue());
        otdrDaoService.save(otdrResultRecord);
        OtdrScanDetail otdrScanDetail = OtdrScanDetail.builder().monitorPortId(monitorPort)
                .nodeId(nodeId).state(OtdrScanResultType.COMPLETE).monitorName(monitorName)
                .nodeName(neName)
                .direction(OtdrUtils.getMonitorDirection(isSource, direction))
                .errorMessage(e.getMessage()).build();
        otdrScanStateNotification.sendOtdrScanStateNotification(otdrScanDetail);
    }


    private void recordTheOtdrData(GetOtdrResultOutput otdrResultOutput, String nodeId,
            String neName,
            MonitorDirection monitorDirection,
            Link otsLink,
            OtdrScanResultType scanResultType, TaskInfoMessage taskInfoMessage,
            String monitorName) {
        String resultId = otdrResultOutput.getResultId();
        String monitorTpId = otdrResultOutput.getMonitorPort();
        OtdrResultRecord otdrResultRecord = otdrDaoService.getOtdrRecordInProgressByResultIdAndTpId(
                resultId,
                monitorTpId);
        int scanResult = scanResultType.getIntValue();
        if (scanResultType == OtdrScanResultType.FAIL) {
            otdrResultRecord.setScanResult(scanResult);
            otdrDaoService.save(otdrResultRecord);
        } else {
            int direction = otdrResultOutput.getMonitorDirection().getIntValue();
            String directionStr = Objects.requireNonNull(
                            OtdrMonitorDirection.getDirection(direction))
                    .getDirectionStr();
            double distance = Double.parseDouble(otdrResultOutput.getDistance());
            double loss = Double.parseDouble(otdrResultOutput.getLoss());
            String outputContent = jsonUtil.fromDataObjectToJson(NAMESPACE, operation,
                    otdrResultOutput,
                    false);

            otdrResultRecord.setScanResult(scanResult);
            otdrResultRecord.setMonitorDirection(direction);
            otdrResultRecord.setMonitorDirectionStr(directionStr);
            otdrResultRecord.setEndTime(new Date());
            otdrResultRecord.setDistance(distance);
            otdrResultRecord.setLoss(loss);
            otdrResultRecord.setContent(outputContent);
            otdrResultRecord.setNeId(nodeId);
            otdrDaoService.save(otdrResultRecord);
        }
        if (scanResultType == OtdrScanResultType.COMPLETE) {
            OTDRLogger.logOTDRFinish(taskInfoMessage, otsLink, monitorName, resultId,
                    otdrResultRecord.getId());
            log.debug("OTDR finish log recorded for successful scan");
        } else {
            OTDRLogger.logOTDRScanFailure(taskInfoMessage,
                    new RuntimeException("OTDR scan failed with result: " + otdrResultOutput),
                    otsLink, monitorTpId, direction, scanMode);
            log.debug("OTDR failure log recorded");
        }
        OtdrScanDetail otdrScanDetail = OtdrScanDetail.builder().monitorPortId(monitorPort)
                .nodeId(nodeId).nodeName(neName)
                .direction(OtdrUtils.getMonitorDirection(isSource, monitorDirection))
                .state(scanResultType).monitorName(monitorName)
                .build();
        otdrScanStateNotification.sendOtdrScanStateNotification(otdrScanDetail);
    }

    private OtdrScanResultType getOtdrTaskStable(Adapter adapter, String nodeId, String neName,
            String monitorPort,
            String monitorName,
            MonitorDirection direction,
            String resultId) {
        log.debug("get the current time state，monitorPort:{} and monitorName:{}", monitorPort,
                monitorName);
        OtdrMonitors otdrMonitors = this.otdrRpc.getOtdrMonitor(
                adapter, nodeId).getOtdrMonitors();
        log.debug("current otdr monitor status is:{}", otdrMonitors);
        List<OtdrMonitor> otdrMonitorList = otdrMonitors.getOtdrMonitor();
        HashMap<String, Results> monitorPortResultMap = otdrMonitorList.stream()
                .collect(HashMap::new,
                        (map, otdrMonitor) -> map.put(otdrMonitor.getMonitorPort(),
                                otdrMonitor.getResults()), HashMap::putAll);

        Results results = monitorPortResultMap.get(monitorPort);
        Result result = getOtdrTaskResult(resultId, results);
        OtdrScanResultType scanResult = result.getScanResult();
        AsynchronousExecutor.execute(() -> {
            sendOtdrScanStateInProgressNotification(neName, monitorName, monitorPort, direction,
                    nodeId,
                    scanResult);
        });
        return result == null ? OtdrScanResultType.FAIL : result.getScanResult();
    }

    private Result getOtdrTaskResult(String resultId, Results results) {
        log.trace("start to get otdr task result");
        Map<String, Result> resultMap = results.getResult().stream()
                .collect(HashMap::new, (map, result) -> map.put(result.getResultId(), result),
                        HashMap::putAll);
        return resultMap.getOrDefault(resultId, null);
    }


    private void sendOtdrScanStateInProgressNotification(String neName, String monitorName,
            String monitorPort,
            MonitorDirection direction,
            String nodeId,
            OtdrScanResultType scanResultType) {
        log.info("otdr scan state state:{} for monitorName:{}:{}", scanResultType, monitorName,
                monitorPort);
        if (scanResultType == null || scanResultType == OtdrScanResultType.COMPLETE) {
            log.debug("current scan work finished");
            return;
        }
        if (scanResultType == OtdrScanResultType.NOSCAN
                || scanResultType == OtdrScanResultType.INPROGRESS) {
            scanResultType = OtdrScanResultType.INPROGRESS;
        }

        OtdrScanDetail otdrScanDetail = OtdrScanDetail.builder().monitorPortId(monitorPort)
                .nodeName(neName)
                .nodeId(nodeId).state(scanResultType).monitorName(monitorName)
                .direction(OtdrUtils.getMonitorDirection(isSource, direction)).build();
        otdrScanStateNotification.sendOtdrScanStateNotification(otdrScanDetail);
    }


}

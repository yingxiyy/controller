package net.flex.dci.otc.controller.otdr.clear.impl;

import static net.flex.dci.otc.controller.otdr.utils.OtdrConstants.RESULT_FAILED;
import static net.flex.dci.otc.controller.otdr.utils.OtdrConstants.operation;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.otdr.clear.OtdrNeRecordClear;
import net.flex.dci.otc.controller.otdr.enums.OtdrMonitorDirection;
import net.flex.dci.otc.controller.otdr.utils.OtdrConstants;
import net.flex.dci.otc.controller.rpc.client.rpcs.OTDRRpc;
import net.flex.dci.otc.mongo.dao.AdapterDao;
import net.flex.dci.otc.serialization.JsonUtil;
import net.flex.dci.otn.db.jpa.entity.OtdrResultRecord;
import net.flex.dci.otn.db.jpa.service.dao.OtdrDaoService;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrMonitorOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrResultInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrResultInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrResultOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.OtdrScanResultType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.otdr.monitor.attributes.results.Result;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.otdr.monitors.grouping.otdr.monitors.OtdrMonitor;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/9/2 12:34
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OtdrNeRecordClearImpl implements OtdrNeRecordClear {

    private final OTDRRpc otdrRpc;

    private final OtdrDaoService otdrDaoService;

    private final AdapterDao adapterDao;

    private final JsonUtil jsonUtil;

    @Override
    public void clearNeRecord(String nodeId, List<OtdrResultRecord> records) {
        log.debug("clear ne :{},otdr record", nodeId);
        Adapter adapter = adapterDao.getAdapterByNeId(nodeId);
        List<OtdrMonitor> otdrMonitors = getNodeOtdrMonitor(nodeId, adapter);
        Map<String, List<Result>> otdrMonitorResultMap = otdrMonitors.stream()
                .filter(otdrMonitor -> otdrMonitor.getResults() != null).collect(HashMap::new,
                        (map, otdrMonitor) -> map.put(otdrMonitor.getMonitorPort(),
                                otdrMonitor.getResults().getResult()), HashMap::putAll);
        clearRecord(otdrMonitorResultMap, records, adapter);
    }

    private void clearRecord(Map<String, List<Result>> otdrMonitorResultMap,
            List<OtdrResultRecord> records, Adapter adapter) {
        log.debug("clear the record");
        if (otdrMonitorResultMap.isEmpty()) {
            return;
        }
        for (OtdrResultRecord record : records) {
            String monitorPort = record.getTpId();
            String resultId = record.getResultId();
            List<Result> results = otdrMonitorResultMap.getOrDefault(monitorPort,
                    new ArrayList<>());
            Optional<Result> optionalResult = results.stream()
                    .filter(result -> result.getResultId().equals(resultId)).findAny();
            if (optionalResult.isPresent()) {
                Result result = optionalResult.get();
                if (result.getScanResult().equals(OtdrScanResultType.COMPLETE)) {
                    recordTheOtdrRes(record, adapter);
                } else {
                    clearTheRecord(record);
                }
            } else {
                clearTheRecord(record);
            }
        }
    }

    private void recordTheOtdrRes(OtdrResultRecord otdrResultRecord, Adapter adapter) {
        log.debug("get otdr result from ne");
        GetOtdrResultInput input = new GetOtdrResultInputBuilder().setResultId(
                        otdrResultRecord.getResultId()).setNodeId(
                        NodeId.getDefaultInstance(otdrResultRecord.getNeId()))
                .setName(otdrResultRecord.getTpName())
                .build();
        GetOtdrResultOutput otdrResultOutput = otdrRpc.getOtdrResult(adapter, input);
        int scanResult = OtdrScanResultType.COMPLETE.getIntValue();
        log.debug("");
        String outputContent = jsonUtil.fromDataObjectToJson(OtdrConstants.NAMESPACE, operation,
                otdrResultOutput,
                false);
        log.info("otdr result output put is:{}", outputContent);
        if (otdrResultOutput.getResultId() == null || otdrResultOutput.getResultId()
                .equals(RESULT_FAILED)) {
            log.warn("current otdr record:{} result id is:{} is invalid,discard it ",
                    otdrResultRecord.getId(), otdrResultRecord.getResultId());
            otdrDaoService.delete(otdrResultRecord.getId());
        } else {
            int direction = otdrResultOutput.getMonitorDirection().getIntValue();
            String directionStr = Objects.requireNonNull(
                            OtdrMonitorDirection.getDirection(direction))
                    .getDirectionStr();
            double distance = Double.parseDouble(otdrResultOutput.getDistance());
            double loss = Double.parseDouble(otdrResultOutput.getLoss());
            otdrResultRecord.setScanResult(scanResult);
            otdrResultRecord.setMonitorDirection(direction);
            otdrResultRecord.setMonitorDirectionStr(directionStr);
            otdrResultRecord.setEndTime(new Date());
            otdrResultRecord.setDistance(distance);
            otdrResultRecord.setLoss(loss);
            otdrResultRecord.setContent(outputContent);
            otdrDaoService.save(otdrResultRecord);
        }
    }

    private void clearTheRecord(OtdrResultRecord record) {
        record.setScanResult(OtdrScanResultType.COMPLETE.getIntValue());
        record.setEndTime(new Date());
        otdrDaoService.save(record);
    }

    private List<OtdrMonitor> getNodeOtdrMonitor(String nodeId, Adapter adapter) {
        log.trace("start to get current otdr monitors");

        GetOtdrMonitorOutput monitorOutput = otdrRpc.getOtdrMonitor(adapter, nodeId);
        return monitorOutput.getOtdrMonitors().getOtdrMonitor();
    }
}

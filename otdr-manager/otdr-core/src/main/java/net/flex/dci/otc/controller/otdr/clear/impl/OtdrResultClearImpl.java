package net.flex.dci.otc.controller.otdr.clear.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.CommonUtil;
import net.flex.dci.otc.controller.otdr.clear.OtdrNeRecordClear;
import net.flex.dci.otc.controller.otdr.clear.OtdrResultClear;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otn.db.jpa.entity.OtdrResultRecord;
import net.flex.dci.otn.db.jpa.service.dao.OtdrDaoService;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.OtdrScanResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/9/1 19:44
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OtdrResultClearImpl implements OtdrResultClear {


    private final OtdrDaoService otdrDaoService;

    private final PhyNodeDao phyNodeDao;

    private final OtdrNeRecordClear otdrNeRecordClear;


    @Override
    public void clearResult() {
        log.trace("start to clear the still in progress otdr job");
        List<OtdrResultRecord> otdrResultRecords = otdrDaoService.listAllInProgressOtdrJob();
        if (!otdrResultRecords.isEmpty()) {
            Map<String, List<OtdrResultRecord>> otdrResultMap = otdrResultRecords.stream()
                    .collect(HashMap::new,
                            (map, otdrRecord) -> {
                                List<OtdrResultRecord> otdrResultRecordList = map.getOrDefault(
                                        otdrRecord.getNeId(), new ArrayList<>());
                                otdrResultRecordList.add(otdrRecord);
                                map.put(otdrRecord.getNeId(), otdrResultRecordList);
                            },
                            HashMap::putAll);
            clearTheOtdrRecords(otdrResultMap);
        }

    }

    private void clearTheOtdrRecords(Map<String, List<OtdrResultRecord>> otdrResultMap) {
        log.debug("clear the otdr records start");
        List<Node> nodes = phyNodeDao.listConfigPhyNodes();
        Set<String> refNodeIds = nodes.stream().map(node -> node.getNodeId().getValue()).collect(
                Collectors.toSet());
        Set<String> inProgressOtdrNodeIds = otdrResultMap.keySet();
        Set<String> notInTopologyNodeIds = CommonUtil.getDifferenceSetByGuava(inProgressOtdrNodeIds,
                refNodeIds);
        Set<String> inTopologyNodeIds = CommonUtil.getIntersectionSetByGuava(inProgressOtdrNodeIds,
                refNodeIds);
        Set<String> connectNodeIds = inTopologyNodeIds.stream()
                .filter(phyNodeDao::existsOpNode).collect(
                        Collectors.toSet());
        Set<String> notConnectIds = CommonUtil.getDifferenceSetByGuava(inTopologyNodeIds,
                connectNodeIds);
        Set<String> finishedNodeIds = new HashSet<>(notInTopologyNodeIds);
        finishedNodeIds.addAll(new HashSet<>(notConnectIds));
        setTheScanJob2Finish(finishedNodeIds, otdrResultMap);
        getAndFinishTheOtdrScanJob(connectNodeIds, otdrResultMap);
    }

    private void getAndFinishTheOtdrScanJob(Set<String> nodeIds,
            Map<String, List<OtdrResultRecord>> otdrResultMap) {
        log.debug("start to get the scan result and finish it");
        for (String nodeId : nodeIds) {
            otdrNeRecordClear.clearNeRecord(nodeId, otdrResultMap.get(nodeId));
        }

    }

    private void setTheScanJob2Finish(Set<String> notInTopologyNodeIds,
            Map<String, List<OtdrResultRecord>> otdrResultMap) {
        log.debug("finish the useless otdr scan job");
        List<OtdrResultRecord> records = new ArrayList<>();
        notInTopologyNodeIds.stream().forEach(id -> {
            records.addAll(otdrResultMap.get(id));
        });
        discardTheRecord(records);
    }

    private void discardTheRecord(List<OtdrResultRecord> records) {
        log.debug("discard the none sense otdr record");
        records.forEach(record -> {
            record.setEndTime(new Date());
            record.setScanResult(OtdrScanResultType.COMPLETE.getIntValue());
            otdrDaoService.save(record);
        });
    }
}

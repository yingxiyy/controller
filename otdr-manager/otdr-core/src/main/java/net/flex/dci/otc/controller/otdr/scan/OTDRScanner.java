package net.flex.dci.otc.controller.otdr.scan;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.controller.otdr.model.OTDRScanParameters;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrParameter.MonitorDirection;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrParameter.ScanMode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 12/1/2023 3:57 PM
 */
@Component
@Slf4j
public class OTDRScanner {

    private final Map<NeYangModel, IOTDRScanner> otdrScannerMap;

    private final PhyNodeDao phyNodeDao;


    public OTDRScanner(List<IOTDRScanner> otdrScannerList, PhyNodeDao phyNodeDao) {
        this.otdrScannerMap = otdrScannerList.stream().collect(HashMap::new,
                (map, scanner) -> map.put(scanner.supportYangModel(), scanner), HashMap::putAll);
        this.phyNodeDao = phyNodeDao;
    }

    /**
     * String nodeId = startOtdrInput.getNodeId().getValue(); String monitorName =
     * startOtdrInput.getName(); String monitorPortId = startOtdrInput.getMonitorPort(); ScanMode
     * scanMode = startOtdrInput.getScanMode(); MonitorDirection direction =
     * startOtdrInput.getMonitorDirection();
     *
     * @return
     */
    public String startOTDR(String nodeId, String monitorName, String monitorPortId,
            ScanMode scanMode,
            MonitorDirection monitorDirection,
            OTDRScanParameters otdrScanParameters,
            Adapter adapter, Link otsLink, TaskInfoMessage taskInfoMessage) {
        log.info(
                "start otdr scan,the reference node id is:{},the monitor name is:{},the monitorPortId is:{},scanMode is:{},monitorDirection is:{} otslink id:{}",
                nodeId, monitorName, monitorPortId, scanMode, monitorDirection,
                otsLink.getLinkId().getValue());
        Node ne = phyNodeDao.getPhyNodeById(nodeId);
        NeYangModel neYangModel = NeYangModel.getModel(ne);
        String resultId = otdrScannerMap.getOrDefault(neYangModel, new DefaultOTDRScanner())
                .startOTDR(ne, monitorName, monitorPortId, scanMode, monitorDirection,
                        otdrScanParameters,
                        adapter,
                        otsLink, taskInfoMessage);
        return resultId;
    }
}

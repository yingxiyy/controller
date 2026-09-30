package net.flex.dci.otc.controller.otdr.components.impl;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.controller.otdr.components.BaseComponent;
import net.flex.dci.otc.controller.otdr.components.OTDRMonitor;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrMonitorOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.otdr.monitors.grouping.OtdrMonitors;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.otdr.monitors.grouping.OtdrMonitorsBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.otdr.monitors.grouping.otdr.monitors.OtdrMonitor;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 2022/8/30 11:43
 */
@Component
@Slf4j
public class OTDRMonitorImpl extends BaseComponent implements OTDRMonitor {


    @Override
    public OtdrMonitors getMonitors(String nodeId, String refCardId) {
        log.debug("get the node all otdr monitor status,node id:{}", nodeId);
        Node node = phyNodeDao.getConfigPhyNodeById(nodeId);
        String friendlyName = node.getAugmentation(Node1.class).getPhysical().getFriendlyName();
        NodeType nodeType = node.getAugmentation(Node1.class).getPhysical().getNodeType();
        if (!nodeType.equals(NodeType.OPC4) && !nodeType.equals(NodeType.OD)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the operation for the ne is not supported");
        }
        Adapter adapter = adapterDao.getAdapterByNeId(nodeId);
        if (adapter == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the ne:(%s) should be registered first", friendlyName));
        }
        OtdrMonitors otdrMonitors = getMonitors(nodeId, adapter);
        OtdrMonitors filterOtdrMonitors =
                refCardId == null ? otdrMonitors : filterMonitorsByCardId(otdrMonitors, refCardId);
        return filterOtdrMonitors;
    }

    private OtdrMonitors getMonitors(String nodeId, Adapter adapter) {
        log.trace("start to get node otdr monitor,the node id :{},adapter is:{}", nodeId,
                adapter.getName().getValue());
        GetOtdrMonitorOutput getOtdrMonitorOutput = otdrRpc.getOtdrMonitor(adapter, nodeId);
        OtdrMonitors otdrMonitors = getOtdrMonitorOutput.getOtdrMonitors();
        return otdrMonitors;
    }


    private OtdrMonitors filterMonitorsByCardId(OtdrMonitors otdrMonitors, String refCardId) {
        log.debug("filterMonitors by cardId:{}", refCardId);
        log.info("filter monitors otdr monitors:{}", otdrMonitors);
        OtdrMonitorsBuilder otdrMonitorsBuilder = new OtdrMonitorsBuilder();
        if (CollectionUtils.isEmpty(otdrMonitors.getOtdrMonitor())) {
            log.warn("current otdr monitor state is not found");
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "Can't get current otdr monitor state");
        }
        List<OtdrMonitor> filterOtdrMonitors = otdrMonitors.getOtdrMonitor().stream()
                .filter(otdrMonitor -> refCardId.endsWith(otdrMonitor.getLinecard()))
                .collect(
                        Collectors.toList());
        if (CollectionUtils.isEmpty(filterOtdrMonitors)) {
            log.error("No otdr monitor matched for cardId:{}", refCardId);
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "No matching OTDR monitor found for card ID: " + refCardId);
        }
        otdrMonitorsBuilder.setOtdrMonitor(filterOtdrMonitors);
        return otdrMonitorsBuilder.build();
    }


}

package net.flex.dci.otc.controller.otdr.scan;

import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.controller.otdr.model.OTDRScanParameters;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrParameter.MonitorDirection;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrParameter.ScanMode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

/**
 * @version 1.0
 * @date 12/1/2023 3:57 PM
 */
public interface IOTDRScanner {

    NeYangModel supportYangModel();

    String startOTDR(Node ne, String monitorName, String monitorPortId, ScanMode scanMode,
            MonitorDirection monitorDirection,
            OTDRScanParameters otdrScanParameters,
            Adapter adapter, Link otsLink,
            TaskInfoMessage taskInfoMessage);
}

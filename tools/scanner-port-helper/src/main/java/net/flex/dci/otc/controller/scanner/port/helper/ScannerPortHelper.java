package net.flex.dci.otc.controller.scanner.port.helper;

import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrParameter.MonitorDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;

/**
 * @version 1.0
 * @date 12/5/2023 3:12 PM
 */
public interface ScannerPortHelper {

    /**
     * @param ne target ne
     * @param targetTerminationPointId business port
     * @param monitorDirection otdr monitor direction
     * @return otdr port for the business card
     */
    TerminationPoint getRealOTDRScanPortFromBusinessCard(Node ne,
            String targetTerminationPointId, MonitorDirection monitorDirection);


    /**
     * @param ne target ne
     * @param targetTerminationPointId business port
     * @param monitorDirection otdr monitor direction
     * @return real business port
     */
    TerminationPoint getOTDRScanPortReferenceBusinessPort(Node ne,
            String targetTerminationPointId, MonitorDirection monitorDirection);
}

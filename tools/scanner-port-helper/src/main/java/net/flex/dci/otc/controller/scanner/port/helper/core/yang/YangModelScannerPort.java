package net.flex.dci.otc.controller.scanner.port.helper.core.yang;

import java.util.List;
import net.flex.dci.otc.common.util.NeYangModel;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrParameter.MonitorDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;

/**
 * @version 1.0
 * @date 12/5/2023 4:12 PM
 */
public interface YangModelScannerPort {

    NeYangModel supportYangModel();

    TerminationPoint getOtdrPortRefBusinessPort(EquipType equipType,
            TerminationPoint targetTerminationPoint,
            List<TerminationPoint> equipmentRefTerminationPoints,
            MonitorDirection monitorDirection);
}

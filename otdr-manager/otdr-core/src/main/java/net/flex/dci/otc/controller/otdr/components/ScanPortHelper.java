package net.flex.dci.otc.controller.otdr.components;

import java.util.List;
import net.flex.dci.otc.controller.otdr.domain.OtsLinkTerminationPointInfo;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.OtdrPortDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;

/**
 * @version 1.0
 * @date 12/4/2023 4:57 PM
 */
public interface ScanPortHelper {

    TerminationPoint getTargetPortRelativeOTDRPort(EquipType businessEquipType,
            List<TerminationPoint> scanPorts,
            TerminationPoint targetPort,
            OtdrPortDirection portDirection);

    OtsLinkTerminationPointInfo resolveOtsLinkEndpoint(String phyLinkId);
}

package net.flex.dci.otn.controller.nms.nms.component.terminationPoint.scan;

import java.util.List;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.OtdrPortDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.springframework.lang.Nullable;

/**
 * @version 1.0
 * @date 11/29/2023 11:07 AM
 */
public interface IScanTerminationPoint {

    List<TerminationPoint> getBusinessCardScanTerminationPoint(Equipments businessCard,
            TerminationPoint targetBusinessTp, List<TerminationPoint> refScanPorts,
            @Nullable OtdrPortDirection direction);

    PortType supportPortType();
}

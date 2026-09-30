package net.flex.dci.otn.controller.nms.nms.component.terminationPoint.impl;

import java.util.ArrayList;
import java.util.List;
import net.flex.dci.otn.controller.nms.nms.component.terminationPoint.AbstractNMSScanTerminationPoint;
import net.flex.dci.otn.controller.nms.nms.enums.RetrieveType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.OtdrPortDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;

/**
 * @version 1.0
 * @date 11/15/2023 11:14 AM
 */

public class DefaultNMSScanTerminationPoint extends AbstractNMSScanTerminationPoint {

    @Override
    public RetrieveType supportType() {
        return RetrieveType.DEFAULT;
    }

    @Override
    public List<TerminationPoint> getElementRefUnOccupiedTerminationPointsByElementId(
            String refElementId, PortType portType, OtdrPortDirection otdrPortDirection) {
        return new ArrayList<>();
    }


}

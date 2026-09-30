package net.flex.dci.otn.controller.nms.nms.component.terminationPoint.scan;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.OtdrPortDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 11/29/2023 11:24 AM
 */
@Component
@Slf4j
public class ScanTerminationPointHandler {

    private final Map<PortType, AbstractScanTerminationPoint> scanTerminationHandlerMap;

    public ScanTerminationPointHandler(List<AbstractScanTerminationPoint> scanTerminationPoints) {
        scanTerminationHandlerMap = scanTerminationPoints.stream().collect(HashMap::new,
                (map, scanTerminationPoint) -> map.put(scanTerminationPoint.supportPortType(),
                        scanTerminationPoint), HashMap::putAll);
    }

    public List<TerminationPoint> getTheTargetTpRefScanPort(Equipments refEquipment,
            List<TerminationPoint> terminationPoints,
            TerminationPoint targetTp,
            OtdrPortDirection otdrPortDirection,
            PortType portType) {
        log.debug(
                "get the target tp reference scan termination point,the target termination point is:{},portType is:{} ,relative equipment is:{} type is:{}",
                targetTp, portType, refEquipment.getFriendlyName(), refEquipment.getEquipType());
        String equipmentId = refEquipment.getEquipmentId();
        List<TerminationPoint> equipmentRefTerminationPoints = terminationPoints.stream()
                .filter(terminationPoint -> terminationPoint.getAugmentation(
                                TerminationPoint1.class).getPhysical().getEquipmentRef()
                        .equals(equipmentId)).collect(
                        Collectors.toList());
        List<TerminationPoint> refScanPorts = equipmentRefTerminationPoints.stream()
                .filter(terminationPoint -> terminationPoint.getAugmentation(
                        TerminationPoint1.class).getPhysical().getPortType().equals(portType))
                .collect(
                        Collectors.toList());
        List<TerminationPoint> filterTerminationPoint = scanTerminationHandlerMap.get(portType)
                .getBusinessCardScanTerminationPoint(refEquipment, targetTp, refScanPorts,
                        otdrPortDirection);
        return filterTerminationPoint;
    }
}

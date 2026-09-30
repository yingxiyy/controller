package net.flex.dci.otc.controller.scanner.port.helper.core;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.controller.scanner.port.helper.core.yang.DefaultYangModelScannerPort;
import net.flex.dci.otc.controller.scanner.port.helper.core.yang.YangModelScannerPort;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrParameter.MonitorDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 12/5/2023 3:44 PM
 */
@Component
@Slf4j
public class ScannerPortHelperImpl extends AbstractScannerPortHelper {


    public ScannerPortHelperImpl(
            List<YangModelScannerPort> yangModelScannerPortList) {
        super(yangModelScannerPortList);
    }

    @Override
    public TerminationPoint getRealOTDRScanPortFromBusinessCard(Node ne,
            String targetTerminationPointId, MonitorDirection monitorDirection) {
        log.debug(
                "get real OTDR scan Port from business card,the ne is:{},target monitor port is:{}",
                ne.getNodeId().getValue(), targetTerminationPointId);
        NeYangModel neYangModel = NeYangModel.getModel(ne);
        Physical neAttr = ne.getAugmentation(Node1.class).getPhysical();

        List<Equipments> equipments = neAttr.getEquipments();
        List<TerminationPoint> terminationPoints = ne.getTerminationPoint();
        TerminationPoint targetTerminationPoint = getTerminationPoint(targetTerminationPointId,
                terminationPoints);
        Equipments relativeEquipments = getRefEquipments(targetTerminationPoint, equipments);

        EquipType equipType = relativeEquipments.getEquipType();
        String equipmentId = relativeEquipments.getEquipmentId();
        List<TerminationPoint> equipmentRefTerminationPoints = terminationPoints.stream()
                .filter(terminationPoint -> terminationPoint.getAugmentation(
                                TerminationPoint1.class).getPhysical().getEquipmentRef()
                        .equals(equipmentId)).collect(
                        Collectors.toList());
        TerminationPoint terminationPoint = neYangModelYangModelScannerPortMap.getOrDefault(
                        neYangModel, new DefaultYangModelScannerPort())
                .getOtdrPortRefBusinessPort(equipType, targetTerminationPoint,
                        equipmentRefTerminationPoints, monitorDirection);
        return terminationPoint;
    }


    @Override
    public TerminationPoint getOTDRScanPortReferenceBusinessPort(Node ne,
            String targetTerminationPointId, MonitorDirection monitorDirection) {
        return null;
    }
}

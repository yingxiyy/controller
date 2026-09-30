package net.flex.dci.otn.controller.nms.nms.component.terminationPoint.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.nms.nms.component.terminationPoint.AbstractNMSScanTerminationPoint;
import net.flex.dci.otn.controller.nms.nms.component.terminationPoint.scan.ScanTerminationPointHandler;
import net.flex.dci.otn.controller.nms.nms.enums.RetrieveType;
import net.flex.dci.otn.controller.nms.properties.scan.TelecomScanPortConfiguration;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.OtdrPortDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
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
 * @date 11/15/2023 10:53 AM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EquipmentRefNMSScanTerminationPoint extends AbstractNMSScanTerminationPoint {

    private final TelecomScanPortConfiguration telecomScanPortConfiguration;

    private final ScanTerminationPointHandler scanTerminationPointHandler;

    @Override
    public RetrieveType supportType() {
        return RetrieveType.EQUIPMENT;
    }

    @Override
    public List<TerminationPoint> getElementRefUnOccupiedTerminationPointsByElementId(
            String tpId, PortType portType, OtdrPortDirection otdrPortDirection) {
        log.debug("get unOccupied termination point by tp id :{}", tpId);
        String refNodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
        Node node = netconfTopology.getConfigPhyNode(refNodeId);
        NeYangModel neYangModel = NeYangModel.getModel(node);
        if (!neYangModel.equals(NeYangModel.ChinaTelecom)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("current yang mode:%s do not support the method",
                            neYangModel.getName()));
        }
        Physical nodePhysical = node.getAugmentation(Node1.class).getPhysical();
        List<Equipments> equipments = nodePhysical.getEquipments();
        List<TerminationPoint> terminationPoints = node.getTerminationPoint();
        TerminationPoint terminationPoint = getTerminationPoint(tpId, terminationPoints);
        Equipments refEquipment = getRefEquipments(terminationPoint, equipments);
        //get under the business card otdr or ocm port
        List<TerminationPoint> relativeScanPorts = scanTerminationPointHandler.getTheTargetTpRefScanPort(
                refEquipment,
                terminationPoints,
                terminationPoint,
                otdrPortDirection,
                portType);

        return relativeScanPorts;
    }


    private TerminationPoint getTerminationPoint(String tpId,
            List<TerminationPoint> terminationPoints) {
        Optional<TerminationPoint> optionalTerminationPoint = terminationPoints.stream()
                .filter(terminationPoint -> terminationPoint.getTpId().getValue().equals(tpId))
                .findAny();
        if (!optionalTerminationPoint.isPresent()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "can not find the required TP :" + tpId);
        }
        TerminationPoint terminationPoint = optionalTerminationPoint.get();
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical tpPhysical = terminationPoint.getAugmentation(
                TerminationPoint1.class).getPhysical();
        PortType portType = tpPhysical.getPortType();
        List<PortType> supportScanPortTypes = Arrays.asList(
                telecomScanPortConfiguration.getSupportScanPortType());
        if (!supportScanPortTypes.contains(portType)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, String.format(
                    "current port type:%s can not support scan,support scan port type is:%s",
                    portType.name(), supportScanPortTypes));
        }
        return terminationPoint;
    }

    private Equipments getRefEquipments(TerminationPoint terminationPoint,
            List<Equipments> equipments) {
        log.debug("get ref equipments by tp id:{}", terminationPoint.getTpRef());
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical tpPhysical = terminationPoint.getAugmentation(
                TerminationPoint1.class).getPhysical();
        String refEquipmentId = tpPhysical.getEquipmentRef();
        Optional<Equipments> equipmentsOptional = equipments.stream()
                .filter(equip -> equip.getEquipmentId().equals(refEquipmentId)).findAny();
        if (!equipmentsOptional.isPresent()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("can not find the required tp:%s relative equipment",
                            terminationPoint.getTpRef()));
        }
        Equipments refEquipment = equipmentsOptional.get();
        List<EquipType> supportBusinessCardTypes = Arrays.asList(
                telecomScanPortConfiguration.getSupportBusinessCardType());
        if (!supportBusinessCardTypes.contains(refEquipment.getEquipType())) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format(
                            "the %s Equipment type:%s can not support scan,supported equip type is:%s",
                            refEquipment.getEquipmentId(), refEquipment.getEquipType(),
                            supportBusinessCardTypes));
        }
        return refEquipment;
    }


}

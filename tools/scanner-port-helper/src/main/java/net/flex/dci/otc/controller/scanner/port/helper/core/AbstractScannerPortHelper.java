package net.flex.dci.otc.controller.scanner.port.helper.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.controller.scanner.port.helper.ScannerPortHelper;
import net.flex.dci.otc.controller.scanner.port.helper.core.yang.YangModelScannerPort;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 12/5/2023 3:37 PM
 */
@Component
@Slf4j
public abstract class AbstractScannerPortHelper implements ScannerPortHelper {

    protected final Map<NeYangModel, YangModelScannerPort> neYangModelYangModelScannerPortMap;

    public AbstractScannerPortHelper(List<YangModelScannerPort> yangModelScannerPortList) {
        neYangModelYangModelScannerPortMap = yangModelScannerPortList.stream()
                .collect(HashMap::new,
                        (map, scannerPort) -> map.put(scannerPort.supportYangModel(), scannerPort),
                        HashMap::putAll);
    }

    protected TerminationPoint getTerminationPoint(String tpId,
            List<TerminationPoint> terminationPoints) {
        Optional<TerminationPoint> optionalTerminationPoint = terminationPoints.stream()
                .filter(terminationPoint -> terminationPoint.getTpId().getValue().equals(tpId))
                .findAny();
        if (!optionalTerminationPoint.isPresent()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "can not find the required TP :" + tpId);
        }
        TerminationPoint terminationPoint = optionalTerminationPoint.get();
        return terminationPoint;
    }


    protected Equipments getRefEquipments(TerminationPoint terminationPoint,
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
        return refEquipment;
    }

}

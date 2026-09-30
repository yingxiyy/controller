package net.flex.dci.otn.controller.nms.nms.component.terminationPoint.scan;

import static net.flex.dci.otn.controller.nms.utils.Constants.DEFAULT;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.OtdrPortDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 11/29/2023 11:08 AM
 */
@Component
@Slf4j
public class OCMScanTerminationPoint extends AbstractScanTerminationPoint {

    @Override
    public List<TerminationPoint> getBusinessCardScanTerminationPoint(Equipments businessCard,
            TerminationPoint targetBusinessTp, List<TerminationPoint> refScanPorts,
            OtdrPortDirection direction) {
        log.debug("get support ocm port by targetBusinessTp ,tp id is:{}",
                targetBusinessTp.getTpId().getValue());
        Map<String, Map<String, String>> ocmPortConfigMap = telecomScanPortConfiguration.getOcmConfig();
        EquipType equipType = businessCard.getEquipType();
        PortType targetPortType = targetBusinessTp.getAugmentation(TerminationPoint1.class)
                .getPhysical()
                .getPortType();
        String filterKey = ocmPortConfigMap.get(DEFAULT).get(DEFAULT);
        if (ocmPortConfigMap.containsKey(equipType.name())) {
            Map<String, String> ocmPortConMap = ocmPortConfigMap.get(equipType.name());
            filterKey = ocmPortConMap.get(targetPortType.name());
        }
        String finalFilterKey = filterKey;
        List<TerminationPoint> refTerminationPoints = refScanPorts.stream()
                .filter(terminationPoint -> terminationPoint.getTpId().getValue()
                        .endsWith(finalFilterKey)).collect(
                        Collectors.toList());
        return refTerminationPoints;
    }

    @Override
    public PortType supportPortType() {
        return PortType.MON;
    }
}

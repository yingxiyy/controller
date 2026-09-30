package net.flex.dci.otn.controller.nms.nms.component.terminationPoint.scan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.properties.scan.OTDRScanConfiguration;
import net.flex.dci.otn.controller.nms.properties.scan.OTDRScanProperty;
import org.apache.commons.lang3.StringUtils;
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
public class OTDRScanTerminationPoint extends AbstractScanTerminationPoint {

    @Override
    public List<TerminationPoint> getBusinessCardScanTerminationPoint(Equipments businessCard,
            TerminationPoint targetBusinessTp, List<TerminationPoint> refScanPorts,
            OtdrPortDirection direction) {
        log.debug(
                "start to get target business card:{} type is:{} target tp :{}  otdr scan termination point scan direction is:{}",
                businessCard.getEquipmentId(), businessCard.getEquipType(),
                targetBusinessTp.getTpId().getValue(), direction);
        Map<EquipType, OTDRScanConfiguration> otdrScanConfigMap = telecomScanPortConfiguration.getOtdrScanConfiguration();
        EquipType equipType = businessCard.getEquipType();
        OTDRScanConfiguration otdrScanConfiguration = otdrScanConfigMap.get(equipType);
        List<TerminationPoint> terminationPoints = filterScanPort(targetBusinessTp, refScanPorts,
                otdrScanConfiguration,
                direction);
        return terminationPoints;
    }


    @Override
    public PortType supportPortType() {
        return PortType.OTDR;
    }

    /**
     * special suffix is higher than default in configuration file
     *
     * @param targetBusinessTp
     * @param refScanPorts
     * @param direction
     * @return
     */
    private List<TerminationPoint> filterScanPort(TerminationPoint targetBusinessTp,
            List<TerminationPoint> refScanPorts, OTDRScanConfiguration otdrScanConfiguration,
            OtdrPortDirection direction) {
        log.debug("filter otdr scan port ,the scan port direction is:{}", direction);
        OTDRScanProperty otdrScanProperty =
                direction.equals(OtdrPortDirection.IN) ? otdrScanConfiguration.getIn()
                        : otdrScanConfiguration.getOut();
        PortType portType = targetBusinessTp.getAugmentation(TerminationPoint1.class).getPhysical()
                .getPortType();
        String targetTpId = targetBusinessTp.getTpId().getValue();
        String defaultSuffix = otdrScanProperty.getDefaultSuffix();
        Map<PortType, Map<String, String>> specialSuffixMap = otdrScanProperty.getSpecialSuffixRule();
        String filterKey = null;
        if (specialSuffixMap == null || specialSuffixMap.isEmpty()) {
            filterKey = defaultSuffix;
        } else {
            Map<String, String> otdrTpReferenceMap = specialSuffixMap.get(portType);
            for (Map.Entry<String, String> entry : otdrTpReferenceMap.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (targetTpId.endsWith(key)) {
                    filterKey = value;
                }
            }
        }

        if (StringUtils.isEmpty(filterKey)) {
            return new ArrayList<>();
        }
        String finalFilterKey = filterKey;
        return refScanPorts.stream().filter(tp -> tp.getTpId().getValue().endsWith(finalFilterKey))
                .collect(
                        Collectors.toList());
    }
}

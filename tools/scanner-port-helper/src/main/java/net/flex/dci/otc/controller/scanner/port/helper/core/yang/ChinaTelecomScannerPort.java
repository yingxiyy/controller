package net.flex.dci.otc.controller.scanner.port.helper.core.yang;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.controller.scanner.port.helper.model.OTDRScanConfiguration;
import net.flex.dci.otc.controller.scanner.port.helper.model.OTDRScanProperty;
import net.flex.dci.otc.controller.scanner.port.helper.model.TelecomScanPortConfiguration;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrParameter.MonitorDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @version 1.0
 * @date 12/5/2023 4:51 PM
 */
@Slf4j
@Component
public class ChinaTelecomScannerPort implements YangModelScannerPort {

    @Autowired
    private TelecomScanPortConfiguration telecomScanPortConfiguration;

    @Override
    public NeYangModel supportYangModel() {
        return NeYangModel.ChinaTelecom;
    }

    @Override
    public TerminationPoint getOtdrPortRefBusinessPort(EquipType equipType,
            TerminationPoint targetTerminationPoint,
            List<TerminationPoint> equipmentRefTerminationPoints,
            MonitorDirection monitorDirection) {
        log.debug("get otdr port ref business port current yang model is china telecom");
        PortType tartPortType = targetTerminationPoint.getAugmentation(TerminationPoint1.class)
                .getPhysical()
                .getPortType();
        validateOtdrScanBusinessEquip(equipType, tartPortType);
        TerminationPoint otdrScanPort = _getOtdrPortRefBusinessPort(equipType,
                targetTerminationPoint, tartPortType, equipmentRefTerminationPoints,
                monitorDirection);
        return otdrScanPort;
    }

    /**
     * to get monitor port
     *
     * @param equipType
     * @param targetTerminationPoint
     * @param equipmentRefTerminationPoints
     * @param monitorDirection
     * @return
     */
    private TerminationPoint _getOtdrPortRefBusinessPort(EquipType equipType,
            TerminationPoint targetTerminationPoint,
            PortType portType,
            List<TerminationPoint> equipmentRefTerminationPoints,
            MonitorDirection monitorDirection) {
        log.debug("get the reference otdr port");
        Map<EquipType, OTDRScanConfiguration> otdrScanConfig = telecomScanPortConfiguration.getOtdrScanConfiguration();
        OTDRScanConfiguration otdrScanConfiguration = otdrScanConfig.get(equipType);
        OTDRScanProperty otdrScanProperty =
                monitorDirection.equals(MonitorDirection.IN) ? otdrScanConfiguration.getIn()
                        : otdrScanConfiguration.getOut();
        String targetTerminationPointId = targetTerminationPoint.getTpId().getValue();
        String defaultSuffix = otdrScanProperty.getDefaultSuffix();
        Map<PortType, Map<String, String>> specialSuffixRule = otdrScanProperty.getSpecialSuffixRule();
        String filterKey = null;
        if (specialSuffixRule == null || specialSuffixRule.isEmpty()) {
            filterKey = defaultSuffix;
        } else {
            Map<String, String> otdrTpReferenceMap = specialSuffixRule.get(portType);
            for (Map.Entry<String, String> entry : otdrTpReferenceMap.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (targetTerminationPointId.endsWith(key)) {
                    filterKey = value;
                }
            }
        }
        if (!StringUtils.hasText(filterKey)) {
            return null;
        }
        String finalFilterKey = filterKey;
        List<TerminationPoint> refTps = equipmentRefTerminationPoints.stream().filter(tp -> {
                    assert finalFilterKey != null;
                    return tp.getTpId().getValue().endsWith(finalFilterKey);
                })
                .collect(
                        Collectors.toList());
        return refTps.get(0);
    }

    /**
     * validate RefBusiness equip
     *
     * @param equipType
     */
    private void validateOtdrScanBusinessEquip(EquipType equipType, PortType targetPortType) {
        log.debug("current business equip type is:{}", equipType);
        EquipType[] supportEquipTypes = telecomScanPortConfiguration.getSupportBusinessCardType();
        PortType[] supportPortTypes = telecomScanPortConfiguration.getSupportScanPortType();
        Optional<EquipType> equipTypeOptional = Arrays.stream(supportEquipTypes)
                .filter(eqType -> eqType.equals(equipType)).findAny();
        if (!equipTypeOptional.isPresent()) {
            throw new CommonException(CommonExceptionType.NOT_SUPPORT_ERROR,
                    String.format(
                            "current equip type:%s is not supported to scan,support equip type is:%s",
                            equipType, Arrays.toString(supportEquipTypes)));
        }
        Optional<PortType> portTypeOptional = Arrays.stream(supportPortTypes)
                .filter(portType -> portType.equals(targetPortType)).findAny();
        if (!portTypeOptional.isPresent()) {
            throw new CommonException(CommonExceptionType.NOT_SUPPORT_ERROR, String.format(
                    "current port type :%s is not supported to scan,support port type is:%s",
                    targetPortType,
                    Arrays.toString(supportPortTypes)));
        }
    }
}

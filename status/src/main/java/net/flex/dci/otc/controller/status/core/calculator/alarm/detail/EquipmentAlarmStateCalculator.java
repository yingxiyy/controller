package net.flex.dci.otc.controller.status.core.calculator.alarm.detail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.controller.status.core.calculator.alarm.IAlarmStateCalculator;
import net.flex.dci.otc.controller.status.dto.alarm.EquipmentsAlarmState;
import net.flex.dci.otc.controller.status.dto.alarm.EquipmentsAlarmState.EquipmentAlarmState;
import net.flex.dci.otc.controller.status.util.StatusUtil;
import net.flex.dci.otn.db.jpa.service.dao.AlarmDaoService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.PhyEquipAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/6 13:39
 */
@Component
@Data
@Slf4j
@RequiredArgsConstructor
public class EquipmentAlarmStateCalculator implements
        IAlarmStateCalculator<EquipmentsAlarmState, Equipments> {

    private final AlarmDaoService alarmDaoService;


    /**
     * calculate ref the phyNode ref
     *
     * @param
     * @return
     */

    @Override
    public EquipmentsAlarmState calculate(Equipments equipments) {
//        log.debug("calculate the equipments alarm state for node:{}",
//                phyNode.getNodeId().getValue());
//        List<EquipmentAlarmState> equipmentAlarmStates = calculatePhyNodeRefAlarmState(phyNode);
        return null;

    }


    @Override
    public EquipmentsAlarmState calculate(String id) {
        return null;
    }


    private List<EquipmentAlarmState> calculatePhyNodeRefAlarmState(Node phyNode) {
        List<TerminationPoint> terminationPoint = phyNode.getTerminationPoint();
        List<Equipments> equipments = phyNode.getAugmentation(Node1.class).getPhysical()
                .getEquipments();
        Map<String, List<String>> equipIdRefTerminationPointMap = getTerminationPointRefEquipIdMap(
                terminationPoint);
        Map<String, Equipments> equipmentsMap = equipments.stream().collect(
                Collectors.toMap(PhyEquipAttributes::getEquipmentId, equipment -> equipment));

        Map<String, AlarmSeverity> alarmSeverityMap = calculateSeverity(
                equipIdRefTerminationPointMap, equipmentsMap);
        return alarmSeverityMap.entrySet().stream().map(entry -> {
            return EquipmentAlarmState.builder().equipmentId(entry.getKey())
                    .alarmSeverity(entry.getValue()).build();
        }).collect(Collectors.toList());
    }

    private Map<String, AlarmSeverity> calculateSeverity(
            Map<String, List<String>> equipIdRefTerminationPointMap,
            Map<String, Equipments> equipmentsMap) {
        Map<String, AlarmSeverity> equipSeverityMap = new HashMap<>();
        equipmentsMap.keySet().forEach(equipId -> {
            List<String> nmlKeys = new ArrayList<>();
            List<String> tpIds = equipIdRefTerminationPointMap.get(equipId);
            nmlKeys.add(equipId);
            if (tpIds != null) {
                nmlKeys.addAll(tpIds);
            }
            List<AlarmSeverity> alarmSeverities = alarmDaoService.getAlarmSeverityByNmlKeys(
                    nmlKeys);
            AlarmSeverity currentAlarmSeverity = StatusUtil.calculateAlarmStateByList(
                    alarmSeverities);
            equipSeverityMap.put(equipId, currentAlarmSeverity);
        });
        return equipSeverityMap;
    }

    private Map<String, List<String>> getTerminationPointRefEquipIdMap(
            List<TerminationPoint> terminationPoints) {
        Map<String, List<String>> equipIdTerminationPoint = new HashMap<>();
        for (TerminationPoint terminationPoint : terminationPoints) {
            String tpId = terminationPoint.getTpId().getValue();
            String equipId = PhysicalTpIdNamingRule.getEquipId(tpId);
            if (equipIdTerminationPoint.containsKey(equipId)) {
                equipIdTerminationPoint.get(equipId).add(tpId);
            } else {
                List<String> terminationPointList = new ArrayList<>();
                terminationPointList.add(tpId);
                equipIdTerminationPoint.put(equipId, terminationPointList);
            }
        }
        return equipIdTerminationPoint;
    }
}

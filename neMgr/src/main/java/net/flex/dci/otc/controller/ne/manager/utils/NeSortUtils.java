package net.flex.dci.otc.controller.ne.manager.utils;

import static net.flex.dci.otc.common.constants.Constants.POUND;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.CHASSIS;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.LINECARD;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.MPO;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.MUX;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.MUXPANEL;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.PANEL;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.TRANSCEIVER;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.controller.ne.manager.dto.SlotInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;

/**
 * @version 1.0
 * @date 2022/3/29 22:21
 */
@Slf4j
public class NeSortUtils {

    private static final String GET_NUMBER_PATTERN = "-*\\d+(\\.\\d+)?";

    /**
     * sort equipments with rules chasiss slot panel mux panel helper disk
     *
     * @param equipments
     * @return
     */
    public static List<Equipments> sortEquipments(List<Equipments> equipments) {
        LinkedList<Equipments> sortedEquipments = new LinkedList<>();
        //chassis 0 slot 1~4 panel muxpanel  helper disk

        Equipments chassis = getChassis(equipments);
        //service card
        List<Equipments> serviceEquips = sortedServiceEquip(equipments);
        //panel
        List<Equipments> panelEquips = sortedPanelCardEquip(equipments);
        //helper equips
        List<Equipments> helperEquips = sortedHelperEquip(equipments);

        List<Equipments> transceiverEquips = sortedTransceiverEquips(equipments);

        sortedEquipments.addFirst(chassis);
        sortedEquipments.addAll(serviceEquips);
        sortedEquipments.addAll(panelEquips);
        sortedEquipments.addAll(helperEquips);
        sortedEquipments.addAll(transceiverEquips);
        return sortedEquipments;
    }

    private static List<Equipments> sortedTransceiverEquips(List<Equipments> equipments) {
        List<Equipments> transceiverEquip = equipments.stream()
                .filter(equipment -> equipment.getEquipmentId().contains(
                        TRANSCEIVER)).collect(Collectors.toList());
//        Map<Integer, Equipments> transceiverEquipEquipMap = transceiverEquip.stream()
//                .collect(Collectors.toMap(equip -> {
//                    String id = equip.getEquipmentId();
//                    String[] number = getNumberData(id, GET_NUMBER_PATTERN);
//                    if (id.contains("L")) {
//                        return Integer.parseInt(number[number.length - 1]) + 10;
//                    } else {
//                        return Integer.parseInt(number[number.length - 1]);
//                    }
//                }, equip -> equip));
//        List<Equipments> sortedEquipments = transceiverEquipEquipMap.keySet().stream().sorted()
//                .map(transceiverEquipEquipMap::get).collect(
//                        Collectors.toList());
        return transceiverEquip;
    }

    private static String[] getNumberData(String id, String pattern) {
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(id);
        List<String> data = new ArrayList<>();
        while (m.find()) {
            data.add(m.group());
        }
        return data.toArray(new String[data.size()]);
    }

    public static List<TerminationPoint> sortTerminationPoints(
            List<TerminationPoint> terminationPoints) {
        Map<String, List<TerminationPoint>> equipRefTpMap = new HashMap<>();
        for (TerminationPoint terminationPoint : terminationPoints) {
            String tpId = terminationPoint.getTpId().getValue();
            String equipId = PhysicalTpIdNamingRule.getEquipId(tpId);
            if (equipRefTpMap.containsKey(equipId)) {
                equipRefTpMap.get(equipId).add(terminationPoint);
            } else {
                List<TerminationPoint> tps = new ArrayList<>();
                tps.add(terminationPoint);
                equipRefTpMap.put(equipId, tps);
            }
        }

        LinkedList<TerminationPoint> sortTps = new LinkedList<>();
        for (Entry<String, List<TerminationPoint>> entry : equipRefTpMap.entrySet()) {
            String equip = entry.getKey();
            List<TerminationPoint> refTps = entry.getValue();
            if (!equip.contains(MUXPANEL)) {
                Collections.sort(refTps, (o1, o2) -> {
                    String tpSegment1 = o1.getTpId().getValue().split(POUND)[3];
                    String tpSegment2 = o2.getTpId().getValue().split(POUND)[3];
                    int tpSegment1IntValue = getIntValue(tpSegment1);
                    int tpSegment2IntValue = getIntValue(tpSegment2);
                    return tpSegment1IntValue - tpSegment2IntValue;
                });
            } else {
                sortMuxPanelTp(refTps);
            }
            sortTps.addAll(refTps);
        }

        return sortTps;
    }

    private static void sortMuxPanelTp(List<TerminationPoint> muxTps) {
        //MUX
        TreeMap<Integer, TerminationPoint> muxTpMap = new TreeMap<>();
        int muxTpCount = 0;
        for (TerminationPoint tp : muxTps) {
            String tpId = tp.getTpId().getValue();
            if (tpId.contains(MUX)) {
                muxTpMap.put(muxTpCount, tp);
                muxTpCount++;
            } else if (tpId.contains(MPO)) {

            } else {

            }
        }
        //M1D1
        //MPO
    }

    private static int getIntValue(String tpSegment) {
        byte[] tpBytes = tpSegment.getBytes(StandardCharsets.UTF_8);
        int value = 0;
        for (byte tpByte : tpBytes) {
            value += tpByte;
        }
        return value;
    }

    private static List<Equipments> sortedHelperEquip(List<Equipments> equipments) {
        List<Equipments> helperEquip = equipments.stream()
                .filter(equipment -> equipment.getEquipType().equals(
                        EquipType.Other) && !equipment.getEquipmentId().contains(PANEL)
                        && !equipment.getEquipmentId().contains(MUX)
                        && !equipment.getEquipmentId().contains(CHASSIS)
                        && !equipment.getEquipmentId().contains(LINECARD))
                .collect(Collectors.toList());
        Map<Integer, Equipments> helperEquipMap = helperEquip.stream()
                .collect(Collectors.toMap(equip -> {
                    String id = equip.getEquipmentId();
                    SlotInfo slotInfo = SlotInfoUtils.extractSlotInfoFromEqId(id);
                    return Integer.parseInt(slotInfo.getSlot());
                }, equip -> equip));
        List<Equipments> sortedEquipments = helperEquipMap.keySet().stream().sorted()
                .map(helperEquipMap::get).collect(
                        Collectors.toList());
        return sortedEquipments;
    }

    private static List<Equipments> sortedPanelCardEquip(
            List<Equipments> equipments) {
        List<Equipments> helperEquip = equipments.stream()
                .filter(equipment -> equipment.getEquipmentId().contains(
                        PANEL) || equipment.getEquipmentId().contains(
                        MUX)).collect(Collectors.toList());
        Map<Integer, Equipments> helperEquipMap = helperEquip.stream()
                .collect(Collectors.toMap(equip -> {
                    String id = equip.getEquipmentId();
                    SlotInfo slotInfo = SlotInfoUtils.extractSlotInfoFromEqId(id);
                    return Integer.parseInt(slotInfo.getSlot());
                }, equip -> equip));
        List<Equipments> sortedEquipments = helperEquipMap.keySet().stream().sorted()
                .map(helperEquipMap::get).collect(
                        Collectors.toList());
        return sortedEquipments;

    }

    private static List<Equipments> sortedServiceEquip(
            List<Equipments> equipments) {
        List<Equipments> helperEquip = equipments.stream()
                .filter(equipment -> equipment.getEquipmentId().contains(
                        LINECARD)).collect(Collectors.toList());
        Map<Integer, Equipments> helperEquipMap = helperEquip.stream()
                .collect(Collectors.toMap(equip -> {
                    String id = equip.getEquipmentId();
                    SlotInfo slotInfo = SlotInfoUtils.extractSlotInfoFromEqId(id);
                    return Integer.parseInt(slotInfo.getSlot());
                }, equip -> equip));
        List<Equipments> sortedEquipments = helperEquipMap.keySet().stream().sorted()
                .map(helperEquipMap::get).collect(
                        Collectors.toList());
        return sortedEquipments;

    }

    private static Equipments getChassis(List<Equipments> equipments) {
        Optional<Equipments> chassis = equipments.stream()
                .filter(equipment -> equipment.getEquipmentId().contains(CHASSIS)).findAny();
        return chassis.orElse(null);
    }

}

package net.flex.dci.otc.controller.ne.manager.core.filter.impl;

import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.BOARD;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.COMMA;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.NON_BUSINESS;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.TYPE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.dto.MissAlignEquipmentDto;
import net.flex.dci.otc.controller.ne.manager.dto.TelecomNonBusinessEquipmentDto;
import net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 11/21/2023 4:29 PM
 */
@Component
@Slf4j
public class TelecomModelEquipmentFilter extends
        AbstractFilter<Equipments, TelecomNonBusinessEquipmentDto> {


    public TelecomModelEquipmentFilter(Environment environment) {
        super(environment);
    }

    @Override
    public List<Equipments> filter(List<Equipments> elements) {
        return null;
    }

    @Override
    public TelecomNonBusinessEquipmentDto filterNoneBusinessEquip(List<Equipments> config,
            List<Equipments> real) {
        log.debug("get telecom model different info by equipments from real and config");
        Map<String, Equipments> configEquipMap = config.stream()
//                .filter(equipments -> nonBusinessEquipTypes.contains(equipments.getEquipType()))
                .collect(HashMap::new, (map, equip) -> map.put(equip.getEquipmentId(), equip),
                        HashMap::putAll);
        Map<String, Equipments> realEquipMap = real.stream()
//                .filter(equipments -> nonBusinessEquipTypes.contains(equipments.getEquipType()))
                .collect(HashMap::new, (map, equip) -> map.put(equip.getEquipmentId(), equip),
                        HashMap::putAll);

        Set<String> configEquipIds = configEquipMap.keySet();
        Set<String> realEquipIds = realEquipMap.keySet();
        TelecomNonBusinessEquipmentDto telecomNonBusinessEquipmentDto = extractNoneBusinessEquipmentDetail(
                configEquipIds, realEquipIds, configEquipMap, realEquipMap);
        return telecomNonBusinessEquipmentDto;
    }

    /**
     * extract detail for the business equipment detail
     *
     * @param configEquipIds
     * @param realEquipIds
     * @param configEquipMap
     * @param realEquipMap
     * @return
     */
    private TelecomNonBusinessEquipmentDto extractNoneBusinessEquipmentDetail(
            Set<String> configEquipIds, Set<String> realEquipIds,
            Map<String, Equipments> configEquipMap, Map<String, Equipments> realEquipMap) {
        Set<String> inRealEquipIds = NeManagerUtils.getDifferenceSetByGuava(realEquipIds,
                configEquipIds);
        Set<String> bothInConfigAndRealEquipIds = NeManagerUtils.getIntersectionSetByGuava(
                configEquipIds, realEquipIds);
        List<Equipments> notInConfigEquipments = filterNotInConfigNoneBusinessEQU(inRealEquipIds,
                realEquipMap);
        List<MissAlignEquipmentDto> missAlignEquipmentDtos = extractMissAlignNonBusinessEquipment(
                bothInConfigAndRealEquipIds, configEquipMap, realEquipMap);
        List<Equipments> notConfiguredEquipments = missAlignEquipmentDtos.stream()
                .filter(missAlignEquipmentDto ->
                        missAlignEquipmentDto.getDesignEquipment().getEquipType()
                                .equals(EquipType.EMPTY)
                                && !missAlignEquipmentDto.getRealEquipment().getEquipType()
                                .equals(EquipType.EMPTY))
                .map(MissAlignEquipmentDto::getRealEquipment).collect(
                        Collectors.toList());
        List<Equipments> configuredButBlankInRealEquipments = missAlignEquipmentDtos.stream()
                .filter(missAlignEquipmentDto ->
                        !missAlignEquipmentDto.getDesignEquipment().getEquipType()
                                .equals(EquipType.EMPTY)
                                && missAlignEquipmentDto.getRealEquipment().getEquipType()
                                .equals(EquipType.EMPTY))
                .map(MissAlignEquipmentDto::getRealEquipment).collect(
                        Collectors.toList());

        List<MissAlignEquipmentDto> missAlignEquips = missAlignEquipmentDtos.stream()
                .filter(missAlignEquipmentDto ->
                        !missAlignEquipmentDto.getDesignEquipment().getEquipType()
                                .equals(EquipType.EMPTY)
                                && !missAlignEquipmentDto.getRealEquipment().getEquipType()
                                .equals(EquipType.EMPTY))
                .collect(Collectors.toList());
        notInConfigEquipments.addAll(notConfiguredEquipments);

        return TelecomNonBusinessEquipmentDto.builder()
                .notInConfigEquipments(notInConfigEquipments)
                .inConfigEquipments(configuredButBlankInRealEquipments)
                .missAlignEquipmentDtos(missAlignEquips).build();
    }

    /**
     * extract miss align equipment
     *
     * @param equipIds
     * @param configEquipMap
     * @param realEquipMap
     * @return
     */
    private List<MissAlignEquipmentDto> extractMissAlignNonBusinessEquipment(
            Set<String> equipIds, Map<String, Equipments> configEquipMap,
            Map<String, Equipments> realEquipMap) {
        log.debug("get miss align equipment ");
        List<MissAlignEquipmentDto> missAlignEquipmentDtos = new ArrayList<>();
        for (String equipId : equipIds) {
            Equipments configEquip = configEquipMap.get(equipId);
            Equipments realEquip = realEquipMap.get(equipId);
            boolean nonCardIsMissAlign = isNonCardIsMissAlign(configEquip, realEquip);
            if (nonCardIsMissAlign) {
                missAlignEquipmentDtos.add(MissAlignEquipmentDto.builder().equipmentId(equipId)
                        .realEquipment(realEquip).designEquipment(configEquip).build());
            }
        }
        return missAlignEquipmentDtos;
    }

    private boolean isNonCardIsMissAlign(Equipments configEquip, Equipments realEquip) {
        if (!isNonBusinessEquipType(configEquip) && !isNonBusinessEquipType(realEquip)) {
            log.debug("neither config nor real equip is business Equipment ");
            return false;
        }
        return !configEquip.getEquipType().equals(realEquip.getEquipType());
    }

    /**
     * @param inRealEquipIds
     * @param realEquipMap
     * @return
     */
    private List<Equipments> filterNotInConfigNoneBusinessEQU(Set<String> inRealEquipIds,
            Map<String, Equipments> realEquipMap) {
        log.debug("filter the not in config none business EQU");
        List<Equipments> equipments = realEquipMap.keySet().stream()
                .filter(inRealEquipIds::contains).filter(id -> isNonBusinessEquipType(
                        realEquipMap.get(id))).map(realEquipMap::get).collect(
                        Collectors.toList());
        return equipments;
    }

    private boolean isNonBusinessEquipType(Equipments equipments) {
        log.debug("############load nonBusiness board type set##################");
        String spEquipTypes = getElementProperty(BOARD, NON_BUSINESS, TYPE);
        Set<String> specialEquipTypeSet = Arrays.stream(spEquipTypes.split(COMMA))
                .collect(Collectors.toSet());
        log.debug("special equip type set is.{}", specialEquipTypeSet);
        Set<EquipType> equipTypes = specialEquipTypeSet.stream()
                .map(EquipType::valueOf).collect(Collectors.toSet());
        return equipTypes.contains(equipments.getEquipType());
    }
}

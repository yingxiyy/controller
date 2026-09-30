package net.flex.dci.otc.controller.ne.manager.core.filter.impl;

import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.COMMA;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.SPECIAL_EQUIPMENT;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.TWO_UNIT;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/5/7 16:08
 */
@Component
@Slf4j
public class EquipmentFilter extends AbstractFilter<Equipments, Equipments> {

    private final static String EQUIP_TYPE = "EqType";

    private final static String LINECARD_FOUR = "LINECARD-1-4";

    public EquipmentFilter(Environment environment) {
        super(environment);
    }


    @Override
    public List<Equipments> filter(List<Equipments> elements) {
        log.debug("filter the useless equipment from now elements");

        Set<String> equipTypes = elements.stream()
                .map(equipments -> equipments.getEquipClass() == null
                        ? equipments.getEquipTypeInstalled() : equipments.getEquipClass())
                .collect(
                        Collectors.toSet());
        Boolean isHaveTwoUnit = isHaveTwoUnit(equipTypes);
        if (isHaveTwoUnit) {
            elements = discardMeaninglessEquip(elements);
        }
        log.debug("elements is :{}", elements);
        return elements;
    }

    /**
     * the LINECARD 4 is useless
     *
     * @param elements
     * @return
     */
    private List<Equipments> discardMeaninglessEquip(List<Equipments> elements) {
        return elements.stream()
                .filter(equipment -> !equipment.getEquipmentId().contains(LINECARD_FOUR)).collect(
                        Collectors.toList());
    }

    private Boolean isHaveTwoUnit(Set<String> equipTypes) {
        log.debug("real equip type set is:{}", equipTypes);
        String spEquipTypes = getElementProperty(SPECIAL_EQUIPMENT, TWO_UNIT, EQUIP_TYPE);
        Set<String> specialEquipTypeSet = Arrays.stream(spEquipTypes.split(COMMA))
                .collect(Collectors.toSet());
        log.debug("special equip type set is.{}", specialEquipTypeSet);
        Set<String> diff = NeManagerUtils.getIntersectionSetByGuava(equipTypes,
                specialEquipTypeSet);
        return !diff.isEmpty();
    }


}

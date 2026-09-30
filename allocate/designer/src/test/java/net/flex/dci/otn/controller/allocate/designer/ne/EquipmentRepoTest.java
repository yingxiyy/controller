/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others. All rights reserved.
 */
package net.flex.dci.otn.controller.allocate.designer.ne;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.lang.reflect.Field;
import net.flex.dci.otn.controller.allocate.designer.model.JsonYangConverter;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfo;
import net.flex.dci.otn.controller.allocate.ne.Card;
import net.flex.dci.otn.controller.allocate.ne.CardClass;
import net.flex.dci.otn.controller.allocate.ne.Ne;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;

class EquipmentRepoTest {

    @Test
    void bone20JsonCreatesConcreteFmuxAndTilaEquipmentTypes() throws Exception {
        String resource = "COHERENT-CHASSIS2.0-ByteDance-OD-card.json";
        Ne ne;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input, "Missing test resource: " + resource);
            ne = new ObjectMapper().readValue(input, Ne.class);
        }

        EquipmentRepo equipmentRepo = new EquipmentRepo();
        Field converterField = EquipmentRepo.class.getDeclaredField("jsonYangConverter");
        converterField.setAccessible(true);
        converterField.set(equipmentRepo, new JsonYangConverter());

        assertEquipmentType(equipmentRepo, ne, "CMUX", "FMUX_32", "FMUX32");
        assertEquipmentType(equipmentRepo, ne, "ILA", "TILA", "TILA");
    }

    private void assertEquipmentType(EquipmentRepo equipmentRepo, Ne ne,
            String cardClassName, String cardType, String expectedTypeName) throws Exception {
        Card card = ne.getCardClass().stream()
                .filter(cardClass -> cardClassName.equals(cardClass.getName()))
                .map(CardClass::getCard)
                .flatMap(java.util.Collection::stream)
                .filter(candidate -> cardType.equals(candidate.getVendorType()))
                .findFirst()
                .orElseThrow(AssertionError::new);
        NeInfo neInfo = mock(NeInfo.class);
        when(neInfo.getNe()).thenReturn(ne);
        when(neInfo.getVendor()).thenReturn(ne.getVendorName());
        when(neInfo.getProductType()).thenReturn(ne.getProductType());
        when(neInfo.getCardClassByCardVendor(cardType)).thenReturn(cardClassName);
        when(neInfo.isReplacedAsEmpty(cardType)).thenReturn(false);

        Equipments equipment = equipmentRepo.createCardEquipment(
                "Site-test#Ne-test", card, card.getPossibleSlot().get(0), neInfo);

        assertEquals(expectedTypeName, equipment.getEquipType().name());
        assertEquals(cardType, equipment.getEquipTypeConfiged());
        assertEquals(cardType, equipment.getEquipTypeVendorSpecific());
    }
}

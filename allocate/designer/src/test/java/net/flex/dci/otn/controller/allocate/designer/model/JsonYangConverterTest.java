/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others. All rights reserved.
 */
package net.flex.dci.otn.controller.allocate.designer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;

class JsonYangConverterTest {

    private final JsonYangConverter converter = new JsonYangConverter();

    @Test
    void bone20FmuxCommonPortsUseDistinctYangPortTypes() throws NeDesignerException {
        assertEquals("OPSIG1", converter.getPortType("OP-SIG1").name().toUpperCase());
        assertEquals("OPSIG2", converter.getPortType("OP-SIG2").name().toUpperCase());
    }

    @Test
    void bone20FlexCardsKeepTheirConcreteEquipmentTypes() throws NeDesignerException {
        assertEquals("FMUX32",
                converter.getEquipTypeByCardClassAndType(
                        "COHERENT", "CHASSIS2.0", "CMUX", "FMUX_32").name());
        assertEquals("TILA",
                converter.getEquipTypeByCardClassAndType(
                        "COHERENT", "CHASSIS2.0", "ILA", "TILA").name());
    }

    @Test
    void legacyModelsAndOtherCardsStillUseTheirCardClass() throws NeDesignerException {
        assertEquals(EquipType.CMUX64,
                converter.getEquipTypeByCardClassAndType(
                        "COHERENT", "CHASSIS", "CMUX", "FMUX_32"));
        assertEquals(EquipType.ILA,
                converter.getEquipTypeByCardClassAndType(
                        "COHERENT", "CHASSIS", "ILA", "TILA"));
        assertEquals(EquipType.CMUX64,
                converter.getEquipTypeByCardClassAndType(
                        "COHERENT", "CHASSIS2.0", "CMUX", "MUX_C64"));
    }
}

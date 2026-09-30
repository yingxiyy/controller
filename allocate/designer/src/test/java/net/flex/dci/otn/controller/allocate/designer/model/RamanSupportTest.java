/*
 * Copyright (c) 2019 Network Flex Any Comp. and others. All rights reserved.
 */

package net.flex.dci.otn.controller.allocate.designer.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import org.junit.jupiter.api.Test;

class RamanSupportTest {

    @Test
    void acceptsRamanAtBothEnds() {
        assertDoesNotThrow(() -> RamanSupport.validatePaired(
                Arrays.asList("IRACL", "RAMANCL_16-3"),
                Arrays.asList("IRACL", "RAMANCL_16-5"),
                "site-a", "site-z"));
    }

    @Test
    void rejectsRamanAtOnlyOneEnd() {
        assertThrows(NeDesignerException.class, () -> RamanSupport.validatePaired(
                Arrays.asList("IRACL", "RAMANCL_16-3"),
                Collections.singletonList("IRACL"),
                "site-a", "site-z"));
    }

    @Test
    void assignsSlotFiveForSingleDirection() {
        assertEquals(5, RamanSupport.getSlot(true, false, true));
        assertEquals(5, RamanSupport.getSlot(false, true, false));
    }

    @Test
    void assignsWestFiveAndEastSixForDualDirections() {
        assertEquals(5, RamanSupport.getSlot(true, true, true));
        assertEquals(6, RamanSupport.getSlot(true, true, false));
    }

    @Test
    void ilaAndDgeSupportDirectionalRaman() {
        assertTrue(RamanSupport.isDualDirectionAmplifier("ILA_CL"));
        assertTrue(RamanSupport.isDualDirectionAmplifier("DGE_CL"));
        assertFalse(RamanSupport.isDualDirectionAmplifier("IRA_CL"));
    }
}

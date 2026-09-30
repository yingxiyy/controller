package net.flex.dci.otn.controller.allocate.link.common;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.flex.dci.otc.common.exception.CommonException;
import org.junit.jupiter.api.Test;

class CreateSiteLinkParamTest {

    @Test
    void bone20OneToTwoAllowsMainOnlyCreation() {
        assertDoesNotThrow(() -> CreateSiteLinkParam.validateProtectionLegs("10", false, true, true));
    }

    @Test
    void bone20OnlyModel10IsAcceptedAndProtected() {
        assertTrue(CreateSiteLinkParam.resolveIsProtectedModel("10", true));
    }

    @Test
    void nonBone20Model10Keeps2606ModelWhitelist() {
        assertThrows(CommonException.class,
                () -> CreateSiteLinkParam.resolveIsProtectedModel("10", false));
    }

    @Test
    void legacyUnprotectedModelKeeps2606Semantics() {
        assertFalse(CreateSiteLinkParam.resolveIsProtectedModel("2", false));
    }

    @Test
    void nonBone20Model10KeepsExistingSlaveRequirement() {
        assertThrows(CommonException.class,
                () -> CreateSiteLinkParam.validateProtectionLegs("10", false, true, false));
    }

    @Test
    void otherProtectedModelsKeepExistingSlaveRequirement() {
        assertThrows(CommonException.class,
                () -> CreateSiteLinkParam.validateProtectionLegs("4", false, true, true));
    }

    @Test
    void unprotectedModelsStillRejectProtectionLegs() {
        assertThrows(CommonException.class,
                () -> CreateSiteLinkParam.validateProtectionLegs("2", true, false, true));
    }
}

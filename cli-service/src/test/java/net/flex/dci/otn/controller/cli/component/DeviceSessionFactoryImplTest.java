package net.flex.dci.otn.controller.cli.component;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DeviceSessionFactoryImplTest {

    @Test
    void shouldSelectLocalTerminalOnlyForExactCredentialPair() {
        assertTrue(DeviceSessionFactoryImpl.isLocalTerminalCredential("@@@TTY1", "TTY1@@@"));
        assertFalse(DeviceSessionFactoryImpl.isLocalTerminalCredential("@@@TTY1", "other"));
        assertFalse(DeviceSessionFactoryImpl.isLocalTerminalCredential("other", "TTY1@@@"));
    }
}

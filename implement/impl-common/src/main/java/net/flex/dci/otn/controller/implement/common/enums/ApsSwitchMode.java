package net.flex.dci.otn.controller.implement.common.enums;

import lombok.Getter;

/**
 *
 * 2025/8/21
 *
 * @author musa
 * @version 1.0
 **/
@Getter
public enum ApsSwitchMode {
    AUTO("AUTO_SWITCH"),
    MANUEL("MANUEL_SWITCH"),
    FORCE("FORCE_SWITCH");

    private final String name;

    ApsSwitchMode(String name) {
        this.name = name;
    }
}

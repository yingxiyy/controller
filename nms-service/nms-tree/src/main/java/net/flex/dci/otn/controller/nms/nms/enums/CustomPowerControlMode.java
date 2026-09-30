package net.flex.dci.otn.controller.nms.nms.enums;

import lombok.Getter;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.PowerControlMode;

/**
 * @version 1.0
 * @date 7/7/2025 5:03 PM
 */
@Getter
public enum CustomPowerControlMode {
    APC(PowerControlMode.APC, "APC"),

    /**
     * Manual power adjustment mode
     */
    MANUAL(PowerControlMode.MANUAL, "MANUAL");

    private final PowerControlMode powerControlMode;

    private final String name;

    CustomPowerControlMode(PowerControlMode powerControlMode, String name) {
        this.powerControlMode = powerControlMode;
        this.name = name;
    }


    public static CustomPowerControlMode fromName(String name) {
        for (CustomPowerControlMode controlMode : CustomPowerControlMode.values()) {
            if (controlMode.getName().equals(name)) {
                return controlMode;
            }
        }
        throw new IllegalArgumentException("No power control mode found for control mode: " + name);
    }
}

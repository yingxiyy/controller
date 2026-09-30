package net.flex.dci.otn.controller.nms.nms.enums;

import lombok.Getter;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AseControlMode;

/**
 * @version 1.0
 * @date 7/7/2025 4:20 PM
 */
@Getter
public enum CustomAseControlMode {
    UNKNOWN(AseControlMode.Unknown, "unknown"),
    ASE_ENABLED(AseControlMode.ASEENABLED, "ASE_ENABLED"),


    ASE_DISABLED(AseControlMode.ASEDISABLED, "ASE_DISABLED"),


    ASE_AUTO(AseControlMode.ASEAUTO, "ASE_AUTO");

    private AseControlMode aseControlMode;
    private String name;

    CustomAseControlMode(AseControlMode aseControlMode, String name) {
        this.aseControlMode = aseControlMode;
        this.name = name;
    }

    public static CustomAseControlMode fromName(String name) {
        for (CustomAseControlMode controlMode : CustomAseControlMode.values()) {
            if (controlMode.getName().equals(name)) {
                return controlMode;
            }
        }
        throw new IllegalArgumentException("No ase control mode found for control mode: " + name);
    }
}

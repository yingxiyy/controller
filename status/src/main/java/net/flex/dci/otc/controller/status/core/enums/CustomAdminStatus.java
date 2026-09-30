package net.flex.dci.otc.controller.status.core.enums;

import lombok.Getter;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;

/**
 *
 * @version 1.0
 * @date 8/26/2025 2:47 PM
 */
@Getter
public enum CustomAdminStatus {
    Up("up", AdminStatus.Up),

    /**
     * Disabled.
     *
     */
    Down("down", AdminStatus.Down),

    /**
     * In some test mode.
     *
     */
    Testing("testing", AdminStatus.Testing),

    /**
     * Resource is disabled in the data plane for maintenance purposes.
     *
     */
    Maintenance("maintenance", AdminStatus.Maintenance),

    /**
     * Status cannot be determined for some reason.
     *
     */
    Unknown("unknown", AdminStatus.Unknown);
    private final String name;

    private final AdminStatus adminStatus;

    CustomAdminStatus(String name, AdminStatus adminStatus) {
        this.name = name;
        this.adminStatus = adminStatus;
    }

    public static AdminStatus fromStateName(String name) {
        for (CustomAdminStatus customAdminStatus : CustomAdminStatus.values()) {
            if (customAdminStatus.name.equals(name)) {
                return customAdminStatus.getAdminStatus();
            }
        }
        throw new IllegalArgumentException("UnSupported admin status name :" + name);
    }

}

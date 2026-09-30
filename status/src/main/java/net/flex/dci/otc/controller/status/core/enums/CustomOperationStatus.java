package net.flex.dci.otc.controller.status.core.enums;

import lombok.Getter;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;

/**
 *
 * @version 1.0
 * @date 8/26/2025 2:47 PM
 */
@Getter
public enum CustomOperationStatus {

    Unknown("unknown", OperStatus.Unknown),

    /**
     * Operational disable.
     *
     */
    Disable("disable", OperStatus.Disable),

    /**
     * Operational up.
     *
     */
    Up("up", OperStatus.Up),

    /**
     * Resource is disabled in the data plane for maintenance purposes.
     *
     */
    Maintenance("maintenance", OperStatus.Maintenance),

    /**
     * In some test mode.
     *
     */
    Testing("testing", OperStatus.Testing),

    /**
     * Some cards/tps down.
     *
     */
    Down("down", OperStatus.Down),

    /**
     * Ne synchronization failure.
     *
     */
    SynchronizationException("synchronizationException", OperStatus.SynchronizationException),

    /**
     * Ne communication failure.
     *
     */
    NeCommunicationException("neCommunicationException", OperStatus.NeCommunicationException);

    private final String name;

    private final OperStatus operStatus;

    CustomOperationStatus(String name, OperStatus operStatus) {
        this.name = name;
        this.operStatus = operStatus;
    }

    public static OperStatus getOperStatusFromName(String operStatus) {
        for (CustomOperationStatus customOperationStatus : CustomOperationStatus.values()) {
            if (customOperationStatus.name.equals(operStatus)) {
                return customOperationStatus.getOperStatus();
            }
        }
        throw new IllegalArgumentException("UnSupported operation status name :" + operStatus);
    }

}

package net.flex.dci.otc.controller.status.core.enums;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;

/**
 * @version 1.0
 * @date 2022/4/7 13:31
 */
public enum OperationStatusCode {
    UNKOWN(0b00000001),
    DISABLED(0b00000010),
    UP(0b00000100),
    Maintenance(0b00001000),
    TESTING(0b00010000),
    DOWN(0b00100000),
    SYNCHRONIZATION_EXCEPTION(0b010000000),
    NECOMMUNICATION_EXCEPTION(0b10000000),
    INACTIVE(0b00000001_00000000);
    private Integer code;

    OperationStatusCode(int code) {
        this.code = code;
    }

    /**
     * get oper status
     *
     * @param operStatus
     * @return
     */
    public static int getOperationStatusCode(OperStatus operStatus) {
        Integer code = UNKOWN.code;
        if (operStatus.equals(OperStatus.Unknown)) {
            code = UNKOWN.code;
        } else if (operStatus.equals(OperStatus.Disable)) {
            code = DISABLED.code;
        } else if (operStatus.equals(OperStatus.Up)) {
            code = UP.code;
        } else if (operStatus.equals(OperStatus.Maintenance)) {
            code = Maintenance.code;
        } else if (operStatus.equals(OperStatus.Testing)) {
            code = TESTING.code;
        } else if (operStatus.equals(OperStatus.Down)) {
            code = DOWN.code;
        } else if (operStatus.equals(OperStatus.SynchronizationException)) {
            code = SYNCHRONIZATION_EXCEPTION.code;
        } else if (operStatus.equals(OperStatus.NeCommunicationException)) {
            code = NECOMMUNICATION_EXCEPTION.code;
        } else if (operStatus.equals(OperStatus.Inactive)) {
            code = INACTIVE.code;
        }

        return code;
    }

    /**
     * get oper status for the code
     *
     * @param statusCode status code
     * @return operStatus
     */
    public static OperStatus getOperStatusType(int statusCode) {
        OperStatus operStatus = OperStatus.Unknown;

        if ((statusCode & NECOMMUNICATION_EXCEPTION.code) == NECOMMUNICATION_EXCEPTION.code) {
            return OperStatus.NeCommunicationException;
        }
        if ((statusCode & SYNCHRONIZATION_EXCEPTION.code)
                == SYNCHRONIZATION_EXCEPTION.code) {
            return OperStatus.SynchronizationException;
        }
        if ((statusCode & DOWN.code) == DOWN.code) {
            return OperStatus.Down;
        }
        if ((statusCode & TESTING.code) == TESTING.code) {
            return OperStatus.Testing;
        }
        if ((statusCode & Maintenance.code) == Maintenance.code) {
            return OperStatus.Maintenance;
        }
        if ((statusCode & UP.code) == UP.code) {
            return OperStatus.Up;
        }
        if ((statusCode & INACTIVE.code) == INACTIVE.code) {
            return OperStatus.Inactive;
        }
        if ((statusCode & DISABLED.code) == DISABLED.code) {
            return OperStatus.Disable;
        }
        if ((statusCode & UNKOWN.code) == UNKOWN.code) {
            return OperStatus.Unknown;
        }
        return operStatus;
    }

    public int getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
}

package net.flex.dci.otc.controller.status.core.enums;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;

/**
 * @version 1.0
 * @date 2022/4/4 10:41
 */
public enum AlarmSeverityCode {
    CLEARED(0b0000001),
    UNKNOWN(0b0000010),
    SYNCHRONIZATIONEXCEPTION(0b0000100),
    WARNING(0b0001000),
    MINOR(0b0010000),
    MAJOR(0b0100000),
    CRITICAL(0b1000000);

    private int code;

    AlarmSeverityCode(int code) {
        this.code = code;
    }

    public static int getSeverityCode(AlarmSeverity alarmSeverity) {
        int code = 0b0000001;
        if (alarmSeverity.equals(AlarmSeverity.Cleared)) {
            code = CLEARED.getCode();
        } else if (alarmSeverity.equals(AlarmSeverity.Unknown)) {
            code = UNKNOWN.getCode();
        } else if (alarmSeverity.equals(AlarmSeverity.SynchronizationException)) {
            code = SYNCHRONIZATIONEXCEPTION.getCode();
        } else if (alarmSeverity.equals(AlarmSeverity.Warning)) {
            code = WARNING.getCode();
        } else if (alarmSeverity.equals(AlarmSeverity.Minor)) {
            code = MINOR.getCode();
        } else if (alarmSeverity.equals(AlarmSeverity.Major)) {
            code = MAJOR.getCode();
        } else if (alarmSeverity.equals(AlarmSeverity.Critical)) {
            code = CRITICAL.getCode();
        }
        return code;
    }

    public static AlarmSeverity getSeverity(int code) {
        if ((code & CRITICAL.code) == CRITICAL.code) {
            return AlarmSeverity.Critical;
        } else if ((code & MAJOR.code) == MAJOR.code) {
            return AlarmSeverity.Major;
        } else if ((code & MINOR.code) == MINOR.code) {
            return AlarmSeverity.Minor;
        } else if ((code & WARNING.code) == WARNING.code) {
            return AlarmSeverity.Warning;
        } else if ((code & SYNCHRONIZATIONEXCEPTION.code) == SYNCHRONIZATIONEXCEPTION.code) {
            return AlarmSeverity.SynchronizationException;
        } else if ((code & CLEARED.code) == CLEARED.code) {
            return AlarmSeverity.Cleared;
        } else if ((code & UNKNOWN.code) == UNKNOWN.code) {
            return AlarmSeverity.Unknown;
        }
        return AlarmSeverity.Unknown;
    }

    public int getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
}

package net.flex.dci.otc.controller.status.alarm.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 *
 * 2025/12/27
 *
 * @author musa
 * @version 1.0
 **/
@Data
public class WrappedCtrlAlarm implements Serializable {

    private List<WrappedAlarm> newAlarms;
    private List<WrappedAlarm> clearAlarms;
    private Long eventTime;
}

package net.flex.dci.otc.controller.status.alarm.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otc.common.model.alarm.Alarm;

/**
 * 2026/4/5
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class AlarmsDetail implements Serializable {

    private List<Alarm> validNewAlarms;

    private List<Alarm> allClearAlarms;

    private List<Alarm> validClearAlarms;

    private int expiredCount;

    private List<Alarm> allNewAlarms;
}

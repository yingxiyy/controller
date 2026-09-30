package net.flex.dci.otc.controller.notifier.core.domain.alarm;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * alarm type id
 *
 * @version 1.0
 * @date 2022/4/2 11:17
 */
@Data
@Builder
@AllArgsConstructor
public class AlarmNotificationBody implements Serializable {

    @JSONField(name = "new-alarm")
    private List<AlarmDetail> newAlarm;

    @JSONField(name = "removed-alarm-id")
    private List<String> removeAlarmId;


    @JSONField(name = "remove-alarm")
    private List<AlarmDetail> removeAlarm;

}

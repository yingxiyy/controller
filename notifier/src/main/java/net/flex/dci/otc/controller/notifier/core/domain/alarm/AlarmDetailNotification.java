package net.flex.dci.otc.controller.notifier.core.domain.alarm;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/4/2 15:42
 */
@Data
@Builder
@AllArgsConstructor
public class AlarmDetailNotification implements Serializable {

    @JSONField(name = "notification")
    private AlarmNotification alarmNotification;
}

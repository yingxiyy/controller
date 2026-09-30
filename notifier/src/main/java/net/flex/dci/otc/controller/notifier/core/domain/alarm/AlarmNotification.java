package net.flex.dci.otc.controller.notifier.core.domain.alarm;

import com.alibaba.fastjson.annotation.JSONField;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/4/2 11:15
 */
@Data
@Builder
@AllArgsConstructor
public class AlarmNotification implements Serializable {

    @JSONField(name = "alarm-notification")
    private AlarmNotificationBody notificationBody;

    @SerializedName("eventTime")
    private String eventTime;
}

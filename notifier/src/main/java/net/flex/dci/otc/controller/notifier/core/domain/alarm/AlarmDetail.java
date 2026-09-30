package net.flex.dci.otc.controller.notifier.core.domain.alarm;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2026/1/17
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmDetail implements Serializable {

    @JSONField(name = "alarm-index")
    private Long index;

    @JSONField(name = "alarm-id")
    private String alarmId;

    @JSONField(name = "resource-ref")
    private String resourceRef;

    @JSONField(name = "component-ref")
    private String componentRef;


    @JSONField(name = "serverity")
    private String severity;

    @JSONField(name = "alarm-type-id")
    private String alarmTypeId;

    @JSONField(name = "alarm-text")
    private String alarmText;

    @JSONField(name = "alarm-group")
    private String alarmGroup;

    @JSONField(name = "creation-time")
    private Long creationTime;

    @JSONField(name = "creation-received-time")
    private Long nmlReceivedTime;

    @JSONField(name = "ne-ip")
    private String neIp;

    @JSONField(name = "ne-id")
    private String neId;

    @JSONField(name = "nml-key")
    private String nmlKey;

    private Boolean sa;

    @JSONField(name = "nml-key-name")
    private String nmlKeyName;
}

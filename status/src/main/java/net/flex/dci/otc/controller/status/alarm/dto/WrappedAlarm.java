package net.flex.dci.otc.controller.status.alarm.dto;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2025/12/27
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WrappedAlarm implements Serializable {

    @JSONField(
            name = "ip"
    )
    private String ip;
    @JSONField(
            name = "toop-key"
    )
    private String toopKey;

    @JSONField(
            name = "toop-key-name"
    )
    private String toopKeyName;

    @JSONField(
            name = "toop-ip"
    )
    private String toopIp;
    @JSONField(
            name = "id"
    )
    private String id;
    @JSONField(
            name = "is-cleared"
    )
    private Boolean isClear;
    @JSONField(
            name = "resource"
    )
    private String resource;
    @JSONField(
            name = "group"
    )
    private String group;
    @JSONField(
            name = "type-id"
    )
    private String typeId;
    @JSONField(
            name = "severity"
    )
    private String severity;
    @JSONField(
            name = "component"
    )
    private String component;
    @JSONField(
            name = "time-created"
    )
    private Long timeCreated;
    @JSONField(
            name = "text"
    )
    private String text;
    @JSONField(
            name = "service-affect"
    )
    private Boolean serviceAffect;
    @JSONField(
            name = "alarm-type-id"
    )
    private String alarmTypeId;


    @JSONField(
            name = "ne-id"
    )
    private String neId;

    @JSONField(
            name = "alarm-index"
    )
    private Long index;
}

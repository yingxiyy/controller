package net.flex.dci.otc.controller.status.dto.alarm;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/8/17
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class AlarmDto {

    @SerializedName("alarm-id")
    private String alarmId;
    @SerializedName("resource-ref")
    private String resourceRef;
    @SerializedName("alarm-type-id")
    private String alarmTypeId;
    @SerializedName("nml-key-name")
    private String nmlKeyName;
    @SerializedName("equipment-ref")
    private String equipmentRef;
    @SerializedName("alarm-group")
    private String alarmGroup;
    @SerializedName("creation-received-time")
    private Long creationReceivedTime;
    @SerializedName("sa")
    private Boolean sa;
    @SerializedName("alarm-text")
    private String alarmText;
    @SerializedName("creation-time")
    private Long creationTime;
    @SerializedName("serverity")
    private String serverity;
    @SerializedName("nml-key")
    private String nmlKey;
    @SerializedName("ne-id")
    private String neId;
    @SerializedName("alarm-index")
    private Long alarmIndex;
    @SerializedName("clear-time")
    private Long clearTime;
    @SerializedName("archive-time")
    private Long archiveTime;
    @SerializedName("archive-type")
    private String archiveType;   // "cleared"
}

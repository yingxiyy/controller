package net.flex.dci.otn.controller.apsswitchlog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;
import net.flex.dci.otn.db.jpa.enums.ApsModeType;
import net.flex.dci.otn.db.jpa.enums.TriggerType;

@Data
@Builder
@AllArgsConstructor
public class ApsSwitchLogDto implements Serializable {

    private Long id;

    private String neId;
    private String neName;
    private String logId;
    private String activePath;
    private String apsModuleName;
    @JsonProperty("aps_mode")
    private ApsModeType apsMode;  // 需补充实际枚举类
    private TriggerType triggerType;
    @JsonProperty("start_time")
    private long startTime;  // 纳秒级时间戳

    @JsonProperty("end_time")
    private long endTime;
    private long duration;
    
    @JsonProperty("log_detail")
    private List<ApsSwitchLogDetail> logDetails;

    private long createTimestamp;


    @Tolerate
    public ApsSwitchLogDto() {

    }
}

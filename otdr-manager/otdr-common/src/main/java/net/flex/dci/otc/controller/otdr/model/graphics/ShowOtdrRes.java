package net.flex.dci.otc.controller.otdr.model.graphics;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otc.controller.otdr.model.OtdrWaveForm;

/**
 * @version 1.0
 * @date 8/18/2023 4:33 PM
 */
@Data
@Builder
public class ShowOtdrRes implements Serializable {

    @JSONField(name = "monitor-port")
    private String monitorPort;

    @JSONField(name = "monitor-port-id")
    private String monitorPortId;

    @JSONField(name = "monitor-port-name")
    private String monitorPortName;

    @JSONField(name = "monitor-ne-id")
    private String monitorNeId;

    @JSONField(name = "monitor-ne-name")
    private String monitorNeName;

    @JSONField(name = "monitor-direction")
    private String monitorDirection;


    @JSONField(name = "scan-time")
    private Long scanTime;

    @JSONField(name = "result-id")
    private String resultId;


    @JSONField(name = "waveform")
    private OtdrWaveForm waveForm;

    @JSONField(name = "start-time")
    private Long startTime;

    @JSONField(name = "task-id")
    private Long taskId;

}

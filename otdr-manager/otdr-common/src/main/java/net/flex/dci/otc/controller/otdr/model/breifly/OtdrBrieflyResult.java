package net.flex.dci.otc.controller.otdr.model.breifly;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.Data;

/**
 * @version 1.0
 * @date 9/4/2023 2:46 PM
 */
@Data
public class OtdrBrieflyResult implements Serializable {

    @JSONField(name = "base-distance")
    private String baseDistance;

    @JSONField(name = "base-loss")
    private String baseLoss;

    @JSONField(name = "delta-distance")
    private String deltaDistance;

    @JSONField(name = "delta-loss")
    private String deltaLoss;

    private String distance;

    @JSONField(name = "monitor-direction")
    private String monitorDirection;

    private String loss;

    @JSONField(name = "monitor-port")
    private String monitorPort;

    @JSONField(name = "monitor-port-name")
    private String monitorPortName;

    @JSONField(name = "node-id")
    private String nodeId;

    @JSONField(name = "start-time")
    private Long startTime;

    @JSONField(name = "task-id")
    private Long taskId;

    @JSONField(name = "scan-mode")
    private String scanMode;

}

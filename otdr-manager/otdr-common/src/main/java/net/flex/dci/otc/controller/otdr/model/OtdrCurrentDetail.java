package net.flex.dci.otc.controller.otdr.model;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/7/20 15:35
 */
@Data
public class OtdrCurrentDetail implements Serializable {

    @JSONField(name = "distance-range")
    private Integer distanceRange;

    @JSONField(name = "monitor-port")
    private String monitorPort;

    @JSONField(name = "monitor-port-name")
    private String monitorPortName;

    @JSONField(name = "monitor-ne-id")
    private String monitorNeId;

    @JSONField(name = "monitor-ne-name")
    private String monitorNeName;

    @JSONField(name = "end-of-fiber-threshold")
    private String endOfFiberThreshold;

    @JSONField(name = "scan-wavelength")
    private String scanWaveLength;

    @JSONField(name = "monitor-direction")
    private String monitorDirection;

    @JSONField(name = "splice-loss-threshold")
    private String spliceLossThreshold;

    @JSONField(name = "scan-state")
    private String scanState;

    @JSONField(name = "scan-mode")
    private String scanMode;

    @JSONField(name = "reflection-threshold")
    private String reflectionThreshold;

    @JSONField(name = "pulse-width")
    private Integer pulseWidth;

    @JSONField(name = "linecard")
    private String lineCard;

    @JSONField(name = "scan-progress")
    private Integer scanProgress;

    @JSONField(name = "loss")
    private String loss;

    @JSONField(name = "refractive-index")
    private String refractiveIndex;

    @JSONField(name = "scan-time")
    private Long scanTime;

    @JSONField(name = "distance")
    private String distance;

    @JSONField(name = "sampling-resolution")
    private String samplingResolution;

    @JSONField(name = "start-time")
    private String startTime;

    @JSONField(name = "result-id")
    private String resultId;

    @JSONField(name = "waveform")
    private OtdrWaveForm waveForm;

    @JSONField(name = "events")
    private OtdrEvents events;
}

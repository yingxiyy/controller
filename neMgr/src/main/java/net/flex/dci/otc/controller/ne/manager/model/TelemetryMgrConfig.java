package net.flex.dci.otc.controller.ne.manager.model;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.Data;

/**
 * @version 1.0
 * @date 8/14/2023 12:56 PM
 */
@Data
public class TelemetryMgrConfig implements Serializable {

    @JSONField(name = "telemetry-manager")
    private TelemetryMgrConfigRoot telemetryMgrConfigRoot;
}

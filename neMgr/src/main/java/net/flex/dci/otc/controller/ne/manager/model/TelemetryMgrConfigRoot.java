package net.flex.dci.otc.controller.ne.manager.model;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.Data;

/**
 * @version 1.0
 * @date 8/14/2023 1:28 PM
 */
@Data
public class TelemetryMgrConfigRoot implements Serializable {


    private Boolean mixed;

    @JSONField(name = "ipArea")
    private TelemetryIpArea ipPrefix;

    @JSONField(name = "ntp")
    private TelemetryMgrNtpConfig telemetryMgrNtpConfig;

    private String timezone;

}

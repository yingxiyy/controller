package net.flex.dci.otc.controller.ne.manager.model;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.Data;

/**
 * @version 1.0
 * @date 8/14/2023 1:36 PM
 */
@Data
public class TelemetryMgrNtpConfig implements Serializable {

    @JSONField(name = "private")
    private String privateNtp;

    @JSONField(name = "public")
    private String publicNtp;
}

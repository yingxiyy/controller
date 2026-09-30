package net.flex.dci.otc.controller.ne.manager.model;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.Data;

/**
 * @version 1.0
 * @date 8/14/2023 1:35 PM
 */
@Data
public class TelemetryIpArea implements Serializable {

    @JSONField(name = "private")
    private String privateIpArea;

    @JSONField(name = "public")
    private String publicIpArea;
}

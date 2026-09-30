package net.flex.dci.otc.controller.ne.manager.core.kafka.model;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/3/25 13:43
 */
@Data
public class NeState implements Serializable {

    private String nodeId;

    @JSONField(name = "ne-track")
    private String operationalState;

    private String implementState;

    private String ip;

    private String port;

}

package net.flex.dci.otn.controller.db.monitor.core.dto;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * 2025/6/20
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class NeStateDto implements Serializable {

    private String nodeId;

    @JSONField(name = "ne-track")
    private String operationalState;


    private String implementState;
}

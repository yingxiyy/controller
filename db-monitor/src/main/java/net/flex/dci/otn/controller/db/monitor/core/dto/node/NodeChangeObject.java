package net.flex.dci.otn.controller.db.monitor.core.dto.node;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/11/14 16:56
 */
@Data
@Builder
@AllArgsConstructor
public class NodeChangeObject implements Serializable {

    @JSONField(name = "node-id")
    private String nodeId;

    private Physical physical;

}

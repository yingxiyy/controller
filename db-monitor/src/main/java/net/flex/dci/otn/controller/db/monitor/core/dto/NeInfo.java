package net.flex.dci.otn.controller.db.monitor.core.dto;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/11/16 13:54
 */
@Data
@Builder
@AllArgsConstructor
public class NeInfo implements Serializable {

    @JSONField(name = "node-id")
    private String neId;

}

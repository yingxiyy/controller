package net.flex.dci.otc.controller.otdr.model;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/7/20 15:46
 */

@Data
public class OtdrEvents implements Serializable {

    @JSONField(name = "event")
    private List<OtdrEvent> events;
}

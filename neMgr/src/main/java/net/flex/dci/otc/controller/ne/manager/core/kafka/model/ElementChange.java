package net.flex.dci.otc.controller.ne.manager.core.kafka.model;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/4/27 15:58
 */
@Data
public class ElementChange implements Serializable {

    @JSONField(name = "msg-type")
    private String msgType;

    @JSONField(name = "obj-change")
    private ObjectChange objectChange;

    @JSONField(name = "ne-conn-status")
    private NeState neState;
}

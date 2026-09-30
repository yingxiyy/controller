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
public class NodeChangeNotifyBody implements Serializable {

    @JSONField(name = "msg-type")
    private String msgType;


    @JSONField(name = "ne-conn-status")
    private NeStateDto neState;
}

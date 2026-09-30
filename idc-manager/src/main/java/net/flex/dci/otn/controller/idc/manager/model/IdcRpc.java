package net.flex.dci.otn.controller.idc.manager.model;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

/**
 * @version 1.0
 * @date 2022/2/8 10:23
 */
@Data
@Builder
public class IdcRpc implements Serializable {

    @Tolerate
    public IdcRpc() {

    }

    @JSONField(name = "input")
    private RpcInput input;

    @JSONField(name = "output")
    private RpcOutput output;
}

package net.flex.dci.otn.controller.idc.manager.model;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

/**
 * @version 1.0
 * @date 2022/10/26 16:24
 */
@Data
@Builder
public class GetCityListByRegionRpc implements Serializable {

    @Tolerate
    public GetCityListByRegionRpc() {

    }

    @JSONField(name = "input")
    private RpcRegionInput input;

    @JSONField(name = "output")
    private RpcOutput output;
}

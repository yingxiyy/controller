package net.flex.dci.otn.controller.idc.manager.model;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;
import net.flex.dci.otn.controller.idc.manager.dto.CityInfo;

/**
 * @version 1.0
 * @date 2022/2/8 10:26
 */
@Data
@Builder
public class RpcOutput implements Serializable {

    @Tolerate
    public RpcOutput() {

    }

    @JSONField(name = "city-name")
    private List<String> cityName;

    @JSONField(name = "city")
    private List<CityInfo> cityInfo;


    @JSONField(name = "idc-list")
    private List<IdcDisplayData> idcs;

}

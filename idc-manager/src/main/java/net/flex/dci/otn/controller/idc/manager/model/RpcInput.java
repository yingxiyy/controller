package net.flex.dci.otn.controller.idc.manager.model;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import javax.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;
import net.flex.dci.otn.controller.idc.manager.validate.IdcValidateGroup;

/**
 * @version 1.0
 * @date 2022/2/8 10:25
 */
@Data
@Builder
public class RpcInput implements Serializable {

    @Tolerate
    public RpcInput() {

    }

    @JSONField(name = "region-name")
//    @NotBlank(message = "region name should not be blank", groups = IdcValidateGroup.RegionName.class)
    @NotBlank(message = "api.request.idc.rpc.regionNullError", groups = IdcValidateGroup.RegionName.class)
    private String regionName;


    @JSONField(name = "city-name")
    @NotBlank(message = "api.request.idc.rpc.cityNullError", groups = IdcValidateGroup.CityData.class)
    private String cityName;
}

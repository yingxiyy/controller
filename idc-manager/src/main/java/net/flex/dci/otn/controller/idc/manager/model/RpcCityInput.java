package net.flex.dci.otn.controller.idc.manager.model;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;
import net.flex.dci.otn.controller.idc.manager.validate.IdcValidateGroup;

/**
 * @version 1.0
 * @date 2022/10/26 16:04
 */
@Data
@Builder
public class RpcCityInput implements Serializable {

    @Tolerate
    public RpcCityInput() {

    }

    @JSONField(name = "city-name")
    @NotBlank(message = "api.request.idc.rpc.cityNullError", groups = IdcValidateGroup.CityData.class)
    private String name;

    @JSONField(name = "city-id")
    @NotNull(message = "api.request.idc.rpc.cityIdNullError", groups = IdcValidateGroup.CityData.class)
    private Long id;

}

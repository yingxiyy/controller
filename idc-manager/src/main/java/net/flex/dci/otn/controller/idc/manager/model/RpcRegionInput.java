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
 * @date 2022/10/26 16:09
 */
@Data
@Builder
public class RpcRegionInput implements Serializable {

    @Tolerate
    public RpcRegionInput() {

    }

    @JSONField(name = "region-name")
//    @NotBlank(message = "region name should not be blank", groups = IdcValidateGroup.RegionName.class)
    @NotBlank(message = "api.request.idc.rpc.regionNullError", groups = IdcValidateGroup.RegionName.class)
    private String regionName;


    @JSONField(name = "region-id")
//    @NotBlank(message = "region name should not be blank", groups = IdcValidateGroup.RegionName.class)
    @NotNull(message = "api.request.idc.rpc.regionIdNullError", groups = IdcValidateGroup.RegionName.class)
    private Long regionId;

}

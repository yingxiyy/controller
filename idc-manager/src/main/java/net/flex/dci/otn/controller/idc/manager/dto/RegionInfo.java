package net.flex.dci.otn.controller.idc.manager.dto;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/10/26 15:52
 */
@Data
@Builder
public class RegionInfo implements Serializable {

    @JSONField(name = "region-name")
    private String name;

    @JSONField(name = "region-id")
    private Long regionId;

}

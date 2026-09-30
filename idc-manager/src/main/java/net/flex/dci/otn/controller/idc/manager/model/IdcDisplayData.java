package net.flex.dci.otn.controller.idc.manager.model;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

/**
 * @version 1.0
 * @date 2022/2/8 10:35
 */
@Data
@Builder
public class IdcDisplayData implements Serializable {

    @Tolerate
    public IdcDisplayData() {

    }

    @JSONField(name = "idc-id")
    private Long id;

    @JSONField(name = "idc-display-name")
    private String idcDisplayName;

    @JSONField(name = "idc-name")
    private String idcName;

    @JSONField(name = "idc-code")
    private String idcCode;

}

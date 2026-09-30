package net.flex.dci.otc.controller.otdr.domain;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/9/2 16:00
 */
@Data
@Builder
public class OtdrTaskResult implements Serializable {

    @JSONField(name = "otdr")
    private OtdrTaskResultDetail detail;

}

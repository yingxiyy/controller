package net.flex.dci.otc.controller.otdr.model;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/7/20 15:15
 */
@Data
public class OtdrResult implements Serializable {

    @JSONField(name = "output")
    private OtdrCurrentDetail detail;
}

package net.flex.dci.otc.controller.otdr.model;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/7/20 15:44
 */
@Data
public class OtdrWaveForm implements Serializable {

    @JSONField(name = "data-length")
    private Long dataLength;

    @JSONField(name = "data")
    private String data;
}

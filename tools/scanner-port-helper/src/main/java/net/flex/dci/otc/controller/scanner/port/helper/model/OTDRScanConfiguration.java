package net.flex.dci.otc.controller.scanner.port.helper.model;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.Data;

/**
 * @version 1.0
 * @date 11/27/2023 4:59 PM
 */
@Data
public class OTDRScanConfiguration implements Serializable {

    @JSONField(name = "IN")
    private OTDRScanProperty in;

    @JSONField(name = "OUT")
    private OTDRScanProperty out;


}

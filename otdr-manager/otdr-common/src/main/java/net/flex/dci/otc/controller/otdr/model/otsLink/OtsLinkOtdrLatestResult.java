package net.flex.dci.otc.controller.otdr.model.otsLink;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 *
 * 2025/8/10
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class OtsLinkOtdrLatestResult implements Serializable {

    @JSONField(name = "link-id")
    private String linkId;

    @JSONField(name = "link-name")
    private String linkName;

    @JSONField(name = "a-to-z")
    private OtsDirectionOtdrResult aToz;

    @JSONField(name = "z-to-a")
    private OtsDirectionOtdrResult zToa;

    private OtsLinkTerminalInfo source;

    private OtsLinkTerminalInfo destination;
}

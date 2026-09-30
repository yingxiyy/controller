package net.flex.dci.otc.controller.otdr.model.otsLink;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 *
 * @version 1.0
 * @date 8/18/2025 2:36 PM
 */
@Data
@Builder
public class OtsResults implements Serializable {

    @JSONField(name = "ots-links")
    private List<OtsLinkOtdrLatestResult> otsLinks;
}

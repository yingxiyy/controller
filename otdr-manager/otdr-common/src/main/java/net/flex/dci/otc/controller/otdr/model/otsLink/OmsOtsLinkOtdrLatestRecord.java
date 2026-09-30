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
public class OmsOtsLinkOtdrLatestRecord implements Serializable {

    @JSONField(name = "oms-link")
    private OmsLinkTotalInfo omsLinkTotalInfo;


    @JSONField(name = "ots-results")
    private OtsResults otsResults;
}

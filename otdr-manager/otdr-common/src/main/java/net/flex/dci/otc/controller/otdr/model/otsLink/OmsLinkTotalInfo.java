package net.flex.dci.otc.controller.otdr.model.otsLink;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/**
 *
 * @version 1.0
 * @date 8/18/2025 2:35 PM
 */
@Data
@Builder
public class OmsLinkTotalInfo implements Serializable {

    @JSONField(name = "oms-link-id")
    private String omsLinkId;

    @JSONField(name = "oms-link-name")
    private String omsLinkName;

    @JSONField(name = "az-route-length")
    private BigDecimal aZRouteLength;

    @JSONField(name = "contract-az-route-length")
    private BigDecimal contractAzRouteLength;


    @JSONField(name = "za-route-length")
    private BigDecimal zARouteLength;

    @JSONField(name = "contract-za-route-length")
    private BigDecimal contractZaRouteLength;
}

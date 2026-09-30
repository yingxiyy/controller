package net.flex.dci.otc.controller.otdr.model.otsLink;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import java.math.BigDecimal;
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
public class OtsDirectionOtdrResult implements Serializable {

    @JSONField(name = "attenuation-db")
    private BigDecimal attenuationDb;

    @JSONField(name = "contract-attenuation-db")
    private BigDecimal contractAttenuationDb;

    @JSONField(name = "length-km")
    private BigDecimal length;

    @JSONField(name = "contract-length-km")
    private BigDecimal contractLength;
}

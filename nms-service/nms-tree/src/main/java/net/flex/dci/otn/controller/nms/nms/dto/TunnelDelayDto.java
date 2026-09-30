package net.flex.dci.otn.controller.nms.nms.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/**
 *
 * 2025/8/16
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class TunnelDelayDto implements Serializable {

    private BigDecimal azDelay;

    private BigDecimal zaDelay;
}

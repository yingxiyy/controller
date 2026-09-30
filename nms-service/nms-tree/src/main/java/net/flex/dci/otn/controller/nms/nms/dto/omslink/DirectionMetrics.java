package net.flex.dci.otn.controller.nms.nms.dto.omslink;

import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/**
 * 2025/8/9
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class DirectionMetrics implements Serializable {

    private BigDecimal routeLength;

    private BigDecimal routeDelay;

    private BigDecimal contractLength;

    private long serviceCount;
}

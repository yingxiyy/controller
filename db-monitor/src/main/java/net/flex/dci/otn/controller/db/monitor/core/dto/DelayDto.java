package net.flex.dci.otn.controller.db.monitor.core.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/**
 *
 * @version 1.0
 * @date 8/18/2025 5:19 PM
 */
@Data
@Builder
public class DelayDto {

    private BigDecimal azDelay;

    private BigDecimal zaDelay;
}

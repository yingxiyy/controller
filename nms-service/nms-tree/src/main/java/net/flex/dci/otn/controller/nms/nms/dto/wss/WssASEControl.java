package net.flex.dci.otn.controller.nms.nms.dto.wss;

import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otn.controller.nms.nms.enums.CustomAseControlMode;

/**
 * @version 1.0
 * @date 7/7/2025 2:26 PM
 */
@Data
@Builder
public class WssASEControl implements Serializable {

    private CustomAseControlMode aseControlMode;

    private BigDecimal injectionThreshold;

    private BigDecimal injectionHysteresis;

}

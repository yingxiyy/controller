package net.flex.dci.otn.controller.nms.nms.dto.wss;

import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otn.controller.nms.nms.enums.CustomPowerControlMode;

/**
 * @version 1.0
 * @date 7/7/2025 2:26 PM
 */
@Data
@Builder
public class WssPowerControl implements Serializable {

    private CustomPowerControlMode mode;

    private BigDecimal targetPower;

    private BigDecimal calibrationPower;

    private BigDecimal activationThreshold;

}

package net.flex.dci.otn.controller.nms.nms.dto.omslink;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 *
 * @version 1.0
 * @date 8/20/2025 3:25 PM
 */
@Data
@Builder
public class OmsLinkPAInfo implements Serializable {

    private OtsLinkAmplifierInfo azPowerAmplifier;

    private OtsLinkAmplifierInfo zaPowerAmplifier;
}

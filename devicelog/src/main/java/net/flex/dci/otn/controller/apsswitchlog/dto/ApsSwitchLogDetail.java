package net.flex.dci.otn.controller.apsswitchlog.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 *
 * @version 1.0
 * @date 9/19/2025 2:48 PM
 */
@Data
@Builder
public class ApsSwitchLogDetail implements Serializable {

    private String channel;

    private List<SamplePoint> samplePoints;
}

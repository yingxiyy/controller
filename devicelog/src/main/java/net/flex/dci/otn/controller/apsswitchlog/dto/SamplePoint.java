package net.flex.dci.otn.controller.apsswitchlog.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 *
 * @version 1.0
 * @date 9/19/2025 3:13 PM
 */
@Data
@Builder
public class SamplePoint implements Serializable {

    private int index;

    private double value;

    private long timeOffsetMs;
}

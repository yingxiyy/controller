package net.flex.dci.otn.controller.system.config.common.model;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/1/4
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class TimezoneInfo implements Serializable {
    private String code;
    private String coordinates;
    private String tz;
}

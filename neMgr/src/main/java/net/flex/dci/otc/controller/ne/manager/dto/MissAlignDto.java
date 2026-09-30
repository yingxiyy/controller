package net.flex.dci.otc.controller.ne.manager.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @version 1.0
 * @date 7/17/2023 2:11 PM
 */
@Data
@Builder
public class MissAlignDto implements Serializable {

    private String slot;

    private String designType;

    private String designName;

    private String actualType;

    private String actualName;
}

package net.flex.dci.otn.controller.implement.common.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * 2025/9/5
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApsSwitchConfigTaskDetail implements Serializable {

    private String tunnelName;

    private String tunnelId;

    private String neId;

    private String neName;

    private String apsXcId;

    private String apsName;

    private ApsSwitchConfig apsSwitch;

    private String commandDetail;

    private String detailResult;
}

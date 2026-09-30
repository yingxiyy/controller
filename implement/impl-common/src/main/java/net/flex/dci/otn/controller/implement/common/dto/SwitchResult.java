package net.flex.dci.otn.controller.implement.common.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otn.controller.implement.common.enums.SetResultCode;

/**
 *
 * 2025/8/13
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class SwitchResult implements Serializable {

    public SetResultCode code;

    public String message;

    public String tunnelId;

    public String ochLinkId;

    public List<String> associatedTunnelIds;
}

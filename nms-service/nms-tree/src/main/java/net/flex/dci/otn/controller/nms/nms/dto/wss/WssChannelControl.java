package net.flex.dci.otn.controller.nms.nms.dto.wss;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 7/7/2025 2:23 PM
 */
@Data
@Builder
public class WssChannelControl implements Serializable {

    private String channelIndex;

    private WssASEControl aSEControl;

    private WssPowerControl azPowerControl;

    private WssPowerControl zaPowerControl;


}

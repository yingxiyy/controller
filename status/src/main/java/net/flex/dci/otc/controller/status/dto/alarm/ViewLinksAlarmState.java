package net.flex.dci.otc.controller.status.dto.alarm;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 8/23/2023 3:49 PM
 */
@Data
@Builder
public class ViewLinksAlarmState implements Serializable {

    private List<ViewLinkAlarmState> viewLinkAlarmStates;
}

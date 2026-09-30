package net.flex.dci.otc.controller.status.dto.alarm;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;

/**
 * @version 1.0
 * @date 8/21/2023 3:29 PM
 */
@Data
@Builder
public class ViewLinkAlarmState implements Serializable {

    private String viewLinkId;

    private AlarmSeverity alarmSeverity;
}

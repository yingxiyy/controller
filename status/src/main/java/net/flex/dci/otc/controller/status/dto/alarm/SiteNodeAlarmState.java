package net.flex.dci.otc.controller.status.dto.alarm;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;

/**
 * @version 1.0
 * @date 2022/4/4 14:37
 */
@Data
@AllArgsConstructor
@Builder
public class SiteNodeAlarmState implements Serializable {

    private String siteNodeId;

    private AlarmSeverity alarmSeverity;
}

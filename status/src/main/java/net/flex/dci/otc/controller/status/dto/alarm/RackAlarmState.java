package net.flex.dci.otc.controller.status.dto.alarm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;

import java.io.Serializable;
import java.util.List;

/**
 * @version 1.0
 * @date 8/2/2023 4:28 PM
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RackAlarmState implements Serializable {

    private String siteNodeId;

    private String rackId;

    private AlarmSeverity alarmSeverity;

    private List<String> neIds;

}

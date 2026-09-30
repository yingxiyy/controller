package net.flex.dci.otc.controller.status.dto.alarm;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;

/**
 * @version 1.0
 * @date 2022/4/4 22:03
 */
@Data
@Builder
@AllArgsConstructor
public class TunnelsAlarmState implements Serializable {

    List<TunnelAlarmState> tunnelAlarmStates;


    @Data
    @Builder
    @AllArgsConstructor
    public static class TunnelAlarmState {

        private String tunnelId;

        private AlarmSeverity alarmSeverity;
    }
}

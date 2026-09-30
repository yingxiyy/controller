package net.flex.dci.otc.controller.status.dto.alarm;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;

/**
 * @version 1.0
 * @date 2022/4/4 22:02
 */
@Data
@Builder
@AllArgsConstructor
public class SiteLinksAlarmState implements Serializable {

    private List<SiteLinkAlarmState> siteLinkAlarmStates;


    @Data
    @Builder
    @AllArgsConstructor
    public static class SiteLinkAlarmState {

        private String siteLinkId;

        private AlarmSeverity alarmSeverity;
    }

}

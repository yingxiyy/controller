package net.flex.dci.otn.controller.nms.nms.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;

/**
 * @version 1.0
 * @date 10/24/2023 4:05 PM
 */
@Data
@Builder
public class FriendInfoObjectDto implements Serializable {

    private String friendName;

    private AlarmSeverity alarmState;

    private AlarmSeverity eventState;

}

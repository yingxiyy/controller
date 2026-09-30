package net.flex.dci.otc.controller.status.dto.alarm;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/4/4 11:50
 */
@Data
@AllArgsConstructor
@Builder
public class AlarmStateResult implements Serializable {

    //rack alarm state
    RackAlarmState rackAlarmState;

    List<RackAlarmState> rackAlarmStateList;
    //view link

    ViewLinkAlarmState viewLinkAlarmState;

    //view node
    ViewNodeAlarmState viewNodeAlarmState;

    List<ViewNodeAlarmState> viewNodeAlarmStates;

    //site node
    SiteNodeAlarmState siteNodeAlarmState;

    PhyLinksAlarmState phyLinkAlarmState;

    SiteLinksAlarmState siteLinkAlarmState;

    OchLinksAlarmState ochLinkAlarmState;

    TunnelsAlarmState tunnelAlarmState;

    EquipmentsAlarmState equipmentsAlarmState;

}
